package com.gas_price_finder.data.repository

import com.gas_price_finder.domain.repository.SyncRepository
import javax.inject.Inject

class SyncRepositoryImpl @Inject constructor(
    private val stationRepository: StationRepositoryImpl
) : SyncRepository {

    override suspend fun syncAll(): Result<Unit> {
        return stationRepository.syncStations()
    }

}