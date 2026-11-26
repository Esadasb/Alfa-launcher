package pl.alfa.launcher.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * [side]: 0 = rolka przy prawej krawędzi, 1 = przy lewej (otwarty folder).
 * Wartości pośrednie to animacja przejazdu – każda litera rusza z lekkim opóźnieniem.
 * [onlyAvailable]: w folderze rolka zatrzymuje się tylko na literach z jego zawartości.
 *
 * Pionowa rolka alfabetu. Pod palcem litery powiększają się falą (efekt lupy),
 * odsuwają się od siebie i wychylają w lewo. Całość rysowana jednym Canvasem,
 * więc ruch palca odświeża tylko rysowanie – bez przebudowy interfejsu.
 */
@Composable
fun AlphabetRail(
    letters: List<Char>,
    available: Set<Char>,
    selected: Char?,
    onSelect: (Char) -> Unit,
    modifier: Modifier = Modifier,
    side: Float = 0f,
    onlyAvailable: Boolean = false,
) {
    val measurer = rememberTextMeasurer()
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val intensity = remember { Animatable(0f) }
    var focusY by remember { mutableFloatStateOf(Float.NaN) }
    val currentSelected by rememberUpdatedState(selected)
    val currentOnSelect by rememberUpdatedState(onSelect)
    val currentAvailable by rememberUpdatedState(available)
    val currentOnlyAvailable by rememberUpdatedState(onlyAvailable)

    val letterStyle = TextStyle(color = Ink.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    val bubbleStyle = TextStyle(color = Ink.White, fontSize = 38.sp, fontWeight = FontWeight.Light)
    val letterLayouts = remember(measurer) { letters.associateWith { measurer.measure(it.toString(), letterStyle) } }
    val bubbleLayouts = remember(measurer) { letters.associateWith { measurer.measure(it.toString(), bubbleStyle) } }

    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val railW = RailWidth.toPx()
            val right = size.width - railW / 2f
            val left = railW / 2f
            val top = RailPaddingVertical.toPx()
            val step = (size.height - 2 * top) / letters.size
            val k = intensity.value
            val selIndex = letters.indexOf(selected)
            val fy = if (focusY.isNaN()) top + (selIndex.coerceAtLeast(0) + 0.5f) * step else focusY
            val sigma = 1.5f
            val maxShift = 30.dp.toPx()
            val lift = 14.dp.toPx()
            val last = (letters.size - 1).coerceAtLeast(1)

            letters.forEachIndexed { i, c ->
                val si = easeInOut(((side - 0.35f * i / last) / 0.65f).coerceIn(0f, 1f))
                val dirX = -1f + 2f * si                         // -1: wychylenie w lewo, +1: w prawo
                val cx = right + (left - right) * si
                val baseY = top + (i + 0.5f) * step
                val d = (baseY - fy) / step
                val wave = exp(-(d * d) / (2 * sigma * sigma)) * k
                val y = baseY + step * 0.9f * d * wave - sin(PI.toFloat() * si) * lift // łuk podczas przejazdu
                val x = cx + dirX * maxShift * wave
                val zoom = 1f + 1.3f * wave
                val has = c in available
                val base = when {
                    c == selected -> 1f
                    has -> 0.55f
                    else -> 0.18f
                }
                val alpha = (base + (1f - base) * wave * (if (has) 1f else 0.35f)).coerceIn(0f, 1f)
                val layout = letterLayouts.getValue(c)
                withTransform({ scale(zoom, zoom, Offset(x, y)) }) {
                    drawText(
                        textLayoutResult = layout,
                        topLeft = Offset(x - layout.size.width / 2f, y - layout.size.height / 2f),
                        alpha = alpha,
                    )
                }
            }

            val s0 = easeInOut(side)
            val dir0 = -1f + 2f * s0
            val cx0 = right + (left - right) * s0

            // Kropka przy wybranej literze, gdy rolka odpoczywa.
            if (selIndex >= 0 && k < 0.99f && (side == 0f || side == 1f)) {
                val y = top + (selIndex + 0.5f) * step
                drawCircle(Ink.White, radius = 2.dp.toPx() * (1f - k), center = Offset(cx0 + dir0 * 15.dp.toPx(), y))
            }

            // „Bańka” z dużą literą obok kciuka – po stronie ekranu przeciwnej do rolki.
            val sel = selected
            if (sel != null && k > 0.01f) {
                val r = 34.dp.toPx()
                val bxRight = size.width - railW - maxShift - 58.dp.toPx()
                val bxLeft = railW + maxShift + 58.dp.toPx()
                val center = Offset(
                    bxRight + (bxLeft - bxRight) * s0,
                    fy.coerceIn(top + r, size.height - top - r),
                )
                val sc = 0.6f + 0.4f * k
                withTransform({ scale(sc, sc, center) }) {
                    drawCircle(Ink.Black, radius = r, center = center, alpha = k)
                    drawCircle(Ink.White, radius = r, center = center, alpha = k, style = Stroke(1.dp.toPx()))
                    val layout = bubbleLayouts.getValue(sel)
                    drawText(
                        textLayoutResult = layout,
                        topLeft = Offset(center.x - layout.size.width / 2f, center.y - layout.size.height / 2f),
                        alpha = k,
                    )
                }
            }
        }

        // Pas dotyku: tylko prawa krawędź, reszta ekranu zostaje dla ikon.
        Box(
            Modifier
                .align(if (side > 0.5f) Alignment.CenterStart else Alignment.CenterEnd)
                .fillMaxHeight()
                .width(RailWidth)
                .pointerInput(letters) {
                    val top = RailPaddingVertical.toPx()
                    awaitEachGesture {
                        var last = letters.indexOf(currentSelected)
                        fun track(y: Float) {
                            focusY = y
                            val step = (size.height - 2 * top) / letters.size
                            val index = ((y - top) / step).toInt().coerceIn(0, letters.lastIndex)
                            val letter = letters[index]
                            if (currentOnlyAvailable && letter !in currentAvailable) return
                            if (index != last) {
                                last = index
                                currentOnSelect(letter)
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            }
                        }

                        val down = awaitFirstDown()
                        down.consume()
                        scope.launch { intensity.animateTo(1f, spring(dampingRatio = 0.85f, stiffness = 700f)) }
                        track(down.position.y)
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            track(change.position.y)
                            change.consume()
                        }
                        scope.launch { intensity.animateTo(0f, spring(dampingRatio = 1f, stiffness = 240f)) }
                    }
                },
        )
    }
}

private fun easeInOut(x: Float): Float =
    if (x < 0.5f) 4f * x * x * x else 1f - (-2f * x + 2f).let { it * it * it } / 2f
