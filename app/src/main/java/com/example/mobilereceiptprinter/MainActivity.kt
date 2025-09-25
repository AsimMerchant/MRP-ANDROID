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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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
import com.example.mobilereceiptprinter.AppDatabase
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding

sealed class Screen(val route: String) {
    object Landing : Screen("landing")
    object Receipt : Screen("receipt")
    object Reports : Screen("reports")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MobileReceiptPrinterTheme {
                MainApp()
            }
        }
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
        }
    }
}

@Composable
fun LandingScreen(navController: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showClearSuggestionsDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .windowInsetsPadding(WindowInsets.systemBars),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }

        item {
            Text(
                "Mobile Receipt Printer",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "Version 11",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }

        item {
            Button(
                onClick = { navController.navigate(Screen.Receipt.route) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Create Receipt", style = MaterialTheme.typography.bodyLarge)
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
    val currentDate = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    val currentTime = remember { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()) }
    var showBluetoothDevices by remember { mutableStateOf(false) }
    var printStatus by remember { mutableStateOf("") }
    var showShareOptions by remember { mutableStateOf(false) }
    var savedPrinterAddress by remember { mutableStateOf(prefs.getString("saved_printer_address", null)) }
    var bluetoothPermissionGranted by remember { mutableStateOf(false) }

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

    // Receipt format matching response.php exactly with proper formatting
    val receiptText = """
=======================
\\u001B\\u0021\\u0030 RECEIPT #$receiptNumber \\u001B\\u0021\\u0000
=======================
Date: $currentDate
Time: $currentTime
-----------------------
Biller: $biller
-----------------------
Volunteer: $volunteer
-----------------------
\\u001B\\u0021\\u0030AMOUNT: Rs. $amount\\u001B\\u0021\\u0000
=======================




""".trimIndent()

    // Clean version for preview display (without ESC/POS commands)
    val receiptPreviewText = """

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
        val connected = printerHelper.connectToDevice(device)
        if (connected) {
            val printed = printerHelper.printText(receiptText)
            printStatus = if (printed) "Printed successfully!" else "Print failed."
            printerHelper.closeConnection()
            saveLastPrinter(device.address)

            // Clear volunteer and amount for next receipt
            if (printed) {
                volunteer = ""
                amount = ""
                showPreview = false
                printStatus = "✓ Printed! Ready for next receipt."
            }
        } else {
            printStatus = "Could not connect to printer."
        }
    }

    // Auto-save receipt when created
    fun createAndSaveReceipt() {
        if (biller.isNotBlank()) {
            receiptNumber = getNextReceiptNumber(biller)
        }
        showPreview = true

        // Auto-save the receipt
        val amountValue = amount.toDoubleOrNull() ?: 0.0
        val receipt = Receipt(
            receiptNumber = receiptNumber,
            biller = biller,
            volunteer = volunteer,
            amount = amount,
            date = currentDate,
            time = currentTime
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
            billerSuggestions.addAll(billers)
            volunteerSuggestions.clear()
            volunteerSuggestions.addAll(volunteers)
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
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .windowInsetsPadding(WindowInsets.systemBars),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "form_card") {
            // Form card
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
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
                            createAndSaveReceipt()
                            focusManager.clearFocus()

                            scope.launch {
                                val db = AppDatabase.getDatabase(context)
                                val billers = withContext(Dispatchers.IO) { db.suggestionDao().getAllBillerSuggestions() }
                                val volunteers = withContext(Dispatchers.IO) { db.suggestionDao().getAllVolunteerSuggestions() }
                                billerSuggestions.clear()
                                billerSuggestions.addAll(billers)
                                volunteerSuggestions.clear()
                                volunteerSuggestions.addAll(volunteers)
                            }
                        },
                        enabled = isFormValid,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Create Receipt", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }

        if (showPreview) {
            item(key = "receipt_preview") {
                ReceiptPreviewCard(
                    receiptPreviewText = receiptPreviewText
                )
            }

            item(key = "print_actions") {
                PrintActionsCard(
                    savedPrinterAddress = savedPrinterAddress,
                    bluetoothPermissionGranted = bluetoothPermissionGranted,
                    onPrint = {
                        if (!bluetoothPermissionGranted) {
                            printStatus = "Bluetooth permission not granted."
                            return@PrintActionsCard
                        }

                        if (savedPrinterAddress != null) {
                            val savedDevice = printerHelper.getPairedDevices()?.find { it.address == savedPrinterAddress }
                            if (savedDevice != null) {
                                printToDevice(savedDevice)
                                return@PrintActionsCard
                            }
                        }

                        showBluetoothDevices = true
                        printStatus = ""
                    }
                )
            }

            // Status messages
            if (printStatus.isNotEmpty()) {
                item(key = "print_status") {
                    PrintStatusCard(printStatus = printStatus)
                }
            }

            // Bluetooth device selection
            if (showBluetoothDevices) {
                item(key = "bluetooth_devices") {
                    BluetoothDeviceSelectionCard(
                        printerHelper = printerHelper,
                        context = context,
                        onDeviceSelected = { device ->
                            showBluetoothDevices = false
                            printToDevice(device)
                        },
                        onDismiss = { showBluetoothDevices = false }
                    )
                }
            }
        }

        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(20.dp))
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
                Text(
                    text = receiptPreviewText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    ),
                    modifier = Modifier.padding(16.dp)
                )
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
fun EditReceiptDialog(
    receipt: Receipt,
    onDismiss: () -> Unit,
    onSave: (Receipt) -> Unit,
    onDelete: (Receipt) -> Unit
) {
    var biller by remember { mutableStateOf(receipt.biller) }
    var volunteer by remember { mutableStateOf(receipt.volunteer) }
    var amount by remember { mutableStateOf(receipt.amount) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    // Memoize form validation
    val isFormValid = remember(biller, volunteer, amount) {
        biller.isNotBlank() && volunteer.isNotBlank() && amount.isNotBlank()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Edit Receipt #${receipt.receiptNumber}")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = biller,
                    onValueChange = { biller = it },
                    label = { Text("Biller Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = volunteer,
                    onValueChange = { volunteer = it },
                    label = { Text("Volunteer Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Amount (Rs.)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text(
                    text = "Date: ${receipt.date} • Time: ${receipt.time}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = { showDeleteConfirmation = true },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }

                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        val updatedReceipt = receipt.copy(
                            biller = biller,
                            volunteer = volunteer,
                            amount = amount
                        )
                        onSave(updatedReceipt)
                    },
                    enabled = isFormValid
                ) {
                    Text("Save")
                }
            }
        }
    )

    // Delete confirmation dialog
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Receipt") },
            text = { Text("Are you sure you want to delete Receipt #${receipt.receiptNumber}? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete(receipt)
                        showDeleteConfirmation = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ReportsScreen(navController: NavHostController) {
    val context = LocalContext.current
    val receipts = remember { mutableStateListOf<Receipt>() }
    val billers = remember { mutableStateListOf<String>() }
    val scope = rememberCoroutineScope()
    var selectedBiller by remember { mutableStateOf<String?>(null) }
    var editingReceipt by remember { mutableStateOf<Receipt?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteBillerDialog by remember { mutableStateOf(false) }
    var billerToDelete by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        scope.launch {
            val db = AppDatabase.getDatabase(context)
            val allReceipts = withContext(Dispatchers.IO) { db.receiptDao().getAllReceipts() }
            val allBillers = withContext(Dispatchers.IO) { db.receiptDao().getAllBillers() }
            receipts.clear()
            receipts.addAll(allReceipts)
            billers.clear()
            billers.addAll(allBillers)
        }
    }

    // Memoize expensive calculations
    val receiptsByBiller = remember(receipts.size) {
        receipts.groupBy { it.biller }
    }

    val billerEntries = remember(receiptsByBiller) {
        receiptsByBiller.entries.toList()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .windowInsetsPadding(WindowInsets.systemBars),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "header") {
            Text(
                "Receipt Reports",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (billerEntries.isEmpty()) {
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
            // Use itemsIndexed with stable keys for better performance
            itemsIndexed(
                items = billerEntries,
                key = { _, (biller, _) -> "biller_$biller" }
            ) { _, (biller, billerReceipts) ->
                BillerCard(
                    biller = biller,
                    billerReceipts = billerReceipts,
                    onDeleteAll = {
                        billerToDelete = biller
                        showDeleteBillerDialog = true
                    },
                    onEditReceipt = { receipt ->
                        editingReceipt = receipt
                        showEditDialog = true
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
                Text("Are you sure you want to delete ALL receipts for \"$billerToDelete\"? This will permanently remove ${receiptsByBiller[billerToDelete]?.size ?: 0} receipts. The next receipt for this biller will start from #1.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val db = AppDatabase.getDatabase(context)
                            withContext(Dispatchers.IO) {
                                // Delete all receipts for this biller
                                db.receiptDao().deleteAllReceiptsFromBiller(billerToDelete!!)
                            }

                            // Reset the biller's receipt count in SharedPreferences so next receipt starts from #1
                            val billerPrefs = context.getSharedPreferences("biller_$billerToDelete", Context.MODE_PRIVATE)
                            billerPrefs.edit {
                                putInt("receipt_count", 0)
                                putFloat("total_amount", 0f)
                            }

                            // Refresh the list
                            val allReceipts = withContext(Dispatchers.IO) { db.receiptDao().getAllReceipts() }
                            val allBillers = withContext(Dispatchers.IO) { db.receiptDao().getAllBillers() }
                            receipts.clear()
                            receipts.addAll(allReceipts)
                            billers.clear()
                            billers.addAll(allBillers)
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

    // Edit Dialog
    if (showEditDialog && editingReceipt != null) {
        EditReceiptDialog(
            receipt = editingReceipt!!,
            onDismiss = {
                showEditDialog = false
                editingReceipt = null
            },
            onSave = { updatedReceipt ->
                scope.launch {
                    val db = AppDatabase.getDatabase(context)
                    withContext(Dispatchers.IO) {
                        db.receiptDao().update(updatedReceipt)
                    }
                    // Refresh the list
                    val allReceipts = withContext(Dispatchers.IO) { db.receiptDao().getAllReceipts() }
                    receipts.clear()
                    receipts.addAll(allReceipts)
                    showEditDialog = false
                    editingReceipt = null
                }
            },
            onDelete = { receiptToDelete ->
                scope.launch {
                    val db = AppDatabase.getDatabase(context)
                    withContext(Dispatchers.IO) {
                        db.receiptDao().delete(receiptToDelete)
                    }
                    // Refresh the list
                    val allReceipts = withContext(Dispatchers.IO) { db.receiptDao().getAllReceipts() }
                    val allBillers = withContext(Dispatchers.IO) { db.receiptDao().getAllBillers() }
                    receipts.clear()
                    receipts.addAll(allReceipts)
                    billers.clear()
                    billers.addAll(allBillers)
                    showEditDialog = false
                    editingReceipt = null
                }
            }
        )
    }
}

// Extracted optimized BillerCard component to reduce recomposition
@Composable
private fun BillerCard(
    biller: String,
    billerReceipts: List<Receipt>,
    onDeleteAll: () -> Unit,
    onEditReceipt: (Receipt) -> Unit
) {
    // Memoize total calculation
    val total = remember(billerReceipts) {
        billerReceipts.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Biller header with total and delete button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = biller,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${billerReceipts.size} receipts",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Text(
                            text = "Total: Rs. ${String.format(Locale.getDefault(), "%.2f", total)}",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    // Delete Biller Button
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
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Use items for individual receipts with stable keys
            billerReceipts.forEach { receipt ->
                key(receipt.id) {
                    ReceiptCard(
                        receipt = receipt,
                        onEdit = { onEditReceipt(receipt) }
                    )
                }
            }
        }
    }
}

// Extracted optimized ReceiptCard component
@Composable
private fun ReceiptCard(
    receipt: Receipt,
    onEdit: () -> Unit
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
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

            // Edit button
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Receipt",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
