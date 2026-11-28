package com.gonzalowebdev.mix.ui

import android.Manifest
import android.view.LayoutInflater
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.gonzalowebdev.mix.R
import com.gonzalowebdev.mix.data.MediaThumb
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import kotlinx.coroutines.delay
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
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
import com.gonzalowebdev.mix.data.Memory
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
    onFolders: () -> Unit,
    onTrash: () -> Unit,
    onSettings: () -> Unit
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
    val searching = vm.searchOpen
    var showAdd by remember { mutableStateOf(false) }
    var topMenu by remember { mutableStateOf(false) }
    var selMenu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    val selecting = vm.selected.isNotEmpty()

    LaunchedEffect(Unit) { vm.autoCheckUpdate() }

    LaunchedEffect(searching) { if (searching && vm.query.isEmpty()) focus.requestFocus() }

    BackHandler(enabled = selecting || searching) {
        if (selecting) {
            vm.clearSelection()
        } else {
            vm.closeSearch()
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

    // Eliminar: papelera del sistema (o definitivo), siempre con confirmación
    val remover = rememberFileRemover { uris, ok ->
        if (ok) {
            vm.onFilesRemoved(uris)
            vm.clearSelection()
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
                            Icon(MixIcons.Close, contentDescription = "Cancelar selección")
                        }
                    },
                    actions = {
                        IconButton(onClick = { vm.selectAllVisible() }) {
                            Icon(MixIcons.SelectAll, contentDescription = "Seleccionar todo")
                        }
                        IconButton(onClick = {
                            vm.playSelected()
                            vm.clearSelection()
                            onPlay()
                        }) {
                            Icon(MixIcons.PlayArrow, contentDescription = "Reproducir selección")
                        }
                        IconButton(onClick = { showAdd = true }) {
                            Icon(MixIcons.PlaylistAdd, contentDescription = "Agregar a lista")
                        }
                        Box {
                            IconButton(onClick = { selMenu = true }) {
                                Icon(MixIcons.MoreVert, contentDescription = "Más opciones")
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
                                    text = { Text("Eliminar…") },
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
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { vm.rememberSearch(vm.query) }),
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    MixIcons.Logo,
                                    contentDescription = null,
                                    tint = MixColors.Pink,
                                    modifier = Modifier.size(30.dp)
                                )
                                Text(
                                    "mix",
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        if (searching) {
                            IconButton(onClick = { vm.closeSearch() }) {
                                Icon(MixIcons.ArrowBack, contentDescription = "Cerrar búsqueda")
                            }
                        }
                    },
                    actions = {
                        if (searching) {
                            if (vm.query.isNotEmpty()) {
                                IconButton(onClick = { vm.query = "" }) {
                                    Icon(MixIcons.Clear, contentDescription = "Borrar texto")
                                }
                            }
                        } else {
                            IconButton(onClick = { vm.reshuffle() }) {
                                Icon(MixIcons.Shuffle, contentDescription = "Mezclar")
                            }
                            IconButton(onClick = { vm.searchOpen = true }) {
                                Icon(MixIcons.Search, contentDescription = "Buscar")
                            }
                            Box {
                                IconButton(onClick = { topMenu = true }) {
                                    Icon(MixIcons.MoreVert, contentDescription = "Más opciones")
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
                                        text = { Text("Papelera") },
                                        onClick = {
                                            topMenu = false
                                            onTrash()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Ajustes") },
                                        onClick = {
                                            topMenu = false
                                            onSettings()
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
                                            vm.updateMosaic(!vm.mosaic)
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
                FilterBar(vm, onFolders)
                Text(
                    text = "${vm.visible.size} archivos",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                Box(Modifier.fillMaxSize()) {
                    when {
                        searching && vm.query.isBlank() -> SearchSuggestions(vm)
                        vm.loading && vm.allMedia.isEmpty() -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                        vm.visible.isEmpty() -> Text(
                            text = when {
                                vm.allMedia.isEmpty() -> "No se encontraron imágenes ni videos"
                                vm.scopedMedia.isEmpty() -> "No hay carpetas seleccionadas.\nElegí cuáles mostrar desde el chip \"Carpetas\"."
                                else -> "Sin resultados"
                            },
                            textAlign = TextAlign.Center,
                            modifier = Modifier.align(Alignment.Center).padding(24.dp)
                        )
                        else -> {
                            var refreshing by remember { mutableStateOf(false) }
                            PullToRefreshBox(
                                isRefreshing = refreshing,
                                onRefresh = {
                                    refreshing = true
                                    if (vm.sortField == SortField.RANDOM) vm.reshuffle() else vm.refresh()
                                    scope.launch {
                                        delay(700)
                                        refreshing = false
                                    }
                                }
                            ) { MediaGrid(vm, onPlay) }
                        }
                    }
                }
            }
        }
    }

    vm.pendingUpdate?.let { info ->
        AlertDialog(
            onDismissRequest = { vm.pendingUpdate = null },
            title = { Text("Hay una versión nueva") },
            text = { Text("Está disponible ${info.tag}. Se descarga desde el navegador y después la instalás.") },
            confirmButton = {
                TextButton(onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(info.url)))
                    vm.pendingUpdate = null
                }) { Text("Descargar") }
            },
            dismissButton = { TextButton(onClick = { vm.pendingUpdate = null }) { Text("Más tarde") } }
        )
    }

    if (confirmDelete) {
        val files = vm.selectedFiles
        DeleteDialog(
            count = files.size,
            name = files.firstOrNull()?.name,
            onTrash = {
                confirmDelete = false
                if (!remover(files.map { it.uri }, false)) {
                    scope.launch { snackbar.showSnackbar("Solo se pueden eliminar archivos de la galería") }
                }
            },
            onDelete = {
                confirmDelete = false
                if (!remover(files.map { it.uri }, true)) {
                    scope.launch { snackbar.showSnackbar("Solo se pueden eliminar archivos de la galería") }
                }
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
private fun FilterBar(vm: MixViewModel, onFolders: () -> Unit) {
    var folderMenu by remember { mutableStateOf(false) }
    var sortMenu by remember { mutableStateOf(false) }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = !vm.useAllFolders,
                onClick = onFolders,
                label = {
                    Text(if (vm.useAllFolders) "Carpetas" else "Carpetas (${vm.selectedFolders.size})")
                },
                leadingIcon = { Icon(MixIcons.Folder, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }
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
                leadingIcon = { Icon(MixIcons.Favorite, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }
        item {
            Box {
                FilterChip(
                    selected = vm.folderFilter != null,
                    onClick = { folderMenu = true },
                    label = { Text(vm.folderFilter ?: "Carpeta", maxLines = 1) },
                    leadingIcon = { Icon(MixIcons.Folder, contentDescription = null, modifier = Modifier.size(18.dp)) }
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
                    leadingIcon = { Icon(MixIcons.SwapVert, contentDescription = null, modifier = Modifier.size(18.dp)) }
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

/** Elige el video del feed que más se ve, para reproducir su vista previa (como Pinterest). */
private fun pickPreview(
    vm: MixViewModel,
    staggered: LazyStaggeredGridState,
    grid: LazyGridState
): String? {
    val videos = vm.visibleVideoKeys
    if (videos.isEmpty()) return null
    val viewportHeight: Int
    val spans = ArrayList<Triple<String, Int, Int>>()
    if (vm.mosaic) {
        val info = staggered.layoutInfo
        viewportHeight = info.viewportSize.height
        for (it in info.visibleItemsInfo) {
            val k = it.key as? String ?: continue
            if (k in videos) spans += Triple(k, it.offset.y, it.offset.y + it.size.height)
        }
    } else {
        val info = grid.layoutInfo
        viewportHeight = info.viewportSize.height
        for (it in info.visibleItemsInfo) {
            val k = it.key as? String ?: continue
            if (k in videos) spans += Triple(k, it.offset.y, it.offset.y + it.size.height)
        }
    }
    val center = viewportHeight / 2f
    var best: String? = null
    var bestScore = Float.MAX_VALUE
    for ((k, top, bottom) in spans) {
        val h = (bottom - top).coerceAtLeast(1)
        val shown = minOf(bottom, viewportHeight) - maxOf(top, 0)
        if (shown.toFloat() / h < 0.75f) continue
        val score = abs((top + bottom) / 2f - center)
        if (score < bestScore) {
            bestScore = score
            best = k
        }
    }
    return best
}

@OptIn(FlowPreview::class)
@Composable
private fun MediaGrid(vm: MixViewModel, onPlay: () -> Unit) {
    val selectedKeys = vm.selected
    val selecting = selectedKeys.isNotEmpty()
    val favorites = vm.favorites
    val inset = LocalNavInset.current
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val memories = vm.memories
    val showMemories = memories.isNotEmpty() && vm.query.isBlank() && vm.typeFilter == TypeFilter.ALL &&
        vm.folderFilter == null && !vm.favoritesOnly
    val staggeredState = rememberLazyStaggeredGridState()
    val gridState = rememberLazyGridState()

    // ---- Vista previa de videos en el feed: un solo reproductor, mudo y en bucle ----
    val preview = remember {
        newExoPlayer(context, handleAudioFocus = false, preview = true).apply {
            volume = 0f
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }
    var previewKey by remember { mutableStateOf<String?>(null) }
    var previewReady by remember { mutableStateOf(false) }
    DisposableEffect(preview) {
        val l = object : Player.Listener {
            override fun onRenderedFirstFrame() {
                previewReady = true
            }
        }
        preview.addListener(l)
        onDispose {
            preview.removeListener(l)
            preview.release()
        }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                previewKey = null
                previewReady = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }
    LaunchedEffect(selecting) {
        snapshotFlow { if (selecting) null else pickPreview(vm, staggeredState, gridState) }
            .distinctUntilChanged()
            .debounce(450)
            .collect {
                previewReady = false
                previewKey = it
            }
    }
    LaunchedEffect(previewKey) {
        val key = previewKey
        if (key == null) {
            preview.stop()
            preview.clearMediaItems()
        } else {
            val dur = vm.mediaFor(key)?.durationMs ?: 0L
            val item = MediaItem.Builder().setUri(key).apply {
                if (dur > 9_000L) {
                    setClippingConfiguration(MediaItem.ClippingConfiguration.Builder().setEndPositionMs(8_000L).build())
                }
            }.build()
            preview.setMediaItem(item)
            preview.prepare()
            preview.playWhenReady = true
        }
    }

    // El botón Inicio de la barra sube al principio cuando ya estás en el feed
    LaunchedEffect(vm.scrollTopTick) {
        if (vm.scrollTopTick > 0) {
            if (vm.mosaic) {
                if (staggeredState.firstVisibleItemIndex > 40) staggeredState.scrollToItem(30)
                staggeredState.animateScrollToItem(0)
            } else {
                if (gridState.firstVisibleItemIndex > 40) gridState.scrollToItem(30)
                gridState.animateScrollToItem(0)
            }
        }
    }

    val dragMod = Modifier.dragSelect(
        keyAt = { pos ->
            val key: Any? = if (vm.mosaic) {
                staggeredState.layoutInfo.visibleItemsInfo.firstOrNull {
                    pos.x >= it.offset.x && pos.x < it.offset.x + it.size.width &&
                        pos.y >= it.offset.y && pos.y < it.offset.y + it.size.height
                }?.key
            } else {
                gridState.layoutInfo.visibleItemsInfo.firstOrNull {
                    pos.x >= it.offset.x && pos.x < it.offset.x + it.size.width &&
                        pos.y >= it.offset.y && pos.y < it.offset.y + it.size.height
                }?.key
            }
            (key as? String)?.takeIf { it in vm.visibleKeys }
        },
        onStart = { key ->
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            vm.dragSelectStart(key)
        },
        onEnter = { key, add -> vm.dragSelectEnter(key, add) }
    )

    Box(Modifier.fillMaxSize()) {
        if (vm.mosaic) {
            LazyVerticalStaggeredGrid(
                state = staggeredState,
                columns = StaggeredGridCells.Adaptive(150.dp),
                contentPadding = PaddingValues(start = 6.dp, end = 6.dp, top = 6.dp, bottom = 6.dp + inset),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalItemSpacing = 6.dp,
                modifier = Modifier.fillMaxSize().then(dragMod)
            ) {
                if (showMemories) {
                    item(key = "memories", span = StaggeredGridItemSpan.FullLine) {
                        MemoryPills(vm, memories, onPlay)
                    }
                }
                items(vm.visible, key = { it.key }, contentType = { it.isVideo }) { file ->
                    MediaCell(
                        file = file,
                        aspect = aspectFor(file, true),
                        selected = file.key in selectedKeys,
                        selecting = selecting,
                        favorite = file.key in favorites,
                        previewPlayer = if (file.key == previewKey) preview else null,
                        previewReady = previewReady,
                        onClick = {
                            if (selecting) {
                                vm.toggleSelect(file)
                            } else {
                                vm.playFromLibrary(file)
                                onPlay()
                            }
                        }
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Adaptive(104.dp),
                contentPadding = PaddingValues(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 4.dp + inset),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxSize().then(dragMod)
            ) {
                if (showMemories) {
                    item(key = "memories", span = { GridItemSpan(maxLineSpan) }) {
                        MemoryPills(vm, memories, onPlay)
                    }
                }
                items(vm.visible, key = { it.key }, contentType = { it.isVideo }) { file ->
                    MediaCell(
                        file = file,
                        aspect = 1f,
                        selected = file.key in selectedKeys,
                        selecting = selecting,
                        favorite = file.key in favorites,
                        previewPlayer = if (file.key == previewKey) preview else null,
                        previewReady = previewReady,
                        onClick = {
                            if (selecting) {
                                vm.toggleSelect(file)
                            } else {
                                vm.playFromLibrary(file)
                                onPlay()
                            }
                        }
                    )
                }
            }
        }
    }
}

/** Búsquedas recientes y carpetas sugeridas, mientras la barra de búsqueda está vacía. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchSuggestions(vm: MixViewModel) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (vm.recentSearches.isNotEmpty()) {
            Text("Recientes", style = MaterialTheme.typography.titleSmall, color = MixColors.Pink)
            vm.recentSearches.forEach { q ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { vm.query = q }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(MixIcons.Search, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(q, modifier = Modifier.weight(1f).padding(start = 12.dp), style = MaterialTheme.typography.bodyLarge)
                    IconButton(onClick = { vm.removeRecentSearch(q) }) {
                        Icon(MixIcons.Close, contentDescription = "Quitar de recientes", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
        if (vm.folderSuggestions.isNotEmpty()) {
            Text("Carpetas", style = MaterialTheme.typography.titleSmall, color = MixColors.Pink, modifier = Modifier.padding(top = 8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                vm.folderSuggestions.forEach { f ->
                    AssistChip(onClick = { vm.query = f }, label = { Text(f) })
                }
            }
        }
        if (vm.recentSearches.isEmpty() && vm.folderSuggestions.isEmpty()) {
            Text("Escribí para buscar por nombre o carpeta.", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    previewPlayer: Player?,
    previewReady: Boolean,
    onClick: () -> Unit
) {
    val isSelected = selected
    val context = LocalContext.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(if (pressed) 0.96f else 1f, tween(120), label = "press")
    val request = remember(file.key) {
        ImageRequest.Builder(context)
            .data(MediaThumb(file.uri, file.isVideo))
            .size(400)
            .build()
    }
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(aspect)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .semantics(mergeDescendants = true) {
                contentDescription = (if (file.isVideo) "Video " else "Imagen ") + file.name
                this.selected = isSelected
                role = Role.Button
            }
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
    ) {
        AsyncImage(
            model = request,
            contentDescription = file.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        if (previewPlayer != null) {
            val alpha by animateFloatAsState(if (previewReady) 1f else 0f, tween(220), label = "previewAlpha")
            AndroidView(
                factory = { c -> LayoutInflater.from(c).inflate(R.layout.preview_view, null) as PlayerView },
                update = { it.player = previewPlayer },
                onRelease = { it.player = null },
                modifier = Modifier.fillMaxSize().graphicsLayer { this.alpha = alpha }
            )
        }
        if (file.isVideo) {
            Row(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
                    .background(Color(0x99000000), RoundedCornerShape(6.dp))
                    .padding(horizontal = 5.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(MixIcons.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                Text(formatDuration(file.durationMs), color = Color.White, style = MaterialTheme.typography.labelSmall)
            }
        }
        if (favorite) {
            Icon(
                MixIcons.FavoriteFilled,
                contentDescription = "Favorito",
                tint = Color(0xFFFF5C7A),
                modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp).size(18.dp)
            )
        }
        if (selected) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)))
            Icon(
                MixIcons.CheckCircle,
                contentDescription = "Seleccionado",
                tint = Color.White,
                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)
            )
        } else if (selecting) {
            Icon(
                MixIcons.RadioButtonUnchecked,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)
            )
        }
    }
}
