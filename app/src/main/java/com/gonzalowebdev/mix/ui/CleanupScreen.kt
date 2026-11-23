package com.gonzalowebdev.mix.ui

import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFramePercent
import com.gonzalowebdev.mix.data.MediaFile
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleanupScreen(vm: MixViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var tab by remember { mutableIntStateOf(0) }
    val selected = remember { mutableStateListOf<String>() }
    var confirm by remember { mutableStateOf(false) }

    val duplicateExtras = remember(vm.duplicateGroups) { vm.duplicateGroups.flatMap { it.drop(1) } }
    val candidates: List<MediaFile> = when (tab) {
        0 -> duplicateExtras
        1 -> vm.screenshots
        2 -> vm.blurry
        else -> vm.heavyVideos
    }

    // Los duplicados vienen preseleccionados (se conserva la mejor copia de cada grupo)
    LaunchedEffect(tab, candidates.size) {
        selected.clear()
        if (tab == 0) selected.addAll(candidates.map { it.key })
    }

    val remover = rememberFileRemover { uris, ok ->
        if (ok) {
            vm.onFilesRemoved(uris)
            selected.clear()
        }
    }

    val chosen = candidates.filter { it.key in selected }
    val chosenBytes = chosen.sumOf { it.size }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Limpieza", style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(MixIcons.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        bottomBar = {
            if (candidates.isNotEmpty()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(onClick = {
                        if (selected.size == candidates.size) {
                            selected.clear()
                        } else {
                            selected.clear()
                            selected.addAll(candidates.map { it.key })
                        }
                    }) { Text(if (selected.size == candidates.size) "Quitar todo" else "Seleccionar todo") }
                    Button(
                        enabled = chosen.isNotEmpty(),
                        onClick = { confirm = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Papelera · ${chosen.size} (${Formatter.formatFileSize(context, chosenBytes)})")
                    }
                }
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val labels = listOf(
                    "Duplicados (${duplicateExtras.size})",
                    "Capturas (${vm.screenshots.size})",
                    "Borrosas (${vm.blurry.size})",
                    "Videos pesados (${vm.heavyVideos.size})"
                )
                items(labels.indices.toList()) { i ->
                    FilterChip(selected = tab == i, onClick = { tab = i }, label = { Text(labels[i]) })
                }
            }
            if (vm.analyzing) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(
                        "Analizando fotos: ${vm.analysisDone} de ${vm.analysisTotal}. Los duplicados y las borrosas se completan al terminar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LinearProgressIndicator(
                        progress = {
                            if (vm.analysisTotal > 0) vm.analysisDone / vm.analysisTotal.toFloat() else 0f
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                    )
                }
            }
            Text(
                text = when (tab) {
                    0 -> "Se conserva la mejor copia de cada grupo. Revisá antes de mover."
                    1 -> "Capturas de pantalla ordenadas por tamaño."
                    2 -> "Fotos con poca nitidez. Revisá antes de mover."
                    else -> "Videos de más de 100 MB, del más pesado al más liviano."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            if (candidates.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (vm.analyzing && tab in 0..2) "Todavía analizando…" else "No hay nada para limpiar acá",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(104.dp),
                    contentPadding = PaddingValues(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(candidates, key = { it.key }) { f ->
                        CleanupCell(
                            file = f,
                            checked = f.key in selected,
                            onToggle = {
                                if (!selected.remove(f.key)) selected.add(f.key)
                            }
                        )
                    }
                }
            }
        }
    }

    if (confirm) {
        DeleteDialog(
            count = chosen.size,
            name = chosen.firstOrNull()?.name,
            onTrash = {
                confirm = false
                if (!remover(chosen.map { it.uri }, false)) {
                    scope.launch { snackbar.showSnackbar("Estos archivos no se pueden eliminar desde Mix") }
                }
            },
            onDelete = {
                confirm = false
                if (!remover(chosen.map { it.uri }, true)) {
                    scope.launch { snackbar.showSnackbar("Estos archivos no se pueden eliminar desde Mix") }
                }
            },
            onDismiss = { confirm = false }
        )
    }
}

@Composable
private fun CleanupCell(file: MediaFile, checked: Boolean, onToggle: () -> Unit) {
    val context = LocalContext.current
    val request = remember(file.key) {
        ImageRequest.Builder(context)
            .data(file.uri)
            .size(300)
            .apply { if (file.isVideo) videoFramePercent(0.1) }
            .build()
    }
    Box(
        Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onToggle)
    ) {
        AsyncImage(
            model = request,
            contentDescription = file.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        if (checked) Box(Modifier.fillMaxSize().background(MixColors.Pink.copy(alpha = 0.30f)))
        Icon(
            imageVector = if (checked) MixIcons.CheckCircle else MixIcons.RadioButtonUnchecked,
            contentDescription = if (checked) "Seleccionado" else "No seleccionado",
            tint = if (checked) MixColors.Pink else Color.White,
            modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(22.dp)
        )
        Text(
            text = Formatter.formatShortFileSize(context, file.size),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(5.dp)
                .background(Color(0x99000000), RoundedCornerShape(5.dp))
                .padding(horizontal = 5.dp, vertical = 1.dp)
        )
    }
}
