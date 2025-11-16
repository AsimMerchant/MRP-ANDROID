package com.example.mobilereceiptprinter

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Foreground service for automatic periodic device synchronization
 * 
 * Features:
 * - Runs as foreground service with persistent notification
 * - Periodic sync at user-configured intervals (1-15 minutes)
 * - WiFi-only constraint (optional)
 * - Updates notification with sync status
 */
class AutoSyncService : Service() {
    
    companion object {
        private const val TAG = "AutoSyncService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "auto_sync_channel"
    }
    
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var syncRunnable: Runnable
    private var syncIntervalMs = 60000L // Default: 1 minute
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private var deviceDiscoveryHelper: DeviceDiscoveryHelper? = null
    private lateinit var notificationManager: NotificationManager
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "AutoSyncService created")
        
        try {
            // Initialize dependencies
            val deviceManager = DeviceManager(this)
            val database = AppDatabase.getDatabase(this)
            val syncStatusManager = SyncStatusManager(database, deviceManager)
            
            deviceDiscoveryHelper = DeviceDiscoveryHelper(this, deviceManager, syncStatusManager)
            deviceDiscoveryHelper?.initialize()
            
            notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            Log.d(TAG, "Dependencies initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize service dependencies: ${e.message}", e)
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Get sync interval from intent (in milliseconds)
        syncIntervalMs = intent?.getLongExtra("interval_ms", 60000L) ?: 60000L
        
        val intervalMinutes = syncIntervalMs / 60000L
        Log.d(TAG, "AutoSyncService started with interval: $intervalMinutes minute(s)")
        
        // Start as foreground service with notification
        val notification = createNotification("Starting auto-sync...")
        startForeground(NOTIFICATION_ID, notification)
        
        // Start periodic sync
        startPeriodicSync()
        
        return START_STICKY // Restart if killed by system
    }
    
    private fun startPeriodicSync() {
        syncRunnable = Runnable {
            performSync()
            // Schedule next sync
            handler.postDelayed(syncRunnable, syncIntervalMs)
        }
        // Start first sync after initial delay
        handler.postDelayed(syncRunnable, syncIntervalMs)
        
        val intervalMinutes = syncIntervalMs / 60000L
        updateNotification("Next sync in $intervalMinutes minute(s)", false)
    }
    
    private fun performSync() {
        // Check WiFi constraint if enabled
        if (AutoSyncSettings.isWifiOnlySync(this) && !isConnectedToWifi()) {
            Log.d(TAG, "Skipping sync: WiFi not connected")
            updateNotification("Waiting for WiFi...", false)
            return
        }
        
        if (deviceDiscoveryHelper == null) {
            Log.e(TAG, "DeviceDiscoveryHelper not initialized")
            updateNotification("❌ Service not ready", true)
            return
        }
        
        scope.launch {
            try {
                Log.d(TAG, "Starting sync cycle...")
                updateNotification("Syncing...", false)
                
                // Perform discovery and sync (30-second timeout)
                val result = deviceDiscoveryHelper!!.discoverAndSync(30000L)
                
                // Update notification with result
                val message = if (result.success) {
                    if (result.devicesSync > 0) {
                        "✅ ${result.devicesSync} device${if (result.devicesSync > 1) "s" else ""}, ${result.receiptsSync} receipt${if (result.receiptsSync != 1) "s" else ""}"
                    } else {
                        "No devices found"
                    }
                } else {
                    "❌ Sync failed"
                }
                
                Log.d(TAG, "Sync completed: $message")
                updateNotification(message, true)
                
            } catch (e: Exception) {
                Log.e(TAG, "Sync error: ${e.message}", e)
                updateNotification("❌ Error: ${e.message}", true)
            }
        }
    }
    
    private fun createNotification(message: String): android.app.Notification {
        val intervalMinutes = syncIntervalMs / 60000L
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_rotate)
            .setContentTitle("MRP Auto-Sync ($intervalMinutes min)")
            .setContentText(message)
            .setOngoing(true) // Cannot be dismissed
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }
    
    private fun updateNotification(message: String, showTimestamp: Boolean) {
        val intervalMinutes = syncIntervalMs / 60000L
        val timestampText = if (showTimestamp) {
            " • ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())}"
        } else {
            ""
        }
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_rotate)
            .setContentTitle("MRP Auto-Sync ($intervalMinutes min)")
            .setContentText(message + timestampText)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    /**
     * Check if device is connected to WiFi
     */
    private fun isConnectedToWifi(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "AutoSyncService destroyed")
        
        // Stop periodic sync
        handler.removeCallbacks(syncRunnable)
        
        // Cancel coroutines
        scope.cancel()
        
        // Cleanup discovery helper
        deviceDiscoveryHelper?.cleanup()
        deviceDiscoveryHelper = null
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
}
