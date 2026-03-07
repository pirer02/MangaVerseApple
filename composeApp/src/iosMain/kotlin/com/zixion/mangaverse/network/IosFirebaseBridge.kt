package com.zixion.mangaverse.network

import com.zixion.mangaverse.models.UsuarioFirebase

// 1. El "contrato" que nuestro código de Swift en Xcode ha firmado
interface IosFirebaseDelegate {
    suspend fun iniciarSesionGoogle(): UsuarioFirebase?
    fun cerrarSesion()
    fun obtenerUsuarioActual(): UsuarioFirebase?
    suspend fun guardarDatosEnNube(datosJson: String)
    suspend fun cargarDatosDeNube(): String?
    suspend fun eliminarCuenta(): Boolean
}

// 2. El puente físico por donde pasa la información
object IosFirebaseBridge {
    var delegate: IosFirebaseDelegate? = null
}