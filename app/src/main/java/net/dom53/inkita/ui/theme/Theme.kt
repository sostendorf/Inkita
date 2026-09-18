package net.dom53.inkita.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * Dark scheme for the library. Three background tiers (background → surface →
 * surfaceContainerHigh) give cards something to lift off, which is what makes a
 * wall of cover art read as a grid rather than a collage.
 */
private val DarkColorScheme =
    darkColorScheme(
        primary = ShelfPrimary,
        onPrimary = Color.White,
        primaryContainer = ShelfPrimary,
        onPrimaryContainer = Color.White,
        secondary = ShelfSecondary,
        onSecondary = Color.White,
        secondaryContainer = ShelfSurfaceRaised,
        onSecondaryContainer = ShelfOnSurface,
        tertiary = ShelfAccent,
        onTertiary = Color(0xFF3A1F14),
        background = ShelfBackground,
        onBackground = ShelfOnSurface,
        surface = ShelfSurface,
        onSurface = ShelfOnSurface,
        surfaceVariant = ShelfSurfaceRaised,
        onSurfaceVariant = ShelfOnSurfaceMuted,
        surfaceContainerLowest = ShelfBackground,
        surfaceContainerLow = ShelfSurface,
        surfaceContainer = ShelfSurface,
        surfaceContainerHigh = ShelfSurfaceRaised,
        surfaceContainerHighest = ShelfSurfaceRaised,
        outline = ShelfOutline,
        outlineVariant = ShelfOutline,
        error = ShelfError,
        onError = Color.White,
        scrim = Color.Black,
    )

private val LightColorScheme =
    lightColorScheme(
        primary = ShelfLightPrimary,
        onPrimary = Color.White,
        secondary = ShelfSecondary,
        onSecondary = Color.White,
        tertiary = ShelfAccent,
        background = ShelfLightBackground,
        onBackground = ShelfLightOnSurface,
        surface = ShelfLightSurface,
        onSurface = ShelfLightOnSurface,
        surfaceVariant = ShelfLightSurfaceRaised,
        onSurfaceVariant = ShelfLightOnSurfaceMuted,
        surfaceContainerLowest = ShelfLightSurface,
        surfaceContainerLow = ShelfLightSurface,
        surfaceContainer = ShelfLightSurfaceRaised,
        surfaceContainerHigh = ShelfLightSurfaceRaised,
        surfaceContainerHighest = ShelfLightSurfaceRaised,
        outline = ShelfLightOutline,
        outlineVariant = ShelfLightOutline,
        error = ShelfError,
        onError = Color.White,
    )

@Composable
fun InkitaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Wallpaper-derived color is off by default: the point of this fork is a
    // consistent dark library, not one that changes with the home screen.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }

            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
