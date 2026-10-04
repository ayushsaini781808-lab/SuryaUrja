package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OpenWeatherForecastResponse(
    @Json(name = "cod") val cod: String? = "200",
    @Json(name = "message") val message: Double? = 0.0,
    @Json(name = "cnt") val cnt: Int? = 0,
    @Json(name = "list") val list: List<OpenWeatherItem> = emptyList(),
    @Json(name = "city") val city: OpenWeatherCity? = null
)

@JsonClass(generateAdapter = true)
data class OpenWeatherItem(
    @Json(name = "dt") val dt: Long = 0L,
    @Json(name = "main") val main: OpenWeatherMain = OpenWeatherMain(),
    @Json(name = "weather") val weather: List<OpenWeatherDescription> = emptyList(),
    @Json(name = "clouds") val clouds: OpenWeatherClouds = OpenWeatherClouds(),
    @Json(name = "wind") val wind: OpenWeatherWind = OpenWeatherWind(),
    @Json(name = "visibility") val visibility: Int? = 10000,
    @Json(name = "pop") val pop: Double? = 0.0,
    @Json(name = "dt_txt") val dtTxt: String = ""
)

@JsonClass(generateAdapter = true)
data class OpenWeatherMain(
    @Json(name = "temp") val temp: Double = 25.0,
    @Json(name = "feels_like") val feelsLike: Double? = 25.0,
    @Json(name = "temp_min") val tempMin: Double? = 20.0,
    @Json(name = "temp_max") val tempMax: Double? = 30.0,
    @Json(name = "pressure") val pressure: Double? = 1013.25,
    @Json(name = "humidity") val humidity: Double = 50.0
)

@JsonClass(generateAdapter = true)
data class OpenWeatherDescription(
    @Json(name = "id") val id: Int = 800,
    @Json(name = "main") val main: String = "Clear",
    @Json(name = "description") val description: String = "clear sky",
    @Json(name = "icon") val icon: String = "01d"
)

@JsonClass(generateAdapter = true)
data class OpenWeatherClouds(
    @Json(name = "all") val all: Double = 0.0
)

@JsonClass(generateAdapter = true)
data class OpenWeatherWind(
    @Json(name = "speed") val speed: Double = 2.5,
    @Json(name = "deg") val deg: Double? = 180.0,
    @Json(name = "gust") val gust: Double? = 3.5
)

@JsonClass(generateAdapter = true)
data class OpenWeatherCity(
    @Json(name = "id") val id: Long? = 0L,
    @Json(name = "name") val name: String = "Local Station",
    @Json(name = "coord") val coord: OpenWeatherCoord? = null,
    @Json(name = "country") val country: String = "",
    @Json(name = "timezone") val timezone: Long = 0L,
    @Json(name = "sunrise") val sunrise: Long = 0L,
    @Json(name = "sunset") val sunset: Long = 0L
)

@JsonClass(generateAdapter = true)
data class OpenWeatherCoord(
    @Json(name = "lat") val lat: Double = 0.0,
    @Json(name = "lon") val lon: Double = 0.0
)

data class OpenWeatherSyncResult(
    val cityName: String,
    val dayAheadResponse: DayAheadForecastResponse,
    val multiDayResponse: MultiDayForecastResponse,
    val currentTemp: Double,
    val currentCloudCover: Double,
    val currentHumidity: Double,
    val currentWindSpeed: Double,
    val weatherDesc: String
)

