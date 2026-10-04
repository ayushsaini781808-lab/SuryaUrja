package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.model.DayForecastSummary
import com.example.domain.model.ForecastPoint
import com.example.ui.SolarUiState
import com.example.ui.components.ForecastChart
import com.example.ui.theme.SolarAmber
import com.example.ui.theme.SolarCyan
import com.example.ui.theme.SolarEmerald
import com.example.ui.theme.SolarRose

@Composable
fun MultiDayForecastScreen(
    state: SolarUiState,
    onDaySelected: (Int) -> Unit,
    onHourSelected: (ForecastPoint?) -> Unit,
    onExportCsv: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedDay = state.daySummaries.getOrNull(state.selectedDayIndex) ?: state.daySummaries.firstOrNull()
    val plant = state.selectedPlant
    val capacity = plant?.capacityKw ?: 200.0

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("multiday_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "7-Day Multi-Day Solar Forecast",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Spatiotemporal CNN-LSTM hourly projections with ENN refinement",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = onExportCsv,
                    colors = ButtonDefaults.buttonColors(containerColor = SolarAmber),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("export_csv_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Export",
                        modifier = Modifier.size(16.dp),
                        tint = Color.Black
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export", style = MaterialTheme.typography.labelMedium, color = Color.Black)
                }
            }
        }

        // 7-Day Horizon Selector Horizontal Carousel
        item {
            val scrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                state.daySummaries.forEachIndexed { idx, day ->
                    val isSelected = idx == state.selectedDayIndex
                    val isHistorical = (idx == 0)

                    Surface(
                        modifier = Modifier
                            .width(130.dp)
                            .clickable { onDaySelected(idx) },
                        color = if (isSelected) SolarAmber.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(14.dp),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, SolarAmber) else null
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isHistorical) "Yesterday" else (if (idx == 1) "Today (Day-Ahead)" else "Day +${idx}"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) SolarAmber else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(text = day.predominantWeather.iconEmoji)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${day.peakPowerKw.toInt()} kW",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${day.totalEnergyKwh.toInt()} kWh total",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                color = if (isHistorical) SolarCyan.copy(alpha = 0.15f) else SolarEmerald.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = if (isHistorical) "Actual SCADA" else "ENN Forecast",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isHistorical) SolarCyan else SolarEmerald,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Selected Day Forecast Chart
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
                        Column {
                            Text(
                                text = "${selectedDay?.dayName ?: "Selected Day"} Hourly Profile",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Peak: ${selectedDay?.peakPowerKw} kW • Total: ${selectedDay?.totalEnergyKwh} kWh • ${selectedDay?.predominantWeather?.label}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val points = selectedDay?.hourlyPoints ?: emptyList()
                    ForecastChart(
                        points = points,
                        capacityKw = capacity,
                        showActual = (state.selectedDayIndex == 0),
                        showPredicted = true,
                        showConfidenceBand = true,
                        showClearSky = true,
                        selectedHour = state.selectedHourPoint?.hour,
                        onHourSelected = onHourSelected
                    )
                }
            }
        }

        // Detailed 24-Hour Table Header
        item {
            Text(
                text = "Hourly Breakdown & Confidence Intervals (kW)",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Hourly Table items
        val points = selectedDay?.hourlyPoints ?: emptyList()
        val daylightPoints = points.filter { it.hour in 5..19 } // Show active daylight hours first
        items(daylightPoints) { pt ->
            Surface(
                color = if (state.selectedHourPoint?.hour == pt.hour) SolarAmber.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onHourSelected(pt) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = pt.timeLabel,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = pt.condition.iconEmoji)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${pt.predictedKw} kW",
                            style = MaterialTheme.typography.titleSmall,
                            color = SolarAmber
                        )
                        Text(
                            text = "CI [${pt.lowerBoundKw} - ${pt.upperBoundKw}]",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (pt.actualKw != null) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Actual: ${pt.actualKw} kW",
                                style = MaterialTheme.typography.labelMedium,
                                color = SolarCyan
                            )
                            val delta = pt.actualKw - pt.predictedKw
                            Text(
                                text = String.format(java.util.Locale.US, "Δ: %+.1f kW", delta),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (kotlin.math.abs(delta) > 10) SolarRose else SolarEmerald
                            )
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "GHI ${pt.ghi.toInt()} W/m²",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${pt.temperature}°C • ${pt.cloudCover.toInt()}% cloud",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
