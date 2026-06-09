package com.gas_price_finder.util

import android.content.Context
import com.gas_price_finder.R
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object HorarioParser {

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun estaAbierto(horario: String?, ahora: LocalDateTime = LocalDateTime.now()): Boolean? {

        if (horario.isNullOrBlank()) return null

        if (horario.contains("24H", ignoreCase = true)) return true

        return try {
            parseHorarioComplejo(horario, ahora)
        } catch (e: Exception) {
            null
        }
    }

    private fun parseHorarioComplejo(horario: String, ahora: LocalDateTime): Boolean? {

        val rangos = horario.split(";")
        for (rango in rangos) {
            val resultado = parseRangoIndividual(rango.trim(), ahora)
            if (resultado != null) return resultado
        }
        return null
    }

    private fun parseRangoIndividual(rango: String, ahora: LocalDateTime): Boolean? {

        val partes = rango.split(":", limit = 2)
        if (partes.size > 2) return null

        val diasStr = partes[0].trim()
        val horasStr = partes[1].trim()

        if (!diaEstaIncluido(diasStr, ahora.dayOfWeek)) return null

        val horas = horasStr.split("-")
        if (horas.size != 2) return null

        val horaApertura = parseHora(horas[0].trim()) ?: return null
        val horaCierre = parseHora(horas[1].trim()) ?: return null

        val horaActual = ahora.toLocalTime()

        return if (horaCierre.isBefore(horaApertura)) {
            horaActual.isAfter(horaApertura) || horaActual.isBefore(horaCierre)
        } else {
            horaActual.isAfter(horaApertura) && horaActual.isBefore(horaCierre)
        }
    }

    private fun diaEstaIncluido(diasStr: String, diaActual: DayOfWeek): Boolean {

        val dias = diasStr.uppercase(Locale.getDefault())
        val diaActualChar = when (diaActual) {
            DayOfWeek.MONDAY -> "L"
            DayOfWeek.TUESDAY -> "M"
            DayOfWeek.WEDNESDAY -> "X"
            DayOfWeek.THURSDAY -> "J"
            DayOfWeek.FRIDAY -> "V"
            DayOfWeek.SATURDAY -> "S"
            DayOfWeek.SUNDAY -> "D"
        }

        return when {
            dias == "L-D" -> true
            dias == "L-V" -> diaActual in DayOfWeek.MONDAY..DayOfWeek.FRIDAY
            dias == "S-D" -> diaActual in DayOfWeek.SATURDAY..DayOfWeek.SUNDAY
            dias.contains(diaActualChar) -> true
            else -> false
        }
    }

    private fun parseHora(horaStr: String): LocalTime? {
        return try {
            LocalTime.parse(horaStr, timeFormatter)
        } catch (e: Exception) {
            null
        }
    }

    fun parseSchedule(horario: String?): List<DaySchedule> {
        return parseScheduleInternal(horario, ::dayName)
    }

    fun parseSchedule(context: Context, horario: String?): List<DaySchedule> {
        return parseScheduleInternal(horario) { day -> dayName(context, day) }
    }

    private fun parseScheduleInternal(
        horario: String?,
        dayNameResolver: (DayOfWeek) -> String
    ): List<DaySchedule> {
        if (horario.isNullOrBlank()) {
            return emptyList()
        }

        if (horario.contains("24H", ignoreCase = true)) {
            return DayOfWeek.entries.map {
                DaySchedule(
                    dayName = dayNameResolver(it),
                    ranges = listOf("24h")
                )
            }
        }

        val dayRanges = mutableMapOf<DayOfWeek, MutableList<String>>()
        DayOfWeek.entries.forEach { dayRanges[it] = mutableListOf() }

        val segments = horario.split(";")
        for (segment in segments) {
            val trimmed = segment.trim()
            if (trimmed.isEmpty()) continue

            val parts = trimmed.split(":", limit = 2)
            if (parts.size < 2) continue

            val daysPart = parts[0].trim()
            val hoursPart = parts[1].trim()

            val affectedDays = expandDays(daysPart)
            val ranges = hoursPart.split(",").map { it.trim() }.filter { it.isNotEmpty() }

            for (day in affectedDays) {
                dayRanges[day]?.addAll(ranges)
            }
        }

        return DayOfWeek.entries.map {
            DaySchedule(
                dayName = dayNameResolver(it),
                ranges = dayRanges[it] ?: emptyList()
            )
        }
    }

    private fun expandDays(daysStr: String): List<DayOfWeek> {
        val s = daysStr.uppercase(Locale.getDefault())
        return when {
            s == "L-D" -> DayOfWeek.entries
            s == "L-V" -> DayOfWeek.entries.filter { it in DayOfWeek.MONDAY..DayOfWeek.FRIDAY }
            s == "S-D" -> DayOfWeek.entries.filter { it in DayOfWeek.SATURDAY..DayOfWeek.SUNDAY }
            else -> {
                val dayChars = s.toCharArray().filter { it in listOf('L', 'M', 'X', 'J', 'V', 'S', 'D') }
                dayChars.mapNotNull { char ->
                    when (char) {
                        'L' -> DayOfWeek.MONDAY
                        'M' -> DayOfWeek.TUESDAY
                        'X' -> DayOfWeek.WEDNESDAY
                        'J' -> DayOfWeek.THURSDAY
                        'V' -> DayOfWeek.FRIDAY
                        'S' -> DayOfWeek.SATURDAY
                        'D' -> DayOfWeek.SUNDAY
                        else -> null
                    }
                }
            }
        }
    }

    private fun dayName(day: DayOfWeek): String {
        return when (day) {
            DayOfWeek.MONDAY -> "Lunes"
            DayOfWeek.TUESDAY -> "Martes"
            DayOfWeek.WEDNESDAY -> "Miércoles"
            DayOfWeek.THURSDAY -> "Jueves"
            DayOfWeek.FRIDAY -> "Viernes"
            DayOfWeek.SATURDAY -> "Sábado"
            DayOfWeek.SUNDAY -> "Domingo"
        }
    }

    private fun dayName(context: Context, day: DayOfWeek): String {
        return when (day) {
            DayOfWeek.MONDAY -> context.getString(R.string.day_monday)
            DayOfWeek.TUESDAY -> context.getString(R.string.day_tuesday)
            DayOfWeek.WEDNESDAY -> context.getString(R.string.day_wednesday)
            DayOfWeek.THURSDAY -> context.getString(R.string.day_thursday)
            DayOfWeek.FRIDAY -> context.getString(R.string.day_friday)
            DayOfWeek.SATURDAY -> context.getString(R.string.day_saturday)
            DayOfWeek.SUNDAY -> context.getString(R.string.day_sunday)
        }
    }
}

data class DaySchedule(
    val dayName: String,
    val ranges: List<String>
)