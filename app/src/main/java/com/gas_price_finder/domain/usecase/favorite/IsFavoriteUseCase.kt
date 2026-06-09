package com.gas_price_finder.domain.usecase.favorite

import com.gas_price_finder.domain.repository.FavoriteRepository
import javax.inject.Inject

class IsFavoriteUseCase @Inject constructor(
    private val repository: FavoriteRepository
) {

    suspend operator fun invoke(usuarioId: Int, estacionId: String): Boolean {
        return repository.isFavorite(usuarioId, estacionId)
    }

}