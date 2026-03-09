package com.zixion.mangaverse

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.zixion.mangaverse.network.MangaUpdateChecker

class CacheUpdateWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // 1. Llamamos al cerebro compartido que ahora vive en commonMain
            val novedades = MangaUpdateChecker.buscarNovedades()

            // 2. Si encontró algo, mandamos la notificación nativa de Android
            if (novedades.isNotEmpty()) {
                mostrarNotificacion(novedades)
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }

    private fun mostrarNotificacion(novedades: List<String>) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) { return }

        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            appContext, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Textos dinámicos dependiendo de si hay 1 o varios mangas actualizados
        val titulo = if (novedades.size == 1) "¡Nuevo capítulo!" else "¡Actualizaciones de Biblioteca!"
        val textoResumen = if (novedades.size == 1) novedades.first() else "Tienes ${novedades.size} mangas con nuevos capítulos."

        val builder = NotificationCompat.Builder(appContext, MangaApplication.CANAL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(titulo)
            .setContentText(textoResumen)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        // ESTILO INBOX: Permite expandir la notificación
        val inboxStyle = NotificationCompat.InboxStyle()
        novedades.forEach { novedad ->
            inboxStyle.addLine(novedad)
        }
        builder.setStyle(inboxStyle)

        with(NotificationManagerCompat.from(appContext)) {
            notify(1001, builder.build())
        }
    }
}