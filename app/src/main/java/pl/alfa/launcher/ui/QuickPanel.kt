package pl.alfa.launcher.ui

import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import pl.alfa.launcher.AppEntry
import pl.alfa.launcher.LauncherViewModel

/** Wysokość pasa przy dolnej krawędzi, z którego wysuwa się panel. */
val SwipeZoneHeight = 44.dp

private val PanelSpring = spring<Float>(dampingRatio = 0.9f, stiffness = 380f)

/**
 * Panel szybkiego dostępu: przesunięcie z dołu do góry wysuwa Telefon, Wiadomości
 * i 8 programowalnych skrótów. Pusty skrót – dotknij, żeby dodać. Zajęty – przytrzymaj,
 * żeby zmienić aplikację; ikona zmienia się razem ze skrótem.
 */
@Composable
fun QuickPanel(vm: LauncherViewModel, modifier: Modifier = Modifier) {
    val open by vm.panelOpen.collectAsStateWithLifecycle()
    val shortcuts by vm.shortcuts.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val progress = remember { Animatable(0f) } // 0 = schowany, 1 = otwarty
    var sheetHeight by remember { mutableFloatStateOf(1f) }
    var picking by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(open) {
        progress.animateTo(if (open) 1f else 0f, PanelSpring)
        if (!open) picking = null
    }
    BackHandler(enabled = open) {
        if (picking != null) picking = null else vm.setPanel(false)
    }

    fun settle(velocity: Float) {
        val target = when {
            velocity < -800f -> true
            velocity > 800f -> false
            else -> progress.value > 0.4f
        }
        if (target == open) {
            scope.launch { progress.animateTo(if (target) 1f else 0f, PanelSpring, initialVelocity = -velocity / sheetHeight) }
        } else {
            vm.setPanel(target)
        }
    }

    val dragState = rememberDraggableState { delta ->
        scope.launch { progress.snapTo((progress.value - delta / sheetHeight).coerceIn(0f, 1f)) }
    }
    val drag = Modifier.draggable(
        state = dragState,
        orientation = Orientation.Vertical,
        onDragStopped = { velocity -> settle(velocity) },
    )

    Box(modifier.fillMaxSize()) {
        // Przyciemnienie tła – dotknięcie zamyka panel.
        if (progress.value > 0.001f) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = progress.value * 0.75f }
                    .background(Ink.Black)
                    .pointerInputTap { vm.setPanel(false) },
            )
        }

        // Pas przy dolnej krawędzi z delikatną strzałką.
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .height(SwipeZoneHeight)
                .then(drag)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    vm.setPanel(true)
                },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(
                Modifier
                    .size(width = 22.dp, height = 12.dp)
                    .graphicsLayer { alpha = 0.45f * (1f - progress.value) },
            ) {
                val w = 1.5.dp.toPx()
                drawLine(Ink.White, Offset(2.dp.toPx(), size.height - 2.dp.toPx()), Offset(size.width / 2, 3.dp.toPx()), w, StrokeCap.Round)
                drawLine(Ink.White, Offset(size.width / 2, 3.dp.toPx()), Offset(size.width - 2.dp.toPx(), size.height - 2.dp.toPx()), w, StrokeCap.Round)
            }
        }

        // Sam panel.
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .onSizeChanged { sheetHeight = it.height.toFloat().coerceAtLeast(1f) }
                .graphicsLayer { translationY = (1f - progress.value) * (sheetHeight + 4f) }
                .background(Ink.Black, RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                .border(1.dp, Ink.Faint, RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                .then(drag)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(start = 22.dp, end = 22.dp, top = 10.dp, bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 36.dp, height = 4.dp)
                    .background(Ink.White.copy(alpha = 0.35f), RoundedCornerShape(2.dp)),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickAction(LineIcons.Phone, "Telefon", Modifier.weight(1f)) {
                    vm.openDialer(); vm.setPanel(false)
                }
                QuickAction(LineIcons.Messages, "Wiadomości", Modifier.weight(1f)) {
                    vm.openMessages(); vm.setPanel(false)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                BasicText("SKRÓTY", style = TextStyle(color = Ink.Dim, fontSize = 11.sp, letterSpacing = 2.sp))
                BasicText("przytrzymaj, aby zmienić", style = TextStyle(color = Ink.White.copy(alpha = 0.35f), fontSize = 11.sp))
            }
            shortcuts.chunked(4).forEachIndexed { row, items ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items.forEachIndexed { col, entry ->
                        val slot = row * 4 + col
                        ShortcutSlot(
                            entry = entry,
                            vm = vm,
                            modifier = Modifier.weight(1f),
                            onAdd = { picking = slot },
                            onChange = { picking = slot },
                            onLaunched = { vm.setPanel(false) },
                        )
                    }
                }
            }
        }

        // Wybór aplikacji dla skrótu.
        AnimatedVisibility(
            visible = picking != null,
            enter = fadeIn(tween(200)) + slideInVertically(tween(280)) { it / 30 },
            exit = fadeOut(tween(160)) + slideOutVertically(tween(200)) { it / 30 },
        ) {
            val slot = picking ?: 0
            AppPicker(
                vm = vm,
                slot = slot,
                current = shortcuts.getOrNull(slot),
                onPick = { vm.setShortcut(slot, it); picking = null },
                onClose = { picking = null },
            )
        }
    }
}

private fun Modifier.pointerInputTap(onTap: () -> Unit): Modifier =
    pointerInput(Unit) { detectTapGestures { onTap() } }

