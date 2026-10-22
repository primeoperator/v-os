package app.vos.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.vos.launcher.LauncherViewModel
import app.vos.launcher.model.AppItem
import app.vos.launcher.model.Room
import app.vos.launcher.ui.theme.LocalVos
import app.vos.launcher.ui.theme.VosTheme
import app.vos.launcher.ui.theme.roomTheme
import java.util.Calendar

/** Корневой экран V-OS: мир, шапка, нижняя панель, настройки и выбор комнаты. */
@Composable
fun VosApp(
    vm: LauncherViewModel,
    onLaunch: (AppItem) -> Unit,
    onRequestHome: () -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()

    VosTheme(settings.themeMode) {
        val vos = LocalVos.current
        val accent = remember(state.room, vos.isDark) { roomTheme(state.room, vos.isDark).accent }
        val feedback = rememberFeedback(settings.soundsEnabled)
        val greeting = remember(settings.userName) { greetingFor(settings.userName) }

        // Назад закрывает настройки или выходит из режима редактирования.
        // На главном экране кнопка «Назад» ничего не делает — так ведёт себя любой лаунчер.
        BackHandler(enabled = state.showSettings || state.editMode) {
            if (state.showSettings) vm.closeSettings() else vm.setEditMode(false)
        }

        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            if (state.ready) {
                Crossfade(
                    targetState = state.room,
                    animationSpec = tween(400),
                    label = "backdrop",
                ) { room ->
                    RoomBackdrop(room, Modifier.fillMaxSize())
                }

                Column(Modifier.fillMaxSize()) {
                    WorldHeader(
                        room = state.room,
                        subtitle = greeting,
                        accent = accent,
                        onSettings = {
                            feedback.tap()
                            vm.openSettings()
                        },
                    )
                    EditBanner(
                        visible = state.editMode,
                        accent = accent,
                        onDone = {
                            feedback.tap()
                            vm.setEditMode(false)
                        },
                    )
                    HomeWorld(
                        room = state.room,
                        apps = state.apps,
                        layout = state.layout,
                        editMode = state.editMode,
                        accent = accent,
                        feedback = feedback,
                        onLaunch = onLaunch,
                        onEnterEdit = { vm.setEditMode(true) },
                        onMove = vm::movePlacement,
                        onChoose = { vm.openChooser(it.key) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    )
                    BottomDock(
                        selected = state.room,
                        editMode = state.editMode,
                        accent = accent,
                        onRoom = { room ->
                            if (room != state.room) {
                                feedback.switchRoom()
                                vm.selectRoom(room)
                            }
                        },
                        onEdit = {
                            if (state.editMode) {
                                feedback.tap()
                                vm.setEditMode(false)
                            } else {
                                feedback.pick()
                                vm.setEditMode(true)
                            }
                        },
                    )
                }
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "V-OS",
                        fontSize = 40.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = vos.textStrong,
                    )
                }
            }

            AnimatedVisibility(
                visible = state.showSettings,
                enter = slideInVertically(tween(280)) { it / 4 } + fadeIn(tween(220)),
                exit = slideOutVertically(tween(220)) { it / 4 } + fadeOut(tween(180)),
            ) {
                SettingsScreen(
                    settings = settings,
                    isDefaultHome = state.isDefaultHome,
                    accent = accent,
                    feedback = feedback,
                    onClose = vm::closeSettings,
                    onName = vm::setUserName,
                    onTheme = vm::setThemeMode,
                    onSounds = vm::setSoundsEnabled,
                    onResetLayout = vm::resetLayout,
                    onMakeHome = onRequestHome,
                )
            }

            val chooserApp = remember(state.chooserKey, state.apps) {
                state.chooserKey?.let { key -> state.apps.firstOrNull { it.key == key } }
            }
            if (chooserApp != null) {
                RoomChooserDialog(
                    app = chooserApp,
                    currentRoom = Room.fromId(state.layout.placements[chooserApp.key]?.room),
                    onPick = { room ->
                        feedback.tap()
                        vm.moveToRoom(chooserApp.key, room)
                    },
                    onDismiss = vm::closeChooser,
                )
            }
        }
    }
}

private fun greetingFor(name: String): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val base = when (hour) {
        in 5..11 -> "Доброе утро"
        in 12..17 -> "Добрый день"
        in 18..22 -> "Добрый вечер"
        else -> "Доброй ночи"
    }
    return if (name.isBlank()) base else "$base, ${name.trim()}"
}
