package com.gonzalowebdev.mix.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gonzalowebdev.mix.data.Memory
import com.gonzalowebdev.mix.data.MemoryKind
import java.time.LocalTime

/** Saludo según la hora del día. */
fun greeting(name: String): String {
    val hour = LocalTime.now().hour
    val salute = when (hour) {
        in 5..11 -> "Buen día"
        in 12..19 -> "Buenas tardes"
        else -> "Buenas noches"
    }
    return if (name.isBlank()) salute else "$salute, $name"
}

private fun iconFor(m: Memory): ImageVector = when (m.kind) {
    MemoryKind.DAILY -> MixIcons.Logo
    MemoryKind.THEME -> when (m.id) {
        "mix1" -> MixIcons.Sun
        "mix2" -> MixIcons.Layers
        else -> MixIcons.Bolt
    }
    MemoryKind.LUCKY -> MixIcons.Dice
    MemoryKind.OLD_IMAGES -> MixIcons.Image
    MemoryKind.OLD_VIDEOS -> MixIcons.Videocam
    MemoryKind.RECENT_PLAYED -> MixIcons.History
    MemoryKind.RECENT_ADDED -> MixIcons.AddPhoto
}

/** Pastilla de un recuerdo. La primera (el mix del día) es redonda y solo lleva el logo. */
@Composable
fun MemoryPill(memory: Memory, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f, tween(120), label = "pill")
    val pink = MixColors.Pink
    val round = memory.kind == MemoryKind.DAILY
    val shape = if (round) CircleShape else RoundedCornerShape(50)
    Row(
        Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(if (round) pink.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant)
            .border(if (round) 1.5.dp else 0.5.dp, if (round) pink else MaterialTheme.colorScheme.outlineVariant, shape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .then(if (round) Modifier.size(52.dp) else Modifier.height(46.dp).padding(horizontal = 16.dp)),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = iconFor(memory),
            contentDescription = if (round) memory.title else null,
            tint = pink,
            modifier = Modifier.size(if (round) 30.dp else 20.dp)
        )
        if (!round) {
            Text(
                text = memory.title,
                style = MaterialTheme.typography.labelLarge,
                color = MixColors.Bone,
                maxLines = 1,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

/** Saludo + fila de pastillas (arriba del feed de Inicio). */
@Composable
fun MemoryPills(vm: MixViewModel, memories: List<Memory>, onPlay: () -> Unit) {
    LaunchedEffect(Unit) { vm.rebuildMemories() }
    Column(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
        Text(
            greeting(vm.prefs.userName),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 6.dp, top = 4.dp, bottom = 10.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(memories, key = { it.id }) { m ->
                MemoryPill(m) {
                    vm.playMemory(m)
                    onPlay()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MemoriesScreen(vm: MixViewModel, onPlay: () -> Unit) {
    val inset = LocalNavInset.current
    val memories = vm.memories

    LaunchedEffect(Unit) { vm.rebuildMemories() }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(greeting(vm.prefs.userName), style = MaterialTheme.typography.headlineSmall) })
        }
    ) { padding ->
        if (memories.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Todavía no hay recuerdos.\nAparecen cuando Mix encuentre fotos y videos en tu biblioteca.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(32.dp)
                )
            }
        } else {
            Column(
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = inset + 16.dp)
            ) {
                Text(
                    "Tocá una pastilla y se reproduce.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    memories.forEach { m ->
                        MemoryPill(m) {
                            vm.playMemory(m)
                            onPlay()
                        }
                    }
                }
            }
        }
    }
}
