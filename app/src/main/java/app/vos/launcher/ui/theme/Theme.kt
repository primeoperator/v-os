package app.vos.launcher.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import app.vos.launcher.model.Room
import app.vos.launcher.model.ThemeMode

/** Дополнительные цвета игрового интерфейса поверх Material-темы. */
@Immutable
data class VosColors(
    val isDark: Boolean,
    val glass: Color,
    val glassBorder: Color,
    val textStrong: Color,
    val textSoft: Color,
    val tileBg: Color,
    val tileBorder: Color,
    val labelBg: Color,
    val card: Color,
    val onAccent: Color,
)

private val LightVos = VosColors(
    isDark = false,
    glass = Color(0xEBFFFFFF),
    glassBorder = Color(0x14000000),
    textStrong = Color(0xFF2B2438),
    textSoft = Color(0xFF6B6478),
    tileBg = Color(0xFFFFFFFF),
    tileBorder = Color(0x80FFFFFF),
    labelBg = Color(0xD9FFFFFF),
    card = Color(0xFFFFFFFF),
    onAccent = Color.White,
)

private val DarkVos = VosColors(
    isDark = true,
    glass = Color(0xE61F1A2E),
    glassBorder = Color(0x1FFFFFFF),
    textStrong = Color(0xFFF3EFFA),
    textSoft = Color(0xFFB7AFC9),
    tileBg = Color(0xFF2C2640),
    tileBorder = Color(0x26FFFFFF),
    labelBg = Color(0xCC191527),
    card = Color(0xFF241F33),
    onAccent = Color(0xFF1B1426),
)

val LocalVos = staticCompositionLocalOf { LightVos }

private val LightScheme = lightColorScheme(
    primary = Color(0xFF6B49E8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE6DDFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFF7B6A9E),
    onSecondary = Color.White,
    background = Color(0xFFFFF9F2),
    onBackground = Color(0xFF2B2438),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF2B2438),
    surfaceVariant = Color(0xFFF1EBF8),
    onSurfaceVariant = Color(0xFF6B6478),
    outline = Color(0xFFCFC6DD),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFB096FF),
    onPrimary = Color(0xFF1B1426),
    primaryContainer = Color(0xFF3F2E80),
    onPrimaryContainer = Color(0xFFE6DDFF),
    secondary = Color(0xFFC9BDE6),
    onSecondary = Color(0xFF1B1426),
    background = Color(0xFF14111D),
    onBackground = Color(0xFFF3EFFA),
    surface = Color(0xFF241F33),
    onSurface = Color(0xFFF3EFFA),
    surfaceVariant = Color(0xFF332C47),
    onSurfaceVariant = Color(0xFFB7AFC9),
    outline = Color(0xFF4A4262),
)

@Composable
fun VosTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = view.context.findActivity()?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !dark
                controller.isAppearanceLightNavigationBars = !dark
            }
        }
    }

    CompositionLocalProvider(LocalVos provides if (dark) DarkVos else LightVos) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            content = content,
        )
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** Цвета комнаты: стены, пол, окно, акцент. */
@Immutable
data class RoomTheme(
    val wallTop: Color,
    val wallBottom: Color,
    val floorTop: Color,
    val floorBottom: Color,
    val trim: Color,
    val rug: Color,
    val window: Color,
    val glow: Color,
    val accent: Color,
)

fun roomTheme(room: Room, dark: Boolean): RoomTheme = if (dark) darkRoom(room) else lightRoom(room)

private fun lightRoom(room: Room): RoomTheme = when (room) {
    Room.CREATE -> RoomTheme(
        wallTop = Color(0xFFFFEFD9), wallBottom = Color(0xFFFFDDB8),
        floorTop = Color(0xFFEBC9A0), floorBottom = Color(0xFFD9B07F),
        trim = Color(0xFFC99A68), rug = Color(0xFFFFB38A),
        window = Color(0xFFFFF6DE), glow = Color(0xFFFFD27A), accent = Color(0xFFE8622C),
    )
    Room.COMMUNICATION -> RoomTheme(
        wallTop = Color(0xFFE4EFFF), wallBottom = Color(0xFFCFE2FF),
        floorTop = Color(0xFFC2D4EE), floorBottom = Color(0xFFA9BFDE),
        trim = Color(0xFF8FA8CC), rug = Color(0xFF8DB4FF),
        window = Color(0xFFF4F9FF), glow = Color(0xFFB9D6FF), accent = Color(0xFF2F6FE0),
    )
    Room.HOME -> RoomTheme(
        wallTop = Color(0xFFE9F8E5), wallBottom = Color(0xFFD2EFCF),
        floorTop = Color(0xFFDCCBA8), floorBottom = Color(0xFFC7B38C),
        trim = Color(0xFFAE9A72), rug = Color(0xFF8AD7A8),
        window = Color(0xFFF6FFF3), glow = Color(0xFFC8F0A8), accent = Color(0xFF1F9E67),
    )
    Room.LEISURE -> RoomTheme(
        wallTop = Color(0xFFEFE6FF), wallBottom = Color(0xFFDDCFFF),
        floorTop = Color(0xFFCDBEEA), floorBottom = Color(0xFFB7A5DC),
        trim = Color(0xFF9C88C9), rug = Color(0xFFB79BFF),
        window = Color(0xFFF7F1FF), glow = Color(0xFFD6C3FF), accent = Color(0xFF6B49E8),
    )
}

private fun darkRoom(room: Room): RoomTheme = when (room) {
    Room.CREATE -> RoomTheme(
        wallTop = Color(0xFF3E2C25), wallBottom = Color(0xFF31231D),
        floorTop = Color(0xFF2A1E19), floorBottom = Color(0xFF1E1512),
        trim = Color(0xFF54392E), rug = Color(0xFF8A4B33),
        window = Color(0xFF5A4A38), glow = Color(0xFFFF9A6E), accent = Color(0xFFFF9A6E),
    )
    Room.COMMUNICATION -> RoomTheme(
        wallTop = Color(0xFF22304D), wallBottom = Color(0xFF1A263F),
        floorTop = Color(0xFF17213A), floorBottom = Color(0xFF111829),
        trim = Color(0xFF2D3F66), rug = Color(0xFF2F4F8F),
        window = Color(0xFF2B3B5E), glow = Color(0xFF6FA2FF), accent = Color(0xFF7FAEFF),
    )
    Room.HOME -> RoomTheme(
        wallTop = Color(0xFF223A2D), wallBottom = Color(0xFF1A2E24),
        floorTop = Color(0xFF18271F), floorBottom = Color(0xFF121D17),
        trim = Color(0xFF2E4A3B), rug = Color(0xFF2F7355),
        window = Color(0xFF2B4538), glow = Color(0xFF5FD29B), accent = Color(0xFF62D6A0),
    )
    Room.LEISURE -> RoomTheme(
        wallTop = Color(0xFF33284F), wallBottom = Color(0xFF2A2041),
        floorTop = Color(0xFF211A36), floorBottom = Color(0xFF17122A),
        trim = Color(0xFF41336A), rug = Color(0xFF5A41A8),
        window = Color(0xFF3A2E5E), glow = Color(0xFFA58BFF), accent = Color(0xFFB096FF),
    )
}
