package com.gas_price_finder.data.local.dao

import androidx.room.*
import com.gas_price_finder.data.local.entity.EstacionRecienteEntity

@Dao
interface EstacionRecienteDao {

    @Query("""
        SELECT * FROM estaciones_recientes 
        WHERE usuarioId = :usuarioId
        ORDER BY ultimaVisita DESC
        LIMIT :limit
    """)
    suspend fun getRecientesByUsuario(
        usuarioId: Int,
        limit: Int = 50
    ): List<EstacionRecienteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reciente: EstacionRecienteEntity)

    @Query("""
        DELETE FROM estaciones_recientes 
        WHERE ultimaVisita < :fechaLimite
    """)
    suspend fun deleteOlderThan(fechaLimite: Long)
}