package com.solene.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp

/**
 * Solene Typography — 5-size expressive scale.
 *
 * Maps the M3 type roles to a clear five-tier hierarchy:
 *   XLarge (display)  → Hero headings, splash text
 *   Large  (headline) → Screen titles, major sections
 *   Medium (title)    → Card titles, section headers
 *   Small  (body)     → Body copy, descriptions
 *   XSmall (label)    → Captions, button labels, overlines
 */

private val googleFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = com.solene.app.R.array.com_google_android_gms_fonts_certs
)

private val outfitFont = GoogleFont("Outfit")

private val OutfitFamily = FontFamily(
    Font(googleFont = outfitFont, fontProvider = googleFontProvider, weight = FontWeight.Light),
    Font(googleFont = outfitFont, fontProvider = googleFontProvider, weight = FontWeight.Normal),
    Font(googleFont = outfitFont, fontProvider = googleFontProvider, weight = FontWeight.Medium),
    Font(googleFont = outfitFont, fontProvider = googleFontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = outfitFont, fontProvider = googleFontProvider, weight = FontWeight.Bold),
)

// Fallback to default sans-serif if Google Fonts unavailable
private val AppFontFamily = try {
    OutfitFamily
} catch (_: Exception) {
    FontFamily.Default
}

/**
 * Five-size expressive button scale dimensions for reuse across the app.
 */
object ButtonScale {
    val xSmall = 28.sp   // Icon-only action buttons
    val small = 32.sp    // Secondary card actions
    val medium = 40.sp   // Standard actions ("Add Data")
    val large = 48.sp    // Primary CTA ("Log Today")
    val xLarge = 56.sp   // Hero buttons on home screen
}

val SoleneTypography = Typography(
    // XLarge — Display: Hero headings
    displayLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        lineHeight = 48.sp,
        letterSpacing = (-0.25).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 42.sp,
        letterSpacing = 0.sp,
    ),
    displaySmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp,
    ),

    // Large — Headline: Screen titles
    headlineLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 34.sp,
        letterSpacing = 0.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 30.sp,
        letterSpacing = 0.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
    ),

    // Medium — Title: Card headers, section headers
    titleLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),

    // Small — Body: Text content
    bodyLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    ),

    // XSmall — Label: Buttons, captions
    labelLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp,
    ),
)
