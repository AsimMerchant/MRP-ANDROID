package com.example.mobilereceiptprinter
import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.security.MessageDigest
import java.util.*

/**
 * QRCodeGenerator - Phase 3: Cross-Device QR Generation
 * 
 * Generates globally unique QR codes for receipts to enable cross-device collection tracking.
 * QR format: MRP_{receiptId}_{deviceId}_{hash}
 * 
 * Features:
 * - Global uniqueness across all devices
 * - Tamper detection via cryptographic hash
 * - Support for thermal printer integration
 * - Bitmap generation for UI display
 */
object QRCodeGenerator {

    private const val QR_PREFIX = "MRP"
    private const val QR_SEPARATOR = "_"
    
    /**
     * Generate a unique QR code content string for a receipt
     * 
     * @param receiptId Global UUID of the receipt
     * @param deviceId Device that created the receipt
     * @param receiptData Additional receipt data for hash generation (biller, amount, timestamp)
     * @return QR code content string in format: MRP_{receiptId}_{deviceId}_{hash}
     */
    fun generateQRContent(
        receiptId: String,
        deviceId: String,
        receiptData: String
    ): String {
        // Generate tamper-detection hash from receipt data
        val hash = generateHash("$receiptId$deviceId$receiptData")
        
        // Format: MRP_{receiptId}_{deviceId}_{hash}
        val qrContent = "${QR_PREFIX}${QR_SEPARATOR}${receiptId}${QR_SEPARATOR}${deviceId}${QR_SEPARATOR}${hash}"
        

        
        return qrContent
    }
    
