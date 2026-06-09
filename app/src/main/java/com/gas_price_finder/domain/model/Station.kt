package com.gas_price_finder.domain.model

import com.gas_price_finder.util.HorarioParser
import kotlin.collections.find

data class Station (
    val ideess: String,
    val rotulo: String,
    val direccion: String?,
    val cp: String?,
    val latitud: Double,
    val longitud: Double,
    val municipio: String?,
    val provincia: String?,
    val horario: String?,
    val tipoEstacion: String?,
    val precios: List<Price> = emptyList(),
    val fechaActualizacion: Long? = null
) {
    fun getPrecio(productoId: Int): Price? {
        return precios.find { it.productoId == productoId }
    }

    fun estaAbierto(): Boolean? {
        return horario?.let { HorarioParser.estaAbierto(it) }
    }
}