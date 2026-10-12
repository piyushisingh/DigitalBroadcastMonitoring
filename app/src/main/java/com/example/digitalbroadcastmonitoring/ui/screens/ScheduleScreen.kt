package com.example.digitalbroadcastmonitoring.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.digitalbroadcastmonitoring.data.model.BroadcastSchedule
import com.example.digitalbroadcastmonitoring.data.model.ScheduleStatus
import com.example.digitalbroadcastmonitoring.data.repository.BroadcastRepository
import com.example.digitalbroadcastmonitoring.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ScheduleScreen(
    onNavigateToAddSchedule: () -> Unit,
    onNavigateToScheduleDetail: (String) -> Unit
) {
    val schedules by BroadcastRepository.schedules.collectAsState()
    val todayDate = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }

    var selectedFilter by remember { mutableStateOf("Today") }
    var selectedChannel by remember { mutableStateOf<String?>(null) }

    val distinctChannels = remember(schedules) {
        schedules.map { it.channel }.distinct()
    }

    val filteredSchedules = remember(schedules, selectedFilter, selectedChannel) {
        schedules.filter { sch ->
            val matchesDate = when (selectedFilter) {
                "Today" -> sch.date == todayDate
                "All" -> true
                else -> true
            }
            val matchesChannel = selectedChannel == null || sch.channel.equals(selectedChannel, ignoreCase = true)
            matchesDate && matchesChannel
        }.sortedBy { it.startTime }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Broadcast Timetable",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
                Text(
                    text = "Daily slot allocations and conflict detection",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMuted
                )
            }

            FloatingActionButton(
                onClick = onNavigateToAddSchedule,
                containerColor = PrimaryBlue,
                contentColor = TextWhite,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(46.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Schedule Slot")
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Date Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Today", "All").forEach { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryBlue,
                        selectedLabelColor = TextWhite,
                        containerColor = DarkSurface,
                        labelColor = TextMuted
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selectedFilter == filter,
                        borderColor = BorderStroke,
                        selectedBorderColor = PrimaryBlue
                    )
                )
            }

            // Channel Filter Chips
            distinctChannels.forEach { channel ->
                FilterChip(
                    selected = selectedChannel == channel,
                    onClick = { selectedChannel = if (selectedChannel == channel) null else channel },
                    label = { Text(channel) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryBlue,
                        selectedLabelColor = TextWhite,
                        containerColor = DarkSurface,
                        labelColor = TextMuted
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selectedChannel == channel,
                        borderColor = BorderStroke,
                        selectedBorderColor = PrimaryBlue
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (filteredSchedules.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.EventBusy,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No schedules for selected criteria",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextMuted
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredSchedules, key = { it.id }) { schedule ->
                    val durationMin = BroadcastRepository.calculateDurationMinutes(schedule.startTime, schedule.endTime)
                    ScheduleItemCard(
                        schedule = schedule,
                        durationMinutes = durationMin,
                        onClick = { onNavigateToScheduleDetail(schedule.id) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(80.dp)) // Bottom nav padding
                }
            }
        }
    }
}

@Composable
fun ScheduleItemCard(
    schedule: BroadcastSchedule,
    durationMinutes: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(
            1.dp,
            if (schedule.hasOverlap) AnomalyRed else com.example.digitalbroadcastmonitoring.ui.theme.BorderStroke
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Channel, Overlap badge, Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(DarkSecondarySurface)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = schedule.channel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlueVariant
                        )
                    }

                    if (schedule.isVerified) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Verified Slot",
                            tint = SuccessGreen,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    if (schedule.hasOverlap) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(AnomalyRedContainer)
                                .border(1.dp, AnomalyRed.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "OVERLAP CONFLICT",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = AnomalyRed
                            )
                        }
                    }
                }

                // Schedule Status
                when (schedule.status) {
                    ScheduleStatus.LIVE -> {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(AnomalyRedContainer)
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(AnomalyRed)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("LIVE NOW", color = AnomalyRed, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                    ScheduleStatus.SCHEDULED -> {
                        Text("SCHEDULED", color = PrimaryBlueVariant, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    ScheduleStatus.COMPLETED -> {
                        Text("COMPLETED", color = TextMuted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    ScheduleStatus.FLAGGED -> {
                        Text("FLAGGED", color = WarningAmber, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = schedule.programTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Time and Duration
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${schedule.startTime} - ${schedule.endTime}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextWhite
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "(${durationMinutes}m)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }

                Text(
                    text = schedule.date,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
        }
    }
}
