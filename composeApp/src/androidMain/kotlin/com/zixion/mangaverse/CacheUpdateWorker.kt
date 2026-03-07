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
import com.zixion.mangaverse.network.MangaService
import com.zixion.mangaverse.network.UserManager
import com.zixion.mangaverse.network.ZipHelper
import kotlinx.serialization.json.Json

class CacheUpdateWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    // Instanciamos el serializador JSON para leer las cachés antiguas
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun doWork(): Result {
        return try {
            UserManager.cargar()
            val servicio = MangaService()

            // 1. Obtenemos solo los mangas que el usuario tiene en "Mi Biblioteca"
            val miBiblioteca = UserManager.getBiblioteca()

            // Aquí guardaremos los mensajes para la notificación (Ej: "One Piece: 1070, 1071")
            val mangasConNovedades = mutableListOf<String>()

            // 2. Comprobamos manga por manga de la biblioteca
            for (mangaTitulo in miBiblioteca) {
                val idManga = mangaTitulo.replace(" ", "_")

                // A) Leemos lo que teníamos guardado LOCALMENTE (sin usar Internet)
                val textoCacheVieja = ZipHelper.leerTexto("caps_${idManga}_normal.json")
                val capsViejos: List<String> = if (textoCacheVieja != null) {
                    try { json.decodeFromString(textoCacheVieja) } catch (e: Exception) { emptyList() }
                } else emptyList()

                // B) Forzamos a descargar la lista ACTUALIZADA desde tu servidor DuckDNS
                val capsNuevos = servicio.obtenerCapitulos(mangaTitulo, isColor = false, forceRefresh = true)

                // C) Comparamos: ¿Qué capítulos están en los Nuevos que NO están en los Viejos?
                val capitulosEstreno = capsNuevos.filter { it !in capsViejos }

                // Solo notificamos si antes YA teníamos capítulos (para que no avise de los 160 de golpe
                // cuando añades un manga por primera vez) y si hay capítulos de estreno.
                if (capsViejos.isNotEmpty() && capitulosEstreno.isNotEmpty()) {
                    // Limpiamos el texto (quitamos el .cbz para que quede bonito en la notificación)
                    val nombresLimpios = capitulosEstreno.map { it.replace(".cbz", "").replace(".zip", "") }
                    mangasConNovedades.add("$mangaTitulo: ${nombresLimpios.joinToString(", ")}")
                }
            }

            // 3. Ya hemos comprobado la biblioteca. Ahora actualizamos el catálogo general de la app
            UserManager.forzarExpiracionCache()
            servicio.obtenerMangas()

            // 4. Si encontramos novedades, lanzamos la notificación inteligente
            if (mangasConNovedades.isNotEmpty()) {
                mostrarNotificacion(mangasConNovedades)
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
            .setSmallIcon(R.mipmap.ic_launcher)            .setContentTitle(titulo)
            .setContentText(textoResumen)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        // ESTILO INBOX: Permite expandir la notificación para ver la lista de todos los mangas y capítulos
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