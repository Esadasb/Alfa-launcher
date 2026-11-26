package pl.alfa.launcher

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import pl.alfa.launcher.ui.Ink
import pl.alfa.launcher.ui.LauncherScreen

class MainActivity : ComponentActivity() {
    private val vm: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        applyLook(vm.settings.value)
        // Ekran główny: przycisk „wstecz” nic nie robi.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = Unit
        })
        // Ponowne naciśnięcie „Home” wraca do skrótów i zamyka panele.
        addOnNewIntentListener { vm.onHomePressed() }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.settings.collect { applyLook(it) }
            }
        }
        setContent { LauncherScreen(vm) }
    }

    private fun blurAvailable(): Boolean =
        Build.VERSION.SDK_INT >= 31 && windowManager.isCrossWindowBlurEnabled

    /** Kolory, tapeta i rozmycie okna według ustawień. */
    private fun applyLook(s: LauncherSettings) {
        val blur = blurAvailable()
        Ink.apply(s, blur)
        val lightBars: Boolean
        if (s.mode == BackgroundMode.WALLPAPER) {
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            if (Build.VERSION.SDK_INT >= 31) {
                window.setBackgroundBlurRadius(if (blur) (s.blurRadiusDp * resources.displayMetrics.density).toInt() else 0)
            }
            lightBars = false
        } else {
            window.setBackgroundDrawable(ColorDrawable(s.background))
            if (Build.VERSION.SDK_INT >= 31) window.setBackgroundBlurRadius(0)
            lightBars = ColorUtils.calculateLuminance(s.background) > 0.5
        }
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = lightBars
            isAppearanceLightNavigationBars = lightBars
        }
    }
}
