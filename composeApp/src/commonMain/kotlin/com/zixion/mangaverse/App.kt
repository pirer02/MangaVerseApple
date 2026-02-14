package com.zixion.mangaverse

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import com.zixion.mangaverse.ui.screens.HomeScreen
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.request.crossfade
import coil3.util.DebugLogger
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import okio.FileSystem

@Composable
fun App() {
    // --- CONFIGURACIÓN GLOBAL DE IMÁGENES (OPTIMIZADA) ---
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            // 1. Usar Dispatchers.IO para evitar bloquear la UI
            .dispatcher(Dispatchers.IO)
            // 2. Configuración de memoria eficiente
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.25) // 25% de RAM máx
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "image_cache")
                    .maxSizeBytes(512L * 1024 * 1024) // 512MB
                    .build()
            }
            // 3. (Solo Android) RGB_565 ahorra 50% de RAM.
            // Kotlin Multiplatform a veces necesita configuración específica,
            // pero Coil intentará optimizarlo por defecto.
            .crossfade(true)
            // .logger(DebugLogger()) // Descomenta para ver errores en Logcat
            .build()
    }

    MaterialTheme {
        Navigator(screen = HomeScreen())
    }
}