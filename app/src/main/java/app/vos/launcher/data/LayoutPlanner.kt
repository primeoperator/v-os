package app.vos.launcher.data

import app.vos.launcher.model.AppItem
import app.vos.launcher.model.LayoutState
import app.vos.launcher.model.Placement
import app.vos.launcher.model.Room
import app.vos.launcher.model.SceneMetrics
import kotlin.math.abs

/** Расставляет приложения, у которых ещё нет сохранённой позиции. */
object LayoutPlanner {

    private val COLUMNS = floatArrayOf(0.04f, 0.5f, 0.96f)

    /** Добавляет позиции для всех приложений без позиции. Уже сохранённые не трогает. */
    fun fill(apps: List<AppItem>, current: LayoutState): LayoutState {
        val missing = apps.filter { it.key !in current.placements }
        if (missing.isEmpty()) return current

        val activeKeys = apps.mapTo(HashSet()) { it.key }
        val blockers = current.placements.filterKeys { it in activeKeys }.toMutableMap()
        val result = current.placements.toMutableMap()

        // Сначала особые приложения (Telegram, браузер, камера, GitHub), затем остальные по алфавиту.
        val ordered = missing.sortedWith(
            compareBy<AppItem>({ RoomClassifier.featuredSpot(it) == null }, { it.label.lowercase() }),
        )

        for (app in ordered) {
            val room = RoomClassifier.roomFor(app)
            val spot = RoomClassifier.featuredSpot(app)
            val placement =
                if (spot != null && isFree(blockers, room, spot.first, spot.second)) {
                    Placement(room.id, spot.first, spot.second)
                } else {
                    nextFreeSlot(blockers, room)
                }
            blockers[app.key] = placement
            result[app.key] = placement
        }
        return LayoutState(result, current.version)
    }

    /** Первое свободное место в сетке комнаты. */
    fun nextFreeSlot(placements: Map<String, Placement>, room: Room): Placement {
        var row = 0
        while (true) {
            val y = SceneMetrics.FIRST_ROW_Y_DP + row * SceneMetrics.ROW_PITCH_DP
            for (x in COLUMNS) {
                if (isFree(placements, room, x, y)) return Placement(room.id, x, y)
            }
            row++
        }
    }

    private fun isFree(
        placements: Map<String, Placement>,
        room: Room,
        x: Float,
        yDp: Float,
    ): Boolean {
        val range = SceneMetrics.ASSUMED_WIDTH_DP - SceneMetrics.OBJECT_WIDTH_DP
        return placements.values.none { p ->
            p.room == room.id &&
                abs(p.x - x) * range < SceneMetrics.OBJECT_WIDTH_DP * 0.85f &&
                abs(p.yDp - yDp) < SceneMetrics.OBJECT_HEIGHT_DP * 0.9f
        }
    }
}
