# Test Plan - Collection Code System v1.4.6

**Feature**: Collection Code System  
**Version**: 1.4.6 (versionCode 18)  
**Feature Branch**: feature/toggle_QRcode  
**Test Date**: November 15, 2025  
**Status**: Ready for Testing

---

## Test Environment Requirements

### Hardware
- Android device (API 31+)
- Bluetooth thermal printer (ESC/POS compatible)
- Multiple Android devices (for sync testing)

### Software
- MRP App v1.4.6 installed
- Bluetooth permissions granted
- Printer paired and connected

### Test Data
- Minimum 5 existing receipts with QR codes
- Receipts should have varying amounts and billers
- At least 2 receipts with similar collection codes (for collision testing)

---

## Test Cases

### Test Case 1: Collection Code Extraction
**Objective**: Verify collection codes are correctly extracted from QR codes

**Prerequisites**: 
- App installed and running
- Settings screen accessible

**Steps**:
1. Navigate to Settings screen
2. Note current code length setting (default: 4)
3. Create a new receipt with QR code
4. Observe the collection code printed on receipt

**Expected Results**:
- Collection code is extracted from last N characters of QR hash
- Code is displayed in uppercase
- Code length matches setting (4-8 characters)
- Example: QR="MRP_123_DEV1_A3F7C2B9" → Code="C2B9" (length 4)

**Pass/Fail**: ___________

---

### Test Case 2: Receipt Printing - Collection Code Only (Default)
**Objective**: Verify receipts print with collection code only when QR is disabled

**Prerequisites**:
- QR printing toggle is OFF (default)
- Printer connected

**Steps**:
1. Navigate to Settings
2. Verify "Print QR Code on Receipt" toggle is OFF
3. Return to Create Receipt screen
4. Create and print a new receipt

**Expected Results**:
- Receipt prints successfully
- Collection code appears centered at top in large text
- NO QR code graphic printed
- Receipt format:
  ```
         C2B9
  
  =======================
   RECEIPT #123
  =======================
  Date: 2025-11-15
  ...
  ```

**Pass/Fail**: ___________

---

### Test Case 3: Receipt Printing - With QR Code
**Objective**: Verify receipts print with both collection code and QR when enabled

**Prerequisites**:
- QR printing toggle is OFF initially
- Printer connected

**Steps**:
1. Navigate to Settings
2. Toggle "Print QR Code on Receipt" to ON
3. Return to Create Receipt screen
4. Create and print a new receipt
5. Return to Settings
6. Toggle QR printing back to OFF (reset to default)

**Expected Results**:
- Receipt prints successfully
- Collection code appears centered at top
- QR code graphic appears below collection code
- Receipt format:
  ```
         C2B9
  
  [QR CODE GRAPHIC]
  
  =======================
   RECEIPT #123
  =======================
  ...
  ```

**Pass/Fail**: ___________

---

### Test Case 4: Manual Collection - Search with Autocomplete
**Objective**: Verify manual collection search works with live autocomplete

**Prerequisites**:
- At least 5 receipts with QR codes exist in database
- Manual Collection screen accessible

**Steps**:
1. Navigate to Landing screen
2. Tap "⌨️ Manual Collection" button
3. In search field, type first 2 characters of a known collection code
4. Observe autocomplete results
5. Type 3rd character
6. Observe results update
7. Type complete code

**Expected Results**:
- Before typing: "Enter at least 2 characters to search" message
- After 2 characters: Matching receipts appear
- Results update in real-time as typing continues
- Each receipt card shows:
  - Receipt number
  - Collection code (highlighted)
  - Biller name
  - Amount
  - Date/time
  - Checkmark icon if already collected

**Pass/Fail**: ___________

---

### Test Case 5: Manual Collection - Collect Receipt
**Objective**: Verify receipt can be collected via manual code entry

**Prerequisites**:
- Uncollected receipt with known collection code exists
- Manual Collection screen open

**Steps**:
1. Type collection code in search field
2. Tap on the matching receipt card
3. Confirm collection in dialog
4. Observe status update

