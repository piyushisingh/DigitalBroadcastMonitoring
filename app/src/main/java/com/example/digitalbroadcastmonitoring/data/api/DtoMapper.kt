package com.example.digitalbroadcastmonitoring.data.api

import com.example.digitalbroadcastmonitoring.data.api.dto.*
import com.example.digitalbroadcastmonitoring.data.model.*

fun ProgramDto.toDomain(): Program {
    return Program(
        id = id,
        title = name,
        channel = channel,
        category = ProgramCategory.fromString(category),
        language = language ?: "English",
        videoStatus = StreamStatus.fromString(videoStatus ?: "Optimal"),
        audioStatus = StreamStatus.fromString(audioStatus ?: "Optimal"),
        complianceScore = complianceScore ?: 98,
        durationMinutes = duration,
        description = description ?: ""
    )
}

fun Program.toCreateRequest(): CreateProgramRequest {
    return CreateProgramRequest(
        id = id,
        name = title,
        channel = channel,
        category = category.displayName,
        language = language,
        duration = durationMinutes,
        description = description
    )
}

fun Program.toUpdateRequest(): UpdateProgramRequest {
    return UpdateProgramRequest(
        name = title,
        channel = channel,
        category = category.displayName,
        language = language,
        duration = durationMinutes,
        description = description
    )
}

fun ScheduleDto.toDomain(): BroadcastSchedule {
    return BroadcastSchedule(
        id = id,
        programId = programId,
        programTitle = programTitle ?: "Broadcast Slot",
        channel = channel,
        date = broadcastDate,
        startTime = startTime,
        endTime = endTime,
        status = ScheduleStatus.fromString(status),
        isVerified = isVerified == 1,
        hasOverlap = hasOverlap == 1,
        notes = remarks ?: ""
    )
}

fun BroadcastSchedule.toCreateRequest(): CreateScheduleRequest {
    return CreateScheduleRequest(
        id = id,
        programId = programId,
        channel = channel,
        broadcastDate = date,
        startTime = startTime,
        endTime = endTime,
        status = status.displayName,
        remarks = notes
    )
}

fun BroadcastSchedule.toUpdateRequest(): UpdateScheduleRequest {
    return UpdateScheduleRequest(
        channel = channel,
        broadcastDate = date,
        startTime = startTime,
        endTime = endTime,
        status = status.displayName,
        remarks = notes
    )
}

fun AlertDto.toDomain(): AlertItem {
    return AlertItem(
        id = id,
        timestamp = timestamp,
        title = title,
        description = description,
        severity = AlertSeverity.fromString(severity),
        channel = channel,
        programTitle = programTitle ?: "Broadcast Stream",
        isSimulated = isSimulated == 1
    )
}

fun DashboardResponseDto.toDomain(): DashboardMetrics {
    return DashboardMetrics(
        totalPrograms = metrics.totalPrograms,
        totalBroadcasts = metrics.totalBroadcasts,
        completedCount = metrics.completedCount,
        liveCount = metrics.liveCount,
        scheduledCount = metrics.scheduledCount,
        cancelledCount = metrics.cancelledCount,
        rescheduledCount = metrics.rescheduledCount,
        flaggedCount = metrics.flaggedCount,
        anomaliesCount = metrics.anomaliesCount,
        warningsCount = metrics.warningsCount,
        activeChannels = metrics.activeChannels,
        streamHealthPercent = metrics.streamHealthPercent,
        complianceRate = metrics.complianceRate,
        lastCheckedTimestamp = lastCheckedTimestamp ?: ""
    )
}

fun ReportsResponseDto.toDomain(): ReportsData {
    return ReportsData(
        totalPrograms = summary.totalPrograms,
        totalBroadcasts = summary.totalBroadcasts,
        completedCount = summary.completedCount,
        liveCount = summary.liveCount,
        scheduledCount = summary.scheduledCount,
        flaggedCount = summary.flaggedCount,
        totalDurationMinutes = summary.totalDurationMinutes,
        totalDurationHours = summary.totalDurationHours,
        totalDurationRemainMinutes = summary.totalDurationRemainMinutes,
        complianceRate = summary.complianceRate,
        categoryDistribution = categoryDistribution ?: emptyMap(),
        channelDistribution = channelDistribution?.map {
            ChannelReportItem(
                channel = it.channel,
                broadcastCount = it.broadcastCount,
                totalMinutes = it.totalMinutes
            )
        } ?: emptyList(),
        statusDistribution = statusDistribution ?: emptyMap()
    )
}

fun AnalysisResponseDto.toDomain(): ScheduleAnalysisResult {
    val domainSchedule = schedule.toDomain()
    val realMl = if (mlAnalysis.modelLoaded) {
        RealMlAnalysis(
            modelLoaded = true,
            modelStatus = mlAnalysis.modelStatus,
            modelType = mlAnalysis.modelType ?: "IsolationForest",
            isAnomaly = mlAnalysis.isAnomaly ?: false,
            anomalyScore = mlAnalysis.anomalyScore ?: 0.0f,
            rawDecisionScore = mlAnalysis.rawDecisionScore ?: 0.0f,
            latencyMs = mlAnalysis.latencyMs ?: 0L,
            compliancePrediction = mlAnalysis.compliancePrediction ?: "Unknown",
            detectedAnomalies = mlAnalysis.detectedAnomalies ?: emptyList(),
            disclaimer = mlAnalysis.disclaimer ?: ""
        )
    } else {
        RealMlAnalysis(
            modelLoaded = false,
            modelStatus = mlAnalysis.modelStatus,
            modelType = "None",
            isAnomaly = false,
            anomalyScore = 0.0f,
            rawDecisionScore = 0.0f,
            latencyMs = mlAnalysis.latencyMs ?: 0L,
            compliancePrediction = "Model not loaded",
            detectedAnomalies = listOf("ML model is not loaded on backend server."),
            disclaimer = mlAnalysis.disclaimer ?: ""
        )
    }

    return ScheduleAnalysisResult(
        schedule = domainSchedule,
        isValid = ruleValidation.isValid,
        ruleStatus = ruleValidation.status,
        ruleErrorMessages = ruleValidation.errorMessages,
        hasOverlap = ruleValidation.hasOverlap,
        durationMinutes = ruleValidation.durationMinutes,
        mlAnalysis = realMl
    )
}
