# Changelog

All notable changes to the Mobile Receipt Printer project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Fixed
- Receipt creation now records the exact timestamp at the moment the user presses the in-form "Create Receipt" action (previously it reused the time from when the screen first opened).
- Printed receipt text now uses a fresh timestamp at print time, ensuring print output reflects the actual print moment.

### Changed
- Time precision increased from minutes (HH:mm) to seconds (HH:mm:ss) for newly created and printed receipts. Existing stored receipts remain with their original minute-level time; no migration required.

### Notes
- Database schema unchanged (reused existing `date` and `time` fields). No migration required.
- Creation time and print time may differ if there is a delay between creation and printing; only creation time is persisted currently.
- Future enhancement (optional): add a nullable `printTime` column if audit of print latency becomes important.


## [11.0.0] - 2025-09-26

### 🎉 Major Release
- Complete rewrite of the application using modern Android development practices
- Migration from traditional View system to Jetpack Compose
- Enhanced user experience with Material Design 3

### ✨ Added
- **Modern UI Framework**: Full migration to Jetpack Compose for better performance and maintainability
- **Advanced Navigation**: Implemented Navigation Compose for seamless screen transitions
- **Smart Autocomplete System**: Intelligent name suggestions for billers and volunteers based on historical data
- **Persistent Suggestions**: Separate database table for autocomplete suggestions that persist even when receipts are deleted
- **Enhanced Reports Screen**: Comprehensive reporting with receipts grouped by biller
- **Receipt Editing**: Full CRUD operations - create, read, update, and delete individual receipts
- **Bulk Operations**: Delete all receipts for a specific biller with confirmation dialogs
- **Printer Management**: Save and automatically reconnect to preferred Bluetooth printers
- **Permission Handling**: Seamless Bluetooth permission management for Android 12+ devices
- **Receipt Preview**: Live preview of receipts before printing with proper formatting
- **Professional Receipt Format**: ESC/POS compatible formatting with proper spacing and emphasis

### 🔧 Technical Improvements
- **Database Architecture**: Room database with proper DAOs for receipts and suggestions
- **Performance Optimization**: Memoized calculations and lazy loading for better performance
- **Memory Management**: Efficient state management with Compose state handling
- **Coroutines Integration**: Proper async operations with Dispatchers for database operations
- **Edge-to-Edge Support**: Modern Android edge-to-edge design implementation
- **Window Insets**: Proper handling of system bars and keyboard interactions

### 🎨 UI/UX Enhancements
- **Material Design 3**: Complete implementation of Material You design system
- **Responsive Layout**: Optimized spacing and padding for better screen utilization
- **Card-based Design**: Clean card layouts for better content organization
- **Status Messages**: Clear feedback for user actions (printing, saving, etc.)
- **Loading States**: Proper loading indicators and state management
- **Error Handling**: User-friendly error messages and recovery options

### 🖨️ Printing System
- **Bluetooth Integration**: Robust Bluetooth printer communication system
- **ESC/POS Support**: Full compatibility with thermal printer command set
- **Connection Management**: Automatic reconnection to saved printers
- **Print Status Feedback**: Real-time feedback on printing operations
- **Multiple Printer Support**: Ability to switch between different paired printers

### 📊 Data Management
- **Receipt Numbering**: Auto-incrementing receipt numbers per biller
- **Historical Data**: Complete receipt history with search and filter capabilities
- **Data Persistence**: Reliable local storage using Room database
- **Backup Ready**: Database structure prepared for future backup/restore features
- **Data Validation**: Input validation and sanitization for all user inputs

### 🔒 Security & Permissions
- **Runtime Permissions**: Proper handling of Bluetooth permissions on Android 12+
- **Permission Education**: Clear explanations of why permissions are needed
- **Graceful Degradation**: App functionality maintained even with limited permissions
- **Data Privacy**: All data stored locally on device with no external transmission

### 🐛 Bug Fixes
- **TextOverflow Import**: Fixed unresolved reference errors for text overflow handling
- **Layout Issues**: Resolved UI layout problems in reports screen
- **Memory Leaks**: Fixed potential memory leaks in database operations
- **State Management**: Improved Compose state handling to prevent UI inconsistencies
- **Bluetooth Connectivity**: Enhanced connection stability and error recovery
- **Keyboard Handling**: Proper keyboard dismissal and focus management

### 🔄 Code Quality
- **Clean Architecture**: Separation of concerns with proper component structure
- **Code Documentation**: Comprehensive inline documentation and comments
- **Error Handling**: Robust error handling throughout the application
- **Performance Monitoring**: Optimized render cycles and reduced recompositions
- **Testing Ready**: Code structure prepared for unit and integration testing

### 📱 Device Compatibility
- **Android Version Support**: Compatible with Android 7.0 (API 24) and above
- **Screen Size Adaptation**: Responsive design for various screen sizes
- **Hardware Support**: Optimized for devices with and without physical keyboards
- **Bluetooth Standards**: Support for modern Bluetooth protocols and legacy compatibility

## [10.x.x] - Previous Versions
*Note: Version history before 11.0.0 represents the legacy implementation with traditional Android Views*

### Legacy Features (Pre-11.0.0)
- Basic receipt creation functionality
- Simple Bluetooth printer connectivity
- Basic data storage using SharedPreferences
- Traditional Android View-based UI
- Limited autocomplete functionality
- Basic receipt printing capabilities

---

## Development Notes

### Migration Path (10.x → 11.0.0)
The migration from version 10.x to 11.0.0 involved:
1. **UI Framework Migration**: Complete rewrite using Jetpack Compose
2. **Data Layer Modernization**: Migration from SharedPreferences to Room database
3. **Architecture Improvement**: Implementation of modern Android architecture patterns
4. **Performance Optimization**: Significant improvements in app responsiveness and memory usage
5. **Feature Enhancement**: Addition of advanced features like autocomplete and detailed reporting

### Technical Debt Addressed
- **Legacy UI Components**: Replaced outdated View system with modern Compose
- **Data Storage**: Moved from primitive SharedPreferences to robust Room database
- **State Management**: Implemented proper Compose state handling
- **Navigation**: Replaced fragment-based navigation with Navigation Compose
- **Permission Handling**: Updated for modern Android permission model

### Future Roadmap
- **Cloud Sync**: Potential cloud backup and synchronization features
- **Advanced Analytics**: Enhanced reporting with charts and insights
- **Multi-language Support**: Internationalization for global use
- **Printer Discovery**: Automatic printer discovery and pairing
- **Receipt Templates**: Customizable receipt formats and templates
- **Export Features**: PDF and CSV export capabilities
- **User Authentication**: Optional user accounts and data protection

### Breaking Changes in 11.0.0
- **Database Schema**: Complete change in data storage structure
- **UI Architecture**: New component-based UI structure
- **API Changes**: Internal API changes (not affecting end users)
- **Minimum SDK**: Updated minimum Android version requirements

---

**Maintained by**: Development Team  
**Release Frequency**: Major releases quarterly, patches as needed  
**Support Policy**: Latest version + 1 previous major version
