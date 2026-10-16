package com.solene.app.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * Calendar screen — monthly grid view of the cycle.
 *
 * Uses M3 surface tokens for day cells:
 * - Bleeding days: tertiary container color
 * - Predicted days: outlined with dashed tertiary border
 * - Today: primary container highlight
 */
@Composable
fun CalendarScreen(
    onDayClick: (LocalDate) -> Unit,
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        // ── Month Navigation Header ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { viewModel.navigateMonth(-1) }) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Previous month",
                )
            }

            Text(
                text = "${uiState.currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${uiState.currentMonth.year}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )

            IconButton(onClick = { viewModel.navigateMonth(1) }) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Next month",
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Day-of-Week Headers ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            val daysOfWeek = listOf(
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY,
                DayOfWeek.SUNDAY,
            )
            daysOfWeek.forEach { day ->
                Text(
                    text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Calendar Grid ──
        CalendarGrid(
            yearMonth = uiState.currentMonth,
            today = uiState.today,
            bleedingDays = uiState.bleedingDays,
            loggedDays = uiState.loggedDays,
            predictedDays = uiState.predictedBleedingDays,
            onDayClick = onDayClick,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Tap a day to log or edit it",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ── Legend ──
        CalendarLegend()
    }
}

@Composable
private fun CalendarGrid(
    yearMonth: YearMonth,
    today: LocalDate,
    bleedingDays: Set<LocalDate>,
    loggedDays: Set<LocalDate>,
    predictedDays: Set<LocalDate>,
    onDayClick: (LocalDate) -> Unit,
) {
    val firstDayOfMonth = yearMonth.atDay(1)
    // Monday = 1, so offset = dayOfWeek - 1
    val startOffset = (firstDayOfMonth.dayOfWeek.value - 1)
    val daysInMonth = yearMonth.lengthOfMonth()

    val totalCells = startOffset + daysInMonth
    val rows = (totalCells + 6) / 7

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        for (row in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    val dayNumber = cellIndex - startOffset + 1

                    if (dayNumber in 1..daysInMonth) {
                        val date = yearMonth.atDay(dayNumber)
                        val isToday = date == today
                        val isBleeding = date in bleedingDays
                        val isPredicted = date in predictedDays

                        CalendarDayCell(
                            day = dayNumber,
                            isToday = isToday,
                            isBleeding = isBleeding,
                            hasOtherData = date in loggedDays && !isBleeding,
                            isPredicted = isPredicted,
                            onClick = { onDayClick(date) }.takeUnless { date.isAfter(today) },
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    day: Int,
    isToday: Boolean,
    isBleeding: Boolean,
    hasOtherData: Boolean,
    isPredicted: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = when {
        isBleeding -> MaterialTheme.colorScheme.tertiaryContainer
        isToday -> MaterialTheme.colorScheme.primaryContainer
        else -> Color.Transparent
    }

    val textColor = when {
        isBleeding -> MaterialTheme.colorScheme.onTertiaryContainer
        isToday -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    val borderModifier = if (isPredicted && !isBleeding) {
        Modifier.border(
            width = 2.dp,
            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f),
            shape = CircleShape,
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .then(borderModifier)
            .background(backgroundColor, CircleShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "$day",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            color = if (onClick == null) textColor.copy(alpha = 0.5f) else textColor,
        )
        if (hasOtherData) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 6.dp)
                    .size(4.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CalendarLegend() {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LegendItem(
            color = MaterialTheme.colorScheme.tertiaryContainer,
            label = "Bleeding",
        )
        LegendItem(
            color = MaterialTheme.colorScheme.primaryContainer,
            label = "Today",
        )
        LegendItem(
            color = MaterialTheme.colorScheme.primary,
            label = "Logged",
            dotSize = 6,
        )
        LegendItem(
            color = Color.Transparent,
            label = "Predicted",
            hasBorder = true,
        )
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String,
    hasBorder: Boolean = false,
    dotSize: Int = 12,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val dotModifier = if (hasBorder) {
            Modifier
                .size(dotSize.dp)
                .border(2.dp, MaterialTheme.colorScheme.tertiary, CircleShape)
        } else {
            Modifier
                .size(dotSize.dp)
                .background(color, CircleShape)
        }
        Box(modifier = dotModifier)
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
