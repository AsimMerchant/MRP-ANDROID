# Collection Code System - Implementation Plan

**Feature Version**: 1.4.6  
**Feature Branch**: feature/toggle_QRcode  
**Status**: In Progress  
**Started**: November 15, 2025

---

## Task Breakdown

### ✅ Task 1: Version Bump (app/build.gradle.kts)
**Status**: ✅ Completed  
**Estimated Time**: 2 minutes  
**Lines Changed**: 2

**Description**:
- Update `versionCode` from 17 → 18
- Update `versionName` from "1.4.4" → "1.4.6"
- Prerequisite for all other changes
- Follows semantic versioning for new features

**Files**:
- `app/build.gradle.kts`

---

### ✅ Task 2: Collection Code Extraction (QRCodeGenerator.kt)
**Status**: ✅ Completed  
**Estimated Time**: 5 minutes  
**Lines Added**: ~15

**Description**:
- Add `getCollectionCode(qrCode: String, length: Int): String` function
- Extract last N characters from QR hash
- Example: QR="MRP_123_DEV1_A3F7C2B9" with length=4 returns "C2B9"
- Handle edge cases (invalid length, short QR codes)

**Files**:
- `app/src/main/java/com/example/mrp/QRCodeGenerator.kt`

**Implementation**:
```kotlin
fun getCollectionCode(qrCode: String, length: Int): String {
    if (length < 1) return ""
    return qrCode.takeLast(length).uppercase()
}
```

---

### ✅ Task 3: CollectionCodeSettings Object (MainActivity.kt)
**Status**: ✅ Completed  
**Estimated Time**: 10 minutes  
**Lines Added**: ~40

**Description**:
- Create SharedPreferences helper object
- Methods: `getCodeLength()`, `setCodeLength()`, `isPrintQREnabled()`, `setPrintQREnabled()`
- Preference name: "collection_code_prefs"
- Defaults: 4 characters, QR printing OFF

**Files**:
- `app/src/main/java/com/example/mrp/MainActivity.kt`

**Implementation**:
```kotlin
object CollectionCodeSettings {
    private const val PREFS_NAME = "collection_code_prefs"
    private const val KEY_CODE_LENGTH = "code_length"
    private const val KEY_PRINT_QR = "print_qr_enabled"
    
    fun getCodeLength(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_CODE_LENGTH, 4)
    }
    
    fun setCodeLength(context: Context, length: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_CODE_LENGTH, length)
            .apply()
    }
    
    fun isPrintQREnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_PRINT_QR, false)
    }
    
    fun setPrintQREnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_PRINT_QR, enabled)
            .apply()
    }
}
```

---

### ✅ Task 4: Database Query (ReceiptDao.kt)
**Status**: ✅ Completed  
**Estimated Time**: 5 minutes  
**Lines Added**: ~10

**Description**:
- Add `searchByCollectionCode()` query with `substr()` SQL pattern
- Query: `WHERE UPPER(substr(qrCode, -:codeLength)) LIKE UPPER(:searchCode) || '%'`
- Returns `Flow<List<Receipt>>`
- Matches only the collection code portion (last N chars)

**Files**:
- `app/src/main/java/com/example/mrp/ReceiptDao.kt`

**Implementation**:
```kotlin
@Query("""
    SELECT * FROM receipts 
    WHERE UPPER(substr(qrCode, -:codeLength)) LIKE UPPER(:searchCode) || '%'
    ORDER BY timestamp DESC
""")
fun searchByCollectionCode(searchCode: String, codeLength: Int): Flow<List<Receipt>>
```

---

### ✅ Task 5: Receipt Printing Logic (MainActivity.kt)
**Status**: ✅ Completed (Updated)  
**Estimated Time**: 10 minutes  
**Lines Modified**: ~30

**Description**:
- Modify `buildReceiptText()` function
- Always print collection code prominently at top
- Check `CollectionCodeSettings.isPrintQREnabled()` and conditionally generate/print QR code
- Format: "Collection Code: XXXX" in large/bold text

**Files**:
- `app/src/main/java/com/example/mrp/MainActivity.kt`

**Changes**:
1. Extract collection code at start of function
2. Add collection code to receipt text (always)
3. Wrap QR generation in if-check (only if enabled)
4. Update ESC/POS commands accordingly

---

### ✅ Task 6: Create ManualCollectionScreen UI (MainActivity.kt)
**Status**: ✅ Completed  
**Estimated Time**: 30 minutes  
**Lines Added**: ~220

**Description**:
- Build Composable screen with TextField for code entry
- Implement live autocomplete using `searchByCollectionCode()`
- Display matching receipts list with tap to collect
- Include loading states and empty states
- Show receipt details (amount, donor, timestamp)

**Files**:
- `app/src/main/java/com/example/mrp/MainActivity.kt`

**Components**:
- TextField with uppercase input transformation
- LazyColumn for results
- Receipt card items
- Loading indicator
- Empty state messages ("Start typing...", "No matches found")

---

### ✅ Task 7i: Create Settings Screen Foundation (MainActivity.kt)
**Status**: ✅ Completed  
**Estimated Time**: 20 minutes  
**Lines Added**: ~80

**Description**:
Create a new Settings screen from scratch since the app currently has no settings UI. This screen will serve as the foundation for Tasks 7 and 8.

