package com.gas_price_finder.domain.usecase.favorite

import com.gas_price_finder.domain.model.Favorite
import com.gas_price_finder.domain.repository.FavoriteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetFavoritesUseCase @Inject constructor(
    private val repository: FavoriteRepository
) {

    operator fun invoke(usuarioId: Int): Flow<List<Favorite>> {
        return repository.getFavorites(usuarioId)
    }

}