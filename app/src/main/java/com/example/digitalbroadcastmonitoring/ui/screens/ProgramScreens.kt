package com.example.digitalbroadcastmonitoring.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.digitalbroadcastmonitoring.data.model.Program
import com.example.digitalbroadcastmonitoring.data.model.ProgramCategory
import com.example.digitalbroadcastmonitoring.data.model.StreamStatus
import com.example.digitalbroadcastmonitoring.data.repository.BroadcastRepository
import com.example.digitalbroadcastmonitoring.ui.components.BroadcastTextField
import com.example.digitalbroadcastmonitoring.ui.components.DeleteConfirmationDialog
import com.example.digitalbroadcastmonitoring.ui.components.StatusBadge
import com.example.digitalbroadcastmonitoring.ui.theme.*
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProgramScreen(
    onNavigateBack: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var channel by remember { mutableStateOf("DD National HD") }
    var selectedCategory by remember { mutableStateOf(ProgramCategory.NEWS) }
    var language by remember { mutableStateOf("English / Hindi") }
    var durationText by remember { mutableStateOf("60") }
    var description by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add New Program Feed", style = MaterialTheme.typography.titleLarge, color = TextWhite) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, BorderStroke)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    BroadcastTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = "Program Title *",
                        placeholder = "e.g. Evening Prime Bulletin"
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    BroadcastTextField(
                        value = channel,
                        onValueChange = { channel = it },
                        label = "Channel / Stream Identifier *",
                        placeholder = "e.g. DD National HD"
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Program Category *", style = MaterialTheme.typography.labelMedium, color = TextWhite)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ProgramCategory.values().take(3).forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat.displayName) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryBlue,
                                    selectedLabelColor = TextWhite,
                                    containerColor = DarkSecondarySurface,
                                    labelColor = TextMuted
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ProgramCategory.values().drop(3).forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat.displayName) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryBlue,
                                    selectedLabelColor = TextWhite,
                                    containerColor = DarkSecondarySurface,
                                    labelColor = TextMuted
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    BroadcastTextField(
                        value = language,
                        onValueChange = { language = it },
                        label = "Broadcast Language",
                        placeholder = "e.g. Hindi / English"
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    BroadcastTextField(
                        value = durationText,
                        onValueChange = { durationText = it },
                        label = "Nominal Duration (Minutes) *",
                        placeholder = "60"
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    BroadcastTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = "Description / Content Summary",
                        placeholder = "Transmission synopsis..."
                    )

                    if (errorMessage.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(errorMessage, color = AnomalyRed, style = MaterialTheme.typography.bodyMedium)
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            val duration = durationText.toIntOrNull()
                            if (title.isBlank() || channel.isBlank()) {
                                errorMessage = "Title and channel are required."
                            } else if (duration == null || duration <= 0) {
                                errorMessage = "Duration must be a positive integer."
                            } else {
                                val newProg = Program(
                                    id = "prog-" + UUID.randomUUID().toString().take(6),
                                    title = title.trim(),
                                    channel = channel.trim(),
                                    category = selectedCategory,
                                    language = language.trim(),
                                    durationMinutes = duration,
                                    description = description.trim(),
                                    videoStatus = StreamStatus.OPTIMAL,
                                    audioStatus = StreamStatus.OPTIMAL,
                                    complianceScore = 98
                                )
                                BroadcastRepository.addProgram(newProg)
                                onNavigateBack()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Text("Save & Register Program", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgramDetailScreen(
    programId: String,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    onNavigateToAnalysis: (String) -> Unit
) {
    val program = BroadcastRepository.getProgramById(programId)
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Program Details", style = MaterialTheme.typography.titleLarge, color = TextWhite) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigateToEdit(programId) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Program", tint = PrimaryBlueVariant)
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Program", tint = AnomalyRed)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        if (program == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Program record not found.", color = TextMuted)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, BorderStroke)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = program.channel,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBlueVariant
                            )
                            StatusBadge(text = program.category.displayName, status = StreamStatus.OPTIMAL)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = program.title,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = program.description.ifEmpty { "No detailed synopsis entered for this broadcast asset." },
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextMuted
                        )

                        Spacer(modifier = Modifier.height(18.dp))
                        HorizontalDivider(color = BorderStroke)
                        Spacer(modifier = Modifier.height(18.dp))

                        // Technical Specs
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Language", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                Text(program.language, style = MaterialTheme.typography.bodyMedium, color = TextWhite)
                            }
                            Column {
                                Text("Duration", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                Text("${program.durationMinutes} minutes", style = MaterialTheme.typography.bodyMedium, color = TextWhite)
                            }
                            Column {
                                Text("Compliance", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                Text("${program.complianceScore}%", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = SuccessGreen)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Video & Audio Signals
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = DarkSecondarySurface)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Videocam, contentDescription = null, tint = SuccessGreen)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("Video Feed", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                        Text(program.videoStatus.displayName, style = MaterialTheme.typography.bodyMedium, color = TextWhite)
                                    }
                                }
                            }

                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = DarkSecondarySurface)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = SuccessGreen)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("Audio Loudness", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                        Text(program.audioStatus.displayName, style = MaterialTheme.typography.bodyMedium, color = TextWhite)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = { onNavigateToAnalysis(program.id) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                        ) {
                            Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Inspect Rule & ML Telemetry", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }

        if (showDeleteDialog) {
            DeleteConfirmationDialog(
                title = "Delete Program Record",
                message = "Are you sure you want to remove '${program?.title}'? This will also remove any linked schedule slots.",
                onConfirm = {
                    showDeleteDialog = false
                    BroadcastRepository.deleteProgram(programId)
                    onNavigateBack()
                },
                onDismiss = { showDeleteDialog = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProgramScreen(
    programId: String,
    onNavigateBack: () -> Unit
) {
    val existing = BroadcastRepository.getProgramById(programId)

    var title by remember(existing) { mutableStateOf(existing?.title ?: "") }
    var channel by remember(existing) { mutableStateOf(existing?.channel ?: "") }
    var selectedCategory by remember(existing) { mutableStateOf(existing?.category ?: ProgramCategory.NEWS) }
    var language by remember(existing) { mutableStateOf(existing?.language ?: "") }
    var durationText by remember(existing) { mutableStateOf(existing?.durationMinutes?.toString() ?: "60") }
    var description by remember(existing) { mutableStateOf(existing?.description ?: "") }
    var errorMessage by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Program", style = MaterialTheme.typography.titleLarge, color = TextWhite) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, BorderStroke)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    BroadcastTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = "Program Title *"
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    BroadcastTextField(
                        value = channel,
                        onValueChange = { channel = it },
                        label = "Channel Identifier *"
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    BroadcastTextField(
                        value = language,
                        onValueChange = { language = it },
                        label = "Language"
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    BroadcastTextField(
                        value = durationText,
                        onValueChange = { durationText = it },
                        label = "Nominal Duration (Minutes) *"
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    BroadcastTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = "Description"
                    )

                    if (errorMessage.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(errorMessage, color = AnomalyRed, style = MaterialTheme.typography.bodyMedium)
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            val duration = durationText.toIntOrNull()
                            if (title.isBlank() || channel.isBlank()) {
                                errorMessage = "Title and channel are required."
                            } else if (duration == null || duration <= 0) {
                                errorMessage = "Duration must be a positive integer."
                            } else if (existing != null) {
                                val updated = existing.copy(
                                    title = title.trim(),
                                    channel = channel.trim(),
                                    category = selectedCategory,
                                    language = language.trim(),
                                    durationMinutes = duration,
                                    description = description.trim()
                                )
                                BroadcastRepository.updateProgram(updated)
                                onNavigateBack()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Text("Save Changes", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
