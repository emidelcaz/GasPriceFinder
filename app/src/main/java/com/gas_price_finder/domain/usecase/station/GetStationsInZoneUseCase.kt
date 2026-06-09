package com.gas_price_finder.domain.usecase.station

import com.gas_price_finder.domain.model.Station
import com.gas_price_finder.domain.repository.StationRepository
import com.gas_price_finder.domain.repository.UserRepository
import com.gas_price_finder.util.GeoUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject

class GetStationsInZoneUseCase @Inject constructor(
    private val stationRepository: StationRepository,
    private val userRepository: UserRepository
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(
        centerLat: Double,
        centerLon: Double,
        viewportRadiusKm: Double
    ): Flow<List<Station>> {
        return userRepository.getActiveUser().flatMapLatest { user ->
            val productoId = user?.combustiblePreferidoId ?: 1
            val box = GeoUtils.calculateBoundingBox(centerLat, centerLon, viewportRadiusKm.toInt())

            stationRepository.getStationsInBoundingBox(
                box.minLat, box.maxLat, box.minLon, box.maxLon
            ).let { stations ->
                kotlinx.coroutines.flow.flowOf(
                    stations.filter {
                        GeoUtils.haversine(
                            centerLat,
                            centerLon,
                            it.latitud,
                            it.longitud
                        ) <= viewportRadiusKm
                    }
                )
            }
        }
    }

}