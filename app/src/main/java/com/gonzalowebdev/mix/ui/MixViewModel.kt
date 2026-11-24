package com.gonzalowebdev.mix.ui

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gonzalowebdev.mix.MixApp
import com.gonzalowebdev.mix.data.FolderInfo
import com.gonzalowebdev.mix.data.MediaFile
import com.gonzalowebdev.mix.data.MediaRepository
import com.gonzalowebdev.mix.data.Memory
import com.gonzalowebdev.mix.data.MemoryGenerator
import com.gonzalowebdev.mix.data.PlaylistEntity
import com.gonzalowebdev.mix.data.PlaylistItemEntity
import com.gonzalowebdev.mix.data.Prefs
import com.gonzalowebdev.mix.data.QueueItem
import com.gonzalowebdev.mix.data.SmartList
import com.gonzalowebdev.mix.data.SortField
import com.gonzalowebdev.mix.data.TypeFilter
import androidx.core.content.pm.PackageInfoCompat
import com.gonzalowebdev.mix.data.MediaAnalysis
import com.gonzalowebdev.mix.data.MediaAnalyzer
import com.gonzalowebdev.mix.data.UpdateChecker
import com.gonzalowebdev.mix.data.UpdateInfo
import com.gonzalowebdev.mix.data.UpdateResult
import com.gonzalowebdev.mix.data.VisualStyle
import com.gonzalowebdev.mix.data.findDuplicateGroups
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.yield
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import kotlin.random.Random

class MixViewModel(app: Application) : AndroidViewModel(app) {

    val prefs = Prefs(app)
    private val repo = MediaRepository(app)
    private val dao = (app as MixApp).db.dao()
    private val analysisDao = (app as MixApp).analysisDb.dao()

    // ---------- Biblioteca ----------
    var allMedia by mutableStateOf<List<MediaFile>>(emptyList())
        private set
    var loading by mutableStateOf(false)
        private set
    var loadedOnce by mutableStateOf(false)
        private set

    var query by mutableStateOf("")

    /** La barra de búsqueda abierta y el texto viven juntos: cerrar una limpia la otra. */
    var searchOpen by mutableStateOf(false)

    fun closeSearch() {
        searchOpen = false
        query = ""
    }
    var typeFilter by mutableStateOf(
        try { TypeFilter.valueOf(prefs.typeFilter) } catch (e: Exception) { TypeFilter.ALL }
    )
    var folderFilter by mutableStateOf<String?>(prefs.folderFilter)
    var favoritesOnly by mutableStateOf(prefs.favoritesOnly)

    // Inicio en orden aleatorio (estilo mosaico)
    var sortField by mutableStateOf(
        try { SortField.valueOf(prefs.sortField) } catch (e: Exception) { SortField.RANDOM }
    )
    var sortAsc by mutableStateOf(prefs.sortAsc)
    var randomSeed by mutableLongStateOf(System.nanoTime())

    init {
        viewModelScope.launch {
            snapshotFlow {
                LibState(typeFilter.name, folderFilter, favoritesOnly, sortField.name, sortAsc)
            }.collect { st ->
                prefs.typeFilter = st.type
                prefs.folderFilter = st.folder
                prefs.favoritesOnly = st.fav
                prefs.sortField = st.sort
                prefs.sortAsc = st.asc
            }
        }
    }

    var mosaic by mutableStateOf(prefs.mosaic)
        private set

    fun updateMosaic(value: Boolean) {
        mosaic = value
        prefs.mosaic = value
    }

    fun reshuffle() {
        sortField = SortField.RANDOM
        randomSeed = System.nanoTime()
    }

    val selected = mutableStateListOf<String>()

    var favorites by mutableStateOf(prefs.favorites)
        private set

    // ---------- Carpetas a mostrar ----------
    var useAllFolders by mutableStateOf(prefs.useAllFolders)
        private set
    var selectedFolders by mutableStateOf(prefs.selectedFolders)
        private set

    /** Todas las carpetas del dispositivo (sin importar la selección). */
    val allFolders: List<FolderInfo> by derivedStateOf {
        allMedia.groupBy { it.folderId }
            .map { (id, files) -> FolderInfo(id, files.first().folder, files.size, files.first()) }
            .sortedBy { it.name.lowercase() }
    }

