package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.ForecastPoint
import com.example.domain.model.PlantProfile
import com.example.domain.model.UserPersona
import com.example.ui.SolarUiState
import com.example.ui.components.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    state: SolarUiState,
    onPlantSelected: (PlantProfile) -> Unit,
    onPersonaSelected: (UserPersona) -> Unit,
    onHourSelected: (ForecastPoint?) -> Unit,
    onNavigateToMultiDay: () -> Unit,
    onNavigateToModels: () -> Unit,
    onSyncAreaWeather: () -> Unit = {},
    onSyncGpsWeather: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var showPlantDialog by remember { mutableStateOf(false) }
    val currentDay = state.daySummaries.getOrNull(state.selectedDayIndex) ?: state.daySummaries.firstOrNull()
    val activePoint = state.selectedHourPoint ?: currentDay?.hourlyPoints?.getOrNull(12)
    val plant = state.selectedPlant

    val locationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            onSyncGpsWeather()
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Plant Selector & Status Header
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showPlantDialog = true }
                    .testTag("plant_selector_card"),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(SolarAmber.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SolarPower,
                                contentDescription = "Plant",
                                tint = SolarAmber,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = plant?.name ?: "Select Solar Plant",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Switch",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "Capacity: ${plant?.capacityKw ?: 200.0} kW • Tilt ${plant?.tiltAngle ?: 28}° • ${plant?.commissionDays ?: 646}d Dataset",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        color = SolarEmerald.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(SolarEmerald, RoundedCornerShape(3.dp))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "ONLINE",
                                style = MaterialTheme.typography.labelSmall,
                                color = SolarEmerald
                            )
                        }
                    }
                }
            }
        }

        // Live Area Weather & Location Sync Banner
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (state.isWeatherLocationSynced) Color(0xFF0F1A24) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (state.isWeatherLocationSynced) SolarEmerald.copy(alpha = 0.4f) else SolarAmber.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(if (state.isWeatherLocationSynced) SolarEmerald.copy(alpha = 0.15f) else SolarAmber.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Area Weather",
                                tint = if (state.isWeatherLocationSynced) SolarEmerald else SolarAmber,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = state.activeLocationName,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = if (state.isWeatherLocationSynced) SolarEmerald.copy(alpha = 0.2f) else SolarAmber.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = if (state.isWeatherLocationSynced) "Live Area Weather" else "Local Station",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (state.isWeatherLocationSynced) SolarEmerald else SolarAmber,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = "${String.format(java.util.Locale.US, "%.2f", state.activeLatitude)}°N, ${String.format(java.util.Locale.US, "%.2f", state.activeLongitude)}°E • ${if (state.isWeatherLocationSynced) "Synced ${state.lastLocationSyncTime ?: "Live"}" else "Tap Sync for live area weather"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
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
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = "GPS Location",
                                tint = SolarCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Button(
                            onClick = onSyncAreaWeather,
                            enabled = !state.isSyncingLocationWeather,
                            colors = ButtonDefaults.buttonColors(containerColor = if (state.isWeatherLocationSynced) SolarEmerald else SolarAmber),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            if (state.isSyncingLocationWeather) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.Black, strokeWidth = 2.dp)
                            } else {
                                Icon(imageVector = Icons.Default.CloudSync, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Sync", color = Color.Black, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }

        // Persona Selection Row
        item {
            Column {
                Text(
                    text = "Operational View Mode",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                PersonaSelectorBar(
                    currentPersona = state.selectedPersona,
                    onPersonaSelected = onPersonaSelected
                )
                Spacer(modifier = Modifier.height(8.dp))
                PersonaInsightBanner(
                    persona = state.selectedPersona,
                    nextDayPeakKw = state.overview.nextDayPeakKw,
                    capacityKw = plant?.capacityKw ?: 200.0
                )
            }
        }

        // Key Metrics Summary Grid (4 Tiles)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DashboardMetricCard(
                        title = "Current Generation",
                        value = "${state.overview.currentGenerationKw} kW",
                        badge = "${state.overview.capacityUtilizationPercent}% Capacity",
                        badgeColor = SolarAmber,
                        icon = Icons.Default.Bolt,
                        modifier = Modifier.weight(1f)
                    )
                    DashboardMetricCard(
                        title = "Day-Ahead Peak",
                        value = "${state.overview.nextDayPeakKw} kW",
                        badge = "Peak at 12:30",
                        badgeColor = SolarCyan,
                        icon = Icons.Default.TrendingUp,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DashboardMetricCard(
                        title = "Day Expected Total",
                        value = "${state.overview.todayTotalKwh.toInt()} kWh",
                        badge = "Clean Solar Power",
                        badgeColor = SolarEmerald,
                        icon = Icons.Default.EnergySavingsLeaf,
                        modifier = Modifier.weight(1f)
                    )
                    DashboardMetricCard(
                        title = "CNN-LSTM + ENN Accuracy",
                        value = "R² ${state.overview.modelR2}",
                        badge = "MAE ${state.overview.dayAheadMaeKw} kW",
                        badgeColor = Color(0xFFA78BFA),
                        icon = Icons.Default.AutoGraph,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToModels
                    )
                }
            }
        }

        // Day-Ahead 24-Hour Solar Forecast Chart Card
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("main_hourly_chart_card"),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Hourly Power: Actual vs. Predicted",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "Interactive Recharts line chart • Drag across curves to inspect SCADA vs. CNN-LSTM telemetry",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = onNavigateToMultiDay,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "7-Day Forecast",
                                tint = SolarAmber
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val points = currentDay?.hourlyPoints ?: emptyList()
                    ForecastChart(
                        points = points,
                        capacityKw = plant?.capacityKw ?: 200.0,
                        showActual = true,
                        showPredicted = true,
                        showConfidenceBand = true,
                        showClearSky = true,
                        selectedHour = activePoint?.hour,
                        onHourSelected = onHourSelected
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        color = Color(0xFF0F172A),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.TouchApp,
                                contentDescription = null,
                                tint = SolarAmber,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Drag or scrub across the chart to view live tooltips. Tap legend items to toggle individual power curves.",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }

        // Solar Arc & Sun Path Tracking Widget
        item {
            SunPathWidget(
                currentHour = activePoint?.hour ?: 12,
                ghi = activePoint?.ghi ?: 780.0,
                dni = activePoint?.dni ?: 640.0,
                dhi = activePoint?.dhi ?: 140.0
            )
        }

        // Live Meteorological Telemetry for selected hour
        item {
            if (activePoint != null) {
                Column {
                    Text(
                        text = "Meteorological Conditions at Hour ${activePoint.timeLabel}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    WeatherMetricsGrid(point = activePoint)
                }
            }
        }
    }

    // Plant Selection Bottom Dialog
    if (showPlantDialog) {
        AlertDialog(
            onDismissRequest = { showPlantDialog = false },
            title = { Text("Select Solar Plant") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.plants.forEach { p ->
                        val isSelected = p.id == plant?.id
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onPlantSelected(p)
                                    showPlantDialog = false
                                },
                            color = if (isSelected) SolarAmber.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        onPlantSelected(p)
                                        showPlantDialog = false
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = p.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${p.capacityKw.toInt()} kW • ${p.panelType} • ${p.location}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPlantDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun DashboardMetricCard(
    title: String,
    value: String,
    badge: String,
    badgeColor: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable { onClick() } else Modifier
        ),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = badgeColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                color = badgeColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall,
                    color = badgeColor,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}
