package id.bantuin.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// Light Palette (WCAG AA terverifikasi via contrast-check.py)
val OrangePrimary = Color(0xFFC43E00) // #FFFFFF di atas #C43E00 = 5.20:1
val OrangePrimaryLight = Color(0xFFE65100)
val OrangeContainer = Color(0xFFFFE0B2)
val OnOrangeContainer = Color(0xFF5D2100) // #5D2100 di atas #FFE0B2 = 9.84:1

val NeutralBackground = Color(0xFFF7F6F4)
val NeutralSurface = Color(0xFFFFFFFF)
val NeutralSurfaceSubtle = Color(0xFFFBFBFB)
val TextPrimary = Color(0xFF1E2022) // 16.41:1 di atas Surface, 15.13:1 di atas Bg
val TextSecondary = Color(0xFF4E555B) // 7.57:1 di atas Surface, 7.01:1 di atas Bg
val TextMuted = Color(0xFF5A6268) // 6.21:1 di atas Surface, 5.75:1 di atas Bg
val BorderSubtle = Color(0xFFD8DCE0) // Divider dekoratif
val OutlineStrong = Color(0xFF737B82) // Border komponen interaktif (>= 3.98:1)

val GreenSuccess = Color(0xFF2E7D32)
val GreenSuccessContainer = Color(0xFFE8F5E9) // 4.56:1
val RedError = Color(0xFFC62828)
val RedErrorContainer = Color(0xFFFFEBEE) // 4.92:1
val AmberSurge = Color(0xFF9A3412)
val AmberSurgeContainer = Color(0xFFFFF8E1) // 6.88:1
val NeutralBadge = Color(0xFF546E7A)
val NeutralBadgeContainer = Color(0xFFECEFF1) // 4.68:1
val BlueInfo = Color(0xFF1D4ED8)
val BlueInfoContainer = Color(0xFFDBEAFE) // 5.49:1
val StarLight = Color(0xFFB45309) // 5.02:1 di atas Surface

// Dark Palette (WCAG AA terverifikasi via contrast-check.py)
val DarkOrangePrimary = Color(0xFFFB8C00) // #121316 di atas #FB8C00 = 7.83:1; di atas #1C1E22 = 7.04:1
val DarkOrangePrimaryLight = Color(0xFFFFB74D)
val DarkOrangeContainer = Color(0xFF2A1F14)
val DarkOnOrangeContainer = Color(0xFFFFB74D) // 9.30:1

val DarkBackground = Color(0xFF121316)
val DarkSurface = Color(0xFF1C1E22)
val DarkSurfaceSubtle = Color(0xFF24272C)
val DarkTextPrimary = Color(0xFFF5F5F4) // 17.03:1 di atas Bg, 13.73:1 di atas SurfaceSubtle
val DarkTextSecondary = Color(0xFFB5BBC0) // 8.61:1 di atas Surface
val DarkTextMuted = Color(0xFF9FA6AD) // 6.78:1 di atas Surface, 6.09:1 di atas SurfaceSubtle
val DarkBorderSubtle = Color(0xFF32363D)
val DarkOutlineStrong = Color(0xFF6B727A) // >= 3.43:1 di atas Surface

val DarkGreenSuccess = Color(0xFF81C784)
val DarkGreenSuccessContainer = Color(0xFF172B1C) // 7.46:1
val DarkRedError = Color(0xFFEF9A9A)
val DarkRedErrorContainer = Color(0xFF2D1618) // 7.87:1
val DarkAmberSurge = Color(0xFFFFB74D)
val DarkAmberSurgeContainer = Color(0xFF2A1F14) // 9.30:1
val DarkNeutralBadge = Color(0xFFB0BEC5)
val DarkNeutralBadgeContainer = Color(0xFF22292E) // 7.73:1
val DarkBlueInfo = Color(0xFF93C5FD)
val DarkBlueInfoContainer = Color(0xFF172554) // 8.15:1
val StarDark = Color(0xFFFBBF24) // 10.00:1 di atas Surface

@Immutable
data class BantuinColors(
    val isDark: Boolean,
    val primary: Color,
    val onPrimary: Color,
    val primaryLight: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val background: Color,
    val surface: Color,
    val surfaceSubtle: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val borderSubtle: Color,
    val outline: Color,
    val success: Color,
    val successContainer: Color,
    val error: Color,
    val errorContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val info: Color,
    val infoContainer: Color,
    val neutralBadge: Color,
    val neutralBadgeContainer: Color,
    val starTint: Color
)

val LightBantuinColors = BantuinColors(
    isDark = false,
    primary = OrangePrimary,
    onPrimary = Color.White,
    primaryLight = OrangePrimaryLight,
    primaryContainer = OrangeContainer,
    onPrimaryContainer = OnOrangeContainer,
    background = NeutralBackground,
    surface = NeutralSurface,
    surfaceSubtle = NeutralSurfaceSubtle,
    textPrimary = TextPrimary,
    textSecondary = TextSecondary,
    textMuted = TextMuted,
    borderSubtle = BorderSubtle,
    outline = OutlineStrong,
    success = GreenSuccess,
    successContainer = GreenSuccessContainer,
    error = RedError,
    errorContainer = RedErrorContainer,
    warning = AmberSurge,
    warningContainer = AmberSurgeContainer,
    info = BlueInfo,
    infoContainer = BlueInfoContainer,
    neutralBadge = NeutralBadge,
    neutralBadgeContainer = NeutralBadgeContainer,
    starTint = StarLight
)

val DarkBantuinColors = BantuinColors(
    isDark = true,
    primary = DarkOrangePrimary,
    onPrimary = DarkBackground,
    primaryLight = DarkOrangePrimaryLight,
    primaryContainer = DarkOrangeContainer,
    onPrimaryContainer = DarkOnOrangeContainer,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceSubtle = DarkSurfaceSubtle,
    textPrimary = DarkTextPrimary,
    textSecondary = DarkTextSecondary,
    textMuted = DarkTextMuted,
    borderSubtle = DarkBorderSubtle,
    outline = DarkOutlineStrong,
    success = DarkGreenSuccess,
    successContainer = DarkGreenSuccessContainer,
    error = DarkRedError,
    errorContainer = DarkRedErrorContainer,
    warning = DarkAmberSurge,
    warningContainer = DarkAmberSurgeContainer,
    info = DarkBlueInfo,
    infoContainer = DarkBlueInfoContainer,
    neutralBadge = DarkNeutralBadge,
    neutralBadgeContainer = DarkNeutralBadgeContainer,
    starTint = StarDark
)
