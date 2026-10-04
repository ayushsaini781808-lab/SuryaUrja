package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MultiDayForecastRequest(
    @Json(name = "plant_id") val plantId: String,
    @Json(name = "start_date") val startDate: String,
    @Json(name = "days") val days: Int = 7,
    @Json(name = "cloud_cover_delta") val cloudCoverDelta: Double? = null,
    @Json(name = "temperature_delta") val temperatureDelta: Double? = null
)

@JsonClass(generateAdapter = true)
data class DayAheadForecastRequest(
    @Json(name = "plant_id") val plantId: String,
    @Json(name = "date") val date: String,
    @Json(name = "cloud_cover_delta") val cloudCoverDelta: Double? = null,
    @Json(name = "temperature_delta") val temperatureDelta: Double? = null
)

@JsonClass(generateAdapter = true)
data class HourlyForecastDto(
    @Json(name = "hour") val hour: Int,
    @Json(name = "time_label") val timeLabel: String,
    @Json(name = "timestamp") val timestamp: Long = 0L,
    @Json(name = "predicted_kw") val predictedKw: Double,
    @Json(name = "lower_bound_kw") val lowerBoundKw: Double,
    @Json(name = "upper_bound_kw") val upperBoundKw: Double,
    @Json(name = "actual_kw") val actualKw: Double? = null,
    @Json(name = "clear_sky_kw") val clearSkyKw: Double = 0.0,
    @Json(name = "ghi") val ghi: Double = 0.0,
    @Json(name = "dni") val dni: Double = 0.0,
    @Json(name = "dhi") val dhi: Double = 0.0,
    @Json(name = "temperature") val temperature: Double = 25.0,
    @Json(name = "cloud_cover") val cloudCover: Double = 0.0,
    @Json(name = "humidity") val humidity: Double = 40.0,
    @Json(name = "wind_speed") val windSpeed: Double = 2.5,
    @Json(name = "weather_condition") val weatherCondition: String = "CLEAR"
)

@JsonClass(generateAdapter = true)
data class DaySummaryDto(
    @Json(name = "day_index") val dayIndex: Int,
    @Json(name = "date") val date: String,
    @Json(name = "day_name") val dayName: String,
    @Json(name = "peak_power_kw") val peakPowerKw: Double,
    @Json(name = "total_energy_kwh") val totalEnergyKwh: Double,
    @Json(name = "predominant_weather") val predominantWeather: String = "CLEAR",
    @Json(name = "hourly_forecasts") val hourlyForecasts: List<HourlyForecastDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class MultiDayForecastResponse(
    @Json(name = "plant_id") val plantId: String,
    @Json(name = "start_date") val startDate: String,
    @Json(name = "days") val days: Int,
    @Json(name = "model_name") val modelName: String = "CNN-LSTM + ENN",
    @Json(name = "summaries") val summaries: List<DaySummaryDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class DayAheadForecastResponse(
    @Json(name = "plant_id") val plantId: String,
    @Json(name = "date") val date: String,
    @Json(name = "model_used") val modelUsed: String = "CNN-LSTM + ENN",
    @Json(name = "r2_score") val r2Score: Double = 0.9991,
    @Json(name = "mae_kw") val maeKw: Double = 1.579,
    @Json(name = "rmse_kw") val rmseKw: Double = 1.850,
    @Json(name = "peak_power_kw") val peakPowerKw: Double = 178.5,
    @Json(name = "total_energy_kwh") val totalEnergyKwh: Double = 1184.0,
    @Json(name = "hourly_forecasts") val hourlyForecasts: List<HourlyForecastDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ModelBenchmarkDto(
    @Json(name = "name") val name: String,
    @Json(name = "type") val type: String,
    @Json(name = "r2") val r2: Double,
    @Json(name = "mae_kw") val maeKw: Double,
    @Json(name = "rmse_kw") val rmseKw: Double,
    @Json(name = "mape_percent") val mapePercent: Double,
    @Json(name = "latency_ms") val latencyMs: Int,
    @Json(name = "is_primary") val isPrimary: Boolean,
    @Json(name = "description") val description: String
)

@JsonClass(generateAdapter = true)
data class NeuralLayerDto(
    @Json(name = "layer_number") val layerNumber: Int,
    @Json(name = "type") val type: String,
    @Json(name = "configuration") val configuration: String,
    @Json(name = "output_shape") val outputShape: String,
    @Json(name = "purpose") val purpose: String
)

@JsonClass(generateAdapter = true)
data class FeatureImportanceDto(
    @Json(name = "name") val name: String,
    @Json(name = "score") val score: Double,
    @Json(name = "category") val category: String
)

@JsonClass(generateAdapter = true)
data class ModelsMetricsResponse(
    @Json(name = "models") val models: List<ModelBenchmarkDto> = emptyList(),
    @Json(name = "neural_layers") val neuralLayers: List<NeuralLayerDto> = emptyList(),
    @Json(name = "feature_importances") val featureImportances: List<FeatureImportanceDto> = emptyList(),
    @Json(name = "ensemble_weights") val ensembleWeights: Map<String, Double> = emptyMap()
)

@JsonClass(generateAdapter = true)
data class SolarAlertDto(
    @Json(name = "id") val id: Long,
    @Json(name = "plant_id") val plantId: String,
    @Json(name = "timestamp") val timestamp: Long,
    @Json(name = "severity") val severity: String,
    @Json(name = "type") val type: String,
    @Json(name = "message") val message: String,
    @Json(name = "is_acknowledged") val isAcknowledged: Boolean
)

@JsonClass(generateAdapter = true)
data class AlertsResponse(
    @Json(name = "alerts") val alerts: List<SolarAlertDto> = emptyList(),
    @Json(name = "total_count") val totalCount: Int = 0,
    @Json(name = "unacknowledged_count") val unacknowledgedCount: Int = 0
)

@JsonClass(generateAdapter = true)
data class AcknowledgeResponse(
    @Json(name = "status") val status: String,
    @Json(name = "alert_id") val alertId: Long,
    @Json(name = "acknowledged_at") val acknowledgedAt: Long
)

@JsonClass(generateAdapter = true)
data class PlantDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "capacity_kw") val capacityKw: Double,
    @Json(name = "tilt_angle") val tiltAngle: Double,
    @Json(name = "panel_type") val panelType: String,
    @Json(name = "location") val location: String,
    @Json(name = "commission_days") val commissionDays: Int
)

@JsonClass(generateAdapter = true)
data class PlantsResponse(
    @Json(name = "plants") val plants: List<PlantDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class WeatherFeedDto(
    @Json(name = "source") val source: String,
    @Json(name = "data_type") val dataType: String,
    @Json(name = "status") val status: String,
    @Json(name = "last_sync") val lastSync: String
)

@JsonClass(generateAdapter = true)
data class WeatherTelemetryResponse(
    @Json(name = "plant_id") val plantId: String,
    @Json(name = "current_ghi") val currentGhi: Double,
    @Json(name = "current_dni") val currentDni: Double,
    @Json(name = "current_dhi") val currentDhi: Double,
    @Json(name = "temperature") val temperature: Double,
    @Json(name = "cloud_cover") val cloudCover: Double,
    @Json(name = "humidity") val humidity: Double,
    @Json(name = "wind_speed") val windSpeed: Double,
    @Json(name = "feeds") val feeds: List<WeatherFeedDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ForecastExportResponse(
    @Json(name = "plant_id") val plantId: String,
    @Json(name = "format") val format: String,
    @Json(name = "record_count") val recordCount: Int,
    @Json(name = "content") val content: String
)
