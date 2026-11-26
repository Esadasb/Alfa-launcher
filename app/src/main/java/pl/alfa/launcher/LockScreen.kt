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

        // Rolka alfabetu po prawej, w dolnych dwóch trzecich ekranu.
        val letters = ALPHABET
        val top = h * 0.30f
        val bottom = h - 72 * d
        val step = (bottom - top) / (letters.size - 1)
        val railX = w - 26 * d
        val focus = 0f // fala wokół litery „A”
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = face
            textAlign = Paint.Align.CENTER
        }
        val baseSize = (if (s.segment) 11f else 13f) * d
        letters.forEachIndexed { i, ch ->
            val dist = i - focus
            val wave = exp(-(dist * dist) / (2 * 1.5f * 1.5f))
            val zoom = 1f + 1.3f * wave
            val y = top + i * step + dist * 0.9f * wave * step * 0.5f
            val x = railX - 30 * d * wave
            paint.textSize = baseSize * zoom
            paint.color = ColorUtils.setAlphaComponent(fg, (255 * (0.45f + 0.55f * wave)).toInt())
            val fm = paint.fontMetrics
            c.drawText(ch.toString(), x, y - (fm.ascent + fm.descent) / 2, paint)
        }

        // Duża cienka litera „A” – znak Alfa, w miejscu dymka przy rolce.
        val big = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = thin
            textAlign = Paint.Align.LEFT
            textSize = (if (s.segment) 120f else 160f) * d
            color = ColorUtils.setAlphaComponent(fg, 235)
        }
        val bigY = top + 96 * d
        val bigX = 28 * d
        c.drawText("A", bigX, bigY + (if (s.segment) 30 else 40) * d, big)

        // Cienka linia i podpis jak nagłówek strony litery.
        val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ColorUtils.setAlphaComponent(fg, 46)
            strokeWidth = d
        }
        val lineY = bigY + 64 * d
        c.drawLine(bigX, lineY, railX - 64 * d, lineY, line)
        val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = face
            textSize = (if (s.segment) 10f else 12f) * d
            color = ColorUtils.setAlphaComponent(fg, 140)
            letterSpacing = 0.2f
        }
        c.drawText("ALFA", bigX + 4 * d, lineY + 22 * d, label)
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
