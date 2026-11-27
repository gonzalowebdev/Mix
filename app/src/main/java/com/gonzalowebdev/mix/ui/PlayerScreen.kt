package com.gonzalowebdev.mix.ui

import android.Manifest
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.TextureView
import android.view.View
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.CompositionLocalProvider
import com.gonzalowebdev.mix.data.MediaThumb
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.gonzalowebdev.mix.data.MediaFile
import com.gonzalowebdev.mix.data.PRESET_STYLES
import com.gonzalowebdev.mix.data.VisualStyle
import com.gonzalowebdev.mix.data.QueueItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import android.graphics.ColorMatrix as AndroidColorMatrix
import androidx.compose.ui.graphics.ColorMatrix as ComposeColorMatrix

private fun buildOrder(n: Int, current: Int, shuffle: Boolean): Pair<List<Int>, Int> =
    if (!shuffle) {
        (0 until n).toList() to current
    } else {
        (listOf(current) + (0 until n).filter { it != current }.shuffled()) to 0
    }

/** Aplica resistencia cuando el valor se pasa de los límites (efecto elástico). */
private fun soft(v: Float, lo: Float, hi: Float): Float = when {
    v < lo -> lo + (v - lo) * 0.35f
    v > hi -> hi + (v - hi) * 0.35f
    else -> v
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun PlayerScreen(vm: MixViewModel, onClose: () -> Unit) {
    var queue by remember { mutableStateOf(vm.queue) }
    if (queue.isEmpty()) {
        LaunchedEffect(Unit) { onClose() }
        return
    }

    val context = LocalContext.current
    val view = LocalView.current
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val prefs = vm.prefs
    val scope = rememberCoroutineScope()
    val introMemory = remember { vm.takeIntro() }
    val memoryMode = introMemory != null
    val inPip = vm.inPip
    val playlists by vm.playlists.collectAsStateWithLifecycle()
    val favorites = vm.favorites

    // ---- Estado ----
    val initial = remember { buildOrder(queue.size, vm.startIndex.coerceIn(0, queue.size - 1), prefs.shuffle) }
    var order by remember { mutableStateOf(initial.first) }
    val pagerState = rememberPagerState(initialPage = initial.second) { order.size }

    var tick by remember { mutableIntStateOf(0) }
    var playing by remember { mutableStateOf(introMemory == null) }
    var finished by remember { mutableStateOf(false) }
    var shuffle by remember { mutableStateOf(prefs.shuffle) }
    var repeat by remember { mutableIntStateOf(prefs.repeatMode) }
    var seconds by remember { mutableIntStateOf(prefs.imageSeconds) }
    var speed by remember { mutableFloatStateOf(prefs.speed) }
    var verticalFeed by remember { mutableStateOf(prefs.feedVertical) }
    var feedMode by remember { mutableStateOf(prefs.feedMode) }
    var kenBurns by remember { mutableStateOf(prefs.kenBurns) }
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
    var showInfo by remember { mutableStateOf(false) }
    var showAddList by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var forcedLandscape by remember { mutableStateOf(false) }
    var hint by remember { mutableStateOf("") }
    var hintStamp by remember { mutableIntStateOf(0) }
    var speedBoost by remember { mutableStateOf(false) }
    var scrubbing by remember { mutableStateOf(false) }
    var scrubText by remember { mutableStateOf("") }
    var mirror by remember { mutableIntStateOf(prefs.mirror) }
    var tvMode by remember { mutableIntStateOf(prefs.tvMode) }
    var tvStatic by remember { mutableStateOf(prefs.tvStatic) }
    var ambient by remember { mutableStateOf(prefs.ambient) }
    var tvTime by remember { mutableFloatStateOf(0f) }
    val tvPower = remember { Animatable(1f) }
    var showTips by remember { mutableStateOf(!prefs.tipsSeen && introMemory == null) }
    var showIntro by remember { mutableStateOf(introMemory != null) }
    var showSaveStyle by remember { mutableStateOf(false) }
    var optionsTab by remember { mutableIntStateOf(0) }
    var staticOn by remember { mutableStateOf(false) }
    val crt = remember { if (Build.VERSION.SDK_INT >= 33) CrtShader.create() else null }

    // Zoom
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var zoomJob by remember { mutableStateOf<Job?>(null) }
    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    val zoomed by remember { derivedStateOf { scale > 1.05f } }
    val zoomLocked by remember { derivedStateOf { scale > 1.01f } }

    val imgProgress = remember { Animatable(0f) }
    var progressKey by remember { mutableLongStateOf(-1L) }
    val osdSeconds by remember {
        derivedStateOf {
            if (itemAt0(queue, order, pagerState.settledPage).isVideo) videoPos / 1000 else (imgProgress.value * seconds).toLong()
        }
    }
    val composeFilter = remember(filterType) {
        filterType.values?.let { ColorFilter.colorMatrix(ComposeColorMatrix(it)) }
    }

    // Dos reproductores: uno reproduce y el otro precarga el siguiente video
    val players = remember {
        List(2) {
            newExoPlayer(context, handleAudioFocus = !prefs.muted)
        }
    }
    val slotUri = remember { mutableStateListOf("", "") }
    var activeSlot by remember { mutableIntStateOf(0) }
    var lastSettled by remember { mutableIntStateOf(pagerState.settledPage) }

    fun itemAt(page: Int) = queue[order[page.coerceIn(0, order.size - 1)]]
    fun settled() = pagerState.settledPage.coerceIn(0, order.size - 1)
    fun currentIsVideo(): Boolean = itemAt(settled()).isVideo

    fun savePosition(page: Int, slot: Int) {
        val target = itemAt(page)
        if (!target.isVideo) return
        val p = players[slot].currentPosition
        val d = players[slot].duration
        if (p > 5_000 && d > 0 && p < d - 8_000) prefs.setPosition(target.uri, p) else prefs.clearPosition(target.uri)
    }

    fun loadVideo(slot: Int, page: Int, playNow: Boolean) {
        val target = itemAt(page)
        val p = players[slot]
        val builder = MediaItem.Builder().setUri(Uri.parse(target.uri))
        val sub = prefs.getSubtitle(target.uri)
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
        p.setMediaItem(builder.build(), prefs.getPosition(target.uri))
        p.repeatMode = if (feedMode) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        p.prepare()
        p.setPlaybackSpeed(speed)
        p.volume = if (muted) 0f else 1f
        p.playWhenReady = playNow
        slotUri[slot] = target.uri
    }

    /** Deja listo el reproductor del elemento actual y precarga el siguiente. */
    fun activate(page: Int, force: Boolean, forward: Boolean) {
        val cur = itemAt(page)
        if (cur.isVideo) {
            var s = slotUri.indexOf(cur.uri)
            if (s < 0 || force || players[s].playerError != null) {
                if (s < 0) {
                    val prevUri = if (page - 1 >= 0) itemAt(page - 1).uri else ""
                    val nextUri = if (page + 1 < order.size) itemAt(page + 1).uri else ""
                    val free = listOf(0, 1).firstOrNull {
                        (prevUri.isEmpty() || slotUri[it] != prevUri) && (nextUri.isEmpty() || slotUri[it] != nextUri)
                    }
                    s = free ?: (if (forward) slotUri.indexOf(prevUri) else slotUri.indexOf(nextUri)).coerceAtLeast(0)
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
        if (nb in 0 until order.size && itemAt(nb).isVideo && slotUri.indexOf(itemAt(nb).uri) < 0) {
            val free = if (cur.isVideo) 1 - activeSlot else activeSlot
            loadVideo(free, nb, false)
        }
    }

    fun advanceAuto() {
        val n = order.size
        if (n == 0 || feedMode) return
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

    fun onMediaError() {
        errors++
        if (errors > order.size) {
            playing = false
            finished = true
        } else {
            val cur = settled()
            if (cur + 1 < order.size) scope.launch { pagerState.animateScrollToPage(cur + 1) } else advanceAuto()
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

    /** Quita un archivo (eliminado) del feed sin romper el deslizamiento. */
    fun removeFromFeed(uri: String) {
        val idx = queue.indexOfFirst { it.uri == uri }
        if (idx < 0) return
        players.forEach {
            it.stop()
            it.clearMediaItems()
        }
        slotUri[0] = ""
        slotUri[1] = ""
        val newQueue = queue.filterIndexed { i, _ -> i != idx }
        if (newQueue.isEmpty()) {
            onClose()
            return
        }
        order = order.filter { it != idx }.map { if (it > idx) it - 1 else it }
        queue = newQueue
        scale = 1f
        offset = Offset.Zero
        tick++
    }

    // ---- Zoom suave ----
    fun boundsFor(s: Float): Offset = Offset(
        viewSize.width * maxOf(s - 1f, 0f) / 2f,
        viewSize.height * maxOf(s - 1f, 0f) / 2f
    )

    fun clampOffset(o: Offset, s: Float): Offset {
        val b = boundsFor(s)
        return Offset(o.x.coerceIn(-b.x, b.x), o.y.coerceIn(-b.y, b.y))
    }

    suspend fun zoomAnim(targetScale: Float, targetOffset: Offset, spec: AnimationSpec<Float>) {
        val s0 = scale
        val o0 = offset
        Animatable(0f).animateTo(1f, spec) {
            val t = value
            scale = s0 + (targetScale - s0) * t
            offset = Offset(o0.x + (targetOffset.x - o0.x) * t, o0.y + (targetOffset.y - o0.y) * t)
        }
    }

    fun settleZoom(velocity: Offset) {
        zoomJob?.cancel()
        if (!scale.isFinite() || !offset.x.isFinite() || !offset.y.isFinite()) {
            scale = 1f
            offset = Offset.Zero
        }
        if (!velocity.x.isFinite() || !velocity.y.isFinite()) {
            scale = scale.coerceAtLeast(1f)
        }
        zoomJob = scope.launch {
            val s = scale
            if (s <= 1.02f) {
                zoomAnim(1f, Offset.Zero, spring(stiffness = Spring.StiffnessMedium))
            } else {
                val clamped = clampOffset(offset, s)
                if (clamped != offset) {
                    zoomAnim(s, clamped, spring(stiffness = Spring.StiffnessMedium))
                } else if (abs(velocity.x) > 150f || abs(velocity.y) > 150f) {
                    val start = offset
                    val b = boundsFor(s)
                    Animatable(Offset.Zero, Offset.VectorConverter)
                        .animateDecay(velocity, splineBasedDecay<Offset>(density)) {
                            offset = Offset(
                                (start.x + value.x).coerceIn(-b.x, b.x),
                                (start.y + value.y).coerceIn(-b.y, b.y)
                            )
                        }
                }
            }
        }
    }

    fun resetZoom() {
        zoomJob?.cancel()
        zoomJob = scope.launch { zoomAnim(1f, Offset.Zero, tween(260, easing = FastOutSlowInEasing)) }
    }

    fun toggleRotation() {
        val act = context.findActivity() ?: return
        forcedLandscape = !forcedLandscape
        act.requestedOrientation = if (forcedLandscape) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    fun applyStyle(st: VisualStyle) {
        filterType = try {
            ColorFilterType.valueOf(st.filter)
        } catch (e: Exception) {
            ColorFilterType.NONE
        }
        prefs.colorFilter = filterType.name
        mirror = st.mirror
        prefs.mirror = st.mirror
        val wasOff = tvMode == 0
        tvMode = st.tvMode
        prefs.tvMode = st.tvMode
        tvStatic = st.tvStatic
        prefs.tvStatic = st.tvStatic
        ambient = st.ambient
        prefs.ambient = st.ambient
        kenBurns = st.kenBurns
        prefs.kenBurns = st.kenBurns
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        if (wasOff && st.tvMode != 0) {
            scope.launch {
                tvPower.snapTo(0f)
                tvPower.animateTo(1f, tween(480))
            }
        }
    }

    // Eliminar (papelera del sistema o definitivo) y limpiar el feed
    val remover = rememberFileRemover { uris, ok ->
        if (ok) {
            vm.onFilesRemoved(uris)
            uris.forEach { removeFromFeed(it.toString()) }
        }
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

    // Cambio de página (como TikTok): guarda la posición anterior, activa la nueva y precarga
    LaunchedEffect(Unit) {
        activate(pagerState.settledPage, false, true)
        vm.markPlayed(itemAt(pagerState.settledPage).uri)
        snapshotFlow { pagerState.settledPage }.collect { page ->
            if (page != lastSettled) {
                vm.markPlayed(itemAt(page).uri)
                val prevSlot = slotUri.indexOf(itemAt(lastSettled).uri)
                if (prevSlot >= 0) savePosition(lastSettled, prevSlot)
                val forward = page >= lastSettled
                lastSettled = page
                zoomJob?.cancel()
                scale = 1f
                offset = Offset.Zero
                finished = false
                if (tvMode != 0 && tvStatic) {
                    staticOn = true
                    scope.launch {
                        delay(260)
                        staticOn = false
                    }
                }
                activate(page, false, forward)
            }
        }
    }
    // Repetir el actual / recargar (subtítulos, archivo eliminado)
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
    LaunchedEffect(feedMode) {
        players.forEach { it.repeatMode = if (feedMode) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF }
    }

    // Temporizador de imágenes (se pausa sin reiniciarse; no corre en modo feed)
    LaunchedEffect(pagerState.settledPage, tick, playing, seconds, feedMode) {
        if (!currentIsVideo()) {
            val key = pagerState.settledPage * 100_000L + tick
            if (key != progressKey) {
                imgProgress.snapTo(0f)
                progressKey = key
            }
            if (playing && !feedMode) {
                val remaining = (1f - imgProgress.value).coerceIn(0f, 1f)
                imgProgress.animateTo(
                    1f,
                    tween(maxOf(1, (seconds * 1000 * remaining).toInt()), easing = LinearEasing)
                )
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
    // Presentación del recuerdo: unos segundos con la portada y el título
    LaunchedEffect(Unit) {
        if (introMemory != null) {
            delay(2600)
            showIntro = false
            playing = true
        }
    }
    LaunchedEffect(hintStamp) {
        if (hint.isNotEmpty()) {
            delay(700)
            hint = ""
        }
    }
    LaunchedEffect(tvMode) {
        while (tvMode != 0) {
            tvTime = ((System.nanoTime() / 1_000_000L) % 1_000_000L) / 1000f
            delay(66)
        }
    }
    LaunchedEffect(sleepMinutes) {
        if (sleepMinutes > 0) {
            delay(sleepMinutes * 60_000L)
            playing = false
            sleepMinutes = 0
        }
    }

    // Imagen en imagen (con botón de pausa en la ventana flotante)
    SideEffect {
        vm.pipReady = playing && currentIsVideo()
        vm.pipPlaying = playing
    }
    DisposableEffect(Unit) {
        onDispose { vm.pipReady = false }
    }
    var pipHandled by remember { mutableIntStateOf(vm.pipToggleCount) }
    LaunchedEffect(vm.pipToggleCount) {
        if (vm.pipToggleCount != pipHandled) {
            pipHandled = vm.pipToggleCount
            togglePlay()
        }
    }
    LaunchedEffect(playing, inPip) {
        if (inPip) (context.findActivity() as? MainActivity)?.updatePipActions(playing)
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

    // Pantalla completa, pantalla encendida y orientación
    DisposableEffect(Unit) {
        val act = context.findActivity()
        val controller = act?.window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        view.keepScreenOn = true
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
            view.keepScreenOn = false
            act?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
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
        val isSettled = pg == pagerState.settledPage
        val kbActive = (kenBurns || memoryMode) && !feedMode && isSettled && !pageItem.isVideo
        val variant = abs(pageItem.uri.hashCode()) % 3
        val kbDx = when (variant) {
            1 -> -0.03f
            2 -> 0.03f
            else -> 0f
        }
        val kbDy = if (variant == 0) -0.02f else 0f
        val layerMod = if (isSettled) {
            Modifier.graphicsLayer {
                val sc = if (scale.isFinite() && scale > 0f) scale else 1f
                val ox = if (offset.x.isFinite()) offset.x else 0f
                val oy = if (offset.y.isFinite()) offset.y else 0f
                val p = if (kbActive && sc <= 1.01f) imgProgress.value else 0f
                val ks = 1f + 0.08f * p
                scaleX = sc * ks
                scaleY = sc * ks
                translationX = ox + kbDx * p * size.width
                translationY = oy + kbDy * p * size.height
            }
        } else {
            Modifier
        }
        Box(Modifier.fillMaxSize().background(Color.Black).clipToBounds()) {
            if (ambient && tvMode == 0) {
                val backdrop = remember(pageItem.uri) {
                    ImageRequest.Builder(context)
                        .data(Uri.parse(pageItem.uri))
                        .size(160)
                        .apply { if (pageItem.isVideo) videoFramePercent(0.1) }
                        .build()
                }
                AsyncImage(
                    model = backdrop,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    colorFilter = composeFilter,
                    modifier = Modifier.fillMaxSize().blur(36.dp)
                )
                Box(Modifier.fillMaxSize().background(Color(0x66000000)))
            }
            Box(Modifier.fillMaxSize().then(layerMod)) {
                Box(
                    Modifier.fillMaxSize().graphicsLayer {
                        scaleX = if ((mirror and 1) != 0) -1f else 1f
                        scaleY = if ((mirror and 2) != 0) -1f else 1f
                    }
                ) {
                    if (!pageItem.isVideo) {
                        val request = remember(pageItem.uri) {
                            ImageRequest.Builder(context)
                                .data(Uri.parse(pageItem.uri))
                                .size(2048)
                                .build()
                        }
                        var intrinsic by remember(pageItem.uri) { mutableStateOf(Size.Zero) }
                        AsyncImage(
                            model = request,
                            contentDescription = pageItem.name,
                            contentScale = ContentScale.Fit,
                            colorFilter = composeFilter,
                            onSuccess = { st ->
                                intrinsic = st.painter.intrinsicSize
                                if (isSettled) errors = 0
                            },
                            onError = { if (isSettled) onMediaError() },
                            modifier = Modifier
                                .fillMaxSize()
                                .then(if (ambient && tvMode == 0) featherModifier(intrinsic) else Modifier)
                        )
                    } else {
                        val thumb = remember(pageItem.uri) {
                            ImageRequest.Builder(context)
                                .data(MediaThumb(Uri.parse(pageItem.uri), true))
                                .size(1080)
                                .build()
                        }
                        AsyncImage(
                            model = thumb,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            colorFilter = composeFilter,
                            modifier = Modifier.fillMaxSize()
                        )
                        val s = slotUri.indexOf(pageItem.uri)
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
        }
    }

    // Gestos: toque, doble toque, mantener (2x), arrastre horizontal (adelantar) y zoom
    val gestureModifier = Modifier
        .onSizeChanged { viewSize = it }
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    if (currentIsVideo() && !zoomLocked) {
                        val job = scope.launch {
                            delay(450)
                            speedBoost = true
                            players[activeSlot].setPlaybackSpeed(2f)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        tryAwaitRelease()
                        job.cancel()
                        if (speedBoost) {
                            speedBoost = false
                            players[activeSlot].setPlaybackSpeed(speed)
                        }
                    }
                },
                onLongPress = { },
                onTap = {
                    if (showOptions) {
                        showOptions = false
                    } else {
                        controlsVisible = !controlsVisible
                        touchStamp++
                    }
                },
                onDoubleTap = { p ->
                    if (currentIsVideo()) {
                        val pl = players[activeSlot]
                        val back = p.x < viewSize.width / 2f
                        val dur = pl.duration
                        var target = pl.currentPosition + if (back) -10_000L else 10_000L
                        if (target < 0L) target = 0L
                        if (dur > 0 && target > dur) target = dur
                        pl.seekTo(target)
                        hint = if (back) "−10 s" else "+10 s"
                        hintStamp++
                    } else if (scale > 1.01f) {
                        resetZoom()
                    } else {
                        val target = 2.5f
                        val c = Offset(viewSize.width / 2f, viewSize.height / 2f)
                        val o = clampOffset((p - c) * (1f - target), target)
                        zoomJob?.cancel()
                        zoomJob = scope.launch { zoomAnim(target, o, tween(260, easing = FastOutSlowInEasing)) }
                    }
                }
            )
        }
        .pointerInput(verticalFeed) {
            if (verticalFeed) {
                var startPos = 0L
                var dx = 0f
                var target = 0L
                detectHorizontalDragGestures(
                    onDragStart = {
                        if (currentIsVideo() && scale <= 1.01f) {
                            scrubbing = true
                            startPos = players[activeSlot].currentPosition
                            dx = 0f
                            target = startPos
                        }
                    },
                    onDragEnd = {
                        if (scrubbing) {
                            players[activeSlot].seekTo(target)
                            scrubbing = false
                        }
                    },
                    onDragCancel = { scrubbing = false },
                    onHorizontalDrag = { change, amount ->
                        if (scrubbing) {
                            change.consume()
                            dx += amount
                            val dur = players[activeSlot].duration.coerceAtLeast(1L)
                            val span = minOf(dur, 120_000L)
                            target = (startPos + (dx / viewSize.width.coerceAtLeast(1)) * span).toLong().coerceIn(0L, dur)
                            scrubText = formatDuration(target) + " / " + formatDuration(dur)
                        }
                    }
                )
            }
        }
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                zoomJob?.cancel()
                var multi = false
                var handled = false
                val tracker = VelocityTracker()
                do {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val pressed = event.changes.count { it.pressed }
                    if (pressed >= 2) multi = true
                    val centroid = event.calculateCentroid(useCurrent = true)
                    if ((multi || scale > 1.01f) && pressed >= 1 && centroid.isSpecified) {
                        handled = true
                        val zoom = event.calculateZoom()
                        val pan = event.calculatePan()
                        val w = viewSize.width.toFloat()
                        val h = viewSize.height.toFloat()
                        val center = Offset(w / 2f, h / 2f)
                        val old = scale
                        val ns = (old * zoom).coerceIn(0.75f, 6f)
                        // el punto bajo los dedos se mantiene bajo los dedos
                        val contentPoint = (centroid - center - offset) / old
                        val raw = centroid + pan - center - contentPoint * ns
                        val bx = w * maxOf(ns - 1f, 0f) / 2f
                        val by = h * maxOf(ns - 1f, 0f) / 2f
                        scale = ns
                        offset = if (ns <= 1f) Offset.Zero else Offset(soft(raw.x, -bx, bx), soft(raw.y, -by, by))
                        if (pressed == 1) {
                            tracker.addPosition(event.changes[0].uptimeMillis, event.changes[0].position)
                        }
                        event.changes.forEach { if (it.positionChanged()) it.consume() }
                    }
                } while (event.changes.any { it.pressed })
                if (handled) {
                    val v = tracker.calculateVelocity()
                    settleZoom(Offset(v.x, v.y))
                }
            }
        }

    // Pantalla retro: el efecto se aplica a todo el feed (curvatura real en Android 13+)
    val crtMod = if (tvMode != 0 && crt != null) {
        Modifier.graphicsLayer {
            if (Build.VERSION.SDK_INT >= 33) {
                renderEffect = crt.effect(size.width, size.height, tvTime, tvMode == 2, tvPower.value, !playing)
            }
        }
    } else {
        Modifier
    }

    // ---- UI ----
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val scrollEnabled = !zoomLocked && !inPip
        if (verticalFeed) {
            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize().then(crtMod).then(gestureModifier),
                beyondViewportPageCount = 1,
                userScrollEnabled = scrollEnabled,
                key = { order[it] }
            ) { pg -> pageContent(pg) }
        } else {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize().then(crtMod).then(gestureModifier),
                beyondViewportPageCount = 1,
                userScrollEnabled = scrollEnabled,
                key = { order[it] }
            ) { pg -> pageContent(pg) }
        }

        val uiPage = pagerState.currentPage.coerceIn(0, order.size - 1)
        val item = itemAt(uiPage)
        val isFavorite = item.uri in favorites
        val canDelete = Uri.parse(item.uri).authority == MediaStore.AUTHORITY
        // Zonas seguras: respeta la cámara frontal y los bordes aunque las barras del sistema estén ocultas
        val topInsets = WindowInsets.statusBarsIgnoringVisibility
            .union(WindowInsets.displayCutout)
            .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
        val bottomInsets = WindowInsets.navigationBarsIgnoringVisibility
            .union(WindowInsets.displayCutout)
            .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)

        if (tvMode != 0 && crt == null && !inPip) CrtOverlay()
        if (tvMode == 2 && !inPip) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(topInsets)
                    .padding(horizontal = 28.dp, vertical = 52.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (playing) "▶ PLAY" else "❚❚ PAUSE",
                    color = Color(0xFFE6FFE6),
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "SP  " + formatDuration(osdSeconds * 1000),
                    color = Color(0xFFE6FFE6),
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
        if (staticOn && !inPip) StaticNoise()

        if (hint.isNotEmpty() || scrubbing) {
            Text(
                text = if (scrubbing) scrubText else hint,
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color(0x99000000), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        AnimatedVisibility(
            visible = speedBoost && !inPip,
            modifier = Modifier.align(Alignment.TopCenter).windowInsetsPadding(topInsets).padding(top = 72.dp)
        ) {
            Text(
                text = "2×  ▶▶",
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .background(Color(0x99000000), RoundedCornerShape(50))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }

        AnimatedVisibility(
            visible = zoomed && !inPip,
            modifier = Modifier.align(Alignment.TopCenter).windowInsetsPadding(topInsets).padding(top = 72.dp)
        ) {
            Text(
                text = "Restablecer zoom",
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .background(Color(0x99000000), RoundedCornerShape(50))
                    .clickable { resetZoom() }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }

        // ---- Barra superior ----
        AnimatedVisibility(
            visible = controlsVisible && !inPip,
            modifier = Modifier.align(Alignment.TopCenter),
            enter = fadeIn(tween(180)) + slideInVertically(tween(220)) { -it / 3 },
            exit = fadeOut(tween(140)) + slideOutVertically(tween(180)) { -it / 3 }
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color(0x99000000), Color.Transparent)))
                    .windowInsetsPadding(topInsets)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GlassIconButton(MixIcons.ArrowBack, "Cerrar", onClick = onClose)
                Column(Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "${uiPage + 1} de ${order.size}",
                        color = Color.White.copy(alpha = 0.75f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                if (item.isVideo) {
                    GlassIconButton(
                        icon = if (muted) MixIcons.VolumeOff else MixIcons.VolumeUp,
                        description = if (muted) "Activar sonido" else "Silenciar",
                        onClick = {
                            muted = !muted
                            prefs.muted = muted
                            touchStamp++
                        }
                    )
                }
                GlassIconButton(MixIcons.Tune, "Ajustes") {
                    showOptions = !showOptions
                    touchStamp++
                }
                Box {
                    GlassIconButton(MixIcons.MoreVert, "Más opciones") {
                        menuOpen = true
                        touchStamp++
                    }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false },
                        shape = RoundedCornerShape(18.dp),
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        DropdownMenuItem(
                            text = { Text("Compartir") },
                            leadingIcon = { Icon(MixIcons.Share, contentDescription = null, modifier = Modifier.size(20.dp)) },
                            onClick = {
                                menuOpen = false
                                shareUris(
                                    context,
                                    listOf(Uri.parse(item.uri)),
                                    allImages = !item.isVideo,
                                    allVideos = item.isVideo
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Agregar a lista") },
                            leadingIcon = { Icon(MixIcons.PlaylistAdd, contentDescription = null, modifier = Modifier.size(20.dp)) },
                            onClick = {
                                menuOpen = false
                                showAddList = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Información") },
                            leadingIcon = { Icon(MixIcons.Info, contentDescription = null, modifier = Modifier.size(20.dp)) },
                            onClick = {
                                menuOpen = false
                                showInfo = true
                            }
                        )
                        if (item.isVideo) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            DropdownMenuItem(
                                text = { Text(if (forcedLandscape) "Pantalla vertical" else "Pantalla horizontal") },
                                leadingIcon = { Icon(MixIcons.ScreenRotation, contentDescription = null, modifier = Modifier.size(20.dp)) },
                                onClick = {
                                    menuOpen = false
                                    toggleRotation()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Imagen en imagen") },
                                leadingIcon = { Icon(MixIcons.PictureInPictureAlt, contentDescription = null, modifier = Modifier.size(20.dp)) },
                                onClick = {
                                    menuOpen = false
                                    (context.findActivity() as? MainActivity)?.enterPip(playing)
                                }
                            )
                        }
                    }
                }
            }
        }

        // ---- Barra inferior: una sola pieza translúcida ----
        AnimatedVisibility(
            visible = controlsVisible && !inPip && !showOptions,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn(tween(180)) + slideInVertically(tween(220)) { it / 3 },
            exit = fadeOut(tween(140)) + slideOutVertically(tween(180)) { it / 3 }
        ) {
            val glass = RoundedCornerShape(30.dp)
            Column(
                Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(bottomInsets)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .clip(glass)
                    .background(Color(0xB3000000))
                    .border(0.5.dp, Color(0x22FFFFFF), glass)
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
                } else if (!feedMode) {
                    LinearProgressIndicator(
                        progress = { imgProgress.value },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 4.dp)
                    )
                } else {
                    Spacer(Modifier.height(6.dp))
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
                        vm.toggleFavoriteKey(item.uri)
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        touchStamp++
                    }) {
                        Icon(
                            imageVector = if (isFavorite) MixIcons.FavoriteFilled else MixIcons.Favorite,
                            contentDescription = if (isFavorite) "Quitar de favoritos" else "Agregar a favoritos",
                            tint = if (isFavorite) MixColors.Pink else Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    IconButton(
                        onClick = {
                            togglePlay()
                            touchStamp++
                        },
                        modifier = Modifier.size(60.dp).background(MixColors.Pink, CircleShape)
                    ) {
                        Icon(
                            imageVector = when {
                                finished -> MixIcons.Replay
                                playing -> MixIcons.Pause
                                else -> MixIcons.PlayArrow
                            },
                            contentDescription = "Reproducir / pausar",
                            tint = MixColors.Black,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    if (canDelete) {
                        IconButton(onClick = {
                            confirmDelete = true
                            touchStamp++
                        }) {
                            Icon(MixIcons.Delete, contentDescription = "Eliminar", tint = Color.White, modifier = Modifier.size(26.dp))
                        }
                    } else {
                        Spacer(Modifier.size(48.dp))
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

        if (confirmDelete) {
            DeleteDialog(
                count = 1,
                name = item.name,
                onTrash = {
                    confirmDelete = false
                    remover(listOf(Uri.parse(item.uri)), false)
                },
                onDelete = {
                    confirmDelete = false
                    remover(listOf(Uri.parse(item.uri)), true)
                },
                onDismiss = { confirmDelete = false }
            )
        }

        if (showAddList) {
            AddToPlaylistDialog(
                playlists = playlists,
                onPick = { p ->
                    vm.addToPlaylist(p.playlist.id, listOf(item))
                    showAddList = false
                    Toast.makeText(context, "Agregado a \"${p.playlist.name}\"", Toast.LENGTH_SHORT).show()
                },
                onCreate = { name ->
                    vm.createPlaylist(name, listOf(item))
                    showAddList = false
                    Toast.makeText(context, "Lista \"$name\" creada", Toast.LENGTH_SHORT).show()
                },
                onDismiss = { showAddList = false }
            )
        }

        if (showInfo) {
            InfoSheet(item = item, file = vm.mediaFor(item.uri), onDismiss = { showInfo = false })
        }

        // ---- Panel de ajustes con vista previa en vivo (el contenido queda visible arriba) ----
        BackHandler(enabled = showOptions) { showOptions = false }
        val panelMax = (LocalConfiguration.current.screenHeightDp * 0.46f).dp
        AnimatedVisibility(
            visible = showOptions && !inPip,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(tween(240)) { it } + fadeIn(tween(200)),
            exit = slideOutVertically(tween(200)) { it } + fadeOut(tween(160))
        ) {
            val panelShape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = panelMax)
                    .clip(panelShape)
                    .background(Color(0xF2070708))
                    .border(0.5.dp, Color(0x22FFFFFF), panelShape)
                    .windowInsetsPadding(bottomInsets)
            ) {
                Box(
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 8.dp)
                        .size(width = 36.dp, height = 4.dp)
                        .background(Color(0x44FFFFFF), RoundedCornerShape(2.dp))
                )
                CompositionLocalProvider(LocalContentColor provides MixColors.Bone) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Filtros", "Vista", "Reproducción", "Estilos").forEachIndexed { i, label ->
                        val sel = optionsTab == i
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (sel) MixColors.Pink else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (sel) MixColors.Pink.copy(alpha = 0.16f) else Color.Transparent)
                                .clickable { optionsTab = i }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
                Column(
                    Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    when (optionsTab) {
                        0 -> {
                            val previewRequest = remember(item.uri) {
                                ImageRequest.Builder(context)
                                    .data(MediaThumb(Uri.parse(item.uri), item.isVideo))
                                    .size(200)
                                    .build()
                            }
                            Text("Tocá un filtro: se aplica en vivo", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(ColorFilterType.entries.toList()) { type ->
                                    FilterCard(
                                        type = type,
                                        selected = type == filterType,
                                        request = previewRequest,
                                        onClick = {
                                            filterType = type
                                            prefs.colorFilter = type.name
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    )
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                        }
                        1 -> {
                            ChipRow(
                                title = "Espejo",
                                options = listOf(0, 1, 2, 3),
                                selected = mirror,
                                label = {
                                    when (it) {
                                        1 -> "Horizontal"
                                        2 -> "Vertical"
                                        3 -> "Ambos"
                                        else -> "Apagado"
                                    }
                                },
                                onSelect = {
                                    mirror = it
                                    prefs.mirror = it
                                }
                            )
                            ChipRow(
                                title = "Pantalla retro",
                                options = listOf(0, 1, 2),
                                selected = tvMode,
                                label = {
                                    when (it) {
                                        1 -> "TV de tubo"
                                        2 -> "VHS"
                                        else -> "Apagado"
                                    }
                                },
                                onSelect = {
                                    val wasOff = tvMode == 0
                                    tvMode = it
                                    prefs.tvMode = it
                                    if (wasOff && it != 0) {
                                        scope.launch {
                                            tvPower.snapTo(0f)
                                            tvPower.animateTo(1f, tween(480))
                                        }
                                    }
                                }
                            )
                            if (tvMode != 0) {
                                OptionSwitch(
                                    title = "Estática al cambiar de archivo",
                                    subtitle = "Efecto de cambio de canal entre un archivo y otro.",
                                    checked = tvStatic,
                                    onChange = {
                                        tvStatic = it
                                        prefs.tvStatic = it
                                    }
                                )
                            }
                            OptionSwitch(
                                title = "Ambiente",
                                subtitle = "Fondo difuminado con los colores del contenido (no se usa con pantalla retro).",
                                checked = ambient,
                                onChange = {
                                    ambient = it
                                    prefs.ambient = it
                                }
                            )
                            OptionSwitch(
                                title = "Efecto Ken Burns",
                                subtitle = "Movimiento lento en las imágenes de la presentación. Se pausa con el zoom.",
                                checked = kenBurns,
                                onChange = {
                                    kenBurns = it
                                    prefs.kenBurns = it
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
                        }
                        2 -> {
                            OptionSwitch(
                                title = "Modo feed",
                                subtitle = "Sin avance automático: las imágenes se quedan hasta que deslizás y los videos se repiten en bucle.",
                                checked = feedMode,
                                onChange = {
                                    feedMode = it
                                    prefs.feedMode = it
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
                            OptionSwitch(
                                title = "Seguir reproduciendo en segundo plano",
                                subtitle = "Solo videos. Muestra una notificación mientras suena.",
                                checked = bgPlayback,
                                onChange = { value ->
                                    bgPlayback = value
                                    prefs.bgPlayback = value
                                    if (value && Build.VERSION.SDK_INT >= 33) {
                                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                }
                            )
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
                            Spacer(Modifier.height(8.dp))
                        }
                        else -> {
                            OutlinedButton(onClick = { showSaveStyle = true }) { Text("Guardar estilo actual") }
                            Text("Sugeridos", style = MaterialTheme.typography.titleSmall)
                            PRESET_STYLES.forEach { st ->
                                StyleRow(st, onApply = { applyStyle(st) }, onDelete = null)
                            }
                            if (vm.styles.isNotEmpty()) {
                                Text("Mis estilos", style = MaterialTheme.typography.titleSmall)
                                vm.styles.forEach { st ->
                                    StyleRow(st, onApply = { applyStyle(st) }, onDelete = { vm.deleteStyle(st.name) })
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
                }
            }
        }

        if (showSaveStyle) {
            TextInputDialog(
                title = "Guardar estilo",
                confirmLabel = "Guardar",
                onConfirm = { name ->
                    vm.saveStyle(VisualStyle(name, filterType.name, mirror, tvMode, tvStatic, ambient, kenBurns))
                    showSaveStyle = false
                    Toast.makeText(context, "Estilo \"$name\" guardado", Toast.LENGTH_SHORT).show()
                },
                onDismiss = { showSaveStyle = false }
            )
        }

        // ---- Presentación del recuerdo ----
        AnimatedVisibility(
            visible = showIntro && introMemory != null && !inPip,
            enter = fadeIn(tween(250)),
            exit = fadeOut(tween(600))
        ) {
            var started by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { started = true }
            val zoomIntro by animateFloatAsState(if (started) 1.12f else 1f, tween(3000), label = "introZoom")
            val coverRequest = remember(introMemory?.cover?.key) {
                ImageRequest.Builder(context)
                    .data(introMemory?.cover?.uri)
                    .size(1080)
                    .apply { if (introMemory?.cover?.isVideo == true) videoFramePercent(0.1) }
                    .build()
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .pointerInput(Unit) {
                        detectTapGestures {
                            showIntro = false
                            playing = true
                        }
                    }
            ) {
                AsyncImage(
                    model = coverRequest,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().graphicsLayer {
                        scaleX = zoomIntro
                        scaleY = zoomIntro
                    }
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(listOf(Color(0x55000000), Color(0xCC000000))))
                )
                Column(
                    Modifier.align(Alignment.Center).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = introMemory?.title ?: "",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MixColors.Bone,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Text(
                        text = introMemory?.subtitle ?: "",
                        style = MaterialTheme.typography.titleMedium,
                        color = MixColors.PinkSoft,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        // ---- Consejos de gestos (solo la primera vez) ----
        AnimatedVisibility(
            visible = showTips && !inPip,
            enter = fadeIn(tween(220)),
            exit = fadeOut(tween(180))
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0xE6000000))
                    .clickable {
                        showTips = false
                        prefs.tipsSeen = true
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    Modifier.padding(32.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Gestos del reproductor", style = MaterialTheme.typography.titleLarge, color = MixColors.Bone)
                    listOf(
                        "Deslizá para cambiar de archivo",
                        "Pellizcá para ampliar; doble toque para hacer zoom en imágenes",
                        "Doble toque en un video: salta 10 s hacia cada lado",
                        "Mantené presionado un video: velocidad 2×",
                        "Arrastrá de costado sobre un video: adelantar o retroceder",
                        "Tocá la pantalla: muestra u oculta los controles"
                    ).forEach {
                        Text("•  $it", style = MaterialTheme.typography.bodyLarge, color = MixColors.Bone)
                    }
                    Text("Tocá para continuar", style = MaterialTheme.typography.labelLarge, color = MixColors.PinkSoft)
                }
            }
        }
    }
}

@Composable
private fun GlassIconButton(
    icon: ImageVector,
    description: String,
    tint: Color = Color.White,
    size: Dp = 48.dp,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(size).background(Color(0x66000000), CircleShape)
    ) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun FilterCard(type: ColorFilterType, selected: Boolean, request: ImageRequest, onClick: () -> Unit) {
    val cf = remember(type) { type.values?.let { ColorFilter.colorMatrix(ComposeColorMatrix(it)) } }
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier.width(78.dp).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(78.dp)
                .clip(shape)
                .border(if (selected) 2.dp else 0.5.dp, if (selected) MixColors.Pink else Color(0x33FFFFFF), shape)
        ) {
            AsyncImage(
                model = request,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                colorFilter = cf,
                modifier = Modifier.fillMaxSize()
            )
            Box(Modifier.align(Alignment.BottomEnd).padding(5.dp)) { SwatchDot(type, 14.dp) }
        }
        Text(
            text = type.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MixColors.Pink else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

/** Bordes suaves entre la imagen y el fondo difuminado (modo Ambiente). */
private fun featherModifier(img: Size): Modifier = Modifier
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        if (img.width > 0f && img.height > 0f) {
            val sc = minOf(size.width / img.width, size.height / img.height)
            val w = img.width * sc
            val h = img.height * sc
            val l = (size.width - w) / 2f
            val t = (size.height - h) / 2f
            val f = 18.dp.toPx()
            if (t > 1f) {
                drawRect(
                    Brush.verticalGradient(listOf(Color.Black, Color.Transparent), startY = t, endY = t + f),
                    topLeft = Offset(l, t),
                    size = Size(w, f),
                    blendMode = BlendMode.DstOut
                )
                drawRect(
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Black), startY = t + h - f, endY = t + h),
                    topLeft = Offset(l, t + h - f),
                    size = Size(w, f),
                    blendMode = BlendMode.DstOut
                )
            }
            if (l > 1f) {
                drawRect(
                    Brush.horizontalGradient(listOf(Color.Black, Color.Transparent), startX = l, endX = l + f),
                    topLeft = Offset(l, t),
                    size = Size(f, h),
                    blendMode = BlendMode.DstOut
                )
                drawRect(
                    Brush.horizontalGradient(listOf(Color.Transparent, Color.Black), startX = l + w - f, endX = l + w),
                    topLeft = Offset(l + w - f, t),
                    size = Size(f, h),
                    blendMode = BlendMode.DstOut
                )
            }
        }
    }

private fun itemAt0(queue: List<QueueItem>, order: List<Int>, page: Int): QueueItem =
    queue[order[page.coerceIn(0, order.size - 1)]]

@Composable
private fun OptionSwitch(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InfoSheet(item: QueueItem, file: MediaFile?, onDismiss: () -> Unit) {
    val context = LocalContext.current
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(item.name, style = MaterialTheme.typography.titleMedium)
            InfoRow("Tipo", if (item.isVideo) "Video" else "Imagen")
            if (file != null) {
                InfoRow("Carpeta", file.folder)
                if (file.size > 0) InfoRow("Tamaño", Formatter.formatFileSize(context, file.size))
                if (file.width > 0 && file.height > 0) InfoRow("Resolución", "${file.width} × ${file.height}")
                if (file.dateTaken > 0) {
                    val fmt = SimpleDateFormat("d 'de' MMMM 'de' yyyy, HH:mm", Locale.forLanguageTag("es-AR"))
                    InfoRow(
                        if (file.realDate) "Fecha de toma" else "Fecha de modificación",
                        fmt.format(Date(file.dateTaken))
                    )
                }
            }
            val duration = if (file != null && file.durationMs > 0) file.durationMs else item.durationMs
            if (item.isVideo && duration > 0) InfoRow("Duración", formatDuration(duration))
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
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
    wrap: Boolean = false,
    leading: (@Composable (T) -> Unit)? = null
) {
    val chips: @Composable () -> Unit = {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(label(option)) },
                leadingIcon = if (leading != null) {
                    { leading(option) }
                } else {
                    null
                }
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

/** Color representativo de cada filtro, para el círculo junto al nombre. */
private fun swatchOf(t: ColorFilterType): Long = when (t) {
    ColorFilterType.NONE -> 0xFF9A959C
    ColorFilterType.SEPIA -> 0xFFB5835A
    ColorFilterType.BW -> 0xFFB0B0B0
    ColorFilterType.KODAK -> 0xFFE8A93A
    ColorFilterType.POLAROID -> 0xFFD9C7B0
    ColorFilterType.EIGHTIES -> 0xFFD94FB0
    ColorFilterType.VINTAGE -> 0xFFC9A66B
    ColorFilterType.GOLDEN -> 0xFFFFA630
    ColorFilterType.CINEMA -> 0xFF3E8E9C
    ColorFilterType.NEON -> 0xFF8A4DFF
    ColorFilterType.LOMO -> 0xFF8FB339
    ColorFilterType.FADED -> 0xFFB59CA6
    ColorFilterType.NOIR -> 0xFF3A3A3F
    ColorFilterType.ROSE -> 0xFFFF7FAF
    ColorFilterType.WARM -> 0xFFF0A05A
    ColorFilterType.COOL -> 0xFF6EC1E4
    ColorFilterType.BLUE -> 0xFF3D6BFF
    ColorFilterType.VIVID -> 0xFFFF3D6E
    ColorFilterType.NIGHT -> 0xFF8B1E2B
}

@Composable
private fun SwatchDot(type: ColorFilterType, size: Dp = 16.dp) {
    val base = Color(swatchOf(type))
    val shape = androidx.compose.foundation.shape.CircleShape
    if (type == ColorFilterType.NONE) {
        Box(
            Modifier
                .size(size)
                .border(1.5.dp, base, shape)
        )
    } else {
        Box(
            Modifier
                .size(size)
                .clip(shape)
                .background(Brush.linearGradient(listOf(Color.White.copy(alpha = 0.55f).compositeOver(base), base)))
                .border(0.5.dp, Color.White.copy(alpha = 0.25f), shape)
        )
    }
}

/** Alternativa para Android anteriores al 13: líneas de barrido y viñeta. */
@Composable
private fun CrtOverlay() {
    Canvas(Modifier.fillMaxSize()) {
        val step = 4.dp.toPx()
        var y = 0f
        while (y < size.height) {
            drawLine(
                color = Color.Black.copy(alpha = 0.22f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = step / 2f
            )
            y += step
        }
        drawRect(
            Brush.radialGradient(
                colors = listOf(Color.Transparent, Color(0xAA000000)),
                center = center,
                radius = size.maxDimension * 0.75f
            )
        )
    }
}

/** Estática breve de "cambio de canal". */
@Composable
private fun StaticNoise() {
    var seed by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            seed++
            delay(45)
        }
    }
    Canvas(Modifier.fillMaxSize()) {
        val rnd = kotlin.random.Random(seed)
        drawRect(Color(0xFF101010))
        repeat(900) {
            val x = rnd.nextFloat() * size.width
            val y = rnd.nextFloat() * size.height
            val g = rnd.nextFloat()
            drawRect(
                color = Color(g, g, g, 1f),
                topLeft = Offset(x, y),
                size = Size(rnd.nextFloat() * 40f + 4f, 3f)
            )
        }
    }
}

private fun styleSummary(st: VisualStyle): String {
    val parts = ArrayList<String>()
    val f = try {
        ColorFilterType.valueOf(st.filter)
    } catch (e: Exception) {
        ColorFilterType.NONE
    }
    if (f != ColorFilterType.NONE) parts += f.label
    if (st.mirror != 0) parts += "Espejo"
    if (st.tvMode == 1) parts += "TV de tubo" else if (st.tvMode == 2) parts += "VHS"
    if (st.ambient) parts += "Ambiente"
    if (st.kenBurns) parts += "Ken Burns"
    return if (parts.isEmpty()) "Sin efectos" else parts.joinToString(" · ")
}

@Composable
private fun StyleRow(style: VisualStyle, onApply: () -> Unit, onDelete: (() -> Unit)?) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x14FFFFFF))
            .clickable(onClick = onApply)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(style.name, style = MaterialTheme.typography.titleSmall)
            Text(
                styleSummary(style),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (onDelete != null) {
            IconButton(onClick = onDelete) {
                Icon(MixIcons.Delete, contentDescription = "Eliminar estilo", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
