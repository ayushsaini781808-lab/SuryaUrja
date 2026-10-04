package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.ForecastPoint
import com.example.ui.theme.*
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

/**
 * High-fidelity Jetpack Compose hourly line chart with Recharts-like interactive capabilities:
 * - Fluid drag-scrubbing with vertical cursor crosshair
 * - Floating glassmorphic tooltip displaying actual vs. predicted power, deviation Δ, and meteorological telemetry
 * - Cubic Bezier spline smoothing for realistic curves
 * - Dual area gradient fills (Predicted & Actual)
 * - 95% Confidence Interval ribbon
 * - Interactive legend toggles to show/hide series
 * - Quick-jump scrubber pills for solar ramp, peak noon, and sunset
 * - Zoom toggle between Full 24 Hours and Solar Window (06:00 - 19:00)
 */
@OptIn(ExperimentalTextApi::class)
@Composable
fun ForecastChart(
    points: List<ForecastPoint>,
    capacityKw: Double,
    modifier: Modifier = Modifier,
    showActual: Boolean = true,
    showPredicted: Boolean = true,
    showConfidenceBand: Boolean = true,
    showClearSky: Boolean = true,
    selectedHour: Int? = null,
    onHourSelected: (ForecastPoint?) -> Unit = {}
) {
    if (points.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(260.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No hourly solar forecast data available.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    // Interactive Series Toggles (Recharts Legend feature)
    var isActualVisible by remember { mutableStateOf(showActual) }
    var isPredictedVisible by remember { mutableStateOf(showPredicted) }
    var isConfidenceBandVisible by remember { mutableStateOf(showConfidenceBand) }
    var isClearSkyVisible by remember { mutableStateOf(showClearSky) }
    var isSolarWindowOnly by remember { mutableStateOf(false) }

    // Filter points if Solar Window is active (06:00 - 19:00)
    val displayedPoints = remember(points, isSolarWindowOnly) {
        if (isSolarWindowOnly) {
            points.filter { it.hour in 6..19 }.ifEmpty { points }
        } else {
            points
        }
    }

    val textMeasurer = rememberTextMeasurer()
    var internalSelectedIndex by remember { mutableStateOf<Int?>(selectedHour?.let { h -> displayedPoints.indexOfFirst { it.hour == h }.takeIf { it >= 0 } } ?: 12) }

    // Keep internal index synced if external selectedHour changes
    LaunchedEffect(selectedHour, displayedPoints) {
        if (selectedHour != null) {
            val idx = displayedPoints.indexOfFirst { it.hour == selectedHour }
            if (idx >= 0) internalSelectedIndex = idx
        }
    }

    val activeIndex = internalSelectedIndex?.coerceIn(0, displayedPoints.lastIndex)
    val activePoint = activeIndex?.let { displayedPoints.getOrNull(it) }

    val maxVal = remember(displayedPoints, capacityKw) {
        val peakPredicted = displayedPoints.maxOfOrNull { it.upperBoundKw } ?: capacityKw
        val peakActual = displayedPoints.mapNotNull { it.actualKw }.maxOfOrNull { it } ?: 0.0
        max(capacityKw * 1.05, max(peakPredicted, peakActual) * 1.12)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("hourly_line_chart_container")
    ) {
        // Recharts Controls & Horizon View Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(SolarAmber)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "RECHARTS HOURLY TELEMETRY",
                    style = MaterialTheme.typography.labelSmall,
                    color = SolarAmber,
                    letterSpacing = 1.sp
                )
            }

            // Window Mode Switcher (24h vs Solar Peak Window)
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.clickable { isSolarWindowOnly = !isSolarWindowOnly }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isSolarWindowOnly) Icons.Default.WbSunny else Icons.Default.Schedule,
                        contentDescription = "Zoom Horizon",
                        tint = if (isSolarWindowOnly) SolarAmber else SolarCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isSolarWindowOnly) "Solar Window (06-19h)" else "Full Day (24h)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Recharts-style Floating / Sticky Tooltip Card
        AnimatedVisibility(
            visible = activePoint != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            if (activePoint != null) {
                RechartsTooltipCard(
                    point = activePoint,
                    capacityKw = capacityKw
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Canvas Area with Continuous Touch & Drag Scrubbing
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF090D14))
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
                .pointerInput(displayedPoints) {
                    val updateIndexAtOffset = { touchX: Float ->
                        val leftPadding = 46.dp.toPx()
                        val rightPadding = 16.dp.toPx()
                        val chartW = size.width - leftPadding - rightPadding
                        val x = touchX - leftPadding
                        if (chartW > 0 && displayedPoints.isNotEmpty()) {
                            val step = chartW / (displayedPoints.size - 1).coerceAtLeast(1)
                            val index = ((x + step / 2f) / step).toInt().coerceIn(0, displayedPoints.lastIndex)
                            internalSelectedIndex = index
                            onHourSelected(displayedPoints.getOrNull(index))
                        }
                    }

                    detectTapGestures { offset ->
                        updateIndexAtOffset(offset.x)
                    }
                }
                .pointerInput(displayedPoints) {
                    val updateIndexAtOffset = { touchX: Float ->
                        val leftPadding = 46.dp.toPx()
                        val rightPadding = 16.dp.toPx()
                        val chartW = size.width - leftPadding - rightPadding
                        val x = touchX - leftPadding
                        if (chartW > 0 && displayedPoints.isNotEmpty()) {
                            val step = chartW / (displayedPoints.size - 1).coerceAtLeast(1)
                            val index = ((x + step / 2f) / step).toInt().coerceIn(0, displayedPoints.lastIndex)
                            internalSelectedIndex = index
                            onHourSelected(displayedPoints.getOrNull(index))
                        }
                    }

                    detectDragGestures { change, _ ->
                        change.consume()
                        updateIndexAtOffset(change.position.x)
                    }
                }
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("hourly_line_chart_canvas")
            ) {
                val leftPadding = 46.dp.toPx()
                val bottomPadding = 26.dp.toPx()
                val topPadding = 18.dp.toPx()
                val rightPadding = 16.dp.toPx()

                val chartW = size.width - leftPadding - rightPadding
                val chartH = size.height - bottomPadding - topPadding

                if (chartW <= 0 || chartH <= 0 || displayedPoints.size < 2) return@Canvas

                val n = displayedPoints.size
                val stepX = chartW / (n - 1)

                // 1. Cartesian Gridlines (Horizontal Power Marks)
                val gridSteps = 4
                for (i in 0..gridSteps) {
                    val ratio = i.toFloat() / gridSteps
                    val yVal = maxVal * ratio
                    val yPos = topPadding + chartH - ratio * chartH

                    drawLine(
                        color = Color(0x1AFFFFFF),
                        start = Offset(leftPadding, yPos),
                        end = Offset(size.width - rightPadding, yPos),
                        strokeWidth = 1.2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 6f))
                    )

                    val textLayout = textMeasurer.measure(
                        text = "${yVal.toInt()} kW",
                        style = TextStyle(fontSize = 9.sp, color = Color(0x8894A3B8))
                    )
                    drawText(
                        textLayoutResult = textLayout,
                        topLeft = Offset(leftPadding - textLayout.size.width - 6.dp.toPx(), yPos - textLayout.size.height / 2f)
                    )
                }

                // Rated Capacity Reference Line
                val capacityRatio = (capacityKw / maxVal).toFloat().coerceIn(0f, 1f)
                val capacityY = topPadding + chartH - capacityRatio * chartH
                drawLine(
                    color = SolarRose.copy(alpha = 0.5f),
                    start = Offset(leftPadding, capacityY),
                    end = Offset(size.width - rightPadding, capacityY),
                    strokeWidth = 1.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
                )
                val capText = textMeasurer.measure(
                    text = "Rated ${capacityKw.toInt()} kW",
                    style = TextStyle(fontSize = 8.sp, color = SolarRose.copy(alpha = 0.8f))
                )
                drawText(
                    textLayoutResult = capText,
                    topLeft = Offset(size.width - rightPadding - capText.size.width, capacityY - capText.size.height - 2.dp.toPx())
                )

                // Compute point coordinates
                val predictedCoords = mutableListOf<Offset>()
                val upperCoords = mutableListOf<Offset>()
                val lowerCoords = mutableListOf<Offset>()
                val actualCoords = mutableListOf<Pair<Int, Offset>>()
                val clearSkyCoords = mutableListOf<Offset>()

                displayedPoints.forEachIndexed { i, pt ->
                    val x = leftPadding + i * stepX

                    val yPred = topPadding + chartH - ((pt.predictedKw / maxVal).toFloat() * chartH).coerceIn(0f, chartH)
                    predictedCoords.add(Offset(x, yPred))

                    val yUpper = topPadding + chartH - ((pt.upperBoundKw / maxVal).toFloat() * chartH).coerceIn(0f, chartH)
                    upperCoords.add(Offset(x, yUpper))

                    val yLower = topPadding + chartH - ((pt.lowerBoundKw / maxVal).toFloat() * chartH).coerceIn(0f, chartH)
                    lowerCoords.add(Offset(x, yLower))

                    val yClear = topPadding + chartH - ((pt.clearSkyKw / maxVal).toFloat() * chartH).coerceIn(0f, chartH)
                    clearSkyCoords.add(Offset(x, yClear))

                    if (pt.actualKw != null && pt.actualKw >= 0.0) {
                        val yActual = topPadding + chartH - ((pt.actualKw / maxVal).toFloat() * chartH).coerceIn(0f, chartH)
                        actualCoords.add(Pair(i, Offset(x, yActual)))
                    }
                }

                // 2. 95% Confidence Interval Ribbon
                if (isConfidenceBandVisible && upperCoords.isNotEmpty()) {
                    val bandPath = Path()
                    smoothPathTo(bandPath, upperCoords)
                    for (i in lowerCoords.indices.reversed()) {
                        val pt = lowerCoords[i]
                        bandPath.lineTo(pt.x, pt.y)
                    }
                    bandPath.close()

                    drawPath(
                        path = bandPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                SolarAmber.copy(alpha = 0.22f),
                                SolarAmber.copy(alpha = 0.05f)
                            )
                        )
                    )
                }

                // 3. Clear-Sky Baseline (Dashed Golden Yellow)
                if (isClearSkyVisible && clearSkyCoords.isNotEmpty()) {
                    val clearSkyPath = Path()
                    smoothPathTo(clearSkyPath, clearSkyCoords)
                    drawPath(
                        path = clearSkyPath,
                        color = Color(0x66FBBF24),
                        style = Stroke(
                            width = 1.8f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f))
                        )
                    )
                }

                // 4. Predicted Power Curve (Recharts-style Area & Line)
                if (isPredictedVisible && predictedCoords.isNotEmpty()) {
                    val predPath = Path()
                    smoothPathTo(predPath, predictedCoords)

                    // Area gradient fill
                    val predFill = Path().apply {
                        addPath(predPath)
                        lineTo(predictedCoords.last().x, topPadding + chartH)
                        lineTo(predictedCoords.first().x, topPadding + chartH)
                        close()
                    }
                    drawPath(
                        path = predFill,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                SolarAmber.copy(alpha = 0.32f),
                                SolarAmber.copy(alpha = 0.02f)
                            ),
                            startY = topPadding,
                            endY = topPadding + chartH
                        )
                    )

                    drawPath(
                        path = predPath,
                        color = SolarAmber,
                        style = Stroke(width = 3.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }

                // 5. Actual Generation Curve (Recharts Cyan Solid Line with Markers)
                if (isActualVisible && actualCoords.isNotEmpty()) {
                    val actualPointsOnly = actualCoords.map { it.second }
                    val actualPath = Path()
                    smoothPathTo(actualPath, actualPointsOnly)

                    // Area gradient fill for actual
                    val actualFill = Path().apply {
                        addPath(actualPath)
                        lineTo(actualPointsOnly.last().x, topPadding + chartH)
                        lineTo(actualPointsOnly.first().x, topPadding + chartH)
                        close()
                    }
                    drawPath(
                        path = actualFill,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                SolarCyan.copy(alpha = 0.18f),
                                Color.Transparent
                            ),
                            startY = topPadding,
                            endY = topPadding + chartH
                        )
                    )

                    drawPath(
                        path = actualPath,
                        color = SolarCyan,
                        style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )

                    // Draw circular nodes
                    actualPointsOnly.forEach { pt ->
                        drawCircle(color = Color(0xFF090D14), radius = 4f, center = pt)
                        drawCircle(color = SolarCyan, radius = 3f, center = pt)
                    }
                }

                // 6. Active Scrubber Cursor & Hover Dots (Recharts Crosshair)
                activeIndex?.let { idx ->
                    if (idx in displayedPoints.indices) {
                        val cursorX = leftPadding + idx * stepX

                        // Vertical dashed crosshair
                        drawLine(
                            color = Color.White.copy(alpha = 0.65f),
                            start = Offset(cursorX, topPadding),
                            end = Offset(cursorX, topPadding + chartH),
                            strokeWidth = 1.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                        )

                        // Glowing target node on Predicted line
                        if (isPredictedVisible && idx < predictedCoords.size) {
                            val predPt = predictedCoords[idx]
                            drawCircle(color = SolarAmber.copy(alpha = 0.35f), radius = 9f, center = predPt)
                            drawCircle(color = SolarAmber, radius = 5f, center = predPt)
                            drawCircle(color = Color.White, radius = 2.5f, center = predPt)
                        }

                        // Glowing target node on Actual line
                        if (isActualVisible) {
                            val actualMatch = actualCoords.firstOrNull { it.first == idx }
                            if (actualMatch != null) {
                                val actPt = actualMatch.second
                                drawCircle(color = SolarCyan.copy(alpha = 0.35f), radius = 9f, center = actPt)
                                drawCircle(color = SolarCyan, radius = 5f, center = actPt)
                                drawCircle(color = Color.White, radius = 2.5f, center = actPt)
                            }
                        }
                    }
                }

                // 7. X-Axis Time Labels
                val labelInterval = if (displayedPoints.size > 14) 4 else 2
                displayedPoints.forEachIndexed { i, pt ->
                    if (i % labelInterval == 0 || i == displayedPoints.lastIndex) {
                        val x = leftPadding + i * stepX
                        val textLayout = textMeasurer.measure(
                            text = pt.timeLabel,
                            style = TextStyle(
                                fontSize = 9.sp,
                                color = if (i == activeIndex) Color.White else Color(0x9994A3B8),
                                fontWeight = if (i == activeIndex) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                            )
                        )
                        drawText(
                            textLayoutResult = textLayout,
                            topLeft = Offset(x - textLayout.size.width / 2f, topPadding + chartH + 6.dp.toPx())
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Recharts Interactive Legend (Tap to Toggle Curves)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            RechartsLegendChip(
                label = "Predicted",
                color = SolarAmber,
                isActive = isPredictedVisible,
                onClick = { isPredictedVisible = !isPredictedVisible }
            )
            RechartsLegendChip(
                label = "Actual SCADA",
                color = SolarCyan,
                isActive = isActualVisible,
                onClick = { isActualVisible = !isActualVisible }
            )
            RechartsLegendChip(
                label = "95% ENN Band",
                color = SolarAmber.copy(alpha = 0.7f),
                isActive = isConfidenceBandVisible,
                onClick = { isConfidenceBandVisible = !isConfidenceBandVisible }
            )
            RechartsLegendChip(
                label = "Clear-Sky",
                color = Color(0xFFFBBF24),
                isActive = isClearSkyVisible,
                onClick = { isClearSkyVisible = !isClearSkyVisible }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Quick-Jump Hour Scrubber Pills
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(
                Pair(8, "08:00 Ramp"),
                Pair(12, "12:00 Solar Noon"),
                Pair(15, "15:00 Afternoon"),
                Pair(18, "18:00 Sunset")
            ).forEach { (h, label) ->
                val isSelected = activePoint?.hour == h
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isSelected) SolarAmber.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) SolarAmber else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            val targetIndex = displayedPoints.indexOfFirst { it.hour == h }
                            if (targetIndex >= 0) {
                                internalSelectedIndex = targetIndex
                                onHourSelected(displayedPoints.getOrNull(targetIndex))
                            }
                        }
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) SolarAmber else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * Recharts-style rich tooltip card with actual vs predicted values, delta badges,
 * and live meteorological conditions.
 */
@Composable
private fun RechartsTooltipCard(
    point: ForecastPoint,
    capacityKw: Double,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF0F172A),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
        tonalElevation = 6.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("recharts_tooltip_card")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Row: Hour & Weather Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = SolarCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Hour ${point.timeLabel} • Day-Ahead Forecast",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White
                    )
                }
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "${point.condition.iconEmoji} ${point.condition.label}",
                        style = MaterialTheme.typography.labelSmall,
                        color = SolarAmber,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Metrics Comparison Grid: Predicted vs Actual
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Predicted Series Row
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(SolarAmber))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Predicted Power", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                    }
                    Text(
                        text = "${String.format(Locale.US, "%.1f", point.predictedKw)} kW",
                        style = MaterialTheme.typography.titleMedium,
                        color = SolarAmber
                    )
                    Text(
                        text = "95% CI: [${String.format(Locale.US, "%.1f", point.lowerBoundKw)} - ${String.format(Locale.US, "%.1f", point.upperBoundKw)}] kW",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF64748B),
                        fontSize = 10.sp
                    )
                }

                // Actual Series Row
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(SolarCyan))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Actual SCADA", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                    }
                    if (point.actualKw != null && point.actualKw >= 0.0) {
                        Text(
                            text = "${String.format(Locale.US, "%.1f", point.actualKw)} kW",
                            style = MaterialTheme.typography.titleMedium,
                            color = SolarCyan
                        )
                        val delta = point.actualKw - point.predictedKw
                        val deltaPct = if (point.predictedKw > 0) (delta / point.predictedKw * 100) else 0.0
                        val isNear = abs(deltaPct) <= 8.0
                        Text(
                            text = String.format(Locale.US, "Δ: %+.1f kW (%+.1f%%)", delta, deltaPct),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isNear) SolarEmerald else SolarRose,
                            fontSize = 10.sp
                        )
                    } else {
                        Text(
                            text = "Awaiting Real-Time",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(0xFF64748B)
                        )
                        Text(
                            text = "Pending SCADA feed",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF475569),
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Meteorological Telemetry Ribbon
            Surface(
                color = Color(0xFF1E293B).copy(alpha = 0.7f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "GHI: ${point.ghi.toInt()} W/m²",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFDE047),
                        fontSize = 10.sp
                    )
                    Text(
                        text = "Temp: ${point.temperature.toInt()}°C",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFCBD5E1),
                        fontSize = 10.sp
                    )
                    Text(
                        text = "Cloud: ${point.cloudCover.toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFCBD5E1),
                        fontSize = 10.sp
                    )
                    Text(
                        text = "Wind: ${String.format(Locale.US, "%.1f", point.windSpeed)} m/s",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFCBD5E1),
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

