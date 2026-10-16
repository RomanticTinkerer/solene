package com.solene.app.ui.log

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Adjust
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Healing
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material.icons.outlined.Opacity
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solene.app.domain.model.BleedingIntensity
import com.solene.app.domain.model.CervixFirmness
import com.solene.app.domain.model.CervixOpening
import com.solene.app.domain.model.CervixPosition
import com.solene.app.domain.model.DesireLevel
import com.solene.app.domain.model.Mood
import com.solene.app.domain.model.MucusQuality
import com.solene.app.domain.model.PainType
import com.solene.app.ui.components.SoleneButtonLarge
import com.solene.app.ui.components.SoleneWavyLoader
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Day log editor — bleeding, temperature, mucus, cervix, mood, pain, desire
 * and a note for one date. Tapping a selected chip again clears it.
 */
@Composable
fun DayLogScreen(
    onDone: () -> Unit,
    viewModel: DayLogViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var confirmClear by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }
    val unsaved = remember(uiState) { viewModel.hasUnsavedChanges() }

    LaunchedEffect(uiState.isDone) {
        if (uiState.isDone) onDone()
    }

    BackHandler(enabled = unsaved) { confirmDiscard = true }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("Discard changes?") },
            text = { Text("Your edits to this day have not been saved.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDiscard = false
                    onDone()
                }) { Text("Discard") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDiscard = false }) { Text("Keep editing") }
            },
        )
    }

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            SoleneWavyLoader()
        }
        return
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear this day?") },
            text = { Text("Everything logged for this date will be removed.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    viewModel.clear()
                }) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("Cancel") }
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        DateHeader(date = uiState.date, cycleDayNumber = uiState.cycleDayNumber)

        LogSection(title = "Bleeding", icon = Icons.Outlined.WaterDrop) {
            ChoiceChips(BleedingIntensity.entries, { it == uiState.bleeding }, viewModel::toggleBleeding)
        }

        LogSection(title = "Temperature", icon = Icons.Outlined.Thermostat) {
            OutlinedTextField(
                value = uiState.temperature,
                onValueChange = viewModel::setTemperature,
                label = { Text("Basal body temperature") },
                suffix = { Text("°C") },
                singleLine = true,
                isError = uiState.temperatureError != null,
                supportingText = {
                    Text(uiState.temperatureError ?: "Taken right after waking, before getting up")
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Exclude from fertility rules", style = MaterialTheme.typography.bodyLarge)
                    SubLabel("Fever, alcohol, short or broken sleep, or taken at an unusual time")
                }
                Switch(
                    checked = uiState.temperatureExcluded,
                    onCheckedChange = viewModel::setTemperatureExcluded,
                )
            }
        }

        LogSection(title = "Cervical mucus", icon = Icons.Outlined.Opacity) {
            SubLabel(
                "The most fertile sign of the day. Dry: feels dry. Nothing: no feeling, nothing seen. " +
                    "Moist: feels moist, nothing seen. Egg white includes a slippery feeling.",
            )
            ChoiceChips(MucusQuality.entries, { it == uiState.mucus }, viewModel::toggleMucus)
        }

        LogSection(title = "Cervix", icon = Icons.Outlined.Adjust) {
            SubLabel("Opening")
            ChoiceChips(CervixOpening.entries, { it == uiState.cervixOpening }, viewModel::toggleCervixOpening)
            SubLabel("Firmness")
            ChoiceChips(CervixFirmness.entries, { it == uiState.cervixFirmness }, viewModel::toggleCervixFirmness)
            SubLabel("Position")
            ChoiceChips(CervixPosition.entries, { it == uiState.cervixPosition }, viewModel::toggleCervixPosition)
        }

        LogSection(title = "Mood", icon = Icons.Outlined.Mood) {
            ChoiceChips(Mood.entries, { it == uiState.mood }, viewModel::toggleMood)
        }

        LogSection(title = "Pain", icon = Icons.Outlined.Healing) {
            ChoiceChips(PainType.entries, { it in uiState.pain }, viewModel::togglePain)
        }

        LogSection(title = "Desire", icon = Icons.Outlined.FavoriteBorder) {
            ChoiceChips(DesireLevel.entries, { it == uiState.desire }, viewModel::toggleDesire)
        }

        LogSection(title = "Note", icon = Icons.Outlined.EditNote) {
            OutlinedTextField(
                value = uiState.note,
                onValueChange = viewModel::setNote,
                placeholder = { Text("Anything else about today") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        SoleneButtonLarge(
            onClick = viewModel::save,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Save", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }

        if (uiState.hasSavedEntry) {
            TextButton(
                onClick = { confirmClear = true },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text("Clear this day", color = MaterialTheme.colorScheme.error)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun DateHeader(date: LocalDate, cycleDayNumber: Int?) {
    val title = if (date == LocalDate.now()) {
        "Today"
    } else {
        date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault()))
    }
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        cycleDayNumber?.let {
            Text(
                text = "Cycle day $it",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LogSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            content()
        }
    }
}

@Composable
private fun SubLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T : Enum<T>> ChoiceChips(
    options: List<T>,
    isSelected: (T) -> Boolean,
    onToggle: (T) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            val selected = isSelected(option)
            FilterChip(
                selected = selected,
                onClick = { onToggle(option) },
                label = { Text(option.label()) },
                leadingIcon = if (selected) {
                    { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
                } else {
                    null
                },
            )
        }
    }
}

/** EGG_WHITE → "Egg white". */
private fun Enum<*>.label(): String =
    name.lowercase(Locale.ROOT).replace('_', ' ').replaceFirstChar { it.titlecase(Locale.ROOT) }
