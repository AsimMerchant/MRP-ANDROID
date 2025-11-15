# Feature: Periodic Auto-Sync

## Overview
Implement automatic periodic synchronization between devices at configurable intervals, eliminating the need for manual sync button presses.

**Status**: Planned (separate from Collection Code System)  
**Target Version**: 1.5.0  
**Estimated Effort**: ~230 lines, 90 minutes  
**Priority**: Enhancement (after Collection Code System v1.4.6)

---

## Current Behavior
- Sync is **manual only** - user must press sync button
- Calls `DeviceDiscoveryHelper.startDiscovery()` and `syncWithAllDevices()`
- No background or scheduled sync capability
- Works well but requires user intervention

---

## Proposed Solution

### 1. WorkManager Implementation
Use Android's WorkManager for reliable background sync:

```kotlin
// Add to app/build.gradle.kts
dependencies {
    implementation("androidx.work:work-runtime-ktx:2.9.0")
}
```

### 2. SyncWorker Class (~100 lines)

```kotlin
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        return try {
            val discoveryHelper = DeviceDiscoveryHelper(applicationContext)
            
            // Start discovery
            discoveryHelper.startDiscovery()
            
            // Wait for devices to be discovered
            delay(5000) // 5 second discovery window
            
            // Sync with all discovered devices
            discoveryHelper.syncWithAllDevices()
            
            // Wait for sync to complete
            delay(2000)
            
            discoveryHelper.stopDiscovery()
            
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}
```

### 3. AutoSyncSettings Helper (~50 lines)

```kotlin
object AutoSyncSettings {
    private const val PREFS_NAME = "auto_sync_prefs"
    private const val KEY_ENABLED = "auto_sync_enabled"
    private const val KEY_INTERVAL_MINUTES = "sync_interval_minutes"
    
    fun isEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENABLED, false)
    }
    
    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, enabled)
            .apply()
        
        if (enabled) {
            scheduleAutoSync(context)
        } else {
            cancelAutoSync(context)
        }
    }
    
    fun getIntervalMinutes(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_INTERVAL_MINUTES, 30) // Default 30 minutes
    }
    
    fun setIntervalMinutes(context: Context, minutes: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_INTERVAL_MINUTES, minutes)
            .apply()
        
        if (isEnabled(context)) {
            scheduleAutoSync(context)
        }
    }
    
    fun scheduleAutoSync(context: Context) {
        val intervalMinutes = getIntervalMinutes(context)
        
        val workRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            intervalMinutes.toLong(), TimeUnit.MINUTES,
            5, TimeUnit.MINUTES // Flex interval for battery optimization
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "auto_sync",
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
    }
    
    fun cancelAutoSync(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork("auto_sync")
    }
}
```

### 4. Settings UI (~80 lines)

Add to existing Settings screen in MainActivity.kt:

```kotlin
@Composable
fun AutoSyncSettingsSection(context: Context) {
    var autoSyncEnabled by remember {
        mutableStateOf(AutoSyncSettings.isEnabled(context))
    }
    var syncInterval by remember {
        mutableStateOf(AutoSyncSettings.getIntervalMinutes(context))
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Automatic Sync",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Auto-sync toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Enable Auto-Sync",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Automatically sync with other devices",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            
            Switch(
                checked = autoSyncEnabled,
                onCheckedChange = { enabled ->
                    autoSyncEnabled = enabled
                    AutoSyncSettings.setEnabled(context, enabled)
                }
            )
        }
        
        // Sync interval slider (only visible when enabled)
        AnimatedVisibility(visible = autoSyncEnabled) {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                Text(
                    text = "Sync Interval: $syncInterval minutes",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Slider(
                    value = syncInterval.toFloat(),
                    onValueChange = { value ->
                        syncInterval = value.toInt()
                    },
                    onValueChangeFinished = {
                        AutoSyncSettings.setIntervalMinutes(context, syncInterval)
                    },
                    valueRange = 15f..120f,
                    steps = 6, // 15, 30, 45, 60, 75, 90, 105, 120
                    modifier = Modifier.fillMaxWidth()
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "15 min",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "120 min",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                
                Text(
                    text = "Battery impact increases with shorter intervals",
                    fontSize = 11.sp,
                    color = Color(0xFFFF9800),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
    
    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
}
```

