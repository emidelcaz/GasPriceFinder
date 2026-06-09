package com.gas_price_finder.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.gas_price_finder.data.local.dao.*
import com.gas_price_finder.data.local.database.converters.DateConverters
import com.gas_price_finder.data.local.entity.*

@Database(
    entities = [
        CcaaEntity::class,
        EstacionEntity::class,
        EstacionRecienteEntity::class,
        FavoritoEntity::class,
        HistorialPrecioEntity::class,
        LocalidadEntity::class,
        MunicipioEntity::class,
        PrecioEntity::class,
        ProductoEntity::class,
        ProvinciaEntity::class,
        UsuarioEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(DateConverters::class)
abstract class GasPriceDatabase : RoomDatabase() {
    abstract fun ccaaDao(): CcaaDao
    abstract fun estacionDao(): EstacionDao
    abstract fun estacionRecienteDao(): EstacionRecienteDao
    abstract fun favoritoDao(): FavoritoDao
    abstract fun historialPrecioDao(): HistorialPrecioDao
    abstract fun localidadDao(): LocalidadDao
    abstract fun municipioDao(): MunicipioDao
    abstract fun precioDao(): PrecioDao
    abstract fun productoDao(): ProductoDao
    abstract fun usuarioDao(): UsuarioDao
    abstract fun provinciaDao(): ProvinciaDao
}