**What to Create**:
1. Add `Settings` to `Screen` sealed class
2. Create `SettingsScreen` Composable function
3. Add navigation route in NavHost
4. Create basic screen layout with Scaffold + TopAppBar
5. Add back navigation button
6. Create "Collection Code Settings" section header
7. Add placeholder content area for slider and toggle
8. Add Settings button to Landing screen (Option A: bottom of menu)

**Files**:
- `app/src/main/java/com/example/mobilereceiptprinter/MainActivity.kt`

**UI Structure - Settings Screen**:
```
TopAppBar("Settings") + Back Button
├── Column with padding
    ├── Section: "Collection Code Settings"
    │   ├── [Task 7: Slider will go here]
    │   └── [Task 8: Toggle will go here]
    └── Future: Other settings sections
```

**UI Structure - Landing Screen Navigation**:
```
Landing Menu:
├── 📄 Create Receipt
├── ⌨️ Manual Collection [Task 10]
├── 📊 View Reports
├── 🔄 Network Sync
├── 📷 QR Scanner
├── 📋 Collection Report
└── ⚙️ Settings [NEW - Option A: bottom position]
```

**Components**:
- Scaffold with TopAppBar
- Section header with divider
- Placeholder for settings controls
- Proper padding and spacing
- Navigation button in Landing screen

---

### ✅ Task 7: Add Settings UI - Code Length (MainActivity.kt)
**Status**: ✅ Completed  
**Estimated Time**: 15 minutes  
**Lines Added**: ~50

**Description**:
- Add to Settings screen: "Collection Code" section
- Slider for code length (4-8 characters)
- Display current value above slider
- Description text explaining the setting
- Calls `CollectionCodeSettings.setCodeLength()` on change

**Files**:
- `app/src/main/java/com/example/mrp/MainActivity.kt`

**UI Elements**:
- Section header: "Collection Code Settings"
- Text: "Code Length: N characters"
- Slider: range 4f..8f, steps = 3
- Description: Impact on collision probability

---

### ✅ Task 8: Add Settings UI - QR Printing Toggle (MainActivity.kt)
**Status**: ✅ Completed  
**Estimated Time**: 10 minutes  
**Lines Added**: ~40

**Description**:
- Add to Settings screen below code length slider
- Switch for "Print QR Code on Receipt"
- Description text explaining the toggle
- Calls `CollectionCodeSettings.setPrintQREnabled()` on change
- Default OFF (collection code only)

**Files**:
- `app/src/main/java/com/example/mrp/MainActivity.kt`

**UI Elements**:
- Switch with label
- Description: "Enable to print QR code on receipts (collection code always printed)"
- Note about thermal printer quality

---

### ✅ Task 9: Add Navigation to Manual Collection (MainActivity.kt)
**Status**: ✅ Completed  
**Estimated Time**: 10 minutes  
**Lines Added**: ~20

**Description**:
- Add "Manual Collection" button/navigation item
- Place in main screen or navigation drawer
- Route to ManualCollectionScreen
- Update navigation graph/when statement
- Add icon (keyboard/text input icon)

**Files**:
- `app/src/main/java/com/example/mrp/MainActivity.kt`

**Changes**:
1. Add navigation button in appropriate location
2. Add screen enum/sealed class entry
3. Update navigation when statement
4. Add back navigation support

---

### ✅ Task 10: Testing & Validation
**Status**: Not Started  
**Estimated Time**: 30 minutes

**Description**:
Comprehensive testing of all features

**Test Cases**:
1. **Code Extraction**: Verify codes extracted correctly from existing receipts
2. **Autocomplete**: Test with 2-3 characters, verify live filtering
3. **QR Toggle**: Turn on/off, verify receipts print correctly
4. **Code Length**: Change from 4-8, verify extraction updates
5. **Manual Collection**: Collect receipt via code entry, verify database update
6. **Sync Compatibility**: Verify sync still works across devices
7. **Collision Handling**: Test similar codes, verify disambiguation works
8. **Edge Cases**: Empty input, invalid codes, already collected receipts
9. **Mixed Methods**: One collector uses QR, another uses code - verify both work
10. **Settings Persistence**: Close app, verify settings saved

---

## Summary

**Total Tasks**: 11 (added Task 7i: Settings Screen Foundation)  
**Estimated Total Time**: 3.5-4.5 hours  
**Files Modified**: 4
- `app/build.gradle.kts` (version bump)
- `app/src/main/java/com/example/mrp/MainActivity.kt` (main implementation)
- `app/src/main/java/com/example/mrp/QRCodeGenerator.kt` (code extraction)
- `app/src/main/java/com/example/mrp/ReceiptDao.kt` (database query)

**Total Lines**: ~430 lines added/modified (updated with Task 7i)

---

## Progress Tracking

**Completed**: 6/11 tasks  
**In Progress**: 0/11 tasks  
**Not Started**: 5/11 tasks  

**Next Task**: Task 7i - Create Settings Screen Foundation

---

## Notes

- All tasks are sequential and build on each other
- Each task can be tested independently
- No database migration required (reuses existing `qrCode` field)
- Network sync completely unaffected
- Backward compatible with existing collection system
- QR scanning and collection code methods fully interoperable

---

## Related Documents

- `FEATURE_COLLECTION_CODE_SYSTEM.md` - Technical specification
- `MOCKUP_COLLECTION_CODE_UI.md` - UI mockups and design
- `FEATURE_PERIODIC_AUTO_SYNC.md` - Future enhancement (v1.5.0)
