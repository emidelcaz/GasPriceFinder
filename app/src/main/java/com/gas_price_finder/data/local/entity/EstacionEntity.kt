package com.gas_price_finder.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "estaciones",
    foreignKeys = [
        ForeignKey(
            entity = MunicipioEntity::class,
            parentColumns = ["id"],
            childColumns = ["municipioId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = LocalidadEntity::class,
            parentColumns = ["id"],
            childColumns = ["localidadId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("municipioId"),
        Index("localidadId"),
        Index(value = ["latitud", "longitud"])
    ]
)
data class EstacionEntity(

    @PrimaryKey
    val ideess: String,
    val rotulo: String,
    val direccion: String?,
    val cp: String?,
    val latitud: Double,
    val longitud: Double,
    val municipioId: String?,
    val localidadId: String?,
    val horario: String?,
    val tipoEstacion: String?,
    val tipoVenta: String?,
    val fechaActualizacion: Long?
)
