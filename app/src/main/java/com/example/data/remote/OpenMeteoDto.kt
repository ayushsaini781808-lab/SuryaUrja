package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OpenMeteoResponse(
    @Json(name = "latitude") val latitude: Double? = null,
    @Json(name = "longitude") val longitude: Double? = null,
    @Json(name = "timezone") val timezone: String? = null,
    @Json(name = "elevation") val elevation: Double? = null,
    @Json(name = "hourly") val hourly: OpenMeteoHourlyData? = null
)

@JsonClass(generateAdapter = true)
data class OpenMeteoHourlyData(
    @Json(name = "time") val time: List<String> = emptyList(),
    @Json(name = "temperature_2m") val temperature2m: List<Double?> = emptyList(),
    @Json(name = "relative_humidity_2m") val relativeHumidity2m: List<Double?> = emptyList(),
    @Json(name = "cloud_cover") val cloudCover: List<Double?> = emptyList(),
    @Json(name = "direct_normal_irradiance") val directNormalIrradiance: List<Double?> = emptyList(),
    @Json(name = "diffuse_radiation") val diffuseRadiation: List<Double?> = emptyList(),
    @Json(name = "shortwave_radiation_instant") val shortwaveRadiationInstant: List<Double?> = emptyList(),
    @Json(name = "wind_speed_10m") val windSpeed10m: List<Double?> = emptyList(),
    @Json(name = "weather_code") val weatherCode: List<Int?> = emptyList()
)

data class LocationWeatherResult(
    val locationName: String,
    val latitude: Double,
    val longitude: Double,
    val timezone: String,
    val elevationMeters: Double,
    val currentGhi: Double,
    val currentDni: Double,
    val currentDhi: Double,
    val currentTemp: Double,
    val currentCloudCover: Double,
    val currentHumidity: Double,
    val currentWindSpeed: Double,
    val conditionLabel: String,
    val hourlyPoints: List<HourlyWeatherSnapshot>
)

data class HourlyWeatherSnapshot(
    val isoTime: String,
    val hour: Int,
    val dateString: String,
    val ghi: Double,
    val dni: Double,
    val dhi: Double,
    val temperature: Double,
    val cloudCover: Double,
    val humidity: Double,
    val windSpeed: Double,
    val weatherCode: Int
)
