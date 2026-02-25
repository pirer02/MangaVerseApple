package com.zixion.mangaverse.network

import com.zixion.mangaverse.models.Manga
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.core.isEmpty
import io.ktor.utils.io.core.readBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM
import okio.buffer
import okio.use
import kotlin.concurrent.Volatile

private val globalJson = Json { ignoreUnknownKeys = true; coerceInputValues = true; prettyPrint = true }
private val globalClient = HttpClient {
    install(ContentNegotiation) { json(globalJson) }
    install(HttpTimeout) {
        requestTimeoutMillis = 60000
        connectTimeoutMillis = 10000
        socketTimeoutMillis = 60000
    }
    install(HttpRequestRetry) { retryOnServerErrors(maxRetries = 2); exponentialDelay() }
}

class MangaService {
    private val client = globalClient

    companion object {
        private const val IP_LOCAL = "oculto"
        private const val IP_PUBLICA = "http://95.61.154.61:5000/"
        @Volatile private var urlActiva: String? = null
    }

    private suspend fun determinarUrlBase(): String = withContext(Dispatchers.IO) {
        if (urlActiva != null) return@withContext urlActiva!!
        return@withContext try {
            client.get("${IP_LOCAL}mangas") { timeout { requestTimeoutMillis = 1500 } }
            urlActiva = IP_LOCAL
            IP_LOCAL
        } catch (e: Exception) {
            urlActiva = IP_PUBLICA
            IP_PUBLICA
        }
    }

    suspend fun obtenerPaginas(mangaTitulo: String, capitulo: String, isColor: Boolean): List<String> = withContext(Dispatchers.IO) {
        try {
            val urlBase = determinarUrlBase()
            val mangaId = mangaTitulo.replace(" ", "_")
            val capName = if (capitulo.endsWith(".cbz")) capitulo else "$capitulo.cbz"
            val capEncoded = capName.replace(" ", "%20")

            val endpoint = if (isColor) "${urlBase}download/$mangaId/color/$capEncoded" else "${urlBase}download/$mangaId/$capEncoded"

            val tempDir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY
            val tempFile = tempDir / "temp_descarga.zip"

            client.prepareGet(endpoint).execute { response ->
                val channel = response.bodyAsChannel()
                FileSystem.SYSTEM.sink(tempFile).buffer().use { sink ->
                    while (!channel.isClosedForRead) {
                        val packet = channel.readRemaining(8192)
                        while (!packet.isEmpty) { sink.write(packet.readBytes()) }
                    }
                }
            }

            // LA MAGIA AQUÍ: Separa las carpetas de caché
            val cacheFolderName = if (isColor) "${capName}_COLOR" else capName

            val rutas = ZipHelper.descomprimir(tempFile.toString(), "cache", mangaId, cacheFolderName)
            FileSystem.SYSTEM.delete(tempFile)
            return@withContext rutas

        } catch (e: Exception) {
            try {
                val mangaId = mangaTitulo.replace(" ", "_")
                val capName = if (capitulo.endsWith(".cbz")) capitulo else "$capitulo.cbz"
                val cacheFolderName = if (isColor) "${capName}_COLOR" else capName
                return@withContext ZipHelper.descomprimir("dummy", "cache", mangaId, cacheFolderName)
            } catch (e2: Exception) {
                return@withContext emptyList()
            }
        }
    }