**Expected Results**:
- Confirmation dialog appears with receipt details
- After confirming:
  - "Receipt collected successfully" message appears
  - Search field clears
  - Receipt disappears from search results (or shows as collected)
- Database updated: `isCollected = true`

**Pass/Fail**: ___________

---

### Test Case 6: Manual Collection - Already Collected Receipt
**Objective**: Verify already collected receipts are visually distinguished

**Prerequisites**:
- At least one collected receipt exists
- Manual Collection screen open

**Steps**:
1. Search for a collection code of an already collected receipt
2. Observe the receipt card appearance
3. Try tapping the card

**Expected Results**:
- Receipt card has different background color (surface variant)
- Green checkmark icon visible
- Card is not clickable / shows "Already collected" state

**Pass/Fail**: ___________

---

### Test Case 7: Settings - Code Length Adjustment
**Objective**: Verify code length can be adjusted and affects extraction

**Prerequisites**:
- Settings screen accessible
- At least 2 receipts exist

**Steps**:
1. Navigate to Settings
2. Note current code length (default: 4)
3. Move slider to 6 characters
4. Note the label updates to "Code Length: 6 characters"
5. Navigate to Manual Collection
6. Observe collection codes displayed
7. Return to Settings and change to 8 characters
8. Return to Manual Collection
9. Observe codes now show 8 characters
10. Reset to 4 characters

**Expected Results**:
- Slider moves smoothly between 4-8
- Label updates immediately as slider moves
- Setting persists after leaving Settings screen
- Collection codes in Manual Collection screen reflect current setting
- Previously printed receipts still searchable with new length

**Pass/Fail**: ___________

---

### Test Case 8: Settings - QR Toggle Persistence
**Objective**: Verify QR toggle setting persists across app restarts

**Prerequisites**:
- Settings screen accessible
- Printer connected

**Steps**:
1. Navigate to Settings
2. Toggle "Print QR Code on Receipt" to ON
3. Close app completely (force stop)
4. Reopen app
5. Navigate to Settings
6. Observe toggle state
7. Create and print a receipt
8. Toggle to OFF
9. Close and reopen app
10. Verify toggle is still OFF

**Expected Results**:
- Toggle state persists after app restart
- Receipts print according to saved setting
- Default state is OFF for fresh installs

**Pass/Fail**: ___________

---

### Test Case 9: Navigation Flow
**Objective**: Verify all navigation routes work correctly

**Prerequisites**:
- App freshly opened

**Steps**:
1. From Landing, tap "⌨️ Manual Collection"
   - Verify ManualCollectionScreen opens
2. Tap back button (←)
   - Verify returns to Landing
3. From Landing, tap "⚙️ Settings"
   - Verify SettingsScreen opens
4. Tap back button (←)
   - Verify returns to Landing
5. From Landing, tap "Create Receipt"
   - Verify ReceiptScreen opens

**Expected Results**:
- All navigation transitions smooth
- Back buttons work correctly
- No crashes or frozen screens
- Screen titles display correctly

**Pass/Fail**: ___________

---

### Test Case 10: Collision Handling
**Objective**: Verify system handles receipts with similar collection codes

**Prerequisites**:
- Create 2 receipts where last 4 characters of hash are similar
- Example: Codes "A3F7" and "A3F9"

**Steps**:
1. Navigate to Manual Collection
2. Type "A3F"
3. Observe results

**Expected Results**:
- Both receipts appear in results
- Each clearly distinguished by:
  - Receipt number
  - Biller name
  - Amount
  - Full collection code visible
- User can select correct receipt
- No duplicate collection codes

**Pass/Fail**: ___________

---

### Test Case 11: Multi-Device Sync Compatibility
**Objective**: Verify collection code system doesn't break existing sync

**Prerequisites**:
- 2 devices with MRP app v1.4.6
- Both on same network
- Network sync enabled

