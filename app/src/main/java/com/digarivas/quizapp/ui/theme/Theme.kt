package com.digarivas.quizapp.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.digarivas.quizapp.settings.SettingsManager
import com.digarivas.quizapp.settings.ThemeMode

// Superficies por capas: el fondo y las tarjetas se diferencian por tono,
// sin sombras ni bordes fuertes.
private val DarkColorScheme = darkColorScheme(
    primary = GraphitePrimary,
    onPrimary = GraphiteOnPrimary,
    primaryContainer = GraphitePrimaryContainer,
    onPrimaryContainer = GraphiteOnPrimaryContainer,
    inversePrimary = Color(0xFF5D5F5F),
    secondary = Color(0xFFC7C6C6),
    onSecondary = Color(0xFF303031),
    secondaryContainer = Color(0xFF464747),
    onSecondaryContainer = Color(0xFFB6B5B5),
    tertiary = Mint,
    onTertiary = MintDeep,
    tertiaryContainer = Mint,
    onTertiaryContainer = MintDeep,
    background = Graphite,
    onBackground = GraphiteOn,
    surface = Graphite,
    onSurface = GraphiteOn,
    surfaceVariant = GraphiteHighest,
    onSurfaceVariant = GraphiteOnVariant,
    surfaceTint = Color.Transparent,
    inverseSurface = GraphiteOn,
    inverseOnSurface = Color(0xFF2F2F2F),
    error = SoftRed,
    onError = Color(0xFF690005),
    errorContainer = DeepRed,
    onErrorContainer = PaleRed,
    outline = GraphiteOutline,
    outlineVariant = GraphiteOutlineVariant,
    scrim = Color.Black,
    surfaceBright = Color(0xFF3A3A3A),
    surfaceDim = Graphite,
    surfaceContainer = GraphiteContainer,
    surfaceContainerHigh = GraphiteHigh,
    surfaceContainerHighest = GraphiteHighest,
    surfaceContainerLow = GraphiteLow,
    surfaceContainerLowest = GraphiteLowest
)

private val LightColorScheme = lightColorScheme(
    primary = PaperPrimary,
    onPrimary = PaperOnPrimary,
    primaryContainer = PaperPrimaryContainer,
    onPrimaryContainer = PaperOnPrimaryContainer,
    inversePrimary = Color(0xFFC6C6C7),
    secondary = PaperSecondary,
    onSecondary = Color.White,
    secondaryContainer = PaperSecondaryContainer,
    onSecondaryContainer = PaperOnSecondaryContainer,
    tertiary = MintDeep,
    onTertiary = Color.White,
    tertiaryContainer = Mint,
    onTertiaryContainer = MintDeep,
    background = Paper,
    onBackground = PaperOn,
    surface = Paper,
    onSurface = PaperOn,
    surfaceVariant = PaperHighest,
    onSurfaceVariant = PaperOnVariant,
    surfaceTint = Color.Transparent,
    inverseSurface = PaperOn,
    inverseOnSurface = PaperLow,
    error = PaperError,
    onError = Color.White,
    errorContainer = PaperErrorContainer,
    onErrorContainer = PaperOnErrorContainer,
    outline = PaperOutline,
    outlineVariant = PaperOutlineVariant,
    scrim = Color.Black,
    surfaceBright = Color.White,
    surfaceDim = Paper,
    surfaceContainer = PaperContainer,
    surfaceContainerHigh = PaperHigh,
    surfaceContainerHighest = PaperHighest,
    surfaceContainerLow = PaperLow,
    surfaceContainerLowest = PaperLowest
)

/**
 * Tema claro / oscuro / sistema. Pinta las barras del sistema del color del
 * fondo e invierte sus iconos según el modo.
 */
@Composable
fun QuizAppTheme(
    settingsManager: SettingsManager,
    content: @Composable () -> Unit
) {
    val theme by settingsManager.theme.collectAsState()
    val isNight = when (theme) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM_DEFAULT -> isSystemInDarkTheme()
    }
    val colorScheme = if (isNight) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val barColor = if (isNight) Graphite else Paper
            @Suppress("DEPRECATION")
            window.statusBarColor = barColor.toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarColor = barColor.toArgb()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !isNight
            controller.isAppearanceLightNavigationBars = !isNight
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
