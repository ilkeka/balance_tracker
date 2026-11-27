package me.ilker.balance_tracker.preferences

import me.ilker.balance_tracker.theme.ThemePreference

interface AppPreferences {
    fun themePreference(): ThemePreference

    fun setThemePreference(preference: ThemePreference)
}
