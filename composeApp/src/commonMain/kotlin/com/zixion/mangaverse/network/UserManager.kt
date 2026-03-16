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
    val progresoPagina: MutableMap<String, Int> = mutableMapOf(),
    val timestampsCapitulos: MutableMap<String, Long> = mutableMapOf(),

    var notificacionesActivas: Boolean = true // <-- AÑADIR ESTO

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

    // 1. NUEVA FUNCIÓN: Solo guarda en el dispositivo (ideal para la caché)
    private fun guardarLocal() {
        val texto = json.encodeToString(data)
        ZipHelper.guardarTexto(FILE_NAME, texto)
        estadoReactivo++
    }

    // 2. FUNCIÓN MODIFICADA: Guarda en el dispositivo Y sube a la nube
    private fun guardar() {
        guardarLocal()

        // Solo sube a la nube cuando hay cambios reales (añadir manga, leer, etc.)
        if (authManager.obtenerUsuarioActual() != null) {
            GlobalScope.launch {
                authManager.guardarDatosEnNube(json.encodeToString(data))
            }
        }
    }

    fun borrarTodoDeFabrica() {
        ZipHelper.borrarTodo()
        data = UserData()
        // NO llamamos a guardar() aquí para evitar borrar la nube por accidente
        val texto = json.encodeToString(data)
        ZipHelper.guardarTexto(FILE_NAME, texto)
        estadoReactivo++
    }

    fun isCacheExpired(): Boolean {
        return (getCurrentTimeMillis() - data.lastUpdateTimestamp) > 3600000L
    }

    fun actualizarTimestampCache() {
        data.lastUpdateTimestamp = getCurrentTimeMillis()
        guardarLocal()
    }

    fun forzarExpiracionCache() {
        data.lastUpdateTimestamp = 0L
        data.timestampsCapitulos.clear() // ¡NUEVO! Al forzar, también borramos los relojes de los capítulos
        guardarLocal()
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
                data = json.decodeFromString(textoNube)
                ZipHelper.guardarTexto(FILE_NAME, textoNube)
                estadoReactivo++
            } catch (e: Exception) { e.printStackTrace() }
        }
        // ELIMINADO el else { guardar() }.
        // Si no hay datos en la nube, no forzamos la creación de un documento vacío.
        // Se creará naturalmente la primera vez que el usuario agregue un manga.
    }


    fun isCapitulosCacheExpired(idCache: String): Boolean {
        val lastUpdate = data.timestampsCapitulos[idCache] ?: 0L
        return (getCurrentTimeMillis() - lastUpdate) > 3600000L // 1 hora
    }

    fun actualizarTimestampCapitulos(idCache: String) {
        data.timestampsCapitulos[idCache] = getCurrentTimeMillis()
        guardarLocal()
    }


    fun setNotificaciones(activas: Boolean) {
        data.notificacionesActivas = activas
        guardar() // Guarda el cambio localmente y en la nube
    }

    fun areNotificacionesActivas(): Boolean = data.notificacionesActivas

    // -------------------------------------------


}