package com.example.digitalbroadcastmonitoring.data.api

import com.example.digitalbroadcastmonitoring.data.api.dto.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // Health
    @GET("api/health")
    suspend fun getHealth(): Response<HealthResponseDto>

    // Programs
    @GET("api/programs")
    suspend fun getPrograms(
        @Query("channel") channel: String? = null,
        @Query("category") category: String? = null,
        @Query("search") search: String? = null
    ): Response<List<ProgramDto>>

    @GET("api/programs/{id}")
    suspend fun getProgram(
        @Path("id") id: String
    ): Response<ProgramDto>

    @POST("api/programs")
    suspend fun createProgram(
        @Body request: CreateProgramRequest
    ): Response<ProgramDto>

    @PUT("api/programs/{id}")
    suspend fun updateProgram(
        @Path("id") id: String,
        @Body request: UpdateProgramRequest
    ): Response<ProgramDto>

    @DELETE("api/programs/{id}")
    suspend fun deleteProgram(
        @Path("id") id: String
    ): Response<MessageResponseDto>

    // Schedules
    @GET("api/schedules")
    suspend fun getSchedules(
        @Query("date") date: String? = null,
        @Query("channel") channel: String? = null,
        @Query("status") status: String? = null,
        @Query("program_id") programId: String? = null
    ): Response<List<ScheduleDto>>

    @GET("api/schedules/{id}")
    suspend fun getSchedule(
        @Path("id") id: String
    ): Response<ScheduleDto>

    @POST("api/schedules")
    suspend fun createSchedule(
        @Body request: CreateScheduleRequest
    ): Response<ScheduleDto>

    @PUT("api/schedules/{id}")
    suspend fun updateSchedule(
        @Path("id") id: String,
        @Body request: UpdateScheduleRequest
    ): Response<ScheduleDto>

    @DELETE("api/schedules/{id}")
    suspend fun deleteSchedule(
        @Path("id") id: String
    ): Response<MessageResponseDto>

    @GET("api/schedules/{id}/analysis")
    suspend fun getScheduleAnalysis(
        @Path("id") id: String
    ): Response<AnalysisResponseDto>

    // Dashboard & Reports
    @GET("api/dashboard")
    suspend fun getDashboard(): Response<DashboardResponseDto>

    @GET("api/reports")
    suspend fun getReports(): Response<ReportsResponseDto>

    // Alerts
    @GET("api/alerts")
    suspend fun getAlerts(
        @Query("severity") severity: String? = null,
        @Query("channel") channel: String? = null,
        @Query("limit") limit: Int? = null
    ): Response<List<AlertDto>>

    @POST("api/alerts")
    suspend fun createAlert(
        @Body alert: AlertDto
    ): Response<AlertDto>

    @DELETE("api/alerts/{id}")
    suspend fun dismissAlert(
        @Path("id") id: String
    ): Response<MessageResponseDto>

    // ML Early-Session Stalling Risk Inference
    @GET("api/ml/health")
    suspend fun getMlHealth(): Response<MlHealthResponseDto>

    @POST("api/ml/stalling-risk")
    suspend fun predictStallingRisk(
        @Body request: StallingRiskRequestDto
    ): Response<StallingRiskResponseDto>
}
