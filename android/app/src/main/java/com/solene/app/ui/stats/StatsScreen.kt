package com.solene.app.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solene.app.domain.model.CompletedCycle
import com.solene.app.domain.model.CycleStats
import com.solene.app.ui.components.SoleneWavyLoader
import com.solene.app.ui.components.StatRow
import java.time.format.DateTimeFormatter

/**
 * Stats screen — cycle length analytics with M3 Expressive components.
 *
 * Features:
 * - Average cycle length hero card with LinearWavyProgressIndicator
 * - Segmented time range selection (3 months / 6 months / All time)
 * - Data table: Min, Max, StdDev, period length, cycle count
 * - Per-cycle lengths, newest first
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StatsScreen(
    viewModel: StatsViewModel = hiltViewModel(),
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
    ) {
        // ── Time Range ButtonGroup (M3 Expressive) ──
        Text(
            text = "Time Range",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(8.dp))

        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth(),
        ) {
            TimeRange.entries.forEachIndexed { index, range ->
                SegmentedButton(
                    selected = uiState.selectedTimeRange == range,
                    onClick = { viewModel.selectTimeRange(range) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = TimeRange.entries.size,
                    ),
                ) {
                    Text(
                        text = range.label,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        val stats = uiState.stats
        if (stats == null) {
            EmptyStats(range = uiState.selectedTimeRange)
        } else {
            AverageCard(stats)
            Spacer(modifier = Modifier.height(24.dp))
            DetailsCard(stats)
            Spacer(modifier = Modifier.height(24.dp))
            CyclesCard(stats.cycles)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun EmptyStats(range: TimeRange) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = if (range == TimeRange.ALL_TIME) "No completed cycles yet" else "No cycles in this range",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (range == TimeRange.ALL_TIME) {
                    "A cycle is complete once the next period starts. Log bleeding when it does and your stats appear here."
                } else {
                    "No cycle started in the last ${range.months} months. Try a longer range."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AverageCard(stats: CycleStats) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Average Cycle Length",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "${stats.averageCycleLength}",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "days",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }

            if (stats.numberOfCycles >= 2) {
                Spacer(modifier = Modifier.height(16.dp))

                // Full at a standard deviation of 0 days, empty at 5 or more.
                val consistency = (1f - (stats.standardDeviation / 5f).toFloat()).coerceIn(0f, 1f)
                LinearWavyProgressIndicator(
                    progress = { consistency },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Cycle consistency: ${(consistency * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                )
            }
        }
    }
}

@Composable
private fun DetailsCard(stats: CycleStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
        ) {
            Text(
                text = "Cycle Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            StatRow(label = "Shortest cycle", value = "${stats.minimumCycleLength} days")
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            StatRow(label = "Longest cycle", value = "${stats.maximumCycleLength} days")
            if (stats.numberOfCycles >= 2) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                StatRow(label = "Standard deviation", value = "±${stats.standardDeviation} days")
            }
            stats.averagePeriodLength?.let {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                StatRow(label = "Average period", value = "$it days")
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            StatRow(label = "Cycles counted", value = "${stats.numberOfCycles}")
        }
    }
}

@Composable
private fun CyclesCard(cycles: List<CompletedCycle>) {
    val longest = cycles.maxOfOrNull { it.length }?.takeIf { it > 0 } ?: return
    val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Cycles",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            cycles.asReversed().forEach { cycle ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = if (cycle.enteredAtSetup) {
                                "~${cycle.start.format(formatter)} · entered at setup"
                            } else {
                                cycle.start.format(formatter)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "${cycle.length} days",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(cycle.length.toFloat() / longest)
                            .height(8.dp)
                            .background(
                                color = if (cycle.enteredAtSetup) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                                shape = MaterialTheme.shapes.small,
                            ),
                    )
                }
            }
        }
    }
}
