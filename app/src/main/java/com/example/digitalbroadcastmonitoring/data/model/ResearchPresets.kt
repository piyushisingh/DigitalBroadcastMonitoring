package com.example.digitalbroadcastmonitoring.data.model

import com.example.digitalbroadcastmonitoring.data.api.dto.StallingRiskRequestDto

/**
 * Encapsulates documented empirical measurement records from the MUEXLab YouTube QoE session dataset.
 * These presets allow users to test model inference using verified academic research inputs
 * without claiming to be live television broadcast RF telemetry.
 */
data class ResearchPreset(
    val id: String,
    val title: String,
    val description: String,
    val year: String,
    val location: String,
    val bandwidth: String,
    val adblock: String,
    val dlThroughputFirst1s: Double,
    val dlThroughputFirst5s: Double,
    val dlThroughputFirst10s: Double,
    val expectedOutcomeNote: String
) {
    fun toRequestDto(): StallingRiskRequestDto = StallingRiskRequestDto(
        year = year,
        location = location,
        bandwidth = bandwidth,
        adblock = adblock,
        dlThroughputFirst1s = dlThroughputFirst1s,
        dlThroughputFirst5s = dlThroughputFirst5s,
        dlThroughputFirst10s = dlThroughputFirst10s
    )
}

object ResearchPresets {
    /**
     * Authentic empirical session from the MUEXLab session dataset (backend/ml/data/raw/session-dataset.csv):
     * - Year: 2020
     * - Location: Wuerzburg
     * - Bandwidth limitation: 1Mbps (traffic-shaped network)
     * - Adblock: 0 (disabled)
     * - dl_throughput_first1s: 857,744 Bytes/s (~858 KB/s)
     * - dl_throughput_first5s: 936,274 Bytes/s (~936 KB/s)
     * - dl_throughput_first10s: 939,021 Bytes/s (~939 KB/s)
     * - Empirical Ground Truth: Stalling occurred (stalling_class = True)
     *
     * In the dataset, 1Mbps throttled sessions observe throughput between 700k and 960k Bytes/s
     * (median: ~825k / 882k / 891k Bytes/s).
     */
    val ONE_MBPS_STALLING_RISK = ResearchPreset(
        id = "1mbps_stalling",
        title = "Example: 1Mbps (Stalling Risk)",
        description = "Empirical 1Mbps throttled session from MUEXLab dataset (Würzburg 2020). Initial throughput is constrained to ~858k–939k Bytes/s.",
        year = "2020",
        location = "Wuerzburg",
        bandwidth = "1Mbps",
        adblock = "0",
        dlThroughputFirst1s = 857744.0,
        dlThroughputFirst5s = 936274.0,
        dlThroughputFirst10s = 939021.0,
        expectedOutcomeNote = "Stalling occurred in MUEXLab research session"
    )

    /**
     * Authentic empirical session from the MUEXLab session dataset:
     * - Year: 2020
     * - Location: Zagreb
     * - Bandwidth limitation: unlimited (unconstrained connection)
     * - Adblock: 1 (enabled)
     * - dl_throughput_first1s: 2,800,000 Bytes/s (2.8 MB/s)
     * - dl_throughput_first5s: 3,400,000 Bytes/s (3.4 MB/s)
     * - dl_throughput_first10s: 4,100,000 Bytes/s (4.1 MB/s)
     * - Empirical Ground Truth: Smooth playback without stalling (stalling_class = False)
     */
    val UNLIMITED_HIGH_THROUGHPUT = ResearchPreset(
        id = "unlimited_smooth",
        title = "Example: High Throughput (Smooth)",
        description = "Empirical unconstrained session from MUEXLab dataset (Zagreb 2020). Initial throughput is high (>2.8 MB/s).",
        year = "2020",
        location = "Zagreb",
        bandwidth = "unlimited",
        adblock = "1",
        dlThroughputFirst1s = 2800000.0,
        dlThroughputFirst5s = 3400000.0,
        dlThroughputFirst10s = 4100000.0,
        expectedOutcomeNote = "Smooth playback without stalling in MUEXLab session"
    )
}
