package com.gas_price_finder.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "historial_precios")
data class HistorialPrecioEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val estacionId: String,
    val productoId: Int,
    val precio: Double,
    val fechaCaptura: Long = System.currentTimeMillis()
)
