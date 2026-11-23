package com.gonzalowebdev.mix.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
fun TrashScreen(vm: MixViewModel, onBack: () -> Unit) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val selected = remember { mutableStateListOf<String>() }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.loadTrash() }

    val action = rememberFileAction { _, ok ->
        if (ok) {
            selected.clear()
            vm.onTrashChanged()
        }
    }

    val items = vm.trashed
    val chosen = items.filter { it.key in selected }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Papelera", style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(MixIcons.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        bottomBar = {
            if (items.isNotEmpty()) {
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
                        if (selected.size == items.size) {
                            selected.clear()
                        } else {
                            selected.clear()
                            selected.addAll(items.map { it.key })
                        }
                    }) { Text(if (selected.size == items.size) "Quitar todo" else "Todo") }
                    OutlinedButton(
                        enabled = chosen.isNotEmpty(),
                        onClick = {
                            if (!action(chosen.map { it.uri }, FileAction.RESTORE)) {
                                scope.launch { snackbar.showSnackbar("No se pudo restaurar") }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Restaurar (${chosen.size})") }
                    Button(
                        enabled = chosen.isNotEmpty(),
                        onClick = { confirmDelete = true },
                        modifier = Modifier.weight(1f)
                    ) { Text("Eliminar") }
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                vm.trashLoading && items.isEmpty() -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                items.isEmpty() -> Text(
                    "La papelera está vacía.\nLo que elimines se guarda unos 30 días antes de borrarse del todo.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center).padding(32.dp)
                )
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(104.dp),
                    contentPadding = PaddingValues(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(items, key = { it.key }) { f ->
                        TrashCell(
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

    if (confirmDelete) {
        ConfirmDialog(
            title = "Eliminar definitivamente",
            message = "Se van a eliminar ${chosen.size} archivos para siempre. No se pueden recuperar.",
            confirmLabel = "Eliminar",
            onConfirm = {
                confirmDelete = false
                if (!action(chosen.map { it.uri }, FileAction.DELETE)) {
                    scope.launch { snackbar.showSnackbar("No se pudo eliminar") }
                }
            },
            onDismiss = { confirmDelete = false }
        )
    }
}

@Composable
private fun TrashCell(file: MediaFile, checked: Boolean, onToggle: () -> Unit) {
    val context = LocalContext.current
    val request = remember(file.key) {
        ImageRequest.Builder(context)
            .data(file.uri)
            .size(300)
            .apply { if (file.isVideo) videoFramePercent(0.1) }
            .build()
    }
    val daysLeft = if (file.expires > 0) {
        ((file.expires - System.currentTimeMillis() / 1000) / 86_400).toInt().coerceAtLeast(0)
    } else {
        -1
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
        if (daysLeft >= 0) {
            Text(
                text = if (daysLeft == 0) "Hoy" else "$daysLeft d",
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
}
