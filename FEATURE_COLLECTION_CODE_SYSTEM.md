# Collection Code System - Implementation Plan

**Branch**: `feature/toggle_QRcode`  
**Feature**: Manual collection entry with flexible code length + Optional QR  
**Version**: 1.4.5 → 1.4.6

---

## Overview

Replace unreliable QR code printing with manual entry system using short collection codes extracted from QR hash. QR scanning remains optional.

---

## System Design

### Collection Code
- **Source**: Extract last N characters from existing QR hash
- **Format**: Uppercase alphanumeric (e.g., "C3D4", "B2C3D")
- **Length**: User-configurable (4-8 characters)
- **Default**: 4 characters

### Example
```
QR Content: MRP_abc123_device-id_a1b2c3d4
                                    ↓
Collection Code: C3D4 (last 4 chars)
```

---

## Key Features

1. **Flexible Code Length Setting**
   - Users choose 4-8 characters via slider
   - Trade-off: Shorter = easier typing, Longer = less collision
   - Stored in SharedPreferences

2. **Dual Collection Methods**
   - Primary: Manual entry (always available)
   - Optional: QR scanner (toggle OFF by default)

3. **No Database Changes**
   - Use existing `qrCode` field
   - No migration required
   - Backward compatible

---

## Implementation Details

### 1. Settings Configuration
**File**: `MainActivity.kt`

```kotlin
object CollectionCodeSettings {
    const val KEY_CODE_LENGTH = "collection_code_length"
    const val DEFAULT_CODE_LENGTH = 4
    
    fun getCodeLength(context: Context): Int {
        val prefs = context.getSharedPreferences("printer_prefs", Context.MODE_PRIVATE)
        return prefs.getInt(KEY_CODE_LENGTH, DEFAULT_CODE_LENGTH).coerceIn(4, 8)
    }
    
    fun setCodeLength(context: Context, length: Int) {
        val prefs = context.getSharedPreferences("printer_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_CODE_LENGTH, length.coerceIn(4, 8)).apply()
    }
}
```

### 2. Code Extraction
**File**: `QRCodeGenerator.kt`

```kotlin
fun getCollectionCode(qrContent: String, length: Int = 4): String {
    val hash = qrContent.substringAfterLast("_")
    return hash.takeLast(length.coerceIn(4, 8)).uppercase()
}
```

### 3. Receipt Printing
**File**: `MainActivity.kt` - Update `buildReceiptText()`

```kotlin
val collectionCode = QRCodeGenerator.getCollectionCode(qrCode, codeLength)

"""
==========================
    CODE: $collectionCode
==========================
${if (printQR) "[QR CODE]" else ""}
RECEIPT #$receiptNumber
...
"""
```

### 4. Manual Collection Screen
**New File**: Replace `CameraScannerScreen.kt` functionality

```kotlin
@Composable
fun ManualCollectionScreen() {
    var searchCode by remember { mutableStateOf("") }
    var matchingReceipts by remember { mutableStateOf(listOf<Receipt>()) }
    val codeLength = CollectionCodeSettings.getCodeLength(context)
    
    // Real-time search as user types each character
    LaunchedEffect(searchCode) {
        if (searchCode.isNotEmpty()) {
            matchingReceipts = receiptDao.searchByCollectionCode(
                searchCode.uppercase(), 
                codeLength
            )
        } else {
            matchingReceipts = emptyList()
        }
    }
    
    Column {
        // Input field with auto-uppercase, limited to code length
        TextField(
            value = searchCode,
            onValueChange = { searchCode = it.uppercase().take(codeLength) },
            label = { Text("Enter ${codeLength}-character code") }
        )
        
        // Show live suggestions as user types
        LazyColumn {
            items(matchingReceipts) { receipt ->
                CollectionSuggestion(
                    receipt = receipt,
                    onClick = { /* Mark as collected */ }
                )
            }
        }
    }
}
```

### 5. Database Query (Critical: Correct Pattern Matching)
**File**: `ReceiptDao.kt`

```kotlin
@Query("""
    SELECT * FROM receipts 
    WHERE isCollected = 0 
    AND UPPER(substr(qrCode, -:codeLength)) LIKE UPPER(:searchCode) || '%'
    ORDER BY date DESC, time DESC
""")
suspend fun searchByCollectionCode(searchCode: String, codeLength: Int): List<Receipt>
```

