package pl.alfa.launcher

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.os.UserManager
import android.view.View
import java.text.Collator
import java.text.Normalizer
import java.util.Locale

/** Jedna ikona na pulpicie. */
class AppEntry(
    val key: String,
    val label: String,
    val letter: Char,
    val info: LauncherActivityInfo,
    val user: UserHandle,
    val lastUpdate: Long,
)

/** Litery rolki: A–Z oraz # dla nazw zaczynających się od cyfry lub znaku. */
val ALPHABET: List<Char> = ('A'..'Z').toList() + '#'

/** Pierwsza litera nazwy, bez polskich znaków (Ł → L, Ś → S, Ż → Z…). */
fun letterOf(label: String): Char {
    val first = label.trim().firstOrNull { it.isLetterOrDigit() } ?: return '#'
    val base = when (first) {
        'ł', 'Ł' -> 'L'
        else -> Normalizer.normalize(first.toString(), Normalizer.Form.NFD).first()
    }.uppercaseChar()
    return if (base in 'A'..'Z') base else '#'
}

class AppRepository(context: Context) {
    private val appContext = context.applicationContext
    private val launcherApps = appContext.getSystemService(LauncherApps::class.java)
    private val userManager = appContext.getSystemService(UserManager::class.java)
    val collator: Collator = Collator.getInstance(Locale.forLanguageTag("pl-PL")).apply {
        strength = Collator.PRIMARY
    }

    fun loadApps(): List<AppEntry> {
        val own = appContext.packageName
        val pm = appContext.packageManager
        return userManager.userProfiles.flatMap { user ->
            launcherApps.getActivityList(null, user)
                .filter { it.applicationInfo.packageName != own }
                .map { info ->
                    val label = info.label?.toString()?.trim().orEmpty()
                        .ifEmpty { info.applicationInfo.packageName }
                    val lastUpdate = runCatching {
                        pm.getPackageInfo(info.applicationInfo.packageName, 0).lastUpdateTime
                    }.getOrDefault(info.firstInstallTime)
                    AppEntry(
                        key = info.componentName.flattenToShortString() + "#" + user.hashCode(),
                        label = label,
                        letter = letterOf(label),
                        info = info,
                        user = user,
                        lastUpdate = lastUpdate,
                    )
                }
        }.sortedWith(compareBy(collator) { it.label })
    }

    /** Wywołuje [onChange] przy instalacji, usunięciu lub aktualizacji aplikacji. */
    fun observe(onChange: () -> Unit): () -> Unit {
        val callback = object : LauncherApps.Callback() {
            override fun onPackageRemoved(packageName: String, user: UserHandle) = onChange()
            override fun onPackageAdded(packageName: String, user: UserHandle) = onChange()
            override fun onPackageChanged(packageName: String, user: UserHandle) = onChange()
            override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = onChange()
            override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = onChange()
        }
        launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))
        return { launcherApps.unregisterCallback(callback) }
    }

    fun launch(entry: AppEntry, view: View, bounds: Rect) {
        val options = ActivityOptions.makeClipRevealAnimation(
            view, bounds.left, bounds.top, bounds.width(), bounds.height(),
        ).toBundle()
        runCatching { launcherApps.startMainActivity(entry.info.componentName, entry.user, bounds, options) }
    }

    /** Domyślna aplikacja Telefon (klawiatura numeryczna). */
    fun openDialer() = start(Intent(Intent.ACTION_DIAL))

    /** Domyślna aplikacja do SMS-ów. */
    fun openMessages() = start(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MESSAGING))

    private fun start(intent: Intent) {
        runCatching { appContext.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    fun openDetails(entry: AppEntry) {
        runCatching { launcherApps.startAppDetailsActivity(entry.info.componentName, entry.user, null, null) }
    }
}
