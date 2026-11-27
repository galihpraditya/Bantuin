package id.bantuin.app.core.theme

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode(val label: String) {
    SYSTEM("Ikuti Sistem"),
    LIGHT("Mode Terang"),
    DARK("Mode Gelap")
}

interface ThemePreferenceStore {
    val themeMode: Flow<ThemeMode>
    val themeModeValue: ThemeMode
    suspend fun setThemeMode(mode: ThemeMode)
}

class InMemoryThemeStore(
    initialMode: ThemeMode = ThemeMode.SYSTEM
) : ThemePreferenceStore {
    private val _themeMode = MutableStateFlow(initialMode)
    override val themeMode: Flow<ThemeMode> = _themeMode.asStateFlow()
    override val themeModeValue: ThemeMode get() = _themeMode.value

    override suspend fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
    }
}

class SharedPreferencesThemeStore(
    private val context: Context
) : ThemePreferenceStore {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("bantuin_theme_prefs", Context.MODE_PRIVATE)
    }

    private val _themeMode = MutableStateFlow(readMode())
    override val themeMode: Flow<ThemeMode> = _themeMode.asStateFlow()
    override val themeModeValue: ThemeMode get() = _themeMode.value

    private fun readMode(): ThemeMode {
        return try {
            val saved = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
            ThemeMode.entries.firstOrNull { it.name == saved } ?: ThemeMode.SYSTEM
        } catch (_: Exception) {
            ThemeMode.SYSTEM
        }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        try {
            prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        } catch (_: Exception) {
        }
    }

    companion object {
        private const val KEY_THEME_MODE = "theme_mode"
    }
}
