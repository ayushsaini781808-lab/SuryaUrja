package com.example.data.remote

import retrofit2.Response
import retrofit2.http.*

interface SolarApiService {

    @POST("api/v1/forecast/multi-day")
    suspend fun getMultiDayForecast(
        @Body request: MultiDayForecastRequest
    ): Response<MultiDayForecastResponse>

    @GET("api/v1/forecast/multi-day")
    suspend fun getMultiDayForecastQuery(
        @Query("plant_id") plantId: String,
        @Query("start_date") startDate: String,
        @Query("days") days: Int = 7
    ): Response<MultiDayForecastResponse>

    @POST("api/v1/forecast/day-ahead")
    suspend fun getDayAheadForecast(
        @Body request: DayAheadForecastRequest
    ): Response<DayAheadForecastResponse>

    @GET("api/v1/forecast/day-ahead")
    suspend fun getDayAheadForecastQuery(
        @Query("plant_id") plantId: String,
        @Query("date") date: String
    ): Response<DayAheadForecastResponse>

    @GET("api/v1/models/metrics")
    suspend fun getModelMetrics(): Response<ModelsMetricsResponse>

    @GET("api/v1/alerts")
    suspend fun getAlerts(
        @Query("plant_id") plantId: String? = null,
        @Query("severity") severity: String? = null
    ): Response<AlertsResponse>

    @POST("api/v1/alerts/{alert_id}/acknowledge")
    suspend fun acknowledgeAlert(
        @Path("alert_id") alertId: Long
    ): Response<AcknowledgeResponse>

    @GET("api/v1/plants")
    suspend fun getPlants(): Response<PlantsResponse>

    @GET("api/v1/weather/telemetry")
    suspend fun getWeatherTelemetry(
        @Query("plant_id") plantId: String
    ): Response<WeatherTelemetryResponse>

    @GET("api/v1/export/forecast")
    suspend fun exportForecast(
        @Query("plant_id") plantId: String,
        @Query("format") format: String = "csv"
    ): Response<ForecastExportResponse>
}
