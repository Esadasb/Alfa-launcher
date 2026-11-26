package pl.alfa.launcher

import android.app.Application
import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LauncherViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = AppRepository(app)
    val icons = IconCache(app)

    private val apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val allApps: StateFlow<List<AppEntry>> = apps.asStateFlow()
    private val prefsFolders = app.getSharedPreferences("alfa", Context.MODE_PRIVATE)
    private val folderStore = FolderStore(prefsFolders)
    private val folders = MutableStateFlow(folderStore.load())
    val folderList: StateFlow<List<Folder>> = folders.asStateFlow()

    /** Strony liter: aplikacje spoza folderów oraz foldery, posortowane po polsku. */
    val groups: StateFlow<Map<Char, List<HomeItem>>> = combine(apps, folders) { list, fs ->
        val byKey = list.associateBy { it.key }
        val inFolders = fs.flatMap { it.appKeys }.toSet()
        val items: List<HomeItem> = list.filter { it.key !in inFolders }.map { HomeItem.AppItem(it) } +
            fs.mapNotNull { f ->
                val content = f.appKeys.mapNotNull(byKey::get)
                if (content.isEmpty()) null else HomeItem.FolderItem(f, content)
            }
        items.sortedWith(compareBy(repository.collator) { it.label }).groupBy { letterOf(it.label) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    private val openFolderId = MutableStateFlow<String?>(null)

    /** Otwarty folder (zawartość posortowana alfabetycznie) albo null. */
    val openFolder: StateFlow<HomeItem.FolderItem?> = combine(openFolderId, apps, folders) { id, list, fs ->
        val f = fs.find { it.id == id } ?: return@combine null
        val byKey = list.associateBy { it.key }
        val content = f.appKeys.mapNotNull(byKey::get).sortedWith(compareBy(repository.collator) { it.label })
        if (content.isEmpty()) null else HomeItem.FolderItem(f, content)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _actionTarget = MutableStateFlow<AppEntry?>(null)

    /** Aplikacja, dla której pokazano opcje (przytrzymanie ikony). */
    val actionTarget: StateFlow<AppEntry?> = _actionTarget.asStateFlow()

    private val _selected = MutableStateFlow<Char?>(null)
    val selected: StateFlow<Char?> = _selected.asStateFlow()

    // Programowalne skróty – zapisywane w pamięci telefonu, przetrwają restart.
    private val prefs = app.getSharedPreferences("alfa", Context.MODE_PRIVATE)
    private val slotKeys = MutableStateFlow(List(SHORTCUT_SLOTS) { prefs.getString("slot_$it", null) })
    val shortcuts: StateFlow<List<AppEntry?>> = combine(slotKeys, apps) { keys, list ->
        val byKey = list.associateBy { it.key }
        keys.map { key -> key?.let(byKey::get) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, List(SHORTCUT_SLOTS) { null })

    private val _panelOpen = MutableStateFlow(false)
    val panelOpen: StateFlow<Boolean> = _panelOpen.asStateFlow()

    private var loadJob: Job? = null
    private val stopObserving: () -> Unit = repository.observe { reload() }

    init {
        reload()
    }

    private fun reload() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val list = withContext(Dispatchers.IO) { repository.loadApps() }
            apps.value = list
            if (_selected.value == null) _selected.value = list.firstOrNull()?.letter
            // Przygotuj wszystkie kontury z wyprzedzeniem – rolka przełącza litery natychmiast.
            withContext(Dispatchers.Default) {
                list.forEach { runCatching { icons.load(it) } }
            }
        }
    }

    fun select(letter: Char) {
        _selected.value = letter
    }

    fun launch(entry: AppEntry, view: View, bounds: Rect) = repository.launch(entry, view, bounds)

    fun setPanel(open: Boolean) {
        _panelOpen.value = open
    }

    fun setShortcut(slot: Int, entry: AppEntry?) {
        prefs.edit().apply {
            if (entry == null) remove("slot_$slot") else putString("slot_$slot", entry.key)
        }.apply()
        slotKeys.update { keys -> keys.toMutableList().also { it[slot] = entry?.key } }
    }

    fun openFolder(id: String) {
        openFolderId.value = id
    }

    fun closeFolder() {
        openFolderId.value = null
    }

    fun showActions(entry: AppEntry) {
        _actionTarget.value = entry
    }

    fun hideActions() {
        _actionTarget.value = null
    }

    fun folderOf(entry: AppEntry): Folder? = folders.value.find { entry.key in it.appKeys }

    fun addToFolder(entry: AppEntry, folderId: String) = editFolders { list ->
        list.map { f ->
            val without = f.appKeys - entry.key
            if (f.id == folderId) f.copy(appKeys = without + entry.key) else f.copy(appKeys = without)
        }
    }

    /** Tworzy folder (albo używa istniejącego o tej samej nazwie) i dodaje do niego aplikację. */
    fun createFolder(name: String, entry: AppEntry) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        val existing = folders.value.find { repository.collator.compare(it.name, clean) == 0 }
        if (existing != null) {
            addToFolder(entry, existing.id)
        } else {
            editFolders { list ->
                list.map { it.copy(appKeys = it.appKeys - entry.key) } + Folder(folderStore.newId(), clean, listOf(entry.key))
            }
        }
    }

    fun removeFromFolder(entry: AppEntry) = editFolders { list ->
        list.map { it.copy(appKeys = it.appKeys - entry.key) }
    }

    private fun editFolders(change: (List<Folder>) -> List<Folder>) {
        // Puste foldery znikają same.
        val updated = change(folders.value).filter { it.appKeys.isNotEmpty() }
        folders.value = updated
        folderStore.save(updated)
    }

    fun openDialer() = repository.openDialer()

    fun openMessages() = repository.openMessages()

    fun openDetails(entry: AppEntry) = repository.openDetails(entry)

    override fun onCleared() {
        stopObserving()
    }

    companion object {
        const val SHORTCUT_SLOTS = 8
    }
}
