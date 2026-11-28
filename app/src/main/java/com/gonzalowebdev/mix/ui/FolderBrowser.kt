package com.gonzalowebdev.mix.ui

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.gonzalowebdev.mix.data.MediaFile
import com.gonzalowebdev.mix.data.MediaThumb

@Composable
private fun Thumb(file: MediaFile, size: Int, modifier: Modifier) {
    val context = LocalContext.current
    val request = remember(file.key, size) {
        ImageRequest.Builder(context).data(MediaThumb(file.uri, file.isVideo)).size(size).build()
    }
    AsyncImage(model = request, contentDescription = file.name, contentScale = ContentScale.Crop, modifier = modifier)
}

/** Todas las carpetas del dispositivo, como en una galería. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderBrowserScreen(vm: MixViewModel, onOpen: (String) -> Unit) {
    val folders = vm.allFolders
    Scaffold(
        topBar = { TopAppBar(title = { Text("Carpetas", style = MaterialTheme.typography.headlineSmall) }) }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 6.dp, bottom = LocalNavInset.current + 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(folders, key = { it.id }) { f ->
                Column(Modifier.clickable { onOpen(f.id) }) {
                    Thumb(
                        f.sample, 400,
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                    Text(
                        f.name,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 8.dp, start = 2.dp)
                    )
                    Text(
                        "${f.count} archivos",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 2.dp)
                    )
                }
            }
        }
    }
}

/** Los archivos de una carpeta; al tocar uno se reproduce la carpeta desde ahí. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderDetailScreen(vm: MixViewModel, folderId: String, onBack: () -> Unit, onPlay: () -> Unit) {
    val files = remember(vm.allMedia, folderId) {
        vm.allMedia.filter { it.folderId == folderId }.sortedByDescending { it.dateTaken }
    }
    val name = files.firstOrNull()?.folder ?: "Carpeta"
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(name, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "${files.size} archivos",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(MixIcons.ArrowBack, contentDescription = "Volver") }
                }
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(104.dp),
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(files, key = { it.key }, contentType = { it.isVideo }) { file ->
                Box(
                    Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable {
                            vm.playQueue(files.map { it.toQueueItem() }, files.indexOfFirst { it.key == file.key })
                            onPlay()
                        }
                ) {
                    Thumb(file, 300, Modifier.fillMaxSize())
                    if (file.isVideo) {
                        Row(
                            Modifier
                                .align(Alignment.BottomStart)
                                .padding(5.dp)
                                .background(Color(0x99000000), RoundedCornerShape(6.dp))
                                .padding(horizontal = 5.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(MixIcons.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Text(formatDuration(file.durationMs), color = Color.White, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}
