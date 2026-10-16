package com.solene.app.ui.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solene.app.notifications.ReminderSchedule
import com.solene.app.ui.components.SoleneButtonSmall
import com.solene.app.ui.components.SoleneSplitButton
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Settings screen — fully functional M3 Expressive settings.
 *
 * Features:
 * - SplitButton for Export Data → launches Android share sheet
 * - Daily and period reminders; enabling one asks for notification permission
 * - Reminder time → TimePicker dialog starting at the saved time
 * - SplitButton for NFP Method → dropdown selection
 * - Clickable About → AlertDialog
 * - Clickable License → AlertDialog with GPL-3.0 summary
 * - Clickable Privacy → AlertDialog
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Re-read on resume: the user may change this in system settings and come back.
    var notificationsAllowed by remember { mutableStateOf(areNotificationsAllowed(context)) }
    var permissionDenied by remember { mutableStateOf(false) }
    var enableAfterPermission by remember { mutableStateOf<(() -> Unit)?>(null) }
    LifecycleResumeEffect(Unit) {
        notificationsAllowed = areNotificationsAllowed(context)
        onPauseOrDispose { }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        notificationsAllowed = areNotificationsAllowed(context)
        permissionDenied = !granted
        if (granted) enableAfterPermission?.invoke()
        enableAfterPermission = null
    }
    val enableReminder: (() -> Unit) -> Unit = { enable ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission(context)) {
            enableAfterPermission = enable
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            enable()
        }
    }

    // Handle export share intent
    LaunchedEffect(uiState.exportDataReady) {
        uiState.exportDataReady?.let { data ->
            val mimeType = if (uiState.exportFormat == ExportFormat.CSV) "text/csv" else "application/json"
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_TEXT, data)
                putExtra(Intent.EXTRA_SUBJECT, "Solene Cycle Data Export")
            }
            context.startActivity(Intent.createChooser(intent, "Export cycle data"))
            viewModel.clearExportData()
        }
    }

    // Time Picker dialog
    if (uiState.showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = uiState.reminderTime.hour,
            initialMinute = uiState.reminderTime.minute,
            is24Hour = DateFormat.is24HourFormat(context),
        )
        AlertDialog(
            onDismissRequest = { viewModel.toggleTimePicker() },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setReminderTime(timePickerState.hour, timePickerState.minute)
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.toggleTimePicker() }) { Text("Cancel") }
            },
            title = { Text("Set Reminder Time") },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    TimePicker(state = timePickerState)
                }
            },
        )
    }

    // About dialog
    if (uiState.showAboutDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.toggleAboutDialog() },
            confirmButton = {
                TextButton(onClick = { viewModel.toggleAboutDialog() }) { Text("Close") }
            },
            icon = { Icon(Icons.Outlined.Info, contentDescription = null) },
            title = { Text("About Solene") },
            text = {
                Column {
                    Text("Version ${uiState.appVersion}", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Solene is a free and open-source menstrual cycle tracking app, " +
                            "forked from drip. Your data never leaves your device.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Built with Jetpack Compose & Material 3 Expressive.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
        )
    }

    // License dialog
    if (uiState.showLicenseDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.toggleLicenseDialog() },
            confirmButton = {
                TextButton(onClick = { viewModel.toggleLicenseDialog() }) { Text("Close") }
            },
            icon = { Icon(Icons.Outlined.Gavel, contentDescription = null) },
            title = { Text("License") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                ) {
                    Text(
                        "GNU General Public License v3.0 or later",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "This program is free software: you can redistribute it and/or modify " +
                            "it under the terms of the GNU General Public License as published by " +
                            "the Free Software Foundation, either version 3 of the License, or " +
                            "(at your option) any later version.\n\n" +
                            "This program is distributed in the hope that it will be useful, " +
                            "but WITHOUT ANY WARRANTY; without even the implied warranty of " +
                            "MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the " +
                            "GNU General Public License for more details.\n\n" +
                            "You should have received a copy of the GNU General Public License " +
                            "along with this program. If not, see https://www.gnu.org/licenses/.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Original project: drip. by Bloody Health GbR\n" +
                            "Fertility rules: Kotlin port of sympto by Julia Friesel, Marie Kochsiek " +
                            "and Tina Baumann, licensed AGPL-3.0-or-later",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
        )
    }

    // Privacy dialog
    if (uiState.showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.togglePrivacyDialog() },
            confirmButton = {
                TextButton(onClick = { viewModel.togglePrivacyDialog() }) { Text("Close") }
            },
            icon = { Icon(Icons.Outlined.PrivacyTip, contentDescription = null) },
            title = { Text("Privacy Policy") },
            text = {
                Column {
                    Text(
                        "Your data stays on your device",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Solene stores all your cycle data locally on your device. " +
                            "No data is sent to any server, cloud service, or third party.\n\n" +
                            "• All tracking data is stored in local app storage\n" +
                            "• No analytics or tracking SDKs are included\n" +
                            "• No internet permission is required\n" +
                            "• Export is manual and under your full control\n\n" +
                            "You own your data. Always.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        // ── Notifications Section ──
        val reminderTimeText = uiState.reminderTime
            .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
        SettingsSection(title = "Notifications") {
            ListItem(
                headlineContent = {
                    Text("Daily reminder", style = MaterialTheme.typography.bodyLarge)
                },
                supportingContent = {
                    Text(
                        "A nudge at $reminderTimeText if you haven't logged that day",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                leadingContent = {
                    Icon(Icons.Outlined.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
                trailingContent = {
                    Switch(
                        checked = uiState.dailyReminderEnabled,
                        onCheckedChange = { checked ->
                            if (checked) {
                                enableReminder { viewModel.setDailyReminder(true) }
                            } else {
                                viewModel.setDailyReminder(false)
                            }
                        },
                    )
                },
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)

            ListItem(
                headlineContent = {
                    Text("Period reminder", style = MaterialTheme.typography.bodyLarge)
                },
                supportingContent = {
                    Text(
                        "${ReminderSchedule.PERIOD_REMINDER_DAYS_BEFORE} days before your predicted period",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                leadingContent = {
                    Icon(Icons.Outlined.WaterDrop, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
                trailingContent = {
                    Switch(
                        checked = uiState.periodReminderEnabled,
                        onCheckedChange = { checked ->
                            if (checked) {
                                enableReminder { viewModel.setPeriodReminder(true) }
                            } else {
                                viewModel.setPeriodReminder(false)
                            }
                        },
                    )
                },
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)

            ListItem(
                headlineContent = { Text("Reminder time", style = MaterialTheme.typography.bodyLarge) },
                supportingContent = {
                    Text(
                        reminderTimeText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                leadingContent = {
                    Icon(Icons.Outlined.Alarm, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
                trailingContent = {
                    TextButton(onClick = { viewModel.toggleTimePicker() }) { Text("Change") }
                },
                modifier = Modifier.clickable { viewModel.toggleTimePicker() },
            )

            val wantsReminders = uiState.dailyReminderEnabled || uiState.periodReminderEnabled || permissionDenied
            if (!notificationsAllowed && wantsReminders) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                ListItem(
                    headlineContent = {
                        Text(
                            "Notifications are off for Solene",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                        )
                    },
                    supportingContent = {
                        Text(
                            "Reminders can't appear until you allow them in system settings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    leadingContent = {
                        Icon(Icons.Outlined.NotificationsOff, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    },
                    trailingContent = {
                        TextButton(onClick = { context.startActivity(appNotificationSettingsIntent(context)) }) {
                            Text("Allow")
                        }
                    },
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Data Management Section ──
        SettingsSection(title = "Data Management") {
            // Export Data
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Outlined.FileDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column {
                            Text("Export Data", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Format: ${uiState.exportFormat.label}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    SoleneSplitButton(
                        leadingText = "Export",
                        onLeadingClick = { viewModel.triggerExport() },
                        expanded = uiState.isExportDropdownExpanded,
                        onTrailingClick = { viewModel.toggleExportDropdown() },
                    )
                }
                if (uiState.isExportDropdownExpanded) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 56.dp, end = 16.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ExportFormat.entries.forEach { format ->
                            SoleneButtonSmall(
                                onClick = { viewModel.selectExportFormat(format) },
                            ) {
                                Text(format.label, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)

            // NFP Method
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Outlined.Science, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column {
                            Text("Fertility signs", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                uiState.nfpMethod,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    SoleneSplitButton(
                        leadingText = "Change",
                        onLeadingClick = { viewModel.toggleNfpDropdown() },
                        expanded = uiState.isNfpDropdownExpanded,
                        onTrailingClick = { viewModel.toggleNfpDropdown() },
                    )
                }
                if (uiState.isNfpDropdownExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 56.dp, end = 16.dp, bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        nfpMethods.forEach { method ->
                            val isSelected = method == uiState.nfpMethod
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.selectNfpMethod(method) },
                                label = { Text(method) },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── About Section — clickable items ──
        SettingsSection(title = "About") {
            ListItem(
                headlineContent = { Text("About Solene", style = MaterialTheme.typography.bodyLarge) },
                leadingContent = {
                    Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                supportingContent = {
                    Text("Version ${uiState.appVersion}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                modifier = Modifier.clickable { viewModel.toggleAboutDialog() },
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)

            ListItem(
                headlineContent = { Text("License", style = MaterialTheme.typography.bodyLarge) },
                supportingContent = {
                    Text("GPL-3.0-or-later", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                leadingContent = {
                    Icon(Icons.Outlined.Gavel, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                modifier = Modifier.clickable { viewModel.toggleLicenseDialog() },
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)

            ListItem(
                headlineContent = { Text("Privacy Policy", style = MaterialTheme.typography.bodyLarge) },
                supportingContent = {
                    Text("Your data stays on your device", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                leadingContent = {
                    Icon(Icons.Outlined.PrivacyTip, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                modifier = Modifier.clickable { viewModel.togglePrivacyDialog() },
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

private fun hasNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

private fun areNotificationsAllowed(context: Context): Boolean =
    hasNotificationPermission(context) && NotificationManagerCompat.from(context).areNotificationsEnabled()

private fun appNotificationSettingsIntent(context: Context): Intent =
    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            Column { content() }
        }
    }
}
