# UI Mockups - Collection Code System

---

## 1. Receipt Output

### Thermal Receipt (QR Toggle OFF - Default)
```
┌─────────────────────────────┐
│  ╔══════════════════════╗   │
│  ║   CODE: C3D4         ║   │  ← Large, bold
│  ╚══════════════════════╝   │
│  =======================    │
│  RECEIPT #42                │
│  Biller: John Doe           │
│  Volunteer: Jane Smith      │
│  Amount: ₹500               │
│  Date: 2025-11-15           │
│  =======================    │
└─────────────────────────────┘
```

### Thermal Receipt (QR Toggle ON - Optional)
```
┌─────────────────────────────┐
│  ╔══════════════════════╗   │
│  ║   CODE: C3D4         ║   │
│  ╚══════════════════════╝   │
│                             │
│  [█████████████████]        │
│  [█  QR CODE HERE █]        │  ← Optional
│  [█████████████████]        │
│                             │
│  =======================    │
│  RECEIPT #42                │
│  Biller: John Doe           │
│  Amount: ₹500               │
│  =======================    │
└─────────────────────────────┘
```

---

## 2. Main Menu Screen

```
┌─────────────────────────────────────────┐
│  📱 Mobile Receipt Printer              │
│  Version 1.4.6                          │
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
│  📋 Collect Receipt          NEW!       │  ← Manual entry (primary)
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│  📱 QR Scanner (Optional)               │  ← Only if toggle ON
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│  ⚙️ Settings                            │
└─────────────────────────────────────────┘
```

---

## 3. Collection Screen - Live Autocomplete

### Step 1: User types "C"
```
┌─────────────────────────────────────────┐
│  ← Back        📋 Collect Receipt       │
├─────────────────────────────────────────┤
│                                         │
│  Enter Collection Code                  │
│  ┌───────────────────────────────────┐ │
│  │ C▊                                │ │
│  └───────────────────────────────────┘ │
│  💡 Keep typing to narrow down...      │
│                                         │
│  📝 Suggestions (12 found):             │
│  ┌───────────────────────────────────┐ │
│  │ C3D4 - John Doe · ₹500 · Nov 15  │ │
│  │ C7F2 - Jane Doe · ₹300 · Nov 14  │ │
│  │ CF89 - Ram Kumar · ₹200 · Nov 13 │ │
│  │ C1A5 - Priya Shah · ₹450 · Nov 12│ │
│  │ ... (tap to select)               │ │
│  └───────────────────────────────────┘ │
└─────────────────────────────────────────┘
```

### Step 2: User types "C3"
```
┌─────────────────────────────────────────┐
│  ← Back        📋 Collect Receipt       │
├─────────────────────────────────────────┤
│                                         │
│  Enter Collection Code                  │
│  ┌───────────────────────────────────┐ │
│  │ C3▊                               │ │
│  └───────────────────────────────────┘ │
│  💡 2 matches found                    │
│                                         │
│  📝 Suggestions (2 found):              │
│  ┌───────────────────────────────────┐ │
│  │ C3D4 - John Doe · ₹500 · Nov 15  │ │
│  │ C3A2 - Jane Smith · ₹150 · Nov 10│ │
│  └───────────────────────────────────┘ │
└─────────────────────────────────────────┘
```

### Step 3: User types "C3D4" OR taps suggestion
```
┌─────────────────────────────────────────┐
│  ← Back        📋 Collect Receipt       │
├─────────────────────────────────────────┤
│                                         │
│  Enter Collection Code                  │
│  ┌───────────────────────────────────┐ │
│  │ C3D4 ✓                            │ │
│  └───────────────────────────────────┘ │
│                                         │
│  ✅ Match Found:                        │
│  ┌───────────────────────────────────┐ │
│  │ CODE: C3D4                        │ │
│  │                                   │ │
│  │   Biller: John Doe                │ │
│  │   Amount: ₹500                    │ │
│  │   Date: 2025-11-15                │ │
│  │   Receipt #42                     │ │
│  │                                   │ │
│  │  [Mark as Collected] ─────────►   │ │
│  └───────────────────────────────────┘ │
│                                         │
└─────────────────────────────────────────┘
```

---

## 4. Collection Screen (Multiple Matches)

```
┌─────────────────────────────────────────┐
│  ← Back        📋 Collect Receipt       │
├─────────────────────────────────────────┤
│                                         │
│  Enter Collection Code                  │
│  ┌───────────────────────────────────┐ │
│  │ C3D4                              │ │
│  └───────────────────────────────────┘ │
│                                         │
│  ─────────────────────────────────────  │
│                                         │
│  📝 Found 2 receipts - Select one:      │
│                                         │
│  ┌───────────────────────────────────┐ │
│  │ 1️⃣ CODE: C3D4                     │ │
│  │    John Doe · ₹500 · 2025-11-15  │ │
│  │    [Select] ──────────────►       │ │
│  └───────────────────────────────────┘ │
│                                         │
│  ┌───────────────────────────────────┐ │
│  │ 2️⃣ CODE: C3D4                     │ │
│  │    Jane Doe · ₹300 · 2025-11-14  │ │
│  │    [Select] ──────────────►       │ │
│  └───────────────────────────────────┘ │
│                                         │
└─────────────────────────────────────────┘
```

---

## 5. Settings Screen

