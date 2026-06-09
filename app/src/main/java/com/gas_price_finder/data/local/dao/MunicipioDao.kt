package com.gas_price_finder.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gas_price_finder.data.local.entity.MunicipioEntity

@Dao
interface MunicipioDao {
    @Query("SELECT * FROM municipios")
    suspend fun getAll(): List<MunicipioEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(municipios: List<MunicipioEntity>)

    @Query("SELECT nombre FROM municipios WHERE id = :id")
    suspend fun getMunicipioNameById(id: Int): String

}