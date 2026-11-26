package pl.alfa.launcher

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Folder użytkownika, np. „Banki”. Trafia pod literę swojej nazwy. */
data class Folder(val id: String, val name: String, val appKeys: List<String>)

/** Element strony litery: pojedyncza aplikacja albo folder. */
sealed interface HomeItem {
    val key: String
    val label: String

    class AppItem(val entry: AppEntry) : HomeItem {
        override val key get() = entry.key
        override val label get() = entry.label
    }

    class FolderItem(val folder: Folder, val apps: List<AppEntry>) : HomeItem {
        override val key get() = "folder:" + folder.id
        override val label get() = folder.name
    }
}

/** Zapis folderów w pamięci telefonu (JSON w SharedPreferences). */
class FolderStore(private val prefs: SharedPreferences) {
    fun load(): List<Folder> = runCatching {
        val array = JSONArray(prefs.getString(KEY, "[]"))
        List(array.length()) { i ->
            val o = array.getJSONObject(i)
            val apps = o.getJSONArray("apps")
            Folder(o.getString("id"), o.getString("name"), List(apps.length()) { apps.getString(it) })
        }
    }.getOrDefault(emptyList())

    fun save(folders: List<Folder>) {
        val array = JSONArray()
        folders.forEach { f ->
            array.put(JSONObject().put("id", f.id).put("name", f.name).put("apps", JSONArray(f.appKeys)))
        }
        prefs.edit().putString(KEY, array.toString()).apply()
    }

    fun newId(): String = UUID.randomUUID().toString()

    private companion object {
        const val KEY = "folders"
    }
}
