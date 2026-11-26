package pl.alfa.launcher.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object Ink {
    val Black = Color(0xFF000000)
    val White = Color(0xFFFFFFFF)
    val Dim = Color(0x8CFFFFFF)
    val Faint = Color(0x2EFFFFFF)
}

/** Szerokość pasa dotyku rolki po prawej stronie. */
val RailWidth = 52.dp

/** Odstęp rolki od górnej i dolnej krawędzi. */
val RailPaddingVertical = 72.dp

fun appsCount(n: Int): String = when {
    n == 1 -> "1 aplikacja"
    n % 10 in 2..4 && n % 100 !in 12..14 -> "$n aplikacje"
    else -> "$n aplikacji"
}
