package com.gas_price_finder.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "municipios",
    foreignKeys = [
        ForeignKey(
            entity = ProvinciaEntity::class,
            parentColumns = ["id"],
            childColumns = ["provinciaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("provinciaId")]
)
data class MunicipioEntity(
    @PrimaryKey
    val id: String,
    val nombre: String,
    val provinciaId: String
)
