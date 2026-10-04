package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.model.AlertSeverity
import com.example.domain.model.SolarAlert
import com.example.ui.SolarUiState
import com.example.ui.theme.SolarAmber
import com.example.ui.theme.SolarCyan
import com.example.ui.theme.SolarEmerald
import com.example.ui.theme.SolarRose
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AlertsScreen(
    state: SolarUiState,
    onAcknowledgeAlert: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSeverityFilter by remember { mutableStateOf<AlertSeverity?>(null) }
    var showThresholdSettings by remember { mutableStateOf(false) }

    // Configurable thresholds state
    var accuracyThreshold by remember { mutableFloatStateOf(0.985f) }
    var deviationPctThreshold by remember { mutableFloatStateOf(15f) }
    var extremeCloudThreshold by remember { mutableFloatStateOf(75f) }

    val filteredAlerts = remember(state.alerts, selectedSeverityFilter) {
        if (selectedSeverityFilter == null) state.alerts
        else state.alerts.filter { it.severity == selectedSeverityFilter }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("alerts_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Grid Alerts & Anomaly Monitor",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${state.alerts.count { !it.isAcknowledged }} active alerts requiring operational review",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedButton(
                    onClick = { showThresholdSettings = true },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SolarAmber)
                ) {
                    Icon(imageVector = Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Thresholds", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        // Filter chips
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedSeverityFilter == null,
                    onClick = { selectedSeverityFilter = null },
                    label = { Text("All (${state.alerts.size})", style = MaterialTheme.typography.labelSmall) }
                )
                FilterChip(
                    selected = selectedSeverityFilter == AlertSeverity.CRITICAL,
                    onClick = { selectedSeverityFilter = AlertSeverity.CRITICAL },
                    label = { Text("Critical", style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SolarRose.copy(alpha = 0.2f), selectedLabelColor = SolarRose)
                )
                FilterChip(
                    selected = selectedSeverityFilter == AlertSeverity.WARNING,
                    onClick = { selectedSeverityFilter = AlertSeverity.WARNING },
                    label = { Text("Warning", style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SolarAmber.copy(alpha = 0.2f), selectedLabelColor = SolarAmber)
                )
                FilterChip(
                    selected = selectedSeverityFilter == AlertSeverity.INFO,
                    onClick = { selectedSeverityFilter = AlertSeverity.INFO },
                    label = { Text("Info", style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SolarCyan.copy(alpha = 0.2f), selectedLabelColor = SolarCyan)
                )
            }
        }

        // Active Alert items
        if (filteredAlerts.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircleOutline, contentDescription = null, tint = SolarEmerald, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "No Active Alerts in this Category", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                        Text(text = "Solar generation output is tracking within normal CNN-LSTM confidence bounds.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(filteredAlerts) { alert ->
                AlertItemCard(
                    alert = alert,
                    onAcknowledge = { onAcknowledgeAlert(alert.id) }
                )
            }
        }
    }

    // Configurable Alert Threshold Settings Modal
    if (showThresholdSettings) {
        AlertDialog(
            onDismissRequest = { showThresholdSettings = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Tune, contentDescription = null, tint = SolarAmber)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Configurable Alert Rules")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "Customize triggers for grid managers and solar plant operators:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Accuracy Threshold Slider
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Model R² Degradation Trigger", style = MaterialTheme.typography.labelSmall)
                            Text(text = String.format(Locale.US, "R² < %.3f", accuracyThreshold), style = MaterialTheme.typography.labelSmall, color = SolarAmber)
                        }
                        Slider(
                            value = accuracyThreshold,
                            onValueChange = { accuracyThreshold = it },
                            valueRange = 0.950f..0.998f
                        )
                    }

                    // Actual vs Forecast Deviation Slider
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Max Actual-Forecast Deviation", style = MaterialTheme.typography.labelSmall)
                            Text(text = "Δ > ${deviationPctThreshold.toInt()}%", style = MaterialTheme.typography.labelSmall, color = SolarRose)
                        }
                        Slider(
                            value = deviationPctThreshold,
                            onValueChange = { deviationPctThreshold = it },
                            valueRange = 5f..30f
                        )
                    }

                    // Extreme Weather Trigger
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Extreme Cloud Cover Spike", style = MaterialTheme.typography.labelSmall)
                            Text(text = "> ${extremeCloudThreshold.toInt()}% cloud", style = MaterialTheme.typography.labelSmall, color = SolarCyan)
                        }
                        Slider(
                            value = extremeCloudThreshold,
                            onValueChange = { extremeCloudThreshold = it },
                            valueRange = 50f..95f
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showThresholdSettings = false },
                    colors = ButtonDefaults.buttonColors(containerColor = SolarAmber)
                ) {
                    Text("Save Thresholds", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showThresholdSettings = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun AlertItemCard(
    alert: SolarAlert,
    onAcknowledge: () -> Unit
) {
    val timeFormat = SimpleDateFormat("MMM d, HH:mm", Locale.US)
    val formattedTime = timeFormat.format(Date(alert.timestamp))

    val (badgeBg, badgeText, badgeIcon) = when (alert.severity) {
        AlertSeverity.CRITICAL -> Triple(SolarRose.copy(alpha = 0.15f), SolarRose, Icons.Default.Warning)
        AlertSeverity.WARNING -> Triple(SolarAmber.copy(alpha = 0.15f), SolarAmber, Icons.Default.ErrorOutline)
        AlertSeverity.INFO -> Triple(SolarCyan.copy(alpha = 0.15f), SolarCyan, Icons.Default.Info)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(14.dp),
        border = if (!alert.isAcknowledged && alert.severity == AlertSeverity.CRITICAL) androidx.compose.foundation.BorderStroke(1.dp, SolarRose) else null
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = badgeBg,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = badgeIcon, contentDescription = null, tint = badgeText, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${alert.severity.name} • ${alert.type}",
                            style = MaterialTheme.typography.labelSmall,
                            color = badgeText
                        )
                    }
                }

                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = alert.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (!alert.isAcknowledged) {
                    Button(
                        onClick = onAcknowledge,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Acknowledge", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = SolarEmerald, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Acknowledged", style = MaterialTheme.typography.labelSmall, color = SolarEmerald)
                    }
                }
            }
        }
    }
}