    /** Biblioteca restringida a las carpetas elegidas. */
    val scopedMedia: List<MediaFile> by derivedStateOf {
        if (useAllFolders) allMedia else allMedia.filter { it.folderId in selectedFolders }
    }

    fun updateUseAllFolders(value: Boolean) {
        if (!value && selectedFolders.isEmpty()) {
            selectedFolders = allFolders.map { it.id }.toSet()
            prefs.selectedFolders = selectedFolders
        }
        useAllFolders = value
        prefs.useAllFolders = value
        fixFolderFilter()
    }

    fun toggleFolder(id: String) {
        selectedFolders = if (id in selectedFolders) selectedFolders - id else selectedFolders + id
        prefs.selectedFolders = selectedFolders
        fixFolderFilter()
    }

    fun selectAllFolders() {
        selectedFolders = allFolders.map { it.id }.toSet()
        prefs.selectedFolders = selectedFolders
    }

    fun selectNoFolders() {
        selectedFolders = emptySet()
        prefs.selectedFolders = selectedFolders
        fixFolderFilter()
    }

    private fun fixFolderFilter() {
        val f = folderFilter
        if (f != null && f !in scopedMedia.map { it.folder }) folderFilter = null
    }

    val folders: List<String> by derivedStateOf {
        scopedMedia.map { it.folder }.distinct().sortedBy { it.lowercase() }
    }

    val visible: List<MediaFile> by derivedStateOf {
        var list = scopedMedia
        when (typeFilter) {
            TypeFilter.IMAGES -> list = list.filter { !it.isVideo }
            TypeFilter.VIDEOS -> list = list.filter { it.isVideo }
            TypeFilter.ALL -> Unit
        }
        folderFilter?.let { f -> list = list.filter { it.folder == f } }
        if (favoritesOnly) list = list.filter { it.key in favorites }
        val q = query.trim()
        if (q.isNotEmpty()) {
            list = list.filter {
                it.name.contains(q, ignoreCase = true) || it.folder.contains(q, ignoreCase = true)
            }
        }
        val sorted = when (sortField) {
            SortField.NAME -> list.sortedBy { it.name.lowercase() }
            SortField.DATE -> list.sortedBy { it.dateAdded }
            SortField.SIZE -> list.sortedBy { it.size }
            SortField.DURATION -> list.sortedBy { it.durationMs }
            SortField.RANDOM -> list.shuffled(Random(randomSeed))
        }
        if (sortField != SortField.RANDOM && !sortAsc) sorted.asReversed() else sorted
    }

    val selectedFiles: List<MediaFile> by derivedStateOf {
        val keys = selected.toSet()
        visible.filter { it.key in keys }
    }

    fun refresh() {
        viewModelScope.launch {
            loading = true
            try {
                val device = repo.load()
                val extras = repo.loadExtras(prefs.extraUris)
                allMedia = device + extras
                if (folderFilter != null && folderFilter !in folders) folderFilter = null
                loadedOnce = true
                startAnalysis()
            } finally {
                loading = false
            }
        }
    }

    fun toggleSelect(file: MediaFile) {
        if (!selected.remove(file.key)) selected.add(file.key)
    }

    fun selectAllVisible() {
        selected.clear()
        selected.addAll(visible.map { it.key })
    }

    fun clearSelection() = selected.clear()

    // ---------- Recuerdos ----------
    private val todayStr = LocalDate.now().toString()
    private val avoidIds: Set<String> = if (prefs.memDay == todayStr) prefs.memPrev else prefs.memIds

    var hiddenMemories by mutableStateOf(prefs.hiddenMemories)
        private set
    var pinnedMemories by mutableStateOf(prefs.pinnedMemories)
        private set

    val memories: List<Memory> by derivedStateOf {
        MemoryGenerator.generate(
            scopedMedia, favorites, LocalDate.now(), analysis, avoidIds, hiddenMemories, pinnedMemories
        )
    }

    /** Presentación inicial ("Un día como hoy · hace 3 años") del recuerdo que se está reproduciendo. */
    var memoryIntro by mutableStateOf<Memory?>(null)
        private set

