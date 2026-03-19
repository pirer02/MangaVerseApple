package com.zixion.mangaverse.network

import platform.Foundation.*
import okio.*
import okio.Path.Companion.toPath

actual object ZipHelper {

    actual fun descomprimir(rutaZip: String, rutaCache: String, nombreManga: String, nombreCapitulo: String): List<String> {
        // 1. EL SALVAVIDAS "DUMMY": Si Ktor falló, nos rendimos en paz sin crashear iOS.
        if (rutaZip == "dummy") return emptyList()

        // Preparar rutas (Caché de iOS)
        val fileManager = NSFileManager.defaultManager
        val urls = fileManager.URLsForDirectory(NSCachesDirectory, NSUserDomainMask)
        val cacheUrl = urls.first() as? NSURL ?: return emptyList()
        val cachePathString = cacheUrl.path ?: return emptyList()

        val nombreCapSinExt = nombreCapitulo.replace(".cbz", "").replace(".zip", "")
        val rootPath = cachePathString.toPath()
        val mangaFolder = rootPath / "mangaverse_caps_v2" / nombreManga.replace(" ", "_") / nombreCapSinExt

        val fs = FileSystem.SYSTEM
        val zipFilePath = rutaZip.toPath()

        try {
            // 2. ANTÍDOTO CACHÉ ZOMBI
            if (fs.exists(mangaFolder)) {
                val existing = fs.list(mangaFolder).filter { esImagen(it.name) }
                if (existing.isNotEmpty()) {
                    return existing.map { it.toString() }.sorted()
                } else {
                    // Si la carpeta está corrupta o vacía, la aniquilamos
                    fs.deleteRecursively(mangaFolder)
                    fs.createDirectories(mangaFolder)
                }
            } else {
                fs.createDirectories(mangaFolder)
            }

            // Usamos Okio para leer el ZIP
            val zipFileSystem = fs.openZip(zipFilePath)
            val archivosEnZip = zipFileSystem.listRecursively("/".toPath()).toList()

            for (archivoEnZip in archivosEnZip) {
                if (!zipFileSystem.metadata(archivoEnZip).isDirectory) {

                    // 3. EL APLANADOR + LIMPIEZA DE SÍMBOLOS ROTOS (#, %)
                    // En Okio, 'archivoEnZip' es una ruta absoluta interna (ej. "/Carpeta/01.jpg")
                    var nombreSeguro = archivoEnZip.toString()
                        .replace("\\", "_")
                        .replace("/", "_")
                        .replace("#", "_")
                        .replace("%", "_")

                    // Le quitamos el primer guion bajo si Okio lo puso al principio
                    if (nombreSeguro.startsWith("_")) {
                        nombreSeguro = nombreSeguro.drop(1)
                    }

                    if (esImagen(nombreSeguro)) {
                        val destino = mangaFolder / nombreSeguro
                        zipFileSystem.source(archivoEnZip).buffer().use { source ->
                            fs.sink(destino).buffer().use { sink ->
                                sink.writeAll(source)
                            }
                        }
                    }
                }
            }
            return fs.list(mangaFolder).filter { esImagen(it.name) }.map { it.toString() }.sorted()

        } catch (e: Exception) {
            e.printStackTrace()
            // Limpieza: Si estalla a mitad de extracción, borramos todo
            if (fs.exists(mangaFolder)) {
                try { fs.deleteRecursively(mangaFolder) } catch (ex: Exception) {}
            }
            return emptyList()
        }
    }

    private fun esImagen(nombre: String): Boolean {
        val n = nombre.lowercase()
        // 4. ANTÍDOTO MAC OS: Ignorar archivos basura '._'
        val nombrePuro = nombre.split("/").last().split("\\").last().lowercase()
        if (nombrePuro.startsWith("._")) return false

        return n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png") || n.endsWith(".webp")
    }

    actual fun guardarTexto(nombreArchivo: String, texto: String) {
        val fileManager = NSFileManager.defaultManager
        val urls = fileManager.URLsForDirectory(NSDocumentDirectory, NSUserDomainMask)
        val docUrl = urls.first() as? NSURL ?: return
        val docPathString = docUrl.path ?: return

        val rootPath = docPathString.toPath()
        val dataFolder = rootPath / "mangaverse_data"
        val fs = FileSystem.SYSTEM

        try {
            if (!fs.exists(dataFolder)) {
                fs.createDirectories(dataFolder)
            }
            val archivoDestino = dataFolder / nombreArchivo
            fs.sink(archivoDestino).buffer().use { sink ->
                sink.writeUtf8(texto)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    actual fun leerTexto(nombreArchivo: String): String? {
        val fileManager = NSFileManager.defaultManager
        val urls = fileManager.URLsForDirectory(NSDocumentDirectory, NSUserDomainMask)
        val docUrl = urls.first() as? NSURL ?: return null
        val docPathString = docUrl.path ?: return null

        val archivoDestino = docPathString.toPath() / "mangaverse_data" / nombreArchivo
        val fs = FileSystem.SYSTEM

        return try {
            if (fs.exists(archivoDestino)) {
                fs.source(archivoDestino).buffer().use { source ->
                    source.readUtf8()
                }
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    actual fun borrarTodo() {
        val fileManager = NSFileManager.defaultManager
        val fs = FileSystem.SYSTEM

        try {
            // Borramos la caché de imágenes (pesada)
            val urlsCache = fileManager.URLsForDirectory(NSCachesDirectory, NSUserDomainMask)
            if (urlsCache.isNotEmpty()) {
                val cacheUrl = urlsCache.first() as? NSURL
                cacheUrl?.path?.let { path ->
                    val cacheImg = path.toPath() / "mangaverse_caps_v2"
                    if (fs.exists(cacheImg)) fs.deleteRecursively(cacheImg)
                }
            }

            // Borramos los datos de guardado (JSONs)
            val urlsDocs = fileManager.URLsForDirectory(NSDocumentDirectory, NSUserDomainMask)
            if (urlsDocs.isNotEmpty()) {
                val docUrl = urlsDocs.first() as? NSURL
                docUrl?.path?.let { path ->
                    val cacheData = path.toPath() / "mangaverse_data"
                    if (fs.exists(cacheData)) fs.deleteRecursively(cacheData)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}