package com.zixion.mangaverse

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.zixion.mangaverse.network.AndroidContext // <-- Asegúrate de importar esto

class MangaApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // ¡LA CLAVE ESTÁ AQUÍ!
        // Inicializamos tu contexto global aquí para que exista aunque la app esté cerrada.
        AndroidContext.context = this

        crearCanalNotificaciones()
    }

    private fun crearCanalNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nombre = "Actualizaciones de Caché"
            val descripcion = "Notifica cuando se actualiza el catálogo en segundo plano"
            val importancia = NotificationManager.IMPORTANCE_DEFAULT

            val canal = NotificationChannel(CANAL_ID, nombre, importancia).apply {
                description = descripcion
            }

            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            notificationManager.createNotificationChannel(canal)
        }
    }

    companion object {
        const val CANAL_ID = "mangaverse_cache_channel"
    }
}