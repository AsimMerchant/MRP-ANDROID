package com.example.mobilereceiptprinter

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "receipts")
data class Receipt(
    @PrimaryKey val id: String = UUID.randomUUID().toString(), // Global UUID for multi-device sync
    val receiptNumber: Int,
    val biller: String,
    val volunteer: String,
    val amount: String,
    val date: String,
    val time: String,
    // NEW: Multi-device sync fields
    val qrCode: String = "", // QR code content for scanning
    val deviceId: String = "", // Device that created this receipt
    val isCollected: Boolean = false, // Collection status
    val syncStatus: String = "PENDING", // SYNCED, PENDING, CONFLICT
    val lastModified: Long = 0L, // For conflict resolution - will be set programmatically
    val version: Int = 1 // For optimistic concurrency control
)

@Entity(tableName = "collected_receipts")
data class CollectedReceipt(
    @PrimaryKey val id: String = UUID.randomUUID().toString(), // Global UUID
    val receiptId: String, // Foreign key to Receipt
    val collectorName: String,
    val collectionTime: String,
    val collectionDate: String,
    val scannedBy: String, // User who performed the scan
    val collectorDeviceId: String, // Device where collection happened
    val syncStatus: String = "PENDING", // Sync status
    val lastModified: Long = 0L // Timestamp for sync ordering - will be set programmatically
)

@Entity(tableName = "collectors")
data class Collector(
    @PrimaryKey val id: String = UUID.randomUUID().toString(), // Global UUID
    val name: String,
    val deviceId: String, // Associated device
    val isActive: Boolean = true,
    val syncStatus: String = "SYNCED",
    val lastModified: Long = 0L // Will be set programmatically
)

@Entity(tableName = "device_sync_logs")
data class DeviceSyncLog(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val deviceId: String,
    val lastSyncTime: Long,
    val syncType: String, // RECEIPT, COLLECTION, FULL, HEARTBEAT
    val recordCount: Int,
    val status: String, // SUCCESS, FAILED, PARTIAL
    val errorMessage: String? = null
)

@Entity(tableName = "suggestions")
data class Suggestion(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "biller" or "volunteer"
    val name: String
)

/**
 * Data class for collection report - combines receipt and collection details
 */
data class CollectedReceiptWithDetails(
    // Receipt fields
    val id: String,
    val receiptNumber: Int,
    val biller: String,
    val volunteer: String,
    val amount: String,
    val date: String,
    val time: String,
    val qrCode: String,
    val deviceId: String,
    val isCollected: Boolean,
    val syncStatus: String,
    val lastModified: Long,
    val version: Int,
    // Collection fields
    val collectionDate: String,
    val collectionTime: String,
    val collectorName: String,
    val scannedBy: String
)
