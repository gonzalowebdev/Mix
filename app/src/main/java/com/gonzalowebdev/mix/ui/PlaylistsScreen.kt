package com.gonzalowebdev.mix.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.gonzalowebdev.mix.data.PlaylistEntity
import com.gonzalowebdev.mix.data.SmartList
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistsScreen(
    vm: MixViewModel,
    onOpen: (Long) -> Unit,
    onPlay: () -> Unit
) {
    val playlists by vm.playlists.collectAsStateWithLifecycle()
    val previews by vm.playlistPreviews.collectAsStateWithLifecycle()
    val smartLists = vm.smartLists
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val inset = LocalNavInset.current

    var showCreate by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<PlaylistEntity?>(null) }
    var deleting by remember { mutableStateOf<PlaylistEntity?>(null) }
    var topMenu by remember { mutableStateOf(false) }
    var smartEditor by remember { mutableStateOf(false) }
    var smartEditing by remember { mutableStateOf<SmartList?>(null) }
    var smartDeleting by remember { mutableStateOf<SmartList?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            vm.exportTo(uri) { ok ->
                scope.launch { snackbar.showSnackbar(if (ok) "Listas exportadas" else "No se pudo exportar") }
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            vm.importFrom(uri) { n ->
                scope.launch {
                    snackbar.showSnackbar(if (n >= 0) "$n listas importadas" else "El archivo no es válido")
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Listas", style = MaterialTheme.typography.headlineSmall) },
                actions = {
                    Box {
                        IconButton(onClick = { topMenu = true }) {
                            Icon(MixIcons.MoreVert, contentDescription = "Más opciones")
                        }
                        DropdownMenu(expanded = topMenu, onDismissRequest = { topMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Nueva lista inteligente") },
                                onClick = {
                                    topMenu = false
                                    smartEditing = null
                                    smartEditor = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Exportar listas") },
                                onClick = {
                                    topMenu = false
                                    exportLauncher.launch("mix-listas.json")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Importar listas") },
                                onClick = {
                                    topMenu = false
                                    importLauncher.launch(arrayOf("*/*"))
                                }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreate = true },
                modifier = Modifier.padding(bottom = inset)
            ) {
                Icon(MixIcons.Add, contentDescription = "Nueva lista")
            }
        }
    ) { padding ->
        if (playlists.isEmpty() && smartLists.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Todavía no hay listas.\nCreá una con el botón +\no desde la biblioteca, seleccionando archivos.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(24.dp)
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = inset + 88.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (smartLists.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) { BoardHeader("Listas inteligentes") }
                    items(smartLists, key = { "smart-${it.id}" }) { s ->
                        val files = vm.smartMedia(s)
                        var menu by remember { mutableStateOf(false) }
                        BoardCard(
                            title = s.name,
                            subtitle = "${files.size} archivos · se actualiza sola",
                            previews = files.take(4).map { it.key to it.isVideo },
                            smart = true,
                            onClick = {
                                if (files.isNotEmpty()) {
                                    vm.playSmart(s)
                                    onPlay()
                                } else {
                                    scope.launch { snackbar.showSnackbar("Esta lista no tiene archivos por ahora") }
                                }
                            },
                            menuOpen = menu,
                            onMenu = { menu = it }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Editar") },
                                onClick = {
                                    menu = false
                                    smartEditing = s
                                    smartEditor = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Eliminar") },
                                onClick = {
                                    menu = false
                                    smartDeleting = s
                                }
                            )
                        }
                    }
                }
                if (playlists.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) { BoardHeader("Mis listas") }
                    items(playlists, key = { it.playlist.id }) { p ->
                        var menu by remember { mutableStateOf(false) }
                        BoardCard(
                            title = p.playlist.name,
                            subtitle = "${p.itemCount} elementos",
                            previews = previews[p.playlist.id].orEmpty().map { it.uri to it.isVideo },
                            smart = false,
                            onClick = { onOpen(p.playlist.id) },
                            menuOpen = menu,
                            onMenu = { menu = it }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Renombrar") },
                                onClick = {
                                    menu = false
                                    renaming = p.playlist
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Eliminar") },
                                onClick = {
                                    menu = false
                                    deleting = p.playlist
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreate) {
        TextInputDialog(
            title = "Nueva lista",
            confirmLabel = "Crear",
            onConfirm = { name ->
                vm.createPlaylist(name, emptyList())
                showCreate = false
            },
            onDismiss = { showCreate = false }
        )
    }
    renaming?.let { p ->
        TextInputDialog(
            title = "Renombrar lista",
            confirmLabel = "Guardar",
            initial = p.name,
            onConfirm = { name ->
                vm.renamePlaylist(p.id, name)
                renaming = null
            },
            onDismiss = { renaming = null }
        )
    }
    deleting?.let { p ->
        ConfirmDialog(
            title = "Eliminar lista",
            message = "¿Eliminar \"${p.name}\"? Los archivos no se borran del dispositivo.",
            confirmLabel = "Eliminar",
            onConfirm = {
                vm.deletePlaylist(p.id)
                deleting = null
            },
            onDismiss = { deleting = null }
        )
    }
    if (smartEditor) {
        SmartListDialog(
            initial = smartEditing,
            folders = vm.folders,
            onSave = { s ->
                vm.saveSmartList(s)
                smartEditor = false
            },
            onDismiss = { smartEditor = false }
        )
    }
    smartDeleting?.let { s ->
        ConfirmDialog(
            title = "Eliminar lista inteligente",
            message = "¿Eliminar \"${s.name}\"?",
            confirmLabel = "Eliminar",
            onConfirm = {
                vm.deleteSmartList(s.id)
                smartDeleting = null
            },
            onDismiss = { smartDeleting = null }
        )
    }
}

@Composable
private fun BoardHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MixColors.Pink,
        modifier = Modifier.padding(top = 8.dp)
    )
}

/** Tarjeta de lista estilo tablero: portada en collage con hasta 4 miniaturas. */
@Composable
private fun BoardCard(
    title: String,
    subtitle: String,
    previews: List<Pair<String, Boolean>>,
    smart: Boolean,
    onClick: () -> Unit,
    menuOpen: Boolean,
    onMenu: (Boolean) -> Unit,
    menuContent: @Composable () -> Unit
) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Collage(previews)
            if (smart) {
                Icon(
                    MixIcons.AutoAwesome,
                    contentDescription = "Lista inteligente",
                    tint = MixColors.Pink,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .background(Color(0x99000000), CircleShape)
                        .padding(6.dp)
                        .size(16.dp)
                )
            }
            Box(Modifier.align(Alignment.TopEnd)) {
                IconButton(
                    onClick = { onMenu(true) },
                    modifier = Modifier
                        .padding(4.dp)
                        .size(36.dp)
                        .background(Color(0x66000000), CircleShape)
                ) {
                    Icon(MixIcons.MoreVert, contentDescription = "Opciones de $title", tint = Color.White, modifier = Modifier.size(20.dp))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { onMenu(false) }) { menuContent() }
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun Collage(previews: List<Pair<String, Boolean>>) {
    if (previews.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                MixIcons.QueueMusic,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(34.dp)
            )
        }
        return
    }
    if (previews.size == 1) {
        CollageCell(previews[0], Modifier.fillMaxSize())
        return
    }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            CollageCell(previews.getOrNull(0), Modifier.weight(1f).fillMaxHeight())
            CollageCell(previews.getOrNull(1), Modifier.weight(1f).fillMaxHeight())
        }
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            CollageCell(previews.getOrNull(2), Modifier.weight(1f).fillMaxHeight())
            CollageCell(previews.getOrNull(3), Modifier.weight(1f).fillMaxHeight())
        }
    }
}

@Composable
private fun CollageCell(item: Pair<String, Boolean>?, modifier: Modifier) {
    if (item == null) {
        Box(modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh))
        return
    }
    val context = LocalContext.current
    val request = remember(item.first) {
        ImageRequest.Builder(context)
            .data(android.net.Uri.parse(item.first))
            .size(240)
            .apply { if (item.second) videoFramePercent(0.1) }
            .build()
    }
    AsyncImage(
        model = request,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
    )
}
