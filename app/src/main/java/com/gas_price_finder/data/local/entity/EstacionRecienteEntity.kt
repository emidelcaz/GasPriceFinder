package com.gas_price_finder.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "estaciones_recientes",
    indices = [
        Index("usuarioId"),
        Index("ultimaVisita")
    ]
)
data class EstacionRecienteEntity(
    @PrimaryKey
    val estacionId: String,
    val usuarioId: Int,
    val ultimaVisita: Long = System.currentTimeMillis()
)
