package app.vos.launcher.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.vos.launcher.model.Room
import app.vos.launcher.ui.theme.LocalVos

/** Верхняя панель: название комнаты, приветствие и кнопка настроек. */
@Composable
fun WorldHeader(
    room: Room,
    subtitle: String,
    accent: Color,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vos = LocalVos.current
    Row(
        modifier
            .fillMaxWidth()
            .windowInsetsPadding(
                WindowInsets.systemBars.union(WindowInsets.displayCutout)
                    .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(room.emoji, fontSize = 28.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = room.title,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = vos.textStrong,
                maxLines = 1,
            )
            Text(
                text = subtitle,
                fontSize = 15.sp,
                color = vos.textSoft,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(vos.glass)
                .border(1.dp, vos.glassBorder, CircleShape)
                .clickable(role = Role.Button, onClickLabel = "Настройки", onClick = onSettings),
            contentAlignment = Alignment.Center,
        ) {
            Text("⚙️", fontSize = 24.sp)
        }
    }
}

/** Нижняя панель: четыре комнаты и «Редактировать». */
@Composable
fun BottomDock(
    selected: Room,
    editMode: Boolean,
    accent: Color,
    onRoom: (Room) -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vos = LocalVos.current
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .windowInsetsPadding(
                WindowInsets.systemBars.union(WindowInsets.displayCutout)
                    .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        val compact = maxWidth < 350.dp
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            color = vos.glass,
            shadowElevation = 14.dp,
            border = BorderStroke(1.dp, vos.glassBorder),
        ) {
            Row(
                Modifier.padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Room.entries.forEach { room ->
                    DockItem(
                        modifier = Modifier.weight(1f),
                        emoji = room.emoji,
                        label = room.dockLabel,
                        selected = room == selected,
                        solid = false,
                        accent = accent,
                        compact = compact,
                        onClick = { onRoom(room) },
                    )
                }
                DockItem(
                    modifier = Modifier.weight(1.8f),
                    emoji = if (editMode) "✅" else "✏️",
                    label = if (editMode) "Готово" else "Редактировать",
                    selected = editMode,
                    solid = true,
                    accent = accent,
                    compact = compact,
                    onClick = onEdit,
                )
            }
        }
    }
}

@Composable
private fun RowScope.DockItem(
    modifier: Modifier,
    emoji: String,
    label: String,
    selected: Boolean,
    solid: Boolean,
    accent: Color,
    compact: Boolean,
    onClick: () -> Unit,
) {
    val vos = LocalVos.current
    val background by animateColorAsState(
        targetValue = when {
            selected && solid -> accent
            selected -> accent.copy(alpha = 0.20f)
            else -> Color.Transparent
        },
        label = "dock-bg",
    )
    val foreground = when {
        selected && solid -> vos.onAccent
        selected -> accent
        else -> vos.textSoft
    }
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.14f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
        label = "dock-scale",
    )

    Column(
        modifier
            .heightIn(min = 62.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(background)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = emoji,
            fontSize = 22.sp,
            modifier = Modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        )
        Text(
            text = label,
            fontSize = if (compact) 12.sp else 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            color = foreground,
            maxLines = 1,
            softWrap = false,
        )
    }
}
