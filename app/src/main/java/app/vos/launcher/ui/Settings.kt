package app.vos.launcher.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.vos.launcher.model.SettingsState
import app.vos.launcher.model.ThemeMode
import app.vos.launcher.ui.theme.LocalVos

/** Экран настроек: имя, тема, звуки, роль «Домой», сброс раскладки. */
@Composable
fun SettingsScreen(
    settings: SettingsState,
    isDefaultHome: Boolean,
    accent: Color,
    feedback: Feedback,
    onClose: () -> Unit,
    onName: (String) -> Unit,
    onTheme: (ThemeMode) -> Unit,
    onSounds: (Boolean) -> Unit,
    onResetLayout: () -> Unit,
    onMakeHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vos = LocalVos.current
    var name by remember { mutableStateOf(settings.userName) }
    var confirmReset by remember { mutableStateOf(false) }

    Column(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            // Экран закрывает сцену целиком и не пропускает нажатия вниз
            .pointerInput(Unit) { detectTapGestures { } }
            .windowInsetsPadding(
                WindowInsets.systemBars.union(WindowInsets.displayCutout)
                    .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
            )
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Настройки",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = vos.textStrong,
                modifier = Modifier.weight(1f),
            )
            Box(
                Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(vos.textSoft.copy(alpha = 0.14f))
                    .clickable(role = Role.Button, onClickLabel = "Закрыть настройки") {
                        feedback.tap()
                        onClose()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text("✕", fontSize = 22.sp, color = vos.textStrong)
            }
        }

        SettingsCard(title = "Твоё имя", emoji = "👋") {
            OutlinedTextField(
                value = name,
                onValueChange = { value ->
                    val trimmed = value.take(24)
                    name = trimmed
                    onName(trimmed)
                },
                singleLine = true,
                label = { Text("Как к тебе обращаться?") },
                textStyle = TextStyle(fontSize = 18.sp, color = vos.textStrong),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        SettingsCard(title = "Тема", emoji = "🎨") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val options = listOf(
                    Triple(ThemeMode.SYSTEM, "🌗", "Система"),
                    Triple(ThemeMode.LIGHT, "☀️", "Светлая"),
                    Triple(ThemeMode.DARK, "🌙", "Тёмная"),
                )
                options.forEach { (mode, emoji, label) ->
                    ChoiceChip(
                        modifier = Modifier.weight(1f),
                        emoji = emoji,
                        label = label,
                        selected = settings.themeMode == mode,
                        accent = accent,
                        onClick = {
                            feedback.tap()
                            onTheme(mode)
                        },
                    )
                }
            }
        }

        SettingsCard(title = "Звуки", emoji = "🔔") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Звуки интерфейса",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = vos.textStrong,
                    )
                    Text(
                        text = "Мягкие щелчки при нажатиях и перетаскивании",
                        fontSize = 14.sp,
                        color = vos.textSoft,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Switch(
                    checked = settings.soundsEnabled,
                    onCheckedChange = { onSounds(it) },
                    modifier = Modifier.heightIn(min = 48.dp),
                )
            }
        }

        SettingsCard(title = "Домашний экран", emoji = "🏠") {
            if (isDefaultHome) {
                Text(
                    text = "✓ V-OS сейчас — твой домашний экран. Кнопка «Домой» открывает этот мир.",
                    fontSize = 16.sp,
                    color = vos.textStrong,
                )
            } else {
                Text(
                    text = "Сделай V-OS приложением «Домой», чтобы он открывался по кнопке «Домой» на телефоне.",
                    fontSize = 15.sp,
                    color = vos.textSoft,
                )
                Button(
                    onClick = {
                        feedback.tap()
                        onMakeHome()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accent,
                        contentColor = vos.onAccent,
                    ),
                ) {
                    Text("Сделать V-OS домашним", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        SettingsCard(title = "Расположение", emoji = "🧩") {
            Text(
                text = "Вернуть все объекты на стартовые места. Имя, тема и звуки сохранятся.",
                fontSize = 15.sp,
                color = vos.textSoft,
            )
            OutlinedButton(
                onClick = { confirmReset = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
                shape = RoundedCornerShape(18.dp),
            ) {
                Text(
                    text = "Сбросить расположение",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = vos.textStrong,
                )
            }
        }

        Text(
            text = "V-OS Launcher 1.0 · Список приложений и раскладка хранятся только на этом телефоне.",
            fontSize = 13.sp,
            color = vos.textSoft,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        )
        Spacer(Modifier.height(8.dp))
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Сбросить расположение?", fontWeight = FontWeight.Bold) },
            text = { Text("Все объекты вернутся на стартовые места в своих комнатах.", fontSize = 16.sp) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmReset = false
                        feedback.pick()
                        onResetLayout()
                    },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text("Сбросить", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { confirmReset = false },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text("Отмена", fontSize = 16.sp)
                }
            },
        )
    }
}

@Composable
private fun SettingsCard(
    title: String,
    emoji: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    val vos = LocalVos.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = vos.card,
        shadowElevation = 3.dp,
        border = BorderStroke(1.dp, vos.glassBorder),
    ) {
        Column(
            Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(emoji, fontSize = 22.sp)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = vos.textStrong,
                )
            }
            content()
        }
    }
}

@Composable
private fun ChoiceChip(
    modifier: Modifier,
    emoji: String,
    label: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    val vos = LocalVos.current
    val shape = RoundedCornerShape(20.dp)
    val background by animateColorAsState(
        targetValue = if (selected) accent.copy(alpha = 0.22f) else vos.textSoft.copy(alpha = 0.10f),
        label = "chip-bg",
    )
    Column(
        modifier
            .heightIn(min = 76.dp)
            .clip(shape)
            .background(background)
            .then(if (selected) Modifier.border(2.dp, accent, shape) else Modifier)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(emoji, fontSize = 24.sp)
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            color = vos.textStrong,
            maxLines = 1,
        )
    }
}
