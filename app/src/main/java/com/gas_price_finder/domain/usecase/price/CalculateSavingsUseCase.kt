package com.gas_price_finder.domain.usecase.price

import com.gas_price_finder.domain.model.SavingsCalculation
import com.gas_price_finder.domain.model.Station
import javax.inject.Inject

class CalculateSavingsUseCase @Inject constructor() {
    operator fun invoke(
        station: Station,
        allStationsInArea: List<Station>,
        capacidadDepositoL: Double
    ): SavingsCalculation {

        val productoId = station.precios.firstOrNull()?.productoId ?: return SavingsCalculation(
            0.0,
            null,
            0.0,
            0.0
        )

        val preciosArea = allStationsInArea.mapNotNull {
            it.precios.find { p -> p.productoId == productoId }?.precio
        }

        if (preciosArea.isEmpty()) {
            return SavingsCalculation(0.0, null, 0.0, 0.0)
        }

        val mediaArea = preciosArea.average()
        val precioEstacion =
            station.precios.find { it.productoId == productoId }?.precio ?: mediaArea

        val ahorroPorLitro = mediaArea - precioEstacion
        val ahorroTotal = ahorroPorLitro * capacidadDepositoL

        val masCara = preciosArea.maxOrNull()
        val ahorroVsMasCara = masCara?.let { (it - precioEstacion) * capacidadDepositoL }

        return SavingsCalculation(
            ahorroVsMedia = ahorroTotal,
            ahorroVsMasCara = ahorroVsMasCara,
            mediaArea = mediaArea,
            depositoLleno = ahorroTotal
        )
    }

}