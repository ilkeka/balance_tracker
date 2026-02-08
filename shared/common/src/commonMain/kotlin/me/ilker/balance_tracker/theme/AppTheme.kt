package me.ilker.balance_tracker.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

val lightColorScheme = lightColorScheme(
    primary = PrimaryColor,
    primaryContainer = PrimaryContainerColor,
    tertiary = TertiaryColor,
    tertiaryContainer = TertiaryContainerColor,
    surface = SurfaceColor,
    onSurface = OnSurfaceColor,
    background = BackgroundColor,
    errorContainer = ErrorContainerColor
)

val darkColorScheme = darkColorScheme(
    primary = PrimaryDarkColor,
    primaryContainer = PrimaryDarkContainerColor,
    tertiary = TertiaryDarkColor,
    tertiaryContainer = TertiaryDarkContainerColor,
    surface = SurfaceDarkColor,
    onSurface = OnSurfaceDarkColor,
    background = BackgroundDarkColor,
    errorContainer = ErrorContainerDarkColor
)

@Composable
internal fun AppTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = if (isSystemInDarkTheme()) {
        darkColorScheme
    } else {
        lightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
