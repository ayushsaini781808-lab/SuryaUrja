package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.domain.model.ForecastPoint
import com.example.ui.theme.SolarAmber
import com.example.ui.theme.SolarCyan
import com.example.ui.theme.SolarEmerald
import com.example.ui.theme.SolarRose

@Composable
fun WeatherMetricsGrid(
    point: ForecastPoint,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricTile(
                title = "Ambient Temp",
                value = "${point.temperature}°C",
                subtitle = "Cell Derate -${String.format(java.util.Locale.US, "%.1f", (point.temperature - 25.0).coerceAtLeast(0.0) * 0.4)}%",
                icon = Icons.Default.Thermostat,
                accentColor = if (point.temperature > 35) SolarRose else SolarAmber,
                modifier = Modifier.weight(1f)
            )
            MetricTile(
                title = "Cloud Cover",
                value = "${point.cloudCover.toInt()}%",
                subtitle = point.condition.label,
                icon = Icons.Default.Cloud,
                accentColor = if (point.cloudCover > 60) Color(0xFF94A3B8) else SolarCyan,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricTile(
                title = "Relative Humidity",
                value = "${point.humidity.toInt()}%",
                subtitle = "Dew Point 18.2°C",
                icon = Icons.Default.WaterDrop,
                accentColor = SolarCyan,
                modifier = Modifier.weight(1f)
            )
            MetricTile(
                title = "Wind Speed",
                value = "${point.windSpeed} m/s",
                subtitle = "Convective Cooling Active",
                icon = Icons.Default.Air,
                accentColor = SolarEmerald,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun MetricTile(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
}
