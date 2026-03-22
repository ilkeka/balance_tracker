package me.ilker.balance_tracker.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

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
    surfaceVariant = SurfaceVariantColor,
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
    surfaceVariant = SurfaceVariantDarkColor,
    onSurface = OnSurfaceDarkColor,
    onSurfaceVariant = OnSurfaceVariantDarkColor,
    background = BackgroundDarkColor,
    error = ErrorDarkColor,
    errorContainer = ErrorDarkContainerColor,
    onError = OnErrorDarkColor,
    onErrorContainer = OnErrorDarkContainerColor
)

@Composable
fun AppTheme(
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
