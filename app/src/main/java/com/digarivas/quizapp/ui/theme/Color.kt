package com.digarivas.quizapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

// ---------------------------------------------------------------------------
// Paleta "grafito + menta" del rediseño.
// La interfaz es neutra; el color se reserva para datos y estados.
// ---------------------------------------------------------------------------

// Dark theme — negro puro y grises neutros, sin tinte azulado.
val Graphite = Color(0xFF000000)
val GraphiteLowest = Color(0xFF0A0A0A)
val GraphiteLow = Color(0xFF121212)
val GraphiteContainer = Color(0xFF1A1A1A)
val GraphiteHigh = Color(0xFF242424)
val GraphiteHighest = Color(0xFF2C2C2C)
val GraphiteOn = Color(0xFFF5F5F5)
val GraphiteOnVariant = Color(0xFFA1A1A6)
val GraphiteOutline = Color(0xFF8E8E93)
val GraphiteOutlineVariant = Color(0xFF2C2C2C)

val GraphitePrimary = Color(0xFFFFFFFF)
val GraphiteOnPrimary = Color(0xFF000000)
val GraphitePrimaryContainer = Color(0xFFE5E5E5)
val GraphiteOnPrimaryContainer = Color(0xFF1A1A1A)

val Mint = Color(0xFF6FFBBE)
val MintDim = Color(0xFF4EDEA3)
val MintDeep = Color(0xFF00734E)
val SoftRed = Color(0xFFFFB4AB)
val DeepRed = Color(0xFF93000A)
val PaleRed = Color(0xFFFFDAD6)

// Light theme — neutro, sin tinte azulado.
val Paper = Color(0xFFF2F2F7)
val PaperLowest = Color(0xFFFFFFFF)
val PaperLow = Color(0xFFFAFAFA)
val PaperContainer = Color(0xFFF0F0F0)
val PaperHigh = Color(0xFFE5E5E5)
val PaperHighest = Color(0xFFD1D1D6)
val PaperOn = Color(0xFF1A1A1A)
val PaperOnVariant = Color(0xFF6C6C70)
val PaperOutline = Color(0xFF8E8E93)
val PaperOutlineVariant = Color(0xFFD1D1D6)

val PaperPrimary = Color(0xFF000000)
val PaperOnPrimary = Color(0xFFFFFFFF)
val PaperPrimaryContainer = Color(0xFF1A1A1A)
val PaperOnPrimaryContainer = Color(0xFFF5F5F5)

val PaperSecondary = Color(0xFF6C6C70)
val PaperSecondaryContainer = Color(0xFFE5E5E5)
val PaperOnSecondaryContainer = Color(0xFF1A1A1A)
val PaperError = Color(0xFFBA1A1A)
val PaperErrorContainer = Color(0xFFFFDAD6)
val PaperOnErrorContainer = Color(0xFF410002)

// ---------------------------------------------------------------------------
// Colores de DATOS: un color fijo por conjunto. Se usan en el punto de la
// lista, en falladas y en el historial, nunca como decoración.
// ---------------------------------------------------------------------------
val SetRed = Color(0xFFEF4444)
val SetBlue = Color(0xFF3B82F6)
val SetAmber = Color(0xFFF59E0B)
val SetPurple = Color(0xFF8B5CF6)
val SetGreen = Color(0xFF10B981)
val SetPink = Color(0xFFEC4899)
val SetOrange = Color(0xFFF97316)
val SetGray = Color(0xFF6B7280)

private val SetPalette = listOf(
    SetRed,
    SetBlue,
    SetAmber,
    SetPurple,
    SetGreen,
    SetPink,
    SetOrange,
    SetGray
)

/** Color fijo y determinista por conjunto. */
fun getSetColor(name: String): Color {
    if (name.isBlank()) return SetGray
    return SetPalette[Math.floorMod(name.hashCode(), SetPalette.size)]
}

/** Acierto: menta en oscuro, verde profundo en claro. */
@Composable
@ReadOnlyComposable
fun successColor(): Color = MaterialTheme.colorScheme.tertiary

/** Error: rojo suave en oscuro, rojo profundo en claro. */
@Composable
@ReadOnlyComposable
fun dangerColor(): Color = MaterialTheme.colorScheme.error

/** Nota: verde si aprueba, neutro en la banda media, rojo si suspende. */
@Composable
@ReadOnlyComposable
fun scoreColor(percent: Int): Color = when {
    percent >= 80 -> MaterialTheme.colorScheme.tertiary
    percent >= 60 -> MaterialTheme.colorScheme.onSurface
    else -> MaterialTheme.colorScheme.error
}
