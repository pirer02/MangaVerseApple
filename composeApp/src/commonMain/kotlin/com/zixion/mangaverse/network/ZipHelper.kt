package com.zixion.mangaverse.network

expect object ZipHelper {

    fun descomprimir(rutaZip: String, rutaCache: String, nombreManga: String, nombreCapitulo: String): List<String>
    fun guardarTexto(nombreArchivo: String, texto: String)
    fun leerTexto(nombreArchivo: String): String?

    // NUEVA FUNCIÓN
    fun borrarTodo()
}