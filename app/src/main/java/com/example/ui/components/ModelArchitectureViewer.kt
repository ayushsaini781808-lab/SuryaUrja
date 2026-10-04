package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.domain.model.FeatureImportance
import com.example.domain.model.NeuralLayerInfo
import com.example.ui.theme.SolarAmber
import com.example.ui.theme.SolarCyan
import com.example.ui.theme.SolarEmerald

@Composable
fun ModelArchitectureViewer(
    layers: List<NeuralLayerInfo>,
    featureImportances: List<FeatureImportance>,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Neural Layers, 1 = ENN Aggregation, 2 = Feature Importance

    Surface(
        modifier = modifier.fillMaxWidth(),
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = "Neural Network",
                        tint = SolarAmber,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CNN-LSTM + ENN Architecture",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Surface(
                    color = SolarCyan.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "R² 0.9991",
                        style = MaterialTheme.typography.labelSmall,
                        color = SolarCyan,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tab toggles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    label = { Text("Network Layers", style = MaterialTheme.typography.labelSmall) }
                )
                FilterChip(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    label = { Text("ENN Ensemble", style = MaterialTheme.typography.labelSmall) }
                )
                FilterChip(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    label = { Text("Feature Weights", style = MaterialTheme.typography.labelSmall) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (selectedTab) {
                0 -> {
                    // Neural layers sequence
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        layers.forEach { layer ->
                            NeuralLayerCard(layer = layer)
                        }
                    }
                }
                1 -> {
                    // ENN Ensemble weighting explanation
                    EnsembleWeightsView()
                }
                2 -> {
                    // Feature importance rankings
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        featureImportances.forEach { feat ->
                            FeatureImportanceRow(feature = feat)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NeuralLayerCard(layer: NeuralLayerInfo) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                when {
                                    layer.type.startsWith("Conv") -> SolarCyan.copy(alpha = 0.2f)
                                    layer.type.startsWith("LSTM") -> SolarAmber.copy(alpha = 0.2f)
                                    layer.type.startsWith("Dense") || layer.type.startsWith("Output") -> SolarEmerald.copy(alpha = 0.2f)
                                    else -> Color(0x33FFFFFF)
                                },
                                RoundedCornerShape(6.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${layer.layerNumber}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = layer.type,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = layer.configuration,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = layer.outputShape,
                    style = MaterialTheme.typography.labelSmall,
                    color = SolarCyan
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Divider(color = Color(0x22FFFFFF))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Function: ${layer.purpose}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun EnsembleWeightsView() {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "The Ensemble Neural Network (ENN) aggregates outputs from 4 base deep learners to minimize prediction variance and suppress extreme weather forecast error:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        EnsembleWeightBar(name = "Hybrid CNN-LSTM", weight = 0.45, color = SolarAmber, r2 = "0.9981")
        EnsembleWeightBar(name = "Standalone 2-Layer LSTM", weight = 0.25, color = SolarCyan, r2 = "0.9850")
        EnsembleWeightBar(name = "Bidirectional GRU Network", weight = 0.20, color = SolarEmerald, r2 = "0.9830")
        EnsembleWeightBar(name = "Deep Feedforward Dense NN", weight = 0.10, color = Color(0xFFA78BFA), r2 = "0.9720")

        Surface(
            color = SolarAmber.copy(alpha = 0.1f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        ) {
            Text(
                text = "✓ Day-ahead error reduction: Ensemble weighting achieves MAE of 1.579 kW vs standalone CNN-LSTM 2.401 kW (34.2% variance reduction).",
                style = MaterialTheme.typography.labelSmall,
                color = SolarAmber,
                modifier = Modifier.padding(10.dp)
            )
        }
    }
}

@Composable
private fun EnsembleWeightBar(name: String, weight: Double, color: Color, r2: String) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = name, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(
                text = "${(weight * 100).toInt()}% Weight (R² $r2)",
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(Color(0x22FFFFFF), RoundedCornerShape(4.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(weight.toFloat())
                    .fillMaxHeight()
                    .background(color, RoundedCornerShape(4.dp))
            )
        }
    }
}

@Composable
private fun FeatureImportanceRow(feature: FeatureImportance) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = feature.name,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${(feature.score * 100).toInt()}% • ${feature.category}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(Color(0x22FFFFFF), RoundedCornerShape(3.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(feature.score.toFloat())
                    .fillMaxHeight()
                    .background(
                        when (feature.category) {
                            "Solar Irradiance" -> SolarAmber
                            "Weather" -> SolarCyan
                            "Historical Power" -> SolarEmerald
                            else -> Color(0xFFA78BFA)
                        },
                        RoundedCornerShape(3.dp)
                    )
            )
        }
    }
}
