package com.gonzalowebdev.mix.ui

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer

/**
 * Crea un ExoPlayer con buffers chicos: los archivos son locales, así que no hace falta
 * esperar segundos de buffer para empezar (eso era el pequeño lag al iniciar cada video).
 */
@androidx.annotation.OptIn(UnstableApi::class)
fun newExoPlayer(context: Context, handleAudioFocus: Boolean, preview: Boolean = false): ExoPlayer {
    val load = DefaultLoadControl.Builder()
        .setBufferDurationsMs(
            if (preview) 1_500 else 2_000,
            if (preview) 6_000 else 15_000,
            150,
            300
        )
        .build()
    return ExoPlayer.Builder(context)
        .setLoadControl(load)
        .setAudioAttributes(AudioAttributes.DEFAULT, handleAudioFocus)
        .setHandleAudioBecomingNoisy(!preview)
        .setWakeMode(if (preview) C.WAKE_MODE_NONE else C.WAKE_MODE_LOCAL)
        .build()
}
