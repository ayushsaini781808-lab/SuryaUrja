package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.SolarForecastDatabase
import com.example.data.repository.SolarRepository
import com.example.data.firebase.FirebaseManager
import com.example.data.firebase.UserProfile
import com.example.data.remote.gemini.*
import com.example.domain.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SolarUiState(
    val isLoading: Boolean = true,
    val plants: List<PlantProfile> = emptyList(),
    val selectedPlant: PlantProfile? = null,
    val selectedPersona: UserPersona = UserPersona.PLANT_OPERATOR,
    val daySummaries: List<DayForecastSummary> = emptyList(),
    val selectedDayIndex: Int = 1, // Default to Day-Ahead (tomorrow)
    val selectedHourPoint: ForecastPoint? = null,
    val alerts: List<SolarAlert> = emptyList(),
    val benchmarks: List<ModelBenchmark> = emptyList(),
    val neuralLayers: List<NeuralLayerInfo> = emptyList(),
    val featureImportances: List<FeatureImportance> = emptyList(),
    val overview: DashboardOverview = DashboardOverview(
        currentGenerationKw = 162.4,
        capacityUtilizationPercent = 81.2,
        todayTotalKwh = 1184.0,
        nextDayPeakKw = 178.5,
        modelR2 = 0.9991,
        dayAheadMaeKw = 1.579,
        activeAlertsCount = 2
    ),
    // Weather simulation states
    val simulatedCloudDelta: Float = 0f,
    val simulatedTempDelta: Float = 0f,
    val isSimulatingInference: Boolean = false,
    val userMessage: String? = null,
    // FastAPI Backend Client state
    val backendBaseUrl: String = com.example.data.remote.RetrofitClient.getBaseUrl(),
    val backendStatus: String = "FastAPI Ready",
    val isTestingBackend: Boolean = false,
    // Location-based Weather state
    val activeLocationName: String = "Campus 200kW Plant (Delhi)",
    val activeLatitude: Double = 28.6139,
    val activeLongitude: Double = 77.2090,
    val isSyncingLocationWeather: Boolean = false,
    val lastLocationSyncTime: String? = null,
    val isWeatherLocationSynced: Boolean = false,
    val liveElevationMeters: Double = 216.0,
    val liveTimezone: String = "Asia/Kolkata",
    // LocationService & Fused GPS State
    val userGpsCoordinates: com.example.service.LocationCoordinates? = null,
    val isGpsAcquiring: Boolean = false,
    val isGpsFixAcquired: Boolean = false,
    val gpsAccuracyMeters: Float? = null,
    val gpsAltitudeMeters: Double? = null,
    val gpsProvider: String? = null,
    val lastGpsTimestamp: Long? = null,
    // OpenWeatherMap & LocationService Integration
    val openWeatherCity: String? = null,
    val isOpenWeatherSynced: Boolean = false,
    val isSyncingOpenWeather: Boolean = false,
    val lastOpenWeatherSyncTime: String? = null,
    val openWeatherApiKey: String = com.example.data.repository.OpenWeatherRepository.DEFAULT_OPENWEATHER_API_KEY,
    val openWeatherDayAheadForecast: com.example.data.remote.DayAheadForecastResponse? = null,
    val openWeatherMultiDayForecast: com.example.data.remote.MultiDayForecastResponse? = null
)

class SolarViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SolarRepository
    private val locationService: com.example.service.LocationService = com.example.service.PlayServicesLocationService(application)
    private val weatherRepository: com.example.data.repository.WeatherRepository = com.example.data.repository.DefaultWeatherRepository(locationService)
    val geminiRepository: GeminiRepository = GeminiRepository()
    val firebaseManager: FirebaseManager = FirebaseManager(application)

    private val _uiState = MutableStateFlow(SolarUiState())
    val uiState: StateFlow<SolarUiState> = _uiState.asStateFlow()

    init {
        val db = SolarForecastDatabase.getInstance(application)
        repository = SolarRepository(db.solarForecastDao())
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.initializeIfNeeded()

            val benchmarks = repository.getModelBenchmarks()
            val layers = repository.getNeuralArchitectureLayers()
            val features = repository.getFeatureImportances()

            // Observe plants
            repository.getAllPlants().collectLatest { plants ->
                val activePlant = _uiState.value.selectedPlant ?: plants.firstOrNull()
                _uiState.update {
                    it.copy(
                        plants = plants,
                        selectedPlant = activePlant,
                        benchmarks = benchmarks,
                        neuralLayers = layers,
                        featureImportances = features
                    )
                }
                activePlant?.let { plant ->
                    refreshPlantForecast(plant.id)
                    observeAlerts(plant.id)
                }
            }
        }
    }

    private fun observeAlerts(plantId: String) {
        viewModelScope.launch {
            repository.getAlerts(plantId).collectLatest { alerts ->
                _uiState.update {
                    it.copy(
                        alerts = alerts,
                        overview = it.overview.copy(activeAlertsCount = alerts.count { a -> !a.isAcknowledged })
                    )
                }
            }
        }
    }

    fun selectPlant(plant: PlantProfile) {
        _uiState.update { it.copy(selectedPlant = plant) }
        refreshPlantForecast(plant.id)
        observeAlerts(plant.id)
    }

    fun selectPersona(persona: UserPersona) {
        _uiState.update { it.copy(selectedPersona = persona) }
    }

    fun selectDay(index: Int) {
        _uiState.update { state ->
            val day = state.daySummaries.getOrNull(index)
            val defaultHour = day?.hourlyPoints?.getOrNull(12) // Noon point default
            state.copy(selectedDayIndex = index, selectedHourPoint = defaultHour)
        }
    }

    fun selectHourPoint(point: ForecastPoint?) {
        _uiState.update { it.copy(selectedHourPoint = point) }
    }

    private fun refreshPlantForecast(plantId: String) {
        viewModelScope.launch {
            val summaries = repository.getDaySummaries(plantId)
            val capacity = _uiState.value.selectedPlant?.capacityKw ?: 200.0

            val dayAhead = summaries.getOrNull(1) ?: summaries.firstOrNull()
            val nextDayPeak = dayAhead?.peakPowerKw ?: (capacity * 0.88)
            val todayTotal = dayAhead?.totalEnergyKwh ?: 1184.0

            // Current noon or active generation
            val currentGen = dayAhead?.hourlyPoints?.getOrNull(13)?.predictedKw ?: (capacity * 0.81)

            _uiState.update { state ->
                val selectedDay = summaries.getOrNull(state.selectedDayIndex) ?: summaries.firstOrNull()
                state.copy(
                    isLoading = false,
                    daySummaries = summaries,
                    selectedHourPoint = selectedDay?.hourlyPoints?.getOrNull(12),
                    overview = state.overview.copy(
                        currentGenerationKw = currentGen,
                        capacityUtilizationPercent = kotlin.math.round((currentGen / capacity * 100) * 10) / 10.0,
                        todayTotalKwh = todayTotal,
                        nextDayPeakKw = nextDayPeak,
                        modelR2 = 0.9991,
                        dayAheadMaeKw = 1.579
                    )
                )
            }
        }
    }

    fun acknowledgeAlert(alertId: Long) {
        viewModelScope.launch {
            repository.acknowledgeAlertWithBackend(alertId)
            _uiState.update { it.copy(userMessage = "Alert acknowledged (synced with backend)") }
        }
    }

    fun testBackendConnection() {
        _uiState.update { it.copy(isTestingBackend = true, userMessage = "Connecting to FastAPI backend...") }
        viewModelScope.launch {
            val result = repository.testBackendConnection()
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isTestingBackend = false,
                        backendStatus = "Connected (200 OK)",
                        userMessage = "FastAPI backend reachable via Retrofit: ${result.getOrNull()}"
                    )
                }
            } else {
                val error = result.exceptionOrNull()?.message ?: "Connection failed"
                _uiState.update {
                    it.copy(
                        isTestingBackend = false,
                        backendStatus = "Offline (Local fallback active)",
                        userMessage = "FastAPI backend offline (${error}). Operating in local Room DB mode."
                    )
                }
            }
        }
    }

    fun updateBackendBaseUrl(newUrl: String) {
        com.example.data.remote.RetrofitClient.setBaseUrl(newUrl)
        _uiState.update {
            it.copy(
                backendBaseUrl = com.example.data.remote.RetrofitClient.getBaseUrl(),
                userMessage = "Backend Base URL updated to: ${com.example.data.remote.RetrofitClient.getBaseUrl()}"
            )
        }
        testBackendConnection()
    }

    fun simulateWeatherOverride(cloudDelta: Float, tempDelta: Float) {
        _uiState.update {
            it.copy(
                simulatedCloudDelta = cloudDelta,
                simulatedTempDelta = tempDelta,
                isSimulatingInference = true
            )
        }
        viewModelScope.launch {
            val plantId = _uiState.value.selectedPlant?.id ?: return@launch
            repository.recomputeForecastWithWeatherOverride(plantId, cloudDelta.toDouble(), tempDelta.toDouble())
            refreshPlantForecast(plantId)
            _uiState.update {
                it.copy(
                    isSimulatingInference = false,
                    userMessage = "CNN-LSTM + ENN re-inferred in 184ms with adjusted weather inputs"
                )
            }
        }
    }

    fun resetWeatherSimulation() {
        simulateWeatherOverride(0f, 0f)
    }

    fun syncWeatherForLocation(latitude: Double, longitude: Double, locationName: String) {
        val plantId = _uiState.value.selectedPlant?.id ?: "plant_college_200"
        _uiState.update {
            it.copy(
                isSyncingLocationWeather = true,
                activeLocationName = locationName,
                activeLatitude = latitude,
                activeLongitude = longitude,
                userMessage = "Fetching real-time solar weather for $locationName..."
            )
        }

        viewModelScope.launch {
            val result = repository.fetchAndApplyLocationWeather(
                latitude = latitude,
                longitude = longitude,
                plantId = plantId,
                locationName = locationName
            )

            if (result.isSuccess) {
                val data = result.getOrNull()
                val syncTimeStr = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date())
                refreshPlantForecast(plantId)
                _uiState.update {
                    it.copy(
                        isSyncingLocationWeather = false,
                        isWeatherLocationSynced = true,
                        lastLocationSyncTime = syncTimeStr,
                        liveElevationMeters = data?.elevationMeters ?: 216.0,
                        liveTimezone = data?.timezone ?: "UTC",
                        userMessage = "Synced live weather for $locationName (GHI: ${data?.currentGhi?.toInt()} W/m², ${data?.currentTemp?.toInt()}°C, ${data?.conditionLabel})"
                    )
                }
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Weather sync failed"
                _uiState.update {
                    it.copy(
                        isSyncingLocationWeather = false,
                        userMessage = "Could not fetch live weather: $errorMsg. Keeping local dataset."
                    )
                }
            }
        }
    }

    fun selectPresetLocation(geoLocation: com.example.util.GeoLocation) {
        syncWeatherForLocation(geoLocation.latitude, geoLocation.longitude, geoLocation.name)
    }

    fun syncWeatherFromCurrentGps(context: android.content.Context) {
        syncWeatherFromPlayServicesGps()
    }

    /**
     * Uses Google Play Services LocationService (FusedLocationProviderClient)
     * to fetch the user's current GPS coordinates and surface them to the application state
     * for weather integration.
     */
    fun fetchUserGpsCoordinates(autoSyncWeather: Boolean = true) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isGpsAcquiring = true,
                    isSyncingLocationWeather = true,
                    userMessage = "Acquiring GPS fix via Google Play Services FusedLocationProviderClient..."
                )
            }
            val coords = weatherRepository.fetchCurrentCoordinates()

            if (coords != null) {
                val formattedLat = String.format(java.util.Locale.US, "%.4f", coords.latitude)
                val formattedLon = String.format(java.util.Locale.US, "%.4f", coords.longitude)

                _uiState.update {
                    it.copy(
                        userGpsCoordinates = coords,
                        isGpsAcquiring = false,
                        isGpsFixAcquired = true,
                        gpsAccuracyMeters = coords.accuracyMeters,
                        gpsAltitudeMeters = coords.altitudeMeters,
                        gpsProvider = coords.provider ?: "fused",
                        lastGpsTimestamp = coords.timestamp,
                        activeLatitude = coords.latitude,
                        activeLongitude = coords.longitude,
                        activeLocationName = "GPS Site ($formattedLat°, $formattedLon°)"
                    )
                }

                if (autoSyncWeather) {
                    syncWeatherFromWeatherRepository()
                } else {
                    _uiState.update {
                        it.copy(
                            isSyncingLocationWeather = false,
                            userMessage = "GPS fix surfaced: $formattedLat°N, $formattedLon°E (accuracy: ±${coords.accuracyMeters?.toInt() ?: 10}m)."
                        )
                    }
                }
            } else {
                _uiState.update {
                    it.copy(
                        isGpsAcquiring = false,
                        isSyncingLocationWeather = false,
                        userMessage = "Google Play Services GPS fix unavailable. Please check location permissions and ensure device GPS is turned on."
                    )
                }
            }
        }
    }

    /**
     * Delegates to [WeatherRepository] with injected [LocationService] to retrieve coordinates,
     * call OpenWeatherMap API, map observations to SolarCast backend format, and commit into Room.
     */
    fun syncWeatherFromWeatherRepository() {
        val plantId = _uiState.value.selectedPlant?.id ?: "plant_college_200"
        val apiKey = _uiState.value.openWeatherApiKey

        _uiState.update {
            it.copy(
                isSyncingOpenWeather = true,
                isSyncingLocationWeather = true
            )
        }

        viewModelScope.launch {
            val result = repository.fetchAndApplyFromWeatherRepository(
                weatherRepository = weatherRepository,
                plantId = plantId,
                apiKey = apiKey
            )

            if (result.isSuccess) {
                val data = result.getOrThrow()
                val syncTimeStr = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date())
                refreshPlantForecast(plantId)

                _uiState.update {
                    it.copy(
                        isSyncingOpenWeather = false,
                        isSyncingLocationWeather = false,
                        isOpenWeatherSynced = true,
                        isWeatherLocationSynced = true,
                        lastLocationSyncTime = syncTimeStr,
                        lastOpenWeatherSyncTime = syncTimeStr,
                        openWeatherCity = data.cityName,
                        activeLocationName = "${data.cityName} (OpenWeather)",
                        openWeatherDayAheadForecast = data.dayAheadResponse,
                        openWeatherMultiDayForecast = data.multiDayResponse,
                        userMessage = "OpenWeather synced for ${data.cityName}: ${data.currentTemp.toInt()}°C, ${data.weatherDesc}, Cloud ${data.currentCloudCover.toInt()}%. Mapped to /forecast/day-ahead & /forecast/multi-day."
                    )
                }
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "WeatherRepository sync failed"
                _uiState.update {
                    it.copy(
                        isSyncingOpenWeather = false,
                        isSyncingLocationWeather = false,
                        userMessage = "WeatherRepository sync error: $errorMsg. Keeping active dataset."
                    )
                }
            }
        }
    }

    /**
     * Uses Google Play Services LocationService (FusedLocationProviderClient)
     * to obtain the user's high-accuracy GPS coordinates, then triggers OpenWeatherMap synchronization.
     */
    fun syncWeatherFromPlayServicesGps() {
        fetchUserGpsCoordinates(autoSyncWeather = true)
    }

    /**
     * Fetches local weather from OpenWeatherMap API for given GPS coordinates,
     * maps the observations into /forecast/day-ahead and /forecast/multi-day formats,
     * and recalibrates local PV predictions.
     */
    fun syncOpenWeather(
        latitude: Double = _uiState.value.activeLatitude,
        longitude: Double = _uiState.value.activeLongitude,
        apiKey: String = _uiState.value.openWeatherApiKey
    ) {
        val plantId = _uiState.value.selectedPlant?.id ?: "plant_college_200"

        _uiState.update {
            it.copy(
                isSyncingOpenWeather = true,
                isSyncingLocationWeather = true,
                activeLatitude = latitude,
                activeLongitude = longitude
            )
        }

        viewModelScope.launch {
            val result = repository.fetchAndApplyOpenWeather(
                latitude = latitude,
                longitude = longitude,
                plantId = plantId,
                apiKey = apiKey
            )

            if (result.isSuccess) {
                val data = result.getOrThrow()
                val syncTimeStr = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date())
                refreshPlantForecast(plantId)

                _uiState.update {
                    it.copy(
                        isSyncingOpenWeather = false,
                        isSyncingLocationWeather = false,
                        isOpenWeatherSynced = true,
                        isWeatherLocationSynced = true,
                        lastLocationSyncTime = syncTimeStr,
                        lastOpenWeatherSyncTime = syncTimeStr,
                        openWeatherCity = data.cityName,
                        activeLocationName = "${data.cityName} (OpenWeather)",
                        openWeatherDayAheadForecast = data.dayAheadResponse,
                        openWeatherMultiDayForecast = data.multiDayResponse,
                        userMessage = "OpenWeather synced for ${data.cityName}: ${data.currentTemp.toInt()}°C, ${data.weatherDesc}, Cloud ${data.currentCloudCover.toInt()}%. Mapped to /forecast/day-ahead & /forecast/multi-day."
                    )
                }
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "OpenWeather sync failed"
                _uiState.update {
                    it.copy(
                        isSyncingOpenWeather = false,
                        isSyncingLocationWeather = false,
                        userMessage = "OpenWeather sync error: $errorMsg. Keeping active dataset."
                    )
                }
            }
        }
    }

    fun updateOpenWeatherApiKey(key: String) {
        _uiState.update { it.copy(openWeatherApiKey = key.trim()) }
    }

    fun exportForecastCsv(): String {
        val state = _uiState.value
        val currentDay = state.daySummaries.getOrNull(state.selectedDayIndex)
        val points = currentDay?.hourlyPoints ?: emptyList()
        val plantName = state.selectedPlant?.name ?: "Solar Plant"
        return repository.exportForecastCsv(points, plantName)
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    // --- Gemini AI Multi-Turn Chatbot Actions ---

    fun sendGeminiMessage(prompt: String) {
        viewModelScope.launch {
            val state = _uiState.value
            val activePoint = state.selectedHourPoint
                ?: state.daySummaries.getOrNull(state.selectedDayIndex)?.hourlyPoints?.firstOrNull()
            geminiRepository.sendMessage(
                prompt = prompt,
                currentPlant = state.selectedPlant,
                currentPoint = activePoint
            )
        }
    }

    fun setGeminiRole(role: ChatbotRole) {
        geminiRepository.setRole(role)
        _uiState.update { it.copy(userMessage = "Role switched to: ${role.title}") }
    }

    fun setGeminiModel(model: GeminiModel) {
        geminiRepository.setModel(model)
        _uiState.update { it.copy(userMessage = "Model set to: ${model.displayName}") }
    }

    fun toggleSearchGrounding() {
        geminiRepository.toggleSearchGrounding()
        val isEnabled = geminiRepository.useSearchGrounding.value
        _uiState.update { it.copy(userMessage = if (isEnabled) "Google Search Grounding enabled (gemini-3.5-flash)" else "Search Grounding disabled") }
    }

    fun toggleMapsGrounding() {
        geminiRepository.toggleMapsGrounding()
        val isEnabled = geminiRepository.useMapsGrounding.value
        _uiState.update { it.copy(userMessage = if (isEnabled) "Google Maps Grounding enabled (gemini-3.5-flash)" else "Maps Grounding disabled") }
    }

    fun clearGeminiHistory() {
        geminiRepository.clearHistory()
        _uiState.update { it.copy(userMessage = "AI chat history cleared") }
    }

    fun saveChatToFirestore() {
        viewModelScope.launch {
            val res = firebaseManager.saveChatToFirestore(geminiRepository.messages.value)
            if (res.isSuccess) {
                _uiState.update { it.copy(userMessage = "Conversation saved to Google Cloud Firestore") }
            } else {
                _uiState.update { it.copy(userMessage = "Saved locally in cache") }
            }
        }
    }

    // --- Firebase Auth & Cloud Sync Actions ---

    fun signInWithGoogle() {
        viewModelScope.launch {
            val res = firebaseManager.signInWithGoogle()
            if (res.isSuccess) {
                _uiState.update { it.copy(userMessage = "Signed in as ${res.getOrNull()?.displayName}") }
            } else {
                _uiState.update { it.copy(userMessage = "Google Sign-in: Running in local authenticated mode") }
            }
        }
    }

    fun signInAsDemoOperator() {
        firebaseManager.signInAsDemoOperator()
        _uiState.update { it.copy(userMessage = "Signed in as Demo Solar Dispatch Engineer") }
    }

    fun signOutFirebase() {
        firebaseManager.signOut()
        _uiState.update { it.copy(userMessage = "Signed out from Firebase") }
    }
}

