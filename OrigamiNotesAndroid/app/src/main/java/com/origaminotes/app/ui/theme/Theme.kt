package com.origaminotes.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

/**
 * True while the dark color scheme is active. Provided by [OrigamiNotesTheme].
 * Components need this because several accent surfaces flip both their background
 * and their foreground between themes — a single [MaterialTheme] slot can't express that.
 */
val LocalIsDarkTheme = staticCompositionLocalOf { true }

/**
 * Accent colors that are not a straight [MaterialTheme] slot: on dark they sit on a deep
 * green surface with a mint foreground, on light they invert to a mint surface with a
 * white foreground.
 */
object OrigamiAccents {
    /** Foreground on a solid [StitchGreen] surface — FAB, Rename, primary buttons. */
    val onAccent: Color
        @Composable get() = if (LocalIsDarkTheme.current) Color.Black else Color.White

    /** Container for accent chips sitting on the page background (Assign button). */
    val chipContainer: Color
        @Composable get() = if (LocalIsDarkTheme.current) StitchSurfaceVariant else StitchGreen

    /** Foreground for [chipContainer]. */
    val onChipContainer: Color
        @Composable get() = if (LocalIsDarkTheme.current) StitchGreen else Color.White

    /** Container for neutral radial-menu buttons (Folder, Resize). */
    val radialContainer: Color
        @Composable get() = if (LocalIsDarkTheme.current) StitchSurfaceDark else StitchGreen

    /** Foreground for [radialContainer] when the icon carries the accent itself. */
    val onRadialContainer: Color
        @Composable get() = if (LocalIsDarkTheme.current) StitchGreen else Color.White
}

/**
 * Bottom padding that keeps scrollable content clear of the floating island bar
 * (90dp tall + 24dp margin) with room left to comfortably tap the last item.
 */
val BottomBarInset = 140.dp

private val DarkColorScheme = darkColorScheme(
    primary          = StitchGreen,
    secondary        = MintLight,
    tertiary         = Pink80,
    background       = StitchBackgroundDark,
    surface          = StitchSurfaceDark,
    surfaceVariant   = StitchSurfaceVariant,
    onPrimary        = Color.Black,
    onSecondary      = Color.Black,
    onTertiary       = Color.White,
    onBackground     = Color(0xFFE2E8F0),
    onSurface        = Color.White,
    onSurfaceVariant = Color(0xFF94A3B8),
)

private val LightColorScheme = lightColorScheme(
    primary          = StitchGreen,
    secondary        = MintPrimary,
    tertiary         = Pink40,
    background       = StitchBackgroundLight,
    surface          = StitchSurfaceLight,
    surfaceVariant   = StitchSurfaceVariantLight,
    onPrimary        = Color.Black,
    onSecondary      = Color.Black,
    onTertiary       = Color.Black,
    onBackground     = StitchOnBackgroundLight,
    onSurface        = StitchOnSurfaceLight,
    onSurfaceVariant = StitchOnSurfaceVariantLight,
)

@Composable
fun OrigamiNotesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else      -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor     = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalIsDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = Typography,
            content     = content
        )
    }
}
