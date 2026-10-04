package com.example.data.remote

import android.util.Log
import com.example.domain.model.*

class SolarNetworkDataSource(
    private val apiService: SolarApiService = RetrofitClient.apiService
) {
    private val tag = "SolarNetworkDataSource"

    suspend fun fetchMultiDayForecast(
        plantId: String,
        startDate: String,
        days: Int = 7,
        cloudDelta: Double? = null,
        tempDelta: Double? = null
    ): Result<List<DayForecastSummary>> {
        return try {
            val response = apiService.getMultiDayForecast(
                MultiDayForecastRequest(
                    plantId = plantId,
                    startDate = startDate,
                    days = days,
                    cloudCoverDelta = cloudDelta,
                    temperatureDelta = tempDelta
                )
            )
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val summaries = body.summaries.map { summaryDto ->
                    DayForecastSummary(
                        dateString = summaryDto.date,
                        dayName = summaryDto.dayName,
                        peakPowerKw = summaryDto.peakPowerKw,
                        totalEnergyKwh = summaryDto.totalEnergyKwh,
                        averageR2 = 0.9991,
                        maeKw = 1.579,
                        predominantWeather = WeatherCondition.values().find {
                            it.name.equals(summaryDto.predominantWeather, ignoreCase = true)
                        } ?: WeatherCondition.CLEAR_SUNNY,
                        hourlyPoints = summaryDto.hourlyForecasts.map { mapHourlyDto(it) }
                    )
                }
                Result.success(summaries)
            } else {
                Log.w(tag, "fetchMultiDayForecast failed with code: ${response.code()}")
                Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(tag, "Network error fetching multi-day forecast", e)
            Result.failure(e)
        }
    }

    suspend fun fetchDayAheadForecast(
        plantId: String,
        date: String,
        cloudDelta: Double? = null,
        tempDelta: Double? = null
    ): Result<DayAheadForecastResponse> {
        return try {
            val response = apiService.getDayAheadForecast(
                DayAheadForecastRequest(
                    plantId = plantId,
                    date = date,
                    cloudCoverDelta = cloudDelta,
                    temperatureDelta = tempDelta
                )
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchModelMetrics(): Result<ModelsMetricsResponse> {
        return try {
            val response = apiService.getModelMetrics()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchAlerts(plantId: String? = null): Result<List<SolarAlert>> {
        return try {
            val response = apiService.getAlerts(plantId = plantId)
            if (response.isSuccessful && response.body() != null) {
                val alerts = response.body()!!.alerts.map { dto ->
                    SolarAlert(
                        id = dto.id,
                        plantId = dto.plantId,
                        timestamp = dto.timestamp,
                        severity = AlertSeverity.values().find {
                            it.name.equals(dto.severity, ignoreCase = true)
                        } ?: AlertSeverity.INFO,
                        type = dto.type,
                        message = dto.message,
                        isAcknowledged = dto.isAcknowledged
                    )
                }
                Result.success(alerts)
            } else {
                Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun acknowledgeAlert(alertId: Long): Result<Boolean> {
        return try {
            val response = apiService.acknowledgeAlert(alertId)
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchPlants(): Result<List<PlantProfile>> {
        return try {
            val response = apiService.getPlants()
            if (response.isSuccessful && response.body() != null) {
                val plants = response.body()!!.plants.map { dto ->
                    PlantProfile(
                        id = dto.id,
                        name = dto.name,
                        capacityKw = dto.capacityKw,
                        panelType = dto.panelType,
                        location = dto.location,
                        tiltAngle = dto.tiltAngle,
                        azimuth = 180.0,
                        commissionDays = dto.commissionDays
                    )
                }
                Result.success(plants)
            } else {
                Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun mapHourlyDto(dto: HourlyForecastDto): ForecastPoint {
        return ForecastPoint(
            hour = dto.hour,
            timeLabel = dto.timeLabel,
            predictedKw = dto.predictedKw,
            lowerBoundKw = dto.lowerBoundKw,
            upperBoundKw = dto.upperBoundKw,
            actualKw = dto.actualKw,
            clearSkyKw = dto.clearSkyKw,
            ghi = dto.ghi,
            dni = dto.dni,
            dhi = dto.dhi,
            temperature = dto.temperature,
            cloudCover = dto.cloudCover,
            humidity = dto.humidity,
            windSpeed = dto.windSpeed,
            condition = WeatherCondition.values().find {
                it.name.equals(dto.weatherCondition, ignoreCase = true)
            } ?: WeatherCondition.CLEAR_SUNNY
        )
    }
}
