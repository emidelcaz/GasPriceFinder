package com.gas_price_finder.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "productos")
data class ProductoEntity(
    @PrimaryKey
    val id: Int,
    val nombre: String,
    val categoria: String? = null
)
