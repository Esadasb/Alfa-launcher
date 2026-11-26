package pl.alfa.launcher.ui

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.alfa.launcher.BackgroundMode
import pl.alfa.launcher.LauncherViewModel

private val BackgroundColors = listOf(
    0xFF000000 to "Czarny",
    0xFF1C1C1E to "Grafit",
    0xFF0B1426 to "Granat",
    0xFF0E1F17 to "Butelkowa zieleń",
    0xFF240B10 to "Bordo",
    0xFFF4F2EE to "Biały",
)

private val ForegroundColors = listOf(
    0xFFFFFFFF to "Biały",
    0xFFF3EAD8 to "Kremowy",
    0xFFB8BCC4 to "Srebrny",
    0xFFFFC15E to "Bursztyn",
    0xFF7FE0C4 to "Miętowy",
    0xFF111111 to "Czarny",
)

/** Ikony na końcu rolki: ustawienia launchera i wyłączenie (powrót do launchera systemu). */
@Composable
fun RailEndButtons(vm: LauncherViewModel, modifier: Modifier = Modifier) {
    Column(modifier.width(RailWidth), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        RoundIcon(LineIcons.Gear, "Ustawienia launchera") { vm.setSettingsOpen(true) }
        RoundIcon(LineIcons.Power, "Wyłącz launcher Alfa") { vm.openHomeSettings() }
    }
}

@Composable
private fun RoundIcon(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(30.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            rememberVectorPainter(icon),
            contentDescription = description,
            modifier = Modifier.size(20.dp),
            colorFilter = ColorFilter.tint(Ink.Dim),
        )
    }
}

/** Pasek widoczny, gdy Alfa nie jest ekranem głównym (uruchomiona z ikony w systemie). */
@Composable
fun MakeDefaultBanner(vm: LauncherViewModel, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier
            .fillMaxWidth()
            .background(Ink.Black, shape)
            .border(1.dp, Ink.Faint, shape)
            .clickable { vm.openHomeSettings() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            BasicText("Włącz Alfa jako ekran główny", style = TextStyle(color = Ink.White, fontSize = 14.sp))
            BasicText("Dotknij i wybierz „Alfa” w ustawieniach telefonu", style = TextStyle(color = Ink.Dim, fontSize = 12.sp))
        }
        BasicText("›", style = TextStyle(color = Ink.White, fontSize = 22.sp))
    }
}

