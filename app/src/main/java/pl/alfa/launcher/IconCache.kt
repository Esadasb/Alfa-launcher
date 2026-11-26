package pl.alfa.launcher

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.DisplayMetrics
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File
import java.security.MessageDigest

/** Pamięć podręczna konturowych ikon: w RAM i na dysku, żeby przewijanie było płynne. */
class IconCache(context: Context) {
    private val memory = LruCache<String, ImageBitmap>(512)
    private val dir = File(context.cacheDir, "outline_icons_v$RENDER_VERSION").apply { mkdirs() }
    private val iconDpi = maxOf(context.resources.displayMetrics.densityDpi, DisplayMetrics.DENSITY_XXHIGH)

    fun peek(entry: AppEntry): ImageBitmap? = memory.get(entry.key)

    /** Blokujące – wywołuj poza wątkiem głównym. */
    fun load(entry: AppEntry): ImageBitmap {
        memory.get(entry.key)?.let { return it }
        val file = File(dir, fileName(entry))
        val bitmap = (if (file.exists()) BitmapFactory.decodeFile(file.path) else null)
            ?: OutlineIconRenderer.render(entry.info.getIcon(iconDpi), SIZE_PX).also { rendered ->
                runCatching { file.outputStream().use { rendered.compress(Bitmap.CompressFormat.PNG, 100, it) } }
            }
        bitmap.prepareToDraw()
        val image = bitmap.asImageBitmap()
        memory.put(entry.key, image)
        return image
    }

    private fun fileName(entry: AppEntry): String {
        val digest = MessageDigest.getInstance("SHA-1")
            .digest("${entry.key}|${entry.lastUpdate}".toByteArray())
        return digest.joinToString("") { "%02x".format(it) } + ".png"
    }

    companion object {
        /** Rozdzielczość konturu – ok. 1,5× rozmiaru na ekranie xxhdpi, więc zawsze ostro. */
        const val SIZE_PX = 216
        const val RENDER_VERSION = 1
    }
}
