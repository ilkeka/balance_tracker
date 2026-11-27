package me.ilker.balance_tracker.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

val lightColorScheme = lightColorScheme(
    primary = PrimaryColor,
    primaryContainer = PrimaryContainerColor,
    onPrimary = OnPrimaryColor,
    onPrimaryContainer = OnPrimaryContainerColor,
    secondary = SecondaryColor,
    secondaryContainer = SecondaryContainerColor,
    onSecondary = OnSecondaryColor,
    onSecondaryContainer = OnSecondaryContainerColor,
    tertiary = TertiaryColor,
    tertiaryContainer = TertiaryContainerColor,
    onTertiary = OnTertiaryColor,
    onTertiaryContainer = OnTertiaryContainerColor,
    outline = OutlineColor,
    outlineVariant = OutlineVariantColor,
    surface = SurfaceColor,
    surfaceDim = SurfaceDimColor,
    surfaceVariant = SurfaceVariantColor,
    surfaceContainerLowest = SurfaceContainerLowestColor,
    surfaceContainerLow = SurfaceContainerLowColor,
    surfaceContainer = SurfaceContainerColor,
    surfaceContainerHigh = SurfaceContainerHighColor,
    surfaceContainerHighest = SurfaceContainerHighestColor,
    onSurface = OnSurfaceColor,
    onSurfaceVariant = OnSurfaceVariantColor,
    background = BackgroundColor,
    error = ErrorColor,
    errorContainer = ErrorContainerColor,
    onError = OnErrorColor,
    onErrorContainer = OnErrorContainerColor,
)

val darkColorScheme = darkColorScheme(
    primary = PrimaryDarkColor,
    onPrimary = OnPrimaryDarkColor,
    primaryContainer = PrimaryDarkContainerColor,
    onPrimaryContainer = OnPrimaryDarkContainerColor,
    secondary = SecondaryDarkColor,
    secondaryContainer = SecondaryDarkContainerColor,
    onSecondary = OnSecondaryDarkColor,
    onSecondaryContainer = OnSecondaryDarkContainerColor,
    tertiary = TertiaryDarkColor,
    tertiaryContainer = TertiaryDarkContainerColor,
    onTertiary = OnTertiaryDarkColor,
    onTertiaryContainer = OnTertiaryDarkContainerColor,
    outline = OutlineDarkColor,
    outlineVariant = OutlineVariantDarkColor,
    surface = SurfaceDarkColor,
    surfaceDim = SurfaceDarkDimColor,
    surfaceVariant = SurfaceVariantDarkColor,
    surfaceContainerLowest = SurfaceDarkContainerLowestColor,
    surfaceContainerLow = SurfaceDarkContainerLowColor,
    surfaceContainer = SurfaceDarkContainerColor,
    surfaceContainerHigh = SurfaceDarkContainerHighColor,
    surfaceContainerHighest = SurfaceDarkContainerHighestColor,
    onSurface = OnSurfaceDarkColor,
    onSurfaceVariant = OnSurfaceVariantDarkColor,
    background = BackgroundDarkColor,
    error = ErrorDarkColor,
    errorContainer = ErrorDarkContainerColor,
    onError = OnErrorDarkColor,
    onErrorContainer = OnErrorDarkContainerColor
)

/** Whether the currently composed theme resolves to a dark color scheme. */
val LocalIsDarkTheme = staticCompositionLocalOf { false }

@Composable
fun AppTheme(
    themePreference: ThemePreference = ThemePreference.System,
    content: @Composable () -> Unit
) {
    val isDarkTheme = when (themePreference) {
        ThemePreference.System -> isSystemInDarkTheme()
        ThemePreference.Light -> false
        ThemePreference.Dark -> true
    }

    CompositionLocalProvider(LocalIsDarkTheme provides isDarkTheme) {
        MaterialTheme(
            colorScheme = if (isDarkTheme) darkColorScheme else lightColorScheme,
            content = content
        )
    }
}
