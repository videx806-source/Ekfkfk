package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Channel(
    val name: String,
    val path: String,
    val group: String
)

@JsonClass(generateAdapter = true)
data class Evento(
    val id: String,
    val titulo: String,
    val equipos: String,
    val liga: String,
    val fecha: String,
    val hora: String,
    @Json(name = "zona_horaria") val zonaHoraria: String,
    @Json(name = "imagen_url") val imagenUrl: String,
    @Json(name = "stream_url") val streamUrl: String,
    val estado: String, // "en_vivo" | "proximo" | "finalizado"
    val destacado: Boolean
)

@JsonClass(generateAdapter = true)
data class EventosResponse(
    val eventos: List<Evento>
)

data class RecordingFile(
    val name: String,
    val absolutePath: String,
    val lastModified: Long,
    val length: Long
)
