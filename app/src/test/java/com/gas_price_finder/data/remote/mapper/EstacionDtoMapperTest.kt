package com.gas_price_finder.data.remote.mapper

import com.gas_price_finder.data.remote.dto.EstacionDto
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class EstacionDtoMapperTest {

    @Test
    fun `mapea coordenadas con coma decimal correctamente`() {
        val dto = EstacionDto(
            ideess = "1234",
            idMunicipio = "28001",
            idProvincia = "28",
            idCcaa = "13",
            rotulo = "Test Station",
            direccion = "Calle Falsa 123",
            cp = "28001",
            municipio = "Madrid",
            provincia = "Madrid",
            localidad = "Madrid",
            latitud = "40,4168",
            longitud = "-3,7038",
            horario = "L-V:08:00-20:00",
            tipoVenta = "P",
            remision = "dm",
            margen = "I",
            precioGasoleoA = null,
            precioGasoleoB = null,
            precioGasoleoPremium = null,
            precioGasolina95E5 = "1,599",
            precioGasolina95E10 = null,
            precioGasolina98E5 = null,
            precioGasolina98E10 = null,
            precioGasolina95E5Premium = null,
            precioGasolina98E5Premium = null,
            precioBiodiesel = null,
            precioBioetanol = null,
            precioGlp = null,
            precioGnc = null,
            precioGnl = null,
            precioHidrogeno = null,
            precioAdBlue = null,
            bioEtanol = null,
            esterMetilico = null
        )

        val entity = EstacionDtoMapper.toEstacionEntity(dto)
        assertThat(entity.latitud).isWithin(0.0001).of(40.4168)
        assertThat(entity.longitud).isWithin(0.0001).of(-3.7038)
    }

    @Test
    fun `filtra precios nulos o vacios y mantiene los validos`() {
        val dto = EstacionDto(
            ideess = "5678",
            idMunicipio = "28001",
            idProvincia = "28",
            idCcaa = "13",
            rotulo = "Test",
            direccion = "Dir",
            cp = "28001",
            municipio = "Madrid",
            provincia = "Madrid",
            localidad = "Madrid",
            latitud = "40,0",
            longitud = "-3,0",
            horario = "24H",
            tipoVenta = "P",
            remision = "dm",
            margen = "D",
            precioGasoleoA = "1,450",
            precioGasoleoB = "",
            precioGasoleoPremium = null,
            precioGasolina95E5 = "0",
            precioGasolina95E10 = "1,550",
            precioGasolina98E5 = "1,700",
            precioGasolina98E10 = null,
            precioGasolina95E5Premium = null,
            precioGasolina98E5Premium = null,
            precioBiodiesel = null,
            precioBioetanol = null,
            precioGlp = null,
            precioGnc = null,
            precioGnl = null,
            precioHidrogeno = null,
            precioAdBlue = null,
            bioEtanol = null,
            esterMetilico = null
        )

        val precios = EstacionDtoMapper.toPrecioEntities(dto)
        val productoIds = precios.map { it.productoId }

        // Gasoleo A (4) y Gasolina 95 E10 (23) y Gasolina 98 E5 (3) son válidos
        assertThat(productoIds).containsExactly(4, 23, 3)
        // Verificar que el precio 0 no se incluyó (Gasolina 95 E5 = 1)
        assertThat(productoIds).doesNotContain(1)
    }
}
