package com.example.mobilereceiptprinter

import android.net.nsd.NsdServiceInfo
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
    val discoveredDevices = remember { mutableStateListOf<NsdServiceInfo>() }

    // Start discovery on enter, stop on exit
    LaunchedEffect(Unit) {
        discoveryHelper.discoverServices { device ->
            if (discoveredDevices.none { it.serviceName == device.serviceName }) {
                discoveredDevices.add(device)
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose { discoveryHelper.stopDiscovery() }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Discovered Devices") })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (discoveredDevices.isEmpty()) {
                item {
                    Text("No devices found.", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                items(discoveredDevices) { device ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Name: ${device.serviceName}", style = MaterialTheme.typography.titleMedium)
                            Text("Host: ${device.host}", style = MaterialTheme.typography.bodySmall)
                            Text("Port: ${device.port}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}