    fun takeIntro(): Memory? {
        val m = memoryIntro
        memoryIntro = null
        return m
    }

    fun playMemory(memory: Memory) {
        playQueue(memory.files.map { it.toQueueItem() }, 0)
        memoryIntro = memory
    }

    /** Guarda qué recuerdos se mostraron hoy, para no repetirlos mañana. */
    fun recordMemoriesShown() {
        val ids = memories.map { it.id }.toSet()
        if (ids.isEmpty()) return
        if (prefs.memDay != todayStr) {
            prefs.memPrev = prefs.memIds
            prefs.memDay = todayStr
        }
        prefs.memIds = ids
    }

    fun hideMemory(id: String) {
        hiddenMemories = hiddenMemories + id
        pinnedMemories = pinnedMemories - id
        prefs.hiddenMemories = hiddenMemories
        prefs.pinnedMemories = pinnedMemories
    }

    fun togglePinMemory(id: String) {
        pinnedMemories = if (id in pinnedMemories) pinnedMemories - id else pinnedMemories + id
        prefs.pinnedMemories = pinnedMemories
    }

    fun unhideAllMemories() {
        hiddenMemories = emptySet()
        prefs.hiddenMemories = hiddenMemories
    }

    // ---------- Análisis de imágenes (nitidez y duplicados), sin IA ----------
    var analysis by mutableStateOf<Map<String, MediaAnalysis>>(emptyMap())
        private set
    var analyzing by mutableStateOf(false)
        private set
    var analysisDone by mutableIntStateOf(0)
        private set
    var analysisTotal by mutableIntStateOf(0)
        private set
    var duplicateGroups by mutableStateOf<List<List<MediaFile>>>(emptyList())
        private set
    private var analysisJob: Job? = null

    fun startAnalysis() {
        if (analysisJob?.isActive == true) return
        analysisJob = viewModelScope.launch(Dispatchers.Default) {
            withContext(Dispatchers.Main) { analyzing = true }
            val known = analysisDao.all().associateBy { it.uri }.toMutableMap()
            val images = withContext(Dispatchers.Main) { allMedia.filter { !it.isVideo } }
            val todo = images.filter { it.key !in known }
            val first = known.toMap()
            withContext(Dispatchers.Main) {
                analysis = first
                analysisTotal = todo.size
                analysisDone = 0
            }
            val batch = ArrayList<MediaAnalysis>()
            var sinceUi = 0
            for ((i, f) in todo.withIndex()) {
                ensureActive()
                val a = MediaAnalyzer.analyze(getApplication(), f)
                if (a != null) {
                    batch += a
                    known[a.uri] = a
                }
                sinceUi++
                if (batch.size >= 50) {
                    analysisDao.insertAll(batch.toList())
                    batch.clear()
                }
                if (sinceUi >= 300) {
                    sinceUi = 0
                    val snap = known.toMap()
                    withContext(Dispatchers.Main) {
                        analysis = snap
                        analysisDone = i + 1
                    }
                }
                yield()
            }
            if (batch.isNotEmpty()) analysisDao.insertAll(batch.toList())
            val snap = known.toMap()
            withContext(Dispatchers.Main) {
                analysis = snap
                analysisDone = todo.size
                analyzing = false
            }
            val media = withContext(Dispatchers.Main) { allMedia }
            val favs = withContext(Dispatchers.Main) { favorites }
            val groups = findDuplicateGroups(media, snap, favs)
            withContext(Dispatchers.Main) { duplicateGroups = groups }
        }
    }

    // ---------- Limpieza ----------
    val screenshots: List<MediaFile> by derivedStateOf {
        allMedia.filter { it.isScreenshot }.sortedByDescending { it.size }
    }
    val blurry: List<MediaFile> by derivedStateOf {
        allMedia.filter {
            !it.isVideo && !it.isScreenshot && it.key !in favorites &&
                (analysis[it.key]?.sharp ?: Float.MAX_VALUE) < MemoryGenerator.BLUR_THRESHOLD
        }.sortedBy { analysis[it.key]?.sharp ?: 0f }
    }
    val heavyVideos: List<MediaFile> by derivedStateOf {
        allMedia.filter { it.isVideo && it.size >= 100L * 1024 * 1024 }.sortedByDescending { it.size }
    }

