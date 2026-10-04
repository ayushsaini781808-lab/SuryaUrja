package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        PlantEntity::class,
        HourlyRecordEntity::class,
        AlertEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SolarForecastDatabase : RoomDatabase() {
    abstract fun solarForecastDao(): SolarForecastDao

    companion object {
        @Volatile
        private var INSTANCE: SolarForecastDatabase? = null

        fun getInstance(context: Context): SolarForecastDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SolarForecastDatabase::class.java,
                    "solar_forecast_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
