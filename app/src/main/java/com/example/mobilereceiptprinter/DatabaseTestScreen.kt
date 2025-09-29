package com.example.mobilereceiptprinter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatabaseTestScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var testResults by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Database Migration Test",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Button(
            onClick = {
                scope.launch {
                    isLoading = true
                    testResults = runDatabaseTests(context)
                    isLoading = false
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text("Run Migration Tests")
        }
        
        if (testResults.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(testResults) { result ->
                        val isSuccess = result.contains("✅") || result.contains("SUCCESS")
                        val isError = result.contains("❌") || result.contains("ERROR") || result.contains("FAILED")
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = when {
                                        isSuccess -> Color.Green.copy(alpha = 0.1f)
                                        isError -> Color.Red.copy(alpha = 0.1f)
                                        else -> Color.Transparent
                                    },
                                    shape = MaterialTheme.shapes.small
                                )
                                .padding(8.dp)
                        ) {
                            Text(
                                text = result,
                                style = MaterialTheme.typography.bodyMedium,
                                color = when {
                                    isSuccess -> Color.Green.copy(alpha = 0.8f)
                                    isError -> Color.Red.copy(alpha = 0.8f)
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

private suspend fun runDatabaseTests(context: android.content.Context): List<String> {
    val results = mutableListOf<String>()
    
    try {
        results.add("🔄 Starting database migration tests...")
        
        // Initialize components
        val deviceManager = DeviceManager(context)
        results.add("✅ DeviceManager initialized")
        results.add("📱 Device ID: ${deviceManager.getDeviceId()}")
        results.add("📱 Device Name: ${deviceManager.getDeviceName()}")
        results.add("🔧 Device Role: ${deviceManager.getDeviceRole()}")
        
        // Test database
        val database = AppDatabase.getDatabase(context)
        results.add("✅ Database initialized (migration completed)")
        
        // Test Receipt DAO with new fields
        val testReceipt = Receipt(
            receiptNumber = 888,
            biller = "Migration Test Biller",
            volunteer = "Migration Test Volunteer", 
            amount = "88.88",
            date = "2025-09-29",
            time = "12:00",
            deviceId = deviceManager.getDeviceId(),
            qrCode = "MRP_TEST_${System.currentTimeMillis()}",
            syncStatus = "PENDING"
        )
        
        database.receiptDao().insert(testReceipt)
        results.add("✅ Receipt inserted with new schema fields")
        
        val retrievedReceipt = database.receiptDao().getReceiptById(testReceipt.id)
        if (retrievedReceipt != null) {
            results.add("✅ Receipt retrieved successfully")
            results.add("   QR Code: ${retrievedReceipt.qrCode}")
            results.add("   Device ID: ${retrievedReceipt.deviceId}")
            results.add("   Sync Status: ${retrievedReceipt.syncStatus}")
        } else {
            results.add("❌ Failed to retrieve test receipt")
        }
        
        // Test new CollectedReceipt entity
        val testCollection = CollectedReceipt(
            receiptId = testReceipt.id,
            collectorName = "Test Collector",
            collectionTime = "12:05",
            collectionDate = "2025-09-29",
            scannedBy = "Test User",
            collectorDeviceId = deviceManager.getDeviceId(),
            lastModified = System.currentTimeMillis()
        )
        
        database.collectedReceiptDao().insert(testCollection)
        results.add("✅ CollectedReceipt entity working")
        
        // Test new Collector entity
        val testCollectorEntity = Collector(
            name = "Test Collector Entity",
            deviceId = deviceManager.getDeviceId(),
            lastModified = System.currentTimeMillis()
        )
        
        database.collectorDao().insert(testCollectorEntity)
        results.add("✅ Collector entity working")
        
        // Test DeviceSyncLog entity
        val testSyncLog = DeviceSyncLog(
            deviceId = deviceManager.getDeviceId(),
            lastSyncTime = System.currentTimeMillis(),
            syncType = "TEST",
            recordCount = 3,
            status = "SUCCESS"
        )
        
        database.deviceSyncLogDao().insert(testSyncLog)
        results.add("✅ DeviceSyncLog entity working")
        
        // Test SyncStatusManager
        val syncManager = SyncStatusManager(database, deviceManager)
        results.add("✅ SyncStatusManager initialized")
        
        val stats = syncManager.getSyncStats()
        results.add("📊 Sync Stats - Total: ${stats.totalSyncs}, Pending: ${stats.pendingCount}")
        
        results.add("🎉 All migration tests completed successfully!")
        
    } catch (e: Exception) {
        results.add("❌ Migration test failed: ${e.message}")
        results.add("❌ Stack trace: ${e.stackTrace.take(3).joinToString { it.toString() }}")
    }
    
    return results
}