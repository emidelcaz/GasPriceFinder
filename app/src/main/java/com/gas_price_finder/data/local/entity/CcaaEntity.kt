package com.gas_price_finder.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ccaa")
data class CcaaEntity(
    @PrimaryKey
    val id: String,
    val nombre: String
)
