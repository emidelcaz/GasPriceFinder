package com.gas_price_finder.domain.model

data class PriceHistory(
    val id: Int = 0,
    val estacionId: String,
    val productoId: Int,
    val precio: Double,
    val fechaCaptura: Long
)