package com.dailyprime.app.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.dailyprime.app.DailyPrimeApplication
import com.dailyprime.app.data.local.TaskEntity
import com.dailyprime.app.data.preferences.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

object ReminderScheduler {
    const val CHANNEL_ID = "daily_prime_reminders"
    const val SILENT_CHANNEL_ID = "daily_prime_reminders_silent"

    fun createNotificationChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Routine & Prayer Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Scheduled reminders for Daily Prime tasks and five daily prayers"
        }
        val silentChannel = NotificationChannel(
            SILENT_CHANNEL_ID,
            "Silent Reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Visual reminders without audio alert"
            setSound(null, null)
        }
        manager.createNotificationChannel(channel)
        manager.createNotificationChannel(silentChannel)
    }

    fun scheduleTaskReminder(
        context: Context,
        task: TaskEntity,
        soundMode: String = "chime",
        masterEnabled: Boolean = true
    ) {
        if (!masterEnabled || !task.reminderEnabled || task.completed) {
            cancelReminderById(context, task.id)
            return
        }

        val date = runCatching { LocalDate.parse(task.date) }.getOrNull() ?: return
        val time = runCatching { LocalTime.parse(task.reminderTime.ifBlank { task.startTime }) }.getOrNull() ?: return
        val triggerAtMillis = LocalDateTime.of(date, time)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        if (triggerAtMillis <= System.currentTimeMillis()) return

        scheduleExactOrInexact(
            context = context,
            id = task.id,
            title = task.title,
            message = task.reminderMessage?.takeIf { it.isNotBlank() }
                ?: "Time for ${task.title} (${task.startTime})",
            triggerAtMillis = triggerAtMillis,
            soundMode = soundMode
        )
    }

    fun schedulePrayerReminders(context: Context, prefs: UserPreferences) {
        val prayerPairs = listOf(
            "prayer_fajr" to ("Fajr Prayer (ফজর)" to prefs.fajrTime),
            "prayer_dhuhr" to ("Dhuhr Prayer (যোহর)" to prefs.dhuhrTime),
            "prayer_asr" to ("Asr Prayer (আসর)" to prefs.asrTime),
            "prayer_maghrib" to ("Maghrib Prayer (মাগরিব)" to prefs.maghribTime),
            "prayer_isha" to ("Isha Prayer (এশা)" to prefs.ishaTime)
        )

        if (!prefs.notificationsEnabled || !prefs.prayerRemindersEnabled) {
            prayerPairs.forEach { (id, _) -> cancelReminderById(context, id) }
            return
        }

        val today = LocalDate.now()
        prayerPairs.forEach { (id, info) ->
            val (label, timeStr) = info
            val time = runCatching { LocalTime.parse(timeStr) }.getOrNull() ?: return@forEach
            var targetDateTime = LocalDateTime.of(today, time)
            if (!targetDateTime.isAfter(LocalDateTime.now())) {
                targetDateTime = targetDateTime.plusDays(1)
            }
            val triggerMillis = targetDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            scheduleExactOrInexact(
                context = context,
                id = id,
                title = label,
                message = "Time for $label — Track your prayer in Daily Prime.",
                triggerAtMillis = triggerMillis,
                soundMode = prefs.notificationSound
            )
        }
    }

    private fun scheduleExactOrInexact(
        context: Context,
        id: String,
        title: String,
        message: String,
        triggerAtMillis: Long,
        soundMode: String
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("task_id", id)
            putExtra("task_title", title)
            putExtra("task_message", message)
            putExtra("sound_mode", soundMode)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    fun cancelReminderById(context: Context, id: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun cancelTaskReminder(context: Context, taskId: String) = cancelReminderById(context, taskId)
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra("task_id") ?: return
        val title = intent.getStringExtra("task_title") ?: "Daily Prime Reminder"
        val message = intent.getStringExtra("task_message") ?: "Time for your routine block."
        val soundMode = intent.getStringExtra("sound_mode") ?: "chime"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val channelId = if (soundMode == "silent") {
            ReminderScheduler.SILENT_CHANNEL_ID
        } else {
            ReminderScheduler.CHANNEL_ID
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(taskId.hashCode(), notification)
    }
}

class BootAndUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val app = context.applicationContext as DailyPrimeApplication
                    val prefs = app.preferencesRepository.preferencesFlow.first()
                    val today = LocalDate.now().toString()
                    val tasks = app.database.taskDao().getTasksForDate(today)
                    tasks.forEach {
                        ReminderScheduler.scheduleTaskReminder(
                            context,
                            it,
                            soundMode = prefs.notificationSound,
                            masterEnabled = prefs.notificationsEnabled && prefs.taskRemindersEnabled
                        )
                    }
                    ReminderScheduler.schedulePrayerReminders(context, prefs)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
