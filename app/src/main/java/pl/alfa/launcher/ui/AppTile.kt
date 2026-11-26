package pl.alfa.launcher.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pl.alfa.launcher.AppEntry
import pl.alfa.launcher.LauncherViewModel

/** Konturowa ikona aplikacji, ładowana w tle i płynnie pojawiająca się. */
@Composable
fun OutlineIcon(entry: AppEntry, vm: LauncherViewModel, size: Dp, modifier: Modifier = Modifier) {
    val icon by produceState<ImageBitmap?>(vm.icons.peek(entry), entry.key) {
        if (value == null) value = withContext(Dispatchers.Default) { runCatching { vm.icons.load(entry) }.getOrNull() }
    }
    val iconAlpha by animateFloatAsState(if (icon != null) 1f else 0f, tween(260), label = "fade")
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        icon?.let {
            if (Ink.Shadows) {
                // Miękki cień pod konturem – ikona odcina się od tapety.
                Image(
                    bitmap = it,
                    contentDescription = null,
                    modifier = Modifier
                        .size(size)
                        .offset(y = 1.5.dp)
                        .blur(3.dp, BlurredEdgeTreatment.Unbounded)
                        .graphicsLayer { alpha = iconAlpha * 0.9f },
                    colorFilter = ColorFilter.tint(Color.Black),
                    filterQuality = FilterQuality.High,
                )
            }
            Image(
                bitmap = it,
                contentDescription = entry.label,
                modifier = Modifier
                    .size(size)
                    .graphicsLayer { alpha = iconAlpha },
                colorFilter = ColorFilter.tint(Ink.White),
                filterQuality = FilterQuality.High,
            )
        }
    }
}

/**
 * Ikona z podpisem. Dotknięcie uruchamia aplikację, przytrzymanie domyślnie
 * pokazuje opcje: foldery i „Informacje o aplikacji” (albo robi to, co poda [onLongClick]).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppTile(
    entry: AppEntry,
    vm: LauncherViewModel,
    modifier: Modifier = Modifier,
    iconSize: Dp = 56.dp,
    labelSize: TextUnit = 14.sp,
    maxLines: Int = 2,
    onLaunched: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
) {
    val view = LocalView.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "press",
    )
    var bounds by remember { mutableStateOf(Rect.Zero) }

    Column(
        modifier = modifier
            .onGloballyPositioned { bounds = it.boundsInRoot() }
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = {
                    vm.launch(
                        entry, view,
                        android.graphics.Rect(
                            bounds.left.toInt(), bounds.top.toInt(), bounds.right.toInt(), bounds.bottom.toInt(),
                        ),
                    )
                    onLaunched()
                },
                onLongClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    if (onLongClick != null) onLongClick() else vm.showActions(entry)
                },
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        OutlineIcon(entry, vm, iconSize)
        Spacer(Modifier.height(8.dp))
        BasicText(
            text = entry.label,
            style = TextStyle(
                shadow = Ink.TextShadow,
                color = Ink.White.copy(alpha = 0.88f),
                fontSize = labelSize,
                lineHeight = labelSize * 1.25f,
                letterSpacing = 0.2.sp,
                textAlign = TextAlign.Center,
            ),
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
