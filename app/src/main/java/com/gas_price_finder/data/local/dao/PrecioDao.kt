package com.gas_price_finder.data.local.dao

import androidx.room.*
import com.gas_price_finder.data.local.entity.PrecioEntity

@Dao
interface PrecioDao {
    @Query("SELECT * FROM precios WHERE estacionId = :estacionId")
    suspend fun getByEstacionId(estacionId: String): List<PrecioEntity>

    @Query(
        """
        SELECT * FROM precios 
        WHERE estacionId = :estacionId 
        AND productoId = :productoId
    """
    )
    suspend fun getByEstacionAndProducto(
        estacionId: String,
        productoId: Int
    ): PrecioEntity?

    @Query(
        """
        SELECT p.* FROM precios p
        INNER JOIN estaciones e ON p.estacionId = e.ideess
        WHERE p.productoId = :productoId
        AND e.latitud BETWEEN :minLat AND :maxLat
        AND e.longitud BETWEEN :minLon AND :maxLon
    """
    )
    suspend fun getPricesInArea(
        productoId: Int,
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double
    ): List<PrecioEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(precios: List<PrecioEntity>)

    @Query("DELETE FROM precios")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(precios: List<PrecioEntity>) {
        deleteAll()
        insertAll(precios)
    }

    @Query("SELECT COUNT(*) FROM precios")
    suspend fun getCount(): Int

}