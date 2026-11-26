package pl.alfa.launcher

import android.content.SharedPreferences

enum class BackgroundMode { COLOR, WALLPAPER }

/** Ustawienia wyglądu launchera. */
data class LauncherSettings(
    val mode: BackgroundMode = BackgroundMode.COLOR,
    val background: Int = 0xFF000000.toInt(),
    val foreground: Int = 0xFFFFFFFF.toInt(),
    /** Rozmycie tapety: 0 = brak, 1 = lekkie, 2 = mocne. */
    val blur: Int = 2,
    /** Przyciemnienie tapety 0..1. */
    val dim: Float = 0.3f,
    val shadows: Boolean = false,
    /** Napisy jak na wyświetlaczu segmentowym zegara elektronicznego. */
    val segment: Boolean = false,
) {
    val blurRadiusDp: Int get() = when (blur) { 0 -> 0; 1 -> 18; else -> 40 }

    fun save(prefs: SharedPreferences) {
        prefs.edit()
            .putString("bg_mode", mode.name)
            .putInt("bg_color", background)
            .putInt("fg_color", foreground)
            .putInt("blur", blur)
            .putFloat("dim", dim)
            .putBoolean("shadows", shadows)
            .putBoolean("segment", segment)
            .apply()
    }

    companion object {
        fun load(prefs: SharedPreferences): LauncherSettings {
            val d = LauncherSettings()
            return LauncherSettings(
                mode = runCatching { BackgroundMode.valueOf(prefs.getString("bg_mode", d.mode.name)!!) }.getOrDefault(d.mode),
                background = prefs.getInt("bg_color", d.background),
                foreground = prefs.getInt("fg_color", d.foreground),
                blur = prefs.getInt("blur", d.blur),
                dim = prefs.getFloat("dim", d.dim),
                shadows = prefs.getBoolean("shadows", d.shadows),
                segment = prefs.getBoolean("segment", d.segment),
            )
        }
    }
}
