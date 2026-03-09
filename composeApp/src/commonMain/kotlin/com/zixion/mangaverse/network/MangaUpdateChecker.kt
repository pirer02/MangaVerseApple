package com.zixion.mangaverse.network

import kotlinx.serialization.json.Json

object MangaUpdateChecker {
    // Instanciamos el serializador JSON para leer las cachés antiguas
    private val json = Json { ignoreUnknownKeys = true }

    // Esta función la llamará Android (WorkManager) y Apple (Background Fetch)
    suspend fun buscarNovedades(): List<String> {
        val mangasConNovedades = mutableListOf<String>()
        try {
            UserManager.cargar()
            val servicio = MangaService()

            // 1. Obtenemos solo los mangas que el usuario tiene en "Mi Biblioteca"
            val miBiblioteca = UserManager.getBiblioteca()

            // 2. Comprobamos manga por manga de la biblioteca
            for (mangaTitulo in miBiblioteca) {
                val idManga = mangaTitulo.replace(" ", "_")

                // A) Leemos lo que teníamos guardado LOCALMENTE (sin usar Internet)
                val textoCacheVieja = ZipHelper.leerTexto("caps_${idManga}_normal.json")
                val capsViejos: List<String> = if (textoCacheVieja != null) {
                    try { json.decodeFromString(textoCacheVieja) } catch (e: Exception) { emptyList() }
                } else emptyList()

                // B) Forzamos a descargar la lista ACTUALIZADA desde tu servidor
                val capsNuevos = servicio.obtenerCapitulos(mangaTitulo, isColor = false, forceRefresh = true)

                // C) Comparamos: ¿Qué capítulos están en los Nuevos que NO están en los Viejos?
                val capitulosEstreno = capsNuevos.filter { it !in capsViejos }

                if (capsViejos.isNotEmpty() && capitulosEstreno.isNotEmpty()) {
                    // Limpiamos el texto (quitamos el .cbz para que quede bonito en la notificación)
                    val nombresLimpios = capitulosEstreno.map { it.replace(".cbz", "").replace(".zip", "") }
                    mangasConNovedades.add("$mangaTitulo: ${nombresLimpios.joinToString(", ")}")
                }
            }

            // 3. Ya hemos comprobado la biblioteca. Ahora actualizamos el catálogo general
            UserManager.forzarExpiracionCache()
            servicio.obtenerMangas()

        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Devolvemos la lista de avisos (Ej: ["One Piece: 1070", "Naruto: 500"])
        return mangasConNovedades
    }
}