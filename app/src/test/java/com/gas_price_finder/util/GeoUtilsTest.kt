package com.gas_price_finder.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GeoUtilsTest {

    @Test
    fun `haversine calcula distancia aproximada entre Madrid y Barcelona`() {
        val distancia = GeoUtils.haversine(40.4168, -3.7038, 41.3851, 2.1734)
        // Distancia real ~505 km. Permitiendo margen de 5 km por redondeos.
        assertThat(distancia).isWithin(5.0).of(505.0)
    }

    @Test
    fun `calculateBoundingBox genera limites correctos para radio de 5 km`() {
        val box = GeoUtils.calculateBoundingBox(40.0, -3.0, 5)
        assertThat(box.minLat).isLessThan(40.0)
        assertThat(box.maxLat).isGreaterThan(40.0)
        assertThat(box.minLon).isLessThan(-3.0)
        assertThat(box.maxLon).isGreaterThan(-3.0)
        // El delta de latitud es aproximadamente 5/111 = 0.045
        assertThat(box.maxLat - box.minLat).isWithin(0.001).of(0.090)
    }

    @Test
    fun `calculateBearing devuelve rumbo aproximado hacia el norte`() {
        // Del ecuador al polo norte
        val bearing = GeoUtils.calculateBearing(0.0, 0.0, 90.0, 0.0)
        assertThat(bearing).isWithin(1.0f).of(0.0f)
    }

    @Test
    fun `zoomLevelForRadius devuelve valores esperados para distintos radios`() {
        assertThat(GeoUtils.zoomLevelForRadius(1)).isEqualTo(15f)
        assertThat(GeoUtils.zoomLevelForRadius(5)).isEqualTo(13f)
        assertThat(GeoUtils.zoomLevelForRadius(20)).isEqualTo(11f)
        assertThat(GeoUtils.zoomLevelForRadius(100)).isEqualTo(9f)
    }
}
