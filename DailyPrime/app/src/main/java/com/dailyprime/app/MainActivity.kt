package com.dailyprime.app

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.dailyprime.app.data.local.NamazRecordEntity
import com.dailyprime.app.data.local.NoteEntity
import com.dailyprime.app.data.local.TaskEntity
import com.dailyprime.app.data.preferences.UserPreferences
import com.dailyprime.app.notifications.ReminderScheduler
import com.dailyprime.app.ui.dashboard.DashboardScreen
import com.dailyprime.app.ui.namaz.NamazScreen
import com.dailyprime.app.ui.namaz.togglePrayer
import com.dailyprime.app.ui.notes.NotesScreen
import com.dailyprime.app.ui.settings.SettingsScreen
import com.dailyprime.app.ui.theme.DailyPrimeTheme
import com.dailyprime.app.ui.theme.EmeraldDeep
import com.dailyprime.app.ui.theme.EmeraldPrimary
import com.dailyprime.app.ui.theme.GoldAccent
import com.dailyprime.app.ui.theme.GoldLight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

private enum class BottomSection(val label: String) {
    HOME("Home"),
    NOTES("Notes"),
    NAMAZ("Namaz"),
    SETTINGS("Settings")
}

private val HOME_QUOTES = listOf(
    "Discipline is choosing what you want most over what you want right now.",
    "Win the morning with intention, and the rest of the day follows calmly.",
    "Small consistent steps every day build extraordinary years.",
    "Protect your focus, honor your promises, and end the day grateful."
)

class MainActivity : ComponentActivity() {
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val app = application as DailyPrimeApplication

