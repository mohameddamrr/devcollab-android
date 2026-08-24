package com.mohamedamr.devcollab.core.settings

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

class ThemePreferences(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val _themeMode = MutableStateFlow(
        preferences.getString(THEME_MODE_KEY, null)
            ?.let { stored -> ThemeMode.entries.firstOrNull { it.name == stored } }
            ?: ThemeMode.SYSTEM,
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        preferences.edit().putString(THEME_MODE_KEY, mode.name).apply()
        _themeMode.value = mode
    }

    private companion object {
        const val PREFERENCES_NAME = "wedevelop_appearance"
        const val THEME_MODE_KEY = "theme_mode"
    }
}
