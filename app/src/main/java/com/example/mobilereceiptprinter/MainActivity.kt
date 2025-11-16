package com.example.mobilereceiptprinter

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mobilereceiptprinter.ui.theme.MobileReceiptPrinterTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.example.mobilereceiptprinter.AppDatabase
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import android.util.Log
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding

/**
 * CollectionCodeSettings - SharedPreferences helper for Collection Code System
 * 
 * Manages user preferences for:
 * - Collection code length (4-8 characters)
 * - QR code printing toggle (on/off)
 */
object CollectionCodeSettings {
    private const val PREFS_NAME = "collection_code_prefs"
    private const val KEY_CODE_LENGTH = "code_length"
    private const val KEY_PRINT_QR = "print_qr_enabled"
    
    /**
     * Get the configured collection code length
     * @return Code length (4-8), defaults to 4
     */
    fun getCodeLength(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_CODE_LENGTH, 4)
    }
    
    /**
     * Set the collection code length
     * @param length Code length (4-8 characters)
     */
    fun setCodeLength(context: Context, length: Int) {
        val clampedLength = length.coerceIn(4, 8)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_CODE_LENGTH, clampedLength)
            .apply()
    }
    
    /**
     * Check if QR code printing is enabled
     * @return true if QR codes should be printed on receipts, false otherwise (default: false)
     */
    fun isPrintQREnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_PRINT_QR, false)
    }
    
    /**
     * Set QR code printing preference
     * @param enabled true to print QR codes on receipts, false to print collection code only
     */
    fun setPrintQREnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_PRINT_QR, enabled)
            .apply()
    }
}

sealed class Screen(val route: String) {
    object Landing : Screen("landing")
    object Receipt : Screen("receipt")
    object Reports : Screen("reports")
    object NetworkSync : Screen("network_sync")
    object Scanner : Screen("scanner") // Phase 4: QR Code Scanner
    object CollectionReport : Screen("collection_report") // Phase 4: Collection Summary
    object ManualCollection : Screen("manual_collection") // Collection Code System
    object Settings : Screen("settings") // Collection Code Settings
}

class MainActivity : ComponentActivity() {
    
    internal lateinit var deviceManager: DeviceManager
    private lateinit var syncStatusManager: SyncStatusManager
    internal var deviceDiscoveryHelper: DeviceDiscoveryHelper? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize multi-device components
        initializeMultiDeviceComponents()
        
        setContent {
            MobileReceiptPrinterTheme {
                MainApp()
            }
        }
    }
    
    private fun initializeMultiDeviceComponents() {
        Log.d("MRP_INIT", "Initializing multi-device components...")
        
        // Initialize device manager
        deviceManager = DeviceManager(this)
        
        Log.d("MRP_INIT", "Device ID: ${deviceManager.getDeviceId()}")
        Log.d("MRP_INIT", "Device Name: ${deviceManager.getDeviceName()}")
        Log.d("MRP_INIT", "Device Role: ${deviceManager.getDeviceRole()}")
        
        // Initialize database and network sync
        lifecycleScope.launch {
            initializeDatabase()
            
            // Initialize network discovery and sync after database is ready
            initializeNetworkSync()
        }
    }
    
    private fun initializeNetworkSync() {
        try {
            Log.d("MRP_INIT", "Initializing network sync and device discovery...")
            
            // Initialize device discovery helper
            deviceDiscoveryHelper = DeviceDiscoveryHelper(this, deviceManager, syncStatusManager)
            deviceDiscoveryHelper?.initialize()
            
            Log.d("MRP_INIT", "Network sync initialized successfully!")
        } catch (e: Exception) {
            Log.e("MRP_INIT", "Failed to initialize network sync: ${e.message}", e)
        }
    }
    
    private suspend fun initializeDatabase() {
        try {
            Log.d("MRP_INIT", "Initializing database...")
            
            // Get database instance (this will trigger migration if needed)
            val database = AppDatabase.getDatabase(this@MainActivity)
            Log.d("MRP_INIT", "Database initialized successfully!")
            
            // Initialize sync status manager with database and device manager
            syncStatusManager = SyncStatusManager(database, deviceManager)
            Log.d("MRP_INIT", "Sync status manager initialized!")
            
        } catch (e: Exception) {
            Log.e("MRP_INIT", "Database initialization failed: ${e.message}", e)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // Cleanup network resources
        deviceDiscoveryHelper?.cleanup()
        
        Log.d("MRP_INIT", "MainActivity cleanup completed")
    }
}

