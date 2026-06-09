package com.gas_price_finder.domain.usecase.price

import com.gas_price_finder.domain.model.Price
import com.gas_price_finder.domain.model.Station
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CalculateSavingsUseCaseTest {

    private val useCase = CalculateSavingsUseCase()

    private fun station(id: String, precio: Double, productoId: Int = 1) = Station(
        ideess = id,
        rotulo = "Test",
        direccion = "Calle Test",
        cp = "28001",
        latitud = 40.0,
        longitud = -3.0,
        municipio = "Madrid",
        provincia = "Madrid",
        horario = "L-V:08:00-20:00",
        tipoEstacion = "P",
        precios = listOf(
            Price(productoId = productoId, nombreProducto = "Gasolina 95", precio = precio, fechaActualizacion = 0L)
        )
    )

    @Test
    fun `calcula ahorro correcto respecto a la media del area`() {
        val target = station("1", 1.50)
        val area = listOf(
            station("2", 1.60),
            station("3", 1.40),
            station("4", 1.60)
        )
        // Media = (1.60 + 1.40 + 1.60 + 1.50) / 4 = 1.525
        // Ahorro por litro = 1.525 - 1.50 = 0.025
        // Depósito 50L -> 1.25 €
        val result = useCase(target, area + target, 50.0)
        assertThat(result.mediaArea).isWithin(0.001).of(1.525)
        assertThat(result.ahorroVsMedia).isGreaterThan(0.0)
        assertThat(result.ahorroVsMedia).isWithin(0.01).of(1.25)
    }

    @Test
    fun `calcula ahorro respecto a la estacion mas cara`() {
        val target = station("1", 1.40)
        val area = listOf(
            station("2", 1.60),
            station("3", 1.70)
        )
        val result = useCase(target, area + target, 40.0)
        // Más cara = 1.70
        // Ahorro vs más cara = (1.70 - 1.40) * 40 = 12.0
        assertThat(result.ahorroVsMasCara).isNotNull()
        assertThat(result.ahorroVsMasCara!!).isWithin(0.01).of(12.0)
    }

    @Test
    fun `devuelve valores neutros cuando no hay precios en el area`() {
        val target = station("1", 1.50)
        val result = useCase(target, emptyList(), 50.0)
        assertThat(result.ahorroVsMedia).isEqualTo(0.0)
        assertThat(result.ahorroVsMasCara).isNull()
        assertThat(result.mediaArea).isEqualTo(0.0)
    }

    @Test
    fun `devuelve valores neutros cuando la estacion no tiene precios`() {
        val target = station("1", 1.50).copy(precios = emptyList())
        val area = listOf(station("2", 1.60))
        val result = useCase(target, area, 50.0)
        assertThat(result.ahorroVsMedia).isEqualTo(0.0)
        assertThat(result.mediaArea).isEqualTo(0.0)
    }
}
