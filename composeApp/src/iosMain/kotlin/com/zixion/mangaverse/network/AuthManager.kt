package com.zixion.mangaverse.network

import com.zixion.mangaverse.models.UsuarioFirebase

actual class AuthManager actual constructor() {

    actual suspend fun iniciarSesionGoogle(): UsuarioFirebase? {
        // Quitamos el .shared y llamamos al delegate directamente
        return IosFirebaseBridge.delegate?.iniciarSesionGoogle()
    }

    actual fun cerrarSesion() {
        IosFirebaseBridge.delegate?.cerrarSesion()
    }

    actual fun obtenerUsuarioActual(): UsuarioFirebase? {
        return IosFirebaseBridge.delegate?.obtenerUsuarioActual()
    }

    actual suspend fun guardarDatosEnNube(datosJson: String) {
        IosFirebaseBridge.delegate?.guardarDatosEnNube(datosJson)
    }

    actual suspend fun cargarDatosDeNube(): String? {
        return IosFirebaseBridge.delegate?.cargarDatosDeNube()
    }

    actual suspend fun eliminarCuenta(): Boolean {
        return IosFirebaseBridge.delegate?.eliminarCuenta() ?: false
    }
}