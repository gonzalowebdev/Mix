package com.gonzalowebdev.mix.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFramePercent
import com.gonzalowebdev.mix.data.PlaylistItemEntity
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    vm: MixViewModel,
    playlistId: Long,
    onBack: () -> Unit,
    onPlay: () -> Unit
) {
    val playlist by remember(playlistId) { vm.playlist(playlistId) }
        .collectAsStateWithLifecycle(initialValue = null)
    val dbItems by remember(playlistId) { vm.playlistItems(playlistId) }
        .collectAsStateWithLifecycle(initialValue = emptyList<PlaylistItemEntity>())

    var items by remember { mutableStateOf(dbItems) }
    LaunchedEffect(dbItems) { items = dbItems }

    var menu by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        items = items.toMutableList().apply { add(to.index, removeAt(from.index)) }
    }

    fun play(start: Int) {
        vm.playQueue(items.map { it.toQueueItem() }, start)
        onPlay()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(playlist?.name ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(MixIcons.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(enabled = items.isNotEmpty(), onClick = { play(0) }) {
                        Icon(MixIcons.PlayArrow, contentDescription = "Reproducir")
                    }
                    Box {
                        IconButton(onClick = { menu = true }) {
                            Icon(MixIcons.MoreVert, contentDescription = "Opciones")
                        }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(
                                text = { Text("Renombrar") },
                                onClick = {
                                    menu = false
                                    renaming = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Eliminar lista") },
                                onClick = {
                                    menu = false
                                    deleting = true
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (items.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "La lista está vacía.\nVolvé a la biblioteca, mantené presionado para seleccionar archivos y tocá \"Agregar a lista\".",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp)
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.padding(padding).fillMaxSize()
            ) {
                items(items, key = { it.id }) { item ->
                    ReorderableItem(reorderState, key = item.id) { isDragging ->
                        val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp, label = "elevation")
                        Surface(shadowElevation = elevation) {
                            ListItem(
                                leadingContent = { Thumb(item) },
                                headlineContent = { Text(item.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                supportingContent = {
                                    Text(if (item.isVideo) "Video · ${formatDuration(item.durationMs)}" else "Imagen")
                                },
                                trailingContent = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { vm.removeItem(item) }) {
                                            Icon(MixIcons.Delete, contentDescription = "Quitar de la lista")
                                        }
                                        IconButton(
                                            onClick = {},
                                            modifier = Modifier.draggableHandle(
                                                onDragStopped = { vm.reorder(items.map { it.id }) }
                                            )
                                        ) {
                                            Icon(MixIcons.DragHandle, contentDescription = "Reordenar")
                                        }
                                    }
                                },
                                modifier = Modifier.clickable { play(items.indexOf(item)) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (renaming) {
        TextInputDialog(
            title = "Renombrar lista",
            confirmLabel = "Guardar",
            initial = playlist?.name ?: "",
            onConfirm = { name ->
                vm.renamePlaylist(playlistId, name)
                renaming = false
            },
            onDismiss = { renaming = false }
        )
    }
    if (deleting) {
        ConfirmDialog(
            title = "Eliminar lista",
            message = "¿Eliminar esta lista? Los archivos no se borran del dispositivo.",
            confirmLabel = "Eliminar",
            onConfirm = {
                deleting = false
                vm.deletePlaylist(playlistId)
                onBack()
            },
            onDismiss = { deleting = false }
        )
    }
}

@Composable
private fun Thumb(item: PlaylistItemEntity) {
    val context = LocalContext.current
    val request = remember(item.uri) {
        ImageRequest.Builder(context)
            .data(android.net.Uri.parse(item.uri))
            .size(160)
            .apply { if (item.isVideo) videoFramePercent(0.1) }
            .build()
    }
    AsyncImage(
        model = request,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    )
}
