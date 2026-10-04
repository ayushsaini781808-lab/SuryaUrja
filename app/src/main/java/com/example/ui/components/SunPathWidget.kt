package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.ui.theme.SolarAmber
import com.example.ui.theme.SolarCyan
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SunPathWidget(
    currentHour: Int,
    ghi: Double,
    dni: Double,
    dhi: Double,
    modifier: Modifier = Modifier
) {
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
                Text(
                    text = "Solar Arc & Sun Position",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Sunrise 06:05 • Sunset 18:40",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Canvas solar dome trajectory
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val baseY = h - 14.dp.toPx()

                    // Horizon line
                    drawLine(
                        color = Color(0x33FFFFFF),
                        start = Offset(16.dp.toPx(), baseY),
                        end = Offset(w - 16.dp.toPx(), baseY),
                        strokeWidth = 1.5f
                    )

                    // Arc path (semi-ellipse)
                    val arcPath = Path()
                    val startX = 24.dp.toPx()
                    val endX = w - 24.dp.toPx()
                    val centerX = (startX + endX) / 2f
                    val radiusX = (endX - startX) / 2f
                    val radiusY = baseY - 12.dp.toPx()

                    val steps = 40
                    for (i in 0..steps) {
                        val angle = Math.PI * (1.0 - i.toDouble() / steps)
                        val x = centerX + radiusX * cos(angle).toFloat()
                        val y = baseY - radiusY * sin(angle).toFloat()
                        if (i == 0) arcPath.moveTo(x, y) else arcPath.lineTo(x, y)
                    }

                    drawPath(
                        path = arcPath,
                        color = SolarAmber.copy(alpha = 0.5f),
                        style = Stroke(
                            width = 2.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
                        )
                    )

                    // Current sun position along arc
                    val progress = ((currentHour - 6).coerceIn(0, 12) / 12.0)
                    val sunAngle = Math.PI * (1.0 - progress)
                    val sunX = centerX + radiusX * cos(sunAngle).toFloat()
                    val sunY = baseY - radiusY * sin(sunAngle).toFloat()

                    // Sun glow
                    drawCircle(
                        color = SolarAmber.copy(alpha = 0.25f),
                        radius = 16.dp.toPx(),
                        center = Offset(sunX, sunY)
                    )
                    drawCircle(
                        color = SolarAmber,
                        radius = 8.dp.toPx(),
                        center = Offset(sunX, sunY)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 3.dp.toPx(),
                        center = Offset(sunX, sunY)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Irradiance trio: GHI, DNI, DHI
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                IrradianceChip(label = "GHI (Total)", value = "${ghi.toInt()} W/m²", accent = SolarAmber)
                IrradianceChip(label = "DNI (Direct)", value = "${dni.toInt()} W/m²", accent = SolarCyan)
                IrradianceChip(label = "DHI (Diffuse)", value = "${dhi.toInt()} W/m²", accent = Color(0xFFA78BFA))
            }
        }
    }
}

@Composable
private fun IrradianceChip(label: String, value: String, accent: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(6.dp).background(accent, RoundedCornerShape(3.dp)))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
