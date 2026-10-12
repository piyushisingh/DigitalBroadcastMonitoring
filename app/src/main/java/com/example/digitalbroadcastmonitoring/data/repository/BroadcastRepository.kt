package com.example.digitalbroadcastmonitoring.data.repository

import com.example.digitalbroadcastmonitoring.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object BroadcastRepository {

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.US)
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    // Initial Programs Mock Data (24 baseline programs)
    private val initialPrograms = listOf(
        Program(
            id = "prog-001",
            title = "Morning News",
            channel = "DD National",
            category = ProgramCategory.NEWS,
            language = "English / Hindi",
            videoStatus = StreamStatus.OPTIMAL,
            audioStatus = StreamStatus.OPTIMAL,
            complianceScore = 99,
            durationMinutes = 30,
            description = "Flagship early morning national news bulletin and headline summary."
        ),
        Program(
            id = "prog-002",
            title = "Educational Hour",
            channel = "DD Bharati",
            category = ProgramCategory.TALK_SHOW,
            language = "Hindi / English",
            videoStatus = StreamStatus.OPTIMAL,
            audioStatus = StreamStatus.OPTIMAL,
            complianceScore = 97,
            durationMinutes = 60,
            description = "Interactive STEM and humanities lecture broadcast for secondary education."
        ),
        Program(
            id = "prog-003",
            title = "Documentary Special",
            channel = "DD India",
            category = ProgramCategory.DOCUMENTARY,
            language = "English",
            videoStatus = StreamStatus.OPTIMAL,
            audioStatus = StreamStatus.OPTIMAL,
            complianceScore = 95,
            durationMinutes = 45,
            description = "Exploration of cultural heritage, archaeological marvels and environmental conservation."
        ),
        Program(
            id = "prog-004",
            title = "National Prime News Hour",
            channel = "DD National HD",
            category = ProgramCategory.NEWS,
            language = "English / Hindi",
            videoStatus = StreamStatus.OPTIMAL,
            audioStatus = StreamStatus.OPTIMAL,
            complianceScore = 98,
            durationMinutes = 60,
            description = "Daily flagship evening bulletin covering national and international affairs."
        ),
        Program(
            id = "prog-005",
            title = "Cricket Championship Live",
            channel = "DD Sports HD",
            category = ProgramCategory.SPORTS,
            language = "Hindi",
            videoStatus = StreamStatus.OPTIMAL,
            audioStatus = StreamStatus.OPTIMAL,
            complianceScore = 94,
            durationMinutes = 180,
            description = "Live telecast of the regional championship semi-finals with multi-audio feeds."
        ),
        Program(
            id = "prog-006",
            title = "Heritage of India",
            channel = "DD Bharati",
            category = ProgramCategory.DOCUMENTARY,
            language = "English",
            videoStatus = StreamStatus.DEGRADED,
            audioStatus = StreamStatus.OPTIMAL,
            complianceScore = 88,
            durationMinutes = 45,
            description = "Architectural survey of classical temples and medieval forts."
        ),
        Program(
            id = "prog-007",
            title = "Morning Financial Express",
            channel = "DD India",
            category = ProgramCategory.TALK_SHOW,
            language = "English",
            videoStatus = StreamStatus.OPTIMAL,
            audioStatus = StreamStatus.OPTIMAL,
            complianceScore = 97,
            durationMinutes = 30,
            description = "Market opening analysis and interviews with central trade analysts."
        ),
        Program(
            id = "prog-008",
            title = "Folk Melodies & Classical Dance",
            channel = "DD Retro",
            category = ProgramCategory.ENTERTAINMENT,
            language = "Hindi",
            videoStatus = StreamStatus.OPTIMAL,
            audioStatus = StreamStatus.CRITICAL,
            complianceScore = 82,
            durationMinutes = 90,
            description = "Archival broadcasts of classical concerts and traditional folk recitals."
        ),
        Program(
            id = "prog-009",
            title = "Public Service: Health Awareness",
            channel = "DD National",
            category = ProgramCategory.COMMERCIAL,
            language = "Multilingual",
            videoStatus = StreamStatus.OPTIMAL,
            audioStatus = StreamStatus.OPTIMAL,
            complianceScore = 100,
            durationMinutes = 15,
            description = "Government sponsored public awareness announcement block."
        ),
        Program(id = "prog-010", title = "Agricultural Horizons", channel = "DD Kisan", category = ProgramCategory.TALK_SHOW, durationMinutes = 45, description = "Farmer advisories and rural economy discussions."),
        Program(id = "prog-011", title = "Parliamentary Proceedings Live", channel = "Sansad TV", category = ProgramCategory.NEWS, durationMinutes = 120, description = "Live parliamentary floor debates."),
        Program(id = "prog-012", title = "Science & Tech Frontiers", channel = "DD India", category = ProgramCategory.DOCUMENTARY, durationMinutes = 30, description = "Innovations in AI, rocketry and green energy."),
        Program(id = "prog-013", title = "Youth & Culture Pulse", channel = "DD Bharati", category = ProgramCategory.ENTERTAINMENT, durationMinutes = 60, description = "Spotlight on contemporary theatre and creative arts."),
        Program(id = "prog-014", title = "Badminton Super League", channel = "DD Sports HD", category = ProgramCategory.SPORTS, durationMinutes = 90, description = "National badminton tournament matches."),
        Program(id = "prog-015", title = "Sanskrit Varta & Bulletin", channel = "DD National", category = ProgramCategory.NEWS, durationMinutes = 15, description = "Daily Sanskrit language news telecast."),
        Program(id = "prog-016", title = "Wildlife Sanctuaries: Western Ghats", channel = "DD Bharati", category = ProgramCategory.DOCUMENTARY, durationMinutes = 45, description = "Biodiversity hotspots documentation."),
        Program(id = "prog-017", title = "Consumer Rights Watch", channel = "DD National", category = ProgramCategory.TALK_SHOW, durationMinutes = 30, description = "Legal protections for retail consumers."),
        Program(id = "prog-018", title = "Traditional Yoga for Wellness", channel = "DD India", category = ProgramCategory.TALK_SHOW, durationMinutes = 30, description = "Morning wellness instruction and asanas."),
        Program(id = "prog-019", title = "Classical Sitar & Tabla Duet", channel = "DD Retro", category = ProgramCategory.ENTERTAINMENT, durationMinutes = 60, description = "Maestros in concert archives."),
        Program(id = "prog-020", title = "Global Trade Digest", channel = "DD India", category = ProgramCategory.TALK_SHOW, durationMinutes = 30, description = "Import/export trends and macroeconomic indicators."),
        Program(id = "prog-021", title = "Women Champions in Sports", channel = "DD Sports HD", category = ProgramCategory.SPORTS, durationMinutes = 45, description = "Profile interviews with Olympic medalists."),
        Program(id = "prog-022", title = "Road Safety Initiative", channel = "DD National", category = ProgramCategory.COMMERCIAL, durationMinutes = 15, description = "Public advocacy campaign on highway safety."),
        Program(id = "prog-023", title = "Literature & Philosophy Forum", channel = "DD Bharati", category = ProgramCategory.TALK_SHOW, durationMinutes = 60, description = "Discussion with eminent authors and essayists."),
        Program(id = "prog-024", title = "Midday Speed News", channel = "DD National HD", category = ProgramCategory.NEWS, durationMinutes = 30, description = "Rapid 50 top stories bulletin.")
    )

    // Initial Schedules Mock Data (Today's broadcasts)
    private val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    // 8 broadcasts today: 3 visible + 5 remaining
    private val initialSchedules = listOf(
        // 1. Morning News: 10:00–10:30 AM, 30 minutes, Completed
        BroadcastSchedule(
            id = "sch-101",
            programId = "prog-001",
            programTitle = "Morning News",
            channel = "DD National",
            date = todayDate,
            startTime = "10:00",
            endTime = "10:30",
            status = ScheduleStatus.COMPLETED,
            isVerified = true,
            hasOverlap = false,
            notes = "Morning news bulletin completed on schedule."
        ),
        // 2. Educational Hour: 11:00 AM–12:00 PM, 60 minutes, Live
        BroadcastSchedule(
            id = "sch-102",
            programId = "prog-002",
            programTitle = "Educational Hour",
            channel = "DD Bharati",
            date = todayDate,
            startTime = "11:00",
            endTime = "12:00",
            status = ScheduleStatus.LIVE,
            isVerified = true,
            hasOverlap = false,
            notes = "On-air stream live. Video and audio nominal."
        ),
        // 3. Documentary Special: 12:00–12:45 PM, 45 minutes, Scheduled
        BroadcastSchedule(
            id = "sch-103",
            programId = "prog-003",
            programTitle = "Documentary Special",
            channel = "DD India",
            date = todayDate,
            startTime = "12:00",
            endTime = "12:45",
            status = ScheduleStatus.SCHEDULED,
            isVerified = true,
            hasOverlap = false,
            notes = "Scheduled transmission ingest ready."
        ),
        // Remaining 5 broadcasts today (total 8)
        BroadcastSchedule(
            id = "sch-104",
            programId = "prog-005",
            programTitle = "Cricket Championship Live",
            channel = "DD Sports HD",
            date = todayDate,
            startTime = "14:00",
            endTime = "17:00",
            status = ScheduleStatus.SCHEDULED,
            isVerified = true,
            hasOverlap = false,
            notes = "Multi-camera stadium feed."
        ),
        BroadcastSchedule(
            id = "sch-105",
            programId = "prog-004",
            programTitle = "National Prime News Hour",
            channel = "DD National HD",
            date = todayDate,
            startTime = "20:00",
            endTime = "21:00",
            status = ScheduleStatus.SCHEDULED,
            isVerified = true,
            hasOverlap = false,
            notes = "Evening prime bulletin."
        ),
        BroadcastSchedule(
            id = "sch-106",
            programId = "prog-009",
            programTitle = "Public Service: Health Awareness",
            channel = "DD National",
            date = todayDate,
            startTime = "21:00",
            endTime = "21:15",
            status = ScheduleStatus.SCHEDULED,
            isVerified = true,
            hasOverlap = false,
            notes = "Statutory public health message."
        ),
        BroadcastSchedule(
            id = "sch-107",
            programId = "prog-008",
            programTitle = "Folk Melodies & Classical Dance",
            channel = "DD Retro",
            date = todayDate,
            startTime = "21:30",
            endTime = "23:00",
            status = ScheduleStatus.SCHEDULED,
            isVerified = false,
            hasOverlap = false,
            notes = "Archival music programming."
        ),
        BroadcastSchedule(
            id = "sch-108",
            programId = "prog-006",
            programTitle = "Heritage of India",
            channel = "DD Bharati",
            date = todayDate,
            startTime = "23:15",
            endTime = "23:45",
            status = ScheduleStatus.SCHEDULED,
            isVerified = true,
            hasOverlap = false,
            notes = "Late night cultural broadcast."
        )
    )

    // Initial Alerts with Schedule-overlap and Unusual-duration alerts
    private val initialAlerts = listOf(
        AlertItem(
            id = "alt-01",
            timestamp = "11:45:00 AM IST",
            title = "Schedule Overlap Detected",
            description = "Conflicting broadcast intervals detected: Educational Hour overlaps with Special Bulletin on DD National mux feed.",
            severity = AlertSeverity.ANOMALY,
            channel = "DD National",
            programTitle = "Educational Hour",
            isSimulated = false
        ),
        AlertItem(
            id = "alt-02",
            timestamp = "10:35:12 AM IST",
            title = "Unusual Duration Alert",
            description = "Transmission elapsed duration exceeded programmed schedule slot limit by 15.4 minutes.",
            severity = AlertSeverity.WARNING,
            channel = "DD Sports HD",
            programTitle = "Morning News",
            isSimulated = false
        )
    )

    private val _programs = MutableStateFlow(initialPrograms)
    val programs: StateFlow<List<Program>> = _programs.asStateFlow()

    private val _schedules = MutableStateFlow(initialSchedules)
    val schedules: StateFlow<List<BroadcastSchedule>> = _schedules.asStateFlow()

    private val _alerts = MutableStateFlow(initialAlerts)
    val alerts: StateFlow<List<AlertItem>> = _alerts.asStateFlow()

    private val _lastRefreshTime = MutableStateFlow(SimpleDateFormat("hh:mm:ss a", Locale.US).format(Date()) + " IST")
    val lastRefreshTime: StateFlow<String> = _lastRefreshTime.asStateFlow()

    fun refreshData() {
        _lastRefreshTime.value = SimpleDateFormat("hh:mm:ss a", Locale.US).format(Date()) + " IST"
    }

    // Program CRUD
    fun addProgram(program: Program) {
        _programs.value = listOf(program) + _programs.value
    }

    fun updateProgram(updated: Program) {
        _programs.value = _programs.value.map { if (it.id == updated.id) updated else it }
        // Also sync programTitle in schedules if title updated
        _schedules.value = _schedules.value.map {
            if (it.programId == updated.id) it.copy(programTitle = updated.title, channel = updated.channel) else it
        }
    }

    fun deleteProgram(programId: String) {
        _programs.value = _programs.value.filter { it.id != programId }
        _schedules.value = _schedules.value.filter { it.programId != programId }
    }

    fun getProgramById(id: String): Program? {
        return _programs.value.find { it.id == id }
    }

    // Schedule CRUD
    fun addSchedule(schedule: BroadcastSchedule): Boolean {
        // Validate overlaps
        val overlap = checkOverlap(schedule.channel, schedule.date, schedule.startTime, schedule.endTime)
        val finalSchedule = schedule.copy(hasOverlap = overlap)
        _schedules.value = _schedules.value + finalSchedule
        return !overlap
    }

    fun updateSchedule(updated: BroadcastSchedule): Boolean {
        val overlap = checkOverlap(updated.channel, updated.date, updated.startTime, updated.endTime, excludeId = updated.id)
        val finalSchedule = updated.copy(hasOverlap = overlap)
        _schedules.value = _schedules.value.map { if (it.id == updated.id) finalSchedule else it }
        return !overlap
    }

    fun deleteSchedule(scheduleId: String) {
        _schedules.value = _schedules.value.filter { it.id != scheduleId }
    }

    fun getScheduleById(id: String): BroadcastSchedule? {
        return _schedules.value.find { it.id == id }
    }

    // Overlap validation logic
    fun checkOverlap(
        channel: String,
        date: String,
        startTime: String,
        endTime: String,
        excludeId: String? = null
    ): Boolean {
        val startMin = parseTimeToMinutes(startTime) ?: return false
        val endMin = parseTimeToMinutes(endTime) ?: return false

        return _schedules.value.any { other ->
            if (other.id == excludeId) return@any false
            if (other.channel.equals(channel, ignoreCase = true) && other.date == date) {
                val oStart = parseTimeToMinutes(other.startTime) ?: return@any false
                val oEnd = parseTimeToMinutes(other.endTime) ?: return@any false
                // Check if intervals [startMin, endMin) and [oStart, oEnd) intersect
                startMin < oEnd && endMin > oStart
            } else {
                false
            }
        }
    }

    // Time Validation
    fun parseTimeToMinutes(timeStr: String): Int? {
        return try {
            val parts = timeStr.trim().split(":")
            if (parts.size == 2) {
                val hours = parts[0].toInt()
                val minutes = parts[1].toInt()
                hours * 60 + minutes
            } else null
        } catch (e: Exception) {
            null
        }
    }

    fun calculateDurationMinutes(startTime: String, endTime: String): Int {
        val s = parseTimeToMinutes(startTime) ?: 0
        val e = parseTimeToMinutes(endTime) ?: 0
        val diff = e - s
        return if (diff > 0) diff else 0
    }

    // Full Rule-Based Validation
    fun performRuleBasedValidation(): RuleValidationResult {
        val allSchedules = _schedules.value
        val invalidTime = mutableListOf<BroadcastSchedule>()
        val overlapping = mutableListOf<Pair<BroadcastSchedule, BroadcastSchedule>>()
        val errors = mutableListOf<String>()

        // 1. Time validity check
        for (sch in allSchedules) {
            val s = parseTimeToMinutes(sch.startTime)
            val e = parseTimeToMinutes(sch.endTime)
            if (s == null || e == null || e <= s) {
                invalidTime.add(sch)
                errors.add("Invalid slot timing for '${sch.programTitle}' (${sch.startTime} - ${sch.endTime})")
            }
        }

        // 2. Overlap check across all schedules
        for (i in allSchedules.indices) {
            for (j in i + 1 until allSchedules.size) {
                val a = allSchedules[i]
                val b = allSchedules[j]
                if (a.channel.equals(b.channel, ignoreCase = true) && a.date == b.date) {
                    val aStart = parseTimeToMinutes(a.startTime) ?: continue
                    val aEnd = parseTimeToMinutes(a.endTime) ?: continue
                    val bStart = parseTimeToMinutes(b.startTime) ?: continue
                    val bEnd = parseTimeToMinutes(b.endTime) ?: continue
                    if (aStart < bEnd && aEnd > bStart) {
                        overlapping.add(Pair(a, b))
                        errors.add("Conflict detected on ${a.channel}: '${a.programTitle}' overlaps with '${b.programTitle}'")
                    }
                }
            }
        }

        return RuleValidationResult(
            isValid = invalidTime.isEmpty() && overlapping.isEmpty(),
            overlappingSchedules = overlapping,
            invalidTimeSchedules = invalidTime,
            errorMessages = errors
        )
    }

    // Simulated ML Analysis (Strictly SIMULATED)
    fun runSimulatedMlAnalysis(programId: String?): SimulatedMlAnalysis {
        val program = programId?.let { getProgramById(it) } ?: _programs.value.firstOrNull()
        val isAnomaly = program?.videoStatus != StreamStatus.OPTIMAL || program?.audioStatus != StreamStatus.OPTIMAL

        return SimulatedMlAnalysis(
            isSimulated = true,
            anomalyScore = if (isAnomaly) 0.84f else 0.08f,
            audioQualityScore = if (program?.audioStatus == StreamStatus.OPTIMAL) 98.5f else 72.0f,
            videoQualityScore = if (program?.videoStatus == StreamStatus.OPTIMAL) 99.1f else 68.4f,
            latencyMs = 18L,
            compliancePrediction = if (isAnomaly) "Action Required: Telemetry deviation" else "Optimal Compliance (>98%)",
            detectedAnomalies = if (isAnomaly) {
                listOf(
                    "Simulated Video Stream Jitter deviation (+14.2%)",
                    "Simulated Audio Clipping probability detected on Ch-1"
                )
            } else {
                listOf("No simulated anomalies detected in current buffer")
            }
        )
    }

    // Alert Actions
    fun dismissAlert(id: String) {
        _alerts.value = _alerts.value.filter { it.id != id }
    }

    fun addAlert(alert: AlertItem) {
        _alerts.value = listOf(alert) + _alerts.value
    }
}
