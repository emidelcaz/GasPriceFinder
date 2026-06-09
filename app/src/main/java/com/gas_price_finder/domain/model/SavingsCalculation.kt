package com.gas_price_finder.domain.model

data class SavingsCalculation(
    val ahorroVsMedia: Double,
    val ahorroVsMasCara: Double?,
    val mediaArea: Double,
    val depositoLleno: Double // Ahorro por llenar el depósito
)
