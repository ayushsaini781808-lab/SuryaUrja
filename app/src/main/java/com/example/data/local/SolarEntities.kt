package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "plants")
data class PlantEntity(
    @PrimaryKey val id: String,
    val name: String,
    val capacityKw: Double,
    val panelType: String,
    val location: String,
    val tiltAngle: Double,
    val azimuth: Double,
    val commissionDays: Int
)

@Entity(tableName = "hourly_records")
data class HourlyRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val plantId: String,
    val timestamp: Long,
    val dateString: String,
    val hour: Int,
    val actualKw: Double,
    val predictedKw: Double,
    val lowerBoundKw: Double,
    val upperBoundKw: Double,
    val ghi: Double,        // Global Horizontal Irradiance (W/m²)
    val dni: Double,        // Direct Normal Irradiance (W/m²)
    val dhi: Double,        // Diffuse Horizontal Irradiance (W/m²)
    val temperature: Double, // °C
    val humidity: Double,    // %
    val windSpeed: Double,   // m/s
    val cloudCover: Double,  // %
    val weatherCondition: String,
    val modelUsed: String
)

@Entity(tableName = "alerts")
data class AlertEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val plantId: String,
    val type: String,       // ACCURACY_DROP, EXTREME_WEATHER, DEVIATION_HIGH, MAINTENANCE
    val severity: String,   // CRITICAL, WARNING, INFO
    val message: String,
    val timestamp: Long,
    val isAcknowledged: Boolean = false
)
