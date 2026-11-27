package me.ilker.balance_tracker.preferences

import java.util.prefs.Preferences
import me.ilker.balance_tracker.theme.ThemePreference

internal class JvmAppPreferences : AppPreferences {
    private val preferences = Preferences.userRoot()
        .node("me")
        .node("ilker")
        .node("balance_tracker")

    override fun themePreference(): ThemePreference = preferences
        .get(ThemePreferenceKey, null)
        ?.let { stored -> ThemePreference.entries.firstOrNull { it.name == stored } }
        ?: ThemePreference.System

    override fun setThemePreference(preference: ThemePreference) {
        preferences.put(ThemePreferenceKey, preference.name)

        try {
            preferences.flush()
        } catch (_: Exception) {
            // The preference is still kept in memory and will be written when possible.
        }
    }

    private companion object {
        const val ThemePreferenceKey = "theme_preference"
    }
}