        setContent {
            val prefs by app.preferencesRepository.preferencesFlow.collectAsState(
                initial = UserPreferences()
            )
            val todayStr = remember { LocalDate.now().toString() }
            val tasks by app.database.taskDao().observeTasksForDate(todayStr)
                .collectAsState(initial = emptyList())
            val notes by app.database.noteDao().observeAllNotes()
                .collectAsState(initial = emptyList())
            val history by app.database.dailyHistoryDao().observeAllHistory()
                .collectAsState(initial = emptyList())
            val todayNamaz by app.database.namazDao().observeNamazRecordForDate(todayStr)
                .collectAsState(initial = null)
            val allNamazRecords by app.database.namazDao().observeAllNamazRecords()
                .collectAsState(initial = emptyList())

            var currentSection by remember { mutableStateOf(BottomSection.HOME) }
            var showDashboardOverlay by remember { mutableStateOf(false) }
            val scope = rememberCoroutineScope()

            LaunchedEffect(todayStr) {
                app.database.namazDao().insertIfAbsent(
                    NamazRecordEntity(
                        date = todayStr,
                        updatedAt = LocalDateTime.now().toString()
                    )
                )
            }

            val currentNamazRecord = todayNamaz ?: NamazRecordEntity(
                date = todayStr,
                updatedAt = LocalDateTime.now().toString()
            )

            DailyPrimeTheme(
                themeMode = prefs.themeMode,
                primaryHex = prefs.primaryColorHex,
                accentHex = prefs.accentColorHex,
                backgroundHex = prefs.backgroundColorHex
            ) {
                if (showDashboardOverlay) {
                    DashboardScreen(
                        todayTasks = tasks,
                        history = history,
                        onBack = { showDashboardOverlay = false }
                    )
                } else {
                    Scaffold(
                        bottomBar = {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface
                            ) {
                                BottomSection.entries.forEach { section ->
                                    val selected = currentSection == section
                                    val icon = when (section) {
                                        BottomSection.HOME -> Icons.Default.Home
                                        BottomSection.NOTES -> Icons.Default.Notes
                                        BottomSection.NAMAZ -> Icons.Default.NightsStay
                                        BottomSection.SETTINGS -> Icons.Default.Settings
                                    }
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { currentSection = section },
                                        icon = { Icon(icon, contentDescription = section.label) },
                                        label = { Text(section.label, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (currentSection) {
                                BottomSection.HOME -> {
                                    DailyPrimeHomeScreen(
                                        prefs = prefs,
                                        todayStr = todayStr,
                                        tasks = tasks,
                                        onOpenDashboard = { showDashboardOverlay = true },
                                        onToggleTask = { task ->
                                            scope.launch {
                                                val updated = task.copy(
                                                    completed = !task.completed,
                                                    completedAt = if (!task.completed) LocalTime.now().toString() else null
                                                )
                                                app.database.taskDao().updateTask(updated)
                                                ReminderScheduler.scheduleTaskReminder(
                                                    this@MainActivity,
                                                    updated,
                                                    soundMode = prefs.notificationSound,
                                                    masterEnabled = prefs.notificationsEnabled && prefs.taskRemindersEnabled
                                                )
                                            }
                                        },
                                        onSaveTask = { taskEntity ->
                                            scope.launch {
                                                app.database.taskDao().upsertTask(taskEntity)
                                                ReminderScheduler.scheduleTaskReminder(
                                                    this@MainActivity,
                                                    taskEntity,
                                                    soundMode = prefs.notificationSound,
                                                    masterEnabled = prefs.notificationsEnabled && prefs.taskRemindersEnabled
                                                )
                                            }
                                        },
                                        onDeleteTask = { taskId ->
                                            scope.launch {
                                                app.database.taskDao().deleteTask(taskId)
                                                ReminderScheduler.cancelTaskReminder(this@MainActivity, taskId)
                                            }
                                        },
                                        onMoveTask = { index, delta ->
                                            scope.launch {
                                                val target = index + delta
                                                if (target in tasks.indices) {
                                                    val a = tasks[index]
                                                    val b = tasks[target]
                                                    app.database.taskDao().upsertAll(
                                                        listOf(
                                                            a.copy(orderIndex = target),
                                                            b.copy(orderIndex = index)
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    )
                                }

                                BottomSection.NOTES -> {
                                    NotesScreen(
                                        notes = notes,
                                        use24HourFormat = prefs.use24HourFormat,
                                        onSaveNote = { note ->
                                            scope.launch { app.database.noteDao().upsertNote(note) }
                                        },
                                        onDeleteNote = { noteId ->
                                            scope.launch { app.database.noteDao().deleteNote(noteId) }
                                        }
                                    )
                                }

                                BottomSection.NAMAZ -> {
                                    NamazScreen(
                                        todayRecord = currentNamazRecord,
                                        allRecords = allNamazRecords,
                                        onTogglePrayer = { prayerKey ->
                                            scope.launch {
                                                app.database.namazDao().upsertNamazRecord(
                                                    currentNamazRecord.togglePrayer(prayerKey)
                                                )
                                            }
                                        },
                                        onResetToday = {
                                            scope.launch {
                                                app.database.namazDao().upsertNamazRecord(
                                                    NamazRecordEntity(
                                                        date = todayStr,
                                                        updatedAt = LocalDateTime.now().toString()
                                                    )
                                                )
                                            }
                                        }
                                    )
                                }

                                BottomSection.SETTINGS -> {
                                    SettingsScreen(
                                        prefs = prefs,
                                        onUpdatePrefs = { transform ->
                                            scope.launch {
                                                app.preferencesRepository.updatePreferences(transform)
                                                val updatedPrefs = transform(prefs)
                                                ReminderScheduler.schedulePrayerReminders(this@MainActivity, updatedPrefs)
                                            }
                                        },
                                        onExportBackupToUri = { uri ->
                                            scope.launch {
                                                val ok = exportBackupJson(app, uri)
                                                Toast.makeText(
                                                    this@MainActivity,
                                                    if (ok) "Backup exported successfully" else "Backup export failed",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        },
                                        onImportBackupFromUri = { uri ->
                                            scope.launch {
                                                val ok = importBackupJson(app, uri)
                                                Toast.makeText(
                                                    this@MainActivity,
                                                    if (ok) "Backup restored successfully" else "Invalid backup file",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        },
                                        onClearSelectedData = { cTasks, cHist, cNotes, cNamaz ->
                                            scope.launch {
                                                if (cTasks) app.database.taskDao().clearAllTasks()
                                                if (cHist) app.database.dailyHistoryDao().clearAllHistory()
                                                if (cNotes) app.database.noteDao().clearAllNotes()
                                                if (cNamaz) {
                                                    app.database.namazDao().clearAllNamaz()
                                                    app.database.namazDao().insertIfAbsent(
                                                        NamazRecordEntity(
                                                            date = todayStr,
                                                            updatedAt = LocalDateTime.now().toString()
                                                        )
                                                    )
                                                }
                                                Toast.makeText(this@MainActivity, "Selected data cleared", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        onResetSettingsOnly = {
                                            scope.launch {
                                                app.preferencesRepository.resetSettingsOnly()
                                                Toast.makeText(this@MainActivity, "Settings reset to defaults", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun exportBackupJson(app: DailyPrimeApplication, uri: Uri): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                val root = JSONObject()
                root.put("app", "Daily Prime")
                root.put("exportedAt", LocalDateTime.now().toString())

                val tasksArr = JSONArray()
                app.database.taskDao().getAllTasks().forEach { t ->
                    tasksArr.put(JSONObject().apply {
                        put("id", t.id)
                        put("title", t.title)
                        put("description", t.description)
                        put("startTime", t.startTime)
                        put("endTime", t.endTime)
                        put("date", t.date)
                        put("categoryId", t.categoryId)
                        put("priority", t.priority)
                        put("reminderEnabled", t.reminderEnabled)
                        put("reminderTime", t.reminderTime)
                        put("reminderMessage", t.reminderMessage ?: "")
                        put("repeatType", t.repeatType)
                        put("repeatDaysCsv", t.repeatDaysCsv)
                        put("completed", t.completed)
                        put("completedAt", t.completedAt ?: "")
                        put("orderIndex", t.orderIndex)
                    })
                }
                root.put("tasks", tasksArr)

                val notesArr = JSONArray()
                app.database.noteDao().getAllNotes().forEach { n ->
                    notesArr.put(JSONObject().apply {
                        put("id", n.id)
                        put("title", n.title)
                        put("content", n.content)
                        put("category", n.category)
                        put("colorHex", n.colorHex)
                        put("isPinned", n.isPinned)
                        put("date", n.date)
                        put("time", n.time)
                        put("createdAt", n.createdAt)
                        put("updatedAt", n.updatedAt)
                    })
                }
                root.put("notes", notesArr)

                val namazArr = JSONArray()
                app.database.namazDao().getAllNamazRecords().forEach { r ->
                    namazArr.put(JSONObject().apply {
                        put("date", r.date)
                        put("fajrDone", r.fajrDone)
                        put("fajrTime", r.fajrTime ?: "")
                        put("dhuhrDone", r.dhuhrDone)
                        put("dhuhrTime", r.dhuhrTime ?: "")
                        put("asrDone", r.asrDone)
                        put("asrTime", r.asrTime ?: "")
                        put("maghribDone", r.maghribDone)
                        put("maghribTime", r.maghribTime ?: "")
                        put("ishaDone", r.ishaDone)
                        put("ishaTime", r.ishaTime ?: "")
                        put("updatedAt", r.updatedAt)
                    })
                }
                root.put("namazRecords", namazArr)

                contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(root.toString(2).toByteArray(Charsets.UTF_8))
                }
                true
            }.getOrDefault(false)
        }

    private suspend fun importBackupJson(app: DailyPrimeApplication, uri: Uri): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                val text = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: return@withContext false
                val root = JSONObject(text)

                val tasksArr = root.optJSONArray("tasks")
                if (tasksArr != null) {
                    val list = (0 until tasksArr.length()).map { i ->
                        val o = tasksArr.getJSONObject(i)
                        TaskEntity(
                            id = o.getString("id"),
                            title = o.getString("title"),
                            description = o.optString("description", ""),
                            startTime = o.optString("startTime", "08:00"),
                            endTime = o.optString("endTime", "08:30"),
                            date = o.optString("date", LocalDate.now().toString()),
                            categoryId = o.optString("categoryId", "personal"),
                            priority = o.optString("priority", "medium"),
                            reminderEnabled = o.optBoolean("reminderEnabled", true),
                            reminderTime = o.optString("reminderTime", "08:00"),
                            reminderMessage = o.optString("reminderMessage").ifBlank { null },
                            repeatType = o.optString("repeatType", "daily"),
                            repeatDaysCsv = o.optString("repeatDaysCsv", ""),
                            completed = o.optBoolean("completed", false),
                            completedAt = o.optString("completedAt").ifBlank { null },
                            orderIndex = o.optInt("orderIndex", i),
                            templateId = null
                        )
                    }
                    app.database.taskDao().upsertAll(list)
                }

                val notesArr = root.optJSONArray("notes")
                if (notesArr != null) {
                    val list = (0 until notesArr.length()).map { i ->
                        val o = notesArr.getJSONObject(i)
                        NoteEntity(
                            id = o.getString("id"),
                            title = o.optString("title", "Untitled"),
                            content = o.optString("content", ""),
                            category = o.optString("category", "Personal"),
                            colorHex = o.optString("colorHex", "#ECFDF5"),
                            isPinned = o.optBoolean("isPinned", false),
                            date = o.optString("date", LocalDate.now().toString()),
                            time = o.optString("time", "09:00"),
                            createdAt = o.optString("createdAt", LocalDateTime.now().toString()),
                            updatedAt = o.optString("updatedAt", LocalDateTime.now().toString())
                        )
                    }
                    app.database.noteDao().upsertAllNotes(list)
                }

                val namazArr = root.optJSONArray("namazRecords")
                if (namazArr != null) {
                    val list = (0 until namazArr.length()).map { i ->
                        val o = namazArr.getJSONObject(i)
                        NamazRecordEntity(
                            date = o.getString("date"),
                            fajrDone = o.optBoolean("fajrDone", false),
                            fajrTime = o.optString("fajrTime").ifBlank { null },
                            dhuhrDone = o.optBoolean("dhuhrDone", false),
                            dhuhrTime = o.optString("dhuhrTime").ifBlank { null },
                            asrDone = o.optBoolean("asrDone", false),
                            asrTime = o.optString("asrTime").ifBlank { null },
                            maghribDone = o.optBoolean("maghribDone", false),
                            maghribTime = o.optString("maghribTime").ifBlank { null },
                            ishaDone = o.optBoolean("ishaDone", false),
                            ishaTime = o.optString("ishaTime").ifBlank { null },
                            updatedAt = o.optString("updatedAt", LocalDateTime.now().toString())
                        )
                    }
                    app.database.namazDao().upsertAllNamaz(list)
                }
                true
            }.getOrDefault(false)
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyPrimeHomeScreen(
    prefs: UserPreferences,
    todayStr: String,
    tasks: List<TaskEntity>,
    onOpenDashboard: () -> Unit,
    onToggleTask: (TaskEntity) -> Unit,
    onSaveTask: (TaskEntity) -> Unit,
    onDeleteTask: (String) -> Unit,
    onMoveTask: (Int, Int) -> Unit
) {
    val context = LocalContext.current
    var editingTask by remember { mutableStateOf<TaskEntity?>(null) }
    var showTaskDialog by remember { mutableStateOf(false) }

    val completedCount = tasks.count { it.completed }
    val progressPct = if (tasks.isEmpty()) 0 else (completedCount * 100) / tasks.size

    val greeting = remember(prefs.customGreeting, prefs.language) {
        if (prefs.customGreeting.isNotBlank()) {
            prefs.customGreeting
        } else {
            val h = LocalTime.now().hour
            when (prefs.language) {
                "bn" -> if (h < 12) "শুভ সকাল!" else if (h < 17) "শুভ অপরাহ্ন!" else "শুভ সন্ধ্যা!"
                "ar" -> if (h < 12) "صباح الخير!" else if (h < 17) "طاب مساؤك!" else "مساء الخير!"
                else -> if (h < 12) "Good Morning!" else if (h < 17) "Good Afternoon!" else "Good Evening!"
            }
        }
    }

    val quoteText = remember(prefs.customQuote, todayStr) {
        if (prefs.customQuote.isNotBlank()) prefs.customQuote
        else HOME_QUOTES[(LocalDate.now().dayOfYear) % HOME_QUOTES.size]
    }

    val customBitmap = remember(prefs.backgroundImageUri) {
        if (prefs.backgroundImageUri.isBlank()) null
        else runCatching {
            val uri = Uri.parse(prefs.backgroundImageUri)
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)?.asImageBitmap()
            }
        }.getOrNull()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = prefs.appDisplayName.ifBlank { "Daily Prime" },
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onOpenDashboard) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = "Productivity Dashboard",
                            tint = EmeraldPrimary
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingTask = null
                    showTaskDialog = true
                },
                containerColor = EmeraldPrimary,
                contentColor = GoldLight
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 92.dp)
        ) {
            // Premium Hero Banner with Default Couple Photo or Custom Gallery Image
            item {
                val bannerHeight = if (prefs.compactHomeLayout) 185.dp else 235.dp
                Card(
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .height(bannerHeight)
                            .fillMaxWidth()
                            .background(Brush.linearGradient(listOf(EmeraldPrimary, EmeraldDeep)))
                    ) {
                        if (!prefs.imageRemoved) {
                            val colorMatrix = ColorMatrix().apply {
                                setToScale(prefs.imageBrightness, prefs.imageBrightness, prefs.imageBrightness, 1f)
                            }
                            if (customBitmap != null) {
                                Image(
                                    bitmap = customBitmap,
                                    contentDescription = "Custom Home Banner Photo",
                                    contentScale = ContentScale.Crop,
                                    colorFilter = ColorFilter.colorMatrix(colorMatrix),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .alpha(prefs.imageOpacity)
                                )
                            } else {
                                Image(
                                    painter = painterResource(id = R.drawable.couple_theme),
                                    contentDescription = "Personal Couple Theme Banner",
                                    contentScale = ContentScale.Crop,
                                    colorFilter = ColorFilter.colorMatrix(colorMatrix),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .alpha(prefs.imageOpacity)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Black.copy(alpha = 0.15f),
                                                EmeraldDeep.copy(alpha = 0.88f)
                                            )
                                        )
                                    )
                            )
                        }

                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp)
                        ) {
                            Text(
                                text = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy")),
                                color = GoldLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "$greeting ${prefs.userName}".trim(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            if (prefs.showQuoteOnHome) {
                                Text(
                                    text = "\"$quoteText\"",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.88f),
                                    maxLines = 2
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "$completedCount/${tasks.size} tasks completed",
                                    color = Color(0xFFD1FAE5),
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "$progressPct%",
                                    color = GoldLight,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { progressPct / 100f },
                                color = GoldLight,
                                trackColor = Color.White.copy(alpha = 0.25f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                        }
                    }
                }
            }

            // Dashboard Quick Card
            item {
                OutlinedButton(
                    onClick = onOpenDashboard,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.BarChart, contentDescription = null, tint = EmeraldPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open Productivity Dashboard & Charts", fontWeight = FontWeight.SemiBold)
                }
            }

            // Today's Routine Schedule
            item {
                Text(
                    text = "Today's Routine Schedule",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            itemsIndexed(tasks, key = { _, task -> task.id }) { index, task ->
                val startDisplay = formatTaskTime(task.startTime, prefs.use24HourFormat)
                val endDisplay = formatTaskTime(task.endTime, prefs.use24HourFormat)

                Card(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { onToggleTask(task) }) {
                            Icon(
                                imageVector = if (task.completed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = "Toggle Complete",
                                tint = if (task.completed) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = task.title,
                                fontWeight = FontWeight.SemiBold,
                                textDecoration = if (task.completed) TextDecoration.LineThrough else null
                            )
                            if (task.description.isNotBlank()) {
                                Text(
                                    text = task.description,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                            Text(
                                text = "$startDisplay - $endDisplay · ${task.priority.uppercase()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = EmeraldPrimary
                            )
                        }
                        if (index > 0) {
                            IconButton(onClick = { onMoveTask(index, -1) }) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", modifier = Modifier.size(18.dp))
                            }
                        }
                        if (index < tasks.lastIndex) {
                            IconButton(onClick = { onMoveTask(index, 1) }) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", modifier = Modifier.size(18.dp))
                            }
                        }
                        IconButton(onClick = {
                            editingTask = task
                            showTaskDialog = true
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Task", modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = { onDeleteTask(task.id) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete Task",
                                tint = Color(0xFFB91C1C),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showTaskDialog) {
        val existing = editingTask
        var title by remember(existing) { mutableStateOf(existing?.title ?: "") }
        var desc by remember(existing) { mutableStateOf(existing?.description ?: "") }
        var start by remember(existing) { mutableStateOf(existing?.startTime ?: "08:00") }
        var end by remember(existing) { mutableStateOf(existing?.endTime ?: "08:30") }
        var priority by remember(existing) { mutableStateOf(existing?.priority ?: "medium") }
        var reminderEnabled by remember(existing) { mutableStateOf(existing?.reminderEnabled ?: true) }

        AlertDialog(
            onDismissRequest = { showTaskDialog = false },
            title = { Text(if (existing != null) "Edit Routine Task" else "New Routine Task") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Task Name") }, singleLine = true)
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, maxLines = 2)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = start, onValueChange = { start = it }, label = { Text("Start HH:MM") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = end, onValueChange = { end = it }, label = { Text("End HH:MM") }, modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("low", "medium", "high").forEach { p ->
                            FilterChip(
                                selected = priority == p,
                                onClick = { priority = p },
                                label = { Text(p.uppercase(), fontSize = 11.sp) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Reminder Notification", fontSize = 13.sp)
                        Switch(checked = reminderEnabled, onCheckedChange = { reminderEnabled = it })
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (title.isNotBlank()) {
                        val saved = TaskEntity(
                            id = existing?.id ?: UUID.randomUUID().toString(),
                            title = title.trim(),
                            description = desc.trim(),
                            startTime = start.trim().ifBlank { "08:00" },
                            endTime = end.trim().ifBlank { "08:30" },
                            date = existing?.date ?: todayStr,
                            categoryId = existing?.categoryId ?: "personal",
                            priority = priority,
                            reminderEnabled = reminderEnabled,
                            reminderTime = start.trim().ifBlank { "08:00" },
                            reminderMessage = "Time for ${title.trim()}",
                            repeatType = existing?.repeatType ?: "daily",
                            repeatDaysCsv = existing?.repeatDaysCsv ?: "",
                            completed = existing?.completed ?: false,
                            completedAt = existing?.completedAt,
                            orderIndex = existing?.orderIndex ?: tasks.size,
                            templateId = existing?.templateId
                        )
                        onSaveTask(saved)
                        showTaskDialog = false
                    }
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTaskDialog = false }) { Text("Cancel") }
            }
        )
    }
}

private fun formatTaskTime(time24: String, use24Hour: Boolean): String {
    if (use24Hour) return time24
    return runCatching {
        LocalTime.parse(time24, DateTimeFormatter.ofPattern("H:mm"))
            .format(DateTimeFormatter.ofPattern("hh:mm a"))
    }.getOrDefault(time24)
}
