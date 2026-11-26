package com.digarivas.quizapp.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { SYSTEM_DEFAULT, LIGHT, DARK }

/**
 * Ajustes de la app sin DI ni DataStore: SharedPreferences + StateFlow.
 * El tema por defecto es oscuro, como la estética de referencia.
 */
class SettingsManager private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _theme = MutableStateFlow(getTheme())
    val theme: StateFlow<ThemeMode> = _theme.asStateFlow()

    fun getTheme(): ThemeMode {
        return when (prefs.getString(KEY_THEME, null)) {
            "LIGHT" -> ThemeMode.LIGHT
            "SYSTEM_DEFAULT" -> ThemeMode.SYSTEM_DEFAULT
            else -> ThemeMode.DARK
        }
    }

    fun setTheme(theme: ThemeMode) {
        prefs.edit().putString(KEY_THEME, theme.name).apply()
        _theme.value = theme
    }

    companion object {
        private const val KEY_THEME = "theme"

        @Volatile
        private var instance: SettingsManager? = null

        fun getInstance(context: Context): SettingsManager {
            return instance ?: synchronized(this) {
                instance ?: SettingsManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
