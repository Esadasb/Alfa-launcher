package pl.alfa.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.alfa.launcher.AppEntry
import pl.alfa.launcher.HomeItem
import pl.alfa.launcher.LauncherViewModel

/** Folder na stronie litery: zaokrąglony kontur z miniaturami czterech pierwszych aplikacji. */
@Composable
fun FolderTile(item: HomeItem.FolderItem, vm: LauncherViewModel, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.88f else 1f, spring(0.6f, Spring.StiffnessMedium), label = "folder")
    Column(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(interactionSource = interaction, indication = null) { vm.openFolder(item.folder.id) },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(56.dp)) {
                val inset = 5.5.dp.toPx()
                drawRoundRect(
                    color = Ink.White.copy(alpha = 0.45f),
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - 2 * inset, size.height - 2 * inset),
                    cornerRadius = CornerRadius(13.dp.toPx()),
                    style = Stroke(1.dp.toPx()),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                item.apps.take(4).chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        row.forEach { OutlineIcon(it, vm, 18.dp) }
                        if (row.size == 1) Spacer(Modifier.size(18.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        BasicText(
            item.folder.name,
            style = TextStyle(shadow = Ink.TextShadow, fontFamily = Ink.Typeface, color = Ink.White.copy(alpha = 0.88f), fontSize = 12.sp, lineHeight = 15.sp, textAlign = TextAlign.Center),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Zawartość otwartego folderu. Rolka jest wtedy po lewej i przewija tę siatkę. */
@Composable
fun FolderPage(item: HomeItem.FolderItem, vm: LauncherViewModel, grid: LazyGridState, highlight: Char?) {
    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 14.dp)) {
            Box(
                Modifier
                    .size(36.dp)
                    .border(1.dp, Ink.Faint, RoundedCornerShape(50))
                    .clickable { vm.closeFolder() },
                contentAlignment = Alignment.Center,
            ) {
                BasicText("←", style = TextStyle(shadow = Ink.TextShadow, fontFamily = Ink.Typeface, color = Ink.White, fontSize = 18.sp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                BasicText(item.folder.name, style = TextStyle(shadow = Ink.TextShadow, fontFamily = Ink.Typeface, color = Ink.White, fontSize = 44.sp * Ink.TextScale, fontWeight = FontWeight.Thin, lineHeight = 46.sp))
                BasicText(appsCount(item.apps.size), style = TextStyle(shadow = Ink.TextShadow, fontFamily = Ink.Typeface, color = Ink.Dim, fontSize = 12.sp, letterSpacing = 1.sp))
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Ink.Faint),
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            state = grid,
            contentPadding = PaddingValues(top = 24.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(26.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(item.apps, key = { it.key }) { entry ->
                val dim by animateFloatAsState(
                    if (highlight == null || entry.letter == highlight) 1f else 0.3f,
                    tween(250),
                    label = "dim",
                )
                AppTile(entry, vm, Modifier.graphicsLayer { alpha = dim })
            }
        }
    }
}

/** Opcje po przytrzymaniu ikony: foldery i „Informacje o aplikacji”. */
@Composable
fun AppActionsSheet(vm: LauncherViewModel) {
    val target by vm.actionTarget.collectAsStateWithLifecycle()
    val folders by vm.folderList.collectAsStateWithLifecycle()
    BackHandler(enabled = target != null) { vm.hideActions() }

    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(visible = target != null, enter = fadeIn(tween(180)), exit = fadeOut(tween(180))) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Ink.Black.copy(alpha = 0.6f))
                    .pointerInput(Unit) { detectTapGestures { vm.hideActions() } },
            )
        }
        AnimatedVisibility(
            visible = target != null,
            enter = fadeIn(tween(180)) + slideInVertically(tween(250)) { it / 6 },
            exit = fadeOut(tween(150)) + slideOutVertically(tween(200)) { it / 6 },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            // Zapamiętaj ostatnią aplikację, żeby zawartość nie znikła w trakcie animacji wyjścia.
            var shown by remember { mutableStateOf<AppEntry?>(null) }
            target?.let { shown = it }
            val entry = shown ?: return@AnimatedVisibility
            val inFolder = vm.folderOf(entry)
            var newName by remember(entry.key) { mutableStateOf("") }
            val shape = RoundedCornerShape(20.dp)

            Column(
                Modifier
                    .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
                    .padding(12.dp)
                    .fillMaxWidth()
                    .background(Ink.Black, shape)
                    .border(1.dp, Ink.Faint, shape)
                    .pointerInput(Unit) { detectTapGestures { } }
                    .padding(vertical = 8.dp),
            ) {
                Row(
                    Modifier.padding(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlineIcon(entry, vm, 36.dp)
                    Spacer(Modifier.width(10.dp))
                    BasicText(entry.label, style = TextStyle(shadow = Ink.TextShadow, fontFamily = Ink.Typeface, color = Ink.White, fontSize = 16.sp, fontWeight = FontWeight.Light))
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(Ink.Faint))

                if (inFolder != null) {
                    ActionRow("Usuń z folderu „${inFolder.name}”") { vm.removeFromFolder(entry); vm.hideActions() }
                } else {
                    folders.forEach { f ->
                        ActionRow("Dodaj do folderu „${f.name}”") { vm.addToFolder(entry, f.id); vm.hideActions() }
                    }
                    Row(
                        Modifier.padding(horizontal = 18.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val fieldShape = RoundedCornerShape(10.dp)
                        Box(
                            Modifier
                                .weight(1f)
                                .height(38.dp)
                                .border(1.dp, Ink.Faint, fieldShape)
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            if (newName.isEmpty()) {
                                BasicText("Nowy folder, np. Banki", style = TextStyle(shadow = Ink.TextShadow, fontFamily = Ink.Typeface, color = Ink.White.copy(alpha = 0.35f), fontSize = 14.sp))
                            }
                            BasicTextField(
                                value = newName,
                                onValueChange = { newName = it.take(30) },
                                singleLine = true,
                                textStyle = TextStyle(shadow = Ink.TextShadow, fontFamily = Ink.Typeface, color = Ink.White, fontSize = 14.sp, fontWeight = FontWeight.Light),
                                cursorBrush = SolidColor(Ink.White),
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = {
                                    if (newName.isNotBlank()) { vm.createFolder(newName, entry); vm.hideActions() }
                                }),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Box(
                            Modifier
                                .height(38.dp)
                                .border(1.dp, Ink.White.copy(alpha = 0.5f), fieldShape)
                                .clickable(enabled = newName.isNotBlank()) { vm.createFolder(newName, entry); vm.hideActions() }
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            BasicText("Utwórz", style = TextStyle(shadow = Ink.TextShadow, fontFamily = Ink.Typeface, color = Ink.White, fontSize = 13.sp))
                        }
                    }
                }
                ActionRow("Informacje o aplikacji") { vm.openDetails(entry); vm.hideActions() }
            }
        }
    }
}

@Composable
private fun ActionRow(label: String, onClick: () -> Unit) {
    BasicText(
        label,
        style = TextStyle(shadow = Ink.TextShadow, fontFamily = Ink.Typeface, color = Ink.White, fontSize = 14.sp, fontWeight = FontWeight.Light),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 13.dp),
    )
}
