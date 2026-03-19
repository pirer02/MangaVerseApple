package com.zixion.mangaverse.network

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile

object AndroidContext {
    lateinit var context: Context
    var activity: androidx.activity.ComponentActivity? = null // AÑADE ESTA LÍNEA
}

actual object ZipHelper {
    actual fun descomprimir(rutaZip: String, rutaCache: String, nombreManga: String, nombreCapitulo: String): List<String> {
        val context = AndroidContext.context
        val cacheDir = File(context.cacheDir, "mangaverse_caps_v2")
        val nombreCapSinExt = nombreCapitulo.replace(".cbz", "").replace(".zip", "")
        val chapterDir = File(cacheDir, "${nombreManga.replace(" ", "_")}/$nombreCapSinExt")

        // 1. ANTÍDOTO CACHÉ ZOMBI
        if (chapterDir.exists()) {
            val imagenesValidas = chapterDir.listFiles()
                ?.filter { esImagen(it.name) }
                ?.map { it.absolutePath }
                ?: emptyList()

            if (imagenesValidas.isNotEmpty()) {
                return imagenesValidas.sorted()
            } else {
                chapterDir.deleteRecursively()
            }
        }

        // 2. EL SALVAVIDAS "DUMMY": Si no hay caché y la red falló, nos rendimos sin crashear.
        if (rutaZip == "dummy") {
            return emptyList()
        }

        chapterDir.mkdirs()
        val listaImagenes = mutableListOf<String>()

        try {
            android.util.Log.d("MANGA_DEBUG", "Iniciando descompresión de: $rutaZip")
            ZipFile(rutaZip).use { zipFile ->
                val entries = zipFile.entries()
                var totalEnZip = 0
                var imagenesAceptadas = 0

                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    totalEnZip++

                    if (!entry.isDirectory) {
// LA ESTOCADA FINAL: Limpiamos símbolos que rompen las URIs en Android
                        val nombreSeguro = entry.name
                            .replace("\\", "_")
                            .replace("/", "_")
                            .replace("#", "_")
                            .replace("%", "_")

                        if (esImagen(nombreSeguro)) {
                            val file = File(chapterDir, nombreSeguro)
                            zipFile.getInputStream(entry).use { input ->
                                FileOutputStream(file).use { output ->
                                    input.copyTo(output)
                                }
                            }
                            listaImagenes.add(file.absolutePath)
                            imagenesAceptadas++
                        } else {
                            // CHIVATO 3: Nos avisa si un archivo fue ignorado
                            android.util.Log.d("MANGA_DEBUG", "RECHAZADO: ${entry.name}")
                        }
                    }
                }
                // CHIVATO 4: Resumen de la extracción
                android.util.Log.d("MANGA_DEBUG", "Resumen ZIP -> Total: $totalEnZip | Aceptadas: $imagenesAceptadas")
            }
        } catch (e: Exception) {
            android.util.Log.e("MANGA_DEBUG", "ERROR AL DESCOMPRIMIR ZIP: ${e.message}")
            e.printStackTrace()
            // Solo borramos si falló una descompresión real, no un dummy
            if (rutaZip != "dummy") {
                chapterDir.deleteRecursively()
            }
            return emptyList()
        }

        return listaImagenes.sorted()
    }

    private fun esImagen(nombre: String): Boolean {
        val n = nombre.lowercase()
        val nombrePuro = File(nombre).name.lowercase()

        // ANTÍDOTO MAC OS
        if (nombrePuro.startsWith("._")) return false

        return n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png") || n.endsWith(".webp")
    }

    actual fun guardarTexto(nombreArchivo: String, texto: String) {
        try {
            val context = AndroidContext.context
            val cacheDir = File(context.cacheDir, "mangaverse_data")
            if (!cacheDir.exists()) cacheDir.mkdirs()
            val file = File(cacheDir, nombreArchivo)
            file.writeText(texto)
        } catch (e: Exception) { e.printStackTrace() }
    }

    actual fun leerTexto(nombreArchivo: String): String? {
        try {
            val context = AndroidContext.context
            val cacheDir = File(context.cacheDir, "mangaverse_data")
            val file = File(cacheDir, nombreArchivo)
            if (file.exists()) return file.readText()
        } catch (e: Exception) { e.printStackTrace() }
        return null
    }

    actual fun borrarTodo() {
        try {
            val context = AndroidContext.context
            val cacheImg = java.io.File(context.cacheDir, "mangaverse_caps_v2")
            val cacheData = java.io.File(context.cacheDir, "mangaverse_data")

            if (cacheImg.exists()) cacheImg.deleteRecursively()
            if (cacheData.exists()) cacheData.deleteRecursively()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}