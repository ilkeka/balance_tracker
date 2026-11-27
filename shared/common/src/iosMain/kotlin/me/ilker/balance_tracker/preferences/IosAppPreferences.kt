package me.ilker.balance_tracker.preferences

import me.ilker.balance_tracker.theme.ThemePreference
import platform.Foundation.NSUserDefaults

internal class IosAppPreferences : AppPreferences {
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun themePreference(): ThemePreference = defaults
        .stringForKey(ThemePreferenceKey)
        ?.let { stored -> ThemePreference.entries.firstOrNull { it.name == stored } }
        ?: ThemePreference.System

    override fun setThemePreference(preference: ThemePreference) {
        defaults.setObject(preference.name, forKey = ThemePreferenceKey)
    }

    private companion object {
        const val ThemePreferenceKey = "theme_preference"
    }
}
