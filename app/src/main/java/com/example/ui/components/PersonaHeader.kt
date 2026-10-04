package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.GridGoldenratio
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.domain.model.UserPersona
import com.example.ui.theme.SolarAmber
import com.example.ui.theme.SolarCyan
import com.example.ui.theme.SolarEmerald

@Composable
fun PersonaSelectorBar(
    currentPersona: UserPersona,
    onPersonaSelected: (UserPersona) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        UserPersona.values().forEach { persona ->
            val isSelected = persona == currentPersona
            val icon = getPersonaIcon(persona)

            FilterChip(
                selected = isSelected,
                onClick = { onPersonaSelected(persona) },
                leadingIcon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = persona.title,
                        modifier = Modifier.size(16.dp),
                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                label = {
                    Text(
                        text = persona.title,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SolarAmber,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

@Composable
fun PersonaInsightBanner(
    persona: UserPersona,
    nextDayPeakKw: Double,
    capacityKw: Double,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        when (persona) {
                            UserPersona.PLANT_OPERATOR -> SolarAmber.copy(alpha = 0.15f)
                            UserPersona.GRID_MANAGER -> SolarCyan.copy(alpha = 0.15f)
                            UserPersona.ENERGY_TRADER -> SolarEmerald.copy(alpha = 0.15f)
                            UserPersona.RESEARCHER -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        },
                        RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getPersonaIcon(persona),
                    contentDescription = null,
                    tint = when (persona) {
                        UserPersona.PLANT_OPERATOR -> SolarAmber
                        UserPersona.GRID_MANAGER -> SolarCyan
                        UserPersona.ENERGY_TRADER -> SolarEmerald
                        UserPersona.RESEARCHER -> MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${persona.role} Advisory",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = when (persona) {
                        UserPersona.PLANT_OPERATOR ->
                            "Peak generation of ${nextDayPeakKw} kW (${(nextDayPeakKw/capacityKw*100).toInt()}% capacity) forecasted at 12:30. Inverter maintenance window: 19:30 - 05:00."
                        UserPersona.GRID_MANAGER ->
                            "Max solar ramp rate: +34 kW/h between 08:00 - 09:00. Fast-response spinning reserves sufficient; low risk of frequency drop."
                        UserPersona.ENERGY_TRADER ->
                            "Day-ahead spot market price peak correlates with solar dip at 17:00. High confidence ENN window supports aggressive forward bidding."
                        UserPersona.RESEARCHER ->
                            "Hybrid CNN-LSTM + ENN achieves R² = 0.9991 and MAE = 1.58 kW over 646 days test split; spatial kernel captures clear-sky transients."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

private fun getPersonaIcon(persona: UserPersona): ImageVector {
    return when (persona) {
        UserPersona.PLANT_OPERATOR -> Icons.Default.Engineering
        UserPersona.GRID_MANAGER -> Icons.Default.GridGoldenratio
        UserPersona.ENERGY_TRADER -> Icons.Default.TrendingUp
        UserPersona.RESEARCHER -> Icons.Default.Science
    }
}
