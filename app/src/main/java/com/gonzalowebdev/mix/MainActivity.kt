package com.gonzalowebdev.mix

import android.app.PictureInPictureParams
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.gonzalowebdev.mix.ui.MixRoot
import com.gonzalowebdev.mix.ui.MixTheme
import com.gonzalowebdev.mix.ui.MixViewModel

class MainActivity : ComponentActivity() {

    private val vm: MixViewModel by viewModels()
    private var externalUri by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(i: Intent?) {
        if (i?.action == Intent.ACTION_VIEW) externalUri = i.data
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (vm.pipReady) enterPip()
    }

    fun enterPip() {
        try {
            enterPictureInPictureMode(
                PictureInPictureParams.Builder().setAspectRatio(Rational(16, 9)).build()
            )
        } catch (e: Exception) {
            // El dispositivo no admite imagen en imagen
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        vm.inPip = isInPictureInPictureMode
    }
}
