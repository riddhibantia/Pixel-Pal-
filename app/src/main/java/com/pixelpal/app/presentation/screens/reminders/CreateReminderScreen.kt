package com.pixelpal.app.presentation.screens.reminders

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.pixelpal.app.domain.model.Companion
import com.pixelpal.app.presentation.components.AppTextField
import com.pixelpal.app.presentation.components.AppTopBar
import com.pixelpal.app.presentation.components.GroupDivider
import com.pixelpal.app.presentation.components.PrimaryButton
import com.pixelpal.app.presentation.components.SectionHeader
import com.pixelpal.app.presentation.components.SettingsGroup
import com.pixelpal.app.presentation.components.SettingsRow
import com.pixelpal.app.presentation.theme.Radius
import com.pixelpal.app.presentation.theme.Spacing
import com.pixelpal.app.util.PermissionHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private enum class TimeMode { QUICK, CLOCK, DATE_TIME }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateReminderScreen(
    navController: NavController,
    viewModel: ReminderViewModel = hiltViewModel()
) {
    var title by remember { mutableStateOf("") }
    val context = LocalContext.current
    val companions by viewModel.companions.collectAsState()
    val activeCompanionId by viewModel.activeCompanionId.collectAsState()

    // Defaults to the nav-arg companion (when opened from a card), else the active one.
    var selectedCompanionId by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(viewModel.initialCompanionId, activeCompanionId) {
        if (selectedCompanionId == null) {
            selectedCompanionId = viewModel.initialCompanionId ?: activeCompanionId
        }
    }

    var selectedMode by remember { mutableStateOf(TimeMode.QUICK) }
    var selectedQuickMinutes by remember { mutableStateOf(60L) }

    val currentTime = Calendar.getInstance()
    val timePickerState = rememberTimePickerState(
        initialHour = currentTime.get(Calendar.HOUR_OF_DAY),
        initialMinute = currentTime.get(Calendar.MINUTE),
        is24Hour = false
    )

    var useClockPicker by remember { mutableStateOf(true) }

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )

    var soundUri by remember { mutableStateOf<String?>(null) }
    var soundName by remember { mutableStateOf("Default Sound") }

    val dateFormatter = remember { SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()) }

    val ringtoneLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(
                    RingtoneManager.EXTRA_RINGTONE_PICKED_URI,
                    Uri::class.java
                )
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            soundUri = uri?.toString()
            soundName = if (uri != null) {
                RingtoneManager.getRingtone(context, uri).getTitle(context)
            } else {
                "Silent"
            }
        }
    }

    fun combineDateAndTime(dateMillis: Long, hour: Int, minute: Int): Long {
        val localMidnight = dateMillis + TimeZone.getDefault().getOffset(dateMillis)
        val date = Calendar.getInstance().apply { timeInMillis = localMidnight }
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, date.get(Calendar.YEAR))
            set(Calendar.MONTH, date.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, date.get(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun computeTriggerTime(): Long? {
        return when (selectedMode) {
            TimeMode.QUICK -> {
                Calendar.getInstance().timeInMillis + selectedQuickMinutes * 60 * 1000L
            }
            TimeMode.DATE_TIME -> {
                val dateMillis = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                combineDateAndTime(dateMillis, timePickerState.hour, timePickerState.minute)
            }
            TimeMode.CLOCK -> {
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                    set(Calendar.MINUTE, timePickerState.minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    if (before(Calendar.getInstance())) {
                        add(Calendar.DAY_OF_MONTH, 1)
                    }
                }
                calendar.timeInMillis
            }
        }
    }

    fun saveReminder() {
        val triggerTime = computeTriggerTime() ?: return
        if (title.isNotBlank()) {
            val cal = Calendar.getInstance().apply { timeInMillis = triggerTime }
            viewModel.createReminder(
                title = title,
                message = null,
                triggerTime = triggerTime,
                hour = cal.get(Calendar.HOUR_OF_DAY),
                minute = cal.get(Calendar.MINUTE),
                soundUri = soundUri,
                companionId = selectedCompanionId
            )
        }
    }

    val exactAlarmLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        saveReminder()
    }

    fun onSavePressed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !PermissionHelper.canScheduleExactAlarms(context)
        ) {
            val intent = Intent(
                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                Uri.parse("package:${context.packageName}")
            )
            exactAlarmLauncher.launch(intent)
        } else {
            saveReminder()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.reminderCreated.collect {
            navController.popBackStack()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(title = "New Reminder", onBack = { navController.popBackStack() })

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenHorizontal)
                .padding(bottom = Spacing.xl)
        ) {
            // ── WHAT ──
            SectionHeader(title = "What")
            AppTextField(
                value = title,
                onValueChange = { title = it },
                label = "Reminder",
                placeholder = "What do you want to be reminded of?"
            )

            // ── WHO ──
            if (companions.isNotEmpty()) {
                SectionHeader(title = "Who")
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    companions.forEach { companion ->
                        FilterChip(
                            selected = selectedCompanionId == companion.id,
                            onClick = { selectedCompanionId = companion.id },
                            label = { Text(companion.name) }
                        )
                    }
                }
            }

            // ── WHEN ──
            SectionHeader(title = "When")
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = selectedMode == TimeMode.QUICK,
                    onClick = { selectedMode = TimeMode.QUICK },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                ) {
                    Text("Quick")
                }
                SegmentedButton(
                    selected = selectedMode == TimeMode.CLOCK,
                    onClick = { selectedMode = TimeMode.CLOCK },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                ) {
                    Text("Clock")
                }
                SegmentedButton(
                    selected = selectedMode == TimeMode.DATE_TIME,
                    onClick = { selectedMode = TimeMode.DATE_TIME },
                    shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                ) {
                    Text("Date & Time")
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            when (selectedMode) {
                TimeMode.QUICK -> {
                    Text(
                        text = "Remind me in:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    val quickOptions = listOf(
                        15L to "15 min",
                        30L to "30 min",
                        60L to "1 hour",
                        180L to "3 hours",
                        360L to "6 hours",
                        720L to "12 hours",
                        1440L to "1 day"
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        quickOptions.forEach { (minutes, label) ->
                            FilterChip(
                                selected = selectedQuickMinutes == minutes,
                                onClick = { selectedQuickMinutes = minutes },
                                label = { Text(label) }
                            )
                        }
                    }
                }

                TimeMode.CLOCK -> {
                    // Toggle between analog clock and digital input
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { useClockPicker = true }) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = "Analog clock",
                                tint = if (useClockPicker) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { useClockPicker = false }) {
                            Icon(
                                imageVector = Icons.Default.Keyboard,
                                contentDescription = "Digital input",
                                tint = if (!useClockPicker) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    if (useClockPicker) {
                        TimePicker(state = timePickerState)
                    } else {
                        TimeInput(state = timePickerState)
                    }
                }

                TimeMode.DATE_TIME -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(Radius.medium))
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(Radius.medium))
                            .clickable { showDatePicker = true }
                            .padding(horizontal = Spacing.md, vertical = Spacing.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Event,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(Spacing.md))
                        Text(
                            text = datePickerState.selectedDateMillis?.let {
                                dateFormatter.format(Date(it + TimeZone.getDefault().getOffset(it)))
                            } ?: "Select date",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (showDatePicker) {
                        DatePickerDialog(
                            onDismissRequest = { showDatePicker = false },
                            confirmButton = {
                                TextButton(onClick = { showDatePicker = false }) {
                                    Text("OK")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDatePicker = false }) {
                                    Text("Cancel")
                                }
                            }
                        ) {
                            DatePicker(state = datePickerState)
                        }
                    }

                    Spacer(modifier = Modifier.height(Spacing.md))
                    TimeInput(state = timePickerState)
                }
            }

            // ── NOTIFICATION ──
            SectionHeader(title = "Notification")
            SettingsGroup {
                SettingsRow(
                    title = "Notification Sound",
                    description = "The sound Pixel plays when the reminder fires",
                    value = soundName,
                    icon = Icons.Default.MusicNote,
                    onClick = {
                        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                        }
                        ringtoneLauncher.launch(intent)
                    }
                )
            }

            Spacer(modifier = Modifier.height(Spacing.lg))

            PrimaryButton(
                text = "Save Reminder",
                enabled = title.isNotBlank(),
                onClick = { onSavePressed() }
            )
        }
    }
}