package com.gas_price_finder.data.local.dao

import androidx.room.*
import com.gas_price_finder.data.local.entity.HistorialPrecioEntity

@Dao
interface HistorialPrecioDao {

    @Query(
        """
        SELECT * FROM historial_precios 
        WHERE estacionId = :estacionId 
        AND productoId = :productoId
        ORDER BY fechaCaptura DESC
        LIMIT :limit
    """
    )
    suspend fun getByEstacionAndProducto(
        estacionId: String,
        productoId: Int,
        limit: Int = 100
    ): List<HistorialPrecioEntity>

    @Insert
    suspend fun insert(historial: HistorialPrecioEntity)

    @Insert
    suspend fun insertAll(historiales: List<HistorialPrecioEntity>)

    @Query(
        """
        DELETE FROM historial_precios 
        WHERE fechaCaptura < :fechaLimite
    """
    )
    suspend fun deleteOlderThan(fechaLimite: Long)

    @Query("""
        SELECT h.* FROM historial_precios h
        INNER JOIN (
            SELECT estacionId, productoId, MAX(fechaCaptura) as maxFecha
            FROM historial_precios
            GROUP BY estacionId, productoId
        ) latest ON h.estacionId = latest.estacionId
                AND h.productoId = latest.productoId
                AND h.fechaCaptura = latest.maxFecha
    """)
    suspend fun getLatestPrices(): List<HistorialPrecioEntity>
}