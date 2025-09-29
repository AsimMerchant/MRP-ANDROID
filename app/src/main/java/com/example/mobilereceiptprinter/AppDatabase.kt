package com.example.mobilereceiptprinter

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Receipt::class, 
        Suggestion::class, 
        CollectedReceipt::class, 
        Collector::class, 
        DeviceSyncLog::class
    ], 
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun receiptDao(): ReceiptDao
    abstract fun suggestionDao(): SuggestionDao
    abstract fun collectedReceiptDao(): CollectedReceiptDao
    abstract fun collectorDao(): CollectorDao
    abstract fun deviceSyncLogDao(): DeviceSyncLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Migration from version 2 to 3 (adding multi-device support)
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add new columns to existing receipts table
                database.execSQL("ALTER TABLE receipts ADD COLUMN qrCode TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE receipts ADD COLUMN deviceId TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE receipts ADD COLUMN isCollected INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE receipts ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'PENDING'")
                database.execSQL("ALTER TABLE receipts ADD COLUMN lastModified INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE receipts ADD COLUMN version INTEGER NOT NULL DEFAULT 1")
                
                // Create new tables
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS collected_receipts (
                        id TEXT PRIMARY KEY NOT NULL,
                        receiptId TEXT NOT NULL,
                        collectorName TEXT NOT NULL,
                        collectionTime TEXT NOT NULL,
                        collectionDate TEXT NOT NULL,
                        scannedBy TEXT NOT NULL,
                        collectorDeviceId TEXT NOT NULL,
                        syncStatus TEXT NOT NULL DEFAULT 'PENDING',
                        lastModified INTEGER NOT NULL
                    )
                """)
                
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS collectors (
                        id TEXT PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        deviceId TEXT NOT NULL,
                        isActive INTEGER NOT NULL DEFAULT 1,
                        syncStatus TEXT NOT NULL DEFAULT 'SYNCED',
                        lastModified INTEGER NOT NULL
                    )
                """)
                
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS device_sync_logs (
                        id TEXT PRIMARY KEY NOT NULL,
                        deviceId TEXT NOT NULL,
                        lastSyncTime INTEGER NOT NULL,
                        syncType TEXT NOT NULL,
                        recordCount INTEGER NOT NULL,
                        status TEXT NOT NULL,
                        errorMessage TEXT
                    )
                """)
                
                // Update existing receipts to have proper UUIDs
                // Note: In production, you'd want to preserve existing data
                // For now, we'll use fallback to destructive migration
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mrp_database"
                )
                .fallbackToDestructiveMigration() // For development - remove in production
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
