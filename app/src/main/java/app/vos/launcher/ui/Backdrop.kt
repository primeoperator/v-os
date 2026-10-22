package app.vos.launcher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import app.vos.launcher.model.Room
import app.vos.launcher.ui.theme.LocalVos
import app.vos.launcher.ui.theme.roomTheme

/** Нарисованная комната: стена, окно, пол, ковёр и пятно света. Без картинок и ассетов. */
@Composable
fun RoomBackdrop(room: Room, modifier: Modifier = Modifier) {
    val dark = LocalVos.current.isDark
    val t = remember(room, dark) { roomTheme(room, dark) }

    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val wallH = h * 0.36f

        // Стена и пол
        drawRect(
            brush = Brush.verticalGradient(listOf(t.wallTop, t.wallBottom), 0f, wallH),
            size = Size(w, wallH),
        )
        drawRect(
            brush = Brush.verticalGradient(listOf(t.floorTop, t.floorBottom), wallH, h),
            topLeft = Offset(0f, wallH),
            size = Size(w, h - wallH),
        )

        // Доски пола: расстояние растёт вниз — получается лёгкая перспектива
        val plank = 46.dp.toPx()
        var y = wallH + plank
        var step = plank
        while (y < h) {
            drawLine(
                color = t.trim.copy(alpha = 0.20f),
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1.5f.dp.toPx(),
            )
            step *= 1.12f
            y += step
        }

        // Плинтус
        drawRect(
            color = t.trim,
            topLeft = Offset(0f, wallH - 6.dp.toPx()),
            size = Size(w, 12.dp.toPx()),
        )

        // Окно: слева у «Создания» и «Быта», справа у остальных комнат
        val ww = w * 0.34f
        val wh = wallH * 0.42f
        val wx = if (room == Room.CREATE || room == Room.HOME) w * 0.09f else w - ww - w * 0.09f
        val wy = wallH * 0.40f
        val frame = 8.dp.toPx()

        drawRoundRect(
            color = t.trim,
            topLeft = Offset(wx - frame, wy - frame),
            size = Size(ww + frame * 2, wh + frame * 2),
            cornerRadius = CornerRadius(20.dp.toPx()),
        )
        drawRoundRect(
            brush = Brush.verticalGradient(
                listOf(t.window, t.glow.copy(alpha = 0.55f)),
                wy,
                wy + wh,
            ),
            topLeft = Offset(wx, wy),
            size = Size(ww, wh),
            cornerRadius = CornerRadius(14.dp.toPx()),
        )
        // Солнце или луна
        drawCircle(
            color = t.glow.copy(alpha = if (room == Room.LEISURE) 0.95f else 0.75f),
            radius = wh * 0.16f,
            center = Offset(wx + ww * 0.72f, wy + wh * 0.30f),
        )
        // Переплёт
        val bar = 3.dp.toPx()
        drawLine(t.trim, Offset(wx + ww / 2, wy), Offset(wx + ww / 2, wy + wh), strokeWidth = bar)
        drawLine(t.trim, Offset(wx, wy + wh / 2), Offset(wx + ww, wy + wh / 2), strokeWidth = bar)

        // Пятно света на полу под окном
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(t.glow.copy(alpha = 0.35f), Color.Transparent),
                center = Offset(wx + ww / 2, wallH + 150.dp.toPx()),
                radius = ww * 1.1f,
            ),
            radius = ww * 1.1f,
            center = Offset(wx + ww / 2, wallH + 150.dp.toPx()),
        )

        // Ковёр
        val rugTop = Offset(w * 0.10f, h * 0.60f)
        val rugSize = Size(w * 0.80f, h * 0.20f)
        drawOval(color = t.rug.copy(alpha = 0.32f), topLeft = rugTop, size = rugSize)
        drawOval(
            color = t.rug.copy(alpha = 0.55f),
            topLeft = Offset(rugTop.x + rugSize.width * 0.08f, rugTop.y + rugSize.height * 0.12f),
            size = Size(rugSize.width * 0.84f, rugSize.height * 0.76f),
            style = Stroke(width = 3.dp.toPx()),
        )
    }
}
