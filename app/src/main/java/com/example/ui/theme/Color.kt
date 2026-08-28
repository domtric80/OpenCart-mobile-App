package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// High-Contrast Light Theme Palette (WCAG AAA compliant)
val ThemeBackground = Color(0xFFF1F5F9) // Slightly deeper light background for better separation
val ThemeOnBackground = Color(0xFF0F172A) // Near jet-black
val ThemeSurface = Color(0xFFFFFFFF)
val ThemeOnSurface = Color(0xFF0F172A) // Near jet-black
val ThemeSurfaceVariant = Color(0xFFE2E8F0)
val ThemeOnSurfaceVariant = Color(0xFF1E293B) // Dark Charcoal (High contrast)

val ThemePrimary = Color(0xFF3730A3) // Deep rich Indigo
val ThemeOnPrimary = Color(0xFFFFFFFF)
val ThemePrimaryContainer = Color(0xFFE0E7FF)
val ThemeOnPrimaryContainer = Color(0xFF1E1B4B)

val ThemeSecondary = Color(0xFF334155) // Deep Slate
val ThemeOnSecondary = Color(0xFFFFFFFF)
val ThemeSecondaryContainer = Color(0xFFE2E8F0)
val ThemeOnSecondaryContainer = Color(0xFF0F172A)

val ThemeTertiary = Color(0xFF0F766E) // Deep Teal
val ThemeTertiaryContainer = Color(0xFFCCFBF1)
val ThemeOnTertiaryContainer = Color(0xFF134E4A)

val ThemeOutline = Color(0xFF475569) // Darker outline for clarity
val ThemeOutlineVariant = Color(0xFF94A3B8)

val BrandAvatarBg = Color(0xFFE0E7FF)
val BrandAvatarText = Color(0xFF312E81)
val LabelPurple = Color(0xFF4338CA)
// Base static fallbacks
val CardSurfaceLightStatic = Color(0xFFF3F4F6)
val CardSurfacePureStatic = Color(0xFFFFFFFF)

// Dynamic theme-aware card surfaces
val CardSurfacePure: Color
    @Composable
    get() = MaterialTheme.colorScheme.surface

val CardSurfaceLight: Color
    @Composable
    get() = MaterialTheme.colorScheme.surfaceVariant
val TrendGreen = Color(0xFF15803D)
val TrendGreenLight = Color(0xFFDCFCE7)
val AlertRed = Color(0xFFB91C1C)
val AlertRedContainer = Color(0xFFFEE2E2)

// Semantic colors with deep, readable foregrounds
val ColorSemanticGreen = Color(0xFF15803D)
val ColorSemanticOrange = Color(0xFFC2410C)
val ColorSemanticRed = Color(0xFFB91C1C)

// Order Status Palette - High Contrast text (WCAG AAA)
val StatusPendingGold = Color(0xFF92400E) // Dark amber text
val StatusPendingGoldBg = Color(0xFFFEF3C7) // Soft amber bg
val StatusConfirmedBlue = Color(0xFF0F766E) // Dark teal text
val StatusConfirmedBlueBg = Color(0xFFCCFBF1) // Soft teal bg
val StatusProcessingPurple = Color(0xFF6D28D9) // Dark purple text
val StatusProcessingPurpleBg = Color(0xFFEDE9FE) // Soft purple bg
val StatusShippedGreen = Color(0xFF0369A1) // Deep sky blue text
val StatusShippedGreenBg = Color(0xFFE0F2FE) // Soft sky bg
val StatusDeliveredGreen = Color(0xFF15803D) // Deep emerald green text
val StatusDeliveredGreenBg = Color(0xFFDCFCE7) // Soft emerald bg
val StatusAlertRed = Color(0xFFB91C1C) // Deep crimson text
val StatusAlertRedBg = Color(0xFFFEE2E2) // Soft crimson bg

// Brand Compatibility Palette
val OpenCartBluePrimary = Color(0xFF1D4ED8)
val OpenCartBlueDark = Color(0xFF1E3A8A)
val OpenCartPurple = Color(0xFF6D28D9)
val OpenCartAccentOrange = Color(0xFFC2410C)
val OpenCartSuccess = Color(0xFF15803D)
val OpenCartSuccessContainer = Color(0xFFDCFCE7)
val OpenCartWarning = Color(0xFFB45309)
val OpenCartWarningContainer = Color(0xFFFEF3C7)
val OpenCartDanger = Color(0xFFB91C1C)
val OpenCartDangerContainer = Color(0xFFFEE2E2)

// Dark Theme counterparts
val ThemeBackgroundDark = Color(0xFF0F172A)
val ThemeOnBackgroundDark = Color(0xFFF8FAFC)
val ThemeSurfaceDark = Color(0xFF1E293B)
val ThemeOnSurfaceDark = Color(0xFFF8FAFC)
val ThemeSurfaceVariantDark = Color(0xFF334155)
val ThemeOnSurfaceVariantDark = Color(0xFFE2E8F0)

val ThemePrimaryDark = Color(0xFF818CF8)
// Several legacy components still use the fixed deep-indigo ThemePrimary as
// their container in dark mode. White is the only safe shared foreground for
// both that color and Material buttons using the dark color scheme.
val ThemeOnPrimaryDark = Color(0xFFFFFFFF)
val ThemePrimaryContainerDark = Color(0xFF3730A3)
val ThemeOnPrimaryContainerDark = Color(0xFFE0E7FF)

