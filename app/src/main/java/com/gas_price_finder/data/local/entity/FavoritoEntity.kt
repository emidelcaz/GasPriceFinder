package com.gas_price_finder.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "favoritos",
    indices = [
        Index(value = ["usuarioId", "estacionId"], unique = true)
    ]
)
data class FavoritoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val usuarioId: Int,
    val estacionId: String,
    val nombreSnapshot: String,
    val direccionSnapshot: String?,
    val latitudSnapshot: Double?,
    val longitudSnapshot: Double?,
    val fechaAnadido: Long = System.currentTimeMillis()
)
