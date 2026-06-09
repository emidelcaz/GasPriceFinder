package com.gas_price_finder.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.gas_price_finder.data.local.entity.EstacionEntity

@Dao
interface EstacionDao {

    @Query("SELECT * FROM estaciones WHERE ideess = :id")
    suspend fun getById(id: String): EstacionEntity?

    @Query(
        """
        SELECT * FROM estaciones 
        WHERE latitud BETWEEN :minLat AND :maxLat 
        AND longitud BETWEEN :minLon AND :maxLon
    """
    )
    suspend fun getInBoundingBox(
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double
    ): List<EstacionEntity>

    @Query("SELECT * FROM estaciones WHERE ideess IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<EstacionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(estaciones: List<EstacionEntity>)

    @Query("DELETE FROM estaciones")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM estaciones")
    suspend fun getCount(): Int

    @Transaction
    suspend fun replaceAll(estaciones: List<EstacionEntity>) {
        deleteAll()
        insertAll(estaciones)
    }
}