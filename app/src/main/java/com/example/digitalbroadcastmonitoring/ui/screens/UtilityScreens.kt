package com.example.digitalbroadcastmonitoring.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
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
import com.example.digitalbroadcastmonitoring.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Operator Profile", style = MaterialTheme.typography.titleLarge, color = TextWhite) },
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(PrimaryBlue)
                    .border(2.dp, BorderStroke, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "PS",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Piyushi Singh",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )
            Text(
                text = "Lead Telemetry Operator • Central Monitoring Station",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, BorderStroke)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Station Accreditation", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = PrimaryBlueVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    ProfileItem(label = "Operator ID", value = "OP-DEL-2026-894")
                    ProfileItem(label = "Multiplex Node", value = "Delhi DTT / DVB-T2 MUX 1")
                    ProfileItem(label = "Clearance Level", value = "Level 4 (Full Schedule & Telemetry Edit)")
                    ProfileItem(label = "Session Protocol", value = "Secure Encrypted Stream Link")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AnomalyRedContainer),
                border = BorderStroke(1.dp, AnomalyRed.copy(alpha = 0.5f))
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = AnomalyRed)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out Terminal", color = AnomalyRed, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ProfileItem(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = TextWhite)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAbout: () -> Unit
) {
    var realTimeSync by remember { mutableStateOf(true) }
    var soundAlerts by remember { mutableStateOf(true) }
    var highPrecisionLoudness by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Terminal Settings", style = MaterialTheme.typography.titleLarge, color = TextWhite) },
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
            Text("TELEMETRY STREAMING", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TextMuted)
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, BorderStroke)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Continuous Feed Sync", style = MaterialTheme.typography.titleMedium, color = TextWhite)
                            Text("Poll live transmission buffer every 5 seconds", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        }
                        Switch(
                            checked = realTimeSync,
                            onCheckedChange = { realTimeSync = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = TextWhite,
                                checkedTrackColor = PrimaryBlue,
                                uncheckedTrackColor = DarkSecondarySurface
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = BorderStroke)
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Audio Clipping Warning", style = MaterialTheme.typography.titleMedium, color = TextWhite)
                            Text("Trigger alert when peak exceeds -1.0 dBFS", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        }
                        Switch(
                            checked = soundAlerts,
                            onCheckedChange = { soundAlerts = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = TextWhite,
                                checkedTrackColor = PrimaryBlue,
                                uncheckedTrackColor = DarkSecondarySurface
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = BorderStroke)
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("EBU R128 Loudness Metric", style = MaterialTheme.typography.titleMedium, color = TextWhite)
                            Text("Target integrated loudness of -23.0 LUFS", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        }
                        Switch(
                            checked = highPrecisionLoudness,
                            onCheckedChange = { highPrecisionLoudness = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = TextWhite,
                                checkedTrackColor = PrimaryBlue,
                                uncheckedTrackColor = DarkSecondarySurface
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("SYSTEM INFO", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TextMuted)
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, BorderStroke)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    TextButton(
                        onClick = onNavigateToAbout,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = PrimaryBlueVariant)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("About Digital Broadcast Monitoring", color = TextWhite, style = MaterialTheme.typography.bodyLarge)
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About System", style = MaterialTheme.typography.titleLarge, color = TextWhite) },
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
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Digital Broadcast Monitoring",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                    Text(
                        text = "Version 1.0 (Native Jetpack Compose)",
                        style = MaterialTheme.typography.labelMedium,
                        color = PrimaryBlueVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "A high-integrity native Android monitoring station designed for digital terrestrial and satellite television broadcast operations. Features comprehensive timeline scheduling, EBU R128 loudness verification, deterministic rule-based conflict checks, and simulated neural anomaly detection.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextMuted
                    )

                    Spacer(modifier = Modifier.height(18.dp))
                    HorizontalDivider(color = BorderStroke)
                    Spacer(modifier = Modifier.height(18.dp))

                    Text("Architecture Specifications", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TextWhite)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• Kotlin 2.0 with Jetpack Compose & Material 3", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                    Text("• Single NavHost with Navigation Compose", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                    Text("• Dynamic wallpaper override disabled for strict operational dark palette", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                    Text("• Local in-memory repository with real-time state synchronization", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                }
            }
        }
    }
}
