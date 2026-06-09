package com.gas_price_finder.domain.usecase.favorite

import com.gas_price_finder.domain.model.Favorite
import com.gas_price_finder.domain.repository.FavoriteRepository
import javax.inject.Inject

class AddFavoriteUseCase @Inject constructor(
    private val repository: FavoriteRepository
) {

    suspend operator fun invoke(favorite: Favorite): Result<Unit> {
        return repository.addFavorite(favorite)
    }

}