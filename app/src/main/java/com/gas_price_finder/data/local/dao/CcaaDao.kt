package com.gas_price_finder.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gas_price_finder.data.local.entity.CcaaEntity

@Dao
interface CcaaDao {
    @Query("SELECT * FROM ccaa")
    suspend fun getAll(): List<CcaaEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(ccaas: List<CcaaEntity>)
}