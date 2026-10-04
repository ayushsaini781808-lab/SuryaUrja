package com.example.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenMeteoApiService {

    @GET("v1/forecast")
    suspend fun getSolarWeatherForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("hourly") hourly: String = "temperature_2m,relative_humidity_2m,cloud_cover,direct_normal_irradiance,diffuse_radiation,shortwave_radiation_instant,wind_speed_10m,weather_code",
        @Query("forecast_days") forecastDays: Int = 7,
        @Query("timezone") timezone: String = "auto"
    ): Response<OpenMeteoResponse>
}
