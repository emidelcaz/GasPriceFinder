package com.gas_price_finder.domain.usecase.station

import com.gas_price_finder.domain.model.*
import com.gas_price_finder.domain.repository.StationRepository
import com.gas_price_finder.domain.repository.UserRepository
import com.gas_price_finder.util.BoundingBox
import com.gas_price_finder.util.GeoUtils
import javax.inject.Inject
import kotlin.math.abs

class GetOptimalStationInRouteUseCase @Inject constructor(
    private val stationRepository: StationRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(
        origenLat: Double,
        origenLon: Double,
        heading: Float,
        autonomiaKm: Double,
        requiereAdBlue: Boolean
    ): List<RouteCalculation> {
        val user = userRepository.getActiveUserSync() ?: return emptyList()
        val productoId = user.combustiblePreferidoId ?: return emptyList()

        val sector = calculateSector(origenLat, origenLon, heading, autonomiaKm)
        val estaciones = stationRepository.getStationsInBoundingBox(
            sector.minLat, sector.maxLat, sector.minLon, sector.maxLon
        )

        val estacionesFiltradas = estaciones.filter { station ->
            val bearing =
                GeoUtils.calculateBearing(origenLat, origenLon, station.latitud, station.longitud)
            val angleDiff = normalizeAngle(bearing - heading)
            abs(angleDiff) <= 30 &&
                    GeoUtils.haversine(
                        origenLat,
                        origenLon,
                        station.latitud,
                        station.longitud
                    ) <= autonomiaKm &&
                    (!requiereAdBlue || station.getPrecio(26) != null)
        }

        // CÁLCULO MEDIA DEL ÁREA (usado para el ahorro de cada estación)
        val preciosEnSector = estacionesFiltradas.mapNotNull {
            it.getPrecio(productoId)?.precio
        }
        val mediaArea = if (preciosEnSector.isNotEmpty()) preciosEnSector.average() else 0.0

        return estacionesFiltradas.mapNotNull { station ->
            val distancia = GeoUtils.haversine(origenLat, origenLon, station.latitud, station.longitud)
            val precio = station.getPrecio(productoId)?.precio ?: return@mapNotNull null

            val litros = (distancia / 100) * user.consumoMedioL100
            val costeCombustible = litros * precio

            val costeAdBlue = if (requiereAdBlue) {
                val proporcion = user.capacidadAdBlueL / user.capacidadDepositoL
                litros * proporcion * (station.getPrecio(26)?.precio ?: 0.0)
            } else 0.0

            val ahorroPorLitro = mediaArea - precio
            val ahorroVsMedia = ahorroPorLitro * user.capacidadDepositoL

            RouteCalculation(
                estacionOptima = station,
                distanciaDesvioKm = distancia,
                costeTotal = costeCombustible + costeAdBlue,
                costeCombustible = costeCombustible,
                costeAdBlue = costeAdBlue,
                costeDesvio = 0.0,
                ahorroVsMedia = ahorroVsMedia,
                autonomiaKm = autonomiaKm
            )
        }.sortedBy { it.costeTotal }
    }

    private fun calculateSector(
        lat: Double, lon: Double,
        heading: Float, distanceKm: Double
    ): BoundingBox {
        val latDelta = distanceKm / 111.0
        val lonDelta = distanceKm / (111.0 * kotlin.math.cos(Math.toRadians(lat)))
        return BoundingBox(
            minLat = lat - latDelta,
            maxLat = lat + latDelta,
            minLon = lon - lonDelta,
            maxLon = lon + lonDelta
        )
    }

    private fun normalizeAngle(angle: Float): Float {
        var result = angle
        while (result > 180) result -= 360
        while (result < -180) result += 360
        return result
    }
}