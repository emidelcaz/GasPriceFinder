package com.gas_price_finder.domain.usecase.price

import com.gas_price_finder.domain.model.PriceHistory
import com.gas_price_finder.domain.repository.PriceHistoryRepository
import javax.inject.Inject

class GetPriceHistoryUseCase @Inject constructor(
    private val repository: PriceHistoryRepository
) {

    suspend operator fun invoke(estacionId: String, productoId: Int): List<PriceHistory> {
        return repository.getHistory(estacionId, productoId)
    }

}