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
        autonomiaKm: Double
    ): RouteCalculation? {
        val user = userRepository.getActiveUserSync() ?: return null
        val productoId = user.combustiblePreferidoId ?: return null

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
                    ) <= autonomiaKm
        }

        val optima = estacionesFiltradas.minByOrNull {
            it.getPrecio(productoId)?.precio ?: Double.MAX_VALUE
        } ?: return null

        val distancia = GeoUtils.haversine(origenLat, origenLon, optima.latitud, optima.longitud)
        val precio = optima.getPrecio(productoId)?.precio ?: return null

        val litros = (distancia / 100) * user.consumoMedioL100
        val costeCombustible = litros * precio

        val costeAdBlue = if (user.usaAdBlue) {
            val proporcion = user.capacidadAdBlueL / user.capacidadDepositoL
            litros * proporcion * (optima.getPrecio(26)?.precio ?: 0.0)
        } else 0.0

        // CÁLCULO AHORRO VS MEDIA DEL ÁREA
        val preciosEnSector = estacionesFiltradas.mapNotNull {
            it.getPrecio(productoId)?.precio
        }
        val mediaArea = if (preciosEnSector.isNotEmpty()) {
            preciosEnSector.average()
        } else {
            precio
        }
        val ahorroPorLitro = mediaArea - precio
        val ahorroVsMedia = ahorroPorLitro * user.capacidadDepositoL

        return RouteCalculation(
            estacionOptima = optima,
            distanciaDesvioKm = distancia,
            costeTotal = costeCombustible + costeAdBlue,
            costeCombustible = costeCombustible,
            costeAdBlue = costeAdBlue,
            costeDesvio = 0.0,
            ahorroVsMedia = ahorroVsMedia,
            autonomiaKm = autonomiaKm
        )
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