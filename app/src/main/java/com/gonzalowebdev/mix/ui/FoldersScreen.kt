package com.gonzalowebdev.mix.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFramePercent
import com.gonzalowebdev.mix.data.FolderInfo
import com.gonzalowebdev.mix.data.MediaThumb

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderScopeScreen(vm: MixViewModel, onBack: () -> Unit) {
    val folders = vm.allFolders
    val useAll = vm.useAllFolders
    val selected = vm.selectedFolders
    var menu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Carpetas del feed", style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(MixIcons.ArrowBack, contentDescription = "Volver") }
                },
                actions = {
                    if (!useAll) {
                        Box {
                            IconButton(onClick = { menu = true }) {
                                Icon(MixIcons.MoreVert, contentDescription = "Más opciones")
                            }
                            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                DropdownMenuItem(
                                    text = { Text("Seleccionar todas") },
                                    onClick = {
                                        menu = false
                                        vm.selectAllFolders()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Quitar todas") },
                                    onClick = {
                                        menu = false
                                        vm.selectNoFolders()
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            ListItem(
                headlineContent = { Text("Mostrar todas las carpetas") },
                supportingContent = {
                    Text(
                        if (useAll) "Se muestran las fotos y videos de todo el dispositivo"
                        else "Solo se muestran las carpetas marcadas (${selected.size})"
                    )
                },
                trailingContent = {
                    Switch(checked = useAll, onCheckedChange = { vm.updateUseAllFolders(it) })
                },
                modifier = Modifier.clickable { vm.updateUseAllFolders(!useAll) }
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = LocalNavInset.current + 8.dp)
            ) {
                items(folders, key = { it.id }) { f ->
                    val checked = useAll || f.id in selected
                    ListItem(
                        leadingContent = { FolderThumb(f) },
                        headlineContent = { Text(f.name, maxLines = 1) },
                        supportingContent = { Text("${f.count} archivos") },
                        trailingContent = {
                            Checkbox(
                                checked = checked,
                                enabled = !useAll,
                                onCheckedChange = { vm.toggleFolder(f.id) }
                            )
                        },
                        modifier = Modifier.clickable(enabled = !useAll) { vm.toggleFolder(f.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FolderThumb(f: FolderInfo) {
    val context = LocalContext.current
    val request = remember(f.sample.key) {
        ImageRequest.Builder(context)
            .data(MediaThumb(f.sample.uri, f.sample.isVideo))
            .size(160)
            .build()
    }
    AsyncImage(
        model = request,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    )
}
