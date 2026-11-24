package com.dailyprime.app.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dailyprime.app.DailyPrimeApplication
import com.dailyprime.app.data.local.DailyHistoryEntity
import com.dailyprime.app.data.local.NamazRecordEntity
import com.dailyprime.app.notifications.ReminderScheduler
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class DailyResetWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as DailyPrimeApplication
        val db = app.database
        val prefs = app.preferencesRepository.preferencesFlow.first()

        val today = LocalDate.now()
        val todayStr = today.toString()
        val previousDate = prefs.lastActiveDate.ifBlank { today.minusDays(1).toString() }

        // Always guarantee today has a fresh unchecked Namaz record without altering previous dates
        db.namazDao().insertIfAbsent(
            NamazRecordEntity(
                date = todayStr,
                updatedAt = LocalDateTime.now().toString()
            )
        )

        if (previousDate < todayStr) {
            val existingHistory = db.dailyHistoryDao().getHistoryForDate(previousDate)
            val prevTasks = db.taskDao().getTasksForDate(previousDate)

            if (existingHistory == null && prevTasks.isNotEmpty()) {
                val completed = prevTasks.count { it.completed }
                val total = prevTasks.size
                val incomplete = total - completed
                val missed = if (prefs.autoMarkMissed) incomplete else 0
                val pct = if (total == 0) 0 else (completed * 100) / total

                db.dailyHistoryDao().insertHistoryIfAbsent(
                    DailyHistoryEntity(
                        date = previousDate,
                        totalTasks = total,
                        completedTasks = completed,
                        incompleteTasks = incomplete,
                        missedTasks = missed,
                        completionPercentage = pct,
                        tasksSnapshotJson = prevTasks.joinToString(prefix = "[", postfix = "]") {
                            """{"id":"${it.id}","title":"${it.title.replace("\"", "\\\"")}","completed":${it.completed}}"""
                        },
                        savedAt = LocalDateTime.now().toString()
                    )
                )
            }

            // Hydrate recurring routines for the new day without duplicating
            val existingToday = db.taskDao().getTasksForDate(todayStr)
            val existingKeys = existingToday.map { it.templateId ?: it.id }.toSet()
            val templates = db.taskDao().getRecurringTemplates()
            val dayOfWeek = today.dayOfWeek.value % 7 // 0=Sun..6=Sat

            val newDayTasks = templates.filter { tpl ->
                val matches = when (tpl.repeatType) {
                    "daily" -> true
                    "weekdays" -> dayOfWeek in 1..5
                    "weekends" -> dayOfWeek == 0 || dayOfWeek == 6
                    "custom" -> tpl.repeatDaysCsv.split(",").mapNotNull { it.toIntOrNull() }.contains(dayOfWeek)
                    else -> false
                }
                matches && !existingKeys.contains(tpl.templateId ?: tpl.id)
            }.mapIndexed { idx, tpl ->
                tpl.copy(
                    id = UUID.randomUUID().toString(),
                    date = todayStr,
                    completed = false,
                    completedAt = null,
                    orderIndex = existingToday.size + idx,
                    templateId = tpl.templateId ?: tpl.id
                )
            }

            if (newDayTasks.isNotEmpty()) {
                db.taskDao().upsertAll(newDayTasks)
                newDayTasks.forEach {
                    ReminderScheduler.scheduleTaskReminder(
                        applicationContext,
                        it,
                        soundMode = prefs.notificationSound,
                        masterEnabled = prefs.notificationsEnabled && prefs.taskRemindersEnabled
                    )
                }
            }

            app.preferencesRepository.setLastActiveDate(todayStr)
        }

        ReminderScheduler.schedulePrayerReminders(applicationContext, prefs)

        return Result.success()
    }
}