```
┌─────────────────────────────────────────┐
│  ← Back              ⚙️ Settings        │
├─────────────────────────────────────────┤
│                                         │
│  ┌───────────────────────────────────┐ │
│  │ Receipt Printing                  │ │
│  │                                   │ │
│  │ Print QR Codes                    │ │
│  │ [────────────────────○] OFF       │ │
│  │ QR codes disabled (still tracked) │ │
│  │                                   │ │
│  │ ───────────────────────────────   │ │
│  │                                   │ │
│  │ Collection Code Length            │ │
│  │ Shorter = easier typing           │ │
│  │ Longer = less collision risk      │ │
│  │                                   │ │
│  │ 4 characters        Fast typing   │ │
│  │ [●────────────────────────] 4     │ │
│  │  4    5    6    7    8           │ │
│  │                                   │ │
│  │ Example code: C3D4                │ │
│  └───────────────────────────────────┘ │
│                                         │
└─────────────────────────────────────────┘
```

---

## 6. Settings Screen (Different Code Length)

```
┌─────────────────────────────────────────┐
│  ⚙️ Settings                            │
├─────────────────────────────────────────┤
│                                         │
│  Collection Code Length                 │
│  6 characters               Safer       │
│  [──────────●──────────────] 6          │
│   4    5    6    7    8                │
│                                         │
│  Example code: B2C3D4                   │
│                                         │
│  ⚠️ This affects newly printed receipts │
│     Existing receipts work with any     │
│     setting                             │
│                                         │
└─────────────────────────────────────────┘
```

---

## 7. Collection Success State

```
┌─────────────────────────────────────────┐
│  📋 Collect Receipt                     │
├─────────────────────────────────────────┤
│                                         │
│            ✅ SUCCESS!                  │
│                                         │
│  ┌───────────────────────────────────┐ │
│  │                                   │ │
│  │   Receipt Collected               │ │
│  │                                   │ │
│  │   CODE: C3D4                      │ │
│  │   Biller: John Doe                │ │
│  │   Amount: ₹500                    │ │
│  │                                   │ │
│  │   Collected: 2025-11-15 14:30     │ │
│  │                                   │ │
│  └───────────────────────────────────┘ │
│                                         │
│  [Collect Another] [Back to Menu]      │
│                                         │
└─────────────────────────────────────────┘
```

---

## 8. Code Length Comparison

```
┌─────────────────────────────────────────┐
│  Code Length Impact:                    │
├─────────────────────────────────────────┤
│                                         │
│  4 chars:  C3D4                         │
│  • 65K combinations                     │
│  • Fastest typing (~3 sec)              │
│  • Best for: < 500 receipts             │
│                                         │
│  5 chars:  B2C3D                        │
│  • 1M combinations                      │
│  • Fast typing (~4 sec)                 │
│  • Best for: < 5K receipts              │
│                                         │
│  6 chars:  A1B2C3                       │
│  • 16M combinations                     │
│  • Medium typing (~5 sec)               │
│  • Best for: < 50K receipts             │
│                                         │
│  8 chars:  A1B2C3D4                     │
│  • 4B combinations                      │
│  • Slower typing (~7 sec)               │
│  • Best for: Enterprise                 │
│                                         │
└─────────────────────────────────────────┘
```

---

## 9. User Journey Flow

```
RECEIPT CREATION:
┌─────────┐    ┌─────────┐    ┌─────────┐
│ Create  │ -> │ Generate│ -> │  Print  │
│ Receipt │    │ QR+Code │    │ CODE    │
└─────────┘    └─────────┘    └─────────┘
                                    │
                                    ▼
                          ┌──────────────────┐
                          │ Receipt printed  │
                          │ with CODE: C3D4  │
                          └──────────────────┘

COLLECTION WITH LIVE AUTOCOMPLETE:
┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐
│ Open    │ -> │ Type "C"│ -> │ Type "C3│ -> │ Select  │
│ Collect │    │ (12 hit)│    │ (2 hits)│    │  or C3D4│
└─────────┘    └─────────┘    └─────────┘    └─────────┘
                                                    │
                                                    ▼
                                               ┌─────────┐
                                               │ Verify  │
                                               │ Details │
                                               └─────────┘
                                                    │
                                                    ▼
                                               ┌─────────┐
                                               │ Confirm │
                                               │ Collect │
                                               └─────────┘

SPEED: 2-3 characters often enough! ⚡
```

---

## 10. Technical Note: Correct SQL Pattern Matching

### ❌ Wrong Approach (matches anywhere):
```sql
WHERE qrCode LIKE '%C3%'
-- Matches: "MRP_abc3_device_..." ✗ False positive!
-- Matches: "MRP_def_device_c3a2..." ✓ Correct
```

### ✅ Correct Approach (matches only collection code):
```sql
WHERE UPPER(substr(qrCode, -4)) LIKE 'C3%'
-- Extracts last 4 chars first: "C3D4"
-- Then checks if starts with "C3": ✓
-- Prevents false matches from middle of QR string
```

**Example**:
```
QR: "MRP_abc_device_a1b2c3d4"
substr(qrCode, -4) → "c3d4"
UPPER(...) → "C3D4"
LIKE 'C3%' → MATCH ✓

QR: "MRP_c3f_device_789c1234"  
substr(qrCode, -4) → "1234"
LIKE 'C3%' → NO MATCH ✗ (correct!)
```

---

## Color Coding

- **Primary actions**: Blue cards/buttons
- **Success states**: Green with ✅
- **Settings**: Neutral gray cards
- **Emphasis**: Bold text for codes
- **Hints**: Light gray italic text
- **Live suggestions**: Subtle background highlight
