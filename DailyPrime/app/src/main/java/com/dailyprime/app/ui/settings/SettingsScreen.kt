package com.dailyprime.app.ui.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailyprime.app.data.preferences.UserPreferences
import com.dailyprime.app.ui.theme.EmeraldPrimary
import com.dailyprime.app.ui.theme.GoldAccent
import com.dailyprime.app.ui.theme.parseHexColorOrDefault
import java.time.LocalDate

private val PRIMARY_SWATCHES = listOf("#064E3B", "#0F766E", "#1D4ED8", "#7C3AED", "#BE123C")
private val ACCENT_SWATCHES = listOf("#D4AF37", "#F3C853", "#10B981", "#F59E0B", "#EC4899")

@Composable
fun SettingsScreen(
    prefs: UserPreferences,
    onUpdatePrefs: ((UserPreferences) -> UserPreferences) -> Unit,
    onExportBackupToUri: (android.net.Uri) -> Unit,
    onImportBackupFromUri: (android.net.Uri) -> Unit,
    onClearSelectedData: (clearTasks: Boolean, clearHistory: Boolean, clearNotes: Boolean, clearNamaz: Boolean) -> Unit,
    onResetSettingsOnly: () -> Unit
) {
    val context = LocalContext.current

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            onUpdatePrefs {
                it.copy(
                    backgroundImageUri = uri.toString(),
                    imageRemoved = false
                )
            }
        }
    }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) onExportBackupToUri(uri)
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onImportBackupFromUri(uri)
    }

    var clearTasks by remember { mutableStateOf(false) }
    var clearHistory by remember { mutableStateOf(false) }
    var clearNotes by remember { mutableStateOf(false) }
    var clearNamaz by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showResetSettingsDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 36.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Customize theme, photo banner, notifications & backups",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                AssistChip(
                    onClick = {},
                    leadingIcon = {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text("Auto-saved", fontSize = 11.sp) }
                )
            }
        }

        // 1. APPEARANCE
        item {
            SettingsCard("1. Appearance & Theme Image") {
                Text("Theme Mode", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("light" to "Light", "dark" to "Dark", "system" to "System").forEach { (mode, label) ->
                        FilterChip(
                            selected = prefs.themeMode == mode,
                            onClick = { onUpdatePrefs { it.copy(themeMode = mode) } },
                            label = { Text(label, fontSize = 12.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Text("Primary & Accent Colors", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PRIMARY_SWATCHES.forEach { hex ->
                        ColorDot(
                            hex = hex,
                            selected = prefs.primaryColorHex == hex,
                            onClick = { onUpdatePrefs { it.copy(primaryColorHex = hex) } }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ACCENT_SWATCHES.forEach { hex ->
                        ColorDot(
                            hex = hex,
                            selected = prefs.accentColorHex == hex,
                            onClick = { onUpdatePrefs { it.copy(accentColorHex = hex) } }
                        )
                    }
                }

                HorizontalDivider()

                Text("Home Banner Image", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { pickImageLauncher.launch(arrayOf("image/*")) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Gallery Photo", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            onUpdatePrefs {
                                it.copy(
                                    backgroundImageUri = "",
                                    imageRemoved = false,
                                    imageBrightness = 1.0f,
                                    imageOpacity = 1.0f
                                )
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Default Photo", fontSize = 12.sp)
                    }
                }

                TextButton(
                    onClick = { onUpdatePrefs { it.copy(imageRemoved = !it.imageRemoved) } }
                ) {
                    Text(
                        text = if (prefs.imageRemoved) "Show Banner Image" else "Remove Banner Image",
                        color = if (prefs.imageRemoved) EmeraldPrimary else Color(0xFFB91C1C)
                    )
                }

                Text("Image Brightness (${(prefs.imageBrightness * 100).toInt()}%)", fontSize = 12.sp)
                Slider(
                    value = prefs.imageBrightness,
                    onValueChange = { v -> onUpdatePrefs { it.copy(imageBrightness = v) } },
                    valueRange = 0.6f..1.35f
                )

                Text("Image Opacity (${(prefs.imageOpacity * 100).toInt()}%)", fontSize = 12.sp)
                Slider(
                    value = prefs.imageOpacity,
                    onValueChange = { v -> onUpdatePrefs { it.copy(imageOpacity = v) } },
                    valueRange = 0.25f..1.0f
                )
            }
        }

        // 2. PERSONALIZATION
        item {
            SettingsCard("2. Personalization & Layout") {
                OutlinedTextField(
                    value = prefs.appDisplayName,
                    onValueChange = { v -> onUpdatePrefs { it.copy(appDisplayName = v) } },
                    label = { Text("App Name Display") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = prefs.userName,
                    onValueChange = { v -> onUpdatePrefs { it.copy(userName = v) } },
                    label = { Text("Your Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = prefs.customGreeting,
                    onValueChange = { v -> onUpdatePrefs { it.copy(customGreeting = v) } },
                    label = { Text("Custom Greeting (blank = auto)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = prefs.customQuote,
                    onValueChange = { v -> onUpdatePrefs { it.copy(customQuote = v) } },
                    label = { Text("Custom Motivational Quote (blank = daily)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Language", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("en" to "English", "bn" to "বাংলা", "ar" to "العربية").forEach { (code, label) ->
                        FilterChip(
                            selected = prefs.language == code,
                            onClick = { onUpdatePrefs { it.copy(language = code) } },
                            label = { Text(label, fontSize = 12.sp) }
                        )
                    }
                }

                SwitchRow(
                    title = "24-Hour Time Format",
                    checked = prefs.use24HourFormat,
                    onCheckedChange = { c -> onUpdatePrefs { it.copy(use24HourFormat = c) } }
                )
                SwitchRow(
                    title = "Compact Home Layout",
                    checked = prefs.compactHomeLayout,
                    onCheckedChange = { c -> onUpdatePrefs { it.copy(compactHomeLayout = c) } }
                )
                SwitchRow(
                    title = "Show Quote Banner on Home",
                    checked = prefs.showQuoteOnHome,
                    onCheckedChange = { c -> onUpdatePrefs { it.copy(showQuoteOnHome = c) } }
                )
            }
        }

        // 3. NOTIFICATIONS
        item {
            SettingsCard("3. Notifications & Prayer Reminders") {
                SwitchRow(
                    title = "Master Notifications",
                    checked = prefs.notificationsEnabled,
                    onCheckedChange = { c -> onUpdatePrefs { it.copy(notificationsEnabled = c) } }
                )
                SwitchRow(
                    title = "Daily Task Reminders",
                    checked = prefs.taskRemindersEnabled,
                    onCheckedChange = { c -> onUpdatePrefs { it.copy(taskRemindersEnabled = c) } }
                )
                SwitchRow(
                    title = "Prayer Tracking Reminders",
                    checked = prefs.prayerRemindersEnabled,
                    onCheckedChange = { c -> onUpdatePrefs { it.copy(prayerRemindersEnabled = c) } }
                )
                SwitchRow(
                    title = "Auto-Mark Overdue Tasks Missed",
                    checked = prefs.autoMarkMissed,
                    onCheckedChange = { c -> onUpdatePrefs { it.copy(autoMarkMissed = c) } }
                )

                Text("Notification Sound", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("chime" to "Chime", "bell" to "Bell", "silent" to "Silent").forEach { (sound, label) ->
                        FilterChip(
                            selected = prefs.notificationSound == sound,
                            onClick = { onUpdatePrefs { it.copy(notificationSound = sound) } },
                            label = { Text(label, fontSize = 12.sp) }
                        )
                    }
                }

                if (prefs.prayerRemindersEnabled) {
                    Text("Prayer Reminder Times (HH:MM)", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = prefs.fajrTime,
                            onValueChange = { v -> onUpdatePrefs { it.copy(fajrTime = v) } },
                            label = { Text("Fajr") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = prefs.dhuhrTime,
                            onValueChange = { v -> onUpdatePrefs { it.copy(dhuhrTime = v) } },
                            label = { Text("Dhuhr") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = prefs.asrTime,
                            onValueChange = { v -> onUpdatePrefs { it.copy(asrTime = v) } },
                            label = { Text("Asr") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = prefs.maghribTime,
                            onValueChange = { v -> onUpdatePrefs { it.copy(maghribTime = v) } },
                            label = { Text("Maghrib") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = prefs.ishaTime,
                            onValueChange = { v -> onUpdatePrefs { it.copy(ishaTime = v) } },
                            label = { Text("Isha") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 4. DATA MANAGEMENT
        item {
            SettingsCard("4. Backup, Clear Data & Reset") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            exportBackupLauncher.launch("DailyPrime-Backup-${LocalDate.now()}.json")
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export JSON", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = { importBackupLauncher.launch(arrayOf("application/json", "*/*")) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Import JSON", fontSize = 12.sp)
                    }
                }

                HorizontalDivider()

                Text("Clear Selected Data", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                CheckboxRow("Clear Daily Tasks", clearTasks) { clearTasks = it }
                CheckboxRow("Clear Task History", clearHistory) { clearHistory = it }
                CheckboxRow("Clear Notes", clearNotes) { clearNotes = it }
                CheckboxRow("Clear Prayer History", clearNamaz) { clearNamaz = it }

                if (clearTasks || clearHistory || clearNotes || clearNamaz) {
                    Button(
                        onClick = { showClearDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB91C1C)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Clear Selected Sections...")
                    }
                }

                OutlinedButton(
                    onClick = { showResetSettingsDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reset Application Settings Only")
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Confirm Clear Selected Data") },
            text = { Text("Selected sections will be permanently erased from this device. Unselected sections remain untouched.") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearSelectedData(clearTasks, clearHistory, clearNotes, clearNamaz)
                        clearTasks = false
                        clearHistory = false
                        clearNotes = false
                        clearNamaz = false
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB91C1C))
                ) {
                    Text("Confirm Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showResetSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showResetSettingsDialog = false },
            title = { Text("Reset Application Settings?") },
            text = { Text("Colors, theme mode, custom greeting/quote, and reminder settings will reset to defaults. Your tasks, notes, and prayers will stay safe.") },
            confirmButton = {
                Button(onClick = {
                    onResetSettingsOnly()
                    showResetSettingsDialog = false
                }) {
                    Text("Reset Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetSettingsDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = EmeraldPrimary)
                content()
            }
        )
    }
}

@Composable
private fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun CheckboxRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(title, fontSize = 13.sp)
    }
}

@Composable
private fun ColorDot(
    hex: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val color = parseHexColorOrDefault(hex, EmeraldPrimary)
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (selected) 2.5.dp else 1.dp,
                color = if (selected) GoldAccent else Color.Gray.copy(alpha = 0.4f),
                shape = CircleShape
            )
            .clickable(onClick = onClick)
    )
}
