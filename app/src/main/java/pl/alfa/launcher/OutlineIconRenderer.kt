package pl.alfa.launcher

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Zamienia dowolną ikonę aplikacji w minimalistyczny, biały kontur na przezroczystym tle.
 *
 * 1. Bierze najprostszą wersję ikony: warstwę monochromatyczną (Android 13+),
 *    a gdy jej brak – pierwszy plan ikony adaptacyjnej, a w ostateczności całą ikonę.
 * 2. Wykrywa krawędzie (operator Sobela) zarówno na kanale przezroczystości,
 *    jak i na jasności, więc kontur obejmuje kształt i najważniejsze detale.
 * 3. Lekko pogrubia linię, żeby była wyraźna na ekranach o wysokiej gęstości.
 */
object OutlineIconRenderer {

    fun render(drawable: Drawable, size: Int): Bitmap = outline(rasterize(drawable, size), size)

    private fun rasterize(drawable: Drawable, size: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        if (drawable is AdaptiveIconDrawable) {
            val mono = if (Build.VERSION.SDK_INT >= 33) drawable.monochrome else null
            val layer = mono ?: drawable.foreground
            if (layer != null) {
                // Warstwa ma 108 jednostek, widoczne jest środkowe 72 – powiększamy do tego obszaru.
                val full = (size * 108f / 72f).toInt()
                val offset = (size - full) / 2
                layer.setBounds(offset, offset, offset + full, offset + full)
                layer.draw(canvas)
                return bitmap
            }
        }
        drawable.setBounds(0, 0, size, size)
        drawable.draw(canvas)
        return bitmap
    }

    private fun outline(src: Bitmap, size: Int): Bitmap {
        val n = size * size
        val pixels = IntArray(n)
        src.getPixels(pixels, 0, size, 0, 0, size, size)
        src.recycle()

        val alpha = FloatArray(n)
        val luma = FloatArray(n)
        for (i in 0 until n) {
            val c = pixels[i]
            val a = (c ushr 24) / 255f
            val r = (c shr 16 and 0xFF) / 255f
            val g = (c shr 8 and 0xFF) / 255f
            val b = (c and 0xFF) / 255f
            alpha[i] = a
            // getPixels zwraca kolory bez mnożenia przez alfę – liczymy jasność na czarnym tle.
            luma[i] = (0.299f * r + 0.587f * g + 0.114f * b) * a
        }

        val edge = FloatArray(n)
        for (y in 1 until size - 1) {
            for (x in 1 until size - 1) {
                val i = y * size + x
                val ea = ramp(sobel(alpha, i, size), 0.45f, 1.6f)
                val el = ramp(sobel(luma, i, size), 0.85f, 1.9f)
                edge[i] = max(ea, el)
            }
        }

        // Delikatne pogrubienie (dylatacja 3×3), żeby linia miała ~1 dp na ekranie.
        val out = IntArray(n)
        for (y in 1 until size - 1) {
            for (x in 1 until size - 1) {
                val i = y * size + x
                var m = 0f
                for (dy in -1..1) for (dx in -1..1) {
                    val v = edge[i + dy * size + dx]
                    if (v > m) m = v
                }
                val v = max(edge[i], m * 0.7f)
                if (v > 0.02f) {
                    val a = (v.coerceIn(0f, 1f) * 255f).toInt()
                    out[i] = (a shl 24) or 0xFFFFFF // biały z przezroczystością
                }
            }
        }
        val result = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        result.setPixels(out, 0, size, 0, 0, size, size)
        result.prepareToDraw()
        return result
    }

    private fun sobel(v: FloatArray, i: Int, w: Int): Float {
        val tl = v[i - w - 1]; val t = v[i - w]; val tr = v[i - w + 1]
        val l = v[i - 1]; val r = v[i + 1]
        val bl = v[i + w - 1]; val b = v[i + w]; val br = v[i + w + 1]
        val gx = (tr + 2 * r + br) - (tl + 2 * l + bl)
        val gy = (bl + 2 * b + br) - (tl + 2 * t + tr)
        return sqrt(gx * gx + gy * gy)
    }

    private fun ramp(x: Float, from: Float, to: Float): Float =
        ((x - from) / (to - from)).coerceIn(0f, 1f)
}
