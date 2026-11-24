package com.dailyprime.app

import android.app.Application
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.dailyprime.app.data.local.CategoryEntity
import com.dailyprime.app.data.local.DailyPrimeDatabase
import com.dailyprime.app.data.local.NamazRecordEntity
import com.dailyprime.app.data.local.NoteEntity
import com.dailyprime.app.data.local.TaskEntity
import com.dailyprime.app.data.preferences.PreferencesRepository
import com.dailyprime.app.notifications.ReminderScheduler
import com.dailyprime.app.worker.DailyResetWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class DailyPrimeApplication : Application() {
    lateinit var database: DailyPrimeDatabase
        private set

    lateinit var preferencesRepository: PreferencesRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val migration1To2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `namaz_records` (
                        `date` TEXT NOT NULL,
                        `fajrDone` INTEGER NOT NULL DEFAULT 0,
                        `fajrTime` TEXT,
                        `dhuhrDone` INTEGER NOT NULL DEFAULT 0,
                        `dhuhrTime` TEXT,
                        `asrDone` INTEGER NOT NULL DEFAULT 0,
                        `asrTime` TEXT,
                        `maghribDone` INTEGER NOT NULL DEFAULT 0,
                        `maghribTime` TEXT,
                        `ishaDone` INTEGER NOT NULL DEFAULT 0,
                        `ishaTime` TEXT,
                        `updatedAt` TEXT NOT NULL,
                        PRIMARY KEY(`date`)
                    )
                    """.trimIndent()
                )
            }
        }

        val migration2To3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `notes` (
                        `id` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `content` TEXT NOT NULL,
                        `category` TEXT NOT NULL DEFAULT 'Personal',
                        `colorHex` TEXT NOT NULL DEFAULT '#ECFDF5',
                        `isPinned` INTEGER NOT NULL DEFAULT 0,
                        `date` TEXT NOT NULL,
                        `time` TEXT NOT NULL,
                        `createdAt` TEXT NOT NULL,
                        `updatedAt` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        database = Room.databaseBuilder(
            applicationContext,
            DailyPrimeDatabase::class.java,
            "daily_prime.db"
        )
            .addMigrations(migration1To2, migration2To3)
            .build()

        preferencesRepository = PreferencesRepository(applicationContext)
        ReminderScheduler.createNotificationChannel(applicationContext)

        CoroutineScope(Dispatchers.IO).launch {
            seedDefaultsIfFirstLaunch()
        }

        val dailyResetRequest = PeriodicWorkRequestBuilder<DailyResetWorker>(1, TimeUnit.HOURS)
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "daily_prime_rollover",
            ExistingPeriodicWorkPolicy.KEEP,
            dailyResetRequest
        )
    }

    private suspend fun seedDefaultsIfFirstLaunch() {
        val prefs = preferencesRepository.preferencesFlow.first()
        val todayStr = LocalDate.now().toString()
        val nowIso = LocalDateTime.now().toString()

        database.namazDao().insertIfAbsent(
            NamazRecordEntity(date = todayStr, updatedAt = nowIso)
        )

        if (database.categoryDao().getAllCategories().isEmpty()) {
            database.categoryDao().upsertAll(
                listOf(
                    CategoryEntity("personal", "Personal", "#064E3B", true),
                    CategoryEntity("health", "Health", "#15803D", true),
                    CategoryEntity("study", "Study", "#2563EB", true),
                    CategoryEntity("work", "Work", "#0F766E", true),
                    CategoryEntity("meals", "Meals", "#B45309", true),
                    CategoryEntity("reading", "Reading", "#6D28D9", true)
                )
            )
        }

        if (!prefs.defaultSeedApplied && database.taskDao().getAllTasks().isEmpty()) {
            val defaultTasks = listOf(
                Triple("Wake Up", "07:00", "07:30") to ("personal" to "high"),
                Triple("Morning Exercise", "07:30", "08:00") to ("health" to "high"),
                Triple("Drink Water", "08:00", "08:15") to ("health" to "medium"),
                Triple("Breakfast", "08:30", "09:00") to ("meals" to "medium"),
                Triple("Study / Work", "09:00", "11:00") to ("study" to "high"),
                Triple("Personal Development", "11:00", "12:30") to ("personal" to "high"),
                Triple("Lunch", "13:00", "14:00") to ("meals" to "medium"),
                Triple("Afternoon Rest", "14:00", "15:00") to ("personal" to "low"),
                Triple("Evening Exercise", "17:00", "17:45") to ("health" to "medium"),
                Triple("Dinner", "20:00", "20:45") to ("meals" to "medium"),
                Triple("Reading", "21:00", "21:45") to ("reading" to "medium"),
                Triple("Sleep", "22:30", "23:00") to ("personal" to "high")
            ).mapIndexed { index, pair ->
                val (times, meta) = pair
                TaskEntity(
                    id = "default_task_$index",
                    title = times.first,
                    description = "Sample routine block — edit or customize freely",
                    startTime = times.second,
                    endTime = times.third,
                    date = todayStr,
                    categoryId = meta.first,
                    priority = meta.second,
                    reminderEnabled = times.first != "Afternoon Rest",
                    reminderTime = times.second,
                    reminderMessage = "Time for ${times.first}",
                    repeatType = "daily",
                    repeatDaysCsv = "",
                    completed = false,
                    completedAt = null,
                    orderIndex = index,
                    templateId = null
                )
            }
            database.taskDao().upsertAll(defaultTasks)

            if (database.noteDao().getAllNotes().isEmpty()) {
                val timeNow = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
                database.noteDao().upsertNote(
                    NoteEntity(
                        id = "starter_note_1",
                        title = "Welcome to Daily Prime Notes",
                        content = "Capture ideas, study notes, goals, and gratitude reflections here. Notes auto-save locally on your phone.",
                        category = "Personal",
                        colorHex = "#ECFDF5",
                        isPinned = true,
                        date = todayStr,
                        time = timeNow,
                        createdAt = nowIso,
                        updatedAt = nowIso
                    )
                )
            }
            preferencesRepository.markDefaultSeedApplied()
        }

        if (prefs.lastActiveDate.isBlank()) {
            preferencesRepository.setLastActiveDate(todayStr)
        }
        ReminderScheduler.schedulePrayerReminders(applicationContext, prefs)
    }
}
