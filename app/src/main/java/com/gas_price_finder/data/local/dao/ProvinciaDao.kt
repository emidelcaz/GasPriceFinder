package com.gas_price_finder.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gas_price_finder.data.local.entity.ProvinciaEntity

@Dao
interface ProvinciaDao {
    @Query("SELECT * FROM provincias")
    suspend fun getAll(): List<ProvinciaEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(provincias: List<ProvinciaEntity>)
}