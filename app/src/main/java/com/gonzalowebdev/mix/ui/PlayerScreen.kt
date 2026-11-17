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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
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
import coil.request.videoFramePercent
import com.gonzalowebdev.mix.MainActivity
import com.gonzalowebdev.mix.PlaybackService
import com.gonzalowebdev.mix.R
import com.gonzalowebdev.mix.data.ColorFilterType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.graphics.ColorMatrix as AndroidColorMatrix
import androidx.compose.ui.graphics.ColorMatrix as ComposeColorMatrix

private fun buildOrder(n: Int, current: Int, shuffle: Boolean): Pair<List<Int>, Int> =
    if (!shuffle) {
        (0 until n).toList() to current
    } else {
        (listOf(current) + (0 until n).filter { it != current }.shuffled()) to 0
    }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    val scope = rememberCoroutineScope()
    val inPip = vm.inPip

    // ---- Estado ----
    val initial = remember { buildOrder(queue.size, vm.startIndex.coerceIn(0, queue.size - 1), prefs.shuffle) }
    var order by remember { mutableStateOf(initial.first) }
    val pagerState = rememberPagerState(initialPage = initial.second) { order.size }

    var tick by remember { mutableIntStateOf(0) }
    var playing by remember { mutableStateOf(true) }
    var finished by remember { mutableStateOf(false) }
    var shuffle by remember { mutableStateOf(prefs.shuffle) }
    var repeat by remember { mutableIntStateOf(prefs.repeatMode) }
    var seconds by remember { mutableIntStateOf(prefs.imageSeconds) }
    var speed by remember { mutableFloatStateOf(prefs.speed) }
    var verticalFeed by remember { mutableStateOf(prefs.feedVertical) }
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
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var hint by remember { mutableStateOf("") }
    var hintStamp by remember { mutableIntStateOf(0) }

    val imgProgress = remember { Animatable(0f) }
    val composeFilter = remember(filterType) {
        filterType.values?.let { ColorFilter.colorMatrix(ComposeColorMatrix(it)) }
    }

    // Dos reproductores: uno reproduce y el otro precarga el siguiente video
    val players = remember {
        List(2) {
            ExoPlayer.Builder(context)
                .setAudioAttributes(AudioAttributes.DEFAULT, !prefs.muted)
                .setHandleAudioBecomingNoisy(true)
                .setWakeMode(C.WAKE_MODE_LOCAL)
                .build()
        }
    }
    val slotPage = remember { mutableStateListOf(-1, -1) }
    var activeSlot by remember { mutableIntStateOf(0) }
    var lastSettled by remember { mutableIntStateOf(pagerState.settledPage) }

    fun itemAt(page: Int) = queue[order[page.coerceIn(0, order.size - 1)]]
    fun settled() = pagerState.settledPage.coerceIn(0, order.size - 1)
    fun currentIsVideo(): Boolean = itemAt(settled()).isVideo

    fun savePosition(page: Int, slot: Int) {
        val it = itemAt(page)
        if (!it.isVideo) return
        val p = players[slot].currentPosition
        val d = players[slot].duration
        if (p > 5_000 && d > 0 && p < d - 8_000) prefs.setPosition(it.uri, p) else prefs.clearPosition(it.uri)
    }

    fun loadVideo(slot: Int, page: Int, playNow: Boolean) {
        val it = itemAt(page)
        val p = players[slot]
        val builder = MediaItem.Builder().setUri(Uri.parse(it.uri))
        val sub = prefs.getSubtitle(it.uri)
        if (sub != null && sub.contains('|')) {
            builder.setSubtitleConfigurations(
                listOf(
                    MediaItem.SubtitleConfiguration.Builder(Uri.parse(sub.substringBeforeLast('|')))
                        .setMimeType(sub.substringAfterLast('|'))
                        .setLanguage("und")
                        .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                        .build()
                )
            )
        }
        p.setMediaItem(builder.build(), prefs.getPosition(it.uri))
        p.prepare()
        p.setPlaybackSpeed(speed)
        p.volume = if (muted) 0f else 1f
        p.playWhenReady = playNow
        slotPage[slot] = page
    }

    /** Deja listo el reproductor del elemento actual y precarga el siguiente. */
    fun activate(page: Int, force: Boolean, forward: Boolean) {
        val cur = itemAt(page)
        if (cur.isVideo) {
            var s = slotPage.indexOf(page)
            if (s < 0 || force || players[s].playerError != null) {
                if (s < 0) {
                    val free = listOf(0, 1).firstOrNull { slotPage[it] != page - 1 && slotPage[it] != page + 1 }
                    s = free ?: (if (forward) slotPage.indexOf(page - 1) else slotPage.indexOf(page + 1)).coerceAtLeast(0)
                }
                loadVideo(s, page, playing)
            }
            activeSlot = s
            players[s].playWhenReady = playing
            players[1 - s].playWhenReady = false
        } else {
            players.forEach { it.playWhenReady = false }
        }
        val nb = if (forward) page + 1 else page - 1
        if (nb in 0 until order.size && itemAt(nb).isVideo && slotPage.indexOf(nb) < 0) {
            val free = if (cur.isVideo) 1 - activeSlot else activeSlot
            loadVideo(free, nb, false)
        }
    }

    fun advanceAuto() {
        val n = order.size
        if (n == 0) return
        if (repeat == 2) {
            tick++
            return
        }
        val cur = settled()
        if (cur + 1 < n) {
            scope.launch { pagerState.animateScrollToPage(cur + 1) }
        } else if (repeat == 1) {
            scope.launch { pagerState.scrollToPage(0) }
        } else {
            playing = false
            finished = true
        }
    }

    fun manual(delta: Int) {
        val n = order.size
        val target = settled() + delta
        scope.launch {
            if (target in 0 until n) pagerState.animateScrollToPage(target)
            else pagerState.scrollToPage(if (target < 0) n - 1 else 0)
        }
    }

    fun onMediaError() {
        errors++
        if (errors > order.size) {
            playing = false
            finished = true
        } else {
            advanceAuto()
        }
    }

    fun togglePlay() {
        if (finished) {
            finished = false
            playing = true
            scope.launch { pagerState.scrollToPage(0) }
            tick++
        } else {
            playing = !playing
        }
    }

    fun setShuffleMode(value: Boolean) {
        shuffle = value
        prefs.shuffle = value
        val (o, p) = buildOrder(queue.size, order[settled()], value)
        order = o
        scope.launch { pagerState.scrollToPage(p) }
    }

    DisposableEffect(Unit) {
        val listeners = players.mapIndexed { idx, p ->
            val l = object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (idx != activeSlot) return
                    if (state == Player.STATE_ENDED) {
                        prefs.clearPosition(itemAt(settled()).uri)
                        advanceAuto()
                    }
                    if (state == Player.STATE_READY) errors = 0
                }

                override fun onPlayerError(error: PlaybackException) {
                    if (idx == activeSlot && currentIsVideo()) onMediaError()
                }

                override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                    if (idx == activeSlot && reason != Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST && currentIsVideo()) {
                        playing = playWhenReady
                    }
                }
            }
            p.addListener(l)
            l
        }
        onDispose {
            savePosition(settled(), activeSlot)
            players.forEachIndexed { i, p ->
                p.removeListener(listeners[i])
                p.release()
            }
        }
    }

    // Cambio de página (TikTok): guarda la posición anterior, activa la nueva y precarga
    LaunchedEffect(Unit) {
        activate(pagerState.settledPage, false, true)
        snapshotFlow { pagerState.settledPage }.collect { page ->
            if (page != lastSettled) {
                val prevSlot = slotPage.indexOf(lastSettled)
                if (prevSlot >= 0) savePosition(lastSettled, prevSlot)
                val forward = page >= lastSettled
                lastSettled = page
                scale = 1f
                offset = Offset.Zero
                finished = false
                activate(page, false, forward)
            }
        }
    }
    // Repetir el actual / recargar (subtítulos)
    LaunchedEffect(tick) {
        if (tick > 0) activate(settled(), true, true)
    }
    LaunchedEffect(playing, activeSlot) {
        if (currentIsVideo()) players[activeSlot].playWhenReady = playing
    }
    LaunchedEffect(speed) { players.forEach { it.setPlaybackSpeed(speed) } }
    LaunchedEffect(muted) {
        players.forEach {
            it.volume = if (muted) 0f else 1f
            it.setAudioAttributes(AudioAttributes.DEFAULT, !muted)
        }
    }

    // Temporizador de imágenes
    LaunchedEffect(pagerState.settledPage, tick, playing, seconds) {
        if (!currentIsVideo()) {
            imgProgress.snapTo(0f)
            if (playing) {
                imgProgress.animateTo(1f, tween(seconds * 1000, easing = LinearEasing))
                advanceAuto()
            }
        }
    }

    // Progreso del video y guardado periódico de la posición
    LaunchedEffect(pagerState.settledPage, tick) {
        var counter = 0
        while (currentIsVideo()) {
            val p = players[activeSlot]
            videoPos = p.currentPosition
            videoDur = p.duration.coerceAtLeast(0L)
            counter++
            if (counter % 10 == 0 && playing) savePosition(settled(), activeSlot)
            delay(300)
        }
    }

    LaunchedEffect(controlsVisible, playing, touchStamp) {
        if (controlsVisible && playing) {
            delay(3500)
            controlsVisible = false
        }
    }
    LaunchedEffect(hintStamp) {
        if (hint.isNotEmpty()) {
            delay(700)
            hint = ""
        }
    }
    LaunchedEffect(sleepMinutes) {
        if (sleepMinutes > 0) {
            delay(sleepMinutes * 60_000L)
            playing = false
            sleepMinutes = 0
        }
    }

    // Imagen en imagen
    SideEffect { vm.pipReady = playing && currentIsVideo() }
    DisposableEffect(Unit) {
        onDispose { vm.pipReady = false }
    }

    // Reproducción en segundo plano
    LaunchedEffect(bgPlayback, playing, pagerState.settledPage) {
        val intent = Intent(context, PlaybackService::class.java)
        if (bgPlayback && playing && currentIsVideo()) {
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
            prefs.setSubtitle(itemAt(settled()).uri, "$uri|$mime")
            tick++
        }
    }

    // ---- Contenido de cada página ----
    val pageContent: @Composable (Int) -> Unit = { pg ->
        val pageItem = itemAt(pg)
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            if (!pageItem.isVideo) {
                val request = remember(pageItem.uri) {
                    ImageRequest.Builder(context)
                        .data(Uri.parse(pageItem.uri))
                        .size(2048)
                        .build()
                }
                val zoomMod = if (pg == pagerState.settledPage) {
                    Modifier.graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    }
                } else {
                    Modifier
                }
                AsyncImage(
                    model = request,
                    contentDescription = pageItem.name,
                    contentScale = ContentScale.Fit,
                    colorFilter = composeFilter,
                    onSuccess = { if (pg == pagerState.settledPage) errors = 0 },
                    onError = { if (pg == pagerState.settledPage) onMediaError() },
                    modifier = Modifier.fillMaxSize().then(zoomMod)
                )
            } else {
                val thumb = remember(pageItem.uri) {
                    ImageRequest.Builder(context)
                        .data(Uri.parse(pageItem.uri))
                        .size(1080)
                        .videoFramePercent(0.1)
                        .build()
                }
                AsyncImage(
                    model = thumb,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    colorFilter = composeFilter,
                    modifier = Modifier.fillMaxSize()
                )
                val s = slotPage.indexOf(pg)
                if (s >= 0) {
                    key(s) {
                        AndroidView(
                            factory = { c ->
                                val pv = LayoutInflater.from(c).inflate(R.layout.player_view, null) as PlayerView
                                pv.player = players[s]
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
                }
            }
        }
    }

    // Gestos de la capa del feed: toque, doble toque, pellizco y arrastre con zoom
    val gestureModifier = Modifier
        .pointerInput(Unit) {
            detectTapGestures(
                onTap = {
                    controlsVisible = !controlsVisible
                    touchStamp++
                },
                onDoubleTap = { p ->
                    if (currentIsVideo()) {
                        val pl = players[activeSlot]
                        val back = p.x < size.width / 2f
                        val dur = pl.duration
                        var target = pl.currentPosition + if (back) -10_000L else 10_000L
                        if (target < 0L) target = 0L
                        if (dur > 0 && target > dur) target = dur
                        pl.seekTo(target)
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
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                var multi = false
                do {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    if (event.changes.count { it.pressed } >= 2) multi = true
                    if (!currentIsVideo() && (multi || scale > 1.01f)) {
                        val zoom = event.calculateZoom()
                        val pan = event.calculatePan()
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
                        event.changes.forEach { if (it.positionChanged()) it.consume() }
                    }
                } while (event.changes.any { it.pressed })
            }
        }

    // ---- UI ----
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val scrollEnabled = scale <= 1.01f && !inPip
        if (verticalFeed) {
            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize().then(gestureModifier),
                beyondViewportPageCount = 1,
                userScrollEnabled = scrollEnabled,
                key = { order[it] }
            ) { pg -> pageContent(pg) }
        } else {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize().then(gestureModifier),
                beyondViewportPageCount = 1,
                userScrollEnabled = scrollEnabled,
                key = { order[it] }
            ) { pg -> pageContent(pg) }
        }

        val uiPage = pagerState.currentPage.coerceIn(0, order.size - 1)
        val item = itemAt(uiPage)

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
                    Icon(MixIcons.ArrowBack, contentDescription = "Cerrar", tint = Color.White)
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
                        text = "${uiPage + 1}/${order.size}",
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
                            imageVector = if (muted) MixIcons.VolumeOff else MixIcons.VolumeUp,
                            contentDescription = if (muted) "Activar sonido" else "Silenciar",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = { (context.findActivity() as? MainActivity)?.enterPip() }) {
                        Icon(MixIcons.PictureInPictureAlt, contentDescription = "Imagen en imagen", tint = Color.White)
                    }
                }
                IconButton(onClick = {
                    shareUris(context, listOf(Uri.parse(item.uri)), allImages = !item.isVideo, allVideos = item.isVideo)
                }) {
                    Icon(MixIcons.Share, contentDescription = "Compartir", tint = Color.White)
                }
                IconButton(onClick = {
                    showOptions = true
                    touchStamp++
                }) {
                    Icon(MixIcons.Tune, contentDescription = "Opciones", tint = Color.White)
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
                                players[activeSlot].seekTo(it.toLong())
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
                            MixIcons.Shuffle,
                            contentDescription = "Aleatorio",
                            tint = if (shuffle) MaterialTheme.colorScheme.primary else Color.White
                        )
                    }
                    IconButton(onClick = {
                        manual(-1)
                        touchStamp++
                    }) {
                        Icon(MixIcons.SkipPrevious, contentDescription = "Anterior", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                    IconButton(onClick = {
                        togglePlay()
                        touchStamp++
                    }, modifier = Modifier.size(64.dp)) {
                        Icon(
                            imageVector = when {
                                finished -> MixIcons.Replay
                                playing -> MixIcons.Pause
                                else -> MixIcons.PlayArrow
                            },
                            contentDescription = "Reproducir / pausar",
                            tint = Color.White,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                    IconButton(onClick = {
                        manual(1)
                        touchStamp++
                    }) {
                        Icon(MixIcons.SkipNext, contentDescription = "Siguiente", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                    IconButton(onClick = {
                        repeat = (repeat + 1) % 3
                        prefs.repeatMode = repeat
                        touchStamp++
                    }) {
                        Icon(
                            imageVector = if (repeat == 2) MixIcons.RepeatOne else MixIcons.Repeat,
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
                    title = "Deslizar para cambiar",
                    options = listOf(true, false),
                    selected = verticalFeed,
                    label = { if (it) "Vertical" else "Horizontal" },
                    onSelect = {
                        verticalFeed = it
                        prefs.feedVertical = it
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
                if (itemAt(settled()).isVideo) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            showOptions = false
                            subtitleLauncher.launch(arrayOf("*/*"))
                        }) { Text("Cargar subtítulos") }
                        if (prefs.getSubtitle(itemAt(settled()).uri) != null) {
                            OutlinedButton(onClick = {
                                prefs.setSubtitle(itemAt(settled()).uri, null)
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
