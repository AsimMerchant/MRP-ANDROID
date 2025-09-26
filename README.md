# Mobile Receipt Printer 📱🖨️

A modern Android application built with Kotlin and Jetpack Compose for creating and printing receipts via Bluetooth thermal printers. Perfect for small businesses, events, and mobile payment collection.

## 🌟 Features

### Core Functionality
- **Receipt Creation**: Generate professional receipts with biller, volunteer, and amount information
- **Bluetooth Printing**: Connect to and print receipts on thermal printers via Bluetooth
- **Smart Autocomplete**: Intelligent name suggestions for billers and volunteers based on historical data
- **Receipt Preview**: View formatted receipts before printing
- **Printer Management**: Save and manage preferred Bluetooth printer connections

### Data Management
- **Local Database**: Persistent storage using Room database
- **Receipt History**: View all created receipts organized by biller
- **Reports & Analytics**: Comprehensive reporting with totals and receipt counts per biller
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
- **Database**: Room (SQLite)
- **Navigation**: Navigation Compose
- **Bluetooth**: Android Bluetooth API
- **Async Operations**: Coroutines with Dispatchers

### Project Structure
```
app/src/main/java/com/example/mobilereceiptprinter/
├── MainActivity.kt              # Main activity with Compose screens
├── AppDatabase.kt              # Room database configuration
├── Receipt.kt                  # Receipt data class and DAO
├── Suggestion.kt               # Autocomplete suggestions data class and DAO
├── BluetoothPrinterHelper.kt   # Bluetooth printer communication
└── ui/theme/                   # Material Design theming
```

### Database Schema
- **Receipts Table**: Stores receipt data (ID, number, biller, volunteer, amount, date, time)
- **Suggestions Table**: Stores autocomplete suggestions separately for persistence
- **SharedPreferences**: Manages printer settings and biller-specific counters

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

---

**Version**: 11  
**Last Updated**: September 2025  
**Minimum Android Version**: 7.0 (API 24)  
**Target Android Version**: 14 (API 34)
