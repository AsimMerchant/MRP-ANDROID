package com.example.mobilereceiptprinter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
 * - Phase 4.1: Rapid scanning feedback (overlay + haptic)
 */
class ScannerViewModel(
    private val database: AppDatabase,
    private val deviceManager: DeviceManager
) : ViewModel() {
    
    /**
     * Phase 4.1: Scan status for immediate feedback
     */
    sealed class ScanStatus {
        data class Success(val receiptNumber: Int) : ScanStatus()
        data class Duplicate(val receiptNumber: Int) : ScanStatus()
        object Invalid : ScanStatus()
    }
    
    // Phase 4.1: Feedback state for rapid scanning
    private val _lastScanStatus = MutableStateFlow<ScanStatus?>(null)
    val lastScanStatus: StateFlow<ScanStatus?> = _lastScanStatus.asStateFlow()
    
    private val _showOverlay = MutableStateFlow(false)
    val showOverlay: StateFlow<Boolean> = _showOverlay.asStateFlow()
    
    // Existing state
    private val _scanResults = MutableStateFlow<List<ScanResult>>(emptyList())
    val scanResults: StateFlow<List<ScanResult>> = _scanResults.asStateFlow()
    
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()
    
    // Phase 4.1: Mutex lock to prevent race conditions during rapid scanning
    private val scanLock = Mutex()
    
    private val recentScans = mutableSetOf<String>()
    private val scanCooldown = 400L // 400ms global cooldown for rapid scanning
    private var lastScanTime = 0L // Global cooldown (not per-QR)
    
    /**
     * Process a scanned QR code
     * Phase 4.1: Enhanced with Mutex lock for race condition prevention
     * and overlay feedback for rapid scanning (1 scan/second target)
     */
    fun processScan(qrContent: String) {
        viewModelScope.launch {
            // Phase 4.1: Use Mutex to prevent race conditions during rapid scanning
            scanLock.withLock {
                val currentTime = System.currentTimeMillis()
                
                // Global cooldown check (prevents rapid duplicate scans)
                if (currentTime - lastScanTime < scanCooldown) {
                    return@launch
                }
                
                lastScanTime = currentTime
                
                // Clear previous status immediately for instant feedback on new scan
                _lastScanStatus.value = null
                _showOverlay.value = false
                
                _isScanning.value = true
                
                try {
                    // Validate QR code format and check database
                    val scanResult = validateAndProcessQR(qrContent)
                    
                    // Phase 4.1: Set scan status for overlay + haptic feedback
                    _lastScanStatus.value = when {
                        scanResult.isValid && scanResult.receiptNumber != null -> 
                            ScanStatus.Success(scanResult.receiptNumber)
                        scanResult.receiptInfo.contains("already collected", ignoreCase = true) && scanResult.receiptNumber != null -> 
                            ScanStatus.Duplicate(scanResult.receiptNumber)
                        else -> ScanStatus.Invalid
                    }
                    
                    // Phase 4.1: Show overlay (400ms for faster scanning)
                    _showOverlay.value = true
                    
                    // Launch separate coroutine to auto-dismiss overlay without blocking next scan
                    viewModelScope.launch {
                        delay(400)
                        _showOverlay.value = false
                        // Don't clear status here - let next scan clear it immediately
                    }
                    
                    // Add to results (keep last 10 scans)
                    val currentResults = _scanResults.value.toMutableList()
                    currentResults.add(0, scanResult) // Add to top
                    
                    if (currentResults.size > 10) {
                        currentResults.removeAt(currentResults.lastIndex)
                    }
                    
                    _scanResults.value = currentResults
                
                } catch (e: Exception) {
                    // Phase 4.1: Show error feedback overlay
                    _lastScanStatus.value = ScanStatus.Invalid
                    _showOverlay.value = true
                    
                    // Launch separate coroutine to auto-dismiss
                    viewModelScope.launch {
                        delay(400)
                        _showOverlay.value = false
                    }
                    
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
            } // End of scanLock.withLock
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
        
        // Fetch receipt from database
        val receipt = getReceiptById(receiptId)
        
        if (receipt == null) {
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
                receiptInfo = "Receipt already collected",
                receiptNumber = receipt.receiptNumber // Pass receipt number for overlay
            )
        }
        
        // Mark as collected in database
        markReceiptAsCollected(receiptId)
        
        return ScanResult(
            qrContent = qrContent,
            timestamp = getCurrentTimestamp(),
            isValid = true,
            receiptInfo = "Receipt #${receipt.receiptNumber} - Successfully collected!",
            receiptNumber = receipt.receiptNumber // Pass receipt number for overlay
        )
    }
    
    /**
     * Get receipt by ID from database
     */
    private suspend fun getReceiptById(receiptId: String): Receipt? {
        return try {
            database.receiptDao().getReceiptById(receiptId)
        } catch (e: Exception) {
            android.util.Log.e("ScannerVM", "Error fetching receipt: ${e.message}")
            null
        }
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
        lastScanTime = 0L // Reset global cooldown timer
    }
}