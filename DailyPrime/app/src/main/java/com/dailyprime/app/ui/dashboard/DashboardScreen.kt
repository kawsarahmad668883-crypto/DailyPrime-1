package com.dailyprime.app.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailyprime.app.data.local.DailyHistoryEntity
import com.dailyprime.app.data.local.TaskEntity
import com.dailyprime.app.ui.theme.EmeraldDeep
import com.dailyprime.app.ui.theme.EmeraldPrimary
import com.dailyprime.app.ui.theme.GoldAccent
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    todayTasks: List<TaskEntity>,
    history: List<DailyHistoryEntity>,
    onBack: () -> Unit
) {
    val todayCompleted = todayTasks.count { it.completed }
    val todayRemaining = (todayTasks.size - todayCompleted).coerceAtLeast(0)
    val todayPct = if (todayTasks.isEmpty()) 0 else (todayCompleted * 100) / todayTasks.size

    val weeklyDays = remember(history, todayPct) {
        val today = LocalDate.now()
        val map = history.associateBy { it.date }
        (6 downTo 0).map { offset ->
            val d = today.minusDays(offset.toLong())
            val dStr = d.toString()
            val pct = if (offset == 0) todayPct else (map[dStr]?.completionPercentage ?: 0)
            d.dayOfWeek.name.take(3) to pct
        }
    }

    val weeklyRate = remember(weeklyDays) {
        if (weeklyDays.isEmpty()) 0 else weeklyDays.sumOf { it.second } / weeklyDays.size
    }

    val monthlyRate = remember(history, todayPct) {
        val today = LocalDate.now()
        val cutoff = today.minusDays(29).toString()
        val last30 = history.filter { it.date >= cutoff }.map { it.completionPercentage } + todayPct
        if (last30.isEmpty()) 0 else last30.sum() / last30.size
    }

    val streak = remember(history, todayPct) {
        var s = if (todayPct >= 60) 1 else 0
        val sorted = history.sortedByDescending { it.date }
        for (item in sorted) {
            if (item.completionPercentage >= 60) s++ else break
        }
        s
    }

    val bestDay = remember(history) {
        history.maxByOrNull { it.completionPercentage }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Productivity Dashboard", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = EmeraldPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "TODAY'S PRODUCTIVITY",
                            color = GoldAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$todayPct%",
                            color = Color.White,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$todayCompleted completed · $todayRemaining remaining today",
                            color = Color(0xFFD1FAE5),
                            fontSize = 13.sp
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard("Weekly Avg", "$weeklyRate%", "Last 7 days", Modifier.weight(1f))
                    MetricCard("Monthly Avg", "$monthlyRate%", "Last 30 days", Modifier.weight(1f))
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Daily Streak", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("$streak days", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Best Day", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = bestDay?.let { "${it.completionPercentage}% (${it.date})" } ?: "$todayPct% (Today)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Weekly Productivity Chart", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                        ) {
                            val barCount = weeklyDays.size.coerceAtLeast(1)
                            val slotWidth = size.width / barCount
                            val barWidth = slotWidth * 0.52f
                            weeklyDays.forEachIndexed { idx, (_, pct) ->
                                val ratio = (pct / 100f).coerceIn(0.05f, 1f)
                                val barH = size.height * ratio
                                val x = idx * slotWidth + (slotWidth - barWidth) / 2f
                                drawRoundRect(
                                    color = Color(0xFFE6EFEA),
                                    topLeft = Offset(x, 0f),
                                    size = Size(barWidth, size.height),
                                    cornerRadius = CornerRadius(12f, 12f)
                                )
                                drawRoundRect(
                                    color = if (idx == barCount - 1) GoldAccent else EmeraldPrimary,
                                    topLeft = Offset(x, size.height - barH),
                                    size = Size(barWidth, barH),
                                    cornerRadius = CornerRadius(12f, 12f)
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            weeklyDays.forEach { (label, pct) ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("$pct%", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                    Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text("Task Completion History", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            items(todayTasks.filter { it.completed }, key = { it.id }) { task ->
                Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldPrimary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(task.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(
                                text = "Scheduled ${task.startTime}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = task.completedAt?.take(5) ?: "Done",
                            fontSize = 11.sp,
                            color = EmeraldDeep,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(shape = RoundedCornerShape(16.dp), modifier = modifier) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
