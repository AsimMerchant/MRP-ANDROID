package com.example.mobilereceiptprinter

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.remember
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Camera Scanner Screen for QR Code Collection
 * 
 * Features:
 * - Camera preview at top 1/3 of screen
 * - Real-time QR code scanning with ML Kit
 * - Instant scanning without targeting overlay (Paytm-style)
 * - Collection status display and scan history
 * - Permission handling for camera access
 */

data class ScanResult(
    val qrContent: String,
    val timestamp: String,
    val isValid: Boolean,
    val receiptInfo: String,
    val receiptNumber: Int? = null // Receipt number for overlay display
)

// Singleton ML Kit scanner for optimized performance
private object MLKitScanner {
    private var _scanner: com.google.mlkit.vision.barcode.BarcodeScanner? = null
    
    val scanner: com.google.mlkit.vision.barcode.BarcodeScanner
        get() {
            if (_scanner == null) {
                val options = BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                    .build()
                _scanner = BarcodeScanning.getClient(options)
            }
            return _scanner!!
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScannerScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    
    // Create ViewModel with dependencies
    val database = AppDatabase.getDatabase(context)
    val deviceManager = DeviceManager(context)
    val scannerViewModel = remember {
        ScannerViewModel(database, deviceManager)
    }
    
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == 
            PackageManager.PERMISSION_GRANTED
        )
    }
    
    // Flashlight state management
    var isFlashlightOn by remember { mutableStateOf(false) }
    
    val scanResults by scannerViewModel.scanResults.collectAsState()
    val isScanning by scannerViewModel.isScanning.collectAsState()
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }
    
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("QR Code Scanner") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 1/3 screen - Camera preview (at top)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                if (hasCameraPermission) {
                    CameraPreview(
                        onQRCodeDetected = { qrContent ->
                            scannerViewModel.processScan(qrContent)
                        },
                        isFlashlightOn = isFlashlightOn,
                        onFlashlightToggle = { isFlashlightOn = !isFlashlightOn },
                        scannerViewModel = scannerViewModel // Phase 4.1: Pass ViewModel for overlay/haptic
                    )
                } else {
                    // Permission denied state
                    Card(
                        modifier = Modifier.fillMaxSize(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = "Camera Permission Required",
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "Camera Permission Required",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            Text(
                                text = "Allow camera access to scan QR codes",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Text("Grant Permission")
                            }
                        }
                    }
                }
            }
            
            // 2/3 screen - Scan results and instructions (at bottom)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(2f)
                    .padding(16.dp)
            ) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Receipt Collection Scanner",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Instant QR scanning - no targeting required",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                    
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recent Scans",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            
                            if (isScanning) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Text(
                                        text = "Scanning...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                    
                    if (scanResults.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                )
                            ) {
                                Text(
                                    text = "No scans yet. Point camera at any QR code above.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp)
                                )
                            }
                        }
                    } else {
                        items(scanResults) { result ->
                            ScanResultCard(result)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScanResultCard(result: ScanResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (result.isValid) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else 
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (result.isValid) Icons.Default.CheckCircle else Icons.Default.Close,
                contentDescription = if (result.isValid) "Valid" else "Invalid",
                tint = if (result.isValid) 
                    MaterialTheme.colorScheme.primary
                else 
                    MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    text = result.receiptInfo,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = result.timestamp,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun CameraPreview(
    onQRCodeDetected: (String) -> Unit,
    isFlashlightOn: Boolean,
    onFlashlightToggle: () -> Unit,
    scannerViewModel: ScannerViewModel // Phase 4.1: ViewModel for overlay/haptic feedback
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    
    // Phase 4.1: Observe scan status for overlay and haptic feedback
    val showOverlay by scannerViewModel.showOverlay.collectAsState()
    val lastScanStatus by scannerViewModel.lastScanStatus.collectAsState()
    
    // Phase 4.1: Haptic feedback using Vibrator service
    val vibrator = remember { 
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(Vibrator::class.java)
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }
    
    // Phase 4.1: Trigger haptic feedback when scan status changes
    LaunchedEffect(lastScanStatus) {
        lastScanStatus?.let { status ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = when (status) {
                    is ScannerViewModel.ScanStatus.Success -> 
                        VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
                    is ScannerViewModel.ScanStatus.Duplicate -> 
                        VibrationEffect.createWaveform(longArrayOf(0, 30, 50, 30), -1)
                    is ScannerViewModel.ScanStatus.Invalid -> 
                        VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)
                }
                vibrator.vibrate(effect)
            } else {
                // Fallback for older devices
                @Suppress("DEPRECATION")
                vibrator.vibrate(50)
            }
        }
    }
    
    // Store camera reference for flashlight control
    var camera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    
    // Check if camera has flash capability
    var hasFlash by remember { mutableStateOf(false) }
    
    // Create executor for image analysis - tied to composable lifecycle to prevent thread leak
    val imageAnalyzerExecutor = remember { java.util.concurrent.Executors.newSingleThreadExecutor() }
    
    // Comprehensive cleanup when composable leaves composition
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            // Properly cleanup camera resources
            try {
                cameraProvider?.unbindAll()
                camera = null
                cameraProvider = null
            } catch (e: Exception) {
                // Log error but don't crash
                android.util.Log.w("CameraPreview", "Error during camera cleanup: ${e.message}")
            }
            
            // Shutdown executor with timeout to prevent hanging
            try {
                imageAnalyzerExecutor.shutdown()
                if (!imageAnalyzerExecutor.awaitTermination(1000, java.util.concurrent.TimeUnit.MILLISECONDS)) {
                    imageAnalyzerExecutor.shutdownNow()
                }
            } catch (e: Exception) {
                imageAnalyzerExecutor.shutdownNow()
                android.util.Log.w("CameraPreview", "Executor shutdown interrupted: ${e.message}")
            }
        }
    }
    
    // Handle flashlight control with error handling
    LaunchedEffect(isFlashlightOn, camera) {
        camera?.let { cam ->
            try {
                // Check if camera has flash capability
                val cameraInfo = cam.cameraInfo
                hasFlash = cameraInfo.hasFlashUnit()
                
                // Only enable torch if camera has flash
                if (hasFlash) {
                    cam.cameraControl.enableTorch(isFlashlightOn)
                }
            } catch (e: Exception) {
                // Handle flashlight control error silently
                hasFlash = false
            }
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val executor = ContextCompat.getMainExecutor(ctx)
                
                cameraProviderFuture.addListener({
                    try {
                        val provider = cameraProviderFuture.get()
                        cameraProvider = provider // Store reference for cleanup
                        
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        
                        val imageAnalyzer = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also {
                                // Use background thread for better performance
                                it.setAnalyzer(imageAnalyzerExecutor) { imageProxy ->
                                    processImageProxyOptimized(imageProxy, onQRCodeDetected)
                                }
                            }
                        
                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                        
                        // Ensure we unbind any existing use cases first
                        provider.unbindAll()
                        
                        // Bind camera to lifecycle with proper error handling
                        camera = provider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalyzer
                        )
                        
                    } catch (exc: Exception) {
                        // Handle camera binding failure with logging
                        android.util.Log.e("CameraPreview", "Camera binding failed: ${exc.message}", exc)
                        camera = null
                        cameraProvider = null
                    }
                }, executor)
                
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Phase 4.1: Animated overlay for immediate scan feedback (600ms duration)
        AnimatedVisibility(
            visible = showOverlay,
            enter = fadeIn(animationSpec = tween(200)) + scaleIn(
                initialScale = 0.5f,
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ),
            exit = fadeOut(animationSpec = tween(300))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        when (lastScanStatus) {
                            is ScannerViewModel.ScanStatus.Success -> 
                                Color.Green.copy(alpha = 0.2f)
                            is ScannerViewModel.ScanStatus.Duplicate -> 
                                Color.Yellow.copy(alpha = 0.2f)
                            is ScannerViewModel.ScanStatus.Invalid -> 
                                Color.Red.copy(alpha = 0.2f)
                            null -> Color.Transparent
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = when (lastScanStatus) {
                            is ScannerViewModel.ScanStatus.Success -> Icons.Default.CheckCircle
                            is ScannerViewModel.ScanStatus.Duplicate -> Icons.Default.Warning
                            is ScannerViewModel.ScanStatus.Invalid -> Icons.Default.Close
                            null -> Icons.Default.Close
                        },
                        contentDescription = when (lastScanStatus) {
                            is ScannerViewModel.ScanStatus.Success -> "Successfully collected"
                            is ScannerViewModel.ScanStatus.Duplicate -> "Already collected"
                            is ScannerViewModel.ScanStatus.Invalid -> "Invalid receipt"
                            null -> null
                        },
                        tint = when (lastScanStatus) {
                            is ScannerViewModel.ScanStatus.Success -> Color.Green
                            is ScannerViewModel.ScanStatus.Duplicate -> Color.Yellow
                            is ScannerViewModel.ScanStatus.Invalid -> Color.Red
                            null -> Color.White
                        },
                        modifier = Modifier.size(120.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Display receipt number
                    Text(
                        text = when (lastScanStatus) {
                            is ScannerViewModel.ScanStatus.Success -> 
                                "Receipt #${(lastScanStatus as ScannerViewModel.ScanStatus.Success).receiptNumber} Scanned"
                            is ScannerViewModel.ScanStatus.Duplicate -> 
                                "Receipt #${(lastScanStatus as ScannerViewModel.ScanStatus.Duplicate).receiptNumber} Already Collected"
                            is ScannerViewModel.ScanStatus.Invalid -> 
                                "Invalid Receipt"
                            null -> ""
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        color = when (lastScanStatus) {
                            is ScannerViewModel.ScanStatus.Success -> Color.Green
                            is ScannerViewModel.ScanStatus.Duplicate -> Color.Yellow
                            is ScannerViewModel.ScanStatus.Invalid -> Color.Red
                            null -> Color.White
                        },
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        
        // Flashlight toggle button (only show if camera has flash)
        if (hasFlash) {
            FloatingActionButton(
                onClick = onFlashlightToggle,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                containerColor = if (isFlashlightOn) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                contentColor = if (isFlashlightOn) 
                    MaterialTheme.colorScheme.onPrimary 
                else 
                    MaterialTheme.colorScheme.onSurface
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = if (isFlashlightOn) "Turn flashlight off" else "Turn flashlight on"
                )
            }
        }
    }
}

// Optimized image processing with singleton scanner
private fun processImageProxyOptimized(
    imageProxy: ImageProxy,
    onQRCodeDetected: (String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        
        // Use singleton scanner with QR-only detection
        MLKitScanner.scanner.process(image)
            .addOnSuccessListener { barcodes ->
                // Process only first QR code for better performance
                barcodes.firstOrNull()?.displayValue?.let { qrContent ->
                    onQRCodeDetected(qrContent)
                }
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}





