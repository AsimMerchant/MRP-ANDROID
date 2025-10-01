package com.example.mobilereceiptprinter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * ViewModel for Camera Scanner Screen
 * 
 * Handles:
 * - QR code scan processing and validation
 * - Scan results management
 * - Database interactions for receipt validation
 * - Collection status updates
 */
class ScannerViewModel(
    private val database: AppDatabase,
    private val deviceManager: DeviceManager
) : ViewModel() {
    
    private val _scanResults = MutableStateFlow<List<ScanResult>>(emptyList())
    val scanResults: StateFlow<List<ScanResult>> = _scanResults.asStateFlow()
    
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()
    
    private val recentScans = mutableSetOf<String>()
    private val scanCooldown = 2000L // 2 seconds cooldown between same QR scans
    private val scanTimestamps = mutableMapOf<String, Long>()
    
    /**
     * Process a scanned QR code
     * Includes cooldown logic to prevent duplicate rapid scans
     */
    fun processScan(qrContent: String) {
        viewModelScope.launch {
            val currentTime = System.currentTimeMillis()
            val lastScanTime = scanTimestamps[qrContent] ?: 0
            
            // Check cooldown period
            if (currentTime - lastScanTime < scanCooldown) {
                return@launch
            }
            
            scanTimestamps[qrContent] = currentTime
            _isScanning.value = true
            
            try {
                // Validate QR code format and check database
                val scanResult = validateAndProcessQR(qrContent)
                
                // Add to results (keep last 10 scans)
                val currentResults = _scanResults.value.toMutableList()
                currentResults.add(0, scanResult) // Add to top
                
                if (currentResults.size > 10) {
                    currentResults.removeAt(currentResults.lastIndex)
                }
                
                _scanResults.value = currentResults
                
            } catch (e: Exception) {
                // Handle scanning error
                val errorResult = ScanResult(
                    qrContent = qrContent.take(20) + "...",
                    timestamp = getCurrentTimestamp(),
                    isValid = false,
                    receiptInfo = "Scan Error: ${e.message ?: "Unknown error"}"
                )
                
                val currentResults = _scanResults.value.toMutableList()
                currentResults.add(0, errorResult)
                if (currentResults.size > 10) {
                    currentResults.removeAt(currentResults.lastIndex)
                }
                _scanResults.value = currentResults
                
            } finally {
                _isScanning.value = false
            }
        }
    }
    
    /**
     * Validate QR code and process collection
     */
    private suspend fun validateAndProcessQR(qrContent: String): ScanResult {

        
        // Check if QR matches MRP format
        if (!QRCodeGenerator.validateQRFormat(qrContent)) {
            return ScanResult(
                qrContent = qrContent.take(20) + "...",
                timestamp = getCurrentTimestamp(),
                isValid = false,
                receiptInfo = "Invalid QR Format - Not an MRP receipt"
            )
        }
        
        // Extract receipt ID from QR
        val receiptId = QRCodeGenerator.extractReceiptId(qrContent)
        if (receiptId == null) {
            return ScanResult(
                qrContent = qrContent.take(20) + "...",
                timestamp = getCurrentTimestamp(),
                isValid = false,
                receiptInfo = "Invalid QR - Could not extract receipt ID"
            )
        }
        
        // Validate receipt exists in database
        val isValidReceipt = validateReceiptExists(receiptId)
        
        if (!isValidReceipt) {
            return ScanResult(
                qrContent = qrContent.take(20) + "...",
                timestamp = getCurrentTimestamp(),
                isValid = false,
                receiptInfo = "Receipt not found in database"
            )
        }
        
        // Check if already collected
        val isAlreadyCollected = checkReceiptCollected(receiptId)
        
        if (isAlreadyCollected) {
            return ScanResult(
                qrContent = qrContent.take(20) + "...",
                timestamp = getCurrentTimestamp(),
                isValid = false,
                receiptInfo = "Receipt already collected"
            )
        }
        
        // Mark as collected in database
        markReceiptAsCollected(receiptId)
        
        return ScanResult(
            qrContent = qrContent,
            timestamp = getCurrentTimestamp(),
            isValid = true,
            receiptInfo = "Receipt #${receiptId.take(8)} - Successfully collected!"
        )
    }
    
    /**
     * Validate receipt exists in database
     */
    private suspend fun validateReceiptExists(receiptId: String): Boolean {
        return try {
            val receipt = database.receiptDao().getReceiptById(receiptId)
            receipt != null
        } catch (e: Exception) {
            android.util.Log.e("ScannerVM", "Error validating receipt: ${e.message}")
            false
        }
    }
    
    /**
     * Check if receipt is already collected
     */
    private suspend fun checkReceiptCollected(receiptId: String): Boolean {
        return try {
            val collections = database.collectedReceiptDao().getCollectionsByReceiptId(receiptId)
            collections.isNotEmpty()
        } catch (e: Exception) {
            android.util.Log.e("ScannerVM", "Error checking collection status: ${e.message}")
            false
        }
    }
    
    /**
     * Mark receipt as collected in database
     */
    private suspend fun markReceiptAsCollected(receiptId: String) {
        try {
            val currentTime = System.currentTimeMillis()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val now = Date(currentTime)
            
            // Create collection record
            val collectedReceipt = CollectedReceipt(
                receiptId = receiptId,
                collectorName = "Scanner User", // TODO: Get from user preferences
                collectionTime = timeFormat.format(now),
                collectionDate = dateFormat.format(now),
                scannedBy = "QR Scanner", // User who scanned the receipt
                collectorDeviceId = deviceManager.getDeviceId(),
                syncStatus = "PENDING"
            )
            
            // Insert collection record
            database.collectedReceiptDao().insert(collectedReceipt)
            
            // Update receipt status
            database.receiptDao().updateCollectionStatusWithTimestamp(
                receiptId = receiptId,
                isCollected = true,
                timestamp = currentTime
            )
            
            android.util.Log.d("ScannerVM", "Receipt $receiptId marked as collected")
        } catch (e: Exception) {
            android.util.Log.e("ScannerVM", "Error marking receipt as collected: ${e.message}")
        }
    }
    
    /**
     * Get current timestamp as formatted string
     */
    private fun getCurrentTimestamp(): String {
        val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return formatter.format(Date())
    }
    
    /**
     * Clear scan results
     */
    fun clearResults() {
        _scanResults.value = emptyList()
        scanTimestamps.clear()
    }
}