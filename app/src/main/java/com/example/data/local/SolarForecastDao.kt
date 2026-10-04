package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SolarForecastDao {
    @Query("SELECT * FROM plants")
    fun getAllPlants(): Flow<List<PlantEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlants(plants: List<PlantEntity>)

    @Query("SELECT * FROM hourly_records WHERE plantId = :plantId ORDER BY timestamp ASC")
    fun getRecordsForPlant(plantId: String): Flow<List<HourlyRecordEntity>>

    @Query("SELECT * FROM hourly_records WHERE plantId = :plantId AND dateString = :dateString ORDER BY hour ASC")
    suspend fun getRecordsForDate(plantId: String, dateString: String): List<HourlyRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: List<HourlyRecordEntity>)

    @Query("SELECT * FROM alerts WHERE plantId = :plantId ORDER BY timestamp DESC")
    fun getAlerts(plantId: String): Flow<List<AlertEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: AlertEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlerts(alerts: List<AlertEntity>)

    @Query("UPDATE alerts SET isAcknowledged = 1 WHERE id = :alertId")
    suspend fun acknowledgeAlert(alertId: Long)

    @Query("DELETE FROM alerts WHERE id = :alertId")
    suspend fun deleteAlert(alertId: Long)

    @Query("DELETE FROM hourly_records WHERE plantId = :plantId")
    suspend fun deleteRecordsForPlant(plantId: String)

    @Query("UPDATE plants SET location = :location WHERE id = :plantId")
    suspend fun updatePlantLocation(plantId: String, location: String)

    @Query("SELECT COUNT(*) FROM hourly_records")
    suspend fun getRecordCount(): Int
}
