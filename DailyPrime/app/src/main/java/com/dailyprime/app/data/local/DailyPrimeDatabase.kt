package com.dailyprime.app.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM routine_tasks WHERE date = :date ORDER BY orderIndex ASC, startTime ASC")
    fun observeTasksForDate(date: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM routine_tasks ORDER BY date DESC, orderIndex ASC")
    suspend fun getAllTasks(): List<TaskEntity>

    @Query("SELECT * FROM routine_tasks WHERE date = :date ORDER BY orderIndex ASC, startTime ASC")
    suspend fun getTasksForDate(date: String): List<TaskEntity>

    @Query("SELECT * FROM routine_tasks WHERE repeatType != 'none'")
    suspend fun getRecurringTemplates(): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTask(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(tasks: List<TaskEntity>)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("DELETE FROM routine_tasks WHERE id = :taskId")
    suspend fun deleteTask(taskId: String)

    @Query("DELETE FROM routine_tasks")
    suspend fun clearAllTasks()

    @Query("UPDATE routine_tasks SET categoryId = :fallbackId WHERE categoryId = :deletedCategoryId")
    suspend fun reassignCategory(deletedCategoryId: String, fallbackId: String)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY isDefault DESC, name ASC")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories")
    suspend fun getAllCategories(): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategory(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(categories: List<CategoryEntity>)

    @Query("DELETE FROM categories WHERE id = :categoryId")
    suspend fun deleteCategory(categoryId: String)
}

@Dao
interface DailyHistoryDao {
    @Query("SELECT * FROM daily_history ORDER BY date DESC")
    fun observeAllHistory(): Flow<List<DailyHistoryEntity>>

    @Query("SELECT * FROM daily_history ORDER BY date DESC")
    suspend fun getAllHistory(): List<DailyHistoryEntity>

    @Query("SELECT * FROM daily_history WHERE date = :date LIMIT 1")
    suspend fun getHistoryForDate(date: String): DailyHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertHistoryIfAbsent(record: DailyHistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAllHistory(records: List<DailyHistoryEntity>)

    @Query("DELETE FROM daily_history WHERE date = :date")
    suspend fun deleteHistoryForDate(date: String)

    @Query("DELETE FROM daily_history")
    suspend fun clearAllHistory()
}

@Dao
interface NamazDao {
    @Query("SELECT * FROM namaz_records WHERE date = :date LIMIT 1")
    fun observeNamazRecordForDate(date: String): Flow<NamazRecordEntity?>

    @Query("SELECT * FROM namaz_records ORDER BY date DESC")
    fun observeAllNamazRecords(): Flow<List<NamazRecordEntity>>

    @Query("SELECT * FROM namaz_records ORDER BY date DESC")
    suspend fun getAllNamazRecords(): List<NamazRecordEntity>

    @Query("SELECT * FROM namaz_records WHERE date = :date LIMIT 1")
    suspend fun getNamazRecordForDate(date: String): NamazRecordEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(record: NamazRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNamazRecord(record: NamazRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAllNamaz(records: List<NamazRecordEntity>)

    @Query("DELETE FROM namaz_records")
    suspend fun clearAllNamaz()
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY isPinned DESC, updatedAt DESC")
    fun observeAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes ORDER BY isPinned DESC, updatedAt DESC")
    suspend fun getAllNotes(): List<NoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNote(note: NoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAllNotes(notes: List<NoteEntity>)

    @Query("DELETE FROM notes WHERE id = :noteId")
    suspend fun deleteNote(noteId: String)

    @Query("DELETE FROM notes")
    suspend fun clearAllNotes()
}

@Database(
    entities = [
        TaskEntity::class,
        CategoryEntity::class,
        DailyHistoryEntity::class,
        NamazRecordEntity::class,
        NoteEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class DailyPrimeDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun categoryDao(): CategoryDao
    abstract fun dailyHistoryDao(): DailyHistoryDao
    abstract fun namazDao(): NamazDao
    abstract fun noteDao(): NoteDao
}
