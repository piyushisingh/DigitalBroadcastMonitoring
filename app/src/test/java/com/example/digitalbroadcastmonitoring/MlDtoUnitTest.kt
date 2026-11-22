package com.example.digitalbroadcastmonitoring

import com.example.digitalbroadcastmonitoring.data.api.dto.*
import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test

class MlDtoUnitTest {

    private val gson = Gson()

    @Test
    fun stallingRiskRequestDto_serializesProperly() {
        val request = StallingRiskRequestDto(
            year = "2020",
            location = "Zagreb",
            bandwidth = "unlimited",
            adblock = "1",
            dlThroughputFirst1s = 2500000.0,
            dlThroughputFirst5s = 3200000.0,
            dlThroughputFirst10s = 3800000.0
        )

        val json = gson.toJson(request)
        assertTrue(json.contains("\"year\":\"2020\""))
        assertTrue(json.contains("\"location\":\"Zagreb\""))
        assertTrue(json.contains("\"bandwidth\":\"unlimited\""))
        assertTrue(json.contains("\"adblock\":\"1\""))
        assertTrue(json.contains("\"dl_throughput_first1s\":2500000.0"))
        assertTrue(json.contains("\"dl_throughput_first5s\":3200000.0"))
        assertTrue(json.contains("\"dl_throughput_first10s\":3800000.0"))
    }

    @Test
    fun stallingRiskResponseDto_deserializesProperly() {
        val json = """
            {
              "predicted_class": 1,
              "prediction_label": "stalling",
              "stalling_risk_score": 0.785,
              "model_name": "EarlySessionStallingClassifier",
              "model_version": "1.1.0",
              "estimator": "RandomForestClassifier",
              "latency_ms": 14.5,
              "decision_threshold": 0.50,
              "score_semantics": "uncalibrated_model_estimate",
              "timestamp": "2026-10-28T02:00:00Z",
              "provenance": {
                "dataset": "MUEXLab YouTube QoE Session Dataset",
                "attribution": "MUEXLab",
                "disclaimer": "Research dataset disclaimer"
              }
            }
        """.trimIndent()

        val dto = gson.fromJson(json, StallingRiskResponseDto::class.java)
        assertEquals(1, dto.predictedClass)
        assertEquals("stalling", dto.predictionLabel)
        assertEquals(0.785, dto.stallingRiskScore, 0.0001)
        assertEquals("EarlySessionStallingClassifier", dto.modelName)
        assertEquals("1.1.0", dto.modelVersion)
        assertEquals("RandomForestClassifier", dto.estimator)
        assertEquals(14.5, dto.latencyMs ?: 0.0, 0.0001)
        assertEquals(0.50, dto.decisionThreshold ?: 0.0, 0.0001)
        assertEquals("uncalibrated_model_estimate", dto.scoreSemantics)
        assertEquals("MUEXLab YouTube QoE Session Dataset", dto.provenance?.dataset)
    }

    @Test
    fun mlHealthResponseDto_deserializesProperly() {
        val json = """
            {
              "status": "READY",
              "is_ready": true,
              "model_name": "EarlySessionStallingClassifier",
              "model_version": "1.0.0",
              "selected_model": "RandomForestClassifier"
            }
        """.trimIndent()

        val dto = gson.fromJson(json, MlHealthResponseDto::class.java)
        assertEquals("READY", dto.status)
        assertTrue(dto.isReady)
        assertEquals("EarlySessionStallingClassifier", dto.modelName)
        assertEquals("1.0.0", dto.modelVersion)
        assertEquals("RandomForestClassifier", dto.selectedModel)
    }

    @Test
    fun researchPreset_oneMbps_matchesDatasetDistributionAndContract() {
        val preset = com.example.digitalbroadcastmonitoring.data.model.ResearchPresets.ONE_MBPS_STALLING_RISK
        
        // 1. Verify bandwidth field is exactly "1Mbps" (the required API value)
        assertEquals("1Mbps", preset.bandwidth)
        assertEquals("2020", preset.year)
        assertEquals("Wuerzburg", preset.location)
        assertEquals("0", preset.adblock)

        // 2. Verify throughput values match empirical MUEXLab 1Mbps distribution [700k, 960k Bytes/s]
        assertTrue("dl_throughput_first1s must be in 1Mbps range", preset.dlThroughputFirst1s in 700000.0..960000.0)
        assertTrue("dl_throughput_first5s must be in 1Mbps range", preset.dlThroughputFirst5s in 700000.0..960000.0)
        assertTrue("dl_throughput_first10s must be in 1Mbps range", preset.dlThroughputFirst10s in 700000.0..960000.0)

        // 3. Explicitly verify it is NOT set to unthrottled unlimited defaults (> 2.5 MB/s)
        assertNotEquals(2500000.0, preset.dlThroughputFirst1s, 0.001)
        assertNotEquals(3200000.0, preset.dlThroughputFirst5s, 0.001)
        assertNotEquals(3800000.0, preset.dlThroughputFirst10s, 0.001)

        // 4. Explicitly verify it is NOT set to outdated arbitrary placeholder values (< 150 KB/s)
        assertNotEquals(85000.0, preset.dlThroughputFirst1s, 0.001)
        assertNotEquals(110000.0, preset.dlThroughputFirst5s, 0.001)
        assertNotEquals(105000.0, preset.dlThroughputFirst10s, 0.001)

        // 5. Verify the actual JSON request payload sent to the Flask API
        val requestDto = preset.toRequestDto()
        val json = gson.toJson(requestDto)
        assertTrue(json.contains("\"bandwidth\":\"1Mbps\""))
        assertTrue(json.contains("\"year\":\"2020\""))
        assertTrue(json.contains("\"location\":\"Wuerzburg\""))
        assertTrue(json.contains("\"adblock\":\"0\""))
        assertTrue(json.contains("\"dl_throughput_first1s\":857744.0"))
        assertTrue(json.contains("\"dl_throughput_first5s\":936274.0"))
        assertTrue(json.contains("\"dl_throughput_first10s\":939021.0"))
    }

    @Test
    fun researchPreset_unlimited_matchesDatasetDistributionAndContract() {
        val preset = com.example.digitalbroadcastmonitoring.data.model.ResearchPresets.UNLIMITED_HIGH_THROUGHPUT

        // 1. Verify bandwidth field is "unlimited"
        assertEquals("unlimited", preset.bandwidth)
        assertEquals("2020", preset.year)
        assertEquals("Zagreb", preset.location)
        assertEquals("1", preset.adblock)

        // 2. Verify throughput values represent unconstrained high bandwidth (> 2.5 MB/s)
        assertTrue(preset.dlThroughputFirst1s >= 2500000.0)
        assertTrue(preset.dlThroughputFirst5s >= 3000000.0)
        assertTrue(preset.dlThroughputFirst10s >= 3500000.0)

        // 3. Verify serialized JSON payload sent to the Flask API
        val requestDto = preset.toRequestDto()
        val json = gson.toJson(requestDto)
        assertTrue(json.contains("\"bandwidth\":\"unlimited\""))
        assertTrue(json.contains("\"year\":\"2020\""))
        assertTrue(json.contains("\"location\":\"Zagreb\""))
        assertTrue(json.contains("\"adblock\":\"1\""))
        assertTrue(json.contains("\"dl_throughput_first1s\":2800000.0"))
        assertTrue(json.contains("\"dl_throughput_first5s\":3400000.0"))
        assertTrue(json.contains("\"dl_throughput_first10s\":4100000.0"))
    }
}
