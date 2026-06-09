package com.gas_price_finder.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gas_price_finder.data.local.entity.ProductoEntity

@Dao
interface ProductoDao {
    @Query("SELECT * FROM productos")
    suspend fun getAll(): List<ProductoEntity>

    @Query("SELECT * FROM productos WHERE id = :id")
    suspend fun getById(id: Int): ProductoEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(productos: List<ProductoEntity>)
}