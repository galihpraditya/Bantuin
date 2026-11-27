package id.bantuin.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import id.bantuin.app.core.theme.ThemeMode

private val LightColorScheme = lightColorScheme(
    primary = OrangePrimary,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = OrangeContainer,
    onPrimaryContainer = OnOrangeContainer,
    secondary = OrangePrimaryLight,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    background = NeutralBackground,
    onBackground = TextPrimary,
    surface = NeutralSurface,
    onSurface = TextPrimary,
    surfaceVariant = NeutralSurfaceSubtle,
    onSurfaceVariant = TextSecondary,
    outline = OutlineStrong,
    outlineVariant = BorderSubtle,
    error = RedError,
    onError = androidx.compose.ui.graphics.Color.White,
    errorContainer = RedErrorContainer,
    onErrorContainer = RedError
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkOrangePrimary,
    onPrimary = DarkBackground,
    primaryContainer = DarkOrangeContainer,
    onPrimaryContainer = DarkOnOrangeContainer,
    secondary = DarkOrangePrimaryLight,
    onSecondary = DarkBackground,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceSubtle,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkOutlineStrong,
    outlineVariant = DarkBorderSubtle,
    error = DarkRedError,
    onError = DarkBackground,
    errorContainer = DarkRedErrorContainer,
    onErrorContainer = DarkRedError
)

val LocalBantuinColors = staticCompositionLocalOf { LightBantuinColors }

object BantuinTheme {
    val colors: BantuinColors
        @Composable
        @ReadOnlyComposable
        get() = LocalBantuinColors.current
}

@Composable
fun BantuinTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    darkTheme: Boolean = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    },
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val customColors = if (darkTheme) DarkBantuinColors else LightBantuinColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            window?.let {
                val controller = WindowCompat.getInsetsController(it, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalBantuinColors provides customColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
