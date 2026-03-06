package com.zixion.mangaverse

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.zixion.mangaverse.network.AndroidContext // <--- IMPORTANTE
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    // 1. Preparamos un "lanzador" para pedir el permiso de notificaciones (Android 13+)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Tanto si nos da permiso como si no, programamos la tarea en segundo plano.
        programarActualizacionEnSegundoPlano()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- LÍNEA NUEVA OBLIGATORIA (Mantenida tal cual la tenías) ---
        AndroidContext.context = applicationContext

        // 2. Gestionamos los permisos de Android 13+ y arrancamos el Worker
        gestionarPermisos()

        setContent {
            App()
        }
    }

    private fun gestionarPermisos() {
        // Comprobamos si el móvil tiene Android 13 (Tiramisu) o superior
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                // Si ya nos dio permiso antes, programamos el trabajo
                programarActualizacionEnSegundoPlano()
            } else {
                // Si no tenemos permiso, lanzamos la ventanita para pedirlo
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            // Si es Android 12 o inferior, el permiso viene dado por defecto
            programarActualizacionEnSegundoPlano()
        }
    }

    private fun programarActualizacionEnSegundoPlano() {
        // Que solo se ejecute si hay internet
        val restricciones = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Repetir cada 15 minutos (el mínimo de Android)
        val peticionTrabajo = PeriodicWorkRequestBuilder<CacheUpdateWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(restricciones)
            .build()

        // Encolamos el trabajo de forma única (KEEP) para no duplicarlo
        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "MangaCacheUpdateWork",
            ExistingPeriodicWorkPolicy.KEEP,
            peticionTrabajo
        )
    }
}