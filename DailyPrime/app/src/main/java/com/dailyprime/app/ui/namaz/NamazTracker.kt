package com.dailyprime.app.ui.namaz

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailyprime.app.data.local.NamazRecordEntity
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

enum class PrayerKey(
    val number: Int,
    val english: String,
    val arabic: String,
    val bangla: String
) {
    FAJR(1, "Fajr", "الفجر", "ফজর"),
    DHUHR(2, "Dhuhr", "الظهر", "যোহর"),
    ASR(3, "Asr", "العصر", "আসর"),
    MAGHRIB(4, "Maghrib", "المغرب", "মাগরিব"),
    ISHA(5, "Isha", "العشاء", "এশা")
}

private val EmeraldDeep = Color(0xFF03291F)
private val EmeraldBase = Color(0xFF064E3B)
private val EmeraldCard = Color(0xFF0A3D2F)
private val EmeraldCardDone = Color(0xFF0D5B45)
private val GoldAccent = Color(0xFFF3C853)
private val GoldMuted = Color(0xFFD4AF37)
private val EmeraldTextMuted = Color(0xFFA7D7C5)

fun NamazRecordEntity.completedCount(): Int {
    var count = 0
    if (fajrDone) count++
    if (dhuhrDone) count++
    if (asrDone) count++
    if (maghribDone) count++
    if (ishaDone) count++
    return count
}

fun NamazRecordEntity.isCompleted(key: PrayerKey): Boolean = when (key) {
    PrayerKey.FAJR -> fajrDone
    PrayerKey.DHUHR -> dhuhrDone
    PrayerKey.ASR -> asrDone
    PrayerKey.MAGHRIB -> maghribDone
    PrayerKey.ISHA -> ishaDone
}

fun NamazRecordEntity.completedTime(key: PrayerKey): String? = when (key) {
    PrayerKey.FAJR -> fajrTime
    PrayerKey.DHUHR -> dhuhrTime
    PrayerKey.ASR -> asrTime
    PrayerKey.MAGHRIB -> maghribTime
    PrayerKey.ISHA -> ishaTime
}

fun NamazRecordEntity.togglePrayer(key: PrayerKey): NamazRecordEntity {
    val timeStr = LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a"))
    val nowStamp = LocalDateTime.now().toString()
    return when (key) {
        PrayerKey.FAJR -> copy(fajrDone = !fajrDone, fajrTime = if (!fajrDone) timeStr else null, updatedAt = nowStamp)
        PrayerKey.DHUHR -> copy(dhuhrDone = !dhuhrDone, dhuhrTime = if (!dhuhrDone) timeStr else null, updatedAt = nowStamp)
        PrayerKey.ASR -> copy(asrDone = !asrDone, asrTime = if (!asrDone) timeStr else null, updatedAt = nowStamp)
        PrayerKey.MAGHRIB -> copy(maghribDone = !maghribDone, maghribTime = if (!maghribDone) timeStr else null, updatedAt = nowStamp)
        PrayerKey.ISHA -> copy(ishaDone = !ishaDone, ishaTime = if (!ishaDone) timeStr else null, updatedAt = nowStamp)
    }
}

private val ISLAMIC_QUOTES = listOf(
    "\"Indeed, prayer has been decreed upon the believers a decree of specified times.\" — Qur'an 4:103",
    "\"Establish prayer for My remembrance.\" — Qur'an 20:14",
    "\"And seek help through patience and prayer.\" — Qur'an 2:45",
    "\"The closest a servant comes to his Lord is when he is prostrating.\" — Sahih Muslim"
)