@Composable
private fun QuickAction(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f, spring(0.6f, Spring.StiffnessMedium), label = "qa")
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier
            .height(72.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .background(if (pressed) Ink.White.copy(alpha = 0.06f) else Ink.Black, shape)
            .border(1.dp, Ink.Faint, shape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(rememberVectorPainter(icon), contentDescription = null, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(10.dp))
        BasicText(label, style = TextStyle(color = Ink.White, fontSize = 14.sp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ShortcutSlot(
    entry: AppEntry?,
    vm: LauncherViewModel,
    modifier: Modifier,
    onAdd: () -> Unit,
    onChange: () -> Unit,
    onLaunched: () -> Unit,
) {
    // Animacja „podmiany” ikony przy zmianie przypisanej aplikacji.
    AnimatedContent(
        targetState = entry,
        contentKey = { it?.key },
        transitionSpec = {
            (fadeIn(tween(220)) + scaleIn(spring(0.55f, Spring.StiffnessMediumLow), initialScale = 0.4f)) togetherWith
                fadeOut(tween(120))
        },
        label = "slot",
        modifier = modifier,
    ) { app ->
        if (app != null) {
            AppTile(
                entry = app,
                vm = vm,
                iconSize = 48.dp,
                labelSize = 11.sp,
                maxLines = 1,
                onLaunched = onLaunched,
                onLongClick = onChange,
            )
        } else {
            val view = LocalView.current
            Column(
                Modifier.combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onAdd,
                    onLongClick = { view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS); onAdd() },
                ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Canvas(Modifier.size(48.dp)) {
                    val stroke = 1.dp.toPx()
                    drawCircle(
                        Ink.White.copy(alpha = 0.4f),
                        radius = size.minDimension * 0.35f,
                        style = Stroke(stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 4.dp.toPx()))),
                    )
                    val c = center
                    val a = size.minDimension * 0.125f
                    drawLine(Ink.White.copy(alpha = 0.4f), Offset(c.x - a, c.y), Offset(c.x + a, c.y), stroke, StrokeCap.Round)
                    drawLine(Ink.White.copy(alpha = 0.4f), Offset(c.x, c.y - a), Offset(c.x, c.y + a), stroke, StrokeCap.Round)
                }
                Spacer(Modifier.height(8.dp))
                BasicText("Dodaj", style = TextStyle(color = Ink.White.copy(alpha = 0.35f), fontSize = 11.sp))
            }
        }
    }
}

@Composable
private fun AppPicker(
    vm: LauncherViewModel,
    slot: Int,
    current: AppEntry?,
    onPick: (AppEntry?) -> Unit,
    onClose: () -> Unit,
) {
    val apps by vm.allApps.collectAsStateWithLifecycle()
    val rows = remember(apps) {
        buildList {
            var last: Char? = null
            apps.forEach { app ->
                if (app.letter != last) {
                    add(PickerRow.Header(app.letter))
                    last = app.letter
                }
                add(PickerRow.App(app))
            }
        }
    }
    Column(
        Modifier
            .fillMaxSize()
            .background(Ink.Black)
            .pointerInputTap { } // nie przepuszczaj dotyku pod spód
            .windowInsetsPadding(WindowInsets.systemBars),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(36.dp)
                    .border(1.dp, Ink.Faint, RoundedCornerShape(50))
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                BasicText("←", style = TextStyle(color = Ink.White, fontSize = 18.sp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                BasicText("Skrót ${slot + 1}", style = TextStyle(color = Ink.White, fontSize = 24.sp, fontWeight = FontWeight.Light))
                BasicText("Wybierz aplikację", style = TextStyle(color = Ink.Dim, fontSize = 12.sp))
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Ink.Faint))
        if (current != null) {
            Box(
                Modifier
                    .padding(start = 20.dp, end = 20.dp, top = 12.dp)
                    .fillMaxWidth()
                    .height(40.dp)
                    .border(1.dp, Ink.Faint, RoundedCornerShape(12.dp))
                    .clickable { onPick(null) },
                contentAlignment = Alignment.Center,
            ) {
                BasicText("Usuń skrót", style = TextStyle(color = Ink.White.copy(alpha = 0.8f), fontSize = 13.sp))
            }
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 6.dp)) {
            items(rows, key = { row -> if (row is PickerRow.App) row.entry.key else "h" + (row as PickerRow.Header).letter }) { row ->
                when (row) {
                    is PickerRow.Header -> BasicText(
                        row.letter.toString(),
                        style = TextStyle(color = Ink.Dim, fontSize = 11.sp, letterSpacing = 2.sp),
                        modifier = Modifier.padding(start = 20.dp, top = 14.dp, bottom = 4.dp),
                    )
                    is PickerRow.App -> Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onPick(row.entry) }
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlineIcon(row.entry, vm, 40.dp)
                        Spacer(Modifier.width(14.dp))
                        BasicText(
                            row.entry.label,
                            style = TextStyle(color = Ink.White, fontSize = 16.sp, fontWeight = FontWeight.Light),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        if (current?.key == row.entry.key) {
                            BasicText("obecny", style = TextStyle(color = Ink.Dim, fontSize = 11.sp, letterSpacing = 1.sp))
                        }
                    }
                }
            }
        }
    }
}

private sealed interface PickerRow {
    data class Header(val letter: Char) : PickerRow
    data class App(val entry: AppEntry) : PickerRow
}
