# UI Mockup: QR Code Toggle Feature

## Main Menu Screen - Toggle Location

```
┌─────────────────────────────────────────┐
│  Mobile Receipt Printer                 │
│  Version 1.4.5                          │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│  📝 Create Receipt                      │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│  📊 View Reports                        │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│  🔄 Network Sync                        │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│  📱 QR Scanner                          │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐  ← NEW TOGGLE HERE
│  Print QR Codes              [ON/OFF]   │
│  QR codes will be printed on receipts   │
│  (or: QR codes disabled, still tracked) │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│  📋 Collection Report                   │
└─────────────────────────────────────────┘
```

## Toggle States

### State 1: QR Printing ENABLED (Default)
```
┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
┃  Print QR Codes              [●──ON]   ┃
┃  QR codes will be printed on receipts  ┃
┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
    ↑ Tertiary container color (highlighted)
```

**Behavior**: 
- Thermal receipts include QR code at top
- QR stored in database ✓
- Collection tracking works ✓

### State 2: QR Printing DISABLED
```
┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
┃  Print QR Codes              [──●OFF]  ┃
┃  QR codes disabled (still tracked in   ┃
┃  database)                             ┃
┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
    ↑ Surface variant color (subdued)
```

**Behavior**: 
- Thermal receipts DO NOT include QR code
- QR stored in database ✓
- Collection tracking works ✓

## Receipt Output Comparison

### WITH QR Printing (Toggle ON)

```
┌─────────────────────────────┐
│  [█████████████████]        │  ← QR Code printed
│  [█ MOBILE RECEIPT █]       │
│  [█ PRINTER MRP   █]        │
│  [█████████████████]        │
│                             │
│  =======================    │
│  RECEIPT #42                │
│  Biller: John Doe           │
│  Volunteer: Jane Smith      │
│  Amount: ₹500               │
│  Date: 2025-11-15           │
│  Time: 14:30:45             │
│  =======================    │
│                             │
│  Thank you!                 │
└─────────────────────────────┘
```

### WITHOUT QR Printing (Toggle OFF)

```
┌─────────────────────────────┐
│  =======================    │  ← No QR Code
│  RECEIPT #42                │
│  Biller: John Doe           │
│  Volunteer: Jane Smith      │
│  Amount: ₹500               │
│  Date: 2025-11-15           │
│  Time: 14:30:45             │
│  =======================    │
│                             │
│  Thank you!                 │
└─────────────────────────────┘
```

## User Flow Diagram

```
┌─────────────────┐
│  User opens     │
│  Main Menu      │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  Sees QR Toggle │
│  (default: ON)  │
└────────┬────────┘
         │
         ├─────────► Toggle OFF ────┐
         │                          │
         ▼                          ▼
┌─────────────────┐        ┌─────────────────┐
│  Create Receipt │        │  Print Receipt  │
└────────┬────────┘        └────────┬────────┘
         │                          │
         ▼                          ▼
┌─────────────────┐        ┌─────────────────┐
│  QR Generated   │        │  QR NOT printed │
│  & Saved to DB  │        │  on thermal     │
└────────┬────────┘        └────────┬────────┘
         │                          │
         ▼                          ▼
┌─────────────────┐        ┌─────────────────┐
│  QR Scanner     │        │  Still scannable│
│  can track it   │        │  (from database)│
└─────────────────┘        └─────────────────┘
```

## Code Location Reference

```
MainActivity.kt structure:

Lines 2500-2510: Screen enum definitions
Lines 2750-2850: MainMenuScreen composable
                 ↳ Line ~2830: Add toggle HERE

Lines 2900-2960: ReceiptScreen composable
                 ↳ Line 2904: SharedPreferences access
                 ↳ Line 2960: buildReceiptText function
                 
Lines 3000-3050: Print operations
                 ↳ Lines 3011, 3049: Update calls
```

## Testing Checklist

### Functional Tests
- [ ] Toggle switches between ON/OFF states
- [ ] Toggle state persists after app restart
- [ ] Toggle state persists after device reboot
- [ ] QR prints when toggle is ON
- [ ] QR does NOT print when toggle is OFF
- [ ] QR always saved to database regardless of toggle
- [ ] Collection tracking works with toggle OFF
- [ ] Preview screen shows receipt correctly

### UI/UX Tests
- [ ] Toggle is easy to find in menu
- [ ] Description text is clear
- [ ] Color changes indicate state clearly
- [ ] Switch animation is smooth
- [ ] No layout shifts when toggling

### Edge Cases
- [ ] First app install (default state)
- [ ] Upgrade from previous version
- [ ] Multiple rapid toggles
- [ ] Toggle while printing in progress
- [ ] Toggle with no printer connected

## Implementation Complexity

```
Feature Complexity Breakdown:

┌─────────────────────────────────────┐
│  Component          │  Complexity   │
├─────────────────────┼───────────────┤
│  SharedPreferences  │  ⭐ Low       │
│  UI Toggle          │  ⭐⭐ Medium  │
│  Function Update    │  ⭐ Low       │
│  Call Site Updates  │  ⭐ Low       │
│  Testing           │  ⭐⭐ Medium  │
│  Documentation     │  ⭐ Low       │
├─────────────────────┼───────────────┤
│  OVERALL           │  ⭐⭐ Medium  │
└─────────────────────────────────────┘

Estimated Development Time: 30-45 minutes
Estimated Testing Time: 15-20 minutes
Total: ~1 hour
```
