package com.gas_price_finder.data.repository

import com.gas_price_finder.data.local.dao.FavoritoDao
import com.gas_price_finder.data.local.entity.FavoritoEntity
import com.gas_price_finder.domain.model.Favorite
import com.gas_price_finder.domain.repository.FavoriteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoriteRepositoryImpl @Inject constructor(
    private val favoritoDao: FavoritoDao
) : FavoriteRepository {

    override fun getFavorites(usuarioId: Int): Flow<List<Favorite>> {
        return favoritoDao.getByUsuarioId(usuarioId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun addFavorite(favorite: Favorite): Result<Unit> {
        return try {
            favoritoDao.insert(favorite.toEntity())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeFavorite(usuarioId: Int, estacionId: String): Result<Unit> {
        return try {
            favoritoDao.delete(usuarioId, estacionId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun isFavorite(usuarioId: Int, estacionId: String): Boolean {
        return favoritoDao.isFavorite(usuarioId, estacionId)
    }

    private fun FavoritoEntity.toDomain(): Favorite {
        return Favorite(
            id = this.id,
            usuarioId = this.usuarioId,
            estacionId = this.estacionId,
            nombreSnapshot = this.nombreSnapshot,
            direccionSnapshot = this.direccionSnapshot,
            latitudSnapshot = this.latitudSnapshot,
            longitudSnapshot = this.longitudSnapshot,
            fechaAnadido = this.fechaAnadido
        )
    }

    private fun Favorite.toEntity(): FavoritoEntity {
        return FavoritoEntity(
            id = this.id,
            usuarioId = this.usuarioId,
            estacionId = this.estacionId,
            nombreSnapshot = this.nombreSnapshot,
            direccionSnapshot = this.direccionSnapshot,
            latitudSnapshot = this.latitudSnapshot,
            longitudSnapshot = this.longitudSnapshot,
            fechaAnadido = this.fechaAnadido
        )
    }

}