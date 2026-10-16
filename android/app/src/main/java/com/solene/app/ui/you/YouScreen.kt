package com.solene.app.ui.you

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solene.app.data.healthconnect.HealthConnectAvailability
import com.solene.app.data.healthconnect.HealthVitals
import com.solene.app.ui.components.SoleneButtonMedium
import com.solene.app.ui.components.SoleneWavyLoader
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.LocalDate

/**
 * "You" personal dashboard screen.
 *
 * Features:
 * - Avatar header with person icon
 * - Health Vitals card (via Health Connect API)
 * - Personal info card (height, weight, birth date)
 * - Cycle preferences card (cycle length, period length)
 * - Partner Links placeholder card
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YouScreen(
    viewModel: YouViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Health Connect permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = viewModel.getPermissionContract(),
    ) { granted ->
        viewModel.onPermissionsResult(granted)
    }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.clearSnackbar()
        }
    }

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            SoleneWavyLoader()
        }
        return
    }

    // Date picker dialog
    if (uiState.showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.birthDate?.let {
                it.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
        )
        DatePickerDialog(
            onDismissRequest = { viewModel.toggleDatePicker() },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = java.time.Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        viewModel.updateBirthDate(date)
                    }
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.toggleDatePicker() }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ── Avatar Header ──
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = "Profile",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Your Profile",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── Health Vitals Card (Health Connect) ──
            HealthVitalsCard(
                availability = uiState.healthConnectAvailability,
                permissionsGranted = uiState.healthPermissionsGranted,
                vitals = uiState.healthVitals,
                isLoading = uiState.isLoadingHealth,
                error = uiState.healthError,
                onRequestPermissions = {
                    permissionLauncher.launch(viewModel.getRequiredPermissions())
                },
                onRefresh = { viewModel.refreshHealthData() },
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Personal Info Card ──
            SectionCard(title = "Personal Info", icon = Icons.Outlined.Person) {
                OutlinedTextField(
                    value = uiState.heightCm,
                    onValueChange = { viewModel.updateHeight(it) },
                    label = { Text("Height (cm)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = uiState.weightKg,
                    onValueChange = { viewModel.updateWeight(it) },
                    label = { Text("Weight (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = uiState.birthDate?.format(DateTimeFormatter.ofPattern("MMM d, yyyy")) ?: "",
                    onValueChange = {},
                    label = { Text("Birth Date") },
                    readOnly = true,
                    enabled = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.toggleDatePicker() },
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )

                Spacer(modifier = Modifier.height(16.dp))

                SoleneButtonMedium(
                    onClick = { viewModel.savePersonalInfo() },
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text("Save")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Cycle Preferences Card ──
            SectionCard(title = "Cycle Preferences", icon = Icons.Outlined.Loop) {
                OutlinedTextField(
                    value = uiState.averageCycleLength,
                    onValueChange = { viewModel.updateCycleLength(it) },
                    label = { Text("Average Cycle Length (days)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text("Typically 21–45 days") },
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = uiState.averagePeriodLength,
                    onValueChange = { viewModel.updatePeriodLength(it) },
                    label = { Text("Average Period Length (days)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text("Typically 2–10 days") },
                )

                Spacer(modifier = Modifier.height(16.dp))

                SoleneButtonMedium(
                    onClick = { viewModel.saveCyclePreferences() },
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text("Save")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Partner Links Card (Placeholder) ──
            SectionCard(title = "Partner Links", icon = Icons.Outlined.Link) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.PeopleAlt,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Share your cycle with a partner",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        SuggestionChip(
                            onClick = {},
                            label = { Text("Coming soon") },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

// ─── Health Vitals Card ──────────────────────────────────────────────

@Composable
private fun HealthVitalsCard(
    availability: HealthConnectAvailability,
    permissionsGranted: Boolean,
    vitals: HealthVitals,
    isLoading: Boolean,
    error: String?,
    onRequestPermissions: () -> Unit,
    onRefresh: () -> Unit,
) {
    SectionCard(title = "Health Vitals", icon = Icons.Outlined.MonitorHeart) {
        when {
            availability == HealthConnectAvailability.NOT_SUPPORTED -> {
                EmptyHealthState(
                    icon = Icons.Outlined.PhonelinkOff,
                    message = "Health Connect is not available on this device",
                )
            }
            availability == HealthConnectAvailability.NOT_INSTALLED -> {
                EmptyHealthState(
                    icon = Icons.Outlined.DownloadForOffline,
                    message = "Install Health Connect from Google Play to sync your vitals",
                )
            }
            !permissionsGranted -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.HealthAndSafety,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Connect to Health Connect to see your vitals",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    SoleneButtonMedium(onClick = onRequestPermissions) {
                        Icon(Icons.Outlined.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Connect")
                    }
                }
            }
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    SoleneWavyLoader()
                }
            }
            error != null -> {
                EmptyHealthState(
                    icon = Icons.Outlined.ErrorOutline,
                    message = error,
                )
            }
            else -> {
                // Show vitals grid
                HealthVitalsGrid(vitals = vitals, onRefresh = onRefresh)
            }
        }
    }
}

@Composable
private fun HealthVitalsGrid(
    vitals: HealthVitals,
    onRefresh: () -> Unit,
) {
    val hasAnyData = vitals.heartRate != null || vitals.oxygenSaturation != null ||
        vitals.bodyTemperature != null || vitals.stepsToday != null ||
        vitals.sleepDurationMinutes != null || vitals.respiratoryRate != null ||
        vitals.systolicBp != null || vitals.restingHeartRate != null

    if (!hasAnyData) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Outlined.DataUsage,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "No recent health data found.\nLog data with your wearable or fitness app.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(12.dp))
            SoleneButtonMedium(onClick = onRefresh) {
                Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Refresh")
            }
        }
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        // Row 1: Heart Rate + SpO2
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            VitalTile(
                icon = Icons.Outlined.FavoriteBorder,
                label = "Heart Rate",
                value = vitals.heartRate?.let { "$it" },
                unit = "bpm",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f),
            )
            VitalTile(
                icon = Icons.Outlined.Bloodtype,
                label = "SpO₂",
                value = vitals.oxygenSaturation?.let { "%.0f".format(it) },
                unit = "%",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
        }

        // Row 2: Body Temp + Respiratory
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            VitalTile(
                icon = Icons.Outlined.Thermostat,
                label = "Body Temp",
                value = vitals.bodyTemperature?.let { "%.1f".format(it) },
                unit = "°C",
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f),
            )
            VitalTile(
                icon = Icons.Outlined.Air,
                label = "Resp. Rate",
                value = vitals.respiratoryRate?.let { "%.0f".format(it) },
                unit = "/min",
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f),
            )
        }

        // Row 3: Blood Pressure + Resting HR
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            VitalTile(
                icon = Icons.Outlined.Speed,
                label = "Blood Pressure",
                value = if (vitals.systolicBp != null && vitals.diastolicBp != null) {
                    "%.0f/%.0f".format(vitals.systolicBp, vitals.diastolicBp)
                } else null,
                unit = "mmHg",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f),
            )
            VitalTile(
                icon = Icons.Outlined.MonitorHeart,
                label = "Resting HR",
                value = vitals.restingHeartRate?.let { "$it" },
                unit = "bpm",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
        }

        // Row 4: Steps + Sleep
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            VitalTile(
                icon = Icons.AutoMirrored.Outlined.DirectionsWalk,
                label = "Steps Today",
                value = vitals.stepsToday?.let { "%,d".format(it) },
                unit = "",
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f),
            )
            VitalTile(
                icon = Icons.Outlined.Bedtime,
                label = "Sleep",
                value = vitals.sleepDurationMinutes?.let {
                    val hours = it / 60
                    val mins = it % 60
                    "${hours}h ${mins}m"
                },
                unit = "",
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Refresh + timestamp
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            vitals.heartRateTimestamp?.let { ts ->
                Text(
                    text = "Last synced: ${
                        ts.atZone(ZoneId.systemDefault())
                            .format(DateTimeFormatter.ofPattern("h:mm a"))
                    }",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } ?: Text(
                text = "Tap refresh to sync",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            IconButton(onClick = onRefresh) {
                Icon(
                    Icons.Outlined.Refresh,
                    contentDescription = "Refresh",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun VitalTile(
    icon: ImageVector,
    label: String,
    value: String?,
    unit: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.padding(vertical = 4.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = color,
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            if (value != null) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (unit.isNotEmpty()) {
                        Text(
                            text = unit,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 2.dp),
                        )
                    }
                }
            } else {
                Text(
                    text = "—",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                )
            }
        }
    }
}

@Composable
private fun EmptyHealthState(
    icon: ImageVector,
    message: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

// ─── Shared Section Card ─────────────────────────────────────────────

@Composable
private fun SectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}
