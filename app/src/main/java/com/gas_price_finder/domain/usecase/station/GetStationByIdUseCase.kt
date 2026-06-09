package com.gas_price_finder.domain.usecase.station

import com.gas_price_finder.domain.model.Station
import com.gas_price_finder.domain.repository.StationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetStationByIdUseCase @Inject constructor(
    private val repository: StationRepository
) {

    operator fun invoke(ideess: String): Flow<Station?> {
        return repository.getStationById(ideess)
    }

}