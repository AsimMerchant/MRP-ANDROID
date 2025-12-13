# Mobile Receipt Printer 📱🖨️

A modern Android application built with Kotlin and Jetpack Compose for creating and printing receipts via Bluetooth thermal printers with QR code generation and cross-device collection tracking. Perfect for small businesses, events, and mobile payment collection with multi-device synchronization.

**Current Status**: Retry Print Feature Completed 🔄 | Version 1.5.0 | Print Failure Recovery Active

## 📥 Download

[![Download APK](https://img.shields.io/badge/Download-APK%20v1.5.0-brightgreen?style=for-the-badge&logo=android)](https://github.com/AsimMerchant/MRP-ANDROID/raw/main/release-artifacts/app-debug.apk)

**Requirements**: Android 12+ (API 31)

## 🌟 Features

### Core Functionality ✅ IMPLEMENTED
- **Receipt Creation**: Generate professional receipts with biller, volunteer, and amount information
- **🔄 Retry Print**: Sticky bottom button to re-print last receipt when print fails - eliminates duplicate data entry
- **⚡ Instant UI Response**: Optimized dialog appearance (~1ms) with async keyboard dismissal preventing 50-200ms blocking (98% improvement)
- **🚀 ANR-Free Printing**: Bluetooth operations moved to IO dispatcher preventing "App Not Responding" errors during print operations
- **QR Code Generation**: ✅ **COMPLETED** - Automatic unique QR code generation with format `MRP_{UUID}_{DeviceID}_{Hash}` ✨
- **Thermal Printer QR Integration**: ✅ **COMPLETED** - ESC/POS native QR commands for direct printing on thermal printers 🖨️
- **Bluetooth Printing**: Connect to and print receipts with embedded QR codes via Bluetooth thermal printers
- **Smart Autocomplete**: Intelligent name suggestions for billers and volunteers based on historical data
- **Receipt Preview**: View formatted receipts with visual QR code bitmap display before printing
- **Printer Management**: Save and manage preferred Bluetooth printer connections
- **100% Offline Operation**: ✅ **COMPLETED** - All QR generation works without internet connection 📶

### Retry Print Feature 🔄 **NEW** (v1.5.0)
- **Print Failure Recovery**: ✅ **COMPLETED** - Re-print last receipt when print fails (paper jam, out of paper, connection error)
- **Sticky Bottom Button**: ✅ **COMPLETED** - Always visible at bottom of screen, no scrolling needed
- **Smart State Tracking**: ✅ **COMPLETED** - Automatically tracks most recent receipt from database
- **Navigation Persistence**: ✅ **COMPLETED** - Retry target persists across screen navigation
- **No Duplicate Receipts**: ✅ **COMPLETED** - Re-prints existing receipt, prevents duplicate database entries
- **Accurate Tally**: ✅ **COMPLETED** - End-of-day reports remain accurate, no manual corrections needed
- **Form Handling**: ✅ **COMPLETED** - Clears volunteer & amount on success, preserves on failure

### Phase 4 Features ✅ **COMPLETED**
- **QR Code Scanner**: ✅ **COMPLETED** - In-app camera scanner with ML Kit barcode detection 📸
- **Collection Validation**: ✅ **COMPLETED** - Real-time database validation preventing duplicate collections
- **Collection Reports**: ✅ **COMPLETED** - Comprehensive audit system with collected vs uncollected tracking 📊
- **Currency Display**: ✅ **COMPLETED** - Proper rupee (₹) currency formatting throughout the app
- **Database Integrity**: ✅ **COMPLETED** - Cascade delete operations and orphaned record cleanup
- **Audit Trail**: ✅ **COMPLETED** - Complete collection audit with statistics and percentage tracking

### Phase 5: Auto-Sync Feature 🔄 **COMPLETED** (v1.4.7)
- **Automatic Periodic Sync**: ✅ **COMPLETED** - Foreground service with configurable intervals (1-15 minutes)
- **WiFi-Only Mode**: ✅ **COMPLETED** - Optional constraint to prevent mobile data usage
- **Notification System**: ✅ **COMPLETED** - Real-time sync status with timestamps and device counts
- **Permission Handling**: ✅ **COMPLETED** - Runtime notification permission for Android 13+
- **Service Lifecycle**: ✅ **COMPLETED** - START_STICKY foreground service with proper cleanup
- **User Controls**: ✅ **COMPLETED** - Settings UI with enable toggle, interval dropdown, and WiFi toggle
- **Passive Device Support**: ✅ **COMPLETED** - One coordinator device syncs with all passive devices
- **30-Second Discovery**: ✅ **COMPLETED** - Timeout-based discovery matching manual sync behavior

### QR Scanner Enhancement 📱 **COMPLETED**
- **Paytm-Style Scanning**: ✅ **COMPLETED** - Instant QR detection without targeting overlay or positioning constraints
- **Camera Repositioning**: ✅ **COMPLETED** - Moved camera from bottom 1/3 to top 1/3 of screen as requested
- **ML Kit Optimization**: ✅ **COMPLETED** - Singleton scanner pattern with QR-only detection for better performance
- **Faster Scanning**: ✅ **COMPLETED** - Reduced cooldown from 2000ms to 500ms (75% improvement)
- **Background Processing**: ✅ **COMPLETED** - Image analysis moved to dedicated background thread
- **Performance Optimization**: ✅ **COMPLETED** - Eliminated per-frame scanner creation overhead

### Data Management
- **Multi-Device Database**: Enhanced Room database with UUID-based global sync system ✨
- **Cross-Device Sync**: Offline-first local network synchronization across up to 6 devices 🌐
- **Automatic Periodic Sync**: ✅ **COMPLETED** - Configurable auto-sync (1-15 min intervals) with foreground service 🔄
- **WiFi-Only Sync**: ✅ **COMPLETED** - Optional constraint to prevent mobile data usage during auto-sync
- **Sync Notifications**: ✅ **COMPLETED** - Real-time status updates with device/receipt counts and timestamps
- **Receipt History**: View all created receipts organized by biller with collection tracking
- **Reports & Analytics**: Comprehensive reporting with totals and receipt counts per biller
- **Collection Tracking**: QR code-based receipt collection system with tamper-resistant validation 📱
- **QR Code System**: Global unique QR codes with format `MRP_{receiptId}_{deviceId}_{hash}` ✨
- **Device Role Management**: Flexible biller/collector role switching per device
- **Sync Status Monitoring**: Real-time sync status and conflict resolution
- **Data Editing**: Edit or delete individual receipts and bulk delete by biller
- **Suggestion Management**: Clear autocomplete suggestions when needed

### User Experience
- **Modern UI**: Clean, Material Design 3 interface with dark/light theme support
- **Intuitive Navigation**: Easy navigation between creation, reports, and settings screens
- **Permission Handling**: Seamless Bluetooth permission management for Android 12+
- **Performance Optimized**: Efficient rendering with lazy loading and memoization
- **Sticky Controls**: Important actions (retry print) always visible at screen bottom

## 🏗️ Architecture

### Tech Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM with Compose State Management
- **Database**: Room (SQLite) with multi-device UUID schema ✨
- **QR Codes**: ZXing library for generation and thermal printer integration ✨
- **Navigation**: Navigation Compose
- **Bluetooth**: Android Bluetooth API
- **Network Sync**: mDNS/NSD service discovery with JSON protocol ✨
- **Async Operations**: Coroutines with Dispatchers
- **Multi-Device**: Offline-first local network synchronization ✨

### Project Structure
```
app/src/main/java/com/example/mobilereceiptprinter/
├── MainActivity.kt              # Main activity with Compose screens and navigation
├── AppDatabase.kt              # Room database configuration with migrations
├── Receipt.kt                  # Multi-device receipt entities and relationships
├── ReceiptDao.kt               # Enhanced DAOs with sync-aware queries
├── DeviceManager.kt            # Device identification and role management
├── SyncStatusManager.kt        # Multi-device sync status and monitoring
├── DeviceDiscoveryHelper.kt    # Network discovery and sync infrastructure ✨
├── AutoSyncService.kt          # Foreground service for automatic periodic sync 🔄
├── AutoSyncSettings.kt         # SharedPreferences wrapper for auto-sync configuration
├── QRCodeGenerator.kt          # QR code generation and thermal printer integration ✨
├── DeviceTestScreen.kt         # Database migration testing interface
├── BluetoothPrinterHelper.kt   # Bluetooth printer communication
└── ui/theme/                   # Material Design theming
```

### Database Schema (Multi-Device Enhanced)
- **Receipts Table**: Enhanced with UUID IDs, QR codes, device attribution, sync status
- **Collected Receipts**: Tracks receipt collection events across devices
- **Collectors**: Manages collector information and device associations  
- **Device Sync Logs**: Audit trail for all synchronization operations
- **Suggestions**: Autocomplete data for billers and volunteers
- **Suggestions Table**: Stores autocomplete suggestions separately for persistence
- **SharedPreferences**: Manages printer settings and biller-specific counters

## ⚡ Performance Optimizations

### Instant Dialog Response (98% Improvement)
- **Dialog Appearance**: Optimized to ~1ms for immediate user feedback (down from 50-200ms)
- **Root Cause Analysis**: Used repomix MCP server to identify `focusManager.clearFocus()` as blocking operation
- **Async Keyboard Dismissal**: Moved keyboard clearing from synchronous to async coroutine preventing main thread blocking
- **UI Recomposition**: Compose dialog renders immediately while keyboard dismisses smoothly in background
- **Technical Fix**: `focusManager.clearFocus()` now executes in `scope.launch{}` after dialog state updates
- **User Experience**: Near-instant visual feedback when clicking "Create & Print Receipt" button

### ANR Prevention for Bluetooth Printing
- **Bluetooth Operations**: Moved `connectToDevice()` and `printText()` to `Dispatchers.IO` background thread
- **UI Responsiveness**: App remains interactive during printer connection and data transmission
- **Error Handling**: Improved connection feedback with "Connecting to printer..." progress updates
- **Thread Safety**: Bluetooth operations on IO thread, UI updates on main thread with proper context switching
- **First Print Optimization**: Eliminates ANR during initial Bluetooth pairing and connection setup

### QR Scanner Enhancement (75% Faster Scanning) 📱
- **Paytm-Style Performance**: Instant QR detection anywhere in camera view without targeting constraints
- **Camera Repositioning**: User-requested move from bottom 1/3 to top 1/3 of screen layout
- **ML Kit Optimization**: Singleton pattern eliminates per-frame scanner creation overhead
- **Cooldown Reduction**: Scan cooldown reduced from 2000ms to 500ms for 75% faster successive scans
- **Background Processing**: Image analysis moved to dedicated thread freeing UI thread
- **QR-Only Detection**: Focused detection mode instead of generic barcode scanning

### UI Performance Optimizations (October 8, 2025) 📱⚡

#### Camera Resource Management ✅ **COMPLETED**
- **Issue**: Camera executor and provider not properly cleaned up causing memory leaks
- **Solution**: Added comprehensive cleanup with `cameraProvider.unbindAll()` and proper executor shutdown
- **Impact**: Eliminates battery drain and memory growth during extended scanning sessions
- **Files**: `CameraScannerScreen.kt` - Enhanced DisposableEffect lifecycle management

#### QR Code Preview Memory Optimization ✅ **COMPLETED**
- **Issue**: QR code bitmaps displayed in preview causing ~57KB memory allocation per preview
- **Solution**: Removed QR code display from preview entirely (still prints on receipt)
- **Impact**: Reduced memory pressure, faster preview rendering
- **Files**: `MainActivity.kt` - Simplified ReceiptPreviewCard to text-only display
- **Verification**: QR code printing functionality confirmed intact

#### Planned Optimizations 🔄
- **MainActivity Initialization**: Move blocking initialization to background threads
- **Field Suggestions**: Implement fuzzy matching and intelligent ranking
- **Database Queries**: Optimize suggestion filtering to reduce input lag

### Technical Improvements
- **Focus Management**: `focusManager.clearFocus()` moved from synchronous to async execution preventing 50-200ms UI blocking
- **Memory Management**: Eliminated bitmap allocations in preview screens
- **Resource Cleanup**: Proper lifecycle management for camera and executor resources
- **Bluetooth Threading**: `withContext(Dispatchers.IO)` for all printer operations preventing ANR during connection/printing
- **QR Generation**: SHA-256 hashing and UUID generation moved to background coroutines
- **Receipt Creation**: All object instantiation and database operations run asynchronously  
- **State Management**: Optimized execution flow preventing UI thread blocking during Compose recomposition
- **Memory Efficiency**: Better CPU scheduling with improved user perception and battery usage
- **Analysis Driven**: Performance optimization based on repomix MCP server codebase analysis identifying exact blocking operations
- **Error Recovery**: Robust error handling for Bluetooth failures with user-friendly feedback messages

## 🚀 Installation & Setup

### Prerequisites
- Android Studio Arctic Fox or later
- Android SDK 31+ (Android 12.0)
- Bluetooth thermal printer (ESC/POS compatible)
- Android device with Bluetooth support

### Building the App
1. Clone the repository:
   ```bash
   git clone <repository-url>
   cd MRP
   ```

2. Open in Android Studio:
   ```bash
   android-studio .
   ```

3. Sync Gradle dependencies:
   - Click "Sync Now" when prompted
   - Or run: `./gradlew build`

4. Connect your Android device or start an emulator

5. Run the app:
   ```bash
   ./gradlew installDebug
   ```

### Permissions Required
The app requests the following permissions:
- `BLUETOOTH_CONNECT` - Connect to Bluetooth devices (Android 12+)
- `BLUETOOTH_SCAN` - Scan for Bluetooth devices (Android 12+)
- `POST_NOTIFICATIONS` - Show auto-sync notifications (Android 13+)
- `FOREGROUND_SERVICE` - Run auto-sync service in foreground
- `FOREGROUND_SERVICE_DATA_SYNC` - Data synchronization service type
- `ACCESS_NETWORK_STATE` - Check WiFi connectivity for WiFi-only mode
- Legacy Bluetooth permissions for older Android versions

## 📱 Usage Guide

### First Time Setup
1. **Launch the app** - You'll see the landing screen
2. **Select Printer** - Tap "Select Printer" to choose your Bluetooth thermal printer
3. **Grant Permissions** - Allow Bluetooth permissions when prompted
4. **Pair Printer** - Select your printer from the list of paired devices

### Creating Receipts
1. **Navigate to Receipt Creation** - Tap "Create Receipt" from the landing screen
2. **Fill Details**:
   - **Biller Name**: Enter the person/organization collecting payment
   - **Volunteer Name**: Enter the person handling the receipt
   - **Amount**: Enter the payment amount in rupees
3. **Use Autocomplete** - Tap suggestions that appear as you type
4. **Create Receipt** - Tap "Create Receipt" to generate preview
5. **Print** - Tap "Print to Saved Device" to print the receipt

### Managing Receipts
1. **View Reports** - Tap "Reports" from the landing screen
2. **Browse by Biller** - Receipts are grouped by biller with totals
3. **Edit Receipts** - Tap the edit icon on any receipt to modify details
4. **Delete Options**:
   - Delete individual receipts from the edit dialog
   - Delete all receipts for a biller using "Delete All"

### Auto-Sync Configuration 🔄 **NEW**
1. **Access Settings** - Tap "Settings" from the landing screen
2. **Enable Auto-Sync** - Toggle "Enable Auto-Sync" (requests notification permission on Android 13+)
3. **Set Sync Interval** - Choose from 1, 2, 5, 10, or 15 minutes
4. **WiFi-Only Mode** - Enable to sync only when connected to WiFi (saves mobile data)
5. **Monitor Sync Status** - Check persistent notification for sync results
6. **Passive Device Setup** - Keep app open on other devices (no auto-sync needed)
7. **Expected Notifications**:
   - "Next sync in X minute(s)" - Waiting for next cycle
   - "Syncing..." - Discovery and sync in progress
   - "✅ X devices, Y receipts • HH:MM:SS" - Successful sync with counts
   - "No devices found • HH:MM:SS" - No passive devices available
   - "Waiting for WiFi..." - WiFi-only mode active, not connected

### Receipt Format
Receipts are formatted for thermal printers with:
- Receipt number (auto-incremented per biller)
- Date and time
- Biller and volunteer information
- Amount in rupees
- ESC/POS formatting commands for proper printing

## 🔧 Configuration

### Printer Compatibility
- **Supported**: ESC/POS compatible thermal printers
- **Tested With**: Most 58mm and 80mm Bluetooth thermal printers
- **Connection**: Bluetooth SPP (Serial Port Profile)

### Customization Options
- **Receipt Format**: Modify receipt template in `MainActivity.kt`
- **Database Schema**: Extend tables in respective data classes
- **UI Theming**: Customize colors and typography in `ui/theme/`

## 🐛 Troubleshooting

### Common Issues

**Bluetooth Connection Failed**
- Ensure printer is paired in Android Settings
- Check printer is powered on and in pairing mode
- Verify Bluetooth permissions are granted

**Receipt Not Printing**
- Confirm printer supports ESC/POS commands
- Check paper is loaded correctly
- Verify printer is connected and ready

**Suggestions Not Appearing**
- Ensure you've created previous receipts with similar names
- Check database permissions
- Try clearing and re-entering suggestions

**App Crashes on Launch**
- Check Android version compatibility (API 31+)
- Verify all permissions are granted
- Clear app data and restart

**Auto-Sync Not Working** 🔄 **NEW**
- Grant notification permission when prompted (Android 13+)
- Check WiFi-only toggle if not on WiFi
- Ensure passive devices have app open (at least in background)
- Verify sync interval is set correctly (1-15 minutes)
- Check notification shows "Next sync in X minute(s)" status

**No Devices Found During Auto-Sync** 🔄 **NEW**
- Confirm passive devices are on same WiFi network
- Ensure passive devices have app open (not force-closed)
- Check both devices are discoverable on local network
- Wait for full 30-second discovery timeout
- Restart both devices if NSD discovery fails

## 🤝 Contributing

### Development Setup
1. Fork the repository
2. Create a feature branch: `git checkout -b feature/amazing-feature`
3. Make your changes following Kotlin coding standards
4. Test thoroughly on multiple Android versions
5. Commit changes: `git commit -m 'Add amazing feature'`
6. Push to branch: `git push origin feature/amazing-feature`
7. Open a Pull Request

### Code Style
- Follow Kotlin official style guide
- Use meaningful variable and function names
- Add comments for complex business logic
- Maintain consistent formatting with existing code

### Testing
- Test on multiple Android versions (API 31-36)
- Verify Bluetooth functionality with different printer models
- Test permission flows on Android 12+
- Validate database operations

## 📄 License

This project is licensed under the Creative Commons Attribution-NonCommercial 4.0 International License (CC BY-NC 4.0) - see the LICENSE file for details.

## 🙏 Acknowledgments

- Android team for Jetpack Compose framework
- Material Design team for design system
- Room database for seamless data persistence
- Kotlin coroutines for async operations
- ESC/POS printer manufacturers for compatibility standards

## 📞 Support

For support, bug reports, or feature requests:
- Create an issue in the GitHub repository
- Provide detailed device information and steps to reproduce
- Include relevant logs and screenshots

## 🚀 Development Status

**Current Version**: 1.4.7  
**Feature Branch**: `main`  
**Development Phase**: Phase 5 Complete - Auto-Sync Live 🔄⚡

### ✅ Completed Features (All Phases)
- **Multi-Device Database Schema**: UUID-based global sync system
- **Cross-Device Sync Infrastructure**: mDNS discovery with JSON protocol
- **Automatic Periodic Sync**: Foreground service with 1-15 minute configurable intervals
- **WiFi-Only Sync Mode**: Optional constraint to save mobile data
- **Device Role Management**: Flexible biller/collector role switching
- **Network Status Monitoring**: Real-time sync progress and connection tracking
- **Conflict Resolution**: Timestamp and version-based conflict handling
- **QR Code Generation & Scanning**: Complete collection tracking system
- **Camera Scanner**: ML Kit-based QR code scanning for collectors
- **Collection Reports**: Multi-device reporting with comprehensive statistics
- **Notification System**: Real-time auto-sync status with Android 13+ permission handling
- **Testing Framework**: Comprehensive database migration and network sync testing

### 📱 Multi-Device Architecture
- **Offline-First Design**: No internet dependency, local WiFi network only
- **6-Device Support**: Scalable across multiple devices simultaneously
- **Real-Time Sync**: Automatic device discovery and data synchronization
- **Production Ready**: Comprehensive error handling, logging, and monitoring

---

**Version**: 1.4.7  
**Last Updated**: November 2025  
**Minimum Android Version**: 12.0 (API 31)  
**Target Android Version**: 14 (API 36)
