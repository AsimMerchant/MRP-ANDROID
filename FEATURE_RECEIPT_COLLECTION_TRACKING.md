# Receipt Collection Tracking Feature

## Problem Statement
Currently, there are discrepancies between:
- **Biller's digital reports** (receipts generated in app)  
- **Collector's manual counts** (physical receipts collected)

This creates accountability issues and makes it difficult to track missing or uncollected receipts.

## Solution Overview
Implement **offline-first QR code scanning** system with **local network synchronization**:
1. Each printed receipt includes a globally unique QR code
2. All devices sync receipt data via local Wi-Fi network (no internet required)
3. Collectors use any device to scan QR codes and validate against synced database
4. Collection status syncs back to all devices in real-time
5. Complete reconciliation possible from any device with aggregated network data

## Workflow Enhancement

### Current Workflow
```
Biller → Creates Receipt → Volunteer → Customer → Collector → Manual Count
                ↓                                      ↓
        Digital Report                          Physical Count
                ↓                                      ↓
                    ❌ DISCREPANCY POSSIBLE ❌
```

### Enhanced Multi-Device Workflow
```
Biller Device A ────→ Creates Receipt + QR ────→ Local Network Sync
                              ↓                        ↓
                      Volunteer → Customer       All Devices Updated
                              ↓                        ↓
Collector Device B ←──── Scans QR Code ←────── Validates Against Synced DB
                              ↓                        ↓
                    Collection Status ────→ Syncs Back to All Devices
                              ↓                        ↓
                    ✅ REAL-TIME NETWORK-WIDE RECONCILIATION ✅
```

## Key Architectural Requirements

### Offline-First Design
- **Full offline operation** - No internet dependency
- **Local data persistence** - Complete SQLite database on each device
- **Network-optional sync** - Works offline, syncs when network available
- **Conflict resolution** - Handle sync conflicts gracefully

### Local Network Communication
- **Device Discovery** - Leverage existing DeviceDiscoveryHelper.kt
- **Data Synchronization** - Receipt and collection status sync
- **Real-time Updates** - Instant propagation of collection events
- **Network Resilience** - Handle device connect/disconnect

## Technical Implementation Plan

### Phase 1: Multi-Device Database Schema (Todo #1)
- **Receipt Entity Updates**:
  ```kotlin
  @Entity
  data class Receipt(
      @PrimaryKey val id: String, // Global UUID across all devices
      val receiptNumber: String,
      val biller: String,
      val volunteer: String,
      val amount: Double,
      val date: String,
      val time: String,
      val qrCode: String, // NEW: Globally unique QR identifier
      val isCollected: Boolean = false, // NEW: Collection status
      val deviceId: String, // NEW: Device that created this receipt
      val syncStatus: String = "SYNCED", // NEW: SYNCED, PENDING, CONFLICT
      val lastModified: Long, // NEW: For conflict resolution
      val version: Int = 1 // NEW: For optimistic concurrency
  )
  ```

- **New CollectedReceipt Entity**:
  ```kotlin
  @Entity
  data class CollectedReceipt(
      @PrimaryKey val id: String, // Global UUID
      val receiptId: String, // Foreign key to Receipt
      val collectorName: String,
      val collectionTime: String,
      val collectionDate: String,
      val scannedBy: String, // Device identifier that scanned
      val collectorDeviceId: String, // NEW: Device where collection happened
      val syncStatus: String = "PENDING", // NEW: Sync status
      val lastModified: Long // NEW: Timestamp for sync ordering
  )
  ```

- **New Collector Entity**:
  ```kotlin
  @Entity  
  data class Collector(
      @PrimaryKey val id: String, // Global UUID
      val name: String,
      val deviceId: String, // NEW: Associated device
      val isActive: Boolean = true,
      val syncStatus: String = "SYNCED"
  )
  ```

- **New DeviceSyncLog Entity**:
  ```kotlin
  @Entity
  data class DeviceSyncLog(
      @PrimaryKey val id: String,
      val deviceId: String,
      val lastSyncTime: Long,
      val syncType: String, // RECEIPT, COLLECTION, FULL
      val recordCount: Int,
      val status: String // SUCCESS, FAILED, PARTIAL
  )
  ```

### Phase 2: Local Network Sync Enhancement (Todo #2)
- **Leverage Existing**: Extend current DeviceDiscoveryHelper.kt
- **Sync Protocol**: JSON-based receipt/collection data exchange
- **Network Types**: Wi-Fi Direct, Local Wi-Fi, Bluetooth fallback
- **Sync Strategy**: Real-time push + periodic full sync
- **Conflict Resolution**: Last-write-wins with timestamp ordering

### Phase 3: Cross-Device QR Generation (Todo #3)
- **Dependencies**: Add ZXing library for QR generation
- **Global Unique IDs**: UUID4 + device prefix for uniqueness
- **QR Content**: `MRP_${globalReceiptId}_${deviceId}_${hash}`
- **Validation**: Cryptographic hash for tamper detection
- **Integration**: Update receipt printing and preview with sync-ready QR codes

### Phase 4: Network-Aware Scanning (Todo #4-5)
- **Permissions**: Camera access + network discovery
- **Library**: ML Kit Barcode Scanning API
- **Validation**: Check QR against local + synced receipt database
- **Offline Support**: Cache validation data for offline operation
- **UI**: Show network sync status during scanning

### Phase 5: Multi-Device Collection Tracking (Todo #6)
- **Local Recording**: Store collection events immediately
- **Network Broadcast**: Push collection status to all devices
- **Duplicate Prevention**: Cross-device duplicate scan detection
- **Offline Queue**: Queue sync operations when network unavailable
- **Audit Trail**: Complete multi-device collection history

### Phase 6: Network-Wide Reconciliation (Todo #7)
- **Data Aggregation**: Combine receipts from all network devices
- **Multi-Device Reports**: Show per-device and network totals
- **Missing Receipt Detection**: Cross-reference all device databases
- **Sync Status Tracking**: Show which devices are up-to-date
- **Network Health**: Monitor connectivity and sync performance

### Phase 7: Multi-Device User Management (Todo #8)
- **Device Roles**: Configure device as Biller, Collector, or Both
- **Network Discovery**: Auto-discover and connect to other MRP devices
- **Role-Based UI**: Different interfaces for different device types
- **Device Identification**: Unique device names and capabilities
- **Connection Management**: Handle device join/leave scenarios

## Expected Benefits

1. **Accountability**: Digital proof of receipt collection
2. **Accuracy**: Eliminate manual counting errors  
3. **Real-time Tracking**: Instant visibility into collection status
4. **Discrepancy Detection**: Immediate identification of missing receipts
5. **Audit Trail**: Complete history of all collection activities
6. **Efficiency**: Faster reconciliation process at day end

## Technical Dependencies

- **QR Code Generation**: ZXing Android library
- **Barcode Scanning**: Google ML Kit Barcode Scanning
- **Camera Access**: Android Camera2 API
- **Database**: Room database migrations
- **Permissions**: Camera, Storage (for QR code images)

## Success Criteria

- [ ] QR codes generated for all new receipts
- [ ] Collectors can successfully scan and record receipt collection
- [ ] Reconciliation reports show accurate generated vs collected data  
- [ ] System prevents duplicate scanning
- [ ] Missing receipt identification works correctly
- [ ] Performance remains smooth with large receipt volumes

---
*Feature Branch: feature/share_reports*  
*Created: September 29, 2025*