package com.example.mobilereceiptprinter

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import java.util.UUID

/**
 * Manages device identification and roles for multi-device sync
 */
class DeviceManager(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("device_prefs", Context.MODE_PRIVATE)
    
    enum class DeviceRole {
        BILLER, COLLECTOR, BOTH
    }
    
    companion object {
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_DEVICE_NAME = "device_name" 
        private const val KEY_DEVICE_ROLE = "device_role"
        private const val KEY_IS_SYNC_ENABLED = "is_sync_enabled"
        
        const val ROLE_BILLER = "BILLER"
        const val ROLE_COLLECTOR = "COLLECTOR"
        const val ROLE_BOTH = "BOTH"
    }
    
    /**
     * Get unique device identifier
     * Creates one if it doesn't exist
     */
    fun getDeviceId(): String {
        var deviceId = prefs.getString(KEY_DEVICE_ID, null)
        
        // Force regenerate device ID if it contains underscore (old format)
        if (deviceId == null || deviceId.contains("_")) {
            // Generate unique device ID combining Android ID and UUID
            // Note: Use hyphen separator to avoid conflicts with QR underscore separators
            val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            deviceId = "${androidId}-${UUID.randomUUID().toString().substring(0, 8)}"
            prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply()

        }
        return deviceId
    }
    
    /**
     * Get human-readable device name
     */
    fun getDeviceName(): String {
        return prefs.getString(KEY_DEVICE_NAME, null) 
            ?: android.os.Build.MODEL.let { model ->
                val name = "MRP Device ${getDeviceId().takeLast(8)}"
                setDeviceName(name)
                name
            }
    }
    
    /**
     * Set device name
     */
    fun setDeviceName(name: String) {
        prefs.edit().putString(KEY_DEVICE_NAME, name).apply()
    }
    
    /**
     * Get current device role
     */
    fun getDeviceRole(): String {
        return prefs.getString(KEY_DEVICE_ROLE, ROLE_BOTH) ?: ROLE_BOTH
    }
    
    /**
     * Set device role
     */
    fun setDeviceRole(role: String) {
        prefs.edit().putString(KEY_DEVICE_ROLE, role).apply()
    }
    
    /**
     * Check if device can create receipts (biller functionality)
     */
    fun canCreateReceipts(): Boolean {
        val role = getDeviceRole()
        return role == ROLE_BILLER || role == ROLE_BOTH
    }
    
    /**
     * Check if device can scan receipts (collector functionality)
     */
    fun canScanReceipts(): Boolean {
        val role = getDeviceRole()
        return role == ROLE_COLLECTOR || role == ROLE_BOTH
    }
    
    /**
     * Check if sync is enabled
     */
    fun isSyncEnabled(): Boolean {
        return prefs.getBoolean(KEY_IS_SYNC_ENABLED, true)
    }
    
    /**
     * Enable/disable sync
     */
    fun setSyncEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_IS_SYNC_ENABLED, enabled).apply()
    }
    
    /**
     * Get device info for network discovery
     */
    fun getDeviceInfo(): DeviceInfo {
        return DeviceInfo(
            deviceId = getDeviceId(),
            deviceName = getDeviceName(),
            role = getDeviceRole(),
            canCreateReceipts = canCreateReceipts(),
            canScanReceipts = canScanReceipts(),
            isSyncEnabled = isSyncEnabled(),
            lastActiveTime = System.currentTimeMillis()
        )
    }
}

/**
 * Device information for network sharing
 */
data class DeviceInfo(
    val deviceId: String,
    val deviceName: String,
    val role: String,
    val canCreateReceipts: Boolean,
    val canScanReceipts: Boolean,
    val isSyncEnabled: Boolean,
    val lastActiveTime: Long
)