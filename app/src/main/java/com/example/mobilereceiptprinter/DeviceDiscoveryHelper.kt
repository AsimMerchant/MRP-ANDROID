package com.example.mobilereceiptprinter

import android.content.Context
import android.content.SharedPreferences
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Data class for discovered devices
data class DiscoveredDevice(
    val name: String,
    val address: String,
    val port: Int
)

class DeviceDiscoveryHelper(private val context: Context) {
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    
    // Global device list that persists throughout app lifecycle
    private val _globalDiscoveredDevices = mutableStateListOf<DiscoveredDevice>()
    val globalDiscoveredDevices: SnapshotStateList<DiscoveredDevice> = _globalDiscoveredDevices
    
    // Track device timestamps for global cleanup
    private val globalDeviceTimestamps = mutableMapOf<String, Long>()
    
    // Global cleanup job
    private var globalCleanupJob: Job? = null
    
    // Track last discovery activity for immediate failure detection
    private var lastDiscoveryActivity = System.currentTimeMillis()
    
    // Android version-aware service type (Android 15 compatibility)
    private val serviceType = if (android.os.Build.VERSION.SDK_INT >= 35) {
        // Android 15+ - ensure strict DNS compliance
        "_mrpreport._tcp"  // No trailing dot for Android 15
    } else {
        // Older Android versions
        "_mrpreport._tcp."
    }
    
    // Use a safe service name generation
    private val serviceName = generateServiceName()
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var resolveListener: NsdManager.ResolveListener? = null

    // Get or create a persistent device UUID that remains consistent across app sessions
    private fun getOrCreatePersistentUUID(): String {
        val prefs = context.getSharedPreferences("device_discovery", Context.MODE_PRIVATE)
        val existingUUID = prefs.getString("device_uuid", null)
        
        return if (existingUUID != null) {
            Log.d("DeviceDiscovery", "Using existing persistent UUID: $existingUUID")
            existingUUID
        } else {
            // Generate new UUID and persist it
            val newUUID = java.util.UUID.randomUUID().toString().replace("-", "").take(6)
            prefs.edit().putString("device_uuid", newUUID).apply()
            Log.d("DeviceDiscovery", "Generated and stored new persistent UUID: $newUUID")
            newUUID
        }
    }

    // Generate a safe, Android 15-compatible service name with backwards compatibility
    private fun generateServiceName(): String {
        return try {
            // Get clean model name (Android 15 is stricter about special characters)
            val model = android.os.Build.MODEL?.let { rawModel ->
                // Remove/replace problematic characters for Android 15 compatibility
                rawModel.replace(Regex("[^a-zA-Z0-9]"), "").takeIf { it.isNotBlank() }
            } ?: "Device"
            
            // Get or generate persistent UUID (Android 15 may have length restrictions)
            val uuid = getOrCreatePersistentUUID()
            
            // Use underscore instead of hyphen (Android 15 compatibility)
            val serviceName = "MRP_${model}_$uuid"
            
            // Validate length (Android 15 may have stricter limits)
            val finalName = if (serviceName.length > 63) {
                // Truncate to DNS-safe length if needed
                serviceName.take(63)
            } else serviceName
            
            Log.d("DeviceDiscovery", "Generated service name: '$finalName' (length: ${finalName.length})")
            Log.d("DeviceDiscovery", "Original model: '${android.os.Build.MODEL}' -> Clean model: '$model'")
            
            finalName
        } catch (e: Exception) {
            // Ultra-safe fallback for any Android version
            val fallback = "MRP_${System.currentTimeMillis() % 1000000}"
            Log.e("DeviceDiscovery", "Failed to generate service name, using fallback: $fallback", e)
            fallback
        }
    }

    // Helper function to get current service status
    fun getServiceInfo(): String {
        return "Service: $serviceName | Type: $serviceType"
    }

