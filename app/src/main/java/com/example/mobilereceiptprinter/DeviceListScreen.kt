package com.example.mobilereceiptprinter

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceListScreen(discoveryHelper: DeviceDiscoveryHelper) {
    // Use the global device list that's maintained throughout app lifecycle
    val discoveredDevices = discoveryHelper.globalDiscoveredDevices
    
    // No local discovery needed - using global discovery started at app launch

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Show Discovered Devices") })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Debug info
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Debug Info:", style = MaterialTheme.typography.titleSmall)
                        Text(discoveryHelper.getServiceInfo(), style = MaterialTheme.typography.bodySmall)
                        Text("Devices found: ${discoveredDevices.size}", style = MaterialTheme.typography.bodySmall)
                        Text(discoveryHelper.getDiscoveryStatus(), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            
            if (discoveredDevices.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("No devices found yet.", style = MaterialTheme.typography.bodyLarge)
                            Text("Real-time discovery active.", style = MaterialTheme.typography.bodyMedium)
                            Text("Make sure:", style = MaterialTheme.typography.titleSmall)
                            Text("• Both devices are on the same WiFi", style = MaterialTheme.typography.bodySmall)
                            Text("• Both devices have the MRP app open", style = MaterialTheme.typography.bodySmall)
                            Text("• Devices appear instantly and disappear within 5 seconds", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            } else {
                items(discoveredDevices) { device ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Name: ${device.name}", style = MaterialTheme.typography.titleMedium)
                            Text("Host: ${device.address}", style = MaterialTheme.typography.bodySmall)
                            Text("Port: ${device.port}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}