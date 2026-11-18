package com.gonzalowebdev.mix

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.gonzalowebdev.mix.ui.MixRoot
import com.gonzalowebdev.mix.ui.MixTheme
import com.gonzalowebdev.mix.ui.MixViewModel

class MainActivity : ComponentActivity() {

    private val vm: MixViewModel by viewModels()
    private var externalUri by mutableStateOf<Uri?>(null)

    private val pipReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_PIP_TOGGLE) vm.pipToggleCount++
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        ContextCompat.registerReceiver(
            this,
            pipReceiver,
            IntentFilter(ACTION_PIP_TOGGLE),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        if (savedInstanceState == null) handleIntent(intent)
        setContent {
            MixTheme {
                MixRoot(
                    vm = vm,
                    externalUri = externalUri,
                    onExternalHandled = { externalUri = null }
                )
            }
        }
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(pipReceiver)
        } catch (e: Exception) {
            // ya estaba desregistrado
        }
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(i: Intent?) {
        if (i?.action == Intent.ACTION_VIEW) externalUri = i.data
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (vm.pipReady) enterPip(vm.pipPlaying)
    }

    private fun pipParams(playing: Boolean): PictureInPictureParams {
        val action = RemoteAction(
            Icon.createWithResource(this, if (playing) R.drawable.ic_pip_pause else R.drawable.ic_pip_play),
            if (playing) "Pausar" else "Reproducir",
            if (playing) "Pausar" else "Reproducir",
            PendingIntent.getBroadcast(
                this,
                0,
                Intent(ACTION_PIP_TOGGLE).setPackage(packageName),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        )
        return PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
            .setActions(listOf(action))
            .build()
    }

    fun enterPip(playing: Boolean = true) {
        try {
            enterPictureInPictureMode(pipParams(playing))
        } catch (e: Exception) {
            // El dispositivo no admite imagen en imagen
        }
    }

    /** Actualiza el botón de pausa/reproducir de la ventana flotante. */
    fun updatePipActions(playing: Boolean) {
        if (isInPictureInPictureMode) {
            try {
                setPictureInPictureParams(pipParams(playing))
            } catch (e: Exception) {
                // sin cambios
            }
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        vm.inPip = isInPictureInPictureMode
    }

    private companion object {
        const val ACTION_PIP_TOGGLE = "com.gonzalowebdev.mix.PIP_TOGGLE"
    }
}
