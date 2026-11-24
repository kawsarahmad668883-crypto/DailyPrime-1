package com.dailyprime.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "routine_tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val startTime: String,
    val endTime: String,
    val date: String,
    val categoryId: String,
    val priority: String,
    val reminderEnabled: Boolean,
    val reminderTime: String,
    val reminderMessage: String?,
    val repeatType: String,
    val repeatDaysCsv: String,
    val completed: Boolean,
    val completedAt: String?,
    val orderIndex: Int,
    val templateId: String?
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val colorHex: String,
    val isDefault: Boolean
)

@Entity(tableName = "daily_history")
data class DailyHistoryEntity(
    @PrimaryKey val date: String,
    val totalTasks: Int,
    val completedTasks: Int,
    val incompleteTasks: Int,
    val missedTasks: Int,
    val completionPercentage: Int,
    val tasksSnapshotJson: String,
    val savedAt: String
)

@Entity(tableName = "namaz_records")
data class NamazRecordEntity(
    @PrimaryKey val date: String,
    val fajrDone: Boolean = false,
    val fajrTime: String? = null,
    val dhuhrDone: Boolean = false,
    val dhuhrTime: String? = null,
    val asrDone: Boolean = false,
    val asrTime: String? = null,
    val maghribDone: Boolean = false,
    val maghribTime: String? = null,
    val ishaDone: Boolean = false,
    val ishaTime: String? = null,
    val updatedAt: String
)

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String,
    val title: String,
    val content: String,
    val category: String = "Personal",
    val colorHex: String = "#ECFDF5",
    val isPinned: Boolean = false,
    val date: String,
    val time: String,
    val createdAt: String,
    val updatedAt: String
)
