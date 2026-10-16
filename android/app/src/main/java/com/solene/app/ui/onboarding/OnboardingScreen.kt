package com.solene.app.ui.onboarding

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * First-startup onboarding — granular, no stock cycle data.
 *
 * 0 Welcome
 * 1 Last period start date (required, no default offset)
 * 2 Day-by-day bleeding intensity for that period
 * 3 Cycle length + optional prior lengths (user-entered only)
 * 4 Optional fertility / reproductive context for the model
 * 5 Optional personal profile
 */
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isCompleted) {
        if (uiState.isCompleted) onComplete()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            repeat(uiState.totalPages) { index ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(
                            width = if (index == uiState.currentPage) 32.dp else 8.dp,
                            height = 8.dp,
                        )
                        .clip(CircleShape)
                        .background(
                            if (index == uiState.currentPage)
                                MaterialTheme.colorScheme.primary
                            else if (index < uiState.currentPage)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            else
                                MaterialTheme.colorScheme.surfaceContainerHigh,
                        ),
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            AnimatedContent(
                targetState = uiState.currentPage,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { it } + fadeIn() togetherWith
                            slideOutHorizontally { -it } + fadeOut()
                    } else {
                        slideInHorizontally { -it } + fadeIn() togetherWith
                            slideOutHorizontally { it } + fadeOut()
                    }
                },
                label = "onboarding_pages",
            ) { page ->
                when (page) {
                    0 -> WelcomePage()
                    1 -> PeriodDatePage(
                        selectedDate = uiState.lastPeriodDate,
                        showDatePicker = uiState.showDatePicker,
                        onToggleDatePicker = { viewModel.toggleDatePicker() },
                        onDateSelected = { viewModel.setLastPeriodDate(it) },
                    )
                    2 -> PeriodIntensityPage(
                        scores = uiState.periodDayScores,
                        onScoreChange = { day, score -> viewModel.setPeriodDayScore(day, score) },
                    )
                    3 -> CycleInfoPage(
                        cycleLength = uiState.averageCycleLength,
                        priorCycleLengthsText = uiState.priorCycleLengthsText,
                        onCycleLengthChange = { viewModel.setCycleLength(it) },
                        onPriorLengthsChange = { viewModel.setPriorCycleLengthsText(it) },
                    )
                    4 -> FertilityContextPage(
                        ovulationDay = uiState.typicalOvulationDay,
                        lutealLength = uiState.typicalLutealLength,
                        reproductiveCategory = uiState.reproductiveCategory,
                        pregnancies = uiState.numberOfPregnancies,
                        livingChildren = uiState.livingChildren,
                        breastfeeding = uiState.breastfeeding,
                        tracksPeakDays = uiState.tracksPeakDays,
                        onOvulationChange = { viewModel.setTypicalOvulationDay(it) },
                        onLutealChange = { viewModel.setTypicalLutealLength(it) },
                        onReproductiveCategory = { viewModel.setReproductiveCategory(it) },
                        onPregnancies = { viewModel.setNumberOfPregnancies(it) },
                        onLivingChildren = { viewModel.setLivingChildren(it) },
                        onBreastfeeding = { viewModel.setBreastfeeding(it) },
                        onTracksPeak = { viewModel.setTracksPeakDays(it) },
                    )
                    5 -> PersonalInfoPage(
                        heightCm = uiState.heightCm,
                        weightKg = uiState.weightKg,
                        birthDate = uiState.birthDate,
                        showDatePicker = uiState.showBirthDatePicker,
                        onHeightChange = { viewModel.updateHeight(it) },
                        onWeightChange = { viewModel.updateWeight(it) },
                        onToggleDatePicker = { viewModel.toggleBirthDatePicker() },
                        onDateSelected = { viewModel.setBirthDate(it) },
                    )
                }
            }
        }

        uiState.validationMessage?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (uiState.currentPage > 0) {
                OutlinedButton(onClick = { viewModel.previousPage() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Back")
                }
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }

            when {
                uiState.currentPage == uiState.totalPages - 1 -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { viewModel.skipPersonalAndComplete() }) {
                            Text("Skip")
                        }
                        Button(onClick = { viewModel.completeOnboarding() }) {
                            Text("Done")
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                }
                else -> {
                    Button(onClick = { viewModel.nextPage() }) {
                        Text("Next")
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomePage() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Spa,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Welcome to Solene",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Predictions run on your phone with an XGBoost model trained on NFP cycle data.\nWe only use what you enter — no stock cycles.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PeriodDatePage(
    selectedDate: LocalDate?,
    showDatePicker: Boolean,
    onToggleDatePicker: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
) {
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = (selectedDate ?: LocalDate.now())
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = onToggleDatePicker,
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        onDateSelected(date)
                    }
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = onToggleDatePicker) { Text("Cancel") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.CalendarMonth,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "When did your last period start?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "No default date is assumed — pick the real start day",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(32.dp))
        ElevatedCard(
            onClick = onToggleDatePicker,
            shape = MaterialTheme.shapes.large,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    Icons.Outlined.EditCalendar,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = selectedDate?.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"))
                        ?: "Tap to choose date",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (selectedDate != null) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PeriodIntensityPage(
    scores: List<Int>,
    onScoreChange: (dayIndex: Int, score: Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Outlined.WaterDrop,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "How was each day of that period?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Day-by-day intensity feeds the on-device model. Leave unused days as None.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(20.dp))

        scores.forEachIndexed { index, score ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Day ${index + 1}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        BleedingScore.entries.forEach { option ->
                            FilterChip(
                                selected = score == option.score,
                                onClick = { onScoreChange(index, option.score) },
                                label = { Text(option.label) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CycleInfoPage(
    cycleLength: Int?,
    priorCycleLengthsText: String,
    onCycleLengthChange: (Int?) -> Unit,
    onPriorLengthsChange: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Outlined.Loop,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Your cycle length",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Nothing is pre-filled with a stock 28-day cycle. Enter what you know.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = cycleLength?.toString().orEmpty(),
            onValueChange = { text ->
                if (text.isBlank()) onCycleLengthChange(null)
                else text.toIntOrNull()?.let(onCycleLengthChange)
            },
            label = { Text("Usual cycle length (days)") },
            placeholder = { Text("e.g. 30") },
            supportingText = { Text("Required unless you list prior cycle lengths below") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        if (cycleLength != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "$cycleLength days",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Slider(
                        value = cycleLength.toFloat(),
                        onValueChange = { onCycleLengthChange(it.toInt()) },
                        valueRange = 18f..60f,
                        steps = 41,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = priorCycleLengthsText,
            onValueChange = onPriorLengthsChange,
            label = { Text("Prior completed cycle lengths (optional)") },
            placeholder = { Text("e.g. 27, 29, 28") },
            supportingText = {
                Text("Oldest first, only lengths you actually remember. With one or two, the on-device model fills in; from three on, your own average is used.")
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun FertilityContextPage(
    ovulationDay: Int?,
    lutealLength: Int?,
    reproductiveCategory: Int,
    pregnancies: String,
    livingChildren: String,
    breastfeeding: Boolean,
    tracksPeakDays: Boolean,
    onOvulationChange: (Int?) -> Unit,
    onLutealChange: (Int?) -> Unit,
    onReproductiveCategory: (Int) -> Unit,
    onPregnancies: (String) -> Unit,
    onLivingChildren: (String) -> Unit,
    onBreastfeeding: (Boolean) -> Unit,
    onTracksPeak: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Outlined.Science,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Model context (optional)",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "These map to NFP features the on-device XGBoost model was trained on.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = ovulationDay?.toString().orEmpty(),
            onValueChange = { onOvulationChange(it.toIntOrNull()) },
            label = { Text("Typical ovulation day (cycle day)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = lutealLength?.toString().orEmpty(),
            onValueChange = { onLutealChange(it.toIntOrNull()) },
            label = { Text("Typical luteal length (days)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = pregnancies,
            onValueChange = onPregnancies,
            label = { Text("Number of pregnancies") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = livingChildren,
            onValueChange = onLivingChildren,
            label = { Text("Living children") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(12.dp))

        Text("Reproductive category", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0, 1, 2).forEach { value ->
                FilterChip(
                    selected = reproductiveCategory == value,
                    onClick = { onReproductiveCategory(value) },
                    label = { Text("$value") },
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Currently breastfeeding")
            Switch(checked = breastfeeding, onCheckedChange = onBreastfeeding)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("I track peak / fertile days")
            Switch(checked = tracksPeakDays, onCheckedChange = onTracksPeak)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PersonalInfoPage(
    heightCm: String,
    weightKg: String,
    birthDate: LocalDate?,
    showDatePicker: Boolean,
    onHeightChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onToggleDatePicker: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
) {
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = birthDate?.let {
                it.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
        )
        DatePickerDialog(
            onDismissRequest = onToggleDatePicker,
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        onDateSelected(date)
                    }
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = onToggleDatePicker) { Text("Cancel") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Person,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "About you",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Optional — age, height, and weight improve on-device predictions",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(32.dp))
        OutlinedTextField(
            value = heightCm,
            onValueChange = onHeightChange,
            label = { Text("Height (cm)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Outlined.Height, contentDescription = null) },
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = weightKg,
            onValueChange = onWeightChange,
            label = { Text("Weight (kg)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Outlined.MonitorWeight, contentDescription = null) },
        )
        Spacer(modifier = Modifier.height(16.dp))
        ElevatedCard(
            onClick = onToggleDatePicker,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    Icons.Outlined.Cake,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = birthDate?.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"))
                        ?: "Set birth date",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (birthDate != null) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