/**
 * Recharts-style interactive legend chip that toggles series visibility on tap.
 */
@Composable
private fun RechartsLegendChip(
    label: String,
    color: Color,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (isActive) color.copy(alpha = 0.15f) else Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isActive) color.copy(alpha = 0.5f) else Color(0xFF334155)
        ),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (isActive) color else Color(0xFF475569))
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (isActive) Color.White else Color(0xFF64748B),
                fontSize = 10.sp
            )
        }
    }
}

/**
 * Helper to construct a smooth cubic Bezier spline path through coordinates.
 */
private fun smoothPathTo(path: Path, points: List<Offset>) {
    if (points.isEmpty()) return
    path.moveTo(points[0].x, points[0].y)
    if (points.size == 1) return

    for (i in 0 until points.size - 1) {
        val p0 = if (i > 0) points[i - 1] else points[i]
        val p1 = points[i]
        val p2 = points[i + 1]
        val p3 = if (i + 2 < points.size) points[i + 2] else p2

        val cp1x = p1.x + (p2.x - p0.x) / 6f
        val cp1y = p1.y + (p2.y - p0.y) / 6f
        val cp2x = p2.x - (p3.x - p1.x) / 6f
        val cp2y = p2.y - (p3.y - p1.y) / 6f

        path.cubicTo(cp1x, cp1y, cp2x, cp2y, p2.x, p2.y)
    }
}
