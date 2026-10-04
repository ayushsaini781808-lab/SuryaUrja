package com.example.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenWeatherApiService {

    @GET("data/2.5/forecast")
    suspend fun get5DayForecast(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"
    ): Response<OpenWeatherForecastResponse>
}
