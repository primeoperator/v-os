package app.vos.launcher.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.vos.launcher.model.LayoutState
import app.vos.launcher.model.SettingsState
import app.vos.launcher.model.ThemeMode
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.vosDataStore: DataStore<Preferences> by preferencesDataStore(name = "vos_launcher")

private val KEY_LAYOUT = stringPreferencesKey("layout_v1")
private val KEY_USER_NAME = stringPreferencesKey("user_name")
private val KEY_THEME = stringPreferencesKey("theme_mode")
private val KEY_SOUNDS = booleanPreferencesKey("sounds_enabled")

/** Локальное хранение раскладки и настроек (DataStore Preferences). */
class DataStoreRepository(context: Context) {

    private val store = context.applicationContext.vosDataStore
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private fun safeData(): Flow<Preferences> = store.data.catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }

    val layout: Flow<LayoutState> = safeData().map { prefs ->
        prefs[KEY_LAYOUT]?.let { raw ->
            runCatching { json.decodeFromString(LayoutState.serializer(), raw) }.getOrNull()
        } ?: LayoutState()
    }

    val settings: Flow<SettingsState> = safeData().map { prefs ->
        SettingsState(
            userName = prefs[KEY_USER_NAME].orEmpty(),
            themeMode = runCatching { ThemeMode.valueOf(prefs[KEY_THEME].orEmpty()) }
                .getOrDefault(ThemeMode.SYSTEM),
            soundsEnabled = prefs[KEY_SOUNDS] ?: true,
        )
    }

    suspend fun saveLayout(state: LayoutState) {
        val encoded = json.encodeToString(LayoutState.serializer(), state)
        store.edit { it[KEY_LAYOUT] = encoded }
    }

    suspend fun clearLayout() {
        store.edit { it.remove(KEY_LAYOUT) }
    }

    suspend fun setUserName(name: String) {
        store.edit { it[KEY_USER_NAME] = name }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        store.edit { it[KEY_THEME] = mode.name }
    }

    suspend fun setSoundsEnabled(enabled: Boolean) {
        store.edit { it[KEY_SOUNDS] = enabled }
    }
}
