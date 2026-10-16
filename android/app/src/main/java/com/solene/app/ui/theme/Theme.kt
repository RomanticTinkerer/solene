package com.solene.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Solene Theme — follows device theme and dynamic colors.
 *
 * On Android 12+ (S): uses dynamic color from the user's wallpaper.
 * On older devices: falls back to the custom Solene palette derived from drip.
 */

private val SoleneLightColorScheme = lightColorScheme(
    primary = SolenePurple,
    onPrimary = SoleneOnPurple,
    primaryContainer = SolenePurpleLight,
    onPrimaryContainer = SolenePurpleDark,

    secondary = SoleneTurquoise,
    onSecondary = SoleneOnTurquoise,
    secondaryContainer = SoleneTurquoiseLight,
    onSecondaryContainer = SoleneTurquoiseDark,

    tertiary = SoleneOrange,
    onTertiary = SoleneOnOrange,
    tertiaryContainer = SoleneOrangeLight,
    onTertiaryContainer = SoleneOrangeDark,

    surface = SoleneSurfaceLight,
    onSurface = SoleneOnSurfaceLight,
    surfaceContainer = SoleneSurfaceContainerLight,
    surfaceContainerHigh = SoleneSurfaceContainerHighLight,

    error = SoleneError,
    onError = SoleneOnError,
)

private val SoleneDarkColorScheme = darkColorScheme(
    primary = SolenePurpleLight,
    onPrimary = SolenePurpleDark,
    primaryContainer = SolenePurple,
    onPrimaryContainer = SoleneOnPurple,

    secondary = SoleneTurquoiseLight,
    onSecondary = SoleneTurquoiseDark,
    secondaryContainer = SoleneTurquoise,
    onSecondaryContainer = SoleneOnTurquoise,

    tertiary = SoleneOrangeLight,
    onTertiary = SoleneOrangeDark,
    tertiaryContainer = SoleneOrange,
    onTertiaryContainer = SoleneOnOrange,

    surface = SoleneSurfaceDark,
    onSurface = SoleneOnSurfaceDark,
    surfaceContainer = SoleneSurfaceContainerDark,
    surfaceContainerHigh = SoleneSurfaceContainerHighDark,

    error = SoleneErrorDark,
    onError = SoleneOnErrorDark,
)

@Composable
fun SoleneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // Dynamic color on Android 12+
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> SoleneDarkColorScheme
        else -> SoleneLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SoleneTypography,
        content = content,
    )
}
