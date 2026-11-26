package pl.alfa.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pl.alfa.launcher.ALPHABET
import pl.alfa.launcher.HomeItem
import pl.alfa.launcher.LauncherViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LauncherScreen(vm: LauncherViewModel = viewModel()) {
    val groups by vm.groups.collectAsStateWithLifecycle()
    val selected by vm.selected.collectAsStateWithLifecycle()
    val folder by vm.openFolder.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    // Rolka przejeżdża na lewą stronę, gdy folder jest otwarty.
    val side by animateFloatAsState(
        targetValue = if (folder != null) 1f else 0f,
        animationSpec = tween(620, easing = androidx.compose.animation.core.LinearEasing),
        label = "side",
    )
    val sideEased = FastOutSlowInEasing.transform(side)

    // Stan folderu: siatka, litera pod rolką i chwilowe wyróżnienie aplikacji na tę literę.
    val folderGrid = rememberLazyGridState()
    var folderLetter by remember { mutableStateOf<Char?>(null) }
    var highlight by remember { mutableStateOf<Char?>(null) }
    val folderApps = folder?.apps.orEmpty()
    val folderLetters = remember(folderApps) { folderApps.map { it.letter }.toSet() }

    LaunchedEffect(folder?.folder?.id) {
        folderLetter = folderApps.firstOrNull()?.letter
        highlight = null
        folderGrid.scrollToItem(0)
    }
    LaunchedEffect(folderGrid, folderApps) {
        snapshotFlow { folderGrid.firstVisibleItemIndex }.collect { i ->
            if (highlight == null) folderApps.getOrNull(i)?.let { folderLetter = it.letter }
        }
    }
    LaunchedEffect(highlight) {
        if (highlight != null) {
            delay(900)
            highlight = null
        }
    }
    BackHandler(enabled = folder != null) { vm.closeFolder() }

    Box(
        Modifier
            .fillMaxSize()
            .background(Ink.Black),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(
                    start = lerp(24.dp, RailWidth + 8.dp, sideEased),
                    end = lerp(RailWidth + 8.dp, 22.dp, sideEased),
                    bottom = SwipeZoneHeight,
                ),
        ) {
            Spacer(Modifier.height(28.dp))
            ClockHeader()
            Spacer(Modifier.height(36.dp))

            val folderNow = folder
            AnimatedContent(
                targetState = folderNow?.folder?.id ?: selected?.toString(),
                transitionSpec = {
                    val folderChange = initialState?.length != 1 || targetState?.length != 1
                    val transform = if (folderChange) {
                        // Wejście do folderu / wyjście: przesunięcie w poziomie.
                        val dir = if (targetState?.length != 1) 1 else -1
                        (fadeIn(tween(260, delayMillis = 60)) +
                            slideInHorizontally(tween(420, easing = FastOutSlowInEasing)) { dir * it / 6 }) togetherWith
                            (fadeOut(tween(160)) +
                                slideOutHorizontally(tween(300, easing = FastOutSlowInEasing)) { -dir * it / 6 })
                    } else {
                        val from = ALPHABET.indexOf(initialState?.firstOrNull() ?: 'A')
                        val to = ALPHABET.indexOf(targetState?.firstOrNull() ?: 'A')
                        val dir = if (to >= from) 1 else -1
                        (fadeIn(tween(220, delayMillis = 30)) +
                            slideInVertically(tween(280, easing = FastOutSlowInEasing)) { dir * it / 12 }) togetherWith
                            (fadeOut(tween(130)) +
                                slideOutVertically(tween(220, easing = FastOutSlowInEasing)) { -dir * it / 12 })
                    }
                    transform using SizeTransform(clip = false)
                },
                label = "page",
                modifier = Modifier.fillMaxSize(),
            ) { key ->
                when {
                    key == null -> Unit
                    key.length == 1 -> LetterPage(key.first(), groups[key.first()].orEmpty(), vm)
                    folderNow != null && folderNow.folder.id == key ->
                        FolderPage(folderNow, vm, folderGrid, highlight)
                }
            }
        }

        AlphabetRail(
            letters = ALPHABET,
            available = if (folder != null) folderLetters else groups.keys,
            selected = if (folder != null) folderLetter else selected,
            onSelect = { c ->
                if (folder != null) {
                    folderLetter = c
                    highlight = c
                    val index = folderApps.indexOfFirst { it.letter == c }
                    if (index >= 0) scope.launch { folderGrid.animateScrollToItem(index) }
                } else {
                    vm.select(c)
                }
            },
            side = side,
            onlyAvailable = folder != null,
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(bottom = SwipeZoneHeight),
        )

        QuickPanel(vm)
        AppActionsSheet(vm)
    }
}

@Composable
private fun ClockHeader() {
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(1_000)
        }
    }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()) }
    val minute = now / 60_000
    val time = remember(minute) { timeFormat.format(Date(now)) }
    val date = remember(minute) { dateFormat.format(Date(now)) }

    Column {
        BasicText(
            time,
            style = TextStyle(color = Ink.White, fontSize = 64.sp, fontWeight = FontWeight.Thin, letterSpacing = (-1).sp),
        )
        BasicText(
            date,
            style = TextStyle(color = Ink.Dim, fontSize = 15.sp, fontWeight = FontWeight.Light, letterSpacing = 0.4.sp),
        )
    }
}

@Composable
private fun LetterPage(letter: Char, items: List<HomeItem>, vm: LauncherViewModel) {
    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.Bottom) {
            BasicText(
                letter.toString(),
                style = TextStyle(color = Ink.White, fontSize = 92.sp, fontWeight = FontWeight.Thin, lineHeight = 92.sp),
            )
            Spacer(Modifier.width(14.dp))
            BasicText(
                if (items.isEmpty()) "brak aplikacji" else appsCount(items.size),
                style = TextStyle(color = Ink.Dim, fontSize = 13.sp, letterSpacing = 1.sp),
                modifier = Modifier.padding(bottom = 18.dp),
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Ink.Faint),
        )
        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                BasicText(
                    "Żadna aplikacja nie zaczyna się na „$letter”",
                    style = TextStyle(color = Ink.Dim, fontSize = 14.sp, textAlign = TextAlign.Center),
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(top = 24.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(26.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(items, key = { it.key }) { item ->
                    when (item) {
                        is HomeItem.AppItem -> AppTile(item.entry, vm)
                        is HomeItem.FolderItem -> FolderTile(item, vm)
                    }
                }
            }
        }
    }
}
