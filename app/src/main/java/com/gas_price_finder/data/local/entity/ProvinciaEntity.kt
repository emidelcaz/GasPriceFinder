package com.gas_price_finder.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "provincias",
    foreignKeys = [
        ForeignKey(
            entity = CcaaEntity::class,
            parentColumns = ["id"],
            childColumns = ["ccaaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("ccaaId")]
)
data class ProvinciaEntity(
    @PrimaryKey
    val id: String,
    val nombre: String,
    val ccaaId: String
)