/** Pełnoekranowe ustawienia launchera. */
@Composable
fun SettingsScreen(vm: LauncherViewModel) {
    val open by vm.settingsOpen.collectAsStateWithLifecycle()
    val s by vm.settings.collectAsStateWithLifecycle()
    BackHandler(enabled = open) { vm.setSettingsOpen(false) }

    AnimatedVisibility(
        visible = open,
        enter = fadeIn(tween(220)) + slideInHorizontally(tween(320)) { it / 8 },
        exit = fadeOut(tween(180)) + slideOutHorizontally(tween(240)) { it / 8 },
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .background(Ink.Black)
                .pointerInput(Unit) { detectTapGestures { } }
                .windowInsetsPadding(WindowInsets.systemBars)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(36.dp)
                        .border(1.dp, Ink.Faint, CircleShape)
                        .clickable { vm.setSettingsOpen(false) },
                    contentAlignment = Alignment.Center,
                ) { BasicText("←", style = TextStyle(color = Ink.White, fontSize = 18.sp)) }
                Spacer(Modifier.width(12.dp))
                BasicText("Ustawienia", style = TextStyle(color = Ink.White, fontSize = 40.sp, fontWeight = FontWeight.Thin))
            }

            SectionLabel("Tło")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Choice("Kolor", s.mode == BackgroundMode.COLOR, Modifier.weight(1f)) {
                    vm.updateSettings { it.copy(mode = BackgroundMode.COLOR) }
                }
                Choice("Tapeta", s.mode == BackgroundMode.WALLPAPER, Modifier.weight(1f)) {
                    // Na tapecie cienie są od razu włączone – ikony lepiej się odcinają.
                    vm.updateSettings { it.copy(mode = BackgroundMode.WALLPAPER, shadows = true) }
                }
            }

            if (s.mode == BackgroundMode.COLOR) {
                SectionLabel("Kolor tła")
                Swatches(BackgroundColors, s.background) { c -> vm.updateSettings { it.copy(background = c) } }
            } else {
                SectionLabel("Rozmycie tapety")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Brak", "Lekkie", "Mocne").forEachIndexed { i, label ->
                        Choice(label, s.blur == i, Modifier.weight(1f)) { vm.updateSettings { it.copy(blur = i) } }
                    }
                }
                if (Build.VERSION.SDK_INT < 31) {
                    Note("Rozmycie działa od Androida 12. Na tym telefonie tapeta jest mocniej przyciemniona.")
                }
                SectionLabel("Przyciemnienie tapety")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Słabe" to 0.15f, "Średnie" to 0.3f, "Mocne" to 0.5f).forEach { (label, v) ->
                        Choice(label, kotlin.math.abs(s.dim - v) < 0.01f, Modifier.weight(1f)) { vm.updateSettings { it.copy(dim = v) } }
                    }
                }
            }

            SectionLabel("Kolor liter i ikon")
            Swatches(ForegroundColors, s.foreground) { c -> vm.updateSettings { it.copy(foreground = c) } }

            SectionLabel("Cienie pod ikonami i napisami")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Choice("Włączone", s.shadows, Modifier.weight(1f)) { vm.updateSettings { it.copy(shadows = true) } }
                Choice("Wyłączone", !s.shadows, Modifier.weight(1f)) { vm.updateSettings { it.copy(shadows = false) } }
            }

            Spacer(Modifier.height(6.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(Ink.Faint))
            ActionLine("Ekran główny telefonu", "Wybierz, który launcher ma być włączony") { vm.openHomeSettings() }
            Note("Ikona Alfa jest też w szufladzie aplikacji telefonu – nią włączysz launcher ponownie.")
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    BasicText(text.uppercase(), style = TextStyle(color = Ink.Dim, fontSize = 11.sp, letterSpacing = 2.sp), modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun Note(text: String) {
    BasicText(text, style = TextStyle(color = Ink.Dim, fontSize = 12.sp, lineHeight = 16.sp))
}

@Composable
private fun Choice(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier
            .height(42.dp)
            .background(if (selected) Ink.White.copy(alpha = 0.12f) else Color.Transparent, shape)
            .border(1.dp, if (selected) Ink.White.copy(alpha = 0.7f) else Ink.Faint, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(label, style = TextStyle(color = if (selected) Ink.White else Ink.Dim, fontSize = 14.sp))
    }
}

@Composable
private fun Swatches(colors: List<Pair<Long, String>>, current: Int, onPick: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        colors.forEach { (value, name) ->
            val c = value.toInt()
            val selected = c == current
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .size(44.dp)
                        .border(1.dp, if (selected) Ink.White else Color.Transparent, CircleShape)
                        .padding(4.dp)
                        .background(Color(c), CircleShape)
                        .border(1.dp, Ink.Faint, CircleShape)
                        .clickable { onPick(c) },
                )
                Spacer(Modifier.height(4.dp))
                BasicText(
                    name.substringBefore(' '),
                    style = TextStyle(color = if (selected) Ink.White else Ink.Dim, fontSize = 10.sp),
                )
            }
        }
    }
}

@Composable
private fun ActionLine(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            BasicText(title, style = TextStyle(color = Ink.White, fontSize = 15.sp))
            BasicText(subtitle, style = TextStyle(color = Ink.Dim, fontSize = 12.sp))
        }
        BasicText("›", style = TextStyle(color = Ink.White, fontSize = 22.sp))
    }
}