---

## Technical Details

### WorkManager Benefits
- **Battery-efficient**: Android schedules work optimally
- **Persistent**: Survives app restarts and device reboots
- **Constraint-aware**: Only syncs when network available
- **Retry logic**: Handles temporary failures gracefully

### Sync Intervals
- **Minimum**: 15 minutes (Android's minimum for periodic work)
- **Maximum**: 120 minutes (2 hours)
- **Steps**: 15, 30, 45, 60, 75, 90, 105, 120 minutes
- **Default**: 30 minutes (good balance of freshness vs. battery)

### Network Constraints
- Requires **active network connection** (WiFi or mobile)
- WorkManager automatically waits for network if unavailable
- No sync attempts when offline (prevents battery drain)

### Discovery Window
- **5 second wait** after starting discovery
- Allows mDNS/NSD to find nearby devices
- Same timeout used in manual sync
- Can be adjusted based on network environment

---

## Implementation Steps

1. **Add WorkManager dependency** to `app/build.gradle.kts`
2. **Create SyncWorker.kt** in `app/src/main/java/com/example/mrp/`
3. **Create AutoSyncSettings.kt** helper object
4. **Add settings UI** to existing Settings screen in MainActivity.kt
5. **Test scenarios**:
   - Enable/disable auto-sync
   - Change intervals and verify rescheduling
   - Verify sync happens in background
   - Test with no network (should wait gracefully)
   - Test with no devices discovered (should complete without error)
   - Verify battery usage acceptable

---

## User Experience

### Settings Screen Flow
1. User navigates to Settings
2. Sees "Automatic Sync" section
3. Toggles "Enable Auto-Sync" switch
4. Slider appears to set interval (15-120 minutes)
5. App schedules background sync using WorkManager

### Background Behavior
- App syncs automatically at configured interval
- User sees sync status update in UI when app is open
- No user intervention required
- Battery-friendly scheduling by Android

### Manual Sync Still Available
- Manual sync button remains functional
- Useful for immediate sync needs
- Does not interfere with scheduled sync

---

## Database & Sync Compatibility

✅ **No changes required** to:
- `DeviceDiscoveryHelper.kt` (reuses existing sync logic)
- Database schema (Room entities unchanged)
- Sync protocol (TCP/IP JSON format intact)
- Network discovery (mDNS/NSD unchanged)

✅ **Fully compatible** with:
- Collection Code System (v1.4.6)
- Multi-device sync
- Existing manual sync
- All current database queries

---

## Testing Checklist

- [ ] Enable auto-sync and verify WorkManager task scheduled
- [ ] Disable auto-sync and verify WorkManager task cancelled
- [ ] Change interval and verify task rescheduled
- [ ] Wait for sync interval and verify sync executes
- [ ] Turn off WiFi and verify sync waits for network
- [ ] Verify sync completes when devices discovered
- [ ] Verify sync completes gracefully when no devices found
- [ ] Check battery usage over 24 hours
- [ ] Verify manual sync still works independently
- [ ] Test app restart - verify scheduled sync persists

---

## Future Enhancements (Out of Scope)

- **Sync logs**: Show history of automatic syncs
- **Adaptive intervals**: Adjust based on data change frequency
- **WiFi-only option**: Prevent mobile data usage
- **Charging-only sync**: Minimize battery impact
- **Last sync timestamp**: Display in settings
- **Sync notifications**: Optional alerts when sync completes

---

## Version Planning

- **v1.4.6**: Collection Code System (primary feature)
- **v1.5.0**: Periodic Auto-Sync (this feature)
- **Future**: Enhanced sync options and logs

---

## Notes

- This feature is **independent** and can be implemented any time after v1.4.6
- No breaking changes or migrations required
- Reuses 100% of existing sync infrastructure
- User-requested feature: "can we somehow make the sync happen periodically and set the period in the settings?"
- Addresses production use case where manual sync is inconvenient
