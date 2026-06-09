package com.gas_price_finder.domain.repository

interface SyncRepository {

    suspend fun syncAll(): Result<Unit>

}
