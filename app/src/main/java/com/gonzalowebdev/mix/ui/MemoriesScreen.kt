package com.gonzalowebdev.mix.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFramePercent
import com.gonzalowebdev.mix.data.Memory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoriesScreen(vm: MixViewModel, onPlay: () -> Unit) {
    val inset = LocalNavInset.current
    val memories = vm.memories

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Recuerdos", style = MaterialTheme.typography.headlineSmall) })
        }
    ) { padding ->
        if (memories.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Todavía no hay recuerdos.\nAparecen a medida que tengas fotos y videos con fecha, o favoritos.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(32.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = inset + 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(memories, key = { it.id }) { m ->
                    MemoryCard(
                        memory = m,
                        modifier = Modifier.fillMaxWidth().aspectRatio(1.6f),
                        onClick = {
                            vm.playMemory(m)
                            onPlay()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MemoryCard(
    memory: Memory,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val request = remember(memory.cover.key, compact) {
        ImageRequest.Builder(context)
            .data(memory.cover.uri)
            .size(if (compact) 400 else 900)
            .apply { if (memory.cover.isVideo) videoFramePercent(0.1) }
            .build()
    }
    Box(
        modifier
            .clip(RoundedCornerShape(if (compact) 18.dp else 24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = request,
            contentDescription = memory.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(0.4f to Color.Transparent, 1f to Color(0xE60B0B0D)))
        )
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(if (compact) 12.dp else 18.dp)
        ) {
            Text(
                text = memory.title,
                style = if (compact) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleLarge,
                color = MixColors.Bone,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (compact) memory.subtitle else "${memory.subtitle} · ${memory.files.size}",
                style = MaterialTheme.typography.labelSmall,
                color = MixColors.PinkSoft,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
