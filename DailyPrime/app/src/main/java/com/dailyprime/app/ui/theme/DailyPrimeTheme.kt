package com.dailyprime.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val EmeraldPrimary = Color(0xFF064E3B)
val EmeraldDeep = Color(0xFF03291F)
val GoldAccent = Color(0xFFD4AF37)
val GoldLight = Color(0xFFF3C853)

fun parseHexColorOrDefault(hex: String, fallback: Color): Color {
    val cleaned = hex.trim()
    if (cleaned.isEmpty()) return fallback
    return try {
        val parsed = android.graphics.Color.parseColor(
            if (cleaned.startsWith("#")) cleaned else "#$cleaned"
        )
        Color(parsed)
    } catch (_: IllegalArgumentException) {
        fallback
    }
}

@Composable
fun DailyPrimeTheme(
    themeMode: String = "system",
    primaryHex: String = "#064E3B",
    accentHex: String = "#D4AF37",
    backgroundHex: String = "",
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }

    val primary = parseHexColorOrDefault(primaryHex, EmeraldPrimary)
    val accent = parseHexColorOrDefault(accentHex, GoldAccent)

    val lightScheme = lightColorScheme(
        primary = primary,
        onPrimary = Color.White,
        secondary = accent,
        onSecondary = EmeraldDeep,
        background = parseHexColorOrDefault(backgroundHex, Color(0xFFF4F7F5)),
        surface = Color(0xFFFFFFFF),
        onBackground = Color(0xFF0F1F1A),
        onSurface = Color(0xFF0F1F1A),
        surfaceVariant = Color(0xFFE6EFEA),
        onSurfaceVariant = Color(0xFF4B6358)
    )

    val darkScheme = darkColorScheme(
        primary = Color(0xFF10B981),
        onPrimary = EmeraldDeep,
        secondary = GoldLight,
        onSecondary = EmeraldDeep,
        background = Color(0xFF0B1210),
        surface = Color(0xFF111B18),
        onBackground = Color(0xFFECFDF5),
        onSurface = Color(0xFFECFDF5),
        surfaceVariant = Color(0xFF172621),
        onSurfaceVariant = Color(0xFF9CB8AC)
    )

    MaterialTheme(
        colorScheme = if (isDark) darkScheme else lightScheme,
        content = content
    )
}
