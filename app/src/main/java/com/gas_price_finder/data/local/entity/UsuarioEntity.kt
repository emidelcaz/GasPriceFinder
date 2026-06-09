package com.gas_price_finder.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "usuarios",
    foreignKeys = [
        ForeignKey(
            entity = ProductoEntity::class,
            parentColumns = ["id"],
            childColumns = ["combustiblePreferidoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("combustiblePreferidoId")]
)
data class UsuarioEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val nombre: String,
    val combustiblePreferidoId: Int? = null,
    val radioBusquedaKm: Int = 10,
    val consumoMedioL100: Double = 7.0,
    val capacidadDepositoL: Double = 50.0,
    val usaAdBlue: Boolean = false,
    val capacidadAdBlueL: Double = 15.0,
    val umbralNotificacionBajada: Double = 0.10,
    val notificacionesActivas: Boolean = true,
    val syncWifi: Boolean = false,
    val tema: String = "SISTEMA",
    val activo: Boolean = false
)