    suspend fun obtenerMangas(): List<Manga> = withContext(Dispatchers.IO) {
        val cachedText = ZipHelper.leerTexto("mangas_list.json")
        val isCacheExpirada = UserManager.isCacheExpired()

        if (cachedText != null && !isCacheExpirada) {
            try {
                val nombres: List<String> = globalJson.decodeFromString(cachedText)
                val url = determinarUrlBase()
                return@withContext nombres.map { Manga(titulo = it.replace("_", " "), urlPortada = "${url}mangas/$it/portada") }
            } catch (e: Exception) { }
        }

        try {
            val url = determinarUrlBase()
            val responseText = client.get("${url}mangas").bodyAsText()
            ZipHelper.guardarTexto("mangas_list.json", responseText)

            // BORRA O COMENTA ESTA LÍNEA:
            UserManager.actualizarTimestampCache()

            val nombres: List<String> = globalJson.decodeFromString(responseText)
            return@withContext nombres.map { Manga(titulo = it.replace("_", " "), urlPortada = "${url}mangas/$it/portada") }
        } catch (e: Exception) {
            if (cachedText != null) {
                try {
                    val nombres: List<String> = globalJson.decodeFromString(cachedText)
                    val url = determinarUrlBase()
                    return@withContext nombres.map { Manga(titulo = it.replace("_", " "), urlPortada = "${url}mangas/$it/portada") }
                } catch (e2: Exception) { return@withContext emptyList() }
            }
            return@withContext emptyList()
        }
    }

    suspend fun obtenerInfoManga(manga: Manga): Manga = withContext(Dispatchers.IO) {
        val id = manga.titulo.replace(" ", "_")
        val cached = ZipHelper.leerTexto("info_${id}.json")
        val isCacheExpirada = UserManager.isCacheExpired()

        if (cached != null && !isCacheExpirada) {
            try {
                val info = globalJson.decodeFromString<MangaInfoResponse>(cached)
                return@withContext manga.copy(sinopsis = info.descripcion ?: "", estado = info.estado ?: "", tipo = info.tipo ?: "", generos = info.generos)
            } catch (e: Exception) { }
        }

        try {
            val url = determinarUrlBase()
            val responseText = client.get("${url}mangas/$id/info").bodyAsText()
            ZipHelper.guardarTexto("info_${id}.json", responseText)
            val info = globalJson.decodeFromString<MangaInfoResponse>(responseText)
            return@withContext manga.copy(sinopsis = info.descripcion ?: "", estado = info.estado ?: "", tipo = info.tipo ?: "", generos = info.generos)
        } catch (e: Exception) {
            if (cached != null) {
                try {
                    val info = globalJson.decodeFromString<MangaInfoResponse>(cached)
                    return@withContext manga.copy(sinopsis = info.descripcion ?: "", estado = info.estado ?: "", tipo = info.tipo ?: "", generos = info.generos)
                } catch(e2: Exception) {}
            }
            return@withContext manga
        }
    }

    suspend fun tieneColor(mangaNombre: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = determinarUrlBase()
            client.get("${url}mangas/${mangaNombre.replace(" ", "_")}/color/capitulos").body<List<String>>().isNotEmpty()
        } catch (e: Exception) { false }
    }

    suspend fun obtenerCapitulos(mangaNombre: String, isColor: Boolean, forceRefresh: Boolean = false): List<String> = withContext(Dispatchers.IO) {
        val id = mangaNombre.replace(" ", "_")
        val tipo = if (isColor) "color" else "normal"
        val cached = ZipHelper.leerTexto("caps_${id}_${tipo}.json")
        val isCacheExpirada = UserManager.isCacheExpired()

        // 1. Añadimos la validación de forceRefresh y de lista.isNotEmpty()
        if (!forceRefresh && cached != null && !isCacheExpirada) {
            try {
                val lista = globalJson.decodeFromString<List<String>>(cached)
                // 2. Solo usamos la caché si realmente tiene capítulos guardados
                if (lista.isNotEmpty()) {
                    return@withContext lista
                }
            } catch(e: Exception) {}
        }



        try {
            val url = determinarUrlBase()
            val endpoint = if (isColor) "color/capitulos" else "capitulos"
            val responseText = client.get("${url}mangas/$id/$endpoint").bodyAsText()
            ZipHelper.guardarTexto("caps_${id}_${tipo}.json", responseText)
            return@withContext globalJson.decodeFromString(responseText)
        } catch (e: Exception) {
            if (cached != null) {
                try { return@withContext globalJson.decodeFromString(cached) } catch(e2: Exception) {}
            }
            return@withContext emptyList()
        }
    }
}

@Serializable
private data class MangaInfoResponse(val titulo: String? = null, val descripcion: String? = null, val estado: String? = null, val tipo: String? = null, val generos: List<String> = emptyList())