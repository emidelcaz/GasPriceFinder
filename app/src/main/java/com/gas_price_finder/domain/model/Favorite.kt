package com.gas_price_finder.domain.model

data class Favorite(
    val id: Int = 0,
    val usuarioId: Int,
    val estacionId: String,
    val nombreSnapshot: String,
    val direccionSnapshot: String?,
    val latitudSnapshot: Double?,
    val longitudSnapshot: Double?,
    val fechaAnadido: Long = System.currentTimeMillis(),
    val precioActual: Double? = null //Precio del combustible preferido si está disponible
)
