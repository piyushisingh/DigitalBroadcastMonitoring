package com.example.digitalbroadcastmonitoring.data.api

import android.util.Log
import com.example.digitalbroadcastmonitoring.data.api.dto.ConflictErrorDto
import com.example.digitalbroadcastmonitoring.data.api.dto.ConflictingScheduleDto
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

sealed class NetworkResult<out T> {
    data class Success<out T>(val data: T) : NetworkResult<T>()
    data class Error(
        val message: String,
        val code: Int? = null,
        val conflictSchedule: ConflictingScheduleDto? = null
    ) : NetworkResult<Nothing>()
    data object Loading : NetworkResult<Nothing>()
}

object ApiClient {

    private const val TAG = "ApiClient"

    // Default to Android Studio Emulator local loopback alias
    const val DEFAULT_BASE_URL = "http://10.0.2.2:5000/"

    @Volatile
    private var baseUrl: String = DEFAULT_BASE_URL

    private val gson: Gson = GsonBuilder()
        .setLenient()
        .create()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    @Volatile
    private var retrofit: Retrofit? = null

    @Volatile
    private var apiServiceInstance: ApiService? = null

    fun getBaseUrl(): String = baseUrl

    /**
     * Updates the base URL at runtime (e.g., for physical device IP testing or production).
     * Ensures trailing slash is present and recreates the Retrofit client.
     */
    fun setBaseUrl(newUrl: String) {
        val sanitized = if (newUrl.trim().endsWith("/")) newUrl.trim() else newUrl.trim() + "/"
        if (baseUrl != sanitized) {
            Log.i(TAG, "Updating base URL to: $sanitized")
            baseUrl = sanitized
            retrofit = null
            apiServiceInstance = null
        }
    }

    fun getService(): ApiService {
        return apiServiceInstance ?: synchronized(this) {
            val currentRetrofit = retrofit ?: Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build().also { retrofit = it }

            currentRetrofit.create(ApiService::class.java).also { apiServiceInstance = it }
        }
    }

    /**
     * Safe API call wrapper executing network request on background thread,
     * converting Retrofit Response to NetworkResult with rich error parsing and debug logging.
     */
    suspend fun <T> safeApiCall(apiCall: suspend () -> Response<T>): NetworkResult<T> {
        return try {
            val response = apiCall()
            val body = response.body()

            if (response.isSuccessful && body != null) {
                Log.d(TAG, "safeApiCall: HTTP ${response.code()} successful")
                NetworkResult.Success(body)
            } else {
                val errorCode = response.code()
                val errorBodyStr = response.errorBody()?.string()

                var conflictDto: ConflictErrorDto? = null
                if (errorCode == 409 && !errorBodyStr.isNullOrBlank()) {
                    try {
                        conflictDto = gson.fromJson(errorBodyStr, ConflictErrorDto::class.java)
                    } catch (_: Exception) {}
                }

                val message = conflictDto?.message
                    ?: conflictDto?.error
                    ?: parseErrorMessage(errorBodyStr)
                    ?: "HTTP Error $errorCode: ${response.message()}"

                Log.w(TAG, "safeApiCall: HTTP $errorCode error: $message")

                NetworkResult.Error(
                    message = message,
                    code = errorCode,
                    conflictSchedule = conflictDto?.conflictingSchedule
                )
            }
        } catch (e: java.net.ConnectException) {
            val errMsg = "Cannot connect to server at $baseUrl. Ensure Flask backend is running on port 5000."
            Log.e(TAG, "safeApiCall ConnectException: $errMsg", e)
            NetworkResult.Error(errMsg)
        } catch (e: java.net.SocketTimeoutException) {
            val errMsg = "Connection timed out. Server at $baseUrl took too long to respond."
            Log.e(TAG, "safeApiCall SocketTimeoutException: $errMsg", e)
            NetworkResult.Error(errMsg)
        } catch (e: java.net.UnknownHostException) {
            val errMsg = "Unable to resolve host: $baseUrl. Check network settings."
            Log.e(TAG, "safeApiCall UnknownHostException: $errMsg", e)
            NetworkResult.Error(errMsg)
        } catch (e: Exception) {
            val errMsg = e.localizedMessage ?: "Unexpected network error occurred."
            Log.e(TAG, "safeApiCall Exception: $errMsg", e)
            NetworkResult.Error(errMsg)
        }
    }

    private fun parseErrorMessage(errorJson: String?): String? {
        if (errorJson.isNullOrBlank()) return null
        return try {
            val map = gson.fromJson(errorJson, Map::class.java)
            map["error"]?.toString() ?: map["message"]?.toString()
        } catch (_: Exception) {
            null
        }
    }
}
