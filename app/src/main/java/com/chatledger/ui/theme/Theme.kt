package com.chatledger.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// 品牌色
val PrimaryGreen = Color(0xFF2D9B6A)
val PrimaryGreenDark = Color(0xFF1B7D4F)
val SecondaryOrange = Color(0xFFF5A623)
val BackgroundLight = Color(0xFFF7F8FA)
val SurfaceLight = Color(0xFFFFFFFF)
val BackgroundDark = Color(0xFF121212)
val SurfaceDark = Color(0xFF1E1E1E)

// 类别颜色
val CategoryColors = mapOf(
    "FOOD" to Color(0xFFFF6B6B),
    "TRANSPORT" to Color(0xFF4ECDC4),
    "SHOPPING" to Color(0xFFFFE66D),
    "ENTERTAINMENT" to Color(0xFFA8E6CF),
    "HOUSING" to Color(0xFF95B8D1),
    "MEDICAL" to Color(0xFFFF8A80),
    "EDUCATION" to Color(0xFF82B1FF),
    "UTILITIES" to Color(0xFFFFCC02),
    "COMMUNICATION" to Color(0xFF7C4DFF),
    "CLOTHING" to Color(0xFFFF80AB),
    "TRAVEL" to Color(0xFF00BFA5),
    "GIFT" to Color(0xFFEA80FC),
    "INVESTMENT" to Color(0xFF69F0AE),
    "INCOME" to Color(0xFF00E676),
    "OTHER" to Color(0xFFBDBDBD)
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8F5E9),
    secondary = SecondaryOrange,
    background = BackgroundLight,
    surface = SurfaceLight,
    onBackground = Color(0xFF1A1A1A),
    onSurface = Color(0xFF1A1A1A)
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1B5E20),
    secondary = SecondaryOrange,
    background = BackgroundDark,
    surface = SurfaceDark,
    onBackground = Color(0xFFE0E0E0),
    onSurface = Color(0xFFE0E0E0)
)

@Composable
fun ChatLedgerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
