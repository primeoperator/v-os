package app.vos.launcher.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.vos.launcher.model.AppItem
import app.vos.launcher.model.Room
import app.vos.launcher.ui.theme.LocalVos
import app.vos.launcher.ui.theme.roomTheme

/** Плашка сверху в режиме редактирования: подсказка и кнопка «Готово». */
@Composable
fun EditBanner(
    visible: Boolean,
    accent: Color,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vos = LocalVos.current
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp),
            shape = RoundedCornerShape(24.dp),
            color = vos.glass,
            shadowElevation = 6.dp,
            border = BorderStroke(1.dp, vos.glassBorder),
        ) {
            Row(
                Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Режим редактирования",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = vos.textStrong,
                    )
                    Text(
                        text = "Тащи объекты. Нажми на объект — выбери комнату.",
                        fontSize = 14.sp,
                        color = vos.textSoft,
                    )
                }
                Button(
                    onClick = onDone,
                    modifier = Modifier.heightIn(min = 48.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accent,
                        contentColor = vos.onAccent,
                    ),
                ) {
                    Text("Готово", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/** Выбор комнаты для объекта: большие кнопки 2×2. */
@Composable
fun RoomChooserDialog(
    app: AppItem,
    currentRoom: Room,
    onPick: (Room) -> Unit,
    onDismiss: () -> Unit,
) {
    val vos = LocalVos.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .widthIn(max = 420.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            color = vos.card,
            shadowElevation = 16.dp,
        ) {
            Column(
                Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val icon = app.icon
                if (icon != null) {
                    Image(
                        bitmap = icon,
                        contentDescription = null,
                        filterQuality = FilterQuality.Medium,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(20.dp)),
                    )
                }
                Text(
                    text = app.label,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = vos.textStrong,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 10.dp),
                )
                Text(
                    text = "В какую комнату переселить?",
                    fontSize = 15.sp,
                    color = vos.textSoft,
                    modifier = Modifier.padding(top = 2.dp, bottom = 14.dp),
                )
                Room.entries.chunked(2).forEach { row ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        row.forEach { room ->
                            RoomChoice(
                                modifier = Modifier.weight(1f),
                                room = room,
                                selected = room == currentRoom,
                                onClick = { onPick(room) },
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
                TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("Закрыть", fontSize = 16.sp, color = vos.textSoft)
                }
            }
        }
    }
}

@Composable
private fun RoomChoice(
    modifier: Modifier,
    room: Room,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val vos = LocalVos.current
    val accent = roomTheme(room, vos.isDark).accent
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier
            .heightIn(min = 84.dp)
            .clip(shape)
            .background(accent.copy(alpha = if (selected) 0.30f else 0.14f))
            .then(if (selected) Modifier.border(2.dp, accent, shape) else Modifier)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(room.emoji, fontSize = 28.sp)
        Text(
            text = room.title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = vos.textStrong,
        )
    }
}
