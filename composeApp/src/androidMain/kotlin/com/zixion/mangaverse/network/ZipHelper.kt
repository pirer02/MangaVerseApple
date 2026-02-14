package com.zixion.mangaverse.network

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile

object AndroidContext {
    lateinit var context: Context
}

actual object ZipHelper {
    actual fun descomprimir(rutaZip: String, rutaCache: String, nombreManga: String, nombreCapitulo: String): List<String> {
        val context = AndroidContext.context
        // Usamos la carpeta v2 para asegurar caché limpia
        val cacheDir = File(context.cacheDir, "mangaverse_caps_v2")

        val nombreCapSinExt = nombreCapitulo.replace(".cbz", "").replace(".zip", "")
        val chapterDir = File(cacheDir, "${nombreManga.replace(" ", "_")}/$nombreCapSinExt")

        // Si ya existe la carpeta y tiene imágenes, retornamos la caché sin hacer nada
        if (chapterDir.exists() && (chapterDir.listFiles()?.isNotEmpty() == true)) {
            return chapterDir.listFiles()!!
                .filter { esImagen(it.name) }
                .sortedBy { it.name }
                .map { it.absolutePath }
        }

        chapterDir.mkdirs()
        val listaImagenes = mutableListOf<String>()

        try {
            // LEER DESDE EL ARCHIVO EN DISCO (Memoria eficiente)
            val zipFile = ZipFile(rutaZip)
            val entries = zipFile.entries()

            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                if (!entry.isDirectory) {
                    val nombreNormalizado = entry.name.replace("\\", "/")
                    val nombreArchivoPuro = File(nombreNormalizado).name

                    if (esImagen(nombreArchivoPuro)) {
                        val file = File(chapterDir, nombreArchivoPuro)
                        // Copiamos del ZIP al destino
                        zipFile.getInputStream(entry).use { input ->
                            FileOutputStream(file).use { output ->
                                input.copyTo(output)
                            }
                        }
                        listaImagenes.add(file.absolutePath)
                    }
                }
            }
            zipFile.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return listaImagenes.sorted()
    }

    private fun esImagen(nombre: String): Boolean {
        val n = nombre.lowercase()
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
