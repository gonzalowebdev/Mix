package com.gonzalowebdev.mix.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gonzalowebdev.mix.data.PlaylistEntity
import com.gonzalowebdev.mix.data.SmartList
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistsScreen(
    vm: MixViewModel,
    onBack: () -> Unit,
    onOpen: (Long) -> Unit,
    onPlay: () -> Unit
) {
    val playlists by vm.playlists.collectAsStateWithLifecycle()
    val smartLists = vm.smartLists
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

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
                title = { Text("Listas de reproducción") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { topMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Más opciones")
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
            FloatingActionButton(onClick = { showCreate = true }) {
                Icon(Icons.Default.Add, contentDescription = "Nueva lista")
            }
        }
    ) { padding ->
        if (playlists.isEmpty() && smartLists.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Todavía no hay listas.\nCreá una con el botón +\no desde la biblioteca, seleccionando archivos.",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp)
                )
            }
        } else {
            LazyColumn(Modifier.padding(padding).fillMaxSize()) {
                if (smartLists.isNotEmpty()) {
                    item {
                        Text(
                            "Listas inteligentes",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp)
                        )
                    }
                    items(smartLists, key = { "smart-${it.id}" }) { s ->
                        var menu by remember { mutableStateOf(false) }
                        ListItem(
                            headlineContent = { Text(s.name) },
                            supportingContent = { Text("${vm.smartMedia(s).size} archivos · se actualiza sola") },
                            leadingContent = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                            trailingContent = {
                                Box {
                                    IconButton(onClick = { menu = true }) {
                                        Icon(Icons.Default.MoreVert, contentDescription = "Opciones")
                                    }
                                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
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
                            },
                            modifier = Modifier.clickable {
                                if (vm.smartMedia(s).isNotEmpty()) {
                                    vm.playSmart(s)
                                    onPlay()
                                } else {
                                    scope.launch { snackbar.showSnackbar("Esta lista no tiene archivos por ahora") }
                                }
                            }
                        )
                    }
                }
                if (playlists.isNotEmpty()) {
                    item {
                        Text(
                            "Mis listas",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp)
                        )
                    }
                    items(playlists, key = { it.playlist.id }) { p ->
                        var menu by remember { mutableStateOf(false) }
                        ListItem(
                            headlineContent = { Text(p.playlist.name) },
                            supportingContent = { Text("${p.itemCount} elementos") },
                            leadingContent = { Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null) },
                            trailingContent = {
                                Box {
                                    IconButton(onClick = { menu = true }) {
                                        Icon(Icons.Default.MoreVert, contentDescription = "Opciones")
                                    }
                                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
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
                            },
                            modifier = Modifier.clickable { onOpen(p.playlist.id) }
                        )
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