@Composable
fun MainApp() {
    val navController = rememberNavController()
    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Landing.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            composable(Screen.Landing.route) { LandingScreen(navController) }
            composable(Screen.Receipt.route) { ReceiptScreen(navController) }
            composable(Screen.Reports.route) { ReportsScreen(navController) }
            composable(Screen.NetworkSync.route) { NetworkSyncScreen(navController) }
            composable(Screen.Settings.route) { SettingsScreen(navController) }
            composable(Screen.ManualCollection.route) { ManualCollectionScreen(navController) }
            composable(Screen.Scanner.route) { 
                CameraScannerScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.CollectionReport.route) { 
                CollectionReportScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
fun LandingScreen(navController: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showClearSuggestionsDialog by remember { mutableStateOf(false) }

    // Printer selection state
    val prefs = context.getSharedPreferences("printer_prefs", Context.MODE_PRIVATE)
    var savedPrinterAddress by remember { mutableStateOf(prefs.getString("saved_printer_address", null)) }
    var savedPrinterName by remember { mutableStateOf(prefs.getString("saved_printer_name", null)) }
    var showBluetoothDevices by remember { mutableStateOf(false) }
    var bluetoothPermissionGranted by remember { mutableStateOf(false) }
    var printerStatus by remember { mutableStateOf("") }

    // Bluetooth permission launcher for Android 12+
    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions: Map<String, Boolean> ->
        bluetoothPermissionGranted = permissions[Manifest.permission.BLUETOOTH_CONNECT] == true &&
            permissions[Manifest.permission.BLUETOOTH_SCAN] == true
        if (bluetoothPermissionGranted) {
            printerStatus = "Bluetooth permissions granted"
        }
    }

    val printerHelper = remember { BluetoothPrinterHelper(context) }

    fun saveSelectedPrinter(address: String, name: String) {
        prefs.edit {
            putString("saved_printer_address", address)
            putString("saved_printer_name", name)
        }
        savedPrinterAddress = address
        savedPrinterName = name
        printerStatus = "Printer selected: $name"
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 4.dp) // Reduced vertical padding from 16.dp to 4.dp
            .windowInsetsPadding(WindowInsets.systemBars),
        verticalArrangement = Arrangement.spacedBy(12.dp), // Reduced spacing from 16.dp to 12.dp
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(modifier = Modifier.height(24.dp)) // Reduced from 32.dp to 24.dp
        }

        item {
            Text(
                "Mobile Receipt Printer",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "v${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        item {
            Spacer(modifier = Modifier.height(16.dp)) // Reduced from 24.dp to 16.dp
        }

        // Printer Selection Card
        item {
            PrinterSelectionCard(
                savedPrinterName = savedPrinterName,
                bluetoothPermissionGranted = bluetoothPermissionGranted,
                onSelectPrinter = {
                    if (!bluetoothPermissionGranted) {
                        bluetoothPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.BLUETOOTH_CONNECT,
                                Manifest.permission.BLUETOOTH_SCAN
                            )
                        )
                    } else {
                        showBluetoothDevices = true
                        printerStatus = ""
                    }
                },
                onRequestPermissions = {
                    bluetoothPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.BLUETOOTH_SCAN
                        )
                    )
                }
            )
        }

        // Status messages
        if (printerStatus.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (printerStatus.contains("selected") || printerStatus.contains("granted"))
                            MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = printerStatus,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (printerStatus.contains("selected") || printerStatus.contains("granted"))
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        // Bluetooth device selection
        if (showBluetoothDevices) {
            item {
                BluetoothDeviceSelectionCard(
                    printerHelper = printerHelper,
                    context = context,
                    onDeviceSelected = { device ->
                        showBluetoothDevices = false
                        saveSelectedPrinter(device.address, device.name ?: "Unknown Printer")
                    },
                    onDismiss = { showBluetoothDevices = false }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Button(
                onClick = { navController.navigate(Screen.Receipt.route) },
                enabled = savedPrinterAddress != null, // Only enable if printer is selected
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Create Receipt", style = MaterialTheme.typography.bodyLarge)
            }
        }

        // Manual Collection Button
        item {
            OutlinedButton(
                onClick = { navController.navigate(Screen.ManualCollection.route) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("⌨️ Manual Collection", style = MaterialTheme.typography.bodyLarge)
            }
        }

        // Show message when no printer is selected
        if (savedPrinterAddress == null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                    )
                ) {
                    Text(
                        text = "⚠️ Please select a printer above to create receipts",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        item {
            OutlinedButton(
                onClick = { navController.navigate(Screen.Reports.route) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Reports", style = MaterialTheme.typography.bodyLarge)
            }
        }

        // Network Sync Button
        item {
            OutlinedButton(
                onClick = { navController.navigate(Screen.NetworkSync.route) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("🌐 Network Sync", style = MaterialTheme.typography.bodyLarge)
            }
        }

        // QR Code Scanner Button (Phase 4)
        item {
            OutlinedButton(
                onClick = { navController.navigate(Screen.Scanner.route) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.tertiary
                )
            ) {
                Text("📱 QR Scanner", style = MaterialTheme.typography.bodyLarge)
            }
        }

        // Collection Report Button (Phase 4)
        item {
            OutlinedButton(
                onClick = { navController.navigate(Screen.CollectionReport.route) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.tertiary
                )
            ) {
                Text("📊 Collection Report", style = MaterialTheme.typography.bodyLarge)
            }
        }

        // Settings Button
        item {
            OutlinedButton(
                onClick = { navController.navigate(Screen.Settings.route) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("⚙️ Settings", style = MaterialTheme.typography.bodyLarge)
            }
        }

        item {
            OutlinedButton(
                onClick = {
                    showClearSuggestionsDialog = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Clear Name Suggestions", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }

    // Clear Suggestions Confirmation Dialog
    if (showClearSuggestionsDialog) {
        AlertDialog(
            onDismissRequest = { showClearSuggestionsDialog = false },
            title = { Text("Clear Name Suggestions") },
            text = { Text("Are you sure you want to clear all saved name suggestions? This will remove all autocomplete suggestions for biller and volunteer names. This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val db = AppDatabase.getDatabase(context)
                            withContext(Dispatchers.IO) {
                                // Clear only suggestions, not receipts
                                db.suggestionDao().clearAllSuggestions()
                            }
                            showClearSuggestionsDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearSuggestionsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ReceiptScreen(navController: NavHostController) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("printer_prefs", Context.MODE_PRIVATE)
    var biller by remember { mutableStateOf("") }
    var volunteer by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var showPreview by remember { mutableStateOf(false) }
    var receiptNumber by remember { mutableStateOf(1) }
    var currentQRCode by remember { mutableStateOf("") }
    // Remove remembered static date/time; we will capture fresh timestamps at creation time
    fun nowDate(): String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    fun nowTime(): String = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
    var showBluetoothDevices by remember { mutableStateOf(false) }
    var printStatus by remember { mutableStateOf("") }
    var showShareOptions by remember { mutableStateOf(false) }
    var savedPrinterAddress by remember { mutableStateOf(prefs.getString("saved_printer_address", null)) }
    var bluetoothPermissionGranted by remember { mutableStateOf(false) }
    var showPrintingDialog by remember { mutableStateOf(false) }
    var printingProgress by remember { mutableStateOf("") }
    var isCreatingAndPrinting by remember { mutableStateOf(false) }

    // Bluetooth permission launcher for Android 12+
    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions: Map<String, Boolean> ->
        bluetoothPermissionGranted = permissions[Manifest.permission.BLUETOOTH_CONNECT] == true &&
            permissions[Manifest.permission.BLUETOOTH_SCAN] == true
    }

    LaunchedEffect(Unit) {
        bluetoothPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )
        )
    }

    val printerHelper = remember { BluetoothPrinterHelper(context) }

    // Function to get next receipt number for biller (matching HTML app logic)
    fun getNextReceiptNumber(billerName: String): Int {
        val billerPrefs = context.getSharedPreferences("biller_$billerName", Context.MODE_PRIVATE)
        val existingReceipts = billerPrefs.getInt("receipt_count", 0)
        return existingReceipts + 1
    }

    // Function to save biller data (matching HTML app logic)
    fun saveBillerData(billerName: String, receiptNum: Int, amountValue: Double) {
        val billerPrefs = context.getSharedPreferences("biller_$billerName", Context.MODE_PRIVATE)
        val currentTotal = billerPrefs.getFloat("total_amount", 0f)
        billerPrefs.edit {
            putInt("receipt_count", receiptNum)
            putFloat("total_amount", currentTotal + amountValue.toFloat())
        }
    }

    // Build receipt text dynamically when printing (uses latest values)
    fun buildReceiptText(date: String, time: String, qrCode: String = "") = """
\\u001B\\u0061\\u0001\\u001B\\u0021\\u0038${QRCodeGenerator.getCollectionCode(qrCode, CollectionCodeSettings.getCodeLength(context))}\\u001B\\u0021\\u0000\\u001B\\u0061\\u0000
${if (qrCode.isNotEmpty() && CollectionCodeSettings.isPrintQREnabled(context)) {
    QRCodeGenerator.generateThermalPrinterQR(qrCode) + "\n"
} else {
    ""
}}=======================
\\u001B\\u0021\\u0030 RECEIPT #$receiptNumber \\u001B\\u0021\\u0000
=======================
Date: $date
Time: $time
-----------------------
Biller: $biller
-----------------------
Volunteer: $volunteer
-----------------------
\\u001B\\u0021\\u0030AMOUNT: Rs. $amount\\u001B\\u0021\\u0000
=======================



""".trimIndent()

    // Clean version for preview display (without ESC/POS commands or QR code)
    fun buildReceiptPreviewText() = """
RECEIPT #$receiptNumber     
Biller: $biller
Volunteer: $volunteer
AMOUNT: Rs. $amount

""".trimIndent()

    fun saveLastPrinter(address: String) {
        prefs.edit { putString("saved_printer_address", address) }
        savedPrinterAddress = address
    }

    fun printToDevice(device: BluetoothDevice) {
        if (!bluetoothPermissionGranted) {
            printStatus = "Bluetooth permission not granted."
            return
        }
        
        // Move all Bluetooth operations to IO thread to prevent ANR
        (context as ComponentActivity).lifecycleScope.launch {
            try {
                printStatus = "Connecting to printer..."
                
                val result = withContext(Dispatchers.IO) {
                    // Use fresh timestamp at the moment of print (may differ from creation time if delayed)
                    val printDate = nowDate()
                    val printTime = nowTime()
                    val receiptText = buildReceiptText(printDate, printTime, currentQRCode)
                    
                    val connected = printerHelper.connectToDevice(device)
                    if (connected) {
                        val printed = printerHelper.printText(receiptText)
                        printerHelper.closeConnection()
                        if (printed) {
                            saveLastPrinter(device.address)
                        }
                        printed
                    } else {
                        false
                    }
                }
                
                // Update UI on main thread
                if (result) {
                    printStatus = "✓ Printed! Ready for next receipt."
                    volunteer = ""
                    amount = ""
                    showPreview = false
                } else {
                    printStatus = "❌ Print failed. Please try again."
                }
            } catch (e: Exception) {
                printStatus = "❌ Error: ${e.message}"
            }
        }
    }

    // Print function for dialog workflow
    fun printToDeviceWithDialog(device: BluetoothDevice) {
        (context as ComponentActivity).lifecycleScope.launch {
            try {
                // Move all Bluetooth operations to IO thread to prevent ANR
                val result = withContext(Dispatchers.IO) {
                    val printDate = nowDate()
                    val printTime = nowTime()
                    val receiptText = buildReceiptText(printDate, printTime, currentQRCode)
                    
                    val connected = printerHelper.connectToDevice(device)
                    if (connected) {
                        val printed = printerHelper.printText(receiptText)
                        printerHelper.closeConnection()
                        if (printed) {
                            saveLastPrinter(device.address)
                        }
                        printed
                    } else {
                        false
                    }
                }
                
                // Update UI on main thread based on result
                if (result) {
                    printingProgress = "✓ Receipt printed successfully!"
                    kotlinx.coroutines.delay(1500)
                    
                    // Clear form for next receipt - do this AFTER dialog closes to avoid keyboard interference
                    showPreview = false
                    printStatus = ""
                    showPrintingDialog = false
                    isCreatingAndPrinting = false
                    
                    // Clear text fields after a small delay to ensure keyboard is fully dismissed
                    kotlinx.coroutines.delay(100)
                    volunteer = ""
                    amount = ""
                } else {
                    printingProgress = "❌ Print failed"
                    kotlinx.coroutines.delay(2000)
                    showPrintingDialog = false
                    isCreatingAndPrinting = false
                }
            } catch (e: Exception) {
                printingProgress = "❌ Could not connect to printer"
                kotlinx.coroutines.delay(2000)
                showPrintingDialog = false
                isCreatingAndPrinting = false
            } catch (e: Exception) {
                printingProgress = "❌ Error: ${e.message}"
                kotlinx.coroutines.delay(2000)
                showPrintingDialog = false
                isCreatingAndPrinting = false
            }
        }
    }

    // Auto-save receipt when created
    fun createAndSaveReceipt() {
        if (biller.isNotBlank()) {
            receiptNumber = getNextReceiptNumber(biller)
        }
        showPreview = true

        // Capture creation timestamp exactly now
        val creationDate = nowDate()
        val creationTime = nowTime()

        // Auto-save the receipt with fresh timestamps
        val amountValue = amount.toDoubleOrNull() ?: 0.0
        
        // Generate unique QR code for receipt (Phase 3)
        val receiptId = java.util.UUID.randomUUID().toString()
        val receiptData = "$biller$volunteer$amount$creationDate$creationTime"
        
        // Cache device manager to avoid repeated context casting
        val deviceManager = (context as MainActivity).deviceManager
        val deviceId = deviceManager.getDeviceId()
        
        val qrCode = QRCodeGenerator.generateQRContent(
            receiptId = receiptId,
            deviceId = deviceId,
            receiptData = receiptData
        )
        currentQRCode = qrCode  // Store for printing/preview
        
        val receipt = Receipt(
            id = receiptId,  // Use generated UUID as primary key
            receiptNumber = receiptNumber,
            biller = biller,
            volunteer = volunteer,
            amount = amount,
            date = creationDate,
            time = creationTime,
            qrCode = qrCode,  // Populate QR code field
            deviceId = deviceId,  // Track device that created receipt
            lastModified = System.currentTimeMillis()
        )
        (context as ComponentActivity).lifecycleScope.launch {
            val db = AppDatabase.getDatabase(context)
            withContext(Dispatchers.IO) {
                db.receiptDao().insert(receipt)
                // Save suggestions separately so they persist even if receipts are deleted
                db.suggestionDao().addBillerSuggestion(biller)
                db.suggestionDao().addVolunteerSuggestion(volunteer)
            }
            saveBillerData(biller, receiptNumber, amountValue)
        }
    }

    // New function: Create receipt and auto-print
    fun createReceiptAndPrint() {
        // Show dialog immediately for instant user feedback
        isCreatingAndPrinting = true
        showPrintingDialog = true
        printingProgress = "Initializing..."
        
        if (!bluetoothPermissionGranted) {
            printingProgress = "❌ Bluetooth permission not granted"
            isCreatingAndPrinting = false
            return
        }

        if (savedPrinterAddress == null) {
            printingProgress = "❌ No printer selected. Please select a printer first."
            isCreatingAndPrinting = false
            return
        }

        // Move all heavy operations to coroutine to avoid blocking dialog display
        (context as ComponentActivity).lifecycleScope.launch {
            // Allow UI to recompose and show dialog first
            delay(1)
            
            printingProgress = "Creating receipt..."
            
            // Inline all createAndSaveReceipt() operations to prevent UI blocking
            if (biller.isNotBlank()) {
                receiptNumber = getNextReceiptNumber(biller)
            }
            showPreview = true
            

            val creationDate = nowDate()
            val creationTime = nowTime()
            val amountValue = amount.toDoubleOrNull() ?: 0.0
            val receiptId = java.util.UUID.randomUUID().toString()
            val receiptData = "$biller$volunteer$amount$creationDate$creationTime"
            
            // Cache device manager to avoid repeated context casting
            val deviceManager = (context as MainActivity).deviceManager
            val deviceId = deviceManager.getDeviceId()
            
            val qrCode = QRCodeGenerator.generateQRContent(
                receiptId = receiptId,
                deviceId = deviceId,
                receiptData = receiptData
            )
            currentQRCode = qrCode
            
            val receipt = Receipt(
                id = receiptId,
                receiptNumber = receiptNumber,
                biller = biller,
                volunteer = volunteer,
                amount = amount,
                date = creationDate,
                time = creationTime,
                qrCode = qrCode,
                deviceId = deviceId,
                lastModified = System.currentTimeMillis()
            )
            
            // Database operations (same as before)
            val db = AppDatabase.getDatabase(context)
            withContext(Dispatchers.IO) {
                db.receiptDao().insert(receipt)
                db.suggestionDao().addBillerSuggestion(biller)
                db.suggestionDao().addVolunteerSuggestion(volunteer)
            }
            saveBillerData(biller, receiptNumber, amountValue)
            
            // Start printing process
            printingProgress = "Connecting to printer..."
            
            val savedDevice = printerHelper.getPairedDevices()?.find { it.address == savedPrinterAddress }
            if (savedDevice != null) {
                printingProgress = "Printing receipt..."
                printToDeviceWithDialog(savedDevice)
            } else {
                printingProgress = "Printer not found"
                isCreatingAndPrinting = false
                kotlinx.coroutines.delay(2000)
                showPrintingDialog = false
            }
        }
    }

    // Autocomplete suggestions
    val billerSuggestions = remember { mutableStateListOf<String>() }
    val volunteerSuggestions = remember { mutableStateListOf<String>() }
    var showBillerSuggestions by remember { mutableStateOf(false) }
    var showVolunteerSuggestions by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Focus manager for keyboard dismissal
    val focusManager = LocalFocusManager.current

    // Load suggestions when screen loads - now from separate suggestions table
    LaunchedEffect(Unit) {
        scope.launch {
            val db = AppDatabase.getDatabase(context)
            val billers = withContext(Dispatchers.IO) { db.suggestionDao().getAllBillerSuggestions() }
            val volunteers = withContext(Dispatchers.IO) { db.suggestionDao().getAllVolunteerSuggestions() }
            billerSuggestions.clear()
            // Limit suggestions to last 50 to prevent memory buildup
            billerSuggestions.addAll(billers.takeLast(50))
            volunteerSuggestions.clear()
            // Limit suggestions to last 50 to prevent memory buildup  
            volunteerSuggestions.addAll(volunteers.takeLast(50))
        }
    }

    // Memoize filtered suggestions for better performance
    val filteredBillerSuggestions = remember(biller, billerSuggestions.size) {
        if (biller.isEmpty()) emptyList()
        else billerSuggestions.filter {
            it.contains(biller, ignoreCase = true) && it != biller
        }.take(5)
    }

    val filteredVolunteerSuggestions = remember(volunteer, volunteerSuggestions.size) {
        if (volunteer.isEmpty()) emptyList()
        else volunteerSuggestions.filter {
            it.contains(volunteer, ignoreCase = true) && it != volunteer
        }.take(5)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 4.dp) // Reduced vertical padding from 8.dp to 4.dp
            .windowInsetsPadding(WindowInsets.systemBars),
        verticalArrangement = Arrangement.spacedBy(10.dp) // Reduced spacing from 12.dp to 10.dp
    ) {
        item(key = "form_card") {
            // Form card
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp), // Reduced padding from 20.dp to 18.dp
                    verticalArrangement = Arrangement.spacedBy(12.dp) // Reduced spacing from 14.dp to 12.dp
                ) {
                    Text(
                        "Create Receipt",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Biller Name with Optimized Autocomplete
                    Column {
                        OutlinedTextField(
                            value = biller,
                            onValueChange = {
                                biller = it
                                showBillerSuggestions = it.isNotEmpty() && billerSuggestions.any { suggestion ->
                                    suggestion.contains(it, ignoreCase = true)
                                }
                            },
                            label = { Text("Biller Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        if (showBillerSuggestions && filteredBillerSuggestions.isNotEmpty()) {
                            SuggestionCard(
                                suggestions = filteredBillerSuggestions,
                                onSuggestionClick = { suggestion ->
                                    biller = suggestion
                                    showBillerSuggestions = false
                                }
                            )
                        }
                    }

                    // Volunteer Name with Optimized Autocomplete
                    Column {
                        OutlinedTextField(
                            value = volunteer,
                            onValueChange = {
                                volunteer = it
                                showVolunteerSuggestions = it.isNotEmpty() && volunteerSuggestions.any { suggestion ->
                                    suggestion.contains(it, ignoreCase = true)
                                }
                            },
                            label = { Text("Volunteer Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        if (showVolunteerSuggestions && filteredVolunteerSuggestions.isNotEmpty()) {
                            SuggestionCard(
                                suggestions = filteredVolunteerSuggestions,
                                onSuggestionClick = { suggestion ->
                                    volunteer = suggestion
                                    showVolunteerSuggestions = false
                                }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Amount (Rs.)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    val isFormValid = remember(biller, volunteer, amount) {
                        biller.isNotBlank() && volunteer.isNotBlank() && amount.isNotBlank()
                    }

                    Button(
                        onClick = {
                            // Clear all autocomplete suggestions that might maintain focus
                            showBillerSuggestions = false
                            showVolunteerSuggestions = false
                            
                            // Show dialog immediately - no blocking operations
                            createReceiptAndPrint()
                            
                            // Move keyboard dismissal and database refresh to async to avoid blocking dialog
                            scope.launch {
                                // Dismiss keyboard asynchronously to prevent blocking dialog appearance
                                focusManager.clearFocus()
                                
                                // Small delay to ensure dialog renders first
                                delay(50)
                                val db = AppDatabase.getDatabase(context)
                                val billers = withContext(Dispatchers.IO) { db.suggestionDao().getAllBillerSuggestions() }
                                val volunteers = withContext(Dispatchers.IO) { db.suggestionDao().getAllVolunteerSuggestions() }
                                billerSuggestions.clear()
                                billerSuggestions.addAll(billers)
                                volunteerSuggestions.clear()
                                volunteerSuggestions.addAll(volunteers)
                            }
                        },
                        enabled = isFormValid && !isCreatingAndPrinting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isCreatingAndPrinting) {
                            Text("Creating & Printing...", style = MaterialTheme.typography.bodyLarge)
                        } else {
                            Text("Create & Print Receipt", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }

        if (showPreview && !showPrintingDialog) {
            item(key = "receipt_preview") {
                ReceiptPreviewCard(
                    receiptPreviewText = buildReceiptPreviewText()
                )
            }
        }

        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(12.dp)) // Reduced bottom spacer from 20.dp to 12.dp
        }
    }
    
    // Printing Progress Dialog
    if (showPrintingDialog) {
        AlertDialog(
            onDismissRequest = { /* Don't allow dismissal while printing */ },
            title = { 
                Text("Creating & Printing Receipt") 
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (showPreview) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                        ) {
                            Text(
                                text = buildReceiptPreviewText(),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                ),
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (isCreatingAndPrinting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        }
                        Text(
                            text = printingProgress,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (printingProgress.contains("❌")) 
                                MaterialTheme.colorScheme.error
                            else if (printingProgress.contains("✓"))
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            },
            confirmButton = {
                if (!isCreatingAndPrinting) {
                    TextButton(
                        onClick = { 
                            showPrintingDialog = false
                            printingProgress = ""
                        }
                    ) {
                        Text("OK")
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualCollectionScreen(navController: NavHostController) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val coroutineScope = rememberCoroutineScope()
    
    var searchCode by remember { mutableStateOf("") }
    val codeLength = CollectionCodeSettings.getCodeLength(context)
    
    // Live search results using Flow
    val searchResults by remember(searchCode, codeLength) {
        if (searchCode.length >= 2) {
            database.receiptDao().searchByCollectionCode(searchCode, codeLength)
        } else {
            MutableStateFlow(emptyList())
        }
    }.collectAsState(initial = emptyList())
    
    var showCollectionDialog by remember { mutableStateOf(false) }
    var selectedReceipt by remember { mutableStateOf<Receipt?>(null) }
    var collectionStatus by remember { mutableStateOf("") }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manual Collection") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Text("←", style = MaterialTheme.typography.headlineMedium)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Instructions
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = "Enter the collection code from the receipt to find and collect it.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            // Search input
            OutlinedTextField(
                value = searchCode,
                onValueChange = { searchCode = it.uppercase() },
                label = { Text("Collection Code") },
                placeholder = { Text("Enter code (e.g., C2B9)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                supportingText = {
                    Text("Min. 2 characters • Code length: $codeLength")
                }
            )
            
            // Results section
            when {
                searchCode.length < 2 -> {
                    // Empty state
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Enter at least 2 characters to search",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                searchResults.isEmpty() -> {
                    // No results
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No receipts found",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Try a different code",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                else -> {
                    // Results list
                    Text(
                        text = "${searchResults.size} receipt(s) found",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(searchResults) { receipt ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (!receipt.isCollected) {
                                            selectedReceipt = receipt
                                            showCollectionDialog = true
                                        }
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (receipt.isCollected)
                                        MaterialTheme.colorScheme.surfaceVariant
                                    else
                                        MaterialTheme.colorScheme.surface
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
                                            text = "Receipt #${receipt.receiptNumber}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Code: ${QRCodeGenerator.getCollectionCode(receipt.qrCode, codeLength)}",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Biller: ${receipt.biller}",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = "Amount: ${receipt.amount}",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = "${receipt.date} ${receipt.time}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    
                                    if (receipt.isCollected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Collected",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Collection confirmation dialog
        if (showCollectionDialog && selectedReceipt != null) {
            AlertDialog(
                onDismissRequest = { showCollectionDialog = false },
                title = { Text("Collect Receipt") },
                text = {
                    Column {
                        Text("Receipt #${selectedReceipt!!.receiptNumber}")
                        Text("Amount: ${selectedReceipt!!.amount}")
                        Text("Biller: ${selectedReceipt!!.biller}")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Mark this receipt as collected?")
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                try {
                                    val receiptId = selectedReceipt!!.id
                                    val currentTime = System.currentTimeMillis()
                                    val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                                    val timeFormat = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                                    val now = java.util.Date(currentTime)
                                    val deviceManager = DeviceManager(context)
                                    
                                    // Create collection record
                                    val collectedReceipt = CollectedReceipt(
                                        receiptId = receiptId,
                                        collectorName = "Manual Entry User",
                                        collectionTime = timeFormat.format(now),
                                        collectionDate = dateFormat.format(now),
                                        scannedBy = "Manual Entry",
                                        collectorDeviceId = deviceManager.getDeviceId(),
                                        syncStatus = "PENDING",
                                        lastModified = currentTime
                                    )
                                    
                                    // Insert collection record
                                    database.collectedReceiptDao().insert(collectedReceipt)
                                    
                                    // Update receipt status
                                    database.receiptDao().updateCollectionStatusWithTimestamp(
                                        receiptId = receiptId,
                                        isCollected = true,
                                        timestamp = currentTime
                                    )
                                    
                                    collectionStatus = "Receipt collected successfully"
                                    showCollectionDialog = false
                                    // Clear search to refresh results
                                    searchCode = ""
                                } catch (e: Exception) {
                                    collectionStatus = "Error: ${e.message}"
                                }
                            }
                        }
                    ) {
                        Text("Collect")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCollectionDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
        
        // Status message
        if (collectionStatus.isNotEmpty()) {
            LaunchedEffect(collectionStatus) {
                delay(3000)
                collectionStatus = ""
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavHostController) {
    val context = LocalContext.current
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Text("←", style = MaterialTheme.typography.headlineMedium)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Collection Code Settings Section
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Collection Code Settings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Code Length Slider (Task 7)
                var codeLength by remember { 
                    mutableStateOf(CollectionCodeSettings.getCodeLength(context).toFloat()) 
                }
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Code Length: ${codeLength.toInt()} characters",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Slider(
                        value = codeLength,
                        onValueChange = { codeLength = it },
                        onValueChangeFinished = {
                            CollectionCodeSettings.setCodeLength(context, codeLength.toInt())
                        },
                        valueRange = 4f..8f,
                        steps = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Text(
                        text = "Shorter codes are easier to type, but longer codes reduce collision probability.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // QR Printing Toggle (Task 8)
                var printQREnabled by remember { 
                    mutableStateOf(CollectionCodeSettings.isPrintQREnabled(context)) 
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Print QR Code on Receipt",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = "Enable to print QR code on receipts. Collection code is always printed.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Switch(
                        checked = printQREnabled,
                        onCheckedChange = { enabled ->
                            printQREnabled = enabled
                            CollectionCodeSettings.setPrintQREnabled(context, enabled)
                        }
                    )
                }
            }
        }
    }
}

// Optimized reusable components
@Composable
private fun SuggestionCard(
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column {
            suggestions.forEachIndexed { index, suggestion ->
                Text(
                    text = suggestion,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSuggestionClick(suggestion) }
                        .padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
                if (index < suggestions.lastIndex) {
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun ReceiptPreviewCard(receiptPreviewText: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "Receipt Preview",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = receiptPreviewText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun PrintActionsCard(
    savedPrinterAddress: String?,
    bluetoothPermissionGranted: Boolean,
    onPrint: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onPrint,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    if (savedPrinterAddress != null) "Print to Saved Device" else "Select Printer & Print",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
private fun PrintStatusCard(printStatus: String) {
    val isSuccess = printStatus.contains("success", ignoreCase = true) || printStatus.contains("✓")

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSuccess)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.errorContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            printStatus,
            modifier = Modifier.padding(16.dp),
            color = if (isSuccess)
                MaterialTheme.colorScheme.onPrimaryContainer
            else
                MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun BluetoothDeviceSelectionCard(
    printerHelper: BluetoothPrinterHelper,
    context: Context,
    onDeviceSelected: (BluetoothDevice) -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            val devices = remember { printerHelper.getPairedDevices()?.toList() ?: emptyList() }

            if (devices.isEmpty()) {
                Text(
                    "No paired Bluetooth devices found.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                        context.startActivity(intent)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Open Bluetooth Settings")
                }
            } else {
                Text(
                    "Select Bluetooth Device:",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))

                devices.forEach { device ->
                    key(device.address) {
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { onDeviceSelected(device) },
                            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth()
                            ) {
                                Text(
                                    text = device.name ?: "Unknown Device",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "MAC: ${device.address}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Tap to print & save as default",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
private fun PrinterSelectionCard(
    savedPrinterName: String?,
    bluetoothPermissionGranted: Boolean,
    onSelectPrinter: () -> Unit,
    onRequestPermissions: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Printer Selection",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            if (savedPrinterName != null) {
                // Show current selected printer
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Selected Printer:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                savedPrinterName,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            } else {
                // No printer selected
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        "No printer selected",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            // Select/Change printer button
            Button(
                onClick = {
                    if (bluetoothPermissionGranted) {
                        onSelectPrinter()
                    } else {
                        onRequestPermissions()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    if (savedPrinterName != null) "Change Printer" else "Select Printer",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            if (!bluetoothPermissionGranted) {
                Text(
                    "Bluetooth permissions required to select printer",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}



@Composable
fun ReportsScreen(navController: NavHostController) {
    val context = LocalContext.current
    val billerSummaries = remember { mutableStateListOf<BillerSummary>() }
    val expandedBillers = remember { mutableStateMapOf<String, Boolean>() }
    val billerReceipts = remember { mutableStateMapOf<String, List<Receipt>>() }
    val scope = rememberCoroutineScope()
    var showDeleteBillerDialog by remember { mutableStateOf(false) }
    var billerToDelete by remember { mutableStateOf<String?>(null) }
    var receiptCountToDelete by remember { mutableStateOf(0) }

    // PERFORMANCE FIX: Load only summaries initially instead of ALL receipts
    LaunchedEffect(Unit) {
        scope.launch {
            val db = AppDatabase.getDatabase(context)
            val summaries = withContext(Dispatchers.IO) { db.receiptDao().getBillerSummaries() }
            billerSummaries.clear()
            billerSummaries.addAll(summaries)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .windowInsetsPadding(WindowInsets.systemBars),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "header") {
            Text(
                "Receipt Reports",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (billerSummaries.isEmpty()) {
            item(key = "empty_state") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        "No receipts found.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        } else {
            itemsIndexed(
                items = billerSummaries,
                key = { _, summary -> "biller_${summary.biller}" }
            ) { _, summary ->
                ExpandableBillerCard(
                    summary = summary,
                    isExpanded = expandedBillers[summary.biller] ?: false,
                    receipts = billerReceipts[summary.biller],
                    onExpandToggle = {
                        val currentlyExpanded = expandedBillers[summary.biller] ?: false
                        if (!currentlyExpanded) {
                            // Load receipts on demand when expanding
                            scope.launch {
                                val db = AppDatabase.getDatabase(context)
                                val receipts = withContext(Dispatchers.IO) {
                                    db.receiptDao().getReceiptsByBiller(summary.biller)
                                }
                                billerReceipts[summary.biller] = receipts
                                expandedBillers[summary.biller] = true
                            }
                        } else {
                            // Collapse and free memory to prevent accumulation
                            expandedBillers[summary.biller] = false
                            billerReceipts.remove(summary.biller) // Clear cached receipts to free memory
                        }
                    },
                    onDeleteAll = {
                        billerToDelete = summary.biller
                        receiptCountToDelete = summary.receiptCount
                        showDeleteBillerDialog = true
                    }
                )
            }
        }
    }

    // Delete Biller Confirmation Dialog
    if (showDeleteBillerDialog && billerToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteBillerDialog = false
                billerToDelete = null
            },
            title = { Text("Delete All Receipts") },
            text = {
                Text("Are you sure you want to delete ALL receipts for \"$billerToDelete\"? This will permanently remove $receiptCountToDelete receipts. The next receipt for this biller will start from #1.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val db = AppDatabase.getDatabase(context)
                            withContext(Dispatchers.IO) {
                                db.receiptDao().deleteAllReceiptsFromBillerWithCleanup(billerToDelete!!)
                            }

                            // Reset the biller's receipt count in SharedPreferences
                            val billerPrefs = context.getSharedPreferences("biller_$billerToDelete", Context.MODE_PRIVATE)
                            billerPrefs.edit {
                                putInt("receipt_count", 0)
                                putFloat("total_amount", 0f)
                            }

                            // Refresh summaries
                            val summaries = withContext(Dispatchers.IO) { db.receiptDao().getBillerSummaries() }
                            billerSummaries.clear()
                            billerSummaries.addAll(summaries)
                            
                            // Clear cached receipts and expanded state for deleted biller
                            billerReceipts.remove(billerToDelete)
                            expandedBillers.remove(billerToDelete)
                            
                            showDeleteBillerDialog = false
                            billerToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete All")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteBillerDialog = false
                    billerToDelete = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// Performance-optimized expandable BillerCard with on-demand receipt loading
@Composable
private fun ExpandableBillerCard(
    summary: BillerSummary,
    isExpanded: Boolean,
    receipts: List<Receipt>?,
    onExpandToggle: () -> Unit,
    onDeleteAll: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Clickable header with expand/collapse icon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onExpandToggle),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${summary.biller} : ${summary.receiptCount} ${if (summary.receiptCount == 1) "receipt" else "receipts"}",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Total: Rs. ${String.format(Locale.getDefault(), "%.2f", summary.totalAmount)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Delete button always visible
            OutlinedButton(
                onClick = onDeleteAll,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.height(40.dp)
            ) {
                Text(
                    "Delete All",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Show receipts only when expanded and loaded
            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                
                if (receipts == null) {
                    // Loading state
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (receipts.isEmpty()) {
                    // Empty state (shouldn't happen in normal flow)
                    Text(
                        "No receipts found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp)
                    )
                } else {
                    // Show all receipts for this biller
                    receipts.forEach { receipt ->
                        key(receipt.id) {
                            ReceiptCard(receipt = receipt)
                        }
                    }
                }
            }
        }
    }
}

// Extracted optimized ReceiptCard component
@Composable
private fun ReceiptCard(
    receipt: Receipt
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = "Receipt #${receipt.receiptNumber}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${receipt.date} • ${receipt.time}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Volunteer: ${receipt.volunteer}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Rs. ${receipt.amount}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun NetworkSyncScreen(navController: NavHostController) {
    val context = LocalContext.current
    val activity = context as MainActivity
    val scope = rememberCoroutineScope()
    
    // Connect to REAL DeviceDiscoveryHelper with null safety
    val discoveredDevices by remember(activity.deviceDiscoveryHelper) {
        activity.deviceDiscoveryHelper?.discoveredDevices ?: MutableStateFlow(emptyList())
    }.collectAsState()
    val isDiscovering by remember(activity.deviceDiscoveryHelper) {
        activity.deviceDiscoveryHelper?.isDiscovering ?: MutableStateFlow(false)
    }.collectAsState()
    var syncStatus by remember { mutableStateOf("Initializing network sync...") }
    var lastSyncResult by remember { mutableStateOf("Click 'Check Network Status' to see diagnostics") }
    
    // Load real database statistics on screen load
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val receiptCount = withContext(Dispatchers.IO) { db.receiptDao().getAllReceipts().size }
                val collectedCount = withContext(Dispatchers.IO) { db.collectedReceiptDao().getAllCollectedReceipts().size }
                val syncLogCount = withContext(Dispatchers.IO) { db.deviceSyncLogDao().getAllSyncLogs().size }
                
                if (lastSyncResult == "Click 'Check Network Status' to see diagnostics") {
                    lastSyncResult = "📊 Local Database Status:\n" +
                        "• Total receipts: $receiptCount\n" +
                        "• Collected receipts: $collectedCount\n" +
                        "• Sync log entries: $syncLogCount\n" +
                        "• Ready for network sync"
                }
            } catch (e: Exception) {
                lastSyncResult = "❌ Database error: ${e.message}"
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .windowInsetsPadding(WindowInsets.systemBars),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Network Sync",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                
                TextButton(onClick = { navController.navigateUp() }) {
                    Text("Back")
                }
            }
        }

        item {
            // Network Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Network Status",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isDiscovering) "🔍 Discovering..." else "📡 Ready for Discovery",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        
                        Button(
                            onClick = {
                                scope.launch {
                                    activity.deviceDiscoveryHelper?.let { helper ->
                                        if (isDiscovering) {
                                            helper.stopDiscovery()
                                            syncStatus = "Discovery stopped"
                                        } else {
                                            syncStatus = "Starting discovery..."
                                            helper.startDiscovery()
                                            syncStatus = "Discovering devices on network..."
                                        }
                                    } ?: run {
                                        syncStatus = "Network sync not initialized yet"
                                    }
                                }
                            },
                            enabled = true
                        ) {
                            Text(if (isDiscovering) "Stop Discovery" else "Start Discovery")
                        }
                    }
                    
                    Text(
                        text = syncStatus,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            // Discovered Devices Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Discovered Devices (${discoveredDevices.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    if (discoveredDevices.isEmpty()) {
                        Text(
                            "No devices found. Start discovery to find other MRP devices on the network.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        discoveredDevices.forEach { device ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = device.deviceName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${device.address}:${device.port}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Role: ${device.role} | ID: ${device.deviceId.take(8)}...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    
                                    Text(
                                        text = "📱 Ready",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            // Sync Controls Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Sync Control",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Button(
                        onClick = {
                            scope.launch {
                                activity.deviceDiscoveryHelper?.let { helper ->
                                    syncStatus = "Syncing data with ${discoveredDevices.size} devices..."
                                    try {
                                        val syncResult = helper.syncWithAllDevices()
                                        
                                        if (syncResult.success) {
                                            lastSyncResult = "✅ Sync completed successfully!\n" +
                                                "• ${syncResult.devicesSync} devices synced\n" +
                                                "• ${syncResult.receiptsSync} receipts synchronized\n" +
                                                "• ${syncResult.collectionsSync} collections updated\n" +
                                                "• ${syncResult.conflicts} conflicts resolved\n" +
                                                "• Completed at: ${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(syncResult.timestamp))}"
                                            syncStatus = "Real sync completed successfully"
                                        } else {
                                            lastSyncResult = "❌ Sync failed: ${syncResult.errorMessage ?: "Unknown error"}\n" +
                                                "• ${syncResult.devicesSync} devices attempted\n" +
                                                "• ${syncResult.receiptsSync} receipts processed\n" +
                                                "• ${syncResult.collectionsSync} collections processed"
                                            syncStatus = "Sync failed - check network connection"
                                        }
                                    } catch (e: Exception) {
                                        lastSyncResult = "❌ Sync error: ${e.message}\n• Check network connectivity\n• Ensure devices are reachable"
                                        syncStatus = "Sync error occurred"
                                    }
                                } ?: run {
                                    lastSyncResult = "❌ Network sync not initialized"
                                    syncStatus = "Initialization error"
                                }
                            }
                        },
                        enabled = discoveredDevices.isNotEmpty() && !isDiscovering,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🔄 Sync Data with All Devices")
                    }
                    
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                activity.deviceDiscoveryHelper?.let { helper ->
                                    syncStatus = "Checking network sync status..."
                                    
                                    val statistics: Map<String, Any> = helper.getSyncStatistics()
                                    val networkStatus = (statistics["networkStatus"] as? Enum<*>)?.name ?: "UNKNOWN"
                                    val isDiscovering = statistics["isDiscovering"] as? Boolean ?: false
                                    val discoveredCount = statistics["discoveredDevices"] as? Int ?: 0
                                    val lastSyncTime = statistics["lastSyncTime"] as? Long ?: 0L
                                    val lastSyncSuccess = statistics["lastSyncSuccess"] as? Boolean ?: false
                                    val totalReceipts = statistics["totalReceiptsSync"] as? Int ?: 0
                                    val totalCollections = statistics["totalCollectionsSync"] as? Int ?: 0
                                    val totalConflicts = statistics["totalConflicts"] as? Int ?: 0
                                    
                                    val timeStr = if (lastSyncTime > 0) {
                                        java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(lastSyncTime))
                                    } else "Never"
                                    
                                    lastSyncResult = "🧪 Network Diagnostics:\n" +
                                        "• Network discovery: ${if (discoveredCount > 0) "✅" else "⚠️"} ($discoveredCount devices found)\n" +
                                        "• Network status: $networkStatus\n" +
                                        "• Currently discovering: ${if (isDiscovering) "✅" else "❌"}\n" +
                                        "• Last sync: $timeStr (${if (lastSyncSuccess) "✅ Success" else "❌ Failed"})\n" +
                                        "• Total synced: $totalReceipts receipts, $totalCollections collections\n" +
                                        "• Conflicts resolved: $totalConflicts"
                                        
                                    syncStatus = "Real diagnostics completed"
                                } ?: run {
                                    lastSyncResult = "❌ DeviceDiscoveryHelper not initialized"
                                    syncStatus = "Diagnostic failed"
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🧪 Check Network Status")
                    }
                }
            }
        }

        item {
            // Sync Results Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Last Sync Result",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Text(
                        text = lastSyncResult,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        item {
            // Phase 2 Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "🚀 Phase 2: Network Sync System",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    
                    Text(
                        "✅ DeviceDiscoveryHelper - Network discovery infrastructure\n" +
                        "✅ JSON Sync Protocol - Multi-device data exchange\n" +
                        "✅ Conflict Resolution - Timestamp & version-based\n" +
                        "✅ Network Monitoring - Real-time connection status\n" +
                        "✅ Permissions & Integration - Ready for deployment",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
