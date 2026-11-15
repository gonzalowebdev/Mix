package com.gonzalowebdev.mix.ui

import android.app.Application
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gonzalowebdev.mix.MixApp
import com.gonzalowebdev.mix.data.MediaFile
import com.gonzalowebdev.mix.data.MediaRepository
import com.gonzalowebdev.mix.data.PlaylistEntity
import com.gonzalowebdev.mix.data.PlaylistItemEntity
import com.gonzalowebdev.mix.data.Prefs
import com.gonzalowebdev.mix.data.QueueItem
import com.gonzalowebdev.mix.data.SortField
import com.gonzalowebdev.mix.data.TypeFilter
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

class MixViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = MediaRepository(app)
    private val dao = (app as MixApp).db.dao()
    val prefs = Prefs(app)

    // ---------- Biblioteca ----------
    var allMedia by mutableStateOf<List<MediaFile>>(emptyList())
        private set
    var loading by mutableStateOf(false)
        private set
    var loadedOnce by mutableStateOf(false)
        private set

    var query by mutableStateOf("")
    var typeFilter by mutableStateOf(TypeFilter.ALL)
    var folderFilter by mutableStateOf<String?>(null)
    var sortField by mutableStateOf(SortField.DATE)
    var sortAsc by mutableStateOf(false)
    var randomSeed by mutableLongStateOf(System.nanoTime())

    val selected = mutableStateListOf<String>()

    val folders: List<String> by derivedStateOf {
        allMedia.map { it.folder }.distinct().sortedBy { it.lowercase() }
    }

    val visible: List<MediaFile> by derivedStateOf {
        var list = allMedia
        when (typeFilter) {
            TypeFilter.IMAGES -> list = list.filter { !it.isVideo }
            TypeFilter.VIDEOS -> list = list.filter { it.isVideo }
            TypeFilter.ALL -> Unit
        }
        folderFilter?.let { f -> list = list.filter { it.folder == f } }
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
                allMedia = repo.load()
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

    // ---------- Reproductor ----------
    var queue by mutableStateOf<List<QueueItem>>(emptyList())
        private set
    var startIndex by mutableIntStateOf(0)
        private set

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
}
