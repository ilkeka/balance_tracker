package me.ilker.balance_tracker.preferences

import android.content.Context
import me.ilker.balance_tracker.theme.ThemePreference

internal class AndroidAppPreferences(context: Context) : AppPreferences {
    private val preferences = context.applicationContext
        .getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)

    override fun themePreference(): ThemePreference = preferences
        .getString(ThemePreferenceKey, null)
        ?.let { stored -> ThemePreference.entries.firstOrNull { it.name == stored } }
        ?: ThemePreference.System

    override fun setThemePreference(preference: ThemePreference) {
        preferences
            .edit()
            .putString(ThemePreferenceKey, preference.name)
            .apply()
    }

    private companion object {
        const val PreferencesName = "app_settings"
        const val ThemePreferenceKey = "theme_preference"
    }
}
