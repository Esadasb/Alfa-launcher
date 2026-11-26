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
import org.json.JSONArray
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

    // Programowalne skróty: lista dowolnej długości (do 12), zapisywana w pamięci telefonu.
    private val prefs = app.getSharedPreferences("alfa", Context.MODE_PRIVATE)
    private val shortcutKeys = MutableStateFlow(loadShortcutKeys())
    val shortcuts: StateFlow<List<AppEntry>> = combine(shortcutKeys, apps) { keys, list ->
        val byKey = list.associateBy { it.key }
        keys.mapNotNull(byKey::get)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Który skrót wybieramy: indeks istniejącego albo rozmiar listy = nowy skrót. */
    private val _picking = MutableStateFlow<Int?>(null)
    val picking: StateFlow<Int?> = _picking.asStateFlow()

    private val _settings = MutableStateFlow(LauncherSettings.load(prefsFolders))
    val settings: StateFlow<LauncherSettings> = _settings.asStateFlow()

    fun updateSettings(change: (LauncherSettings) -> LauncherSettings) {
        val updated = change(_settings.value)
        _settings.value = updated
        updated.save(prefsFolders)
        if (_lockOn.value) scheduleLockRefresh()
    }

    // Tapeta ekranu blokady w stylu Alfa – odświeżana po każdej zmianie wyglądu.
    private val _lockOn = MutableStateFlow(LockScreen.isOn(app))
    val lockOn: StateFlow<Boolean> = _lockOn.asStateFlow()
    private val _lockStatus = MutableStateFlow<String?>(null)
    val lockStatus: StateFlow<String?> = _lockStatus.asStateFlow()
    private var lockJob: Job? = null

    fun setLockScreen(on: Boolean) {
        lockJob?.cancel()
        lockJob = viewModelScope.launch {
            _lockStatus.value = if (on) "Ustawiam tapetę ekranu blokady…" else "Przywracam ekran blokady…"
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    if (on) LockScreen.apply(getApplication<Application>(), _settings.value) else LockScreen.clear(getApplication<Application>())
                }.isSuccess
            }
            if (ok) _lockOn.value = on
            _lockStatus.value = when {
                !ok -> "Telefon nie pozwolił zmienić tapety ekranu blokady."
                on -> "Gotowe. Zablokuj telefon, żeby zobaczyć."
                else -> "Ekran blokady ma znowu tapetę ekranu głównego."
            }
        }
    }

    private fun scheduleLockRefresh() {
        lockJob?.cancel()
        lockJob = viewModelScope.launch {
            kotlinx.coroutines.delay(800)
            withContext(Dispatchers.IO) { runCatching { LockScreen.apply(getApplication<Application>(), _settings.value) } }
        }
    }

    private val _settingsOpen = MutableStateFlow(false)
    val settingsOpen: StateFlow<Boolean> = _settingsOpen.asStateFlow()

    fun setSettingsOpen(open: Boolean) {
        _settingsOpen.value = open
        if (open) setPanel(false)
    }

    private val _homeTick = MutableStateFlow(0)

    /** Rośnie przy każdym naciśnięciu Home – ekran wraca wtedy do skrótów. */
    val homeTick: StateFlow<Int> = _homeTick.asStateFlow()

    fun onHomePressed() {
        _homeTick.value++
        setPanel(false)
        hideActions()
        closeFolder()
        _settingsOpen.value = false
    }

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
            createSuggestedFoldersOnce(list)
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
        if (!open) _picking.value = null
    }

    fun openPicker(index: Int) {
        _picking.value = index
    }

    fun closePicker() {
        _picking.value = null
    }

    /** Ustawia skrót o danym indeksie (indeks = liczba skrótów dodaje nowy); null usuwa skrót. */
    fun setShortcut(index: Int, entry: AppEntry?) {
        val visible = shortcuts.value.map { it.key }
        val updated = visible.toMutableList()
        when {
            entry == null -> if (index in updated.indices) updated.removeAt(index)
            index in updated.indices -> updated[index] = entry.key
            entry.key !in updated && updated.size < MAX_SHORTCUTS -> updated.add(entry.key)
        }
        val keys = updated.distinct()
        shortcutKeys.value = keys
        prefs.edit().putString("shortcuts", JSONArray(keys).toString()).apply()
    }

    private fun loadShortcutKeys(): List<String> {
        prefs.getString("shortcuts", null)?.let { json ->
            return runCatching { JSONArray(json).let { a -> List(a.length()) { a.getString(it) } } }.getOrDefault(emptyList())
        }
        // Przeniesienie ze starszej wersji (8 stałych miejsc).
        return (0 until 8).mapNotNull { prefs.getString("slot_$it", null) }.distinct()
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

    /** Przy pierwszym uruchomieniu sam układa aplikacje bankowe i gry w foldery. */
    private fun createSuggestedFoldersOnce(list: List<AppEntry>) {
        if (prefsFolders.getBoolean(AUTO_FOLDERS_DONE, false) || list.isEmpty()) return
        prefsFolders.edit().putBoolean(AUTO_FOLDERS_DONE, true).apply()
        if (folders.value.isNotEmpty()) return
        val suggested = suggestedFolders(list, folderStore::newId)
        if (suggested.isNotEmpty()) editFolders { suggested }
    }

    private fun editFolders(change: (List<Folder>) -> List<Folder>) {
        // Puste foldery znikają same.
        val updated = change(folders.value).filter { it.appKeys.isNotEmpty() }
        folders.value = updated
        folderStore.save(updated)
    }

    fun openHomeSettings() = repository.openHomeSettings()

    fun isDefaultHome() = repository.isDefaultHome()

    fun openDialer() = repository.openDialer()

    fun openMessages() = repository.openMessages()

    fun openDetails(entry: AppEntry) = repository.openDetails(entry)

    override fun onCleared() {
        stopObserving()
    }

    companion object {
        const val MAX_SHORTCUTS = 12
        private const val AUTO_FOLDERS_DONE = "auto_folders_v1"
    }
}
