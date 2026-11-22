package com.example.digitalbroadcastmonitoring.data.model

import com.google.gson.annotations.SerializedName

enum class ProgramCategory(val displayName: String) {
    NEWS("News"),
    SPORTS("Sports"),
    ENTERTAINMENT("Entertainment"),
    DOCUMENTARY("Documentary"),
    COMMERCIAL("Commercial"),
    TALK_SHOW("Talk Show");

    companion object {
        fun fromString(value: String): ProgramCategory {
            val clean = value.trim().replace("_", " ")
            return values().firstOrNull {
                it.displayName.equals(clean, ignoreCase = true) || it.name.equals(clean, ignoreCase = true)
            } ?: NEWS
        }
    }
}

enum class StreamStatus(val displayName: String) {
    OPTIMAL("Optimal"),
    DEGRADED("Degraded"),
    CRITICAL("Critical"),
    OFFLINE("Offline");

    companion object {
        fun fromString(value: String): StreamStatus {
            return values().firstOrNull {
                it.displayName.equals(value.trim(), ignoreCase = true) || it.name.equals(value.trim(), ignoreCase = true)
            } ?: OPTIMAL
        }
    }
}

enum class ScheduleStatus(val displayName: String) {
    SCHEDULED("Scheduled"),
    LIVE("Live"),
    COMPLETED("Completed"),
    FLAGGED("Flagged"),
    CANCELLED("Cancelled"),
    RESCHEDULED("Rescheduled");

    companion object {
        fun fromString(value: String): ScheduleStatus {
            return values().firstOrNull {
                it.displayName.equals(value.trim(), ignoreCase = true) || it.name.equals(value.trim(), ignoreCase = true)
            } ?: SCHEDULED
        }
    }
}

enum class AlertSeverity {
    ANOMALY, // Red #EF4444
    WARNING, // Amber #F59E0B
    SUCCESS; // Green #22C55E

    companion object {
        fun fromString(value: String): AlertSeverity {
            return values().firstOrNull { it.name.equals(value.trim(), ignoreCase = true) } ?: WARNING
        }
    }
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
    val isSimulated: Boolean = false
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

// Real ML & Telemetry Analysis structures matching Flask Backend
data class RealMlAnalysis(
    val modelLoaded: Boolean,
    val modelStatus: String,
    val modelType: String,
    val isAnomaly: Boolean,
    val anomalyScore: Float,
    val rawDecisionScore: Float,
    val latencyMs: Long,
    val compliancePrediction: String,
    val detectedAnomalies: List<String>,
    val disclaimer: String
)

data class ScheduleAnalysisResult(
    val schedule: BroadcastSchedule,
    val isValid: Boolean,
    val ruleStatus: String,
    val ruleErrorMessages: List<String>,
    val hasOverlap: Boolean,
    val durationMinutes: Int,
    val mlAnalysis: RealMlAnalysis?
)

data class DashboardMetrics(
    val totalPrograms: Int = 0,
    val totalBroadcasts: Int = 0,
    val completedCount: Int = 0,
    val liveCount: Int = 0,
    val scheduledCount: Int = 0,
    val cancelledCount: Int = 0,
    val rescheduledCount: Int = 0,
    val flaggedCount: Int = 0,
    val anomaliesCount: Int = 0,
    val warningsCount: Int = 0,
    val activeChannels: Int = 0,
    val streamHealthPercent: Int = 100,
    val complianceRate: Float = 98.0f,
    val lastCheckedTimestamp: String = ""
)

data class ChannelReportItem(
    val channel: String,
    val broadcastCount: Int,
    val totalMinutes: Int
)

data class ReportsData(
    val totalPrograms: Int = 0,
    val totalBroadcasts: Int = 0,
    val completedCount: Int = 0,
    val liveCount: Int = 0,
    val scheduledCount: Int = 0,
    val flaggedCount: Int = 0,
    val totalDurationMinutes: Int = 0,
    val totalDurationHours: Int = 0,
    val totalDurationRemainMinutes: Int = 0,
    val complianceRate: Float = 98.0f,
    val categoryDistribution: Map<String, Int> = emptyMap(),
    val channelDistribution: List<ChannelReportItem> = emptyList(),
    val statusDistribution: Map<String, Int> = emptyMap()
)

enum class ConnectionStatus {
    ONLINE,
    OFFLINE,
    CONNECTING,
    ERROR
}

data class SyncFeedback(
    val isSuccess: Boolean,
    val message: String,
    val timestamp: String = "",
    val isError: Boolean = false
)

