package com.example.mobilereceiptprinter

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages auto-sync configuration settings using SharedPreferences
 * 
 * Settings:
 * - Auto-sync enabled/disabled
 * - Sync interval in minutes
 * - WiFi-only sync constraint
 */
object AutoSyncSettings {
    
    private const val PREFS_NAME = "auto_sync_settings"
    private const val KEY_ENABLED = "auto_sync_enabled"
    private const val KEY_INTERVAL_MINUTES = "sync_interval_minutes"
    private const val KEY_WIFI_ONLY = "wifi_only_sync"
    
    /**
     * Check if auto-sync is enabled
     * @return true if enabled, false by default
     */
    fun isAutoSyncEnabled(context: Context): Boolean {
        return getPreferences(context).getBoolean(KEY_ENABLED, false)
    }
    
    /**
     * Enable or disable auto-sync
     * @param enabled true to enable, false to disable
     */
    fun setAutoSyncEnabled(context: Context, enabled: Boolean) {
        getPreferences(context).edit()
            .putBoolean(KEY_ENABLED, enabled)
            .apply()
    }
    
    /**
     * Get sync interval in minutes
     * @return interval in minutes, default is 1 minute
     */
    fun getSyncIntervalMinutes(context: Context): Int {
        return getPreferences(context).getInt(KEY_INTERVAL_MINUTES, 1)
    }
    
    /**
     * Set sync interval in minutes
     * @param minutes interval in minutes (typically 1, 2, 5, 10, or 15)
     */
    fun setSyncIntervalMinutes(context: Context, minutes: Int) {
        getPreferences(context).edit()
            .putInt(KEY_INTERVAL_MINUTES, minutes)
            .apply()
    }
    
    /**
     * Check if WiFi-only sync is enabled
     * @return true if WiFi-only, true by default
     */
    fun isWifiOnlySync(context: Context): Boolean {
        return getPreferences(context).getBoolean(KEY_WIFI_ONLY, true)
    }
    
    /**
     * Enable or disable WiFi-only sync
     * @param wifiOnly true for WiFi-only, false to allow any network
     */
    fun setWifiOnlySync(context: Context, wifiOnly: Boolean) {
        getPreferences(context).edit()
            .putBoolean(KEY_WIFI_ONLY, wifiOnly)
            .apply()
    }
    
    /**
     * Get SharedPreferences instance
     */
    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
