package com.gonzalowebdev.mix.ui

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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gonzalowebdev.mix.data.PlaylistEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistsScreen(
    vm: MixViewModel,
    onBack: () -> Unit,
    onOpen: (Long) -> Unit
) {
    val playlists by vm.playlists.collectAsStateWithLifecycle()
    var showCreate by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<PlaylistEntity?>(null) }
    var deleting by remember { mutableStateOf<PlaylistEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Listas de reproducción") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
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
        if (playlists.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Todavía no hay listas.\nCreá una con el botón +\no desde la biblioteca, seleccionando archivos.",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp)
                )
            }
        } else {
            LazyColumn(Modifier.padding(padding).fillMaxSize()) {
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
}
