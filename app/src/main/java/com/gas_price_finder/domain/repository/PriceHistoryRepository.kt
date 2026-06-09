package com.gas_price_finder.domain.repository

import com.gas_price_finder.domain.model.PriceHistory

interface PriceHistoryRepository {

    suspend fun getHistory(estacionId: String, productoId: Int): List<PriceHistory>

    suspend fun savePriceSnapshot(estacionId: String, productoId: Int, precio: Double): Result<Unit>

    suspend fun cleanOldHistory(daysToKeep: Long): Result<Unit>

}