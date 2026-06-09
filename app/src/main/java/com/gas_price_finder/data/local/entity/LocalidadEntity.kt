package com.gas_price_finder.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "localidades",
    foreignKeys = [
        ForeignKey(
            entity = MunicipioEntity::class,
            parentColumns = ["id"],
            childColumns = ["municipioId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("municipioId")]
)
data class LocalidadEntity(
    @PrimaryKey
    val id: String,
    val nombre: String,
    val municipioId: String
)
