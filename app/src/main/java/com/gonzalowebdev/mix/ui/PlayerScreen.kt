package com.gonzalowebdev.mix.ui

import android.Manifest
import android.content.Intent
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.TextureView
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.gonzalowebdev.mix.MainActivity
import com.gonzalowebdev.mix.PlaybackService
import com.gonzalowebdev.mix.R
import com.gonzalowebdev.mix.data.ColorFilterType
import com.gonzalowebdev.mix.data.QueueItem
import kotlinx.coroutines.delay
import kotlin.math.abs
import android.graphics.ColorMatrix as AndroidColorMatrix
import androidx.compose.ui.graphics.ColorMatrix as ComposeColorMatrix

private data class Slide(val tick: Int, val item: QueueItem)

private fun buildOrder(n: Int, current: Int, shuffle: Boolean): Pair<List<Int>, Int> =
    if (!shuffle) {
        (0 until n).toList() to current
    } else {
        (listOf(current) + (0 until n).filter { it != current }.shuffled()) to 0
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(vm: MixViewModel, onClose: () -> Unit) {
    val queue = vm.queue
    if (queue.isEmpty()) {
        LaunchedEffect(Unit) { onClose() }
        return
    }

    val context = LocalContext.current
    val view = LocalView.current
    val prefs = vm.prefs
    val inPip = vm.inPip

    // ---- Estado ----
    val initial = remember { buildOrder(queue.size, vm.startIndex.coerceIn(0, queue.size - 1), prefs.shuffle) }
    var order by remember { mutableStateOf(initial.first) }
    var pos by remember { mutableIntStateOf(initial.second) }
    var tick by remember { mutableIntStateOf(0) }
    var playing by remember { mutableStateOf(true) }
    var finished by remember { mutableStateOf(false) }
    var shuffle by remember { mutableStateOf(prefs.shuffle) }
    var repeat by remember { mutableIntStateOf(prefs.repeatMode) }
    var seconds by remember { mutableIntStateOf(prefs.imageSeconds) }
    var transition by remember { mutableIntStateOf(prefs.transition) }
    var speed by remember { mutableFloatStateOf(prefs.speed) }
    var filterType by remember {
        mutableStateOf(
            try {
                ColorFilterType.valueOf(prefs.colorFilter)
            } catch (e: Exception) {
                ColorFilterType.NONE
            }
        )
    }
    var bgPlayback by remember { mutableStateOf(prefs.bgPlayback) }
    var muted by remember { mutableStateOf(prefs.muted) }
    var sleepMinutes by remember { mutableIntStateOf(0) }
    var errors by remember { mutableIntStateOf(0) }
    var controlsVisible by remember { mutableStateOf(true) }
    var touchStamp by remember { mutableIntStateOf(0) }
    var videoPos by remember { mutableLongStateOf(0L) }
    var videoDur by remember { mutableLongStateOf(0L) }
    var showOptions by remember { mutableStateOf(false) }
    var dir by remember { mutableIntStateOf(1) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var hint by remember { mutableStateOf("") }
    var hintStamp by remember { mutableIntStateOf(0) }

    val item = queue[order[pos]]
    val imgProgress = remember { Animatable(0f) }

    val composeFilter = remember(filterType) {
        filterType.values?.let { ColorFilter.colorMatrix(ComposeColorMatrix(it)) }
    }

    // ---- ExoPlayer ----
    val player = remember {
        ExoPlayer.Builder(context)
            .setAudioAttributes(AudioAttributes.DEFAULT, !prefs.muted)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
    }

    fun currentIsVideo(): Boolean = queue[order[pos]].isVideo

    fun savePosition() {
        val cur = queue[order[pos]]
        if (!cur.isVideo) return
        val p = player.currentPosition
        val d = player.duration
        if (p > 5_000 && d > 0 && p < d - 8_000) prefs.setPosition(cur.uri, p) else prefs.clearPosition(cur.uri)
    }

    fun go(delta: Int, auto: Boolean) {
        val n = order.size
        if (n == 0) return
        savePosition()
        scale = 1f
        offset = Offset.Zero
        dir = if (delta >= 0) 1 else -1
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
        prefs.shuffle = value
        val (o, p) = buildOrder(queue.size, order[pos], value)
        order = o
        pos = p
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
            savePosition()
            player.removeListener(listener)
            player.release()
        }
    }

    // Cargar el elemento actual (retomando donde se dejó y con subtítulos guardados)
    LaunchedEffect(tick) {
        val current = queue[order[pos]]
        if (current.isVideo) {
            val start = prefs.getPosition(current.uri)
            val builder = MediaItem.Builder().setUri(Uri.parse(current.uri))
            val sub = prefs.getSubtitle(current.uri)
            if (sub != null && sub.contains('|')) {
                val subUri = sub.substringBeforeLast('|')
                val subMime = sub.substringAfterLast('|')
                builder.setSubtitleConfigurations(
                    listOf(
                        MediaItem.SubtitleConfiguration.Builder(Uri.parse(subUri))
                            .setMimeType(subMime)
                            .setLanguage("und")
                            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                            .build()
                    )
                )
            }
            player.setMediaItem(builder.build(), start)
            player.prepare()
            player.setPlaybackSpeed(speed)
            player.playWhenReady = playing
        } else {
            player.stop()
            player.clearMediaItems()
        }
    }
    LaunchedEffect(playing) {
        if (currentIsVideo()) player.playWhenReady = playing
    }
    LaunchedEffect(speed) { player.setPlaybackSpeed(speed) }

    // Sonido apagado por defecto: sin volumen y sin quitarle el audio a otras apps
    LaunchedEffect(muted) {
        player.volume = if (muted) 0f else 1f
        player.setAudioAttributes(AudioAttributes.DEFAULT, !muted)
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

    // Progreso del video y guardado periódico de la posición
    LaunchedEffect(tick) {
        var counter = 0
        while (queue[order[pos]].isVideo) {
            videoPos = player.currentPosition
            videoDur = player.duration.coerceAtLeast(0L)
            counter++
            if (counter % 10 == 0 && playing) savePosition()
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

    // Aviso de salto de ±10 s
    LaunchedEffect(hintStamp) {
        if (hint.isNotEmpty()) {
            delay(700)
            hint = ""
        }
    }

    // Temporizador de apagado
    LaunchedEffect(sleepMinutes) {
        if (sleepMinutes > 0) {
            delay(sleepMinutes * 60_000L)
            playing = false
            sleepMinutes = 0
        }
    }

    // Imagen en imagen
    SideEffect { vm.pipReady = playing && item.isVideo }
    DisposableEffect(Unit) {
        onDispose { vm.pipReady = false }
    }

    // Reproducción en segundo plano (servicio en primer plano)
    LaunchedEffect(bgPlayback, playing, item.isVideo) {
        val intent = Intent(context, PlaybackService::class.java)
        if (bgPlayback && playing && item.isVideo) {
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (e: Exception) {
                // no se pudo iniciar el servicio
            }
        } else {
            context.stopService(intent)
        }
    }
    DisposableEffect(Unit) {
        onDispose { context.stopService(Intent(context, PlaybackService::class.java)) }
    }

    // Pantalla completa y pantalla encendida
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
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        if (!(bgPlayback && playing && currentIsVideo())) playing = false
    }

    BackHandler { onClose() }

    // Selectores
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val subtitleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: Exception) {
                // permiso no persistible
            }
            var name = uri.lastPathSegment ?: ""
            try {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                    if (c.moveToFirst()) name = c.getString(0) ?: name
                }
            } catch (e: Exception) {
                // nombre no disponible
            }
            val lower = name.lowercase()
            val mime = when {
                lower.endsWith(".vtt") -> MimeTypes.TEXT_VTT
                lower.endsWith(".ass") || lower.endsWith(".ssa") -> MimeTypes.TEXT_SSA
                else -> MimeTypes.APPLICATION_SUBRIP
            }
            prefs.setSubtitle(queue[order[pos]].uri, "$uri|$mime")
            tick++
        }
    }

    // ---- UI ----
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (item.isVideo) {
            AndroidView(
                factory = { c ->
                    val pv = LayoutInflater.from(c).inflate(R.layout.player_view, null) as PlayerView
                    pv.player = player
                    pv
                },
                update = { pv ->
                    val tv = pv.videoSurfaceView as? TextureView
                    val values = filterType.values
                    if (tv != null) {
                        if (values != null) {
                            val paint = Paint()
                            paint.colorFilter = ColorMatrixColorFilter(AndroidColorMatrix(values))
                            tv.setLayerType(View.LAYER_TYPE_HARDWARE, paint)
                        } else {
                            tv.setLayerType(View.LAYER_TYPE_NONE, null)
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        AnimatedContent(
            targetState = Slide(tick, item),
            transitionSpec = {
                when (transition) {
                    1 -> fadeIn(tween(400)) togetherWith fadeOut(tween(400))
                    2 -> (slideInHorizontally(tween(350)) { full -> full * dir } + fadeIn(tween(350))) togetherWith
                        (slideOutHorizontally(tween(350)) { full -> -full * dir } + fadeOut(tween(350)))
                    else -> EnterTransition.None togetherWith ExitTransition.None
                }
            },
            modifier = Modifier.fillMaxSize(),
            label = "slide"
        ) { slide ->
            if (!slide.item.isVideo) {
                val request = remember(slide.item.uri, slide.tick) {
                    ImageRequest.Builder(context)
                        .data(Uri.parse(slide.item.uri))
                        .size(2048)
                        .build()
                }
                AsyncImage(
                    model = request,
                    contentDescription = slide.item.name,
                    contentScale = ContentScale.Fit,
                    colorFilter = composeFilter,
                    onSuccess = { errors = 0 },
                    onError = { onMediaError() },
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x
                            translationY = offset.y
                        }
                )
            } else {
                Box(Modifier.fillMaxSize())
            }
        }

        // Capa de gestos: toque, doble toque, pellizco para zoom, deslizar para cambiar
        if (!inPip) {
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                controlsVisible = !controlsVisible
                                touchStamp++
                            },
                            onDoubleTap = { p ->
                                if (currentIsVideo()) {
                                    val back = p.x < size.width / 2f
                                    val dur = player.duration
                                    var target = player.currentPosition + if (back) -10_000L else 10_000L
                                    if (target < 0L) target = 0L
                                    if (dur > 0 && target > dur) target = dur
                                    player.seekTo(target)
                                    hint = if (back) "−10 s" else "+10 s"
                                    hintStamp++
                                } else if (scale > 1.01f) {
                                    scale = 1f
                                    offset = Offset.Zero
                                } else {
                                    scale = 2.5f
                                    offset = Offset.Zero
                                }
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            var swipe = 0f
                            var multi = false
                            do {
                                val event = awaitPointerEvent()
                                if (event.changes.count { it.pressed } >= 2) multi = true
                                val zoom = event.calculateZoom()
                                val pan = event.calculatePan()
                                if (!currentIsVideo() && (multi || scale > 1.01f)) {
                                    val newScale = (scale * zoom).coerceIn(1f, 6f)
                                    val maxX = size.width * (newScale - 1f) / 2f
                                    val maxY = size.height * (newScale - 1f) / 2f
                                    scale = newScale
                                    offset = if (newScale <= 1.01f) {
                                        Offset.Zero
                                    } else {
                                        Offset(
                                            (offset.x + pan.x).coerceIn(-maxX, maxX),
                                            (offset.y + pan.y).coerceIn(-maxY, maxY)
                                        )
                                    }
                                    if (event.changes.any { it.positionChanged() }) {
                                        event.changes.forEach { it.consume() }
                                    }
                                } else {
                                    swipe += pan.x
                                    if (abs(swipe) > viewConfiguration.touchSlop) {
                                        event.changes.forEach { it.consume() }
                                    }
                                }
                            } while (event.changes.any { it.pressed })
                            if (!multi && scale <= 1.01f) {
                                if (swipe < -150f) go(1, false) else if (swipe > 150f) go(-1, false)
                            }
                        }
                    }
            )
        }

        if (hint.isNotEmpty()) {
            Text(
                text = hint,
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color(0x99000000), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        AnimatedVisibility(visible = controlsVisible && !inPip, modifier = Modifier.align(Alignment.TopCenter)) {
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
                Column(Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "${pos + 1}/${order.size}",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                if (item.isVideo) {
                    IconButton(onClick = {
                        muted = !muted
                        prefs.muted = muted
                        touchStamp++
                    }) {
                        Icon(
                            imageVector = if (muted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = if (muted) "Activar sonido" else "Silenciar",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = { (context.findActivity() as? MainActivity)?.enterPip() }) {
                        Icon(Icons.Default.PictureInPictureAlt, contentDescription = "Imagen en imagen", tint = Color.White)
                    }
                }
                IconButton(onClick = {
                    shareUris(context, listOf(Uri.parse(item.uri)), allImages = !item.isVideo, allVideos = item.isVideo)
                }) {
                    Icon(Icons.Default.Share, contentDescription = "Compartir", tint = Color.White)
                }
                IconButton(onClick = {
                    showOptions = true
                    touchStamp++
                }) {
                    Icon(Icons.Default.Tune, contentDescription = "Opciones", tint = Color.White)
                }
            }
        }

        AnimatedVisibility(visible = controlsVisible && !inPip, modifier = Modifier.align(Alignment.BottomCenter)) {
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
                    IconButton(onClick = {
                        setShuffleMode(!shuffle)
                        touchStamp++
                    }) {
                        Icon(
                            Icons.Default.Shuffle,
                            contentDescription = "Aleatorio",
                            tint = if (shuffle) MaterialTheme.colorScheme.primary else Color.White
                        )
                    }
                    IconButton(onClick = {
                        go(-1, false)
                        touchStamp++
                    }) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Anterior", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                    IconButton(onClick = {
                        togglePlay()
                        touchStamp++
                    }, modifier = Modifier.size(64.dp)) {
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
                    IconButton(onClick = {
                        go(1, false)
                        touchStamp++
                    }) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Siguiente", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                    IconButton(onClick = {
                        repeat = (repeat + 1) % 3
                        prefs.repeatMode = repeat
                        touchStamp++
                    }) {
                        Icon(
                            imageVector = if (repeat == 2) Icons.Default.RepeatOne else Icons.Default.Repeat,
                            contentDescription = "Repetir",
                            tint = if (repeat == 0) Color.White else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }

    if (showOptions) {
        ModalBottomSheet(onDismissRequest = { showOptions = false }) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ChipRow(
                    title = "Filtro de color",
                    options = ColorFilterType.entries.toList(),
                    selected = filterType,
                    label = { it.label },
                    wrap = true,
                    onSelect = {
                        filterType = it
                        prefs.colorFilter = it.name
                    }
                )
                ChipRow(
                    title = "Duración de cada imagen",
                    options = listOf(2, 3, 5, 8, 10, 15, 30, 60),
                    selected = seconds,
                    label = { "$it s" },
                    onSelect = {
                        seconds = it
                        prefs.imageSeconds = it
                    }
                )
                ChipRow(
                    title = "Transición entre imágenes",
                    options = listOf(0, 1, 2),
                    selected = transition,
                    label = { if (it == 0) "Ninguna" else if (it == 1) "Fundido" else "Deslizar" },
                    onSelect = {
                        transition = it
                        prefs.transition = it
                    }
                )
                ChipRow(
                    title = "Velocidad de video",
                    options = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f),
                    selected = speed,
                    label = { "${it}x" },
                    onSelect = {
                        speed = it
                        prefs.speed = it
                    }
                )
                ChipRow(
                    title = "Temporizador de apagado",
                    options = listOf(0, 15, 30, 45, 60, 90),
                    selected = sleepMinutes,
                    label = { if (it == 0) "Apagado" else "$it min" },
                    onSelect = { sleepMinutes = it }
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Seguir reproduciendo en segundo plano", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Solo videos. Muestra una notificación mientras suena.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = bgPlayback,
                        onCheckedChange = { value ->
                            bgPlayback = value
                            prefs.bgPlayback = value
                            if (value && Build.VERSION.SDK_INT >= 33) {
                                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    )
                }
                if (item.isVideo) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            showOptions = false
                            subtitleLauncher.launch(arrayOf("*/*"))
                        }) { Text("Cargar subtítulos") }
                        if (prefs.getSubtitle(item.uri) != null) {
                            OutlinedButton(onClick = {
                                prefs.setSubtitle(item.uri, null)
                                showOptions = false
                                tick++
                            }) { Text("Quitar subtítulos") }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> ChipRow(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    wrap: Boolean = false
) {
    val chips: @Composable () -> Unit = {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(label(option)) }
            )
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        if (wrap) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) { chips() }
        } else {
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) { chips() }
        }
    }
}
