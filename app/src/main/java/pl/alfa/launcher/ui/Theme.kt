package pl.alfa.launcher.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.unit.dp
import pl.alfa.launcher.BackgroundMode
import pl.alfa.launcher.LauncherSettings

/**
 * Kolory launchera. Zmieniają się na żywo z ustawień (kolor tła, kolor liter, tapeta),
 * a cały interfejs odczytuje je stąd, więc od razu się przerysowuje.
 */
object Ink {
    private var fg by mutableStateOf(Color.White)
    private var surface by mutableStateOf(Color.Black)
    private var page by mutableStateOf(Color.Black)
    private var shadows by mutableStateOf(false)

    /** Kolor liter, linii i konturów ikon. */
    val White: Color get() = fg

    /** Kolor powierzchni: panele, karty, „bańka” rolki. */
    val Black: Color get() = surface

    /** Tło ekranu głównego (przy tapecie półprzezroczyste przyciemnienie). */
    val Page: Color get() = page

    val Dim: Color get() = fg.copy(alpha = 0.55f)
    val Faint: Color get() = fg.copy(alpha = 0.18f)

    /** Czy rysować cienie pod ikonami i napisami (dla kontrastu na tapecie). */
    val Shadows: Boolean get() = shadows

    /** Cień tekstu albo null, gdy cienie są wyłączone. */
    val TextShadow: Shadow? get() = if (shadows) Shadow(Color.Black.copy(alpha = 0.75f), Offset(0f, 2f), 8f) else null

    fun apply(s: LauncherSettings, blurAvailable: Boolean) {
        fg = Color(s.foreground)
        shadows = s.shadows
        when (s.mode) {
            BackgroundMode.COLOR -> {
                page = Color(s.background)
                surface = Color(s.background)
            }
            BackgroundMode.WALLPAPER -> {
                // Bez rozmycia przyciemniamy mocniej, żeby ikony były czytelne.
                val dim = s.dim + if (blurAvailable && s.blur > 0) 0f else 0.2f
                page = Color.Black.copy(alpha = dim.coerceIn(0f, 0.85f))
                surface = Color(0xF00A0A0A)
            }
        }
    }
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
