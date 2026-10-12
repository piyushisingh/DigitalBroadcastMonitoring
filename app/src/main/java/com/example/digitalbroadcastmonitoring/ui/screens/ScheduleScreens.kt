package com.example.digitalbroadcastmonitoring.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.digitalbroadcastmonitoring.data.model.BroadcastSchedule
import com.example.digitalbroadcastmonitoring.data.model.ScheduleStatus
import com.example.digitalbroadcastmonitoring.data.repository.BroadcastRepository
import com.example.digitalbroadcastmonitoring.ui.components.BroadcastTextField
import com.example.digitalbroadcastmonitoring.ui.components.DeleteConfirmationDialog
import com.example.digitalbroadcastmonitoring.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddScheduleScreen(
    onNavigateBack: () -> Unit
) {
    val programs by BroadcastRepository.programs.collectAsState()
    val todayDate = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }

    var selectedProgram by remember { mutableStateOf(programs.firstOrNull()) }
    var channel by remember(selectedProgram) { mutableStateOf(selectedProgram?.channel ?: "DD National HD") }
    var date by remember { mutableStateOf(todayDate) }
    var startTime by remember { mutableStateOf("19:00") }
    var endTime by remember { mutableStateOf("20:00") }
    var notes by remember { mutableStateOf("Synchronized schedule entry.") }
    var errorMessage by remember { mutableStateOf("") }
    var isOverlapWarning by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Schedule Broadcast Slot", style = MaterialTheme.typography.titleLarge, color = TextWhite) },
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
                    Text("Select Program Feed *", style = MaterialTheme.typography.labelMedium, color = TextWhite)
                    Spacer(modifier = Modifier.height(6.dp))

                    programs.forEach { prog ->
                        val isSelected = selectedProgram?.id == prog.id
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    selectedProgram = prog
                                    channel = prog.channel
                                },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) DarkSecondarySurface else DarkSurface
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) PrimaryBlue else BorderStroke.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(prog.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TextWhite)
                                    Text("${prog.channel} • ${prog.durationMinutes}m", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Selected", tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    BroadcastTextField(
                        value = channel,
                        onValueChange = { channel = it },
                        label = "Transmission Channel *",
                        placeholder = "DD National HD"
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    BroadcastTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = "Broadcast Date (YYYY-MM-DD) *",
                        placeholder = todayDate
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BroadcastTextField(
                            value = startTime,
                            onValueChange = { startTime = it },
                            label = "Start Time (HH:mm) *",
                            placeholder = "19:00",
                            modifier = Modifier.weight(1f)
                        )

                        BroadcastTextField(
                            value = endTime,
                            onValueChange = { endTime = it },
                            label = "End Time (HH:mm) *",
                            placeholder = "20:00",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    BroadcastTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = "Operational Notes",
                        placeholder = "e.g. Ingest remarks or cue tones"
                    )

                    if (errorMessage.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(AnomalyRedContainer)
                                .border(1.dp, AnomalyRed.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text(errorMessage, color = AnomalyRed, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            val prog = selectedProgram
                            val sMin = BroadcastRepository.parseTimeToMinutes(startTime)
                            val eMin = BroadcastRepository.parseTimeToMinutes(endTime)

                            if (prog == null) {
                                errorMessage = "Please select a program."
                            } else if (channel.isBlank() || date.isBlank()) {
                                errorMessage = "Channel and date must be specified."
                            } else if (sMin == null || eMin == null) {
                                errorMessage = "Time format must be HH:mm (24-hour)."
                            } else if (eMin <= sMin) {
                                errorMessage = "End time must be after start time."
                            } else {
                                val hasOverlap = BroadcastRepository.checkOverlap(channel.trim(), date.trim(), startTime.trim(), endTime.trim())
                                val newSchedule = BroadcastSchedule(
                                    id = "sch-" + UUID.randomUUID().toString().take(6),
                                    programId = prog.id,
                                    programTitle = prog.title,
                                    channel = channel.trim(),
                                    date = date.trim(),
                                    startTime = startTime.trim(),
                                    endTime = endTime.trim(),
                                    status = if (hasOverlap) ScheduleStatus.FLAGGED else ScheduleStatus.SCHEDULED,
                                    isVerified = !hasOverlap,
                                    hasOverlap = hasOverlap,
                                    notes = notes.trim()
                                )
                                BroadcastRepository.addSchedule(newSchedule)
                                onNavigateBack()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Text("Validate & Save Slot", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleDetailScreen(
    scheduleId: String,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit
) {
    val schedule = BroadcastRepository.getScheduleById(scheduleId)
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Slot Verification Details", style = MaterialTheme.typography.titleLarge, color = TextWhite) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigateToEdit(scheduleId) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Schedule", tint = PrimaryBlueVariant)
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Schedule", tint = AnomalyRed)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        if (schedule == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Schedule entry not found.", color = TextMuted)
            }
        } else {
            val durationMinutes = BroadcastRepository.calculateDurationMinutes(schedule.startTime, schedule.endTime)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                if (schedule.hasOverlap) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = AnomalyRedContainer),
                        border = BorderStroke(1.dp, AnomalyRed.copy(alpha = 0.5f))
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = AnomalyRed)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Rule-based conflict detected: This slot overlaps with another scheduled broadcast on ${schedule.channel}.",
                                color = AnomalyRed,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

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
                                text = schedule.channel,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBlueVariant
                            )

                            if (schedule.isVerified) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Verified, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("VERIFIED SLOT", color = SuccessGreen, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = schedule.programTitle,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = BorderStroke)
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Date", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                Text(schedule.date, style = MaterialTheme.typography.bodyMedium, color = TextWhite)
                            }
                            Column {
                                Text("Broadcast Time", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                Text("${schedule.startTime} - ${schedule.endTime}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TextWhite)
                            }
                            Column {
                                Text("Duration", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                Text("${durationMinutes} mins", style = MaterialTheme.typography.bodyMedium, color = TextWhite)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Column {
                            Text("Transmission Status", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = schedule.status.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = when (schedule.status) {
                                    ScheduleStatus.LIVE -> AnomalyRed
                                    ScheduleStatus.COMPLETED -> TextMuted
                                    ScheduleStatus.SCHEDULED -> PrimaryBlueVariant
                                    ScheduleStatus.FLAGGED -> WarningAmber
                                }
                            )
                        }

                        if (schedule.notes.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Column {
                                Text("Operational Ingest Notes", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(schedule.notes, style = MaterialTheme.typography.bodyMedium, color = TextWhite)
                            }
                        }
                    }
                }
            }
        }

        if (showDeleteDialog) {
            DeleteConfirmationDialog(
                title = "Cancel & Delete Slot",
                message = "Are you sure you want to remove the broadcast slot for '${schedule?.programTitle}'?",
                onConfirm = {
                    showDeleteDialog = false
                    BroadcastRepository.deleteSchedule(scheduleId)
                    onNavigateBack()
                },
                onDismiss = { showDeleteDialog = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScheduleScreen(
    scheduleId: String,
    onNavigateBack: () -> Unit
) {
    val existing = BroadcastRepository.getScheduleById(scheduleId)

    var channel by remember(existing) { mutableStateOf(existing?.channel ?: "") }
    var date by remember(existing) { mutableStateOf(existing?.date ?: "") }
    var startTime by remember(existing) { mutableStateOf(existing?.startTime ?: "") }
    var endTime by remember(existing) { mutableStateOf(existing?.endTime ?: "") }
    var notes by remember(existing) { mutableStateOf(existing?.notes ?: "") }
    var errorMessage by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Broadcast Slot", style = MaterialTheme.typography.titleLarge, color = TextWhite) },
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
                    Text("Program: ${existing?.programTitle ?: ""}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextWhite)
                    Spacer(modifier = Modifier.height(14.dp))

                    BroadcastTextField(
                        value = channel,
                        onValueChange = { channel = it },
                        label = "Channel Identifier *"
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    BroadcastTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = "Date (YYYY-MM-DD) *"
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        BroadcastTextField(
                            value = startTime,
                            onValueChange = { startTime = it },
                            label = "Start (HH:mm) *",
                            modifier = Modifier.weight(1f)
                        )
                        BroadcastTextField(
                            value = endTime,
                            onValueChange = { endTime = it },
                            label = "End (HH:mm) *",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    BroadcastTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = "Notes"
                    )

                    if (errorMessage.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(errorMessage, color = AnomalyRed, style = MaterialTheme.typography.bodyMedium)
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            val sMin = BroadcastRepository.parseTimeToMinutes(startTime)
                            val eMin = BroadcastRepository.parseTimeToMinutes(endTime)

                            if (channel.isBlank() || date.isBlank()) {
                                errorMessage = "Channel and date must be specified."
                            } else if (sMin == null || eMin == null || eMin <= sMin) {
                                errorMessage = "End time must be after start time."
                            } else if (existing != null) {
                                val hasOverlap = BroadcastRepository.checkOverlap(channel.trim(), date.trim(), startTime.trim(), endTime.trim(), excludeId = existing.id)
                                val updated = existing.copy(
                                    channel = channel.trim(),
                                    date = date.trim(),
                                    startTime = startTime.trim(),
                                    endTime = endTime.trim(),
                                    status = if (hasOverlap) ScheduleStatus.FLAGGED else existing.status,
                                    hasOverlap = hasOverlap,
                                    notes = notes.trim()
                                )
                                BroadcastRepository.updateSchedule(updated)
                                onNavigateBack()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Text("Save & Validate", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
