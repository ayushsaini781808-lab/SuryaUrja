package com.example.domain.model

enum class UserPersona(
    val title: String,
    val role: String,
    val keyMetricFocus: String,
    val description: String
) {
    PLANT_OPERATOR(
        title = "Plant Operator",
        role = "Operations & Maintenance",
        keyMetricFocus = "Daily Peak & Curtailment Risk",
        description = "Optimizes panel dispatch, schedules inverter maintenance during low irradiance, and tracks battery state."
    ),
    GRID_MANAGER(
        title = "Grid Manager",
        role = "System Stability & Balancing",
        keyMetricFocus = "Ramp-Rate & Voltage Variance",
        description = "Monitors solar intermittency, balances reserve generators, and avoids frequency deviations."
    ),
    ENERGY_TRADER(
        title = "Energy Trader",
        role = "Market Bidding & Hedging",
        keyMetricFocus = "Day-Ahead Margin & Confidence Bounds",
        description = "Executes risk-adjusted day-ahead spot bids and short-term intra-day trades based on ENN predictions."
    ),
    RESEARCHER(
        title = "Researcher / Data Scientist",
        role = "Model Evaluation & Spatiotemporal AI",
        keyMetricFocus = "Loss Metrics (R², MAE, RMSE)",
        description = "Analyzes CNN spatial convolution and LSTM temporal cell activations to study atmospheric transfer."
    )
}

enum class WeatherCondition(val label: String, val iconEmoji: String, val clearnessIndex: Double) {
    CLEAR_SUNNY("Clear Sky", "☀️", 0.95),
    PARTLY_CLOUDY("Partly Cloudy", "⛅", 0.75),
    SCATTERED_CLOUDS("Scattered Clouds", "🌤️", 0.65),
    OVERCAST("Overcast", "☁️", 0.35),
    RAIN_HAZE("Atmospheric Rain/Dust", "🌧️", 0.20)
}

data class ForecastPoint(
    val hour: Int,
    val timeLabel: String,
    val actualKw: Double?,
    val predictedKw: Double,
    val lowerBoundKw: Double,
    val upperBoundKw: Double,
    val clearSkyKw: Double,
    val ghi: Double,        // W/m²
    val dni: Double,        // W/m²
    val dhi: Double,        // W/m²
    val temperature: Double, // °C
    val humidity: Double,    // %
    val windSpeed: Double,   // m/s
    val cloudCover: Double,  // %
    val condition: WeatherCondition,
    val modelUsed: String = "CNN-LSTM + ENN"
)

data class DayForecastSummary(
    val dateString: String,
    val dayName: String,
    val peakPowerKw: Double,
    val totalEnergyKwh: Double,
    val averageR2: Double,
    val maeKw: Double,
    val predominantWeather: WeatherCondition,
    val hourlyPoints: List<ForecastPoint>
)

data class ModelBenchmark(
    val name: String,
    val tag: String,
    val r2: Double,
    val maeKw: Double,
    val rmseKw: Double,
    val mapePercent: Double,
    val latencyMs: Int,
    val isPrimary: Boolean = false,
    val description: String
)

data class NeuralLayerInfo(
    val layerNumber: Int,
    val type: String,
    val configuration: String,
    val outputShape: String,
    val purpose: String
)

data class FeatureImportance(
    val name: String,
    val score: Double, // 0.0 .. 1.0
    val category: String
)

data class PlantProfile(
    val id: String,
    val name: String,
    val capacityKw: Double,
    val panelType: String,
    val location: String,
    val tiltAngle: Double,
    val azimuth: Double,
    val commissionDays: Int
)

data class SolarAlert(
    val id: Long,
    val plantId: String,
    val type: String,
    val severity: AlertSeverity,
    val message: String,
    val timestamp: Long,
    val isAcknowledged: Boolean
)

enum class AlertSeverity {
    CRITICAL,
    WARNING,
    INFO
}

data class DashboardOverview(
    val currentGenerationKw: Double,
    val capacityUtilizationPercent: Double,
    val todayTotalKwh: Double,
    val nextDayPeakKw: Double,
    val modelR2: Double,
    val dayAheadMaeKw: Double,
    val activeAlertsCount: Int
)
