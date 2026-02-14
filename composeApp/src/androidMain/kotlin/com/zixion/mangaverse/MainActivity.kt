package com.zixion.mangaverse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.zixion.mangaverse.network.AndroidContext // <--- IMPORTANTE

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- LÍNEA NUEVA OBLIGATORIA ---
        AndroidContext.context = applicationContext

        setContent {
            App()
        }
    }
}