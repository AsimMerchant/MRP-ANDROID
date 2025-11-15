# Feature Analysis: QR Code Toggle for Receipt Printing

**Branch**: `feature/toggle_QRcode`  
**Feature**: Allow users to enable/disable QR code printing on receipts  
**Date**: November 15, 2025

---

## Executive Summary

This feature will add a user preference to toggle QR code printing on/off for thermal receipts. The QR code will still be generated and stored in the database (for collection tracking), but the thermal printer output can exclude it based on user preference.

---

## Current QR Code Implementation Analysis

### 1. **QR Code Generation** (`QRCodeGenerator.kt`)
- **Purpose**: Generates unique QR codes for receipt tracking
- **Format**: `MRP_{receiptId}_{deviceId}_{hash}`
- **Key Functions**:
  - `generateQRContent()` - Creates QR string
  - `generateQRBitmap()` - Creates visual QR for UI preview
  - `generateThermalPrinterQR()` - ESC/POS commands for printing
  
### 2. **Current Printing Flow** (`MainActivity.kt`)
- **Location**: Lines 2960-2980 (ReceiptScreen composable)
- **Function**: `buildReceiptText(date, time, qrCode)`
- **Current Behavior**: 
  ```kotlin
  ${if (qrCode.isNotEmpty()) {
      QRCodeGenerator.generateThermalPrinterQR(qrCode) + "\n"
  } else {
      ""
  }}
  ```
- QR is conditionally included if `qrCode` parameter is not empty
- Called at lines 3011 and 3049 with `currentQRCode` parameter

### 3. **Receipt Creation Flow**
- **Line 3113-3136**: QR code is ALWAYS generated during receipt creation
- **Line 3121**: `QRCodeGenerator.generateQRContent()` creates unique QR
- **Line 3136**: QR is stored in database Receipt entity
- **Purpose**: Essential for Phase 4 collection tracking system

### 4. **Storage Locations**
- **SharedPreferences Used**:
  - `printer_prefs` - Stores printer address/name (line 2623, 2904)
  - `biller_{name}` - Stores biller-specific receipt counts (line 2944, 2951)
  - `device_prefs` - Stores device ID (line 2298)

---

## Proposed Implementation Plan

### Phase 1: Add User Preference Setting ✅

#### 1.1 Create Settings Storage
**File**: `MainActivity.kt`  
**Location**: Near existing SharedPreferences usage (around line 2904)

**Add constant**:
```kotlin
private const val PREF_PRINT_QR_CODE = "print_qr_code_enabled"
```

**Add preference access functions**:
```kotlin
fun isQRPrintingEnabled(context: Context): Boolean {
    val prefs = context.getSharedPreferences("printer_prefs", Context.MODE_PRIVATE)
    return prefs.getBoolean(PREF_PRINT_QR_CODE, true) // Default: enabled
}

fun setQRPrintingEnabled(context: Context, enabled: Boolean) {
    val prefs = context.getSharedPreferences("printer_prefs", Context.MODE_PRIVATE)
    prefs.edit().putBoolean(PREF_PRINT_QR_CODE, enabled).apply()
}
```

#### 1.2 Add UI Toggle in Main Menu
**File**: `MainActivity.kt`  
**Location**: MainMenuScreen function (around line 2758-2847)  
**Position**: Add toggle button after "QR Scanner" button (line 2829)

**New UI Element**:
```kotlin
// QR Code Print Toggle (after Scanner button)
item {
    var isQRPrintEnabled by remember { 
        mutableStateOf(isQRPrintingEnabled(context)) 
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isQRPrintEnabled) 
                MaterialTheme.colorScheme.tertiaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Print QR Codes",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = if (isQRPrintEnabled) 
                        "QR codes will be printed on receipts" 
                    else 
                        "QR codes disabled (still tracked in database)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = isQRPrintEnabled,
                onCheckedChange = { enabled ->
                    isQRPrintEnabled = enabled
                    setQRPrintingEnabled(context, enabled)
                }
            )
        }
    }
}
```

### Phase 2: Modify Printing Logic ✅

#### 2.1 Update buildReceiptText Function
**File**: `MainActivity.kt`  
**Location**: Line 2960  
**Current signature**: `fun buildReceiptText(date: String, time: String, qrCode: String = "")`

