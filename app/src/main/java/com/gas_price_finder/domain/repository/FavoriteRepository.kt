package com.gas_price_finder.domain.repository

import com.gas_price_finder.domain.model.Favorite
import kotlinx.coroutines.flow.Flow

interface FavoriteRepository {

    fun getFavorites(usuarioId: Int): Flow<List<Favorite>>

    suspend fun addFavorite(favorite: Favorite): Result<Unit>

    suspend fun removeFavorite(usuarioId: Int, estacionId: String): Result<Unit>

    suspend fun isFavorite(usuarioId: Int, estacionId: String): Boolean

}