@Composable
fun NamazScreen(
    todayRecord: NamazRecordEntity,
    allRecords: List<NamazRecordEntity>,
    onTogglePrayer: (PrayerKey) -> Unit,
    onResetToday: () -> Unit
) {
    var selectedSubTab by remember { mutableStateOf(0) } // 0 = Today's Prayers, 1 = History & Calendar

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedSubTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = EmeraldBase
        ) {
            Tab(
                selected = selectedSubTab == 0,
                onClick = { selectedSubTab = 0 },
                text = { Text("Today's Prayers", fontWeight = FontWeight.SemiBold) }
            )
            Tab(
                selected = selectedSubTab == 1,
                onClick = { selectedSubTab = 1 },
                text = { Text("History & Calendar", fontWeight = FontWeight.SemiBold) }
            )
        }

        if (selectedSubTab == 0) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
            ) {
                item {
                    NamazTrackerSection(
                        record = todayRecord,
                        onTogglePrayer = onTogglePrayer,
                        onResetToday = onResetToday,
                        onOpenHistory = { selectedSubTab = 1 }
                    )
                }
            }
        } else {
            PrayerHistoryContent(
                records = allRecords,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun NamazTrackerSection(
    record: NamazRecordEntity,
    onTogglePrayer: (PrayerKey) -> Unit,
    onResetToday: () -> Unit,
    onOpenHistory: () -> Unit
) {
    var showResetConfirm by remember { mutableStateOf(false) }
    val completed = record.completedCount()
    val remaining = 5 - completed
    val percent = (completed * 100) / 5
    val animatedProgress by animateFloatAsState(
        targetValue = completed / 5f,
        animationSpec = tween(durationMillis = 500),
        label = "namazProgress"
    )
    val quoteIndex = remember(record.date) {
        (runCatching { LocalDate.parse(record.date).dayOfYear }.getOrDefault(0)) % ISLAMIC_QUOTES.size
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, GoldMuted.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(EmeraldBase, EmeraldDeep)))
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val step = 68.dp.toPx()
                val strokeColor = GoldAccent.copy(alpha = 0.08f)
                var y = step / 2f
                while (y < size.height + step) {
                    var x = step / 2f
                    while (x < size.width + step) {
                        val r = step * 0.28f
                        drawRect(
                            color = strokeColor,
                            topLeft = Offset(x - r, y - r),
                            size = androidx.compose.ui.geometry.Size(r * 2, r * 2),
                            style = Stroke(width = 1.dp.toPx())
                        )
                        val diamond = Path().apply {
                            moveTo(x, y - r * 1.35f)
                            lineTo(x + r * 1.35f, y)
                            lineTo(x, y + r * 1.35f)
                            lineTo(x - r * 1.35f, y)
                            close()
                        }
                        drawPath(diamond, color = strokeColor, style = Stroke(width = 1.dp.toPx()))
                        x += step
                    }
                    y += step
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "NAMAZ TRACKER · صلاة",
                            color = GoldAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.1.sp
                        )
                        Text(
                            text = "Five Daily Prayers",
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = ISLAMIC_QUOTES[quoteIndex],
                            color = EmeraldTextMuted,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(EmeraldCard.copy(alpha = 0.85f))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(76.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawArc(
                                color = Color.White.copy(alpha = 0.14f),
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                            )
                            drawArc(
                                color = GoldAccent,
                                startAngle = -90f,
                                sweepAngle = 360f * animatedProgress,
                                useCenter = false,
                                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                        Text(
                            text = "$percent%",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "$completed/5 Prayers Completed",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$percent% Complete · Total: 5 · Remaining: $remaining",
                            color = EmeraldTextMuted,
                            fontSize = 12.sp
                        )
                    }
                }

                AnimatedVisibility(visible = completed == 5) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = GoldAccent.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.55f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Alhamdulillah! All 5 prayers completed today.",
                            color = GoldAccent,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    PrayerKey.entries.forEach { prayer ->
                        val isDone = record.isCompleted(prayer)
                        val timeText = record.completedTime(prayer)
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isDone) EmeraldCardDone else EmeraldCard,
                            border = BorderStroke(
                                1.dp,
                                if (isDone) GoldAccent.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.12f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onTogglePrayer(prayer) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(if (isDone) GoldAccent else Color.White.copy(alpha = 0.12f))
                                    ) {
                                        if (isDone) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Prayed",
                                                tint = EmeraldDeep,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = "${prayer.number}. ${prayer.english}",
                                                color = Color.White,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 15.sp
                                            )
                                            Text(
                                                text = prayer.bangla,
                                                color = EmeraldTextMuted,
                                                fontSize = 13.sp
                                            )
                                        }
                                        Text(
                                            text = if (isDone && timeText != null) "Prayed · $timeText"
                                            else if (isDone) "Prayed"
                                            else "Pending",
                                            color = if (isDone) GoldAccent else EmeraldTextMuted,
                                            fontSize = 12.sp,
                                            fontWeight = if (isDone) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                    }
                                }

                                Text(
                                    text = prayer.arabic,
                                    color = if (isDone) GoldAccent else Color.White.copy(alpha = 0.85f),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenHistory,
                        border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.55f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldAccent),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Prayer History", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = { showResetConfirm = true },
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset Today", fontSize = 12.sp)
                    }
                }
            }
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset Today's Prayer Checklist?") },
            text = { Text("All 5 prayers for today will be unchecked. Past days' records remain saved permanently.") },
            confirmButton = {
                Button(onClick = {
                    onResetToday()
                    showResetConfirm = false
                }) {
                    Text("Confirm Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerHistoryScreen(
    records: List<NamazRecordEntity>,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Prayer History", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        PrayerHistoryContent(
            records = records,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        )
    }
}

