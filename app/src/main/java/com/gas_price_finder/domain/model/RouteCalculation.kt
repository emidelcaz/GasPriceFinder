package com.gas_price_finder.domain.model

data class RouteCalculation(
    val estacionOptima: Station?,
    val distanciaDesvioKm: Double,
    val costeTotal: Double,
    val costeCombustible: Double,
    val costeAdBlue: Double,
    val costeDesvio: Double,
    val ahorroVsMedia: Double,
    val autonomiaKm: Double
)

data class CosteEnRuta(
    val costeTotal: Double,
    val costeCombustible: Double,
    val costeAdBlue: Double,
    val costeDesvio: Double,
    val litrosNecesarios: Double,
    val litrosAdBlueNecesarios: Double
)
