package com.gonzalowebdev.mix.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.delay

private fun Context.findActivity(): Activity? {
    var c: Context = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

private fun buildOrder(n: Int, current: Int, shuffle: Boolean): Pair<List<Int>, Int> =
    if (!shuffle) {
        (0 until n).toList() to current
    } else {
        (listOf(current) + (0 until n).filter { it != current }.shuffled()) to 0
    }

@Composable
fun PlayerScreen(vm: MixViewModel, onClose: () -> Unit) {
    val queue = vm.queue
    if (queue.isEmpty()) {
        LaunchedEffect(Unit) { onClose() }
        return
    }

    val context = LocalContext.current
    val view = LocalView.current

    // ---- Estado de reproducción ----
    val initial = remember { buildOrder(queue.size, vm.startIndex.coerceIn(0, queue.size - 1), vm.prefs.shuffle) }
    var order by remember { mutableStateOf(initial.first) }
    var pos by remember { mutableIntStateOf(initial.second) }
    var tick by remember { mutableIntStateOf(0) }
    var playing by remember { mutableStateOf(true) }
    var finished by remember { mutableStateOf(false) }
    var shuffle by remember { mutableStateOf(vm.prefs.shuffle) }
    var repeat by remember { mutableIntStateOf(vm.prefs.repeatMode) }
    var seconds by remember { mutableIntStateOf(vm.prefs.imageSeconds) }
    var errors by remember { mutableIntStateOf(0) }
    var controlsVisible by remember { mutableStateOf(true) }
    var touchStamp by remember { mutableLongStateOf(0L) }
    var videoPos by remember { mutableLongStateOf(0L) }
    var videoDur by remember { mutableLongStateOf(0L) }
    var timerMenu by remember { mutableStateOf(false) }

    val item = queue[order[pos]]
    val imgProgress = remember { Animatable(0f) }

    fun currentIsVideo(): Boolean = queue[order[pos]].isVideo

    fun go(delta: Int, auto: Boolean) {
        val n = order.size
        if (n == 0) return
        if (auto && repeat == 2) {
            tick++
            return
        }
        val np = pos + delta
        if (np in 0 until n) {
            pos = np
            tick++
            finished = false
        } else if (!auto || repeat == 1) {
            pos = ((np % n) + n) % n
            tick++
            finished = false
        } else {
            playing = false
            finished = true
        }
    }

    fun onMediaError() {
        errors++
        if (errors > order.size) {
            playing = false
            finished = true
        } else {
            go(1, true)
        }
    }

    fun togglePlay() {
        if (finished) {
            pos = 0
            tick++
            finished = false
            playing = true
        } else {
            playing = !playing
        }
    }

    fun setShuffleMode(value: Boolean) {
        shuffle = value
        vm.prefs.shuffle = value
        val (o, p) = buildOrder(queue.size, order[pos], value)
        order = o
        pos = p
    }

    // ---- ExoPlayer ----
    val player = remember {
        ExoPlayer.Builder(context)
            .setAudioAttributes(AudioAttributes.DEFAULT, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
    }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) go(1, true)
                if (state == Player.STATE_READY) errors = 0
            }

            override fun onPlayerError(error: PlaybackException) {
                onMediaError()
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                if (reason != Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST && currentIsVideo()) {
                    playing = playWhenReady
                }
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    // Cargar el elemento actual
    LaunchedEffect(tick) {
        val current = queue[order[pos]]
        if (current.isVideo) {
            player.setMediaItem(MediaItem.fromUri(Uri.parse(current.uri)))
            player.prepare()
            player.playWhenReady = playing
        } else {
            player.stop()
            player.clearMediaItems()
        }
    }
    LaunchedEffect(playing) {
        if (currentIsVideo()) player.playWhenReady = playing
    }

    // Temporizador de imágenes
    LaunchedEffect(tick, playing, seconds) {
        if (!queue[order[pos]].isVideo) {
            imgProgress.snapTo(0f)
            if (playing) {
                imgProgress.animateTo(1f, tween(seconds * 1000, easing = LinearEasing))
                go(1, true)
            }
        }
    }

    // Progreso del video
    LaunchedEffect(tick) {
        while (queue[order[pos]].isVideo) {
            videoPos = player.currentPosition
            videoDur = player.duration.coerceAtLeast(0L)
            delay(300)
        }
    }

    // Ocultar controles automáticamente
    LaunchedEffect(controlsVisible, playing, touchStamp) {
        if (controlsVisible && playing) {
            delay(3500)
            controlsVisible = false
        }
    }

    // Pantalla completa, pantalla encendida y pausa al salir de la app
    DisposableEffect(Unit) {
        val window = context.findActivity()?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        view.keepScreenOn = true
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
            view.keepScreenOn = false
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { playing = false }

    BackHandler { onClose() }

    // ---- UI ----
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (item.isVideo) {
            AndroidView(
                factory = { c ->
                    PlayerView(c).apply {
                        useController = false
                        this.player = player
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            val request = remember(item.uri, tick) {
                ImageRequest.Builder(context)
                    .data(Uri.parse(item.uri))
                    .crossfade(true)
                    .build()
            }
            AsyncImage(
                model = request,
                contentDescription = item.name,
                contentScale = ContentScale.Fit,
                onSuccess = { errors = 0 },
                onError = { onMediaError() },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Capa de gestos: toque para mostrar/ocultar controles, deslizar para cambiar
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        controlsVisible = !controlsVisible
                        touchStamp++
                    })
                }
                .pointerInput(Unit) {
                    var total = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { total = 0f },
                        onDragEnd = {
                            if (total < -150f) go(1, false) else if (total > 150f) go(-1, false)
                        },
                        onHorizontalDrag = { _, amount -> total += amount }
                    )
                }
        )

        AnimatedVisibility(visible = controlsVisible, modifier = Modifier.align(Alignment.TopCenter)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color(0xB3000000), Color.Transparent)))
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cerrar", tint = Color.White)
                }
                Text(
                    text = item.name,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${pos + 1}/${order.size}",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(end = 12.dp)
                )
            }
        }

        AnimatedVisibility(visible = controlsVisible, modifier = Modifier.align(Alignment.BottomCenter)) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xB3000000))))
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                if (item.isVideo) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(formatDuration(videoPos), color = Color.White, style = MaterialTheme.typography.labelMedium)
                        Slider(
                            value = videoPos.toFloat().coerceIn(0f, maxOf(1f, videoDur.toFloat())),
                            onValueChange = {
                                videoPos = it.toLong()
                                player.seekTo(it.toLong())
                                touchStamp++
                            },
                            valueRange = 0f..maxOf(1f, videoDur.toFloat()),
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                        )
                        Text(formatDuration(videoDur), color = Color.White, style = MaterialTheme.typography.labelMedium)
                    }
                } else {
                    LinearProgressIndicator(
                        progress = { imgProgress.value },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp)
                    )
                }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { setShuffleMode(!shuffle); touchStamp++ }) {
                        Icon(
                            Icons.Default.Shuffle,
                            contentDescription = "Aleatorio",
                            tint = if (shuffle) MaterialTheme.colorScheme.primary else Color.White
                        )
                    }
                    IconButton(onClick = { go(-1, false); touchStamp++ }) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Anterior", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                    IconButton(onClick = { togglePlay(); touchStamp++ }, modifier = Modifier.size(64.dp)) {
                        Icon(
                            imageVector = when {
                                finished -> Icons.Default.Replay
                                playing -> Icons.Default.Pause
                                else -> Icons.Default.PlayArrow
                            },
                            contentDescription = "Reproducir / pausar",
                            tint = Color.White,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                    IconButton(onClick = { go(1, false); touchStamp++ }) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Siguiente", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                    IconButton(onClick = {
                        repeat = (repeat + 1) % 3
                        vm.prefs.repeatMode = repeat
                        touchStamp++
                    }) {
                        Icon(
                            imageVector = if (repeat == 2) Icons.Default.RepeatOne else Icons.Default.Repeat,
                            contentDescription = "Repetir",
                            tint = if (repeat == 0) Color.White else MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { timerMenu = true }) {
                                Icon(Icons.Default.Timer, contentDescription = "Duración de imágenes", tint = Color.White)
                            }
                            Text("Imágenes: ${seconds} s", color = Color.White, style = MaterialTheme.typography.labelMedium)
                        }
                        DropdownMenu(expanded = timerMenu, onDismissRequest = { timerMenu = false }) {
                            listOf(2, 3, 5, 8, 10, 15, 30, 60).forEach { s ->
                                DropdownMenuItem(
                                    text = { Text("$s segundos") },
                                    onClick = {
                                        seconds = s
                                        vm.prefs.imageSeconds = s
                                        timerMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
