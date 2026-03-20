package com.zixion.mangaverse.models

import com.zixion.mangaverse.CommonSerializable
import kotlinx.serialization.Serializable

@Serializable
data class Manga(
    val titulo: String,
    val urlPortada: String?,
    val sinopsis: String = "Sin descripción",
    val generos: List<String> = emptyList(),
    val estado: String = "Desconocido",
    val tipo: String = "Manga"
) : CommonSerializable
