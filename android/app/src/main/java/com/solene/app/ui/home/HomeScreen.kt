package com.solene.app.ui.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solene.app.domain.model.FertilityLevel
import com.solene.app.ui.components.CycleProgressRing
import com.solene.app.ui.components.SoleneButtonXLarge
import com.solene.app.ui.components.SoleneInfoCard
import com.solene.app.ui.components.SoleneWavyLoader
import java.time.LocalDate

/**
 * Home screen — the main dashboard of Solene.
 *
 * Features:
 * - CircularWavyProgressIndicator showing cycle progress (M3 Expressive)
 * - Fertility status card
 * - Period prediction card
 * - XLarge "Log Today" hero button (five-size scale)
 */
@Composable
fun HomeScreen(
    onLogDay: (LocalDate) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            SoleneWavyLoader()
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // ── Date Header ──
        Text(
            text = uiState.todayFormatted,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(32.dp))

        // ── Hero: Wavy Circular Progress Ring ──
        if (uiState.cycleLength > 0 && uiState.cycleDayNumber > 0) {
            CycleProgressRing(
                progress = uiState.cycleProgress,
                cycleDayNumber = uiState.cycleDayNumber,
                totalDays = uiState.cycleLength,
                modifier = Modifier.size(220.dp),
            )
        } else {
            Text(
                text = "Complete onboarding to see your personalized cycle",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // ── Fertility Status Card ──
        uiState.fertilityStatus?.takeIf { it.fertilityTracked }?.let { fertility ->
            val fertilityIcon = when (fertility.status) {
                FertilityLevel.FERTILE -> Icons.Filled.FavoriteBorder
                else -> Icons.Filled.WaterDrop
            }
            val fertilityLabel = when (fertility.status) {
                FertilityLevel.FERTILE -> "Fertile"
                FertilityLevel.INFERTILE -> "Infertile"
                FertilityLevel.UNKNOWN -> "Unknown"
            }

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Icon(
                        imageVector = fertilityIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(32.dp),
                    )
                    Column {
                        Text(
                            text = "Phase ${fertility.phase} • $fertilityLabel",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = fertility.statusText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Prediction Card (on-device XGBoost) ──
        uiState.daysUntilNextPeriod?.let { days ->
            SoleneInfoCard(
                title = "Next Period",
                value = when {
                    days > 1 -> "in $days days"
                    days == 1 -> "Tomorrow"
                    days == 0 -> "Expected today"
                    days == -1 -> "1 day late"
                    else -> "${-days} days late"
                },
                subtitle = buildString {
                    uiState.fertilityStatus?.predictedNextPeriod?.let {
                        append("Predicted: ${it.monthValue}/${it.dayOfMonth}")
                    }
                    append(
                        if (uiState.fertilityStatus?.usedOnDeviceModel == true) {
                            " · on-device model"
                        } else {
                            " · from your cycle history"
                        }
                    )
                },
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Period dates are estimates. Fertility status applies NFP (Sensiplan) rules to " +
                "what you log; it is not medical advice, so double-check it yourself.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(32.dp))

        // ── Hero CTA: Log Today (XLarge button) ──
        SoleneButtonXLarge(
            onClick = { onLogDay(LocalDate.now()) },
        ) {
            Icon(
                imageVector = if (uiState.todayLogged) Icons.Filled.Edit else Icons.Filled.Add,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (uiState.todayLogged) "Edit today's log" else "Log Today",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
