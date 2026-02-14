package com.zixion.mangaverse

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

// NUEVO: Función para obtener el tiempo en milisegundos en cualquier plataforma
expect fun getCurrentTimeMillis(): Long