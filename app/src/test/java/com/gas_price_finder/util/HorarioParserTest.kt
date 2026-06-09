package com.gas_price_finder.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDateTime

class HorarioParserTest {

    @Test
    fun `estaAbierto devuelve true para horario 24H`() {
        assertThat(HorarioParser.estaAbierto("24H")).isTrue()
    }

    @Test
    fun `estaAbierto devuelve correcto para horario L-V dentro de hora`() {
        // Lunes 12:00, horario L-V:08:00-20:00
        val ahora = LocalDateTime.of(2024, 6, 3, 12, 0) // Lunes
        assertThat(HorarioParser.estaAbierto("L-V:08:00-20:00", ahora)).isTrue()
    }

    @Test
    fun `estaAbierto devuelve false para horario L-V fuera de hora`() {
        // Lunes 22:00, horario L-V:08:00-20:00
        val ahora = LocalDateTime.of(2024, 6, 3, 22, 0) // Lunes
        assertThat(HorarioParser.estaAbierto("L-V:08:00-20:00", ahora)).isFalse()
    }

    @Test
    fun `estaAbierto maneja horario nocturno con cruce de medianoche`() {
        // Horario nocturno: 22:00 a 06:00. A las 23:00 debe estar abierto.
        val ahora = LocalDateTime.of(2024, 6, 3, 23, 0)
        assertThat(HorarioParser.estaAbierto("L-D:22:00-06:00", ahora)).isTrue()
    }

    @Test
    fun `estaAbierto devuelve null para horario nulo o vacio`() {
        assertThat(HorarioParser.estaAbierto(null)).isNull()
        assertThat(HorarioParser.estaAbierto("")).isNull()
        assertThat(HorarioParser.estaAbierto("   ")).isNull()
    }

    @Test
    fun `parseSchedule genera lista completa para 24H`() {
        val schedule = HorarioParser.parseSchedule("24H")
        assertThat(schedule).hasSize(7)
        schedule.forEach {
            assertThat(it.ranges).containsExactly("24h")
        }
    }

    @Test
    fun `parseSchedule asigna rangos correctos por dia`() {
        val schedule = HorarioParser.parseSchedule("L-V:08:00-20:00;S:09:00-14:00")
        val lunes = schedule.find { it.dayName == "Lunes" }
        val sabado = schedule.find { it.dayName == "Sábado" }
        val domingo = schedule.find { it.dayName == "Domingo" }

        assertThat(lunes?.ranges).containsExactly("08:00-20:00")
        assertThat(sabado?.ranges).containsExactly("09:00-14:00")
        assertThat(domingo?.ranges).isEmpty()
    }
}
