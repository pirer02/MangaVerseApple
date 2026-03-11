package com.zixion.mangaverse.network

import kotlinx.serialization.json.Json

    // Esta función la llamará Android (WorkManager) y Apple (Background Fetch)
    object MangaUpdateChecker {
        // Instanciamos el serializador JSON para leer las cachés antiguas
        private val json = Json { ignoreUnknownKeys = true }



        // --- En MangaUpdateChecker.kt ---
        suspend fun buscarNovedades(): List<String> {
            val mangasConNovedades = mutableListOf<String>()
            try {
                // 1. Cargamos primero los datos locales que conocemos
                UserManager.cargar()

                // 2. NUEVO: Intentamos sincronizar con la nube antes de buscar
                // Esto permite que si añadiste un manga en Android, el iPhone lo sepa antes de buscar capítulos
                val auth = AuthManager()
                if (auth.obtenerUsuarioActual() != null) {
                    try {
                        UserManager.sincronizarDesdeNube() // Actualiza la biblioteca con lo que haya en Firestore
                    } catch (e: Exception) {
                        // Si no hay internet o falla, seguimos con lo que ya sabíamos localmente
                    }
                }

                // 3. Verificamos si el usuario quiere recibir notificaciones
                if (!UserManager.areNotificacionesActivas()) {
                    return emptyList()
                }

                val servicio = MangaService()
                val miBiblioteca = UserManager.getBiblioteca()

                for (mangaTitulo in miBiblioteca) {
                    val idManga = mangaTitulo.replace(" ", "_")

                    // Comparamos caché local vs Servidor
                    val textoCacheVieja = ZipHelper.leerTexto("caps_${idManga}_normal.json")
                    val capsViejos: List<String> = if (textoCacheVieja != null) {
                        try { json.decodeFromString(textoCacheVieja) } catch (e: Exception) { emptyList() }
                    } else emptyList()

                    // Al llamar a obtenerCapitulos con forceRefresh = true, se sobreescribe el archivo JSON local
                    // actualizando así los datos de la aplicación en segundo plano.
                    val capsNuevos = servicio.obtenerCapitulos(mangaTitulo, isColor = false, forceRefresh = true)

                    val capitulosEstreno = capsNuevos.filter { it !in capsViejos }

                    if (capsViejos.isNotEmpty() && capitulosEstreno.isNotEmpty()) {
                        val nombresLimpios = capitulosEstreno.map { it.replace(".cbz", "").replace(".zip", "") }
                        mangasConNovedades.add("$mangaTitulo: ${nombresLimpios.joinToString(", ")}")
                    }
                }

                UserManager.forzarExpiracionCache()
                servicio.obtenerMangas()

            } catch (e: Exception) {
                e.printStackTrace()
            }

            return mangasConNovedades
        }
    }
