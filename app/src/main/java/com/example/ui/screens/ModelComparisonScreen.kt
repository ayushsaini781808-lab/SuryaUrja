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
import com.example.domain.model.ModelBenchmark
import com.example.ui.SolarUiState
import com.example.ui.components.ModelArchitectureViewer
import com.example.ui.theme.SolarAmber
import com.example.ui.theme.SolarCyan
import com.example.ui.theme.SolarEmerald
import com.example.ui.theme.SolarRose

@Composable
fun ModelComparisonScreen(
    state: SolarUiState,
    modifier: Modifier = Modifier
) {
    var showModelCard by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("models_screen"),
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
                        text = "Model Evaluation & Benchmarking",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Published College 200 kW PV Dataset (646 Days Holdout)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedButton(
                    onClick = { showModelCard = true },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SolarAmber)
                ) {
                    Icon(imageVector = Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Model Card", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        // Target Achievement Banner
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SolarEmerald.copy(alpha = 0.12f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = SolarEmerald,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Business Goals Exceeded",
                            style = MaterialTheme.typography.titleSmall,
                            color = SolarEmerald
                        )
                        Text(
                            text = "Goal: R² ≥ 0.998 & MAE ≤ 2.5 kW → Achieved: R² 0.9991 & MAE 1.579 kW with CNN-LSTM + ENN.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Benchmark Cards List
        item {
            Text(
                text = "Model Benchmark Rankings",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        items(state.benchmarks) { bench ->
            ModelBenchmarkCard(benchmark = bench)
        }

        // Neural Architecture Visualizer
        item {
            ModelArchitectureViewer(
                layers = state.neuralLayers,
                featureImportances = state.featureImportances
            )
        }
    }

    // Model Card Dialog (Appendix C)
    if (showModelCard) {
        AlertDialog(
            onDismissRequest = { showModelCard = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = SolarAmber)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Model Card: SolarForecast-Hybrid-v1")
                }
            },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        ModelCardRow("Architecture", "CNN-LSTM + Ensemble Neural Network")
                        ModelCardRow("Input Dimension", "24 hours × 12 features (Irradiance, Weather, Lags)")
                        ModelCardRow("Output Horizon", "24-168h hourly power forecasts (kW)")
                        ModelCardRow("Training Data", "646 days, College 200 kW PV Plant")
                        ModelCardRow("Validation R²", "0.9991 (Superior to standalone LSTM: 0.985)")
                        ModelCardRow("Validation MAE", "1.579 kW (vs 2.401 kW baseline)")
                        ModelCardRow("Validation RMSE", "1.850 kW")
                        ModelCardRow("Inference Latency", "< 200ms per day-ahead forecast")
                        ModelCardRow("Intended Use", "Operational dispatch, grid stability balancing, spot market trading")
                        ModelCardRow("Known Limitations", "Performance degrades under severe dust storms; requires plant calibration")
                        ModelCardRow("Ethical/Privacy", "Zero PII; advisory forecasts for decision support")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showModelCard = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun ModelBenchmarkCard(benchmark: ModelBenchmark) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (benchmark.isPrimary) SolarAmber.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(14.dp),
        border = if (benchmark.isPrimary) androidx.compose.foundation.BorderStroke(1.5.dp, SolarAmber) else null
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = benchmark.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (benchmark.isPrimary) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = SolarAmber,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "RECOMMENDED",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = "${benchmark.latencyMs}ms",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricScorePill(label = "R² Score", value = String.format(java.util.Locale.US, "%.4f", benchmark.r2), highlight = benchmark.r2 >= 0.998)
                MetricScorePill(label = "MAE", value = "${benchmark.maeKw} kW", highlight = benchmark.maeKw <= 2.5)
                MetricScorePill(label = "RMSE", value = "${benchmark.rmseKw} kW", highlight = benchmark.rmseKw <= 2.5)
                MetricScorePill(label = "MAPE", value = "${benchmark.mapePercent}%", highlight = benchmark.mapePercent <= 3.5)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = benchmark.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MetricScorePill(label: String, value: String, highlight: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = if (highlight) SolarEmerald else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ModelCardRow(title: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = title, style = MaterialTheme.typography.labelSmall, color = SolarAmber)
        Text(text = value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
    }
}
