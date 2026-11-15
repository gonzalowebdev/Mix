package com.gonzalowebdev.mix

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.gonzalowebdev.mix.ui.MixRoot
import com.gonzalowebdev.mix.ui.MixTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MixTheme {
                MixRoot()
            }
        }
    }
}
