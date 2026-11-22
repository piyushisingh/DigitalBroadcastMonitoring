package com.example.digitalbroadcastmonitoring.data.api.dto

import com.google.gson.annotations.SerializedName

data class ProgramDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("channel") val channel: String,
    @SerializedName("category") val category: String,
    @SerializedName("language") val language: String? = "English",
    @SerializedName("description") val description: String? = "",
    @SerializedName("duration") val duration: Int,
    @SerializedName("video_status") val videoStatus: String? = "Optimal",
    @SerializedName("audio_status") val audioStatus: String? = "Optimal",
    @SerializedName("compliance_score") val complianceScore: Int? = 98
)

data class CreateProgramRequest(
    @SerializedName("id") val id: String? = null,
    @SerializedName("name") val name: String,
    @SerializedName("channel") val channel: String,
    @SerializedName("category") val category: String,
    @SerializedName("language") val language: String = "English",
    @SerializedName("duration") val duration: Int,
    @SerializedName("description") val description: String = ""
)

data class UpdateProgramRequest(
    @SerializedName("name") val name: String? = null,
    @SerializedName("channel") val channel: String? = null,
    @SerializedName("category") val category: String? = null,
    @SerializedName("language") val language: String? = null,
    @SerializedName("duration") val duration: Int? = null,
    @SerializedName("description") val description: String? = null
)

data class ScheduleDto(
    @SerializedName("id") val id: String,
    @SerializedName("program_id") val programId: String,
    @SerializedName("channel") val channel: String,
    @SerializedName("broadcast_date") val broadcastDate: String,
    @SerializedName("start_time") val startTime: String,
    @SerializedName("end_time") val endTime: String,
    @SerializedName("status") val status: String = "Scheduled",
    @SerializedName("remarks") val remarks: String? = "",
    @SerializedName("is_verified") val isVerified: Int? = 1,
    @SerializedName("has_overlap") val hasOverlap: Int? = 0,
    @SerializedName("program_title") val programTitle: String? = null,
    @SerializedName("program_category") val programCategory: String? = null,
    @SerializedName("video_status") val videoStatus: String? = "Optimal",
    @SerializedName("audio_status") val audioStatus: String? = "Optimal",
    @SerializedName("compliance_score") val complianceScore: Int? = 98
)

data class CreateScheduleRequest(
    @SerializedName("id") val id: String? = null,
    @SerializedName("program_id") val programId: String,
    @SerializedName("channel") val channel: String,
    @SerializedName("broadcast_date") val broadcastDate: String,
    @SerializedName("start_time") val startTime: String,
    @SerializedName("end_time") val endTime: String,
    @SerializedName("status") val status: String = "Scheduled",
    @SerializedName("remarks") val remarks: String = ""
)

data class UpdateScheduleRequest(
    @SerializedName("channel") val channel: String? = null,
    @SerializedName("broadcast_date") val broadcastDate: String? = null,
    @SerializedName("start_time") val startTime: String? = null,
    @SerializedName("end_time") val endTime: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("remarks") val remarks: String? = null
)

data class DashboardResponseDto(
    @SerializedName("metrics") val metrics: DashboardMetricsDto,
    @SerializedName("recent_schedules") val recentSchedules: List<ScheduleDto>? = emptyList(),
    @SerializedName("recent_alerts") val recentAlerts: List<AlertDto>? = emptyList(),
    @SerializedName("last_checked_timestamp") val lastCheckedTimestamp: String? = ""
)

data class DashboardMetricsDto(
    @SerializedName("total_programs") val totalPrograms: Int = 0,
    @SerializedName("total_broadcasts") val totalBroadcasts: Int = 0,
    @SerializedName("completed_count") val completedCount: Int = 0,
    @SerializedName("live_count") val liveCount: Int = 0,
    @SerializedName("scheduled_count") val scheduledCount: Int = 0,
    @SerializedName("cancelled_count") val cancelledCount: Int = 0,
    @SerializedName("rescheduled_count") val rescheduledCount: Int = 0,
    @SerializedName("flagged_count") val flaggedCount: Int = 0,
    @SerializedName("anomalies_count") val anomaliesCount: Int = 0,
    @SerializedName("warnings_count") val warningsCount: Int = 0,
    @SerializedName("active_channels") val activeChannels: Int = 0,
    @SerializedName("stream_health_percent") val streamHealthPercent: Int = 100,
    @SerializedName("compliance_rate") val complianceRate: Float = 98.0f
)

data class ReportsResponseDto(
    @SerializedName("summary") val summary: ReportSummaryDto,
    @SerializedName("category_distribution") val categoryDistribution: Map<String, Int>? = emptyMap(),
    @SerializedName("channel_distribution") val channelDistribution: List<ChannelReportDto>? = emptyList(),
    @SerializedName("status_distribution") val statusDistribution: Map<String, Int>? = emptyMap()
)

data class ReportSummaryDto(
    @SerializedName("total_programs") val totalPrograms: Int = 0,
    @SerializedName("total_broadcasts") val totalBroadcasts: Int = 0,
    @SerializedName("completed_count") val completedCount: Int = 0,
    @SerializedName("live_count") val liveCount: Int = 0,
    @SerializedName("scheduled_count") val scheduledCount: Int = 0,
    @SerializedName("cancelled_count") val cancelledCount: Int = 0,
    @SerializedName("rescheduled_count") val rescheduledCount: Int = 0,
    @SerializedName("flagged_count") val flaggedCount: Int = 0,
    @SerializedName("anomalies_count") val anomaliesCount: Int = 0,
    @SerializedName("total_duration_minutes") val totalDurationMinutes: Int = 0,
    @SerializedName("total_duration_hours") val totalDurationHours: Int = 0,
    @SerializedName("total_duration_remain_minutes") val totalDurationRemainMinutes: Int = 0,
    @SerializedName("compliance_rate") val complianceRate: Float = 98.0f
)

