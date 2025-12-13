package com.example.mobilereceiptprinter

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Collection Projects Screen
 * 
 * Displays list of all collection projects with summary statistics
 * Allows creating new projects and selecting active project
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionProjectsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToProjectDetails: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val database = AppDatabase.getDatabase(context)
    val scope = rememberCoroutineScope()
    
    var projectSummaries by remember { mutableStateOf<List<ProjectSummary>>(emptyList()) }
    var activeProjectId by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isCreating by remember { mutableStateOf(false) }
    var showSelectionDialog by remember { mutableStateOf(false) }
    var selectedProject by remember { mutableStateOf<ProjectSummary?>(null) }
    
    // Function to refresh project list
    fun refreshProjects() {
        scope.launch {
            try {
                val summaries = withContext(Dispatchers.IO) {
                    database.collectionProjectDao().getProjectSummaries()
                }
                val activeId = ActiveProjectSettings.getActiveProjectId(context)
                
                projectSummaries = summaries
                activeProjectId = activeId
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    // Function to switch active project
    fun switchActiveProject(projectId: String) {
        ActiveProjectSettings.setActiveProjectId(context, projectId)
        activeProjectId = projectId
        showSelectionDialog = false
    }
    
    // Function to create new project
    fun createNewProject() {
        scope.launch {
            try {
                isCreating = true
                
                // Get current project count to generate next number
                val projectCount = withContext(Dispatchers.IO) {
                    database.collectionProjectDao().getProjectCount()
                }
                
                // Auto-generate project name: "Project 1", "Project 2", etc.
                val projectName = "Project ${projectCount + 1}"
                
                // Get current date and time
                val currentTime = System.currentTimeMillis()
                val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                val timeFormat = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                val now = java.util.Date(currentTime)
                
                // Get device manager for deviceId
                val deviceManager = DeviceManager(context)
                
                // Create new project
                val newProject = CollectionProject(
                    name = projectName,
                    createdDate = dateFormat.format(now),
                    createdTime = timeFormat.format(now),
                    deviceId = deviceManager.getDeviceId(),
                    syncStatus = "PENDING",
                    lastModified = currentTime
                )
                
                // Insert project into database
                withContext(Dispatchers.IO) {
                    database.collectionProjectDao().insert(newProject)
                }
                
                // Set as active project automatically
                ActiveProjectSettings.setActiveProjectId(context, newProject.id)
                
                // Refresh project list
                refreshProjects()
            } catch (e: Exception) {
                // Handle error
            } finally {
                isCreating = false
            }
        }
    }
    
    // Load projects and active project ID
    LaunchedEffect(Unit) {
        refreshProjects()
        isLoading = false
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Collection Projects") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Active project indicator
                item {
                    if (activeProjectId != null) {
                        val activeProject = projectSummaries.find { it.projectId == activeProjectId }
                        if (activeProject != null) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = "🎯 Active Project",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = activeProject.projectName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Active",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Create new project button
                item {
                    Divider()
                }
                
                item {
                    Button(
                        onClick = { createNewProject() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isCreating
                    ) {
                        if (isCreating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(if (isCreating) "Creating..." else "+ Create New Project")
                    }
                }
                
                item {
                    Divider()
                }
                
                // Empty state
                if (projectSummaries.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "📦",
                                    style = MaterialTheme.typography.displayMedium
                                )
                                Text(
                                    text = "No projects created yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Create a project to start organizing your collections",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    // Project list
                    items(projectSummaries) { project ->
                        ProjectCard(
                            project = project,
                            isActive = project.projectId == activeProjectId,
                            onClick = {
                                if (project.projectId != activeProjectId) {
                                    selectedProject = project
                                    showSelectionDialog = true
                                }
                            },
                            onViewDetails = { onNavigateToProjectDetails(project.projectId) }
                        )
                    }
                }
            }
        }
    }
    
    // Project selection dialog
    if (showSelectionDialog && selectedProject != null) {
        AlertDialog(
            onDismissRequest = { showSelectionDialog = false },
            title = { Text("Switch Project?") },
            text = {
                Column {
                    Text("Set ${selectedProject!!.projectName} as active project?")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "All future collections will be added to this project.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = selectedProject!!.projectName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${selectedProject!!.receiptCount} receipts",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "₹${String.format("%.2f", selectedProject!!.totalAmount)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { switchActiveProject(selectedProject!!.projectId) }
                ) {
                    Text("Set as Active")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSelectionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ProjectCard(
    project: ProjectSummary,
    isActive: Boolean,
    onClick: () -> Unit,
    onViewDetails: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) 
                MaterialTheme.colorScheme.secondaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Project name with active indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📦 ${project.projectName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (isActive) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Active",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            // Statistics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "${project.receiptCount} receipts",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Total: ₹${String.format("%.2f", project.totalAmount)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Created:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = project.createdDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = project.createdTime,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // View details button
            TextButton(
                onClick = onViewDetails,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("View Details →")
            }
        }
    }
}

/**
 * Project Details Screen
 * 
 * Shows detailed view of a specific project including:
 * - Project summary statistics
 * - List of all receipts collected in this project
 * - Option to set as active project
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailsScreen(
    projectId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val database = AppDatabase.getDatabase(context)
    val scope = rememberCoroutineScope()
    
    var project by remember { mutableStateOf<CollectionProject?>(null) }
    var receipts by remember { mutableStateOf<List<CollectedReceiptWithDetails>>(emptyList()) }
    var receiptCount by remember { mutableStateOf(0) }
    var totalAmount by remember { mutableStateOf(0.0) }
    var isActive by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Load project details
    LaunchedEffect(projectId) {
        scope.launch {
            try {
                val projectData = withContext(Dispatchers.IO) {
                    database.collectionProjectDao().getProjectById(projectId)
                }
                val receiptsList = withContext(Dispatchers.IO) {
                    database.collectionProjectDao().getReceiptsForProject(projectId)
                }
                val count = withContext(Dispatchers.IO) {
                    database.collectionProjectDao().getReceiptCountForProject(projectId)
                }
                val amount = withContext(Dispatchers.IO) {
                    database.collectionProjectDao().getTotalAmountForProject(projectId)
                }
                val activeId = ActiveProjectSettings.getActiveProjectId(context)
                
                project = projectData
                receipts = receiptsList
                receiptCount = count
                totalAmount = amount
                isActive = activeId == projectId
            } catch (e: Exception) {
                // Handle error
            } finally {
                isLoading = false
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(project?.name ?: "Project Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (project != null) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Project summary card
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isActive) 
                                MaterialTheme.colorScheme.primaryContainer 
                            else 
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📊 Project Summary",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (isActive) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Active",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "Active",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                            
                            Divider()
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Receipts Collected",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "$receiptCount",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Total Amount",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "₹${String.format("%.2f", totalAmount)}",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Created",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${project!!.createdDate} ${project!!.createdTime}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                            
                            // Set as active button (only if not already active)
                            if (!isActive) {
                                Button(
                                    onClick = {
                                        ActiveProjectSettings.setActiveProjectId(context, projectId)
                                        isActive = true
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Set as Active Project")
                                }
                            }
                        }
                    }
                }
                
                // Receipts section header
                item {
                    Text(
                        text = "📝 Collected Receipts ($receiptCount)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Receipts list
                if (receipts.isEmpty()) {
                    item {
                        Card(
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
                                    text = "📭",
                                    style = MaterialTheme.typography.displaySmall
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No receipts collected yet",
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Start collecting to see receipts here",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(receipts) { receipt ->
                        Card {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Receipt #${receipt.receiptNumber}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "₹${receipt.amount}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = "Biller: ${receipt.biller}",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = "Volunteer: ${receipt.volunteer}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "Collected: ${receipt.collectionDate}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = receipt.collectionTime,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "By: ${receipt.scannedBy}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
