package com.gas_price_finder.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gas_price_finder.data.local.entity.LocalidadEntity

@Dao
interface LocalidadDao {
    @Query("SELECT * FROM localidades")
    suspend fun getAll(): List<LocalidadEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(localidades: List<LocalidadEntity>)
}