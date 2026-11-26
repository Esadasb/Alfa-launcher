package pl.alfa.launcher

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.hardware.display.DisplayManager
import android.util.DisplayMetrics
import android.view.Display
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils
import kotlin.math.exp

/**
 * Tapeta ekranu blokady w stylu Alfa. Android nie pozwala zwykłej aplikacji zastąpić
 * ekranu blokady, ale pozwala ustawić jego tapetę – rysujemy ją w kolorach i stylu
 * launchera: tło, rolka alfabetu z efektem lupy i duża litera „A”.
 * Górna część zostaje pusta, bo tam system rysuje zegar i powiadomienia.
 */
object LockScreen {
    private const val PREF_ON = "lock_alfa"

    fun isOn(context: Context): Boolean =
        context.getSharedPreferences("alfa", Context.MODE_PRIVATE).getBoolean(PREF_ON, false)

    /** Rysuje i ustawia tapetę ekranu blokady. Wywoływać poza wątkiem UI. */
    fun apply(context: Context, s: LauncherSettings) {
        val bmp = render(context, s)
        WallpaperManager.getInstance(context).setBitmap(bmp, null, true, WallpaperManager.FLAG_LOCK)
        bmp.recycle()
        context.getSharedPreferences("alfa", Context.MODE_PRIVATE).edit().putBoolean(PREF_ON, true).apply()
    }

    /** Usuwa tapetę Alfa – ekran blokady wraca do tapety ekranu głównego. */
    fun clear(context: Context) {
        WallpaperManager.getInstance(context).clear(WallpaperManager.FLAG_LOCK)
        context.getSharedPreferences("alfa", Context.MODE_PRIVATE).edit().putBoolean(PREF_ON, false).apply()
    }

    fun render(context: Context, s: LauncherSettings): Bitmap {
        val m = realMetrics(context)
        val w = m.widthPixels
        val h = m.heightPixels
        val d = m.density
        // Przy tle z tapety ekran blokady dostaje czarne tło – tapety systemowej nie da się odczytać bez uprawnień.
        val bg = if (s.mode == BackgroundMode.COLOR) s.background else Color.BLACK
        val fg = s.foreground
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(bg)

        val face: Typeface = if (s.segment) {
            ResourcesCompat.getFont(context, R.font.dseg14_regular) ?: Typeface.DEFAULT
        } else {
            Typeface.create("sans-serif", Typeface.NORMAL)
        }
        val thin: Typeface = if (s.segment) {
            ResourcesCompat.getFont(context, R.font.dseg14_light) ?: Typeface.DEFAULT
        } else {
            Typeface.create("sans-serif-thin", Typeface.NORMAL)
        }

        // Cała grafika jest w dolnej części ekranu (poniżej ~60% wysokości): wyżej system
        // rysuje zegar, datę i powiadomienia, a ich położenie zależy od producenta.
        // Nad samym dołem zostaje 150 dp na skróty aparatu i latarki.
        val left = 28 * d
        val right = w - 28 * d
        val rowY = h - 150 * d

        // Duża cienka litera „A” – znak Alfa.
        val big = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = thin
            textAlign = Paint.Align.LEFT
            textSize = (if (s.segment) 84f else 112f) * d
            color = ColorUtils.setAlphaComponent(fg, 235)
        }
        val bigY = rowY - 64 * d
        c.drawText("A", left - (if (s.segment) 0f else 6 * d), bigY, big)

        // Cienka linia jak pod nagłówkiem strony litery.
        val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ColorUtils.setAlphaComponent(fg, 46)
            strokeWidth = d
        }
        val lineY = bigY + 20 * d
        c.drawLine(left, lineY, right, lineY, line)

        // Alfabet w poziomie z efektem lupy na „A” – rolka launchera obrócona na bok.
        val letters = ALPHABET
        val step = (right - left) / (letters.size - 1)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = face
            textAlign = Paint.Align.CENTER
        }
        val baseSize = (if (s.segment) 8f else 10f) * d
        val focus = 0f
        letters.forEachIndexed { i, ch ->
            val dist = i - focus
            val wave = exp(-(dist * dist) / (2 * 1.5f * 1.5f))
            val zoom = 1f + 1.3f * wave
            val x = left + i * step + dist * 0.9f * wave * step * 0.6f
            val y = rowY - 10 * d * wave
            paint.textSize = baseSize * zoom
            paint.color = ColorUtils.setAlphaComponent(fg, (255 * (0.4f + 0.6f * wave)).toInt())
            val fm = paint.fontMetrics
            c.drawText(ch.toString(), x, y - (fm.ascent + fm.descent) / 2, paint)
        }
        return bmp
    }

    @Suppress("DEPRECATION")
    private fun realMetrics(context: Context): DisplayMetrics {
        val m = DisplayMetrics()
        val dm = context.getSystemService(DisplayManager::class.java)
        dm.getDisplay(Display.DEFAULT_DISPLAY).getRealMetrics(m)
        return m
    }
}
