package com.example.mobilereceiptprinter

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import androidx.room.OnConflictStrategy

@Dao
interface ReceiptDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(receipt: Receipt)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(receipts: List<Receipt>)

    @Update
    suspend fun update(receipt: Receipt)

    @Delete
    suspend fun delete(receipt: Receipt)

    @Query("SELECT * FROM receipts ORDER BY receiptNumber DESC")
    suspend fun getAllReceipts(): List<Receipt>

    @Query("SELECT * FROM receipts WHERE biller = :billerName ORDER BY receiptNumber DESC")
    suspend fun getReceiptsByBiller(billerName: String): List<Receipt>

    @Query("SELECT DISTINCT biller FROM receipts ORDER BY biller")
    suspend fun getAllBillers(): List<String>

    @Query("SELECT DISTINCT volunteer FROM receipts ORDER BY volunteer")
    suspend fun getAllVolunteers(): List<String>

    @Query("DELETE FROM receipts WHERE biller = :billerName")
    suspend fun deleteAllReceiptsFromBiller(billerName: String)

    @Query("DELETE FROM receipts")
    suspend fun deleteAllReceipts()

    // NEW: Multi-device sync queries
    @Query("SELECT * FROM receipts WHERE id = :receiptId")
    suspend fun getReceiptById(receiptId: String): Receipt?

    @Query("SELECT * FROM receipts WHERE qrCode = :qrCode")
    suspend fun getReceiptByQrCode(qrCode: String): Receipt?

    @Query("SELECT * FROM receipts WHERE syncStatus = :status")
    suspend fun getReceiptsBySyncStatus(status: String): List<Receipt>

    @Query("SELECT * FROM receipts WHERE deviceId = :deviceId ORDER BY receiptNumber DESC")
    suspend fun getReceiptsByDevice(deviceId: String): List<Receipt>

    @Query("SELECT * FROM receipts WHERE isCollected = 0")
    suspend fun getUncollectedReceipts(): List<Receipt>

    @Query("UPDATE receipts SET isCollected = :isCollected, syncStatus = 'PENDING', lastModified = :timestamp WHERE id = :receiptId")
    suspend fun updateCollectionStatusWithTimestamp(receiptId: String, isCollected: Boolean, timestamp: Long)
    
    @Query("UPDATE receipts SET isCollected = :isCollected WHERE id = :receiptId")
    suspend fun updateCollectionStatus(receiptId: String, isCollected: Boolean)

    @Query("UPDATE receipts SET syncStatus = :status WHERE id = :receiptId")
    suspend fun updateSyncStatus(receiptId: String, status: String)
}

@Dao
interface CollectedReceiptDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(collectedReceipt: CollectedReceipt)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(collectedReceipts: List<CollectedReceipt>)

    @Update
    suspend fun update(collectedReceipt: CollectedReceipt)

    @Delete
    suspend fun delete(collectedReceipt: CollectedReceipt)

    @Query("SELECT * FROM collected_receipts ORDER BY collectionDate DESC, collectionTime DESC")
    suspend fun getAllCollectedReceipts(): List<CollectedReceipt>

    @Query("SELECT * FROM collected_receipts WHERE receiptId = :receiptId")
    suspend fun getCollectionsByReceiptId(receiptId: String): List<CollectedReceipt>

    @Query("SELECT * FROM collected_receipts WHERE collectorName = :collectorName ORDER BY collectionDate DESC")
    suspend fun getCollectionsByCollector(collectorName: String): List<CollectedReceipt>

    @Query("SELECT * FROM collected_receipts WHERE collectorDeviceId = :deviceId ORDER BY collectionDate DESC")
    suspend fun getCollectionsByDevice(deviceId: String): List<CollectedReceipt>

    @Query("SELECT * FROM collected_receipts WHERE syncStatus = :status")
    suspend fun getCollectedReceiptsBySyncStatus(status: String): List<CollectedReceipt>
    
    @Query("SELECT * FROM collected_receipts WHERE id = :id")
    suspend fun getCollectedReceiptById(id: String): CollectedReceipt?

    @Query("UPDATE collected_receipts SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String)

    @Query("DELETE FROM collected_receipts")
    suspend fun deleteAllCollections()
}

@Dao
interface CollectorDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(collector: Collector)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(collectors: List<Collector>)

    @Update
    suspend fun update(collector: Collector)

    @Delete
    suspend fun delete(collector: Collector)

    @Query("SELECT * FROM collectors WHERE isActive = 1 ORDER BY name")
    suspend fun getActiveCollectors(): List<Collector>

    @Query("SELECT * FROM collectors ORDER BY name")
    suspend fun getAllCollectors(): List<Collector>

    @Query("SELECT * FROM collectors WHERE deviceId = :deviceId")
    suspend fun getCollectorsByDevice(deviceId: String): List<Collector>

    @Query("SELECT * FROM collectors WHERE syncStatus = :status")
    suspend fun getCollectorsBySyncStatus(status: String): List<Collector>

    @Query("UPDATE collectors SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String)

    @Query("DELETE FROM collectors")
    suspend fun deleteAllCollectors()
}

@Dao
interface DeviceSyncLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(syncLog: DeviceSyncLog)

    @Query("SELECT * FROM device_sync_logs WHERE deviceId = :deviceId ORDER BY lastSyncTime DESC")
    suspend fun getSyncLogsByDevice(deviceId: String): List<DeviceSyncLog>

    @Query("SELECT * FROM device_sync_logs ORDER BY lastSyncTime DESC")
    suspend fun getAllSyncLogs(): List<DeviceSyncLog>

    @Query("SELECT * FROM device_sync_logs WHERE deviceId = :deviceId AND syncType = :syncType ORDER BY lastSyncTime DESC LIMIT 1")
    suspend fun getLastSyncLog(deviceId: String, syncType: String): DeviceSyncLog?

    @Query("DELETE FROM device_sync_logs WHERE lastSyncTime < :cutoffTime")
    suspend fun deleteOldSyncLogs(cutoffTime: Long)

    @Query("DELETE FROM device_sync_logs")
    suspend fun deleteAllSyncLogs()
}
