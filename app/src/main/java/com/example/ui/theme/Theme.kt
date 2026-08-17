package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = ThemePrimaryDark,
    onPrimary = ThemeOnPrimaryDark,
    primaryContainer = ThemePrimaryContainerDark,
    onPrimaryContainer = ThemeOnPrimaryContainerDark,
    secondary = ThemeSecondaryContainer,
    onSecondary = ThemeOnSecondaryContainer,
    tertiary = ThemeTertiary,
    background = ThemeBackgroundDark,
    onBackground = ThemeOnBackgroundDark,
    surface = ThemeSurfaceDark,
    onSurface = ThemeOnSurfaceDark,
    surfaceVariant = ThemeSurfaceVariantDark,
    onSurfaceVariant = ThemeOnSurfaceVariantDark,
    outline = ThemeOutline,
    outlineVariant = ThemeOutlineVariant,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = ThemePrimary,
    onPrimary = ThemeOnPrimary,
    primaryContainer = ThemePrimaryContainer,
    onPrimaryContainer = ThemeOnPrimaryContainer,
    secondary = ThemeSecondary,
    onSecondary = ThemeOnSecondary,
    secondaryContainer = ThemeSecondaryContainer,
    onSecondaryContainer = ThemeOnSecondaryContainer,
    tertiary = ThemeTertiary,
    tertiaryContainer = ThemeTertiaryContainer,
    onTertiaryContainer = ThemeOnTertiaryContainer,
    background = ThemeBackground,
    onBackground = ThemeOnBackground,
    surface = ThemeSurface,
    onSurface = ThemeOnSurface,
    surfaceVariant = ThemeSurfaceVariant,
    onSurfaceVariant = ThemeOnSurfaceVariant,
    outline = ThemeOutline,
    outlineVariant = ThemeOutlineVariant,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Keep intentional Bold Typography palette
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

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

