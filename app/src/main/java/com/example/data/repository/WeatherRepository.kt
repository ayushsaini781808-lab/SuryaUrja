package com.example.data.repository

import com.example.data.remote.*
import com.example.service.LocationCoordinates
import com.example.service.LocationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Result packaging the resolved user coordinates, meteorological conditions,
 * and the SolarCast backend mapped forecast endpoints format.
 */
data class LocalWeatherForecastResult(
    val coordinates: LocationCoordinates,
    val cityName: String,
    val currentTemp: Double,
    val currentCloudCover: Double,
    val currentHumidity: Double,
    val currentWindSpeed: Double,
    val weatherCondition: String,
    val dayAheadForecast: DayAheadForecastResponse,
    val multiDayForecast: MultiDayForecastResponse
)

/**
 * Repository interface defining access to local weather data using injected location services
 * and mapping directly to the SolarCast backend forecast models.
 */
interface WeatherRepository {
    /**
     * Obtains the user's current GPS coordinates using the injected [LocationService].
     */
    suspend fun fetchCurrentCoordinates(): LocationCoordinates?

    /**
     * Resolves the user's current coordinates via [LocationService], calls the OpenWeatherMap API,
     * and maps the response into the formats required by the `/forecast/day-ahead` and `/forecast/multi-day` endpoints.
     */
    suspend fun getLocalWeatherForecast(
        plantId: String = "plant_college_200",
        capacityKw: Double = 200.0,
        apiKey: String? = null
    ): Result<LocalWeatherForecastResult>

    /**
     * Fetches local weather for the current user location and maps it into [DayAheadForecastResponse]
     * formatted as required by the SolarCast `/forecast/day-ahead` endpoint.
     */
    suspend fun getDayAheadForecast(
        plantId: String = "plant_college_200",
        capacityKw: Double = 200.0,
        targetDate: String? = null,
        apiKey: String? = null
    ): Result<DayAheadForecastResponse>

    /**
     * Fetches local weather for the current user location and maps it into [MultiDayForecastResponse]
     * formatted as required by the SolarCast `/forecast/multi-day` endpoint.
     */
    suspend fun getMultiDayForecast(
        plantId: String = "plant_college_200",
        capacityKw: Double = 200.0,
        days: Int = 5,
        apiKey: String? = null
    ): Result<MultiDayForecastResponse>

    /**
     * Fetches weather for explicit coordinates and maps into the SolarCast backend forecast formats.
     */
    suspend fun getWeatherForCoordinates(
        latitude: Double,
        longitude: Double,
        plantId: String = "plant_college_200",
        capacityKw: Double = 200.0,
        days: Int = 5,
        apiKey: String? = null
    ): Result<LocalWeatherForecastResult>
}

/**
 * Default implementation of [WeatherRepository] which uses the injected [LocationService]
 * to query GPS coordinates from Google Play Services FusedLocationProviderClient,
 * executes network queries via [OpenWeatherApiService], and maps data into SolarCast backend models.
 */
class DefaultWeatherRepository(
    private val locationService: LocationService,
    private val openWeatherApiService: OpenWeatherApiService = RetrofitClient.openWeatherApiService,
    private val openWeatherMapper: OpenWeatherRepository = OpenWeatherRepository(openWeatherApiService),
    private val defaultApiKey: String = OpenWeatherRepository.DEFAULT_OPENWEATHER_API_KEY
) : WeatherRepository {

    override suspend fun fetchCurrentCoordinates(): LocationCoordinates? {
        return locationService.getCurrentLocation() ?: locationService.getLastKnownLocation()
    }

    override suspend fun getLocalWeatherForecast(
        plantId: String,
        capacityKw: Double,
        apiKey: String?
    ): Result<LocalWeatherForecastResult> = withContext(Dispatchers.IO) {
        val coords = fetchCurrentCoordinates()
            ?: LocationCoordinates(
                latitude = 28.6139,
                longitude = 77.2090,
                accuracyMeters = 15.0f,
                provider = "Default Fallback"
            )

        getWeatherForCoordinates(
            latitude = coords.latitude,
            longitude = coords.longitude,
            plantId = plantId,
            capacityKw = capacityKw,
            days = 5,
            apiKey = apiKey
        )
    }

    override suspend fun getDayAheadForecast(
        plantId: String,
        capacityKw: Double,
        targetDate: String?,
        apiKey: String?
    ): Result<DayAheadForecastResponse> = withContext(Dispatchers.IO) {
        val weatherResult = getLocalWeatherForecast(plantId, capacityKw, apiKey)
        if (weatherResult.isSuccess) {
            Result.success(weatherResult.getOrThrow().dayAheadForecast)
        } else {
            Result.failure(weatherResult.exceptionOrNull() ?: Exception("Failed to fetch day-ahead forecast"))
        }
    }

    override suspend fun getMultiDayForecast(
        plantId: String,
        capacityKw: Double,
        days: Int,
        apiKey: String?
    ): Result<MultiDayForecastResponse> = withContext(Dispatchers.IO) {
        val weatherResult = getLocalWeatherForecast(plantId, capacityKw, apiKey)
        if (weatherResult.isSuccess) {
            Result.success(weatherResult.getOrThrow().multiDayForecast)
        } else {
            Result.failure(weatherResult.exceptionOrNull() ?: Exception("Failed to fetch multi-day forecast"))
        }
    }

    override suspend fun getWeatherForCoordinates(
        latitude: Double,
        longitude: Double,
        plantId: String,
        capacityKw: Double,
        days: Int,
        apiKey: String?
    ): Result<LocalWeatherForecastResult> = withContext(Dispatchers.IO) {
        try {
            val key = if (!apiKey.isNullOrBlank()) apiKey else defaultApiKey
            val response = openWeatherApiService.get5DayForecast(
                latitude = latitude,
                longitude = longitude,
                apiKey = key,
                units = "metric"
            )

            if (!response.isSuccessful || response.body() == null) {
                val errorMsg = response.errorBody()?.string() ?: "HTTP ${response.code()}"
                return@withContext Result.failure(Exception("OpenWeatherMap API request failed: $errorMsg"))
            }

            val openWeatherData = response.body()!!
            val dayAheadForecast = openWeatherMapper.mapToDayAheadForecast(
                openWeatherData = openWeatherData,
                plantId = plantId,
                capacityKw = capacityKw
            )

            val multiDayForecast = openWeatherMapper.mapToMultiDayForecast(
                openWeatherData = openWeatherData,
                plantId = plantId,
                capacityKw = capacityKw,
                days = days
            )

            val firstItem = openWeatherData.list.firstOrNull()
            val firstWeather = firstItem?.weather?.firstOrNull()
            val cityName = openWeatherData.city?.name ?: "Local Solar Site"

            val result = LocalWeatherForecastResult(
                coordinates = LocationCoordinates(
                    latitude = latitude,
                    longitude = longitude,
                    accuracyMeters = 5.0f,
                    provider = "FusedLocation"
                ),
                cityName = cityName,
                currentTemp = firstItem?.main?.temp ?: 25.0,
                currentCloudCover = firstItem?.clouds?.all ?: 0.0,
                currentHumidity = firstItem?.main?.humidity ?: 50.0,
                currentWindSpeed = firstItem?.wind?.speed ?: 2.5,
                weatherCondition = firstWeather?.description ?: "clear sky",
                dayAheadForecast = dayAheadForecast,
                multiDayForecast = multiDayForecast
            )

            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