**Steps**:
1. Device A: Create a receipt
2. Device A: Trigger sync
3. Device B: Receive synced receipt
4. Device B: Collect receipt via Manual Collection
5. Device B: Trigger sync
6. Device A: Verify collection status synced
7. Device A: Try to collect same receipt

**Expected Results**:
- Receipts sync between devices as before
- QR codes sync correctly
- Collection status syncs
- Device A shows receipt as already collected
- No data corruption or sync errors

**Pass/Fail**: ___________

---

### Test Case 12: Empty States
**Objective**: Verify proper handling of empty/error states

**Prerequisites**:
- Manual Collection screen accessible

**Steps**:
1. Manual Collection with no receipts in database
2. Search for non-existent code
3. Type only 1 character
4. Clear search field after typing

**Expected Results**:
- No receipts: "No receipts found" message
- Non-existent code: "No receipts found" 
- Less than 2 chars: "Enter at least 2 characters to search"
- Clear field: Returns to initial state
- No crashes or blank screens

**Pass/Fail**: ___________

---

### Test Case 13: Code Length Impact on Search
**Objective**: Verify changing code length doesn't break existing searches

**Prerequisites**:
- 3 receipts exist with codes extracted at length 4
- Settings screen accessible

**Steps**:
1. Manual Collection: Note codes at length 4 (e.g., "C2B9")
2. Settings: Change code length to 6
3. Manual Collection: Search using original 4-char code "C2B9"
4. Settings: Change to length 8
5. Manual Collection: Search again

**Expected Results**:
- Search still finds receipts regardless of length setting
- Query uses substr() to match last N characters
- No receipts lost due to length change
- All receipts remain searchable

**Pass/Fail**: ___________

---

### Test Case 14: Performance - Large Dataset
**Objective**: Verify system performs well with many receipts

**Prerequisites**:
- Database with 100+ receipts (or maximum available)
- Manual Collection screen

**Steps**:
1. Navigate to Manual Collection
2. Type 2 characters that match many receipts
3. Observe response time
4. Scroll through results
5. Type additional characters to narrow results

**Expected Results**:
- Search results appear within 1 second
- Scrolling is smooth
- Real-time filtering responsive
- No lag or frame drops
- App remains stable

**Pass/Fail**: ___________

---

### Test Case 15: Edge Cases
**Objective**: Test boundary conditions and unusual inputs

**Prerequisites**:
- Manual Collection screen accessible

**Steps**:
1. Search with special characters (!@#$%)
2. Search with lowercase letters
3. Search with spaces
4. Search with very long string (20+ chars)
5. Rapid typing and deleting
6. Toggle QR setting multiple times rapidly
7. Move code length slider rapidly

**Expected Results**:
- Special chars: No crash, graceful handling
- Lowercase: Converted to uppercase automatically
- Spaces: Ignored or handled gracefully
- Long string: Truncated or handled safely
- Rapid input: UI remains responsive
- Rapid toggles: Settings save correctly
- No crashes or data corruption

**Pass/Fail**: ___________

---

## Test Summary

**Total Test Cases**: 15  
**Passed**: ___ / 15  
**Failed**: ___ / 15  
**Blocked**: ___ / 15  

---

## Defects Found

| ID | Test Case | Severity | Description | Status |
|----|-----------|----------|-------------|--------|
| 1  |           |          |             |        |
| 2  |           |          |             |        |
| 3  |           |          |             |        |

---

## Regression Testing

### Areas to Verify
- [ ] Existing QR scanner still works
- [ ] Receipt creation unchanged
- [ ] Reports screen unaffected
- [ ] Network sync still functional
- [ ] Bluetooth printing works as before
- [ ] Name suggestions still work
- [ ] All existing features operational

---

## Sign-Off

**Tester Name**: ___________________________  
**Test Date**: ___________________________  
**Overall Result**: ⬜ PASS  ⬜ FAIL  ⬜ PARTIAL  

**Notes**:
```




```

**Approved for Production**: ⬜ YES  ⬜ NO  

**Approver**: ___________________________  
**Date**: ___________________________
