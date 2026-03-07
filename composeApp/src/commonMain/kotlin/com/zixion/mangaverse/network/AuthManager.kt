package com.zixion.mangaverse.network

import com.zixion.mangaverse.models.UsuarioFirebase

// La palabra "expect" significa que el código real se escribirá
// en las carpetas de Android y de iOS por separado.
expect class AuthManager() {
    suspend fun iniciarSesionGoogle(): UsuarioFirebase?
    fun cerrarSesion()
    fun obtenerUsuarioActual(): UsuarioFirebase?

    // --- LÍNEAS NUEVAS ---
    suspend fun guardarDatosEnNube(datosJson: String)
    suspend fun cargarDatosDeNube(): String?

    // --- MÉTODO DE ELIMINACIÓN ---
    suspend fun eliminarCuenta(): Boolean
}