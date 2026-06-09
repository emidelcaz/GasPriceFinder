package com.gas_price_finder.domain.usecase.sync

import com.gas_price_finder.domain.repository.StationRepository
import javax.inject.Inject

class SyncStationsUseCase @Inject constructor(
    private val repository: StationRepository
) {

    suspend operator fun invoke(): Result<Unit> {
        return repository.syncStations()
    }

}