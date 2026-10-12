package com.example.digitalbroadcastmonitoring.data.model

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

enum class ProgramCategory(val displayName: String) {
    NEWS("News"),
    SPORTS("Sports"),
    ENTERTAINMENT("Entertainment"),
    DOCUMENTARY("Documentary"),
    COMMERCIAL("Commercial"),
    TALK_SHOW("Talk Show")
}

enum class StreamStatus(val displayName: String) {
    OPTIMAL("Optimal"),
    DEGRADED("Degraded"),
    CRITICAL("Critical"),
    OFFLINE("Offline")
}

enum class ScheduleStatus(val displayName: String) {
    SCHEDULED("Scheduled"),
    LIVE("Live"),
    COMPLETED("Completed"),
    FLAGGED("Flagged")
}

enum class AlertSeverity {
    ANOMALY, // Red #EF4444
    WARNING, // Amber #F59E0B
    SUCCESS  // Green #22C55E
}

data class Program(
    val id: String,
    val title: String,
    val channel: String,
    val category: ProgramCategory,
    val language: String = "English",
    val videoStatus: StreamStatus = StreamStatus.OPTIMAL,
    val audioStatus: StreamStatus = StreamStatus.OPTIMAL,
    val complianceScore: Int = 98,
    val durationMinutes: Int = 60,
    val description: String = ""
)

data class BroadcastSchedule(
    val id: String,
    val programId: String,
    val programTitle: String,
    val channel: String,
    val date: String, // YYYY-MM-DD
    val startTime: String, // HH:mm
    val endTime: String, // HH:mm
    val status: ScheduleStatus = ScheduleStatus.SCHEDULED,
    val isVerified: Boolean = true,
    val hasOverlap: Boolean = false,
    val notes: String = ""
)

data class AlertItem(
    val id: String,
    val timestamp: String,
    val title: String,
    val description: String,
    val severity: AlertSeverity,
    val channel: String,
    val programTitle: String,
    val isSimulated: Boolean = true
)

data class SystemOverview(
    val totalBroadcastsToday: Int,
    val activeChannels: Int,
    val complianceRate: Float,
    val totalAnomalies: Int,
    val pendingWarnings: Int,
    val streamHealthPercent: Int,
    val lastCheckedTimestamp: String
)

data class RuleValidationResult(
    val isValid: Boolean,
    val overlappingSchedules: List<Pair<BroadcastSchedule, BroadcastSchedule>>,
    val invalidTimeSchedules: List<BroadcastSchedule>,
    val errorMessages: List<String>
)

data class SimulatedMlAnalysis(
    val isSimulated: Boolean = true,
    val anomalyScore: Float, // 0.0 - 1.0
    val audioQualityScore: Float,
    val videoQualityScore: Float,
    val latencyMs: Long,
    val compliancePrediction: String,
    val detectedAnomalies: List<String>
)
