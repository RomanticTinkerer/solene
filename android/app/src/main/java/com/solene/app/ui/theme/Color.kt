package com.solene.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Solene color palette — evolved from the original drip. theme.
 *
 * Primary:   Purple tones  (from drip's #3A2671)
 * Secondary: Turquoise     (from drip's #69CBC1 / #CFECEA)
 * Tertiary:  Orange accent  (from drip's #F38337)
 */

// — Primary Purple family —
val SolenePurple = Color(0xFF6750A4)
val SolenePurpleLight = Color(0xFF938EB2)
val SolenePurpleDark = Color(0xFF3A2671)
val SoleneOnPurple = Color(0xFFFFFFFF)

// — Secondary Turquoise family —
val SoleneTurquoise = Color(0xFF69CBC1)
val SoleneTurquoiseLight = Color(0xFFCFECEA)
val SoleneTurquoiseDark = Color(0xFF3DA69C)
val SoleneOnTurquoise = Color(0xFF003733)

// — Tertiary Orange family —
val SoleneOrange = Color(0xFFF38337)
val SoleneOrangeLight = Color(0xFFFFBE8A)
val SoleneOrangeDark = Color(0xFFBC6642)
val SoleneOnOrange = Color(0xFFFFFFFF)

// — Surface / Background —
val SoleneSurfaceLight = Color(0xFFFEF7FF)
val SoleneOnSurfaceLight = Color(0xFF1D1B20)
val SoleneSurfaceContainerLight = Color(0xFFF3EDF7)
val SoleneSurfaceContainerHighLight = Color(0xFFECE6F0)

val SoleneSurfaceDark = Color(0xFF141218)
val SoleneOnSurfaceDark = Color(0xFFE6E0E9)
val SoleneSurfaceContainerDark = Color(0xFF211F26)
val SoleneSurfaceContainerHighDark = Color(0xFF2B2930)

// — Error —
val SoleneError = Color(0xFFB3261E)
val SoleneOnError = Color(0xFFFFFFFF)
val SoleneErrorDark = Color(0xFFF2B8B5)
val SoleneOnErrorDark = Color(0xFF601410)

// — Symptom tracking indicator colors (matching original drip icon colors) —
object SymptomColors {
    val bleeding = Color(0xFFC3000D)
    val mucus = Color(0xFF6A7B98)
    val cervix = Color(0xFFDBB40C)
    val sex = Color(0xFF6F2565)
    val desire = Color(0xFF9E346C)
    val pain = Color(0xFFBCCD67)
    val mood = Color(0xFFBC6642)
    val note = Color(0xFF6CA299)
}
