package com.solene.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Reusable M3 Expressive component wrappers for Solene.
 *
 * Provides the app's five-size button scale and shared expressive
 * component patterns used across multiple screens.
 */

// ─── Five-Size Button Scale ──────────────────────────────────────────

/**
 * XSmall button — icon-only actions (edit, delete, favorite).
 */
@Composable
fun SoleneButtonXSmall(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.height(28.dp),
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
        content = content,
    )
}

/**
 * Small button — secondary actions in cards.
 */
@Composable
fun SoleneButtonSmall(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(34.dp),
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        content = content,
    )
}

/**
 * Medium button — standard actions ("Add Data", "Save").
 */
@Composable
fun SoleneButtonMedium(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.height(42.dp),
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        content = content,
    )
}

/**
 * Large button — primary CTA ("Log Today").
 */
@Composable
fun SoleneButtonLarge(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 12.dp),
        content = content,
    )
}

/**
 * XLarge button — hero action on the home screen.
 */
@Composable
fun SoleneButtonXLarge(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(62.dp)
            .fillMaxWidth(),
        enabled = enabled,
        shape = MaterialTheme.shapes.extraLarge,
        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp),
        content = content,
    )
}

// ─── Wavy Progress Hero ──────────────────────────────────────────────

/**
 * Large hero circular wavy progress indicator with cycle day info overlay.
 * Uses M3 Expressive CircularWavyProgressIndicator.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CycleProgressRing(
    progress: Float,
    cycleDayNumber: Int,
    totalDays: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(200.dp),
    ) {
        // Wavy progress ring — M3 Expressive
        CircularWavyProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            stroke = Stroke(width = 12.dp.value),
        )

        // Center content — day number
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Day",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "$cycleDayNumber",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "of $totalDays",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ─── Indeterminate Wavy Loader ──────────────────────────────────────

/**
 * Indeterminate wavy progress indicator for loading states.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SoleneWavyLoader(
    modifier: Modifier = Modifier,
) {
    CircularWavyProgressIndicator(
        modifier = modifier.size(48.dp),
        color = MaterialTheme.colorScheme.tertiary,
    )
}

// ─── Split Button Wrapper ────────────────────────────────────────────

/**
 * Expressive SplitButton for settings and multi-action items.
 *
 * @param leadingText Text for the primary action
 * @param onLeadingClick Primary action callback
 * @param expanded Whether the trailing dropdown is expanded
 * @param onTrailingClick Toggle for the trailing dropdown
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SoleneSplitButton(
    leadingText: String,
    onLeadingClick: () -> Unit,
    expanded: Boolean,
    onTrailingClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SplitButtonLayout(
        leadingButton = {
            SplitButtonDefaults.LeadingButton(
                onClick = onLeadingClick,
            ) {
                Text(
                    text = leadingText,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        },
        trailingButton = {
            SplitButtonDefaults.TrailingButton(
                checked = expanded,
                onCheckedChange = { onTrailingClick() },
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = "Expand",
                )
            }
        },
        modifier = modifier,
    )
}

// ─── Info Card ───────────────────────────────────────────────────────

/**
 * Reusable info card with M3 surface container styling.
 */
@Composable
fun SoleneInfoCard(
    title: String,
    value: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ─── Stat Row ────────────────────────────────────────────────────────

/**
 * Horizontal stat display for the Stats screen.
 */
@Composable
fun StatRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
