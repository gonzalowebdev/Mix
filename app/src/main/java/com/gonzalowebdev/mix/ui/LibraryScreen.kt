package com.gonzalowebdev.mix.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFramePercent
import com.gonzalowebdev.mix.data.MediaFile
import com.gonzalowebdev.mix.data.SortField
import com.gonzalowebdev.mix.data.TypeFilter
import kotlinx.coroutines.launch
import kotlin.math.abs

private fun requiredPermissions(): Array<String> = when {
    Build.VERSION.SDK_INT >= 34 -> arrayOf(
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VIDEO,
        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
    )
    Build.VERSION.SDK_INT >= 33 -> arrayOf(
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VIDEO
    )
    else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
}

private fun hasAccess(context: android.content.Context, perms: Array<String>): Boolean =
    perms.any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    vm: MixViewModel,
    onPlay: () -> Unit,
    onPlaylists: () -> Unit
) {
    val context = LocalContext.current
    val perms = remember { requiredPermissions() }
    var access by remember { mutableStateOf(hasAccess(context, perms)) }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        access = hasAccess(context, perms)
        if (access) vm.refresh()
    }

    LaunchedEffect(Unit) {
        if (access) {
            if (!vm.loadedOnce) vm.refresh()
        } else {
            permLauncher.launch(perms)
        }
    }

    val playlists by vm.playlists.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var searching by remember { mutableStateOf(false) }
    var showAdd by remember { mutableStateOf(false) }
    var topMenu by remember { mutableStateOf(false) }
    var selMenu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    val selecting = vm.selected.isNotEmpty()

    LaunchedEffect(searching) { if (searching) focus.requestFocus() }

    BackHandler(enabled = selecting || searching) {
        if (selecting) {
            vm.clearSelection()
        } else {
            searching = false
            vm.query = ""
        }
    }

    // Agregar archivos desde el selector de documentos (tarjeta SD, carpetas especiales...)
    val addFilesLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach { u ->
                try {
                    context.contentResolver.takePersistableUriPermission(u, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (e: Exception) {
                    // permiso no persistible
                }
            }
            vm.addExtras(uris)
            scope.launch { snackbar.showSnackbar("${uris.size} archivos agregados") }
        }
    }

    // Eliminar archivos de la galería
    val deleteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            vm.clearSelection()
            vm.refresh()
        }
    }

    fun deleteSelected() {
        val uris = vm.selectedFiles.filter { it.uri.authority == MediaStore.AUTHORITY }.map { it.uri }
        if (uris.isEmpty()) {
            scope.launch { snackbar.showSnackbar("Solo se pueden eliminar archivos de la galería") }
            return
        }
        if (Build.VERSION.SDK_INT >= 30) {
            val sender = MediaStore.createDeleteRequest(context.contentResolver, uris).intentSender
            deleteLauncher.launch(IntentSenderRequest.Builder(sender).build())
        } else {
            var failed = false
            for (u in uris) {
                try {
                    context.contentResolver.delete(u, null, null)
                } catch (e: Exception) {
                    failed = true
                }
            }
            vm.clearSelection()
            vm.refresh()
            if (failed) scope.launch { snackbar.showSnackbar("No se pudieron eliminar algunos archivos") }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            if (selecting) {
                TopAppBar(
                    title = { Text("${vm.selectedFiles.size} seleccionados") },
                    navigationIcon = {
                        IconButton(onClick = { vm.clearSelection() }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancelar selección")
                        }
                    },
                    actions = {
                        IconButton(onClick = { vm.selectAllVisible() }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Seleccionar todo")
                        }
                        IconButton(onClick = {
                            vm.playSelected()
                            vm.clearSelection()
                            onPlay()
                        }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Reproducir selección")
                        }
                        IconButton(onClick = { showAdd = true }) {
                            Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = "Agregar a lista")
                        }
                        Box {
                            IconButton(onClick = { selMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Más opciones")
                            }
                            DropdownMenu(expanded = selMenu, onDismissRequest = { selMenu = false }) {
                                val files = vm.selectedFiles
                                val allFav = files.isNotEmpty() && files.all { it.key in vm.favorites }
                                DropdownMenuItem(
                                    text = { Text(if (allFav) "Quitar de favoritos" else "Agregar a favoritos") },
                                    onClick = {
                                        selMenu = false
                                        vm.toggleFavorite(files)
                                        vm.clearSelection()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Compartir") },
                                    onClick = {
                                        selMenu = false
                                        shareUris(
                                            context,
                                            files.map { it.uri },
                                            allImages = files.all { !it.isVideo },
                                            allVideos = files.all { it.isVideo }
                                        )
                                    }
                                )
                                if (files.any { it.isExtra }) {
                                    DropdownMenuItem(
                                        text = { Text("Quitar de la biblioteca") },
                                        onClick = {
                                            selMenu = false
                                            vm.removeExtras(files.filter { it.isExtra }.map { it.key })
                                            vm.clearSelection()
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("Eliminar del dispositivo") },
                                    onClick = {
                                        selMenu = false
                                        confirmDelete = true
                                    }
                                )
                            }
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = {
                        if (searching) {
                            TextField(
                                value = vm.query,
                                onValueChange = { vm.query = it },
                                placeholder = { Text("Buscar por nombre o carpeta") },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focus)
                            )
                        } else {
                            Text("Mix")
                        }
                    },
                    navigationIcon = {
                        if (searching) {
                            IconButton(onClick = {
                                searching = false
                                vm.query = ""
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cerrar búsqueda")
                            }
                        }
                    },
                    actions = {
                        if (searching) {
                            if (vm.query.isNotEmpty()) {
                                IconButton(onClick = { vm.query = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Borrar texto")
                                }
                            }
                        } else {
                            IconButton(onClick = { vm.reshuffle() }) {
                                Icon(Icons.Default.Shuffle, contentDescription = "Mezclar")
                            }
                            IconButton(onClick = { searching = true }) {
                                Icon(Icons.Default.Search, contentDescription = "Buscar")
                            }
                            IconButton(onClick = onPlaylists) {
                                Icon(Icons.AutoMirrored.Filled.PlaylistPlay, contentDescription = "Listas de reproducción")
                            }
                            Box {
                                IconButton(onClick = { topMenu = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "Más opciones")
                                }
                                DropdownMenu(expanded = topMenu, onDismissRequest = { topMenu = false }) {
                                    DropdownMenuItem(
                                        text = { Text("Agregar archivos…") },
                                        onClick = {
                                            topMenu = false
                                            addFilesLauncher.launch(arrayOf("image/*", "video/*"))
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Actualizar biblioteca") },
                                        onClick = {
                                            topMenu = false
                                            if (access) vm.refresh() else permLauncher.launch(perms)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(if (vm.mosaic) "Vista: cuadrícula" else "Vista: mosaico") },
                                        onClick = {
                                            topMenu = false
                                            vm.setMosaic(!vm.mosaic)
                                        }
                                    )
                                }
                            }
                        }
                    }
                )
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (!access) {
                PermissionPrompt(
                    onGrant = { permLauncher.launch(perms) },
                    onSettings = {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                        )
                    }
                )
            } else {
                FilterBar(vm)
                Text(
                    text = "${vm.visible.size} archivos",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                Box(Modifier.fillMaxSize()) {
                    when {
                        vm.loading && vm.allMedia.isEmpty() -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                        vm.visible.isEmpty() -> Text(
                            text = if (vm.allMedia.isEmpty()) "No se encontraron imágenes ni videos" else "Sin resultados",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.align(Alignment.Center).padding(24.dp)
                        )
                        else -> MediaGrid(vm, onPlay)
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "Eliminar archivos",
            message = "Se van a eliminar ${vm.selectedFiles.size} archivos del dispositivo. Esta acción no se puede deshacer.",
            confirmLabel = "Eliminar",
            onConfirm = {
                confirmDelete = false
                deleteSelected()
            },
            onDismiss = { confirmDelete = false }
        )
    }

    if (showAdd) {
        AddToPlaylistDialog(
            playlists = playlists,
            onPick = { p ->
                vm.addToPlaylist(p.playlist.id, vm.selectedFiles.map { it.toQueueItem() })
                vm.clearSelection()
                showAdd = false
                scope.launch { snackbar.showSnackbar("Agregado a \"${p.playlist.name}\"") }
            },
            onCreate = { name ->
                vm.createPlaylist(name, vm.selectedFiles.map { it.toQueueItem() })
                vm.clearSelection()
                showAdd = false
                scope.launch { snackbar.showSnackbar("Lista \"$name\" creada") }
            },
            onDismiss = { showAdd = false }
        )
    }
}

@Composable
private fun PermissionPrompt(onGrant: () -> Unit, onSettings: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Mix necesita acceso a tus fotos y videos para mostrarlos y reproducirlos.",
            textAlign = TextAlign.Center
        )
        Button(onClick = onGrant, modifier = Modifier.padding(top = 16.dp)) { Text("Dar permiso") }
        OutlinedButton(onClick = onSettings, modifier = Modifier.padding(top = 8.dp)) { Text("Abrir ajustes") }
    }
}

@Composable
private fun FilterBar(vm: MixViewModel) {
    var folderMenu by remember { mutableStateOf(false) }
    var sortMenu by remember { mutableStateOf(false) }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = vm.typeFilter == TypeFilter.ALL,
                onClick = { vm.typeFilter = TypeFilter.ALL },
                label = { Text("Todo") }
            )
        }
        item {
            FilterChip(
                selected = vm.typeFilter == TypeFilter.IMAGES,
                onClick = { vm.typeFilter = TypeFilter.IMAGES },
                label = { Text("Imágenes") }
            )
        }
        item {
            FilterChip(
                selected = vm.typeFilter == TypeFilter.VIDEOS,
                onClick = { vm.typeFilter = TypeFilter.VIDEOS },
                label = { Text("Videos") }
            )
        }
        item {
            FilterChip(
                selected = vm.favoritesOnly,
                onClick = { vm.favoritesOnly = !vm.favoritesOnly },
                label = { Text("Favoritos") },
                leadingIcon = { Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }
        item {
            Box {
                FilterChip(
                    selected = vm.folderFilter != null,
                    onClick = { folderMenu = true },
                    label = { Text(vm.folderFilter ?: "Carpeta", maxLines = 1) },
                    leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                DropdownMenu(expanded = folderMenu, onDismissRequest = { folderMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Todas las carpetas") },
                        onClick = {
                            vm.folderFilter = null
                            folderMenu = false
                        }
                    )
                    vm.folders.forEach { folder ->
                        DropdownMenuItem(
                            text = { Text(folder) },
                            onClick = {
                                vm.folderFilter = folder
                                folderMenu = false
                            }
                        )
                    }
                }
            }
        }
        item {
            Box {
                val arrow = when {
                    vm.sortField == SortField.RANDOM -> ""
                    vm.sortAsc -> " ↑"
                    else -> " ↓"
                }
                FilterChip(
                    selected = false,
                    onClick = { sortMenu = true },
                    label = { Text(vm.sortField.label + arrow) },
                    leadingIcon = { Icon(Icons.Default.SwapVert, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                    SortField.entries.forEach { field ->
                        DropdownMenuItem(
                            text = { Text(field.label) },
                            onClick = {
                                if (field == SortField.RANDOM) {
                                    vm.randomSeed = System.nanoTime()
                                } else if (vm.sortField == field) {
                                    vm.sortAsc = !vm.sortAsc
                                } else {
                                    vm.sortAsc = field == SortField.NAME
                                }
                                vm.sortField = field
                                sortMenu = false
                            }
                        )
                    }
                }
            }
        }
    }
}

private val bentoRatios = listOf(0.8f, 1f, 1.25f, 0.9f, 1.4f)

private fun aspectFor(file: MediaFile, mosaic: Boolean): Float {
    if (!mosaic) return 1f
    val variety = bentoRatios[abs(file.key.hashCode()) % bentoRatios.size]
    if (file.width <= 0 || file.height <= 0) return variety
    val r = (file.width.toFloat() / file.height).coerceIn(0.6f, 1.7f)
    // Las fotos casi cuadradas toman proporciones variadas para lograr el efecto mosaico
    return if (r in 0.85f..1.25f) variety else r
}

@Composable
private fun MediaGrid(vm: MixViewModel, onPlay: () -> Unit) {
    val selectedKeys = vm.selected
    val selecting = selectedKeys.isNotEmpty()
    val favorites = vm.favorites

    if (vm.mosaic) {
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Adaptive(150.dp),
            contentPadding = PaddingValues(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalItemSpacing = 6.dp,
            modifier = Modifier.fillMaxSize()
        ) {
            items(vm.visible, key = { it.key }, contentType = { it.isVideo }) { file ->
                MediaCell(
                    file = file,
                    aspect = aspectFor(file, true),
                    selected = file.key in selectedKeys,
                    selecting = selecting,
                    favorite = file.key in favorites,
                    onClick = {
                        if (selecting) {
                            vm.toggleSelect(file)
                        } else {
                            vm.playFromLibrary(file)
                            onPlay()
                        }
                    },
                    onLongClick = { vm.toggleSelect(file) }
                )
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(104.dp),
            contentPadding = PaddingValues(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(vm.visible, key = { it.key }, contentType = { it.isVideo }) { file ->
                MediaCell(
                    file = file,
                    aspect = 1f,
                    selected = file.key in selectedKeys,
                    selecting = selecting,
                    favorite = file.key in favorites,
                    onClick = {
                        if (selecting) {
                            vm.toggleSelect(file)
                        } else {
                            vm.playFromLibrary(file)
                            onPlay()
                        }
                    },
                    onLongClick = { vm.toggleSelect(file) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaCell(
    file: MediaFile,
    aspect: Float,
    selected: Boolean,
    selecting: Boolean,
    favorite: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    val request = remember(file.key) {
        ImageRequest.Builder(context)
            .data(file.uri)
            .size(400)
            .apply { if (file.isVideo) videoFramePercent(0.1) }
            .build()
    }
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(aspect)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        AsyncImage(
            model = request,
            contentDescription = file.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        if (file.isVideo) {
            Row(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
                    .background(Color(0x99000000), RoundedCornerShape(6.dp))
                    .padding(horizontal = 5.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                Text(formatDuration(file.durationMs), color = Color.White, style = MaterialTheme.typography.labelSmall)
            }
        }
        if (favorite) {
            Icon(
                Icons.Default.Favorite,
                contentDescription = "Favorito",
                tint = Color(0xFFFF5C7A),
                modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp).size(18.dp)
            )
        }
        if (selected) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)))
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = "Seleccionado",
                tint = Color.White,
                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)
            )
        } else if (selecting) {
            Icon(
                Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)
            )
        }
    }
}