    fun registerService(port: Int) {
        Log.d("DeviceDiscovery", "🚀 Attempting to register service:")
        Log.d("DeviceDiscovery", "   Name: '$serviceName' (length: ${serviceName.length})")
        Log.d("DeviceDiscovery", "   Type: '$serviceType' (length: ${serviceType.length})")
        Log.d("DeviceDiscovery", "   Port: $port")
        
        // Enhanced validation for Android 15 compatibility
        if (serviceName.isBlank()) {
            Log.e("DeviceDiscovery", "❌ Service name is blank!")
            return
        }
        if (serviceType.isBlank()) {
            Log.e("DeviceDiscovery", "❌ Service type is blank!")
            return
        }
        
        // Additional Android 15 validations
        if (serviceName.length > 63) {
            Log.e("DeviceDiscovery", "❌ Service name too long: ${serviceName.length} chars")
            return
        }
        
        // Check for invalid characters (Android 15 is stricter)
        val invalidChars = Regex("[^a-zA-Z0-9._-]")
        if (invalidChars.containsMatchIn(serviceName)) {
            Log.e("DeviceDiscovery", "❌ Service name contains invalid characters")
            return
        }
        
        try {
            val serviceInfo = NsdServiceInfo().apply {
                // Explicit assignment to avoid any scoping issues
                this.serviceName = this@DeviceDiscoveryHelper.serviceName
                this.serviceType = this@DeviceDiscoveryHelper.serviceType
                this.port = port
            }
            
            // Android 15 specific: Additional validation before registration
            if (android.os.Build.VERSION.SDK_INT >= 35) {
                Log.d("DeviceDiscovery", "🤖 Android 15+ detected - using enhanced validation")
                // Ensure all fields are properly set
                if (serviceInfo.serviceName.isNullOrBlank() || serviceInfo.serviceType.isNullOrBlank()) {
                    Log.e("DeviceDiscovery", "❌ ServiceInfo validation failed: name='${serviceInfo.serviceName}', type='${serviceInfo.serviceType}'")
                    return
                }
            }
            registrationListener = object : NsdManager.RegistrationListener {
                override fun onServiceRegistered(NsdServiceInfo: NsdServiceInfo) {
                    Log.d("DeviceDiscovery", "✅ Service registered successfully: '${NsdServiceInfo.serviceName}' on port ${NsdServiceInfo.port}")
                    Log.d("DeviceDiscovery", "📱 Android ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})")
                }
                override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    Log.e("DeviceDiscovery", "❌ Service registration failed: errorCode=$errorCode, Android API=${android.os.Build.VERSION.SDK_INT}")
                    Log.e("DeviceDiscovery", "   Failed service: name='${serviceInfo.serviceName}', type='${serviceInfo.serviceType}'")
                    
                    // Android 15 specific error handling
                    if (android.os.Build.VERSION.SDK_INT >= 35) {
                        when (errorCode) {
                            -1 -> Log.e("DeviceDiscovery", "   Android 15: Generic failure - possibly strict validation")
                            -2 -> Log.e("DeviceDiscovery", "   Android 15: Protocol error - service type may be invalid")
                            -3 -> Log.e("DeviceDiscovery", "   Android 15: Internal error - NSD service unavailable")
                            else -> Log.e("DeviceDiscovery", "   Android 15: Unknown error code: $errorCode")
                        }
                    }
                }
                override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
                    Log.d("DeviceDiscovery", "Service unregistered: ${serviceInfo.serviceName}")
                }
                override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    Log.e("DeviceDiscovery", "Service unregistration failed: errorCode=$errorCode")
                }
            }
            nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
        } catch (e: Exception) {
            Log.e("DeviceDiscovery", "Failed to register service", e)
        }
    }

    fun unregisterService() {
        try {
            registrationListener?.let { 
                Log.d("DeviceDiscovery", "🔄 Unregistering service: $serviceName")
                nsdManager.unregisterService(it)
                Log.d("DeviceDiscovery", "✅ Service unregistration initiated: $serviceName")
            } ?: run {
                Log.d("DeviceDiscovery", "ℹ️ No service registration to unregister")
            }
        } catch (e: Exception) {
            Log.e("DeviceDiscovery", "Failed to unregister service: $serviceName", e)
        }
    }

    fun discoverServices(
        onDeviceFound: (NsdServiceInfo) -> Unit,
        onDeviceLost: ((NsdServiceInfo) -> Unit)? = null
    ) {
        try {
            discoveryListener = object : NsdManager.DiscoveryListener {
                override fun onDiscoveryStarted(regType: String) {
                    Log.d("DeviceDiscovery", "🔍 Discovery started for service type: $regType")
                    Log.d("DeviceDiscovery", "👤 This device service name: $serviceName")
                }
                override fun onServiceFound(service: NsdServiceInfo) {
                    Log.d("DeviceDiscovery", "📱 Service found: ${service.serviceName} (type: ${service.serviceType})")
                    try {
                        // Flexible service type matching for cross-Android version compatibility
                        val isMatchingServiceType = service.serviceType == serviceType || 
                                                  service.serviceType == "_mrpreport._tcp." ||
                                                  service.serviceType == "_mrpreport._tcp"
                        
                        if (isMatchingServiceType) {
                            if (service.serviceName != serviceName) {
                                Log.d("DeviceDiscovery", "🔄 Attempting to resolve service: ${service.serviceName}")
                                val currentResolveListener = object : NsdManager.ResolveListener {
                                    override fun onServiceResolved(resolved: NsdServiceInfo) {
                                        Log.d("DeviceDiscovery", "✅ Service resolved: ${resolved.serviceName} at ${resolved.host?.hostAddress}:${resolved.port}")
                                        onDeviceFound(resolved)
                                    }
                                    override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                                        Log.e("DeviceDiscovery", "❌ Service resolve failed for ${serviceInfo.serviceName}: errorCode=$errorCode")
                                    }
                                }
                                nsdManager.resolveService(service, currentResolveListener)
                            } else {
                                Log.d("DeviceDiscovery", "⏭️ Skipping own service: ${service.serviceName}")
                            }
                        } else {
                            Log.d("DeviceDiscovery", "⏭️ Skipping different service type: '${service.serviceType}' (expected: '$serviceType')")
                        }
                    } catch (e: Exception) {
                        Log.e("DeviceDiscovery", "Error processing found service", e)
                    }
                }
                override fun onServiceLost(service: NsdServiceInfo) {
                    Log.d("DeviceDiscovery", "📤 Service lost: ${service.serviceName}")
                    // Check if it's our service type and notify UI for removal
                    val isMatchingServiceType = service.serviceType == serviceType || 
                                              service.serviceType == "_mrpreport._tcp." ||
                                              service.serviceType == "_mrpreport._tcp"
                    
                    if (isMatchingServiceType && service.serviceName != serviceName) {
                        Log.d("DeviceDiscovery", "🗑️ Removing lost device from UI: ${service.serviceName}")
                        onDeviceLost?.invoke(service)
                    }
                }
                override fun onDiscoveryStopped(serviceType: String) {
                    Log.d("DeviceDiscovery", "Discovery stopped")
                }
                override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                    Log.e("DeviceDiscovery", "❌ Start discovery failed: $errorCode")
                    // Try to restart discovery after a short delay for Android 15 NSD issues
                    CoroutineScope(Dispatchers.Main).launch {
                        delay(5000) // Wait 5 seconds before retry
                        Log.d("DeviceDiscovery", "🔄 Attempting to restart discovery due to previous failure")
                        try {
                            stopDiscovery()
                            startGlobalDiscovery()
                        } catch (e: Exception) {
                            Log.e("DeviceDiscovery", "Failed to restart discovery", e)
                        }
                    }
                }
                override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                    Log.e("DeviceDiscovery", "❌ Stop discovery failed: $errorCode")
                }
            }
            nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {
            Log.e("DeviceDiscovery", "Failed to start service discovery", e)
        }
    }

    fun stopDiscovery() {
        try {
            discoveryListener?.let { nsdManager.stopServiceDiscovery(it) }
        } catch (e: Exception) {
            Log.e("DeviceDiscovery", "Failed to stop discovery", e)
        }
    }
    
    /**
     * Start global discovery that maintains app-wide device list
     * This runs throughout the app lifecycle
     */
    fun startGlobalDiscovery() {
        Log.d("DeviceDiscovery", "🌐 Starting global device discovery")
        
        // Always clear any existing state for fresh real-time discovery
        _globalDiscoveredDevices.clear()
        globalDeviceTimestamps.clear()
        lastDiscoveryActivity = System.currentTimeMillis() // Reset activity tracker
        Log.d("DeviceDiscovery", "🧹 Cleared all existing devices for fresh start")
        
        // Start discovery with global device list management
        startDiscoveryCallbacks()
        
        // Start global cleanup coroutine for stale devices
        startGlobalCleanup()
    }
    
    /**
     * Start discovery with device callbacks (reusable for restarts)
     */
    private fun startDiscoveryCallbacks() {
        discoverServices(
            onDeviceFound = { serviceInfo ->
                val device = DiscoveredDevice(
                    name = serviceInfo.serviceName,
                    address = serviceInfo.host?.hostAddress ?: "Unknown",
                    port = serviceInfo.port
                )
                
                Log.d("DeviceDiscovery", "🔍 Device found callback triggered: ${device.name} at ${device.address}:${device.port}")
                
                // Enhanced duplicate detection - check by name AND IP address
                val existingByName = _globalDiscoveredDevices.indexOfFirst { it.name == device.name }
                val existingByIp = _globalDiscoveredDevices.indexOfFirst { 
                    it.address == device.address && it.address != "Unknown" 
                }
                
                when {
                    existingByName != -1 -> {
                        // Update existing device by name
                        _globalDiscoveredDevices[existingByName] = device
                        Log.d("DeviceDiscovery", "🔄 Updated existing device in global list: ${device.name} (Total: ${_globalDiscoveredDevices.size})")
                    }
                    existingByIp != -1 -> {
                        // IP collision detected - replace old entry with new one (newer UUID for same device)
                        val oldDevice = _globalDiscoveredDevices[existingByIp]
                        _globalDiscoveredDevices[existingByIp] = device
                        globalDeviceTimestamps.remove(oldDevice.name) // Remove old timestamp
                        Log.d("DeviceDiscovery", "🔄 IP collision detected - replaced '${oldDevice.name}' with '${device.name}' at ${device.address}")
                        Log.d("DeviceDiscovery", "   This prevents duplicate devices from same IP address")
                    }
                    else -> {
                        // Truly new device
                        _globalDiscoveredDevices.add(device)
                        Log.d("DeviceDiscovery", "➕ Added device to global list: ${device.name} (Total: ${_globalDiscoveredDevices.size})")
                    }
                }
                
                // Update timestamp for cleanup tracking
                val timestamp = System.currentTimeMillis()
                globalDeviceTimestamps[device.name] = timestamp
                lastDiscoveryActivity = timestamp // Track discovery activity
                Log.d("DeviceDiscovery", "⏰ Updated timestamp for ${device.name}: $timestamp")
            },
            onDeviceLost = { serviceInfo ->
                // Remove from global list when service is lost
                val wasRemoved = _globalDiscoveredDevices.removeAll { it.name == serviceInfo.serviceName }
                if (wasRemoved) {
                    globalDeviceTimestamps.remove(serviceInfo.serviceName)
                    Log.d("DeviceDiscovery", "➖ Removed device from global list: ${serviceInfo.serviceName}")
                }
            }
        )
    }
    
    /**
     * Start periodic cleanup of stale devices in global list
     */
    private fun startGlobalCleanup() {
        globalCleanupJob?.cancel() // Cancel any existing cleanup
        globalCleanupJob = CoroutineScope(Dispatchers.Main).launch {
            var cleanupCount = 0
            while (true) {
                delay(2000) // Check every 2 seconds for real-time updates
                cleanupCount++
                
                val currentTime = System.currentTimeMillis()
                val staleThreshold = 5000 // 5 seconds - aggressive real-time cleanup as requested
                
                Log.d("DeviceDiscovery", "🕐 Cleanup check #$cleanupCount - Current devices: ${_globalDiscoveredDevices.size}")
                
                // Check for discovery silence (potential NSD failure) - restart immediately if detected
                val timeSinceLastActivity = currentTime - lastDiscoveryActivity
                if (timeSinceLastActivity > 8000) { // 8 seconds of silence suggests NSD failure
                    Log.d("DeviceDiscovery", "🚨 Discovery silence detected (${timeSinceLastActivity}ms) - Immediate NSD restart!")
                    try {
                        stopDiscovery()
                        delay(500) // Brief pause
                        startDiscoveryCallbacks() // Restart discovery immediately
                        lastDiscoveryActivity = currentTime // Reset activity tracker
                        Log.d("DeviceDiscovery", "✅ Immediate recovery from discovery silence")
                    } catch (e: Exception) {
                        Log.e("DeviceDiscovery", "❌ Failed immediate recovery from discovery silence", e)
                    }
                }
                
                // Find and remove stale devices
                val staleDevices = globalDeviceTimestamps.filter { (deviceName, timestamp) ->
                    val age = currentTime - timestamp
                    val isStale = age > staleThreshold
                    Log.d("DeviceDiscovery", "📊 Device '$deviceName' age: ${age}ms, stale: $isStale")
                    isStale
                }.keys.toList()
                
                staleDevices.forEach { deviceName ->
                    _globalDiscoveredDevices.removeAll { it.name == deviceName }
                    globalDeviceTimestamps.remove(deviceName)
                    Log.d("DeviceDiscovery", "🧹 Cleaned up stale device: $deviceName")
                }
                
                if (staleDevices.isNotEmpty()) {
                    Log.d("DeviceDiscovery", "🧹 Global cleanup removed ${staleDevices.size} stale devices")
                } else {
                    Log.d("DeviceDiscovery", "✅ No stale devices found, keeping ${_globalDiscoveredDevices.size} active devices")
                }
                
                // Android 15 NSD Bug Workaround: Restart discovery every 10 seconds (5 cleanup cycles)
                // Faster recovery for real-time use case while maintaining aggressive cleanup
                if (cleanupCount % 5 == 0) {
                    Log.d("DeviceDiscovery", "🔄 Android 15 NSD bug workaround: Restarting discovery every 10s (cycle #$cleanupCount)")
                    try {
                        stopDiscovery()
                        delay(1000) // Brief pause to let NSD service clean up
                        
                        // Restart discovery callbacks
                        startDiscoveryCallbacks()
                        Log.d("DeviceDiscovery", "✅ Discovery restarted successfully")
                    } catch (e: Exception) {
                        Log.e("DeviceDiscovery", "❌ Failed to restart discovery during periodic refresh", e)
                    }
                }
            }
        }
    }
    
    /**
     * Get debug info about current discovery state
     */
    fun getDiscoveryStatus(): String {
        return "Discovery active: ${discoveryListener != null}, " +
               "Devices: ${_globalDiscoveredDevices.size}, " +
               "Timestamps: ${globalDeviceTimestamps.size}, " +
               "Cleanup job: ${globalCleanupJob?.isActive ?: false}"
    }
    
    /**
     * Restart discovery - useful for Android 15 NSD recovery
     */
    fun restartGlobalDiscovery() {
        Log.d("DeviceDiscovery", "🔄 Manually restarting global discovery")
        try {
            stopDiscovery()
            // Small delay to allow NSD service to clean up
            CoroutineScope(Dispatchers.Main).launch {
                delay(1000)
                startGlobalDiscovery()
            }
        } catch (e: Exception) {
            Log.e("DeviceDiscovery", "Failed to restart discovery", e)
        }
    }
    
    /**
     * Stop global discovery and cleanup
     */
    fun stopGlobalDiscovery() {
        Log.d("DeviceDiscovery", "🛑 Stopping global device discovery and cleanup")
        stopDiscovery()
        unregisterService()  // Ensure service is properly unregistered
        globalCleanupJob?.cancel()
        _globalDiscoveredDevices.clear()
        globalDeviceTimestamps.clear()
        Log.d("DeviceDiscovery", "🛑 Stopped global device discovery - service unregistered")
    }
}