**Why this query?**
- `substr(qrCode, -:codeLength)` extracts the LAST N characters (the collection code)
- `UPPER(...)` ensures case-insensitive matching
- `LIKE :searchCode || '%'` matches codes that START with user's input
- Prevents false matches from middle of QR string (e.g., "c" in "device" or "abc")

**Example**:
```
QR: "MRP_abc_device_a1b2c3d4"
substr(qrCode, -4) = "c3d4"
User types "C3" → matches "C3D4" ✓

QR: "MRP_c3f_device_789c1234"  
substr(qrCode, -4) = "1234"
User types "C3" → no match ✗ (correct!)
```

### 6. Settings UI
**File**: `MainActivity.kt` - Add to MainMenuScreen

```kotlin
// Settings Card
Card {
    Column {
        // Print QR Toggle (existing)
        Row {
            Text("Print QR Codes")
            Switch(checked = printQR, onCheckedChange = {...})
        }
        
        Divider()
        
        // NEW: Code Length Setting
        Text("Collection Code Length")
        Slider(
            value = codeLength.toFloat(),
            onValueChange = { CollectionCodeSettings.setCodeLength(context, it.toInt()) },
            valueRange = 4f..8f,
            steps = 3
        )
        Text("${codeLength} characters - Example: ${getExampleCode(codeLength)}")
    }
}
```

---

## Files to Modify

| File | Changes | Lines |
|------|---------|-------|
| `MainActivity.kt` | Add CollectionCodeSettings, Settings UI | ~100 |
| `MainActivity.kt` | Add ManualCollectionScreen with live search | ~300 |
| `MainActivity.kt` | Update buildReceiptText | ~20 |
| `QRCodeGenerator.kt` | Add getCollectionCode() | ~15 |
| `ReceiptDao.kt` | Add searchByCollectionCode() with substr | ~10 |
| `CHANGELOG.md` | Document changes | ~30 |
| `app/build.gradle.kts` | Update version | ~2 |

**Total**: ~420 lines  
**Effort**: 3-4 hours

---

## User Flow

### Receipt Creation
1. Create receipt → QR generated automatically
2. Collection code extracted from QR hash
3. Print receipt with large, bold CODE: C3D4

### Collection Process (with Live Autocomplete)
1. Collector receives receipt with code "C3D4"
2. Opens Collection screen
3. Types "C" → sees all codes starting with C
4. Types "C3" → list narrows to codes starting with C3
5. Types "C3D4" or taps suggestion → sees full receipt details
6. Confirms collection
7. Receipt marked as collected

**Speed**: Often only 2-3 characters needed to find receipt!

### If QR Toggle is ON (Optional)
- QR code also printed
- Scanner tab available
- Can use either method

---

## Collision Handling

| Code Length | Combinations | Strategy |
|-------------|--------------|----------|
| 4 chars | 65,536 | Show all matches, user selects |
| 5 chars | 1,048,576 | Rare collision |
| 6+ chars | 16M+ | Virtually no collision |

If multiple receipts found, display list with biller/amount/date for disambiguation.

---

## Benefits

✅ No database migration  
✅ Works with existing receipts  
✅ Reliable thermal printing  
✅ **Live autocomplete** - suggestions as you type  
✅ Fast collection (2-3 characters often enough)  
✅ Scalable (adjustable code length)  
✅ QR remains optional  
✅ Backward compatible  
✅ Prevents false matches (exact substring extraction)  

---

## Version Update

**Current**: 1.4.4 (versionCode 17)  
**New**: 1.4.6 (versionCode 18)

---

## Testing Checklist

- [ ] Code extraction works (4-8 char lengths)
- [ ] Settings slider updates code length
- [ ] Receipt prints with correct code length
- [ ] **Live autocomplete** shows suggestions as typing
- [ ] **Correct pattern matching** (no false matches from middle of QR)
- [ ] Single character search returns valid results
- [ ] Multi-character narrows down results correctly
- [ ] Collision handling works (multiple matches)
- [ ] QR toggle still works
- [ ] Existing receipts searchable
- [ ] Code persistence across app restart

---

## Next Steps

**Awaiting approval to implement.**
