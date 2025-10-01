package com.example.mobilereceiptprinter

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

/**
 * Manages synchronization status across multiple devices
 */
class SyncStatusManager(
    private val database: AppDatabase,
    private val deviceManager: DeviceManager
) {
    
    private val _syncStatus = MutableStateFlow(SyncStatus.IDLE)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus
    
    private val _connectedDevices = MutableStateFlow<List<DeviceInfo>>(emptyList())
    val connectedDevices: StateFlow<List<DeviceInfo>> = _connectedDevices
    
    private val _pendingSyncCount = MutableStateFlow(0)
    val pendingSyncCount: StateFlow<Int> = _pendingSyncCount
    
    private val syncScope = CoroutineScope(Dispatchers.IO)
    
    enum class SyncStatus {
        IDLE,
        SYNCING,
        SUCCESS,
        ERROR,
        OFFLINE
    }
    
    companion object {
        const val SYNC_STATUS_SYNCED = "SYNCED"
        const val SYNC_STATUS_PENDING = "PENDING"
        const val SYNC_STATUS_CONFLICT = "CONFLICT"
        const val SYNC_STATUS_ERROR = "ERROR"
    }
    
    /**
     * Update list of connected devices
     */
    fun updateConnectedDevices(devices: List<DeviceInfo>) {
        _connectedDevices.value = devices
    }
    
    /**
     * Start sync operation
     */
    fun startSync() {
        _syncStatus.value = SyncStatus.SYNCING
        updatePendingSyncCount()
    }
    
    /**
     * Complete sync operation
     */
    fun completeSyncSuccess() {
        _syncStatus.value = SyncStatus.SUCCESS
        updatePendingSyncCount()
    }
    
    /**
     * Mark sync as failed
     */
    fun completeSyncError(error: String) {
        _syncStatus.value = SyncStatus.ERROR
        logSyncError(error)
        updatePendingSyncCount()
    }
    
    /**
     * Set offline status
     */
    fun setOfflineStatus() {
        _syncStatus.value = SyncStatus.OFFLINE
    }
    
    /**
     * Set idle status
     */
    fun setIdleStatus() {
        _syncStatus.value = SyncStatus.IDLE
    }
    
    /**
     * Mark receipt as pending sync
     */
    suspend fun markReceiptPendingSync(receiptId: String) {
        database.receiptDao().updateSyncStatus(receiptId, SYNC_STATUS_PENDING)
        updatePendingSyncCount()
    }
    
    /**
     * Mark receipt as synced
     */
    suspend fun markReceiptSynced(receiptId: String) {
        database.receiptDao().updateSyncStatus(receiptId, SYNC_STATUS_SYNCED)
        updatePendingSyncCount()
    }
    
    /**
     * Mark receipt as conflict
     */
    suspend fun markReceiptConflict(receiptId: String) {
        database.receiptDao().updateSyncStatus(receiptId, SYNC_STATUS_CONFLICT)
        updatePendingSyncCount()
    }
    
    /**
     * Mark collection as pending sync
     */
    suspend fun markCollectionPendingSync(collectionId: String) {
        database.collectedReceiptDao().updateSyncStatus(collectionId, SYNC_STATUS_PENDING)
        updatePendingSyncCount()
    }
    
    /**
     * Mark collection as synced
     */
    suspend fun markCollectionSynced(collectionId: String) {
        database.collectedReceiptDao().updateSyncStatus(collectionId, SYNC_STATUS_SYNCED)
        updatePendingSyncCount()
    }
    
    /**
     * Get pending receipts for sync
     */
    suspend fun getPendingReceipts(): List<Receipt> {
        return database.receiptDao().getReceiptsBySyncStatus(SYNC_STATUS_PENDING)
    }
    
    /**
     * Get pending collections for sync
     */
    suspend fun getPendingCollections(): List<CollectedReceipt> {
        return database.collectedReceiptDao().getCollectedReceiptsBySyncStatus(SYNC_STATUS_PENDING)
    }
    
    /**
     * Get conflicted receipts
     */
    suspend fun getConflictedReceipts(): List<Receipt> {
        return database.receiptDao().getReceiptsBySyncStatus(SYNC_STATUS_CONFLICT)
    }
    
    /**
     * Update pending sync count
     */
    private fun updatePendingSyncCount() {
        syncScope.launch {
            val pendingReceipts = database.receiptDao().getReceiptsBySyncStatus(SYNC_STATUS_PENDING)
            val pendingCollections = database.collectedReceiptDao().getCollectedReceiptsBySyncStatus(SYNC_STATUS_PENDING)
            val conflictedReceipts = database.receiptDao().getReceiptsBySyncStatus(SYNC_STATUS_CONFLICT)
            
            val totalPending = pendingReceipts.size + pendingCollections.size + conflictedReceipts.size
            _pendingSyncCount.value = totalPending
        }
    }
    
    /**
     * Log sync operation
     */
    suspend fun logSync(syncType: String, recordCount: Int, status: String, error: String? = null) {
        val syncLog = DeviceSyncLog(
            deviceId = deviceManager.getDeviceId(),
            lastSyncTime = System.currentTimeMillis(),
            syncType = syncType,
            recordCount = recordCount,
            status = status,
            errorMessage = error
        )
        database.deviceSyncLogDao().insert(syncLog)
    }
    
    /**
     * Log sync error
     */
    private fun logSyncError(error: String) {
        syncScope.launch {
            logSync("ERROR", 0, "FAILED", error)
        }
    }
    
    /**
     * Get sync statistics
     */
    suspend fun getSyncStats(): SyncStats {
        val deviceId = deviceManager.getDeviceId()
        val logs = database.deviceSyncLogDao().getSyncLogsByDevice(deviceId)
        
        val successCount = logs.count { it.status == "SUCCESS" }
        val failureCount = logs.count { it.status == "FAILED" }
        val lastSync = logs.maxByOrNull { it.lastSyncTime }
        
        return SyncStats(
            totalSyncs = logs.size,
            successfulSyncs = successCount,
            failedSyncs = failureCount,
            lastSyncTime = lastSync?.lastSyncTime,
            pendingCount = _pendingSyncCount.value,
            connectedDevicesCount = _connectedDevices.value.size
        )
    }
}

/**
 * Sync statistics data class
 */
data class SyncStats(
    val totalSyncs: Int,
    val successfulSyncs: Int,
    val failedSyncs: Int,
    val lastSyncTime: Long?,
    val pendingCount: Int,
    val connectedDevicesCount: Int
)