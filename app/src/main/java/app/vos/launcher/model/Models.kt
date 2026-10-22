package app.vos.launcher.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.serialization.Serializable

/** Комнаты игрового мира. */
enum class Room(
    val id: String,
    val title: String,
    val dockLabel: String,
    val emoji: String,
) {
    CREATE("create", "Создание", "Создавать", "🎨"),
    COMMUNICATION("communication", "Связь", "Связь", "💬"),
    HOME("home", "Быт", "Быт", "🏡"),
    LEISURE("leisure", "Отдых", "Отдых", "🎧");

    companion object {
        fun fromId(id: String?): Room = entries.firstOrNull { it.id == id } ?: HOME
    }
}

/** Установленное приложение, которое можно запустить. */
@Immutable
data class AppItem(
    val packageName: String,
    val activityName: String,
    val label: String,
    val icon: ImageBitmap?,
    val category: Int,
    val isBrowser: Boolean,
    val isCamera: Boolean,
) {
    /** Уникальный ключ: пакет + запускаемая активность. */
    val key: String get() = "$packageName/$activityName"
}

/**
 * Положение объекта в комнате.
 * [x] — доля свободной ширины сцены (0..1), [yDp] — отступ сверху в dp.
 */
@Serializable
data class Placement(
    val room: String,
    val x: Float,
    val yDp: Float,
)

/** Сохраняемая раскладка: ключ приложения -> положение. */
@Serializable
data class LayoutState(
    val placements: Map<String, Placement> = emptyMap(),
    val version: Int = 1,
)

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class SettingsState(
    val userName: String = "",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val soundsEnabled: Boolean = true,
)

@Immutable
data class PlacedApp(
    val app: AppItem,
    val placement: Placement,
)

/** Размеры сцены в dp — общие для UI и для расстановки по умолчанию. */
object SceneMetrics {
    const val OBJECT_WIDTH_DP = 92f
    const val OBJECT_HEIGHT_DP = 104f
    const val TILE_DP = 68f
    const val ASSUMED_WIDTH_DP = 360f
    const val FIRST_ROW_Y_DP = 130f
    const val ROW_PITCH_DP = 124f
    const val MAX_Y_DP = 3000f
}