@Composable
fun PrayerHistoryContent(
    records: List<NamazRecordEntity>,
    modifier: Modifier = Modifier
) {
    val today = remember { LocalDate.now() }
    var currentMonth by remember { mutableStateOf(YearMonth.from(today)) }
    var selectedDate by remember { mutableStateOf(today.toString()) }

    val recordMap = remember(records) { records.associateBy { it.date } }

    val weeklyRecords = remember(records) {
        val cutoff = today.minusDays(6).toString()
        records.filter { it.date >= cutoff }
    }
    val monthlyRecords = remember(records) {
        val cutoff = today.minusDays(29).toString()
        records.filter { it.date >= cutoff }
    }
    val weeklyDone = weeklyRecords.sumOf { it.completedCount() }
    val weeklyPossible = maxOf(7, weeklyRecords.size) * 5
    val weeklyPct = if (weeklyPossible == 0) 0 else (weeklyDone * 100) / weeklyPossible

    val monthlyDone = monthlyRecords.sumOf { it.completedCount() }
    val monthlyPossible = maxOf(30, monthlyRecords.size) * 5
    val monthlyPct = if (monthlyPossible == 0) 0 else (monthlyDone * 100) / monthlyPossible

    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = EmeraldBase),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Weekly (7d)", color = GoldAccent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text("$weeklyPct%", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text("$weeklyDone/$weeklyPossible prayers", color = EmeraldTextMuted, fontSize = 12.sp)
                    }
                }
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = EmeraldBase),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Monthly (30d)", color = GoldAccent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text("$monthlyPct%", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text("$monthlyDone/$monthlyPossible prayers", color = EmeraldTextMuted, fontSize = 12.sp)
                    }
                }
            }
        }

        // Monthly Calendar Grid with X/5 badges
        item {
            Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = EmeraldBase)
                            Text(
                                text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row {
                            IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = "Prev Month")
                            }
                            IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                                Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
                            }
                        }
                    }

                    val firstDay = currentMonth.atDay(1)
                    val offset = firstDay.dayOfWeek.value % 7
                    val totalDays = currentMonth.lengthOfMonth()
                    val cells = (0 until offset + totalDays).toList()

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        cells.chunked(7).forEach { week ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                for (i in 0 until 7) {
                                    val cellIdx = week.getOrNull(i)
                                    val dayNum = if (cellIdx != null && cellIdx >= offset) cellIdx - offset + 1 else null
                                    if (dayNum == null) {
                                        Spacer(modifier = Modifier.weight(1f).height(44.dp))
                                    } else {
                                        val dateStr = currentMonth.atDay(dayNum).toString()
                                        val rec = recordMap[dateStr]
                                        val done = rec?.completedCount()
                                        val isSelected = selectedDate == dateStr

                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isSelected) EmeraldBase else MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(44.dp)
                                                .clickable { selectedDate = dateStr }
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Text(
                                                    text = "$dayNum",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                                )
                                                if (done != null) {
                                                    Text(
                                                        text = "$done/5",
                                                        fontSize = 9.sp,
                                                        color = if (isSelected) GoldAccent else EmeraldBase,
                                                        textAlign = TextAlign.Center
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Selected Date Detail
        item {
            val selectedRecord = recordMap[selectedDate]
            Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Selected Date: $selectedDate", fontWeight = FontWeight.Bold)
                    if (selectedRecord == null) {
                        Text(
                            text = "No prayer record saved for this date.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        PrayerKey.entries.forEach { p ->
                            val ok = selectedRecord.isCompleted(p)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${p.english} (${p.bangla})", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                Text(
                                    text = if (ok) "Prayed" else "Missed",
                                    color = if (ok) EmeraldBase else Color(0xFFB91C1C),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        items(records, key = { it.date }) { day ->
            val done = day.completedCount()
            val pct = (done * 100) / 5
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedDate = day.date }
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(day.date, fontWeight = FontWeight.Bold)
                        Text(
                            text = "$done/5 · $pct%",
                            color = if (done == 5) EmeraldBase else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        PrayerKey.entries.forEach { p ->
                            val ok = day.isCompleted(p)
                            Text(
                                text = "${p.english}: ${if (ok) "Prayed" else "Missed"}",
                                fontSize = 11.sp,
                                color = if (ok) EmeraldBase else Color(0xFFB91C1C),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}
