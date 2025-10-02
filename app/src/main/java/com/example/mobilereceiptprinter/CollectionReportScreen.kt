package com.example.mobilereceiptprinter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Collection Report Screen - Phase 4
 * 
 * Shows summary of collected receipts including:
 * - Total number of receipts collected
 * - Total amount collected  
 * - List of collected receipts with details
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionReportScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val database = AppDatabase.getDatabase(context)
    
    var collectionsWithDetails by remember { mutableStateOf<List<CollectedReceiptWithDetails>>(emptyList()) }
    var uncollectedReceipts by remember { mutableStateOf<List<Receipt>>(emptyList()) }
    
    // Audit data
    var totalReceiptsCount by remember { mutableStateOf(0) }
    var totalReceiptsAmount by remember { mutableStateOf(0.0) }
    var collectedCount by remember { mutableStateOf(0) }
    var collectedAmount by remember { mutableStateOf(0.0) }
    var uncollectedCount by remember { mutableStateOf(0) }
    var uncollectedAmount by remember { mutableStateOf(0.0) }
    
    var selectedTabIndex by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }

    // Load audit data
    LaunchedEffect(Unit) {
        try {
            // Clean up orphaned collection records first
            withContext(Dispatchers.IO) {
                database.collectedReceiptDao().cleanupOrphanedCollections()
            }
            
            // Load all audit data
            val collections = withContext(Dispatchers.IO) {
                database.collectedReceiptDao().getCollectedReceiptsWithDetails()
            }
            val uncollected = withContext(Dispatchers.IO) {
                database.receiptDao().getUncollectedReceiptsList()
            }
            
            // Audit statistics
            val totalCount = withContext(Dispatchers.IO) {
                database.receiptDao().getTotalReceiptsCount()
            }
            val totalAmount = withContext(Dispatchers.IO) {
                database.receiptDao().getTotalReceiptsAmount() ?: 0.0
            }
            val collCount = withContext(Dispatchers.IO) {
                database.collectedReceiptDao().getCollectedReceiptsCount()
            }
            val collAmount = withContext(Dispatchers.IO) {
                database.collectedReceiptDao().getTotalCollectedAmount() ?: 0.0
            }
            val uncollCount = withContext(Dispatchers.IO) {
                database.receiptDao().getUncollectedReceiptsCount()
            }
            val uncollAmount = withContext(Dispatchers.IO) {
                database.receiptDao().getUncollectedReceiptsAmount() ?: 0.0
            }
            
            // Update state
            collectionsWithDetails = collections
            uncollectedReceipts = uncollected
            totalReceiptsCount = totalCount
            totalReceiptsAmount = totalAmount
            collectedCount = collCount
            collectedAmount = collAmount
            uncollectedCount = uncollCount
            uncollectedAmount = uncollAmount
        } catch (e: Exception) {
            // Handle error - could show error message
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Collection Report") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                // Audit Summary Cards
                Column {
                    // Main audit summary
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "📊 Collection Audit",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            val collectionPercentage = if (totalReceiptsCount > 0) {
                                (collectedCount * 100.0 / totalReceiptsCount)
                            } else 0.0
                            
                            Text(
                                text = "${String.format("%.1f", collectionPercentage)}% Collection Rate",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (collectionPercentage >= 90) MaterialTheme.colorScheme.primary 
                                        else if (collectionPercentage >= 70) MaterialTheme.colorScheme.secondary
                                        else MaterialTheme.colorScheme.error
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Total Receipts", style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        text = "$totalReceiptsCount",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "₹${String.format("%.2f", totalReceiptsAmount)}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Collected", style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        text = "$collectedCount",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "₹${String.format("%.2f", collectedAmount)}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Missing", style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        text = "$uncollectedCount",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (uncollectedCount > 0) MaterialTheme.colorScheme.error 
                                                else MaterialTheme.colorScheme.outline
                                    )
                                    Text(
                                        text = "₹${String.format("%.2f", uncollectedAmount)}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Tab Row
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = { Text("Collected ($collectedCount)") }
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = { Text("Missing ($uncollectedCount)") }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tab Content
                when (selectedTabIndex) {
                    0 -> {
                        // Collected Receipts Tab
                        if (collectionsWithDetails.isEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "✅",
                                        style = MaterialTheme.typography.displayMedium
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "No receipts collected yet",
                                        style = MaterialTheme.typography.bodyLarge,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "Use the QR Scanner to collect receipts",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(collectionsWithDetails) { collection ->
                                    CollectedReceiptCard(collection = collection)
                                }
                            }
                        }
                    }
                    1 -> {
                        // Uncollected Receipts Tab
                        if (uncollectedReceipts.isEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "🎉",
                                        style = MaterialTheme.typography.displayMedium
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "All receipts collected!",
                                        style = MaterialTheme.typography.bodyLarge,
                                        textAlign = TextAlign.Center,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Great job! No missing receipts found.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(uncollectedReceipts) { receipt ->
                                    UncollectedReceiptCard(receipt = receipt)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UncollectedReceiptCard(receipt: Receipt) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with receipt number and amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚠️",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Receipt #${receipt.receiptNumber}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = receipt.biller,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "₹${receipt.amount}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            
            // Receipt details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Created: ${receipt.date}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "at ${receipt.time}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Volunteer: ${receipt.volunteer}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Status: NOT COLLECTED",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun CollectedReceiptCard(collection: CollectedReceiptWithDetails) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with receipt number and amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "Receipt #${collection.receiptNumber}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = collection.biller,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "₹${collection.amount}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            
            // Collection details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Collected on ${collection.collectionDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "at ${collection.collectionTime}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "by ${collection.scannedBy}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = collection.volunteer,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}