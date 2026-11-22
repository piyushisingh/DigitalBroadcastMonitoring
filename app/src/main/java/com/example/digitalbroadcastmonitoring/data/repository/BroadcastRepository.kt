package com.example.digitalbroadcastmonitoring.data.repository

import android.util.Log
import com.example.digitalbroadcastmonitoring.data.api.ApiClient
import com.example.digitalbroadcastmonitoring.data.api.NetworkResult
import com.example.digitalbroadcastmonitoring.data.api.dto.*
import com.example.digitalbroadcastmonitoring.data.api.toCreateRequest
import com.example.digitalbroadcastmonitoring.data.api.toDomain
import com.example.digitalbroadcastmonitoring.data.api.toUpdateRequest
import com.example.digitalbroadcastmonitoring.data.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BroadcastRepository {

    private const val TAG = "BroadcastRepo"
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.US)
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    // Initial Baseline Programs (for offline or initial render)
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

    private val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private val initialSchedules = listOf(
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

    // Observable States
    private val _programs = MutableStateFlow(initialPrograms)
    val programs: StateFlow<List<Program>> = _programs.asStateFlow()

    private val _schedules = MutableStateFlow(initialSchedules)
    val schedules: StateFlow<List<BroadcastSchedule>> = _schedules.asStateFlow()

    private val _alerts = MutableStateFlow(initialAlerts)
    val alerts: StateFlow<List<AlertItem>> = _alerts.asStateFlow()

    private val _dashboardMetrics = MutableStateFlow(
        DashboardMetrics(
            totalPrograms = 24,
            totalBroadcasts = 8,
            completedCount = 1,
            liveCount = 1,
            scheduledCount = 6,
            anomaliesCount = 1,
            warningsCount = 1,
            activeChannels = 8,
            streamHealthPercent = 91,
            complianceRate = 96.0f,
            lastCheckedTimestamp = SimpleDateFormat("hh:mm:ss a", Locale.US).format(Date()) + " IST"
        )
    )
    val dashboardMetrics: StateFlow<DashboardMetrics> = _dashboardMetrics.asStateFlow()

    private val _reportsData = MutableStateFlow(
        ReportsData(
            totalPrograms = 24,
            totalBroadcasts = 8,
            completedCount = 1,
            liveCount = 1,
            scheduledCount = 6,
            totalDurationMinutes = 510,
            totalDurationHours = 8,
            totalDurationRemainMinutes = 30,
            complianceRate = 96.0f
        )
    )
    val reportsData: StateFlow<ReportsData> = _reportsData.asStateFlow()

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.CONNECTING)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _lastErrorMessage = MutableStateFlow<String?>(null)
    val lastErrorMessage: StateFlow<String?> = _lastErrorMessage.asStateFlow()

    private val _lastRefreshTime = MutableStateFlow(SimpleDateFormat("hh:mm:ss a", Locale.US).format(Date()) + " IST")
    val lastRefreshTime: StateFlow<String> = _lastRefreshTime.asStateFlow()

    private val _syncFeedback = MutableStateFlow<SyncFeedback?>(null)
    val syncFeedback: StateFlow<SyncFeedback?> = _syncFeedback.asStateFlow()

    fun clearSyncFeedback() {
        _syncFeedback.value = null
    }

    init {
        // Trigger background initial sync on startup
        refreshData()
    }

    /**
     * Refreshes all data by querying backend endpoints asynchronously.
     * Retains fallback in-memory data if the server is temporarily unreachable.
     */
    fun refreshData() {
        _isLoading.value = true
        _connectionStatus.value = ConnectionStatus.CONNECTING
        val currentTime = SimpleDateFormat("hh:mm:ss a", Locale.US).format(Date()) + " IST"

        repositoryScope.launch {
            Log.i(TAG, "Sync Feed triggered. Connecting to backend at: ${ApiClient.getBaseUrl()}")
            // Slight delay ensures the UI loading state is visible to user even on instantaneous local loopback
            delay(200)

            try {
                // 1. Health check
                val healthResult = ApiClient.safeApiCall { ApiClient.getService().getHealth() }
                if (healthResult is NetworkResult.Success) {
                    _connectionStatus.value = ConnectionStatus.ONLINE
                    _lastErrorMessage.value = null

                    // 2. Load dashboard metrics from backend
                    val dashResult = ApiClient.safeApiCall { ApiClient.getService().getDashboard() }
                    if (dashResult is NetworkResult.Success) {
                        _dashboardMetrics.value = dashResult.data.toDomain()
                        Log.d(TAG, "Dashboard metrics loaded successfully")
                    } else if (dashResult is NetworkResult.Error) {
                        Log.w(TAG, "Dashboard endpoint returned error: ${dashResult.message}")
                    }

                    // 3. Load programs from backend
                    val progResult = ApiClient.safeApiCall { ApiClient.getService().getPrograms() }
                    var progCount = _programs.value.size
                    if (progResult is NetworkResult.Success) {
                        val mappedPrograms = progResult.data.map { it.toDomain() }
                        _programs.value = mappedPrograms
                        progCount = mappedPrograms.size
                        Log.d(TAG, "Loaded $progCount programs from backend")
                    } else if (progResult is NetworkResult.Error) {
                        Log.w(TAG, "Programs endpoint returned error: ${progResult.message}")
                    }

                    // 4. Load schedules from backend
                    val schedResult = ApiClient.safeApiCall { ApiClient.getService().getSchedules() }
                    var schedCount = _schedules.value.size
                    if (schedResult is NetworkResult.Success) {
                        val mappedSchedules = schedResult.data.map { it.toDomain() }
                        _schedules.value = mappedSchedules
                        schedCount = mappedSchedules.size
                        Log.d(TAG, "Loaded $schedCount schedules from backend")
                    } else if (schedResult is NetworkResult.Error) {
                        Log.w(TAG, "Schedules endpoint returned error: ${schedResult.message}")
                    }

                    // 5. Load alerts from backend
                    val alertResult = ApiClient.safeApiCall { ApiClient.getService().getAlerts() }
                    if (alertResult is NetworkResult.Success) {
                        _alerts.value = alertResult.data.map { it.toDomain() }
                        Log.d(TAG, "Loaded ${_alerts.value.size} alerts from backend")
                    } else if (alertResult is NetworkResult.Error) {
                        Log.w(TAG, "Alerts endpoint returned error: ${alertResult.message}")
                    }

                    // 6. Load reports from backend
                    val repResult = ApiClient.safeApiCall { ApiClient.getService().getReports() }
                    if (repResult is NetworkResult.Success) {
                        _reportsData.value = repResult.data.toDomain()
                        Log.d(TAG, "Reports data loaded successfully")
                    } else if (repResult is NetworkResult.Error) {
                        Log.w(TAG, "Reports endpoint returned error: ${repResult.message}")
                    }

                    val updatedTime = SimpleDateFormat("hh:mm:ss a", Locale.US).format(Date()) + " IST"
                    _lastRefreshTime.value = updatedTime
                    _syncFeedback.value = SyncFeedback(
                        isSuccess = true,
                        message = "Synchronized $schedCount broadcast schedules & $progCount programs from backend",
                        timestamp = "Synced at $updatedTime"
                    )
                    Log.i(TAG, "Sync Feed completed successfully at $updatedTime")
                } else if (healthResult is NetworkResult.Error) {
                    _connectionStatus.value = ConnectionStatus.OFFLINE
                    _lastErrorMessage.value = healthResult.message
                    _syncFeedback.value = SyncFeedback(
                        isSuccess = false,
                        message = "Sync failed: ${healthResult.message}",
                        timestamp = "Attempted at $currentTime",
                        isError = true
                    )
                    Log.e(TAG, "Sync Feed failed: Health check error: ${healthResult.message}")
                }
            } catch (e: Exception) {
                val errMsg = e.localizedMessage ?: "Unexpected network error"
                _connectionStatus.value = ConnectionStatus.OFFLINE
                _lastErrorMessage.value = errMsg
                _syncFeedback.value = SyncFeedback(
                    isSuccess = false,
                    message = "Sync failed: $errMsg",
                    timestamp = "Attempted at $currentTime",
                    isError = true
                )
                Log.e(TAG, "Sync Feed failed with exception: $errMsg", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Program CRUD Operations with Backend Integration
    fun addProgram(program: Program, onResult: (NetworkResult<Program>) -> Unit = {}) {
        // Optimistic local update
        _programs.value = listOf(program) + _programs.value

        repositoryScope.launch {
            val result = ApiClient.safeApiCall {
                ApiClient.getService().createProgram(program.toCreateRequest())
            }
            withContext(Dispatchers.Main) {
                when (result) {
                    is NetworkResult.Success -> {
                        val created = result.data.toDomain()
                        _programs.value = _programs.value.map { if (it.id == program.id) created else it }
                        refreshData()
                        onResult(NetworkResult.Success(created))
                    }
                    is NetworkResult.Error -> {
                        _lastErrorMessage.value = result.message
                        onResult(result)
                    }
                    NetworkResult.Loading -> {}
                }
            }
        }
    }

    fun updateProgram(updated: Program, onResult: (NetworkResult<Program>) -> Unit = {}) {
        _programs.value = _programs.value.map { if (it.id == updated.id) updated else it }
        _schedules.value = _schedules.value.map {
            if (it.programId == updated.id) it.copy(programTitle = updated.title, channel = updated.channel) else it
        }

        repositoryScope.launch {
            val result = ApiClient.safeApiCall {
                ApiClient.getService().updateProgram(updated.id, updated.toUpdateRequest())
            }
            withContext(Dispatchers.Main) {
                when (result) {
                    is NetworkResult.Success -> {
                        val res = result.data.toDomain()
                        _programs.value = _programs.value.map { if (it.id == updated.id) res else it }
                        refreshData()
                        onResult(NetworkResult.Success(res))
                    }
                    is NetworkResult.Error -> {
                        _lastErrorMessage.value = result.message
                        onResult(result)
                    }
                    NetworkResult.Loading -> {}
                }
            }
        }
    }

    fun deleteProgram(programId: String, onResult: (NetworkResult<Unit>) -> Unit = {}) {
        _programs.value = _programs.value.filter { it.id != programId }
        _schedules.value = _schedules.value.filter { it.programId != programId }

        repositoryScope.launch {
            val result = ApiClient.safeApiCall {
                ApiClient.getService().deleteProgram(programId)
            }
            withContext(Dispatchers.Main) {
                when (result) {
                    is NetworkResult.Success -> {
                        refreshData()
                        onResult(NetworkResult.Success(Unit))
                    }
                    is NetworkResult.Error -> {
                        _lastErrorMessage.value = result.message
                        onResult(result)
                    }
                    NetworkResult.Loading -> {}
                }
            }
        }
    }

    fun getProgramById(id: String): Program? {
        return _programs.value.find { it.id == id }
    }

    // Schedule CRUD Operations with Overlap & Conflict Handling
    fun addSchedule(
        schedule: BroadcastSchedule,
        onResult: (NetworkResult<BroadcastSchedule>) -> Unit = {}
    ): Boolean {
        // Synchronous rule check for immediate UI responsiveness
        val localOverlap = checkOverlap(schedule.channel, schedule.date, schedule.startTime, schedule.endTime)
        val candidate = schedule.copy(hasOverlap = localOverlap)

        repositoryScope.launch {
            val result = ApiClient.safeApiCall {
                ApiClient.getService().createSchedule(schedule.toCreateRequest())
            }
            withContext(Dispatchers.Main) {
                when (result) {
                    is NetworkResult.Success -> {
                        val created = result.data.toDomain()
                        _schedules.value = _schedules.value.filter { it.id != schedule.id } + created
                        refreshData()
                        onResult(NetworkResult.Success(created))
                    }
                    is NetworkResult.Error -> {
                        _lastErrorMessage.value = result.message
                        onResult(result)
                    }
                    NetworkResult.Loading -> {}
                }
            }
        }

        // Return whether local check passed (optimistic)
        return !localOverlap
    }

    fun updateSchedule(
        updated: BroadcastSchedule,
        onResult: (NetworkResult<BroadcastSchedule>) -> Unit = {}
    ): Boolean {
        val localOverlap = checkOverlap(updated.channel, updated.date, updated.startTime, updated.endTime, excludeId = updated.id)

        repositoryScope.launch {
            val result = ApiClient.safeApiCall {
                ApiClient.getService().updateSchedule(updated.id, updated.toUpdateRequest())
            }
            withContext(Dispatchers.Main) {
                when (result) {
                    is NetworkResult.Success -> {
                        val res = result.data.toDomain()
                        _schedules.value = _schedules.value.map { if (it.id == updated.id) res else it }
                        refreshData()
                        onResult(NetworkResult.Success(res))
                    }
                    is NetworkResult.Error -> {
                        _lastErrorMessage.value = result.message
                        onResult(result)
                    }
                    NetworkResult.Loading -> {}
                }
            }
        }

        return !localOverlap
    }

    fun deleteSchedule(scheduleId: String, onResult: (NetworkResult<Unit>) -> Unit = {}) {
        _schedules.value = _schedules.value.filter { it.id != scheduleId }

        repositoryScope.launch {
            val result = ApiClient.safeApiCall {
                ApiClient.getService().deleteSchedule(scheduleId)
            }
            withContext(Dispatchers.Main) {
                when (result) {
                    is NetworkResult.Success -> {
                        refreshData()
                        onResult(NetworkResult.Success(Unit))
                    }
                    is NetworkResult.Error -> {
                        _lastErrorMessage.value = result.message
                        onResult(result)
                    }
                    NetworkResult.Loading -> {}
                }
            }
        }
    }

    fun getScheduleById(id: String): BroadcastSchedule? {
        return _schedules.value.find { it.id == id }
    }

    // Schedule Analysis (Deterministic Rule + Real ML Anomaly Inference)
    fun fetchScheduleAnalysis(
        scheduleId: String,
        onResult: (NetworkResult<ScheduleAnalysisResult>) -> Unit
    ) {
        repositoryScope.launch {
            val result = ApiClient.safeApiCall {
                ApiClient.getService().getScheduleAnalysis(scheduleId)
            }
            withContext(Dispatchers.Main) {
                when (result) {
                    is NetworkResult.Success -> {
                        onResult(NetworkResult.Success(result.data.toDomain()))
                    }
                    is NetworkResult.Error -> {
                        // Fallback to local rule evaluation if server unavailable
                        val sched = getScheduleById(scheduleId)
                        if (sched != null) {
                            val localRule = performRuleBasedValidation()
                            val localSimMl = runSimulatedMlAnalysis(sched.programId)
                            val fallbackResult = ScheduleAnalysisResult(
                                schedule = sched,
                                isValid = !sched.hasOverlap,
                                ruleStatus = if (sched.hasOverlap) "FAIL" else "PASS",
                                ruleErrorMessages = if (sched.hasOverlap) listOf("Conflict detected on ${sched.channel}") else emptyList(),
                                hasOverlap = sched.hasOverlap,
                                durationMinutes = calculateDurationMinutes(sched.startTime, sched.endTime),
                                mlAnalysis = RealMlAnalysis(
                                    modelLoaded = false,
                                    modelStatus = "OFFLINE_FALLBACK",
                                    modelType = "Simulated Fallback",
                                    isAnomaly = localSimMl.anomalyScore > 0.5f,
                                    anomalyScore = localSimMl.anomalyScore,
                                    rawDecisionScore = 0.0f,
                                    latencyMs = localSimMl.latencyMs,
                                    compliancePrediction = localSimMl.compliancePrediction,
                                    detectedAnomalies = localSimMl.detectedAnomalies,
                                    disclaimer = "Server offline. Displaying local simulated heuristics fallback."
                                )
                            )
                            onResult(NetworkResult.Success(fallbackResult))
                        } else {
                            onResult(result)
                        }
                    }
                    NetworkResult.Loading -> {}
                }
            }
        }
    }

    // Machine Learning QoE Stalling Prediction (MUEXLab Model)
    fun predictStallingRisk(
        request: StallingRiskRequestDto,
        onResult: (NetworkResult<StallingRiskResponseDto>) -> Unit
    ) {
        repositoryScope.launch {
            val result = ApiClient.safeApiCall {
                ApiClient.getService().predictStallingRisk(request)
            }
            withContext(Dispatchers.Main) {
                onResult(result)
            }
        }
    }

    fun fetchMlHealth(
        onResult: (NetworkResult<MlHealthResponseDto>) -> Unit
    ) {
        repositoryScope.launch {
            val result = ApiClient.safeApiCall {
                ApiClient.getService().getMlHealth()
            }
            withContext(Dispatchers.Main) {
                onResult(result)
            }
        }
    }

    // Alerts CRUD
    fun dismissAlert(id: String) {
        _alerts.value = _alerts.value.filter { it.id != id }

        repositoryScope.launch {
            ApiClient.safeApiCall { ApiClient.getService().dismissAlert(id) }
            refreshData()
        }
    }

    fun addAlert(alert: AlertItem) {
        _alerts.value = listOf(alert) + _alerts.value
    }

    // Helper: Local Overlap validation logic
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

    // Helper: Time Formatting
    fun parseTimeToMinutes(timeStr: String): Int? {
        return try {
            val parts = timeStr.trim().split(":")
            if (parts.size >= 2) {
                val hours = parts[0].toInt()
                val minutes = parts[1].toInt()
                hours * 60 + minutes
            } else null
        } catch (_: Exception) {
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

    // Simulated ML Analysis (Strictly SIMULATED Fallback)
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
}
