package com.dailyprime.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "daily_prime_prefs")

data class UserPreferences(
    val appDisplayName: String = "Daily Prime",
    val userName: String = "Kawsar",
    val customGreeting: String = "",
    val customQuote: String = "",
    val themeMode: String = "system", // "light" | "dark" | "system"
    val primaryColorHex: String = "#064E3B",
    val accentColorHex: String = "#D4AF37",
    val backgroundColorHex: String = "",
    val backgroundImageUri: String = "",
    val imageRemoved: Boolean = false,
    val imageBrightness: Float = 1.0f,
    val imageOpacity: Float = 1.0f,
    val use24HourFormat: Boolean = false,
    val language: String = "en", // "en" | "bn" | "ar"
    val compactHomeLayout: Boolean = false,
    val showQuoteOnHome: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val taskRemindersEnabled: Boolean = true,
    val prayerRemindersEnabled: Boolean = true,
    val notificationSound: String = "chime", // "chime" | "bell" | "silent"
    val fajrTime: String = "05:00",
    val dhuhrTime: String = "12:30",
    val asrTime: String = "15:45",
    val maghribTime: String = "17:45",
    val ishaTime: String = "19:30",
    val autoMarkMissed: Boolean = true,
    val lastActiveDate: String = "",
    val defaultSeedApplied: Boolean = false
)

