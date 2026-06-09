package com.gas_price_finder.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class StationTest {

    private fun createStation(horario: String? = null, precios: List<Price> = emptyList()) = Station(
        ideess = "1234",
        rotulo = "Repsol",
        direccion = "Calle Mayor 1",
        cp = "28001",
        latitud = 40.4168,
        longitud = -3.7038,
        municipio = "Madrid",
        provincia = "Madrid",
        horario = horario,
        tipoEstacion = "P",
        precios = precios
    )

    @Test
    fun `getPrecio devuelve el precio correcto por productoId`() {
        val precios = listOf(
            Price(1, "Gasolina 95", 1.50, 0L),
            Price(4, "Gasoleo A", 1.40, 0L)
        )
        val station = createStation(precios = precios)
        assertThat(station.getPrecio(4)?.precio).isEqualTo(1.40)
        assertThat(station.getPrecio(99)).isNull()
    }

    @Test
    fun `estaAbierto delega correctamente en HorarioParser`() {
        val stationOpen = createStation(horario = "24H")
        val stationClosed = createStation(horario = null)

        assertThat(stationOpen.estaAbierto()).isTrue()
        assertThat(stationClosed.estaAbierto()).isNull()
    }
}
