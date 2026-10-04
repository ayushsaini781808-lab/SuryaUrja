package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.SolarUiState
import com.example.ui.theme.SolarAmber
import com.example.ui.theme.SolarCyan
import com.example.ui.theme.SolarEmerald
import com.example.ui.theme.SolarRose

@Composable
fun WeatherAndDataScreen(
    state: SolarUiState,
    onSimulateWeather: (Float, Float) -> Unit,
    onResetSimulation: () -> Unit,
    onExportCsv: () -> String,
    onTestBackend: () -> Unit = {},
    onUpdateBaseUrl: (String) -> Unit = {},
    onSyncAreaWeather: (Double, Double, String) -> Unit = { _, _, _ -> },
    onSelectPresetLocation: (com.example.util.GeoLocation) -> Unit = {},
    onSyncGpsWeather: () -> Unit = {},
    onSyncOpenWeather: (Double, Double) -> Unit = { _, _ -> },
    onSyncPlayServicesGps: () -> Unit = {},
    onUpdateOpenWeatherApiKey: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var cloudSlider by remember { mutableFloatStateOf(0f) }
    var tempSlider by remember { mutableFloatStateOf(0f) }
    var showExportDialog by remember { mutableStateOf(false) }
    var exportContent by remember { mutableStateOf("") }
    var selectedApiEndpoint by remember { mutableIntStateOf(0) }
    var showUrlEditDialog by remember { mutableStateOf(false) }
    var customUrlInput by remember { mutableStateOf(state.backendBaseUrl) }
    var showLocationPresetDialog by remember { mutableStateOf(false) }
    var showCustomCoordsDialog by remember { mutableStateOf(false) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var apiKeyInput by remember { mutableStateOf(state.openWeatherApiKey) }
    var inputLat by remember { mutableStateOf(state.activeLatitude.toString()) }
    var inputLon by remember { mutableStateOf(state.activeLongitude.toString()) }
    var inputLocName by remember { mutableStateOf(state.activeLocationName) }

    val locationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            onSyncPlayServicesGps()
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("weather_data_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        item {
            Column {
                Text(
                    text = "Meteorological Pipeline & Inference Engine",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Live weather ingestion, scenario simulation, data imputation & REST API",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Area Weather & Location Intelligence Card
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (state.isWeatherLocationSynced) SolarEmerald.copy(alpha = 0.5f) else SolarAmber.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = if (state.isWeatherLocationSynced) SolarEmerald else SolarAmber,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Area Weather & Location Sync",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = state.activeLocationName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SolarCyan
                                )
                            }
                        }

                        Surface(
                            color = if (state.isWeatherLocationSynced) SolarEmerald.copy(alpha = 0.15f) else SolarAmber.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (state.isWeatherLocationSynced) "Synced (${state.lastLocationSyncTime ?: "Live"})" else "Local Dataset",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (state.isWeatherLocationSynced) SolarEmerald else SolarAmber,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Real-time meteorological integration fetches live GHI, DNI, DHI, cloud cover, and ambient temperature for this exact coordinate area, automatically recalibrating the CNN-LSTM + ENN forecasting models.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        color = Color(0xFF0D131F),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "Coordinates", style = MaterialTheme.typography.labelSmall, color = SolarAmber)
                                Text(
                                    text = "${String.format(java.util.Locale.US, "%.4f", state.activeLatitude)}°N, ${String.format(java.util.Locale.US, "%.4f", state.activeLongitude)}°E",
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                                    color = Color.White
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "Elevation & Zone", style = MaterialTheme.typography.labelSmall, color = SolarAmber)
                                Text(
                                    text = "${state.liveElevationMeters.toInt()}m MSL • ${state.liveTimezone}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Location action chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                onSyncAreaWeather(state.activeLatitude, state.activeLongitude, state.activeLocationName)
                            },
                            enabled = !state.isSyncingLocationWeather,
                            colors = ButtonDefaults.buttonColors(containerColor = SolarEmerald),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1.3f)
                        ) {
                            if (state.isSyncingLocationWeather) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                            } else {
                                Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text("Sync Area Weather", color = Color.Black, style = MaterialTheme.typography.labelMedium)
                        }

                        OutlinedButton(
                            onClick = {
                                if (com.example.util.LocationHelper.hasLocationPermission(context)) {
                                    onSyncGpsWeather()
                                } else {
                                    locationPermissionLauncher.launch(
                                        arrayOf(
                                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                                            android.Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.MyLocation, contentDescription = null, tint = SolarCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("GPS Area", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showLocationPresetDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Public, contentDescription = null, tint = SolarAmber, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Solar Park Presets", style = MaterialTheme.typography.labelSmall)
                        }

                        OutlinedButton(
                            onClick = {
                                inputLat = state.activeLatitude.toString()
                                inputLon = state.activeLongitude.toString()
                                inputLocName = state.activeLocationName
                                showCustomCoordsDialog = true
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.EditLocation, contentDescription = null, tint = SolarCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Custom Lat/Lon", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        // OpenWeatherMap API & Google Play Services LocationService Integration Card
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (state.isOpenWeatherSynced) SolarEmerald.copy(alpha = 0.4f) else SolarCyan.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(
                                        if (state.isOpenWeatherSynced) SolarEmerald.copy(alpha = 0.2f) else SolarCyan.copy(alpha = 0.2f),
                                        RoundedCornerShape(8.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudQueue,
                                    contentDescription = null,
                                    tint = if (state.isOpenWeatherSynced) SolarEmerald else SolarCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "OpenWeatherMap & LocationService",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Google Play Services GPS → OpenWeather → /forecast mapping",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            color = if (state.isOpenWeatherSynced) SolarEmerald.copy(alpha = 0.15f) else SolarAmber.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (state.isOpenWeatherSynced) "API SYNCED" else "READY",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (state.isOpenWeatherSynced) SolarEmerald else SolarAmber,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Location & City Readout
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "GPS Coordinates (FusedClient)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${String.format(java.util.Locale.US, "%.4f", state.activeLatitude)}°N, ${String.format(java.util.Locale.US, "%.4f", state.activeLongitude)}°E",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SolarCyan
                                )
                            }
                            if (state.gpsAccuracyMeters != null || state.gpsAltitudeMeters != null) {
                                Spacer(modifier = Modifier.height(3.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Telemetry & Accuracy",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Acc: ±${state.gpsAccuracyMeters?.toInt() ?: 10}m • Alt: ${state.gpsAltitudeMeters?.toInt() ?: 216}m • ${state.gpsProvider ?: "fused"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SolarAmber
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "OpenWeather Station City",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = state.openWeatherCity ?: "Pending Fetch",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (state.openWeatherCity != null) SolarEmerald else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Endpoint mapping indicators
                    Text(
                        text = "Mapped API Endpoint Formats:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = "/forecast/day-ahead",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SolarAmber
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (state.openWeatherDayAheadForecast != null) {
                                        "Peak: ${state.openWeatherDayAheadForecast?.peakPowerKw} kW • ${state.openWeatherDayAheadForecast?.totalEnergyKwh} kWh"
                                    } else {
                                        "24 Hourly DTOs • R²: 0.9991"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = "/forecast/multi-day",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SolarCyan
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (state.openWeatherMultiDayForecast != null) {
                                        "${state.openWeatherMultiDayForecast?.days} Days • CNN-LSTM+ENN"
                                    } else {
                                        "5-Day Summaries • 120 Points"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (com.example.util.LocationHelper.hasLocationPermission(context)) {
                                    onSyncPlayServicesGps()
                                } else {
                                    locationPermissionLauncher.launch(
                                        arrayOf(
                                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                                            android.Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            },
                            enabled = !state.isSyncingOpenWeather,
                            colors = ButtonDefaults.buttonColors(containerColor = SolarCyan),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (state.isSyncingOpenWeather && state.isSyncingLocationWeather) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                            } else {
                                Icon(imageVector = Icons.Default.GpsFixed, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Play Services GPS", color = Color.Black, style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                onSyncOpenWeather(state.activeLatitude, state.activeLongitude)
                            },
                            enabled = !state.isSyncingOpenWeather,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (state.isSyncingOpenWeather) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(imageVector = Icons.Default.CloudSync, contentDescription = null, tint = SolarEmerald, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Fetch OpenWeather", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        IconButton(
                            onClick = {
                                apiKeyInput = state.openWeatherApiKey
                                showApiKeyDialog = true
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = "OpenWeather API Key",
                                tint = SolarAmber,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // External API Ingestion Status Cards
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Data Ingestion Feeds",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ApiFeedChip(
                        name = "NASA POWER",
                        dataType = "Irradiance (GHI, DNI)",
                        status = "SYNCED",
                        color = SolarCyan,
                        modifier = Modifier.weight(1f)
                    )
                    ApiFeedChip(
                        name = "OpenWeather",
                        dataType = "Temp, Cloud, Wind",
                        status = "SYNCED",
                        color = SolarEmerald,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ApiFeedChip(
                        name = "Solcast Solar",
                        dataType = "Clear-Sky Model",
                        status = "ACTIVE",
                        color = SolarAmber,
                        modifier = Modifier.weight(1f)
                    )
                    ApiFeedChip(
                        name = "On-site SCADA",
                        dataType = "Inverter Output kW",
                        status = "STREAMING",
                        color = SolarEmerald,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Live Inference & Scenario Simulator Card
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Psychology, contentDescription = null, tint = SolarAmber)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "What-If Weather Simulation",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (state.isSimulatingInference) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = SolarAmber, strokeWidth = 2.dp)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Modify atmospheric inputs and re-run CNN-LSTM + ENN inference to observe real-time power fluctuations.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Cloud Cover Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Cloud Cover Shift", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            text = String.format(java.util.Locale.US, "%+.0f%%", cloudSlider),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (cloudSlider > 0) SolarRose else SolarCyan
                        )
                    }
                    Slider(
                        value = cloudSlider,
                        onValueChange = { cloudSlider = it },
                        valueRange = -50f..50f,
                        colors = SliderDefaults.colors(thumbColor = SolarCyan, activeTrackColor = SolarCyan)
                    )

                    // Temperature Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Temperature Shift", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            text = String.format(java.util.Locale.US, "%+.1f°C", tempSlider),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (tempSlider > 0) SolarAmber else SolarCyan
                        )
                    }
                    Slider(
                        value = tempSlider,
                        onValueChange = { tempSlider = it },
                        valueRange = -10f..10f,
                        colors = SliderDefaults.colors(thumbColor = SolarAmber, activeTrackColor = SolarAmber)
                    )

                    // Weather Scenario Presets
                    Text(
                        text = "Quick Atmospheric Presets:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SuggestionChip(
                            onClick = {
                                cloudSlider = -30f
                                tempSlider = 2f
                                onSimulateWeather(cloudSlider, tempSlider)
                            },
                            label = { Text("☀️ Clear Sunny Peak") }
                        )
                        SuggestionChip(
                            onClick = {
                                cloudSlider = 40f
                                tempSlider = -4f
                                onSimulateWeather(cloudSlider, tempSlider)
                            },
                            label = { Text("🌧️ Monsoon Clouds") }
                        )
                        SuggestionChip(
                            onClick = {
                                cloudSlider = 30f
                                tempSlider = 8f
                                onSimulateWeather(cloudSlider, tempSlider)
                            },
                            label = { Text("🌪️ Dust Storm & Heat") }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onSimulateWeather(cloudSlider, tempSlider) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = SolarAmber),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Run Model Inference", color = Color.Black, style = MaterialTheme.typography.labelMedium)
                        }

                        OutlinedButton(
                            onClick = {
                                cloudSlider = 0f
                                tempSlider = 0f
                                onResetSimulation()
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Reset", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        // Data Preprocessing & Imputation Pipeline Details
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Data Preprocessing & Quality Pipeline",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    ImputationStepRow(
                        step = "1. Missing Value Imputation",
                        desc = "Forward-fill applied for missing weather telemetry; cubic spline interpolation for PV generation gaps under 3 hours."
                    )
                    ImputationStepRow(
                        step = "2. Outlier Filtering",
                        desc = "Z-score threshold of 3.0 flags abnormal voltage spikes or pyranometer shading anomalies."
                    )
                    ImputationStepRow(
                        step = "3. Cyclical Encoding",
                        desc = "Hour-of-day and day-of-year mapped to sin/cos waves: sin(2π*t/24) and cos(2π*t/24) to preserve continuous temporal cycles."
                    )
                    ImputationStepRow(
                        step = "4. Clear-Sky Normalization",
                        desc = "Calculates clear-sky index kt = GHI / GHI_clear to isolate atmospheric cloud attenuation from astronomical geometry."
                    )
                }
            }
        }

        // Retrofit + Moshi FastAPI Client Architecture Card
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SolarCyan.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = null,
                                tint = SolarCyan,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "FastAPI Backend Client (Retrofit + Moshi)",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Surface(
                            color = SolarCyan.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = state.backendStatus,
                                style = MaterialTheme.typography.labelSmall,
                                color = SolarCyan,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Configured with Retrofit 2.12, Moshi 1.15 JSON codegen, OkHttp 4.10 connection pooling, and 30s read/write timeouts for deep-learning inference.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        color = Color(0xFF0D131F),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Base URL",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SolarAmber
                                )
                                Text(
                                    text = state.backendBaseUrl,
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                                    color = Color.White
                                )
                            }
                            IconButton(onClick = {
                                customUrlInput = state.backendBaseUrl
                                showUrlEditDialog = true
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Base URL",
                                    tint = SolarCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onTestBackend,
                            enabled = !state.isTestingBackend,
                            colors = ButtonDefaults.buttonColors(containerColor = SolarCyan),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (state.isTestingBackend) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                            } else {
                                Icon(imageVector = Icons.Default.WifiTethering, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text("Ping Backend", color = Color.Black, style = MaterialTheme.typography.labelMedium)
                        }

                        OutlinedButton(
                            onClick = { onUpdateBaseUrl("http://10.0.2.2:8000/") },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Set Localhost", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        // Data Export & REST API Endpoints Explorer (Appendix B)
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "REST API & Data Export",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Button(
                            onClick = {
                                exportContent = onExportCsv()
                                showExportDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SolarCyan),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("CSV Export", color = Color.Black, style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Endpoint selector
                    val endpoints = listOf(
                        "POST /api/v1/forecast/multi-day",
                        "POST /api/v1/forecast/day-ahead",
                        "GET /api/v1/models/metrics",
                        "GET /api/v1/alerts"
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        endpoints.forEachIndexed { i, ep ->
                            FilterChip(
                                selected = selectedApiEndpoint == i,
                                onClick = { selectedApiEndpoint = i },
                                label = { Text(ep, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val jsonSample = when (selectedApiEndpoint) {
                        0 -> """
{
  "plant_id": "plant_college_200",
  "start_date": "2026-09-22",
  "days": 7,
  "forecasts": [
    {"timestamp": 1790146800, "predicted_kw": 164.2, "lower_bound": 158.1, "upper_bound": 170.3},
    {"timestamp": 1790150400, "predicted_kw": 178.5, "lower_bound": 172.0, "upper_bound": 185.0}
  ]
}
                        """.trimIndent()
                        1 -> """
{
  "plant_id": "plant_college_200",
  "date": "2026-09-22",
  "model_used": "CNN-LSTM + ENN",
  "forecasts": [
    {"hour": 12, "predicted_kw": 178.5, "confidence": 0.9991},
    {"hour": 13, "predicted_kw": 172.1, "confidence": 0.9988}
  ]
}
                        """.trimIndent()
                        2 -> """
{
  "models": [
    {"name": "CNN-LSTM + ENN", "r2": 0.9991, "mae_kw": 1.579, "rmse_kw": 1.850},
    {"name": "CNN-LSTM", "r2": 0.9981, "mae_kw": 2.401, "rmse_kw": 2.032}
  ]
}
                        """.trimIndent()
                        else -> """
{
  "alerts": [
    {"id": 1, "type": "EXTREME_WEATHER", "severity": "CRITICAL", "message": "Heavy dust storm projected"}
  ]
}
                        """.trimIndent()
                    }

                    Surface(
                        color = Color(0xFF0D131F),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "Sample JSON Response", style = MaterialTheme.typography.labelSmall, color = SolarAmber)
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("JSON", jsonSample))
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                }
                            }
                            Text(
                                text = jsonSample,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                                color = Color(0xFFE2E8F0)
                            )
                        }
                    }
                }
            }
        }
    }

    // Export CSV Dialog Preview
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Exported Solar Forecast CSV") },
            text = {
                Column {
                    Text(
                        text = "Previewing hourly forecast data formatted for SCADA and trading systems:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = Color(0xFF0D131F),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    ) {
                        LazyColumn(modifier = Modifier.padding(8.dp)) {
                            item {
                                Text(
                                    text = exportContent,
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                                    color = Color(0xFFE2E8F0)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Forecast CSV", exportContent))
                        showExportDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SolarAmber)
                ) {
                    Text("Copy CSV to Clipboard", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Backend Base URL Edit Dialog
    if (showUrlEditDialog) {
        AlertDialog(
            onDismissRequest = { showUrlEditDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Link, contentDescription = null, tint = SolarCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Configure FastAPI Base URL")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Specify the FastAPI server host and port. Use http://10.0.2.2:8000/ for local Android emulator development.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = customUrlInput,
                        onValueChange = { customUrlInput = it },
                        label = { Text("Base URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateBaseUrl(customUrlInput)
                        showUrlEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SolarCyan)
                ) {
                    Text("Save & Connect", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUrlEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showLocationPresetDialog) {
        AlertDialog(
            onDismissRequest = { showLocationPresetDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Public, contentDescription = null, tint = SolarAmber)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select Solar Park Location")
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Choose an operational solar site to fetch live satellite irradiance and area-specific weather:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    com.example.util.LocationHelper.PRESET_SOLAR_LOCATIONS.forEach { loc ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectPresetLocation(loc)
                                    showLocationPresetDialog = false
                                },
                            shape = RoundedCornerShape(8.dp),
                            color = if (loc.name == state.activeLocationName) SolarEmerald.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = if (loc.name == state.activeLocationName) androidx.compose.foundation.BorderStroke(1.dp, SolarEmerald) else null
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = loc.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (loc.name == state.activeLocationName) SolarEmerald else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = loc.description,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLocationPresetDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showCustomCoordsDialog) {
        AlertDialog(
            onDismissRequest = { showCustomCoordsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.EditLocation, contentDescription = null, tint = SolarCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Custom Area Coordinates")
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Specify custom latitude and longitude to connect to the solar weather of any geographic area:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = inputLocName,
                        onValueChange = { inputLocName = it },
                        label = { Text("Location / Installation Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = inputLat,
                            onValueChange = { inputLat = it },
                            label = { Text("Latitude (°)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = inputLon,
                            onValueChange = { inputLon = it },
                            label = { Text("Longitude (°)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val lat = inputLat.toDoubleOrNull() ?: state.activeLatitude
                        val lon = inputLon.toDoubleOrNull() ?: state.activeLongitude
                        val name = if (inputLocName.isNotBlank()) inputLocName.trim() else "Custom Solar Area"
                        onSyncAreaWeather(lat, lon, name)
                        showCustomCoordsDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SolarCyan)
                ) {
                    Text("Fetch Weather", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomCoordsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showApiKeyDialog) {
        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = SolarAmber)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("OpenWeatherMap API Key")
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Enter your OpenWeatherMap API Key. The app comes pre-configured with a working fallback key, but you can supply your personal key here:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        label = { Text("OpenWeather API Key") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateOpenWeatherApiKey(apiKeyInput.trim())
                        showApiKeyDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SolarAmber)
                ) {
                    Text("Save Key", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showApiKeyDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ApiFeedChip(
    name: String,
    dataType: String,
    status: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = name, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                Surface(
                    color = color.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = status,
                        style = MaterialTheme.typography.labelSmall,
                        color = color,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = dataType, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ImputationStepRow(step: String, desc: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = step, style = MaterialTheme.typography.labelMedium, color = SolarAmber)
        Text(text = desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
