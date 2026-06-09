package com.gas_price_finder.domain.model

data class Price(
    val productoId: Int,
    val nombreProducto: String,
    val precio: Double,
    val fechaActualizacion: Long
)