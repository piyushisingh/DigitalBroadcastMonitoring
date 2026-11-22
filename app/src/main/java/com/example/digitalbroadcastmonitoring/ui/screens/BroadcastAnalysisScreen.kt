package com.example.digitalbroadcastmonitoring.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.digitalbroadcastmonitoring.data.api.NetworkResult
import com.example.digitalbroadcastmonitoring.data.api.dto.*
import com.example.digitalbroadcastmonitoring.data.model.*
import com.example.digitalbroadcastmonitoring.data.repository.BroadcastRepository
import com.example.digitalbroadcastmonitoring.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BroadcastAnalysisScreen(
    programId: String? = null,
    onNavigateBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    val schedules by BroadcastRepository.schedules.collectAsState()

    // Find the target schedule for this program or fallback to first schedule
    val targetSchedule = remember(schedules, programId) {
        if (!programId.isNullOrBlank() && programId != "default") {
            schedules.firstOrNull { it.programId == programId || it.id == programId }
                ?: schedules.firstOrNull()
        } else {
            schedules.firstOrNull()
        }
    }

    // Live backend schedule analysis state (Section 1)
    var analysisResult by remember { mutableStateOf<ScheduleAnalysisResult?>(null) }
    var isLoadingAnalysis by remember { mutableStateOf(false) }

    // Fetch live backend schedule analysis
    LaunchedEffect(targetSchedule?.id) {
        targetSchedule?.id?.let { scheduleId ->
            isLoadingAnalysis = true
            BroadcastRepository.fetchScheduleAnalysis(scheduleId) { result ->
                isLoadingAnalysis = false
                if (result is NetworkResult.Success) {
                    analysisResult = result.data
                }
            }
        }
    }

    // Fallback rule validation if backend call hasn't resolved
    val ruleValidation = analysisResult?.let {
        RuleValidationResult(
            isValid = it.isValid,
            overlappingSchedules = emptyList(),
            invalidTimeSchedules = emptyList(),
            errorMessages = it.ruleErrorMessages
        )
    } ?: BroadcastRepository.performRuleBasedValidation()

    // Real ML Early-Session Stalling Model State (Section 2)
    var mlHealth by remember { mutableStateOf<MlHealthResponseDto?>(null) }
    var mlHealthLoading by remember { mutableStateOf(false) }

    // Seven required model input fields initialized from research preset
    var inputYear by remember { mutableStateOf(ResearchPresets.UNLIMITED_HIGH_THROUGHPUT.year) }
    var inputLocation by remember { mutableStateOf(ResearchPresets.UNLIMITED_HIGH_THROUGHPUT.location) }
    var inputBandwidth by remember { mutableStateOf(ResearchPresets.UNLIMITED_HIGH_THROUGHPUT.bandwidth) }
    var inputAdblock by remember { mutableStateOf(ResearchPresets.UNLIMITED_HIGH_THROUGHPUT.adblock) }
    var inputT1 by remember { mutableStateOf(ResearchPresets.UNLIMITED_HIGH_THROUGHPUT.dlThroughputFirst1s.toLong().toString()) }
    var inputT5 by remember { mutableStateOf(ResearchPresets.UNLIMITED_HIGH_THROUGHPUT.dlThroughputFirst5s.toLong().toString()) }
    var inputT10 by remember { mutableStateOf(ResearchPresets.UNLIMITED_HIGH_THROUGHPUT.dlThroughputFirst10s.toLong().toString()) }
    var activePresetTitle by remember { mutableStateOf<String?>(ResearchPresets.UNLIMITED_HIGH_THROUGHPUT.title) }

    var isPredicting by remember { mutableStateOf(false) }
    var predictionResult by remember { mutableStateOf<StallingRiskResponseDto?>(null) }
    var predictionError by remember { mutableStateOf<String?>(null) }

    val applyPreset: (ResearchPreset) -> Unit = { preset ->
        inputYear = preset.year
        inputLocation = preset.location
        inputBandwidth = preset.bandwidth
        inputAdblock = preset.adblock
        inputT1 = preset.dlThroughputFirst1s.toLong().toString()
        inputT5 = preset.dlThroughputFirst5s.toLong().toString()
        inputT10 = preset.dlThroughputFirst10s.toLong().toString()
        activePresetTitle = preset.title
        predictionError = null
    }

    // Fetch ML Health on initial load
    LaunchedEffect(Unit) {
        mlHealthLoading = true
        BroadcastRepository.fetchMlHealth { result ->
            mlHealthLoading = false
            if (result is NetworkResult.Success) {
                mlHealth = result.data
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Broadcast Analysis & Streaming QoE",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextWhite
                        )
                        val subText = if (mlHealth?.isReady == true) {
                            "Connected: Flask REST API & MUEXLab ML Pipeline"
                        } else {
                            "Schedule Rule Engine & Streaming QoE Inference"
                        }
                        Text(subText, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    }
                },
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
            // =========================================================================
            // SECTION 1: DETERMINISTIC SCHEDULE RULE-BASED VALIDATION
            // =========================================================================
            Text(
                text = "SECTION 1: DETERMINISTIC SCHEDULE RULE-BASED VALIDATION",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(
                    1.dp,
                    if (ruleValidation.isValid) SuccessGreen.copy(alpha = 0.5f) else AnomalyRed.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (ruleValidation.isValid) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = null,
                                tint = if (ruleValidation.isValid) SuccessGreen else AnomalyRed,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (ruleValidation.isValid) "Schedule Rules Compliant" else "Rule Deviations Detected",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (ruleValidation.isValid) SuccessGreenContainer else AnomalyRedContainer)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (ruleValidation.isValid) "PASS" else "FAIL",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (ruleValidation.isValid) SuccessGreen else AnomalyRed
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Deterministic validation verifies broadcast schedule timeline intervals, detecting overlaps on the same channel and invalid start/end timestamp sequences.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (ruleValidation.errorMessages.isEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkSecondarySurface)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Verified, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "All active broadcast schedule slots have non-overlapping, valid chronological intervals.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextWhite
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ruleValidation.errorMessages.forEach { msg ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(AnomalyRedContainer)
                                        .border(1.dp, AnomalyRed.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = AnomalyRed, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(msg, style = MaterialTheme.typography.bodyMedium, color = TextWhite)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // =========================================================================
            // SECTION 2: EARLY-SESSION VIDEO STREAMING STALLING PREDICTION (MUEXLab ML)
            // =========================================================================
            val isModelReady = mlHealth?.isReady == true
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SECTION 2: STREAMING QOE STALLING PREDICTION",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 1.sp
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isModelReady) SuccessGreenContainer else WarningAmberContainer)
                        .border(
                            1.dp,
                            if (isModelReady) SuccessGreen.copy(alpha = 0.5f) else WarningAmber.copy(alpha = 0.5f),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    val statusText = if (isModelReady) {
                        "MUEXLAB PIPELINE ACTIVE (${mlHealth?.selectedModel ?: "RandomForestClassifier"})"
                    } else if (mlHealthLoading) {
                        "CHECKING ML BACKEND..."
                    } else {
                        "ML MODEL OFFLINE"
                    }
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isModelReady) SuccessGreen else WarningAmber
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, BorderStroke)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Transparent Disclaimer Banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSecondarySurface)
                            .border(1.dp, BorderStroke, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = PrimaryBlueVariant,
                                modifier = Modifier.size(20.dp).padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Research Dataset Provenance & Scope",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "This machine learning model predicts video playback stalling risk from the initial 10 seconds of streaming traffic, trained on the real MUEXLab encrypted session dataset (2020-2021). " +
                                            "This task analyzes streaming Quality of Experience (QoE). It does NOT measure direct television broadcast hardware faults (such as RF transmitter loss, black frames, or EBU R128 audio clipping).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Preset Example Selectors (Clearly labeled as example research inputs)
                    Text(
                        text = "Research Example Inputs (Select preset or edit values below):",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextWhite
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { applyPreset(ResearchPresets.UNLIMITED_HIGH_THROUGHPUT) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SuccessGreen),
                            border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.5f))
                        ) {
                            Text(ResearchPresets.UNLIMITED_HIGH_THROUGHPUT.title, fontSize = 11.sp, maxLines = 1)
                        }

                        OutlinedButton(
                            onClick = { applyPreset(ResearchPresets.ONE_MBPS_STALLING_RISK) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AnomalyRed),
                            border = BorderStroke(1.dp, AnomalyRed.copy(alpha = 0.5f))
                        ) {
                            Text(ResearchPresets.ONE_MBPS_STALLING_RISK.title, fontSize = 11.sp, maxLines = 1)
                        }
                    }

                    if (activePresetTitle != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Loaded Preset: $activePresetTitle (MUEXLab research dataset reference)",
                            style = MaterialTheme.typography.labelSmall,
                            color = PrimaryBlueVariant,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Form: 7 Model Input Features
                    Text(
                        text = "Model Input Parameters (Initial 10s Signals):",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextWhite
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Field 1 & 2: Year and Location
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Year (2020 / 2021)", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf("2020", "2021").forEach { yr ->
                                    val isSelected = inputYear == yr
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) PrimaryBlue else DarkSecondarySurface)
                                            .border(1.dp, if (isSelected) PrimaryBlueVariant else BorderStroke, RoundedCornerShape(8.dp))
                                            .clickable {
                                                inputYear = yr
                                                activePresetTitle = null
                                            }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(yr, color = if (isSelected) TextWhite else TextMuted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("Location (City)", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf("Zagreb", "Wuerzburg").forEach { loc ->
                                    val isSelected = inputLocation == loc
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) PrimaryBlue else DarkSecondarySurface)
                                            .border(1.dp, if (isSelected) PrimaryBlueVariant else BorderStroke, RoundedCornerShape(8.dp))
                                            .clickable {
                                                inputLocation = loc
                                                activePresetTitle = null
                                            }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(loc.take(6), color = if (isSelected) TextWhite else TextMuted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Field 3 & 4: Bandwidth and Adblock
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1.3f)) {
                            Text("Bandwidth Restriction", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf("unlimited", "1Mbps", "stochastic").forEach { bw ->
                                    val isSelected = inputBandwidth == bw
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) PrimaryBlue else DarkSecondarySurface)
                                            .border(1.dp, if (isSelected) PrimaryBlueVariant else BorderStroke, RoundedCornerShape(8.dp))
                                            .clickable {
                                                inputBandwidth = bw
                                                activePresetTitle = null
                                            }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            when (bw) {
                                                "unlimited" -> "Unlim"
                                                "stochastic" -> "Stoch"
                                                else -> "1M"
                                            },
                                            color = if (isSelected) TextWhite else TextMuted,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(0.7f)) {
                            Text("Adblock", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf("0" to "Off", "1" to "On").forEach { (v, lbl) ->
                                    val isSelected = inputAdblock == v
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) PrimaryBlue else DarkSecondarySurface)
                                            .border(1.dp, if (isSelected) PrimaryBlueVariant else BorderStroke, RoundedCornerShape(8.dp))
                                            .clickable {
                                                inputAdblock = v
                                                activePresetTitle = null
                                            }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(lbl, color = if (isSelected) TextWhite else TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // Informative helper banner when 1Mbps is selected but throughput fields still show unthrottled values (>1 MB/s)
                    val t1Val = inputT1.toDoubleOrNull() ?: 0.0
                    val t5Val = inputT5.toDoubleOrNull() ?: 0.0
                    val t10Val = inputT10.toDoubleOrNull() ?: 0.0
                    val is1MbpsThroughputMismatched = inputBandwidth == "1Mbps" && (t1Val > 1_000_000.0 || t5Val > 1_000_000.0 || t10Val > 1_000_000.0)

                    if (is1MbpsThroughputMismatched) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = DarkSecondarySurface,
                            border = BorderStroke(1.dp, PrimaryBlueVariant.copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Bandwidth selector set to 1M (1Mbps). Current throughput (>1 MB/s) exceeds typical 1Mbps dataset limits (~800k–950k Bytes/s).",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextWhite,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Tap 'Load 1Mbps Values' to populate representative MUEXLab measurements, or edit fields manually.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted,
                                        fontSize = 10.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedButton(
                                    onClick = { applyPreset(ResearchPresets.ONE_MBPS_STALLING_RISK) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    border = BorderStroke(1.dp, PrimaryBlueVariant)
                                ) {
                                    Text("Load 1Mbps Values", fontSize = 11.sp, color = PrimaryBlueVariant, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Fields 5, 6, 7: Throughput inputs
                    OutlinedTextField(
                        value = inputT1,
                        onValueChange = {
                            inputT1 = it
                            activePresetTitle = null
                        },
                        label = { Text("DL Throughput 1st second (Bytes/s)", color = TextMuted) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DarkSecondarySurface,
                            unfocusedContainerColor = DarkSecondarySurface,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = BorderStroke
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = inputT5,
                        onValueChange = {
                            inputT5 = it
                            activePresetTitle = null
                        },
                        label = { Text("DL Throughput first 5 seconds (Bytes/s)", color = TextMuted) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DarkSecondarySurface,
                            unfocusedContainerColor = DarkSecondarySurface,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = BorderStroke
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = inputT10,
                        onValueChange = {
                            inputT10 = it
                            activePresetTitle = null
                        },
                        label = { Text("DL Throughput first 10 seconds (Bytes/s)", color = TextMuted) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DarkSecondarySurface,
                            unfocusedContainerColor = DarkSecondarySurface,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = BorderStroke
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action Button: Predict
                    Button(
                        onClick = {
                            val t1 = inputT1.toDoubleOrNull()
                            val t5 = inputT5.toDoubleOrNull()
                            val t10 = inputT10.toDoubleOrNull()

                            if (t1 == null || t5 == null || t10 == null || t1 < 0 || t5 < 0 || t10 < 0) {
                                predictionError = "Please enter valid non-negative numerical throughput values in Bytes/s."
                                return@Button
                            }

                            predictionError = null
                            isPredicting = true

                            val request = StallingRiskRequestDto(
                                year = inputYear,
                                location = inputLocation,
                                bandwidth = inputBandwidth,
                                adblock = inputAdblock,
                                dlThroughputFirst1s = t1,
                                dlThroughputFirst5s = t5,
                                dlThroughputFirst10s = t10
                            )

                            BroadcastRepository.predictStallingRisk(request) { networkResult ->
                                isPredicting = false
                                when (networkResult) {
                                    is NetworkResult.Success -> {
                                        predictionResult = networkResult.data
                                        predictionError = null
                                    }
                                    is NetworkResult.Error -> {
                                        predictionError = networkResult.message
                                    }
                                    is NetworkResult.Loading -> {}
                                }
                            }
                        },
                        enabled = !isPredicting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        if (isPredicting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = TextWhite,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Running ML Inference...", color = TextWhite)
                        } else {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = TextWhite)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Predict Video Stalling Risk", color = TextWhite, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Error Display
                    if (predictionError != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(AnomalyRedContainer)
                                .border(1.dp, AnomalyRed.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Error, contentDescription = null, tint = AnomalyRed)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = predictionError ?: "Prediction failed",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextWhite
                                )
                            }
                        }
                    }

                    // Prediction Results Section
                    val result = predictionResult
                    if (result != null) {
                        Spacer(modifier = Modifier.height(16.dp))

                        val isStalling = result.predictedClass == 1 || result.predictionLabel.equals("stalling", ignoreCase = true)
                        val riskScore = result.stallingRiskScore
                        val riskPct = (riskScore * 100).toInt()

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSecondarySurface),
                            border = BorderStroke(
                                1.dp,
                                if (isStalling) AnomalyRed.copy(alpha = 0.7f) else SuccessGreen.copy(alpha = 0.7f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "ML PREDICTION RESULT",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TextMuted,
                                        letterSpacing = 1.sp
                                    )

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isStalling) AnomalyRedContainer else SuccessGreenContainer)
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = if (isStalling) "STALLING RISK DETECTED" else "NO STALLING PREDICTED",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isStalling) AnomalyRed else SuccessGreen
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Card(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(containerColor = DarkSurface)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text("Stalling Risk Score", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "$riskPct%",
                                                style = MaterialTheme.typography.headlineMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isStalling) AnomalyRed else SuccessGreen
                                            )
                                            val thresholdPct = result.decisionThreshold?.let { (it * 100).toInt() } ?: 50
                                            val semanticsLabel = if (result.scoreSemantics == "uncalibrated_model_estimate") "Uncalibrated model score" else "Score"
                                            Text(
                                                text = "$semanticsLabel (>$thresholdPct% risk threshold)",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextMuted
                                            )
                                        }
                                    }

                                    Card(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(containerColor = DarkSurface)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text("Model Latency", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            val latencyText = result.latencyMs?.let { "${it.toInt()} ms" } ?: "< 10 ms"
                                            Text(
                                                text = latencyText,
                                                style = MaterialTheme.typography.headlineMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = PrimaryBlueVariant
                                            )
                                            Text(
                                                text = result.estimator ?: "RandomForestClassifier",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextMuted,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = "Pipeline: ${result.modelName} ${result.modelVersion}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )

                                result.provenance?.disclaimer?.let { discl ->
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Research Note: $discl",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
