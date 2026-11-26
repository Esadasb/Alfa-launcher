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
    private var fgColor by mutableStateOf(Color.White)
    private var surfaceColor by mutableStateOf(Color.Black)
    private var pageColor by mutableStateOf(Color.Black)
    private var shadowsOn by mutableStateOf(false)

    /** Kolor liter, linii i konturów ikon. */
    val White: Color get() = fgColor

    /** Kolor powierzchni: panele, karty, „bańka” rolki. */
    val Black: Color get() = surfaceColor

    /** Tło ekranu głównego (przy tapecie półprzezroczyste przyciemnienie). */
    val Page: Color get() = pageColor

    val Dim: Color get() = fgColor.copy(alpha = 0.55f)
    val Faint: Color get() = fgColor.copy(alpha = 0.18f)

    /** Czy rysować cienie pod ikonami i napisami (dla kontrastu na tapecie). */
    val Shadows: Boolean get() = shadowsOn

    /** Cień tekstu albo null, gdy cienie są wyłączone. */
    val TextShadow: Shadow? get() = if (shadowsOn) Shadow(Color.Black.copy(alpha = 0.75f), Offset(0f, 2f), 8f) else null

    fun apply(s: LauncherSettings, blurAvailable: Boolean) {
        fgColor = Color(s.foreground)
        shadowsOn = s.shadows
        when (s.mode) {
            BackgroundMode.COLOR -> {
                pageColor = Color(s.background)
                surfaceColor = Color(s.background)
            }
            BackgroundMode.WALLPAPER -> {
                // Bez rozmycia przyciemniamy mocniej, żeby ikony były czytelne.
                val dim = s.dim + if (blurAvailable && s.blur > 0) 0f else 0.2f
                pageColor = Color.Black.copy(alpha = dim.coerceIn(0f, 0.85f))
                surfaceColor = Color(0xF00A0A0A)
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
