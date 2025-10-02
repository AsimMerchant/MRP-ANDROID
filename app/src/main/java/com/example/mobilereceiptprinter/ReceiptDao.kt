package com.example.mobilereceiptprinter

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import androidx.room.OnConflictStrategy
import androidx.room.Transaction

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
    
    // Cascade delete methods that also clean up collections
    @Transaction
    suspend fun deleteAllReceiptsFromBillerWithCleanup(billerName: String) {
        // Delete collection records for receipts from this biller
        deleteCollectionsForBiller(billerName)
        // Delete the receipts
        deleteAllReceiptsFromBiller(billerName)
    }
    

    
    @Query("""
        DELETE FROM collected_receipts 
        WHERE receiptId IN (SELECT id FROM receipts WHERE biller = :billerName)
    """)
    suspend fun deleteCollectionsForBiller(billerName: String)

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
    
    // Audit and reporting queries
    @Query("SELECT COUNT(*) FROM receipts")
    suspend fun getTotalReceiptsCount(): Int
    
    @Query("SELECT SUM(CAST(REPLACE(amount, ',', '') AS REAL)) FROM receipts")
    suspend fun getTotalReceiptsAmount(): Double?
    
    @Query("SELECT COUNT(*) FROM receipts WHERE isCollected = 0")
    suspend fun getUncollectedReceiptsCount(): Int
    
    @Query("SELECT SUM(CAST(REPLACE(amount, ',', '') AS REAL)) FROM receipts WHERE isCollected = 0")
    suspend fun getUncollectedReceiptsAmount(): Double?
    
    @Query("SELECT * FROM receipts WHERE isCollected = 0 ORDER BY receiptNumber DESC")
    suspend fun getUncollectedReceiptsList(): List<Receipt>
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
    
    // Collection summary queries - only show collections where receipt still exists
    @Query("""
        SELECT r.*, cr.collectionDate, cr.collectionTime, cr.collectorName, cr.scannedBy 
        FROM receipts r 
        INNER JOIN collected_receipts cr ON r.id = cr.receiptId 
        ORDER BY cr.collectionDate DESC, cr.collectionTime DESC
    """)
    suspend fun getCollectedReceiptsWithDetails(): List<CollectedReceiptWithDetails>
    
    @Query("""
        SELECT COUNT(*) 
        FROM collected_receipts cr 
        INNER JOIN receipts r ON cr.receiptId = r.id
    """)
    suspend fun getCollectedReceiptsCount(): Int
    
    @Query("""
        SELECT SUM(CAST(REPLACE(r.amount, ',', '') AS REAL)) 
        FROM receipts r 
        INNER JOIN collected_receipts cr ON r.id = cr.receiptId
    """)
    suspend fun getTotalCollectedAmount(): Double?
    
    // Clean up orphaned collection records (where receipt no longer exists)
    @Query("""
        DELETE FROM collected_receipts 
        WHERE receiptId NOT IN (SELECT id FROM receipts)
    """)
    suspend fun cleanupOrphanedCollections()
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
