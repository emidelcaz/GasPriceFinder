package com.gas_price_finder.data.local.dao

import androidx.room.*
import com.gas_price_finder.data.local.entity.FavoritoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoritoDao {
    @Query(
        """
        SELECT * FROM favoritos 
        WHERE usuarioId = :usuarioId 
        ORDER BY fechaAnadido DESC
    """
    )
    fun getByUsuarioId(usuarioId: Int): Flow<List<FavoritoEntity>>

    @Query(
        """
        SELECT * FROM favoritos 
        WHERE usuarioId = :usuarioId 
        AND estacionId = :estacionId
    """
    )
    suspend fun getByUsuarioAndEstacion(
        usuarioId: Int,
        estacionId: String
    ): FavoritoEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(favorito: FavoritoEntity)

    @Query(
        """
        DELETE FROM favoritos 
        WHERE usuarioId = :usuarioId 
        AND estacionId = :estacionId
    """
    )
    suspend fun delete(usuarioId: Int, estacionId: String)

    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM favoritos 
            WHERE usuarioId = :usuarioId 
            AND estacionId = :estacionId
        )
    """
    )
    suspend fun isFavorite(usuarioId: Int, estacionId: String): Boolean
}