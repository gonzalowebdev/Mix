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
import kotlinx.coroutines.Dispatchers
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

    // ---------- Biblioteca ----------
    var allMedia by mutableStateOf<List<MediaFile>>(emptyList())
        private set
    var loading by mutableStateOf(false)
        private set
    var loadedOnce by mutableStateOf(false)
        private set

    var query by mutableStateOf("")
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
    val memories: List<Memory> by derivedStateOf {
        MemoryGenerator.generate(scopedMedia, favorites, LocalDate.now())
    }

    fun playMemory(memory: Memory) {
        playQueue(memory.files.map { it.toQueueItem() }, 0)
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

    fun playQueue(items: List<QueueItem>, start: Int) {
        queue = items
        startIndex = start.coerceIn(0, (items.size - 1).coerceAtLeast(0))
    }

    fun playFromLibrary(file: MediaFile) {
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
    private suspend fun exportJson(): String {
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
        return root.toString(2)
    }

    fun exportTo(uri: Uri, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = try {
                val json = exportJson()
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
        return count
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
