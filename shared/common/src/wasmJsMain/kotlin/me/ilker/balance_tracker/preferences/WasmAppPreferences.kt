package me.ilker.balance_tracker.preferences

import kotlin.js.ExperimentalWasmJsInterop
import me.ilker.balance_tracker.theme.ThemePreference

internal class WasmAppPreferences : AppPreferences {
    override fun themePreference(): ThemePreference {
        val stored = readThemePreference()

        return ThemePreference.entries.firstOrNull { it.name == stored } ?: ThemePreference.System
    }

    override fun setThemePreference(preference: ThemePreference) {
        writeThemePreference(value = preference.name)
    }
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun readThemePreference(): String? =
    js("window.localStorage.getItem('theme_preference')")

@OptIn(ExperimentalWasmJsInterop::class)
private fun writeThemePreference(value: String) {
    js("window.localStorage.setItem('theme_preference', value)")
}
