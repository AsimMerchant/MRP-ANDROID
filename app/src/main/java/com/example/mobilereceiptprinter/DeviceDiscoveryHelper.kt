package com.example.mobilereceiptprinter

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.net.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * DeviceDiscoveryHelper - Enhanced for Receipt Data Synchronization
 * 
 * Handles:
 * - mDNS/NSD service discovery for local network device detection
 * - Receipt data synchronization between devices
 * - JSON-based sync protocol
 * - Network status monitoring
 * - Conflict resolution for multi-device sync
 */
class DeviceDiscoveryHelper(
    private val context: Context,
    private val deviceManager: DeviceManager,
    private val syncStatusManager: SyncStatusManager
) {
    companion object {
        private const val TAG = "DeviceDiscoveryHelper"
        private const val SERVICE_TYPE = "_mrp_sync._tcp"
        private const val SERVICE_NAME = "MRP-Sync"
        private const val SYNC_PORT = 8765
        private const val DISCOVERY_TIMEOUT = 30000L // 30 seconds
        private const val SYNC_TIMEOUT = 10000L // 10 seconds
        private const val MAX_RETRY_ATTEMPTS = 3
    }

    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Network Discovery State
    private val _discoveredDevices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<DiscoveredDevice>> = _discoveredDevices.asStateFlow()
    
    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()
    
    private val _networkStatus = MutableStateFlow<NetworkStatus>(NetworkStatus.DISCONNECTED)
    val networkStatus: StateFlow<NetworkStatus> = _networkStatus.asStateFlow()
    
    // Sync State
    private val _syncProgress = MutableStateFlow<SyncProgress>(SyncProgress.IDLE)
    val syncProgress: StateFlow<SyncProgress> = _syncProgress.asStateFlow()
    
    private val _lastSyncResult = MutableStateFlow<SyncResult?>(null)
    val lastSyncResult: StateFlow<SyncResult?> = _lastSyncResult.asStateFlow()

    // Network Infrastructure
    private var serviceRegistration: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var syncServer: ServerSocket? = null
    private var isServiceRegistered = false

    // Data Classes
    data class DiscoveredDevice(
        val deviceId: String,
        val deviceName: String,
        val address: String,
        val port: Int,
        val role: DeviceManager.DeviceRole,
        val capabilities: List<String>,
        val lastSeen: Long = System.currentTimeMillis(),
        val networkLatency: Long = 0
    )

    enum class NetworkStatus {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        SYNC_AVAILABLE,
        SYNC_ERROR
    }

    data class SyncProgress(
        val status: SyncStatus,
        val progress: Float = 0f,
        val currentOperation: String = "",
        val deviceCount: Int = 0,
        val completedDevices: Int = 0
    ) {
        enum class SyncStatus {
            IDLE, DISCOVERING, CONNECTING, SYNCING_RECEIPTS, 
            SYNCING_COLLECTIONS, RESOLVING_CONFLICTS, COMPLETED, ERROR
        }
        
        companion object {
            val IDLE = SyncProgress(SyncStatus.IDLE)
        }
    }

    data class SyncResult(
        val success: Boolean,
        val timestamp: Long,
        val devicesSync: Int,
        val receiptsSync: Int,
        val collectionsSync: Int,
        val conflicts: Int,
        val errorMessage: String? = null
    )

    /**
     * Initialize network discovery and sync services
     */
    fun initialize() {
        Log.d(TAG, "Initializing DeviceDiscoveryHelper for device: ${deviceManager.getDeviceId()}")
        
        scope.launch {
            try {
                startSyncServer()
                registerService()
                updateNetworkStatus(NetworkStatus.CONNECTED)
                Log.d(TAG, "DeviceDiscoveryHelper initialized successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize DeviceDiscoveryHelper: ${e.message}", e)
                updateNetworkStatus(NetworkStatus.SYNC_ERROR)
            }
        }
    }

    /**
     * Start service discovery to find other MRP devices
     */
    fun startDiscovery() {
        if (_isDiscovering.value) {
            Log.d(TAG, "Discovery already in progress")
            return
        }

        Log.d(TAG, "Starting device discovery...")
        _isDiscovering.value = true
        updateSyncProgress(SyncProgress(SyncProgress.SyncStatus.DISCOVERING, 0f, "Discovering devices..."))

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {
                Log.e(TAG, "Discovery start failed: Error $errorCode")
                _isDiscovering.value = false
                updateNetworkStatus(NetworkStatus.SYNC_ERROR)
            }

            override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {
                Log.e(TAG, "Discovery stop failed: Error $errorCode")
            }

            override fun onDiscoveryStarted(serviceType: String?) {
                Log.d(TAG, "Discovery started for service type: $serviceType")
                updateNetworkStatus(NetworkStatus.SYNC_AVAILABLE)
            }

            override fun onDiscoveryStopped(serviceType: String?) {
                Log.d(TAG, "Discovery stopped")
                _isDiscovering.value = false
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo?) {
                serviceInfo?.let { 
                    Log.d(TAG, "Service found: ${it.serviceName}")
                    resolveService(it)
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo?) {
                serviceInfo?.let {
                    Log.d(TAG, "Service lost: ${it.serviceName}")
                    removeDiscoveredDevice(it.serviceName)
                }
            }
        }

        try {
            nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
            
            // Auto-stop discovery after timeout
            scope.launch {
                delay(DISCOVERY_TIMEOUT)
                if (_isDiscovering.value) {
                    stopDiscovery()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start discovery: ${e.message}", e)
            _isDiscovering.value = false
            updateNetworkStatus(NetworkStatus.SYNC_ERROR)
        }
    }

    /**
     * Stop service discovery
     */
    fun stopDiscovery() {
        if (!_isDiscovering.value) return

        Log.d(TAG, "Stopping device discovery...")
        
        try {
            discoveryListener?.let { nsdManager.stopServiceDiscovery(it) }
            _isDiscovering.value = false
            updateSyncProgress(SyncProgress.IDLE)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping discovery: ${e.message}", e)
        }
    }

    /**
     * Synchronize data with all discovered devices
     */
    suspend fun syncWithAllDevices(): SyncResult {
        val devices = _discoveredDevices.value
        if (devices.isEmpty()) {
            Log.w(TAG, "No devices found for synchronization")
            return SyncResult(false, System.currentTimeMillis(), 0, 0, 0, 0, "No devices found")
        }

        Log.d(TAG, "Starting sync with ${devices.size} devices...")
        updateSyncProgress(SyncProgress(
            SyncProgress.SyncStatus.CONNECTING, 
            0f, 
            "Connecting to devices...", 
            devices.size, 
            0
        ))

        val database = AppDatabase.getDatabase(context)
        var totalReceipts = 0
        var totalCollections = 0
        var totalConflicts = 0
        var successfulDevices = 0

        try {
            for ((index, device) in devices.withIndex()) {
                try {
                    updateSyncProgress(SyncProgress(
                        SyncProgress.SyncStatus.SYNCING_RECEIPTS,
                        index.toFloat() / devices.size,
                        "Syncing with ${device.deviceName}...",
                        devices.size,
                        index
                    ))

                    val syncResult = syncWithDevice(device, database)
                    if (syncResult.success) {
                        totalReceipts += syncResult.receiptsSync
                        totalCollections += syncResult.collectionsSync
                        totalConflicts += syncResult.conflicts
                        successfulDevices++
                    }
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync with device ${device.deviceName}: ${e.message}", e)
                }
            }

            val finalResult = SyncResult(
                success = successfulDevices > 0,
                timestamp = System.currentTimeMillis(),
                devicesSync = successfulDevices,
                receiptsSync = totalReceipts,
                collectionsSync = totalCollections,
                conflicts = totalConflicts
            )

            updateSyncProgress(SyncProgress(SyncProgress.SyncStatus.COMPLETED, 1f, "Sync completed", devices.size, successfulDevices))
            _lastSyncResult.value = finalResult
            
            // Log sync result to database
            syncStatusManager.logSync("MULTI_DEVICE_SYNC", successfulDevices, if (finalResult.success) "SUCCESS" else "PARTIAL")
            
            Log.d(TAG, "Sync completed: ${finalResult}")
            return finalResult

        } catch (e: Exception) {
            val errorResult = SyncResult(false, System.currentTimeMillis(), 0, 0, 0, 0, e.message)
            updateSyncProgress(SyncProgress(SyncProgress.SyncStatus.ERROR, 0f, "Sync failed: ${e.message}"))
            _lastSyncResult.value = errorResult
            Log.e(TAG, "Sync operation failed: ${e.message}", e)
            return errorResult
        }
    }

    /**
     * Synchronize data with a specific device
     */
    private suspend fun syncWithDevice(device: DiscoveredDevice, database: AppDatabase): SyncResult {
        return withContext(Dispatchers.IO) {
            var socket: Socket? = null
            var receiptCount = 0
            var collectionCount = 0
            var conflictCount = 0

            try {
                Log.d(TAG, "Connecting to device: ${device.deviceName} at ${device.address}:${device.port}")
                
                socket = Socket()
                socket.connect(InetSocketAddress(device.address, device.port), SYNC_TIMEOUT.toInt())
                
                val input = BufferedReader(InputStreamReader(socket.getInputStream()))
                val output = PrintWriter(socket.getOutputStream(), true)

                // Send sync request
                val syncRequest = createSyncRequest()
                output.println(syncRequest.toString())
                Log.d(TAG, "Sent sync request to ${device.deviceName}")

                // Receive and process response
                val response = input.readLine()
                if (response != null) {
                    val responseJson = JSONObject(response)
                    val syncResults = processSyncResponse(responseJson, database)
                    receiptCount = syncResults.first
                    collectionCount = syncResults.second
                    conflictCount = syncResults.third
                }

                return@withContext SyncResult(
                    success = true,
                    timestamp = System.currentTimeMillis(),
                    devicesSync = 1,
                    receiptsSync = receiptCount,
                    collectionsSync = collectionCount,
                    conflicts = conflictCount
                )

            } catch (e: Exception) {
                Log.e(TAG, "Error syncing with device ${device.deviceName}: ${e.message}", e)
                return@withContext SyncResult(
                    success = false,
                    timestamp = System.currentTimeMillis(),
                    devicesSync = 0,
                    receiptsSync = 0,
                    collectionsSync = 0,
                    conflicts = 0,
                    errorMessage = e.message
                )
            } finally {
                try {
                    socket?.close()
                } catch (e: Exception) {
                    Log.w(TAG, "Error closing socket: ${e.message}")
                }
            }
        }
    }

    /**
     * Create JSON sync request with local data
     */
    private suspend fun createSyncRequest(): JSONObject {
        val database = AppDatabase.getDatabase(context)
        val request = JSONObject()
        
        request.put("type", "SYNC_REQUEST")
        request.put("deviceId", deviceManager.getDeviceId())
        request.put("deviceName", deviceManager.getDeviceName())
        request.put("timestamp", System.currentTimeMillis())
        request.put("version", "1.0")

        // Add ALL receipts for bidirectional sync (not just PENDING)
        // This ensures deleted receipts can be recovered from other devices
        val receipts = database.receiptDao().getAllReceipts()
        val receiptsArray = JSONArray()
        
        receipts.forEach { receipt ->
            val receiptJson = JSONObject().apply {
                put("id", receipt.id)
                put("receiptNumber", receipt.receiptNumber)
                put("biller", receipt.biller)
                put("volunteer", receipt.volunteer)
                put("amount", receipt.amount)
                put("date", receipt.date)
                put("time", receipt.time)
                put("qrCode", receipt.qrCode)
                put("deviceId", receipt.deviceId)
                put("isCollected", receipt.isCollected)
                put("syncStatus", receipt.syncStatus)
                put("lastModified", receipt.lastModified)
                put("version", receipt.version)
            }
            receiptsArray.put(receiptJson)
        }
        request.put("receipts", receiptsArray)

        // Add ALL collections for bidirectional sync (not just PENDING)
        // This ensures deleted collections can be recovered from other devices  
        val collections = database.collectedReceiptDao().getAllCollectedReceipts()
        val collectionsArray = JSONArray()
        
        collections.forEach { collection ->
            val collectionJson = JSONObject().apply {
                put("id", collection.id)
                put("receiptId", collection.receiptId)
                put("collectorName", collection.collectorName)
                put("collectionTime", collection.collectionTime)
                put("collectionDate", collection.collectionDate)
                put("scannedBy", collection.scannedBy)
                put("collectorDeviceId", collection.collectorDeviceId)
                put("syncStatus", collection.syncStatus)
                put("lastModified", collection.lastModified)
            }
            collectionsArray.put(collectionJson)
        }
        request.put("collections", collectionsArray)

        return request
    }

    /**
     * Process sync response from remote device
     */
    private suspend fun processSyncResponse(
        response: JSONObject, 
        database: AppDatabase
    ): Triple<Int, Int, Int> {
        var receiptCount = 0
        var collectionCount = 0
        var conflictCount = 0

        try {
            // Process incoming receipts
            if (response.has("receipts")) {
                val receiptsArray = response.getJSONArray("receipts")
                for (i in 0 until receiptsArray.length()) {
                    val receiptJson = receiptsArray.getJSONObject(i)
                    val result = processIncomingReceipt(receiptJson, database)
                    when (result) {
                        "SYNCED" -> receiptCount++
                        "CONFLICT" -> conflictCount++
                    }
                }
            }

            // Process incoming collections
            if (response.has("collections")) {
                val collectionsArray = response.getJSONArray("collections")
                for (i in 0 until collectionsArray.length()) {
                    val collectionJson = collectionsArray.getJSONObject(i)
                    val result = processIncomingCollection(collectionJson, database)
                    when (result) {
                        "SYNCED" -> collectionCount++
                        "CONFLICT" -> conflictCount++
                    }
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error processing sync response: ${e.message}", e)
        }

        return Triple(receiptCount, collectionCount, conflictCount)
    }

    /**
     * Process incoming receipt with conflict resolution
     */
    private suspend fun processIncomingReceipt(receiptJson: JSONObject, database: AppDatabase): String {
        return withContext(Dispatchers.IO) {
            try {
                val receiptId = receiptJson.getString("id")
                val existingReceipt = database.receiptDao().getReceiptById(receiptId)
                
                val incomingReceipt = Receipt(
                    id = receiptId,
                    receiptNumber = receiptJson.getInt("receiptNumber"),
                    biller = receiptJson.getString("biller"),
                    volunteer = receiptJson.getString("volunteer"),
                    amount = receiptJson.getString("amount"),
                    date = receiptJson.getString("date"),
                    time = receiptJson.getString("time"),
                    qrCode = receiptJson.getString("qrCode"),
                    deviceId = receiptJson.getString("deviceId"),
                    isCollected = receiptJson.getBoolean("isCollected"),
                    syncStatus = receiptJson.getString("syncStatus"),
                    lastModified = receiptJson.getLong("lastModified"),
                    version = receiptJson.getInt("version")
                )

                if (existingReceipt == null) {
                    // New receipt - insert it
                    database.receiptDao().insert(incomingReceipt.copy(syncStatus = "SYNCED"))
                    Log.d(TAG, "Inserted new receipt: $receiptId")
                    return@withContext "SYNCED"
                } else {
                    // Conflict resolution based on timestamp and version
                    when {
                        incomingReceipt.version > existingReceipt.version -> {
                            // Incoming has higher version - update
                            database.receiptDao().update(incomingReceipt.copy(syncStatus = "SYNCED"))
                            Log.d(TAG, "Updated receipt (higher version): $receiptId")
                            return@withContext "SYNCED"
                        }
                        incomingReceipt.lastModified > existingReceipt.lastModified -> {
                            // Incoming is more recent - update
                            database.receiptDao().update(incomingReceipt.copy(syncStatus = "SYNCED"))
                            Log.d(TAG, "Updated receipt (more recent): $receiptId")
                            return@withContext "SYNCED"
                        }
                        else -> {
                            // Keep existing, mark conflict
                            database.receiptDao().update(existingReceipt.copy(syncStatus = "CONFLICT"))
                            Log.w(TAG, "Conflict detected for receipt: $receiptId")
                            return@withContext "CONFLICT"
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing incoming receipt: ${e.message}", e)
                return@withContext "ERROR"
            }
        }
    }

    /**
     * Process incoming collection with conflict resolution
     */
    private suspend fun processIncomingCollection(collectionJson: JSONObject, database: AppDatabase): String {
        return withContext(Dispatchers.IO) {
            try {
                val collectionId = collectionJson.getString("id")
                val existingCollection = database.collectedReceiptDao().getCollectedReceiptById(collectionId)
                
                val incomingCollection = CollectedReceipt(
                    id = collectionId,
                    receiptId = collectionJson.getString("receiptId"),
                    collectorName = collectionJson.getString("collectorName"),
                    collectionTime = collectionJson.getString("collectionTime"),
                    collectionDate = collectionJson.getString("collectionDate"),
                    scannedBy = collectionJson.getString("scannedBy"),
                    collectorDeviceId = collectionJson.getString("collectorDeviceId"),
                    syncStatus = collectionJson.getString("syncStatus"),
                    lastModified = collectionJson.getLong("lastModified")
                )

                if (existingCollection == null) {
                    // New collection - insert it
                    database.collectedReceiptDao().insert(incomingCollection.copy(syncStatus = "SYNCED"))
                    
                    // Update the corresponding receipt as collected
                    database.receiptDao().updateCollectionStatus(incomingCollection.receiptId, true)
                    
                    Log.d(TAG, "Inserted new collection: $collectionId")
                    return@withContext "SYNCED"
                } else {
                    // Conflict resolution based on timestamp
                    if (incomingCollection.lastModified > existingCollection.lastModified) {
                        database.collectedReceiptDao().update(incomingCollection.copy(syncStatus = "SYNCED"))
                        Log.d(TAG, "Updated collection (more recent): $collectionId")
                        return@withContext "SYNCED"
                    } else {
                        // Keep existing, mark conflict
                        database.collectedReceiptDao().update(existingCollection.copy(syncStatus = "CONFLICT"))
                        Log.w(TAG, "Conflict detected for collection: $collectionId")
                        return@withContext "CONFLICT"
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing incoming collection: ${e.message}", e)
                return@withContext "ERROR"
            }
        }
    }

    // Helper methods for network status and service management
    private fun startSyncServer() {
        scope.launch {
            try {
                syncServer = ServerSocket(SYNC_PORT)
                Log.d(TAG, "Sync server started on port $SYNC_PORT")
                
                while (!syncServer?.isClosed!!) {
                    try {
                        val clientSocket = syncServer?.accept()
                        clientSocket?.let { handleSyncRequest(it) }
                    } catch (e: Exception) {
                        if (!syncServer?.isClosed!!) {
                            Log.e(TAG, "Error accepting sync connection: ${e.message}", e)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting sync server: ${e.message}", e)
            }
        }
    }

    private fun handleSyncRequest(clientSocket: Socket) {
        scope.launch {
            try {
                val input = BufferedReader(InputStreamReader(clientSocket.getInputStream()))
                val output = PrintWriter(clientSocket.getOutputStream(), true)
                
                val requestLine = input.readLine()
                if (requestLine != null) {
                    val request = JSONObject(requestLine)
                    val response = createSyncResponse(request)
                    output.println(response.toString())
                    Log.d(TAG, "Handled sync request from device: ${request.optString("deviceName", "Unknown")}")
                }
                
                clientSocket.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error handling sync request: ${e.message}", e)
            }
        }
    }

    private suspend fun createSyncResponse(request: JSONObject): JSONObject {
        val database = AppDatabase.getDatabase(context)
        val response = JSONObject()
        
        response.put("type", "SYNC_RESPONSE")
        response.put("deviceId", deviceManager.getDeviceId())
        response.put("deviceName", deviceManager.getDeviceName())
        response.put("timestamp", System.currentTimeMillis())
        
        // Process incoming data from request
        if (request.has("receipts")) {
            processSyncResponse(request, database)
        }
        
        // Send our data back
        val syncData = createSyncRequest()
        response.put("receipts", syncData.getJSONArray("receipts"))
        response.put("collections", syncData.getJSONArray("collections"))
        
        return response
    }

    private fun registerService() {
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = "${SERVICE_NAME}_${deviceManager.getDeviceId()}"
            serviceType = SERVICE_TYPE
            port = SYNC_PORT
        }

        serviceRegistration = object : NsdManager.RegistrationListener {
            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                Log.e(TAG, "Service registration failed: $errorCode")
                updateNetworkStatus(NetworkStatus.SYNC_ERROR)
            }

            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                Log.e(TAG, "Service unregistration failed: $errorCode")
            }

            override fun onServiceRegistered(serviceInfo: NsdServiceInfo?) {
                Log.d(TAG, "Service registered: ${serviceInfo?.serviceName}")
                isServiceRegistered = true
                updateNetworkStatus(NetworkStatus.CONNECTED)
            }

            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo?) {
                Log.d(TAG, "Service unregistered")
                isServiceRegistered = false
            }
        }

        try {
            nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, serviceRegistration)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register service: ${e.message}", e)
            updateNetworkStatus(NetworkStatus.SYNC_ERROR)
        }
    }

    private fun resolveService(serviceInfo: NsdServiceInfo) {
        val resolveListener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                Log.w(TAG, "Resolve failed for ${serviceInfo?.serviceName}: $errorCode")
            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo?) {
                serviceInfo?.let { info ->
                    if (info.serviceName.contains(deviceManager.getDeviceId())) {
                        // Don't add ourselves
                        return
                    }

                    val device = DiscoveredDevice(
                        deviceId = extractDeviceId(info.serviceName),
                        deviceName = info.serviceName,
                        address = info.host.hostAddress ?: "",
                        port = info.port,
                        role = DeviceManager.DeviceRole.BOTH, // Default role
                        capabilities = listOf("SYNC", "RECEIPT_CREATION", "RECEIPT_COLLECTION")
                    )

                    addDiscoveredDevice(device)
                    Log.d(TAG, "Device resolved: ${device.deviceName} at ${device.address}:${device.port}")
                }
            }
        }

        nsdManager.resolveService(serviceInfo, resolveListener)
    }

    private fun extractDeviceId(serviceName: String): String {
        return serviceName.substringAfterLast("_").ifEmpty { serviceName }
    }

    private fun addDiscoveredDevice(device: DiscoveredDevice) {
        val currentDevices = _discoveredDevices.value.toMutableList()
        val existingIndex = currentDevices.indexOfFirst { it.deviceId == device.deviceId }
        
        if (existingIndex != -1) {
            currentDevices[existingIndex] = device
        } else {
            currentDevices.add(device)
        }
        
        _discoveredDevices.value = currentDevices
    }

    private fun removeDiscoveredDevice(serviceName: String) {
        val deviceId = extractDeviceId(serviceName)
        val currentDevices = _discoveredDevices.value.toMutableList()
        currentDevices.removeAll { it.deviceId == deviceId }
        _discoveredDevices.value = currentDevices
    }

    private fun updateNetworkStatus(status: NetworkStatus) {
        _networkStatus.value = status
        Log.d(TAG, "Network status updated: $status")
    }

    private fun updateSyncProgress(progress: SyncProgress) {
        _syncProgress.value = progress
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        Log.d(TAG, "Cleaning up DeviceDiscoveryHelper...")
        
        try {
            stopDiscovery()
            
            if (isServiceRegistered) {
                serviceRegistration?.let { nsdManager.unregisterService(it) }
            }
            
            syncServer?.close()
            scope.cancel()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup: ${e.message}", e)
        }
        
        Log.d(TAG, "DeviceDiscoveryHelper cleanup completed")
    }

    /**
     * Get current sync statistics
     */
    fun getSyncStatistics(): Map<String, Any> {
        val lastResult = _lastSyncResult.value
        return mapOf(
            "discoveredDevices" to _discoveredDevices.value.size,
            "networkStatus" to _networkStatus.value.name,
            "isDiscovering" to _isDiscovering.value,
            "lastSyncTime" to (lastResult?.timestamp ?: 0L),
            "lastSyncSuccess" to (lastResult?.success ?: false),
            "totalDevicesSync" to (lastResult?.devicesSync ?: 0),
            "totalReceiptsSync" to (lastResult?.receiptsSync ?: 0),
            "totalCollectionsSync" to (lastResult?.collectionsSync ?: 0),
            "totalConflicts" to (lastResult?.conflicts ?: 0)
        )
    }
}