    /**
     * Generate QR code bitmap for UI display
     * 
     * @param qrContent The QR code content string
     * @param size Size of the QR code in pixels (width and height)
     * @return Bitmap of the QR code or null if generation fails
     */
    fun generateQRBitmap(qrContent: String, size: Int = 200): Bitmap? {
        return try {
            val writer = QRCodeWriter()
            val hints = hashMapOf<EncodeHintType, Any>(
                EncodeHintType.MARGIN to 1,
                EncodeHintType.CHARACTER_SET to "UTF-8"
            )
            
            val bitMatrix = writer.encode(qrContent, BarcodeFormat.QR_CODE, size, size, hints)
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
            
            // Convert bit matrix to bitmap
            for (x in 0 until size) {
                for (y in 0 until size) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            
            bitmap
        } catch (e: Exception) {
            android.util.Log.e("QRCodeGenerator", "Failed to generate QR bitmap: ${e.message}", e)
            null
        }
    }
    
    /**
     * Generate thermal printer compatible QR code string with left alignment
     * 
     * @param qrContent The QR code content string
     * @return ESC/POS commands for printing QR code on thermal printer
     */
    fun generateThermalPrinterQR(qrContent: String): String {
        // Simplified QR code generation at top of receipt
        val esc = "\u001B"  // ESC character
        val gs = "\u001D"   // GS character
        
        return buildString {
            // No spacing - keep QR code generation clean

            // Set QR code model (Model 2)
            append("${gs}(k")
            append("\u0004\u0000")  // Length: 4 bytes
            append("\u0031A")       // Function 49, Command 65 (Set model)
            append("\u0002\u0000")  // Model 2
            
            // Set QR code size (size 5 for better mobile scanning)
            append("${gs}(k")
            append("\u0003\u0000")  // Length: 3 bytes
            append("\u0031C")       // Function 49, Command 67 (Set size)
            append("\u0005")        // Size 5
            
            // Set error correction level (Level M - 15%)
            append("${gs}(k")
            append("\u0003\u0000")  // Length: 3 bytes
            append("\u0031E")       // Function 49, Command 69 (Set error correction)
            append("\u0031")        // Level M
            
            // Store QR code data
            val dataLength = qrContent.length + 3
            val lengthLow = (dataLength % 256).toChar()
            val lengthHigh = (dataLength / 256).toChar()
            append("${gs}(k")
            append("$lengthLow$lengthHigh")  // Length of data
            append("\u0031P0")               // Function 49, Command 80, Store data
            append(qrContent)
            
            // Print QR code
            append("${gs}(k")
            append("\u0003\u0000")  // Length: 3 bytes
            append("\u0031Q")       // Function 49, Command 81 (Print)
            append("\u0030")        // Print stored QR code
        }
    }
    
    /**
     * Generate thermal printer QR code with receipt number positioned optimally
     * 
     * @param qrContent The QR code content string
     * @param receiptNumber The receipt number to display
     * @return ESC/POS commands for QR code with receipt number
     */
    fun generateThermalPrinterQRWithNumber(qrContent: String, receiptNumber: Int): String {
        val esc = "\u001B"  // ESC character
        val gs = "\u001D"   // GS character
        
        return buildString {
            // First print the receipt number in bold on the right
            append("${esc}a\u0002")  // Right alignment
            append("${esc}!\u0030#$receiptNumber${esc}!\u0000\n")  // Bold receipt number
            
            // Then print QR code on the left
            append("${esc}a\u0000")  // Left alignment
            
            // Set QR code model (Model 2) - Using proper byte structure
            append("${gs}(k")
            append("\u0004\u0000")  // Length: 4 bytes
            append("\u0031A")       // Function 49, Command 65 (Set model)
            append("\u0002\u0000")  // Model 2
            
            // Set QR code size (module size 3 for compact size)
            append("${gs}(k")
            append("\u0003\u0000")  // Length: 3 bytes
            append("\u0031C")       // Function 49, Command 67 (Set size)
            append("\u0003")        // Size 3
            
            // Set error correction level (Level M - 15%)
            append("${gs}(k")
            append("\u0003\u0000")  // Length: 3 bytes
            append("\u0031E")       // Function 49, Command 69 (Set error correction)
            append("\u0031")        // Level M
            
            // Store QR code data
            val dataLength = qrContent.length + 3
            val lengthLow = (dataLength % 256).toChar()
            val lengthHigh = (dataLength / 256).toChar()
            append("${gs}(k")
            append("$lengthLow$lengthHigh")  // Length of data
            append("\u0031P0")               // Function 49, Command 80, Store data
            append(qrContent)
            
            // Set left margin to 0 to force QR code to far left
            append("${gs}L\u0000\u0000")  // GS L - set left margin to 0
            
            // Print QR code at leftmost position
            append("${gs}(k")
            append("\u0003\u0000")  // Length: 3 bytes
            append("\u0031Q")       // Function 49, Command 81 (Print)
            append("\u0030")        // Print stored QR code
            
            // Reset left margin to default
            append("${gs}L\u0000\u0000")
        }
    }
    
    /**
     * Validate QR code content format
     * 
     * @param qrContent The QR code content to validate
     * @return True if format is valid MRP QR code, false otherwise
     */
    fun validateQRFormat(qrContent: String): Boolean {
        val parts = qrContent.split(QR_SEPARATOR)
        return parts.size == 4 && 
               parts[0] == QR_PREFIX &&
               parts[1].isNotEmpty() &&  // receiptId
               parts[2].isNotEmpty() &&  // deviceId
               parts[3].length == 8      // hash (8 characters)
    }
    
    /**
     * Extract receipt ID from QR code content
     * 
     * @param qrContent The QR code content string
     * @return Receipt ID or null if invalid format
     */
    fun extractReceiptId(qrContent: String): String? {
        if (!validateQRFormat(qrContent)) return null
        return qrContent.split(QR_SEPARATOR)[1]
    }
    
    /**
     * Extract device ID from QR code content
     * 
     * @param qrContent The QR code content string
     * @return Device ID or null if invalid format
     */
    fun extractDeviceId(qrContent: String): String? {
        if (!validateQRFormat(qrContent)) return null
        return qrContent.split(QR_SEPARATOR)[2]
    }
    
    /**
     * Generate SHA-256 hash for tamper detection
     * 
     * @param input Input string to hash
     * @return First 8 characters of SHA-256 hash
     */
    private fun generateHash(input: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(input.toByteArray())
            val hashHex = hashBytes.joinToString("") { "%02x".format(it) }
            hashHex.take(8) // Use first 8 characters for compact QR codes
        } catch (e: Exception) {
            // Fallback to simple hash if SHA-256 fails
            Math.abs(input.hashCode()).toString().padStart(8, '0').take(8)
        }
    }
}