data class ChannelReportDto(
    @SerializedName("channel") val channel: String,
    @SerializedName("broadcast_count") val broadcastCount: Int = 0,
    @SerializedName("total_minutes") val totalMinutes: Int = 0
)

data class AlertDto(
    @SerializedName("id") val id: String,
    @SerializedName("timestamp") val timestamp: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String,
    @SerializedName("severity") val severity: String,
    @SerializedName("channel") val channel: String,
    @SerializedName("program_title") val programTitle: String? = null,
    @SerializedName("schedule_id") val scheduleId: String? = null,
    @SerializedName("is_simulated") val isSimulated: Int? = 0
)

data class AnalysisResponseDto(
    @SerializedName("schedule") val schedule: ScheduleDto,
    @SerializedName("rule_validation") val ruleValidation: RuleValidationDto,
    @SerializedName("ml_analysis") val mlAnalysis: MlAnalysisDto
)

data class RuleValidationDto(
    @SerializedName("is_valid") val isValid: Boolean = true,
    @SerializedName("status") val status: String = "PASS",
    @SerializedName("error_messages") val errorMessages: List<String> = emptyList(),
    @SerializedName("duration_minutes") val durationMinutes: Int = 0,
    @SerializedName("has_overlap") val hasOverlap: Boolean = false
)

data class MlAnalysisDto(
    @SerializedName("model_loaded") val modelLoaded: Boolean = false,
    @SerializedName("model_status") val modelStatus: String = "UNKNOWN",
    @SerializedName("model_type") val modelType: String? = null,
    @SerializedName("is_anomaly") val isAnomaly: Boolean? = false,
    @SerializedName("anomaly_score") val anomalyScore: Float? = 0.0f,
    @SerializedName("raw_decision_score") val rawDecisionScore: Float? = 0.0f,
    @SerializedName("latency_ms") val latencyMs: Long? = 0L,
    @SerializedName("compliance_prediction") val compliancePrediction: String? = null,
    @SerializedName("detected_anomalies") val detectedAnomalies: List<String>? = emptyList(),
    @SerializedName("disclaimer") val disclaimer: String? = null
)

data class ConflictErrorDto(
    @SerializedName("error") val error: String,
    @SerializedName("message") val message: String,
    @SerializedName("conflicting_schedule") val conflictingSchedule: ConflictingScheduleDto? = null
)

data class ConflictingScheduleDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("program_title") val programTitle: String? = null,
    @SerializedName("start_time") val startTime: String? = null,
    @SerializedName("end_time") val endTime: String? = null,
    @SerializedName("channel") val channel: String? = null,
    @SerializedName("broadcast_date") val broadcastDate: String? = null
)

data class HealthResponseDto(
    @SerializedName("status") val status: String,
    @SerializedName("service") val service: String? = null,
    @SerializedName("database") val database: String? = null,
    @SerializedName("ml_model") val mlModel: String? = null,
    @SerializedName("timestamp") val timestamp: String? = null,
    @SerializedName("version") val version: String? = null
)

data class MessageResponseDto(
    @SerializedName("message") val message: String? = null,
    @SerializedName("error") val error: String? = null
)

// ==========================================
// ML EARLY-SESSION STALLING PREDICTION DTOS
// ==========================================

data class StallingRiskRequestDto(
    @SerializedName("year") val year: String,
    @SerializedName("location") val location: String,
    @SerializedName("bandwidth") val bandwidth: String,
    @SerializedName("adblock") val adblock: String,
    @SerializedName("dl_throughput_first1s") val dlThroughputFirst1s: Double,
    @SerializedName("dl_throughput_first5s") val dlThroughputFirst5s: Double,
    @SerializedName("dl_throughput_first10s") val dlThroughputFirst10s: Double
)

data class StallingRiskResponseDto(
    @SerializedName("predicted_class") val predictedClass: Int,
    @SerializedName("prediction_label") val predictionLabel: String,
    @SerializedName("stalling_risk_score") val stallingRiskScore: Double,
    @SerializedName("decision_threshold") val decisionThreshold: Double? = null,
    @SerializedName("score_semantics") val scoreSemantics: String? = null,
    @SerializedName("model_name") val modelName: String,
    @SerializedName("model_version") val modelVersion: String,
    @SerializedName("estimator") val estimator: String? = null,
    @SerializedName("latency_ms") val latencyMs: Double? = null,
    @SerializedName("timestamp") val timestamp: String? = null,
    @SerializedName("inputs") val inputs: Map<String, Any?>? = null,
    @SerializedName("provenance") val provenance: StallingProvenanceDto? = null
)

data class StallingProvenanceDto(
    @SerializedName("dataset") val dataset: String? = null,
    @SerializedName("attribution") val attribution: String? = null,
    @SerializedName("disclaimer") val disclaimer: String? = null
)

data class MlHealthResponseDto(
    @SerializedName("status") val status: String,
    @SerializedName("is_ready") val isReady: Boolean,
    @SerializedName("model_name") val modelName: String? = null,
    @SerializedName("model_version") val modelVersion: String? = null,
    @SerializedName("selected_model") val selectedModel: String? = null,
    @SerializedName("provenance") val provenance: Map<String, String>? = null
)