class PreferencesRepository(private val context: Context) {
    private object Keys {
        val APP_DISPLAY_NAME = stringPreferencesKey("app_display_name")
        val USER_NAME = stringPreferencesKey("user_name")
        val CUSTOM_GREETING = stringPreferencesKey("custom_greeting")
        val CUSTOM_QUOTE = stringPreferencesKey("custom_quote")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DARK_MODE_LEGACY = booleanPreferencesKey("dark_mode")
        val PRIMARY_COLOR = stringPreferencesKey("primary_color_hex")
        val ACCENT_COLOR = stringPreferencesKey("accent_color_hex")
        val BACKGROUND_COLOR = stringPreferencesKey("background_color_hex")
        val BG_IMAGE_URI = stringPreferencesKey("bg_image_uri")
        val IMAGE_REMOVED = booleanPreferencesKey("image_removed")
        val IMAGE_BRIGHTNESS = floatPreferencesKey("image_brightness")
        val IMAGE_OPACITY = floatPreferencesKey("image_opacity")
        val USE_24H = booleanPreferencesKey("use_24_hour_format")
        val LANGUAGE = stringPreferencesKey("language")
        val COMPACT_HOME = booleanPreferencesKey("compact_home_layout")
        val SHOW_QUOTE = booleanPreferencesKey("show_quote_on_home")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val TASK_REMINDERS = booleanPreferencesKey("task_reminders_enabled")
        val PRAYER_REMINDERS = booleanPreferencesKey("prayer_reminders_enabled")
        val NOTIFICATION_SOUND = stringPreferencesKey("notification_sound")
        val FAJR_TIME = stringPreferencesKey("fajr_time")
        val DHUHR_TIME = stringPreferencesKey("dhuhr_time")
        val ASR_TIME = stringPreferencesKey("asr_time")
        val MAGHRIB_TIME = stringPreferencesKey("maghrib_time")
        val ISHA_TIME = stringPreferencesKey("isha_time")
        val AUTO_MARK_MISSED = booleanPreferencesKey("auto_mark_missed")
        val LAST_ACTIVE_DATE = stringPreferencesKey("last_active_date")
        val DEFAULT_SEED_APPLIED = booleanPreferencesKey("default_seed_applied")
    }

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        val legacyDark = prefs[Keys.DARK_MODE_LEGACY]
        val mode = prefs[Keys.THEME_MODE] ?: if (legacyDark == true) "dark" else "system"
        UserPreferences(
            appDisplayName = prefs[Keys.APP_DISPLAY_NAME] ?: "Daily Prime",
            userName = prefs[Keys.USER_NAME] ?: "Kawsar",
            customGreeting = prefs[Keys.CUSTOM_GREETING] ?: "",
            customQuote = prefs[Keys.CUSTOM_QUOTE] ?: "",
            themeMode = mode,
            primaryColorHex = prefs[Keys.PRIMARY_COLOR] ?: "#064E3B",
            accentColorHex = prefs[Keys.ACCENT_COLOR] ?: "#D4AF37",
            backgroundColorHex = prefs[Keys.BACKGROUND_COLOR] ?: "",
            backgroundImageUri = prefs[Keys.BG_IMAGE_URI] ?: "",
            imageRemoved = prefs[Keys.IMAGE_REMOVED] ?: false,
            imageBrightness = prefs[Keys.IMAGE_BRIGHTNESS] ?: 1.0f,
            imageOpacity = prefs[Keys.IMAGE_OPACITY] ?: 1.0f,
            use24HourFormat = prefs[Keys.USE_24H] ?: false,
            language = prefs[Keys.LANGUAGE] ?: "en",
            compactHomeLayout = prefs[Keys.COMPACT_HOME] ?: false,
            showQuoteOnHome = prefs[Keys.SHOW_QUOTE] ?: true,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
            taskRemindersEnabled = prefs[Keys.TASK_REMINDERS] ?: true,
            prayerRemindersEnabled = prefs[Keys.PRAYER_REMINDERS] ?: true,
            notificationSound = prefs[Keys.NOTIFICATION_SOUND] ?: "chime",
            fajrTime = prefs[Keys.FAJR_TIME] ?: "05:00",
            dhuhrTime = prefs[Keys.DHUHR_TIME] ?: "12:30",
            asrTime = prefs[Keys.ASR_TIME] ?: "15:45",
            maghribTime = prefs[Keys.MAGHRIB_TIME] ?: "17:45",
            ishaTime = prefs[Keys.ISHA_TIME] ?: "19:30",
            autoMarkMissed = prefs[Keys.AUTO_MARK_MISSED] ?: true,
            lastActiveDate = prefs[Keys.LAST_ACTIVE_DATE] ?: "",
            defaultSeedApplied = prefs[Keys.DEFAULT_SEED_APPLIED] ?: false
        )
    }

    suspend fun updatePreferences(transform: (UserPreferences) -> UserPreferences) {
        context.dataStore.edit { prefs ->
            val current = UserPreferences(
                appDisplayName = prefs[Keys.APP_DISPLAY_NAME] ?: "Daily Prime",
                userName = prefs[Keys.USER_NAME] ?: "Kawsar",
                customGreeting = prefs[Keys.CUSTOM_GREETING] ?: "",
                customQuote = prefs[Keys.CUSTOM_QUOTE] ?: "",
                themeMode = prefs[Keys.THEME_MODE] ?: "system",
                primaryColorHex = prefs[Keys.PRIMARY_COLOR] ?: "#064E3B",
                accentColorHex = prefs[Keys.ACCENT_COLOR] ?: "#D4AF37",
                backgroundColorHex = prefs[Keys.BACKGROUND_COLOR] ?: "",
                backgroundImageUri = prefs[Keys.BG_IMAGE_URI] ?: "",
                imageRemoved = prefs[Keys.IMAGE_REMOVED] ?: false,
                imageBrightness = prefs[Keys.IMAGE_BRIGHTNESS] ?: 1.0f,
                imageOpacity = prefs[Keys.IMAGE_OPACITY] ?: 1.0f,
                use24HourFormat = prefs[Keys.USE_24H] ?: false,
                language = prefs[Keys.LANGUAGE] ?: "en",
                compactHomeLayout = prefs[Keys.COMPACT_HOME] ?: false,
                showQuoteOnHome = prefs[Keys.SHOW_QUOTE] ?: true,
                notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
                taskRemindersEnabled = prefs[Keys.TASK_REMINDERS] ?: true,
                prayerRemindersEnabled = prefs[Keys.PRAYER_REMINDERS] ?: true,
                notificationSound = prefs[Keys.NOTIFICATION_SOUND] ?: "chime",
                fajrTime = prefs[Keys.FAJR_TIME] ?: "05:00",
                dhuhrTime = prefs[Keys.DHUHR_TIME] ?: "12:30",
                asrTime = prefs[Keys.ASR_TIME] ?: "15:45",
                maghribTime = prefs[Keys.MAGHRIB_TIME] ?: "17:45",
                ishaTime = prefs[Keys.ISHA_TIME] ?: "19:30",
                autoMarkMissed = prefs[Keys.AUTO_MARK_MISSED] ?: true,
                lastActiveDate = prefs[Keys.LAST_ACTIVE_DATE] ?: "",
                defaultSeedApplied = prefs[Keys.DEFAULT_SEED_APPLIED] ?: false
            )
            val next = transform(current)
            prefs[Keys.APP_DISPLAY_NAME] = next.appDisplayName
            prefs[Keys.USER_NAME] = next.userName
            prefs[Keys.CUSTOM_GREETING] = next.customGreeting
            prefs[Keys.CUSTOM_QUOTE] = next.customQuote
            prefs[Keys.THEME_MODE] = next.themeMode
            prefs[Keys.PRIMARY_COLOR] = next.primaryColorHex
            prefs[Keys.ACCENT_COLOR] = next.accentColorHex
            prefs[Keys.BACKGROUND_COLOR] = next.backgroundColorHex
            prefs[Keys.BG_IMAGE_URI] = next.backgroundImageUri
            prefs[Keys.IMAGE_REMOVED] = next.imageRemoved
            prefs[Keys.IMAGE_BRIGHTNESS] = next.imageBrightness
            prefs[Keys.IMAGE_OPACITY] = next.imageOpacity
            prefs[Keys.USE_24H] = next.use24HourFormat
            prefs[Keys.LANGUAGE] = next.language
            prefs[Keys.COMPACT_HOME] = next.compactHomeLayout
            prefs[Keys.SHOW_QUOTE] = next.showQuoteOnHome
            prefs[Keys.NOTIFICATIONS_ENABLED] = next.notificationsEnabled
            prefs[Keys.TASK_REMINDERS] = next.taskRemindersEnabled
            prefs[Keys.PRAYER_REMINDERS] = next.prayerRemindersEnabled
            prefs[Keys.NOTIFICATION_SOUND] = next.notificationSound
            prefs[Keys.FAJR_TIME] = next.fajrTime
            prefs[Keys.DHUHR_TIME] = next.dhuhrTime
            prefs[Keys.ASR_TIME] = next.asrTime
            prefs[Keys.MAGHRIB_TIME] = next.maghribTime
            prefs[Keys.ISHA_TIME] = next.ishaTime
            prefs[Keys.AUTO_MARK_MISSED] = next.autoMarkMissed
            prefs[Keys.LAST_ACTIVE_DATE] = next.lastActiveDate
            prefs[Keys.DEFAULT_SEED_APPLIED] = next.defaultSeedApplied
        }
    }

    suspend fun setLastActiveDate(date: String) {
        updatePreferences { it.copy(lastActiveDate = date) }
    }

    suspend fun markDefaultSeedApplied() {
        updatePreferences { it.copy(defaultSeedApplied = true) }
    }

    suspend fun resetSettingsOnly() {
        updatePreferences { current ->
            UserPreferences(
                lastActiveDate = current.lastActiveDate,
                defaultSeedApplied = current.defaultSeedApplied
            )
        }
    }
}
