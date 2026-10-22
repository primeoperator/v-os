package app.vos.launcher

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.vos.launcher.data.DataStoreRepository
import app.vos.launcher.data.LauncherRepository
import app.vos.launcher.data.LayoutPlanner
import app.vos.launcher.model.AppItem
import app.vos.launcher.model.LayoutState
import app.vos.launcher.model.Room
import app.vos.launcher.model.SettingsState
import app.vos.launcher.model.ThemeMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UiState(
    val ready: Boolean = false,
    val apps: List<AppItem> = emptyList(),
    val layout: LayoutState = LayoutState(),
    val room: Room = Room.CREATE,
    val editMode: Boolean = false,
    val showSettings: Boolean = false,
    val chooserKey: String? = null,
    val isDefaultHome: Boolean = false,
)

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val launcherRepository = LauncherRepository(application)
    private val dataStore = DataStoreRepository(application)

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _settings = MutableStateFlow(SettingsState())
    val settings: StateFlow<SettingsState> = _settings.asStateFlow()

    private val layoutLoaded = CompletableDeferred<Unit>()
    private var refreshJob: Job? = null
    private var saveJob: Job? = null

    init {
        viewModelScope.launch {
            val savedSettings = dataStore.settings.first()
            val savedLayout = dataStore.layout.first()
            _settings.value = savedSettings
            _state.update { it.copy(layout = savedLayout) }
            layoutLoaded.complete(Unit)

            launch { dataStore.settings.collect { _settings.value = it } }
            refreshNow()
        }
    }

    // ---------- Приложения ----------

    fun refreshApps() = scheduleRefresh(0L)

    /** Вызывается при установке, удалении и обновлении пакетов. */
    fun onPackageEvent(packageName: String?, replaced: Boolean) {
        if (replaced && packageName != null) launcherRepository.invalidate(packageName)
        scheduleRefresh(400L)
    }

    private fun scheduleRefresh(delayMs: Long) {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            if (delayMs > 0) delay(delayMs)
            layoutLoaded.await()
            refreshNow()
        }
    }

    private suspend fun refreshNow() {
        val apps = try {
            launcherRepository.loadApps()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            _state.value.apps
        }
        val before = _state.value.layout
        val planned = LayoutPlanner.fill(apps, before)
        _state.update { it.copy(apps = apps, layout = planned, ready = true) }
        if (planned !== before) persistLayout()
    }

    fun launch(context: Context, app: AppItem): Boolean {
        val ok = launcherRepository.launch(context, app)
        if (!ok) refreshApps()
        return ok
    }

    // ---------- Навигация и режимы ----------

    fun selectRoom(room: Room) = _state.update { it.copy(room = room) }

    fun setEditMode(on: Boolean) = _state.update {
        it.copy(editMode = on, chooserKey = if (on) it.chooserKey else null)
    }

    fun openSettings() = _state.update { it.copy(showSettings = true, chooserKey = null) }

    fun closeSettings() = _state.update { it.copy(showSettings = false) }

    fun openChooser(key: String) = _state.update { it.copy(chooserKey = key) }

    fun closeChooser() = _state.update { it.copy(chooserKey = null) }

    /** Нажата кнопка «Домой», пока V-OS уже открыт: возвращаемся на главный экран. */
    fun onHomePressed() = _state.update {
        it.copy(showSettings = false, editMode = false, chooserKey = null)
    }

    fun setDefaultHome(value: Boolean) = _state.update {
        if (it.isDefaultHome == value) it else it.copy(isDefaultHome = value)
    }

    // ---------- Раскладка ----------

    fun movePlacement(key: String, x: Float, yDp: Float) {
        _state.update { s ->
            val old = s.layout.placements[key] ?: return@update s
            val moved = old.copy(x = x, yDp = yDp)
            s.copy(layout = s.layout.copy(placements = s.layout.placements + (key to moved)))
        }
        persistLayout()
    }

    fun moveToRoom(key: String, room: Room) {
        _state.update { s ->
            val current = s.layout.placements[key] ?: return@update s.copy(chooserKey = null)
            if (current.room == room.id) return@update s.copy(chooserKey = null)
            val active = s.apps.mapTo(HashSet()) { it.key }
            val others = s.layout.placements.filter { (k, _) -> k != key && k in active }
            val slot = LayoutPlanner.nextFreeSlot(others, room)
            s.copy(
                layout = s.layout.copy(placements = s.layout.placements + (key to slot)),
                chooserKey = null,
            )
        }
        persistLayout()
    }

    fun resetLayout() {
        _state.update { s ->
            s.copy(
                layout = LayoutPlanner.fill(s.apps, LayoutState()),
                editMode = false,
                chooserKey = null,
            )
        }
        persistLayout()
    }

    private fun persistLayout() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch { dataStore.saveLayout(_state.value.layout) }
    }

    // ---------- Настройки ----------

    fun setUserName(name: String) {
        viewModelScope.launch { dataStore.setUserName(name.take(24)) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { dataStore.setThemeMode(mode) }
    }

    fun setSoundsEnabled(enabled: Boolean) {
        viewModelScope.launch { dataStore.setSoundsEnabled(enabled) }
    }
}