**Change to**:
```kotlin
fun buildReceiptText(
    date: String, 
    time: String, 
    qrCode: String = "", 
    printQR: Boolean = true
) = """
${if (qrCode.isNotEmpty() && printQR) {
    QRCodeGenerator.generateThermalPrinterQR(qrCode) + "\n"
} else {
    ""
}}=======================
...
"""
```

#### 2.2 Update Print Call Sites
**File**: `MainActivity.kt`  
**Locations**: Lines 3011 and 3049

**Change from**:
```kotlin
val receiptText = buildReceiptText(printDate, printTime, currentQRCode)
```

**Change to**:
```kotlin
val printQREnabled = isQRPrintingEnabled(context)
val receiptText = buildReceiptText(printDate, printTime, currentQRCode, printQREnabled)
```

### Phase 3: Testing & Documentation ✅

#### 3.1 Test Scenarios
1. **Toggle ON → Print Receipt** → QR code appears on thermal print
2. **Toggle OFF → Print Receipt** → QR code does NOT appear on thermal print
3. **Toggle OFF → Create Receipt** → QR code still saved to database
4. **Toggle OFF → Scan QR with Scanner** → Collection tracking still works
5. **Toggle State Persistence** → Toggle survives app restart

#### 3.2 Documentation Updates
- Update `CHANGELOG.md` with new feature
- Update `README.md` with toggle instructions
- Update version number in `app/build.gradle.kts`

---

## Files to Modify

| File | Lines | Changes | Complexity |
|------|-------|---------|------------|
| `MainActivity.kt` | ~2904 | Add preference functions | Low |
| `MainActivity.kt` | ~2830 | Add toggle UI in menu | Medium |
| `MainActivity.kt` | 2960 | Update buildReceiptText signature | Low |
| `MainActivity.kt` | 3011, 3049 | Pass printQR parameter | Low |
| `CHANGELOG.md` | Top | Document new feature | Low |
| `app/build.gradle.kts` | versionCode/Name | Increment version | Low |

**Total Lines Changed**: ~150 lines  
**New Files**: 0  
**Complexity**: Low-Medium

---

## Important Design Decisions

### ✅ What We WILL Do:
1. **Keep QR generation in database** - Essential for collection tracking
2. **Only toggle thermal printing** - Doesn't affect app functionality
3. **Default to ON** - Backward compatible with existing behavior
4. **Store in printer_prefs** - Logical grouping with print settings
5. **Simple toggle UI** - Easy to find and understand

### ❌ What We WON'T Do:
1. **Remove QR from database** - Would break collection tracking
2. **Disable QR scanner** - Independent feature, always available
3. **Hide QR in preview** - User should see what's in database
4. **Complex settings screen** - Keep it simple with inline toggle

---

## Version Update Plan

**Current Version**: 1.4.4 (versionCode 17)  
**New Version**: 1.4.5

**Update in** `app/build.gradle.kts`:
```kotlin
versionCode = 18  // Increment by 1
versionName = "1.4.5"  // Patch version bump
```

---

## Risks & Mitigation

| Risk | Impact | Mitigation |
|------|--------|------------|
| Users disable QR, can't track collections | Medium | Clear UI text explaining QR still tracked |
| Breaking printer compatibility | Low | QR is optional, text printing unchanged |
| State not persisting | Low | Use proven SharedPreferences pattern |
| Confusion about toggle purpose | Medium | Add descriptive subtitle text in UI |

---

## Success Criteria

✅ User can toggle QR printing ON/OFF  
✅ Toggle state persists across app restarts  
✅ QR codes always saved to database regardless of toggle  
✅ Thermal prints respect toggle setting  
✅ Collection tracking works with toggle OFF  
✅ No crashes or printer errors  
✅ Clear UI/UX with helpful descriptions

---

## Next Steps

**Awaiting your approval to proceed with implementation.**

When approved, implementation order will be:
1. Add preference storage functions
2. Add UI toggle to main menu
3. Modify buildReceiptText function
4. Update call sites
5. Test all scenarios
6. Update version and documentation

**Estimated Time**: 30-45 minutes of development + testing
