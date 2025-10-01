# Mobile Receipt Printer 📱🖨️

A modern Android application built with Kotlin and Jetpack Compose for creating and printing receipts via Bluetooth thermal printers with QR code generation and cross-device collection tracking. Perfect for small businesses, events, and mobile payment collection with multi-device synchronization.

**Current Status**: Phase 4 Complete ✅ | Performance Optimized ⚡ | Production Ready 🚀 | Version 1.4.1

## 🌟 Features

### Core Functionality ✅ IMPLEMENTED
- **Receipt Creation**: Generate professional receipts with biller, volunteer, and amount information
- **⚡ Instant UI Response**: Optimized dialog appearance time (<16ms) for immediate user feedback
- **QR Code Generation**: ✅ **COMPLETED** - Automatic unique QR code generation with format `MRP_{UUID}_{DeviceID}_{Hash}` ✨
- **Thermal Printer QR Integration**: ✅ **COMPLETED** - ESC/POS native QR commands for direct printing on thermal printers 🖨️
- **Bluetooth Printing**: Connect to and print receipts with embedded QR codes via Bluetooth thermal printers
- **Smart Autocomplete**: Intelligent name suggestions for billers and volunteers based on historical data
- **Receipt Preview**: View formatted receipts with visual QR code bitmap display before printing
- **Printer Management**: Save and manage preferred Bluetooth printer connections
- **100% Offline Operation**: ✅ **COMPLETED** - All QR generation works without internet connection 📶

### Phase 4 Features ✅ **COMPLETED**
- **QR Code Scanner**: ✅ **COMPLETED** - In-app camera scanner with ML Kit barcode detection 📸
- **Collection Validation**: ✅ **COMPLETED** - Real-time database validation preventing duplicate collections
- **Collection Reports**: ✅ **COMPLETED** - Comprehensive audit system with collected vs uncollected tracking 📊
- **Currency Display**: ✅ **COMPLETED** - Proper rupee (₹) currency formatting throughout the app
- **Database Integrity**: ✅ **COMPLETED** - Cascade delete operations and orphaned record cleanup
- **Audit Trail**: ✅ **COMPLETED** - Complete collection audit with statistics and percentage tracking

### Data Management
- **Multi-Device Database**: Enhanced Room database with UUID-based global sync system ✨
- **Cross-Device Sync**: Offline-first local network synchronization across up to 6 devices 🌐
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

### Instant Dialog Response
- **Dialog Appearance**: Optimized to <16ms (1 frame) for immediate user feedback
- **Async Operations**: Heavy operations (QR generation, database writes) run in background
- **UI Thread Protection**: All blocking operations moved to coroutines
- **User Experience**: Zero delay when clicking "Create & Print Receipt" button

### Technical Improvements
- **QR Generation**: SHA-256 hashing and UUID generation moved to background threads
- **Receipt Creation**: Object instantiation and database operations run asynchronously  
- **State Management**: Optimized state updates for smooth UI recomposition
- **Memory Efficiency**: Reduced UI thread blocking for better overall performance

## 🚀 Installation & Setup

### Prerequisites
- Android Studio Arctic Fox or later
- Android SDK 24+ (Android 7.0)
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
- Check Android version compatibility (API 24+)
- Verify all permissions are granted
- Clear app data and restart

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
- Test on multiple Android versions (API 24-34)
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

**Current Version**: 1.1.0  
**Feature Branch**: `feature/share_reports`  
**Development Phase**: Phase 2 Complete ✅

### ✅ Completed Features (Phase 1 & 2)
- **Multi-Device Database Schema**: UUID-based global sync system
- **Cross-Device Sync Infrastructure**: mDNS discovery with JSON protocol
- **Device Role Management**: Flexible biller/collector role switching
- **Network Status Monitoring**: Real-time sync progress and connection tracking
- **Conflict Resolution**: Timestamp and version-based conflict handling
- **Testing Framework**: Comprehensive database migration and network sync testing

### 🔄 In Development (Phase 3+)
- **QR Code Generation**: Receipt QR codes for scanning-based collection
- **Camera Scanner**: ML Kit-based QR code scanning for collectors
- **Collection Interface**: Network-aware collector UI with sync status
- **Reconciliation Reports**: Multi-device reporting with network-wide statistics

### 📱 Multi-Device Architecture
- **Offline-First Design**: No internet dependency, local WiFi network only
- **6-Device Support**: Scalable across multiple devices simultaneously
- **Real-Time Sync**: Automatic device discovery and data synchronization
- **Production Ready**: Comprehensive error handling, logging, and monitoring

---

**Version**: 11  
**Last Updated**: September 2025  
**Minimum Android Version**: 7.0 (API 24)  
**Target Android Version**: 14 (API 34)
