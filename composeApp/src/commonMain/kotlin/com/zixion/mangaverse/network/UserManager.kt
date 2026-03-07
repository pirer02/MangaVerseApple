package com.zixion.mangaverse.network

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.zixion.mangaverse.getCurrentTimeMillis
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@Serializable
data class UserData(
    val biblioteca: MutableSet<String> = mutableSetOf(),
    val historial: MutableMap<String, String> = mutableMapOf(),
    val capitulosLeidos: MutableMap<String, MutableSet<String>> = mutableMapOf(),
    var lastUpdateTimestamp: Long = 0L,
    val progresoPagina: MutableMap<String, Int> = mutableMapOf()
)

object UserManager {
    private const val FILE_NAME = "user_data_v2.json"
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private var data: UserData = UserData()
    private val authManager = AuthManager() // LÍNEA NUEVA

    var estadoReactivo by mutableStateOf(0)
        private set

    fun cargar() {
        val texto = ZipHelper.leerTexto(FILE_NAME)
        if (texto != null) {
            try {
                data = json.decodeFromString(texto)
                estadoReactivo++
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun guardar() {
        val texto = json.encodeToString(data)
        ZipHelper.guardarTexto(FILE_NAME, texto)
        estadoReactivo++

        // --- LÍNEAS NUEVAS: Subir a la nube silenciosamente ---
        if (authManager.obtenerUsuarioActual() != null) {
            GlobalScope.launch {
                authManager.guardarDatosEnNube(texto)
            }
        }
    }

    fun borrarTodoDeFabrica() {
        ZipHelper.borrarTodo()
        // Eliminada la referencia a MusicManager
        data = UserData()
        guardar()
    }

    fun isCacheExpired(): Boolean {
        return (getCurrentTimeMillis() - data.lastUpdateTimestamp) > 3600000L
    }

    fun actualizarTimestampCache() {
        data.lastUpdateTimestamp = getCurrentTimeMillis()
        guardar()
    }

    fun forzarExpiracionCache() {
        data.lastUpdateTimestamp = 0L
        guardar()
    }

    fun toggleBiblioteca(titulo: String): Boolean {
        val agregado = if (data.biblioteca.contains(titulo)) {
            data.biblioteca.remove(titulo)
            false
        } else {
            data.biblioteca.add(titulo)
            true
        }
        guardar()
        return agregado
    }
    fun enBiblioteca(titulo: String): Boolean = data.biblioteca.contains(titulo)

    // ===============================================
    // LÓGICA CON EL PARÁMETRO "isColor" AÑADIDO
    // ===============================================
    private fun capKey(capitulo: String, isColor: Boolean) = if (isColor) "${capitulo}___COLOR" else capitulo
    private fun histKey(titulo: String, isColor: Boolean) = if (isColor) "${titulo}___COLOR" else titulo

    fun guardarProgreso(titulo: String, capitulo: String, pagina: Int = 0, isColor: Boolean = false) {
        data.historial[histKey(titulo, isColor)] = capitulo
        data.progresoPagina["${titulo}___${capKey(capitulo, isColor)}"] = pagina
        guardar()
    }

    fun actualizarHistorial(titulo: String, capitulo: String, isColor: Boolean = false) {
        val key = if (isColor) "${titulo}___COLOR" else titulo
        data.historial[key] = capitulo
        guardar()
    }

    fun getUltimoCapitulo(titulo: String, isColor: Boolean = false): String? = data.historial[histKey(titulo, isColor)]

    fun getPaginaGuardada(titulo: String, capitulo: String, isColor: Boolean = false): Int {
        return data.progresoPagina["${titulo}___${capKey(capitulo, isColor)}"] ?: 0
    }

    fun marcarCapituloComoLeido(titulo: String, capitulo: String, isColor: Boolean = false) {
        if (!data.capitulosLeidos.containsKey(titulo)) data.capitulosLeidos[titulo] = mutableSetOf()
        data.capitulosLeidos[titulo]?.add(capKey(capitulo, isColor))
        guardar()
    }

    fun desmarcarCapitulo(titulo: String, capitulo: String, isColor: Boolean = false) {
        data.capitulosLeidos[titulo]?.remove(capKey(capitulo, isColor))
        guardar()
    }

    fun isCapituloLeido(titulo: String, capitulo: String, isColor: Boolean = false): Boolean {
        return data.capitulosLeidos[titulo]?.contains(capKey(capitulo, isColor)) == true
    }

    fun tieneCapitulosLeidos(titulo: String): Boolean {
        return data.capitulosLeidos[titulo]?.isNotEmpty() == true ||
                data.historial.containsKey(titulo) ||
                data.historial.containsKey("${titulo}___COLOR")
    }

    fun borrarProgresoManga(titulo: String) {
        data.historial.remove(titulo)
        data.historial.remove("${titulo}___COLOR")
        data.capitulosLeidos.remove(titulo)
        val keysAEliminar = data.progresoPagina.keys.filter { it.startsWith("${titulo}___") }
        keysAEliminar.forEach { data.progresoPagina.remove(it) }
        guardar()
    }

    fun forzarRecomposicion() { estadoReactivo++ }

    fun getBiblioteca(): List<String> = data.biblioteca.toList()


    suspend fun sincronizarDesdeNube() {
        val textoNube = authManager.cargarDatosDeNube()
        if (textoNube != null) {
            try {
                // Sobrescribimos los datos locales con los de la nube
                data = json.decodeFromString(textoNube)
                ZipHelper.guardarTexto(FILE_NAME, textoNube)
                estadoReactivo++
            } catch (e: Exception) { e.printStackTrace() }
        } else {
            // Es un usuario nuevo en la nube, subimos su progreso local actual
            guardar()
        }
    }

}