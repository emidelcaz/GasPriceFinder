package com.gas_price_finder.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "precios",
    primaryKeys = ["estacionId", "productoId"],
    foreignKeys = [
        ForeignKey(
            entity = EstacionEntity::class,
            parentColumns = ["ideess"],
            childColumns = ["estacionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ProductoEntity::class,
            parentColumns = ["id"],
            childColumns = ["productoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("productoId")]
)
data class PrecioEntity(
    val estacionId: String,
    val productoId: Int,
    val precio: Double?,
    val fechaActualizacion: Long
)