package com.gas_price_finder.domain.usecase.station

import com.gas_price_finder.domain.model.Station
import com.gas_price_finder.domain.repository.StationRepository
import com.gas_price_finder.domain.repository.UserRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject

class GetNearbyStationsUseCase @Inject constructor(
    private val stationRepository: StationRepository,
    private val userRepository: UserRepository
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(
        latitud: Double,
        longitud: Double
    ): Flow<List<Station>> {
        return userRepository.getActiveUser().flatMapLatest { user ->
            val radioKm = user?.radioBusquedaKm ?: 10
            val productoId = user?.combustiblePreferidoId ?: 1
            stationRepository.getNearbyStations(latitud, longitud, radioKm, productoId)
        }
    }
}