    // ---------- Papelera ----------
    var trashed by mutableStateOf<List<MediaFile>>(emptyList())
        private set
    var trashLoading by mutableStateOf(false)
        private set

    fun loadTrash() {
        viewModelScope.launch {
            trashLoading = true
            try {
                trashed = repo.loadTrashed().sortedBy { it.expires }
            } finally {
                trashLoading = false
            }
        }
    }

    fun onTrashChanged() {
        loadTrash()
        refresh()
    }

    // ---------- Estilos visuales guardados ----------
    var styles by mutableStateOf(loadStyles())
        private set

    private fun loadStyles(): List<VisualStyle> = try {
        val arr = JSONArray(prefs.stylesJson)
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            VisualStyle(
                name = o.getString("name"),
                filter = o.getString("filter"),
                mirror = o.getInt("mirror"),
                tvMode = o.getInt("tv"),
                tvStatic = o.getBoolean("static"),
                ambient = o.getBoolean("ambient"),
                kenBurns = o.getBoolean("kb")
            )
        }
    } catch (e: Exception) {
        emptyList()
    }

    private fun saveStyles(list: List<VisualStyle>) {
        styles = list
        val arr = JSONArray()
        for (st in list) {
            val o = JSONObject()
            o.put("name", st.name)
            o.put("filter", st.filter)
            o.put("mirror", st.mirror)
            o.put("tv", st.tvMode)
            o.put("static", st.tvStatic)
            o.put("ambient", st.ambient)
            o.put("kb", st.kenBurns)
            arr.put(o)
        }
        prefs.stylesJson = arr.toString()
    }

    fun saveStyle(style: VisualStyle) {
        saveStyles(styles.filter { !it.name.equals(style.name, true) } + style)
    }

    fun deleteStyle(name: String) = saveStyles(styles.filter { it.name != name })

    // ---------- Búsquedas recientes y sugerencias ----------
    var recentSearches by mutableStateOf(prefs.recentSearches)
        private set

    val folderSuggestions: List<String> by derivedStateOf {
        scopedMedia.groupBy { it.folder }.entries
            .sortedByDescending { it.value.size }
            .take(6)
            .map { it.key }
    }

    fun rememberSearch(q: String) {
        val t = q.trim()
        if (t.length < 2) return
        recentSearches = (listOf(t) + recentSearches.filter { !it.equals(t, true) }).take(8)
        prefs.recentSearches = recentSearches
    }

    fun removeRecentSearch(q: String) {
        recentSearches = recentSearches.filter { it != q }
        prefs.recentSearches = recentSearches
    }

    // ---------- Selección arrastrando el dedo ----------
    val visibleKeys: Set<String> by derivedStateOf { visible.map { it.key }.toHashSet() }

    /** Devuelve true si el gesto va a seleccionar (false: va a quitar de la selección). */
    fun dragSelectStart(key: String): Boolean {
        if (key !in visibleKeys) return true
        val add = key !in selected
        setSelected(key, add)
        return add
    }

    fun dragSelectEnter(key: String, add: Boolean) {
        if (key in visibleKeys) setSelected(key, add)
    }

    private fun setSelected(key: String, add: Boolean) {
        if (add) {
            if (key !in selected) selected.add(key)
        } else {
            selected.remove(key)
        }
    }

    // ---------- Actualizaciones ----------
    var pendingUpdate by mutableStateOf<UpdateInfo?>(null)
    var updateStatus by mutableStateOf("")
        private set
    var checkingUpdate by mutableStateOf(false)
        private set

    fun currentBuild(): Int = try {
        val app = getApplication<Application>()
        PackageInfoCompat.getLongVersionCode(app.packageManager.getPackageInfo(app.packageName, 0)).toInt()
    } catch (e: Exception) {
        0
    }

    fun checkUpdate(manual: Boolean) {
        viewModelScope.launch {
            checkingUpdate = true
            when (val r = UpdateChecker.check(currentBuild())) {
                is UpdateResult.Available -> {
                    pendingUpdate = r.info
                    updateStatus = "Hay una versión nueva: ${r.info.tag}"
                }
                UpdateResult.UpToDate -> if (manual) updateStatus = "Ya tenés la última versión"
                UpdateResult.Error -> if (manual) updateStatus = "No se pudo consultar. Revisá tu conexión"
            }
            prefs.lastUpdateCheck = System.currentTimeMillis()
            checkingUpdate = false
        }
    }

    /** Si la persona lo activó, consulta como máximo una vez por día. */
    fun autoCheckUpdate() {
        if (prefs.autoUpdateCheck && System.currentTimeMillis() - prefs.lastUpdateCheck > 24L * 3600_000L) {
            checkUpdate(false)
        }
    }

    // ---------- Vistas previas de las listas (portadas en collage) ----------
    val playlistPreviews = dao.previews()
        .map { rows -> rows.groupBy { it.playlistId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    // ---------- Archivos eliminados ----------
    /** Limpia favoritos, listas, cola y biblioteca cuando se mueven a la papelera o se eliminan. */
    fun onFilesRemoved(uris: List<Uri>) {
        val keys = uris.map { it.toString() }.toSet()
        allMedia = allMedia.filter { it.key !in keys }
        selected.removeAll { it in keys }
        if (favorites.any { it in keys }) {
            favorites = favorites - keys
            prefs.favorites = favorites
        }
        queue = queue.filter { it.uri !in keys }
        keys.forEach { prefs.clearPosition(it) }
        duplicateGroups = duplicateGroups.map { g -> g.filter { it.key !in keys } }.filter { it.size >= 2 }
        viewModelScope.launch { keys.toList().chunked(500).forEach { analysisDao.delete(it) } }
        viewModelScope.launch {
            keys.toList().chunked(500).forEach { dao.deleteItemsByUri(it) }
        }
        refresh()
    }

    fun mediaFor(uri: String): MediaFile? = allMedia.firstOrNull { it.key == uri }

    fun toggleFavoriteKey(key: String) {
        favorites = if (key in favorites) favorites - key else favorites + key
        prefs.favorites = favorites
    }

    // ---------- Favoritos ----------
    fun toggleFavorite(files: List<MediaFile>) {
        val keys = files.map { it.key }.toSet()
        val allFav = files.isNotEmpty() && files.all { it.key in favorites }
        favorites = if (allFav) favorites - keys else favorites + keys
        prefs.favorites = favorites
    }

    // ---------- Archivos agregados manualmente ----------
    fun addExtras(uris: List<Uri>) {
        prefs.extraUris = prefs.extraUris + uris.map { it.toString() }
        refresh()
    }

    fun removeExtras(keys: Collection<String>) {
        prefs.extraUris = prefs.extraUris - keys.toSet()
        refresh()
    }

    // ---------- Reproductor ----------
    var queue by mutableStateOf<List<QueueItem>>(emptyList())
        private set
    var startIndex by mutableIntStateOf(0)
        private set

    /** Los marca la pantalla del reproductor / la actividad para imagen en imagen. */
    var pipReady by mutableStateOf(false)
    var inPip by mutableStateOf(false)
    var pipPlaying by mutableStateOf(true)
    var pipToggleCount by mutableIntStateOf(0)

    fun playQueue(items: List<QueueItem>, start: Int) {
        queue = items
        startIndex = start.coerceIn(0, (items.size - 1).coerceAtLeast(0))
    }

    fun playFromLibrary(file: MediaFile) {
        if (query.isNotBlank()) rememberSearch(query)
        val list = visible
        playQueue(list.map { it.toQueueItem() }, list.indexOfFirst { it.key == file.key })
    }

    fun playSelected() {
        playQueue(selectedFiles.map { it.toQueueItem() }, 0)
    }

    fun playExternal(uri: Uri) {
        val cr = getApplication<Application>().contentResolver
        val mime = cr.getType(uri) ?: ""
        var name: String? = null
        try {
            cr.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) name = c.getString(0)
            }
        } catch (e: Exception) {
            // nombre no disponible
        }
        playQueue(
            listOf(QueueItem(uri.toString(), name ?: uri.lastPathSegment ?: "Archivo", mime.startsWith("video"), 0L)),
            0
        )
    }

    // ---------- Listas de reproducción ----------
    val playlists = dao.playlists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun playlist(id: Long) = dao.playlist(id)
    fun playlistItems(id: Long) = dao.items(id)

    fun createPlaylist(name: String, items: List<QueueItem>) {
        viewModelScope.launch {
            val id = dao.insertPlaylist(
                PlaylistEntity(name = name.trim().ifEmpty { "Lista" }, createdAt = System.currentTimeMillis())
            )
            if (items.isNotEmpty()) dao.addItems(id, items)
        }
    }

    fun addToPlaylist(id: Long, items: List<QueueItem>) {
        viewModelScope.launch { dao.addItems(id, items) }
    }

    fun renamePlaylist(id: Long, name: String) {
        viewModelScope.launch { dao.rename(id, name.trim()) }
    }

    fun deletePlaylist(id: Long) {
        viewModelScope.launch { dao.deletePlaylist(id) }
    }

    fun removeItem(item: PlaylistItemEntity) {
        viewModelScope.launch { dao.deleteItem(item.id) }
    }

    fun reorder(ids: List<Long>) {
        viewModelScope.launch { dao.reorder(ids) }
    }

    // ---------- Listas inteligentes ----------
    var smartLists by mutableStateOf(loadSmart())
        private set

    private fun loadSmart(): List<SmartList> = try {
        val arr = JSONArray(prefs.smartListsJson)
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            SmartList(
                id = o.getLong("id"),
                name = o.getString("name"),
                type = TypeFilter.valueOf(o.getString("type")),
                folder = if (o.isNull("folder")) null else o.getString("folder"),
                minMinutes = o.getInt("min"),
                maxMinutes = o.getInt("max"),
                query = o.getString("query"),
                favoritesOnly = o.getBoolean("fav")
            )
        }
    } catch (e: Exception) {
        emptyList()
    }

    private fun smartToJson(list: List<SmartList>): JSONArray {
        val arr = JSONArray()
        for (s in list) {
            val o = JSONObject()
            o.put("id", s.id)
            o.put("name", s.name)
            o.put("type", s.type.name)
            o.put("folder", s.folder ?: JSONObject.NULL)
            o.put("min", s.minMinutes)
            o.put("max", s.maxMinutes)
            o.put("query", s.query)
            o.put("fav", s.favoritesOnly)
            arr.put(o)
        }
        return arr
    }

    private fun saveSmart(list: List<SmartList>) {
        smartLists = list
        prefs.smartListsJson = smartToJson(list).toString()
    }

    fun saveSmartList(s: SmartList) {
        val list = smartLists.toMutableList()
        val i = list.indexOfFirst { it.id == s.id }
        if (i >= 0) list[i] = s else list.add(s)
        saveSmart(list)
    }

    fun deleteSmartList(id: Long) = saveSmart(smartLists.filter { it.id != id })

    fun smartMedia(s: SmartList): List<MediaFile> {
        val q = s.query.trim()
        return scopedMedia.filter { f ->
            (s.type == TypeFilter.ALL ||
                (s.type == TypeFilter.IMAGES && !f.isVideo) ||
                (s.type == TypeFilter.VIDEOS && f.isVideo)) &&
                (s.folder == null || f.folder == s.folder) &&
                (s.minMinutes <= 0 || (f.isVideo && f.durationMs >= s.minMinutes * 60_000L)) &&
                (s.maxMinutes <= 0 || (f.isVideo && f.durationMs <= s.maxMinutes * 60_000L)) &&
                (q.isEmpty() || f.name.contains(q, ignoreCase = true)) &&
                (!s.favoritesOnly || f.key in favorites)
        }.sortedByDescending { it.dateAdded }
    }

    fun playSmart(s: SmartList) {
        val list = smartMedia(s)
        if (list.isNotEmpty()) playQueue(list.map { it.toQueueItem() }, 0)
    }

    // ---------- Exportar / importar listas ----------
    private suspend fun exportJson(includePrefs: Boolean): String {
        val lists = dao.allPlaylists()
        val grouped = dao.allItems().groupBy { it.playlistId }
        val root = JSONObject()
        root.put("app", "Mix")
        root.put("version", 1)
        val arr = JSONArray()
        for (p in lists) {
            val o = JSONObject()
            o.put("name", p.name)
            val itemsArr = JSONArray()
            for (item in grouped[p.id].orEmpty()) {
                val io = JSONObject()
                io.put("uri", item.uri)
                io.put("name", item.name)
                io.put("isVideo", item.isVideo)
                io.put("durationMs", item.durationMs)
                itemsArr.put(io)
            }
            o.put("items", itemsArr)
            arr.put(o)
        }
        root.put("playlists", arr)
        root.put("smartLists", smartToJson(smartLists))
        if (includePrefs) root.put("prefs", prefs.dumpJson())
        return root.toString(2)
    }

    fun exportTo(uri: Uri, includePrefs: Boolean = false, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = try {
                val json = exportJson(includePrefs)
                withContext(Dispatchers.IO) {
                    val out = getApplication<Application>().contentResolver.openOutputStream(uri, "w")
                        ?: throw IllegalStateException("sin salida")
                    out.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                }
                true
            } catch (e: Exception) {
                false
            }
            onDone(ok)
        }
    }

    fun importFrom(uri: Uri, onDone: (Int) -> Unit) {
        viewModelScope.launch {
            val count = try {
                val text = withContext(Dispatchers.IO) {
                    val input = getApplication<Application>().contentResolver.openInputStream(uri)
                        ?: throw IllegalStateException("sin entrada")
                    input.use { String(it.readBytes(), Charsets.UTF_8) }
                }
                importJson(text)
            } catch (e: Exception) {
                -1
            }
            onDone(count)
        }
    }

    private suspend fun importJson(text: String): Int {
        val root = JSONObject(text)
        val arr = root.getJSONArray("playlists")
        var count = 0
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val id = dao.insertPlaylist(
                PlaylistEntity(name = o.getString("name"), createdAt = System.currentTimeMillis())
            )
            val itemsArr = o.getJSONArray("items")
            val items = (0 until itemsArr.length()).map { j ->
                val io = itemsArr.getJSONObject(j)
                QueueItem(io.getString("uri"), io.getString("name"), io.getBoolean("isVideo"), io.getLong("durationMs"))
            }
            if (items.isNotEmpty()) dao.addItems(id, items)
            count++
        }
        if (root.has("smartLists")) {
            val sArr = root.getJSONArray("smartLists")
            val incoming = ArrayList<SmartList>()
            val tmp = loadSmartFrom(sArr)
            tmp.forEachIndexed { index, s -> incoming += s.copy(id = System.nanoTime() + index) }
            if (incoming.isNotEmpty()) saveSmart(smartLists + incoming)
        }
        if (root.has("prefs")) {
            prefs.restoreJson(root.getJSONObject("prefs"))
            withContext(Dispatchers.Main) { reloadFromPrefs() }
        }
        return count
    }

    /** Vuelve a leer los ajustes guardados después de importar un respaldo. */
    private fun reloadFromPrefs() {
        favorites = prefs.favorites
        mosaic = prefs.mosaic
        smartLists = loadSmart()
        styles = loadStyles()
        hiddenMemories = prefs.hiddenMemories
        pinnedMemories = prefs.pinnedMemories
        recentSearches = prefs.recentSearches
        useAllFolders = prefs.useAllFolders
        selectedFolders = prefs.selectedFolders
        typeFilter = try { TypeFilter.valueOf(prefs.typeFilter) } catch (e: Exception) { TypeFilter.ALL }
        sortField = try { SortField.valueOf(prefs.sortField) } catch (e: Exception) { SortField.RANDOM }
        sortAsc = prefs.sortAsc
        favoritesOnly = prefs.favoritesOnly
        folderFilter = prefs.folderFilter
        refresh()
    }

    private fun loadSmartFrom(arr: JSONArray): List<SmartList> = try {
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            SmartList(
                id = o.getLong("id"),
                name = o.getString("name"),
                type = TypeFilter.valueOf(o.getString("type")),
                folder = if (o.isNull("folder")) null else o.getString("folder"),
                minMinutes = o.getInt("min"),
                maxMinutes = o.getInt("max"),
                query = o.getString("query"),
                favoritesOnly = o.getBoolean("fav")
            )
        }
    } catch (e: Exception) {
        emptyList()
    }
}

private data class LibState(
    val type: String,
    val folder: String?,
    val fav: Boolean,
    val sort: String,
    val asc: Boolean
)
