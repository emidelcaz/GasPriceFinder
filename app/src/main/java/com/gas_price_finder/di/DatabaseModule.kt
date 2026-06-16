package com.gas_price_finder.data.local.database

import android.content.Context
import com.gas_price_finder.util.AppLogger
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.gas_price_finder.R
import com.gas_price_finder.data.local.dao.CcaaDao
import com.gas_price_finder.data.local.dao.EstacionDao
import com.gas_price_finder.data.local.dao.EstacionRecienteDao
import com.gas_price_finder.data.local.dao.FavoritoDao
import com.gas_price_finder.data.local.dao.HistorialPrecioDao
import com.gas_price_finder.data.local.dao.LocalidadDao
import com.gas_price_finder.data.local.dao.MunicipioDao
import com.gas_price_finder.data.local.dao.PrecioDao
import com.gas_price_finder.data.local.dao.ProductoDao
import com.gas_price_finder.data.local.dao.ProvinciaDao
import com.gas_price_finder.data.local.dao.UsuarioDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private const val TAG = "DatabaseModule"

    // ===== SINGLETON (con doble verificacion) =====
    // Garantiza que solo existe UNA instancia de la base de datos
    // en toda la aplicación, incluso si Hilt falla en el scope.
    @Volatile
    private var INSTANCE: GasPriceDatabase? = null

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        appLogger: AppLogger
    ): GasPriceDatabase {
        appLogger.d(TAG, ">>> provideDatabase llamado. INSTANCE=${INSTANCE != null}")

        return INSTANCE ?: synchronized(this) {
            val existing = INSTANCE
            if (existing != null) {
                appLogger.d(TAG, ">>> Retornando instancia existente")
                existing
            } else {
                appLogger.d(TAG, ">>> CREANDO NUEVA instancia de base de datos")
                val newInstance = Room.databaseBuilder(
                    context.applicationContext,
                    GasPriceDatabase::class.java,
                    "gasprice_database"
                )
                    .addCallback(createImportCallback(context))
                    .build()
                INSTANCE = newInstance
                appLogger.d(TAG, ">>> INSTANCIA CREADA y cacheada")
                newInstance
            }
        }
    }

    @Provides
    fun provideCcaaDao(db: GasPriceDatabase): CcaaDao = db.ccaaDao()

    @Provides
    fun provideEstacionDao(db: GasPriceDatabase): EstacionDao = db.estacionDao()

    @Provides
    fun provideEstacionRecienteDao(db: GasPriceDatabase): EstacionRecienteDao =
        db.estacionRecienteDao()

    @Provides
    fun provideFavoritoDao(db: GasPriceDatabase): FavoritoDao = db.favoritoDao()

    @Provides
    fun provideHistorialPrecioDao(db: GasPriceDatabase): HistorialPrecioDao =
        db.historialPrecioDao()

    @Provides
    fun provideLocalidadDao(db: GasPriceDatabase): LocalidadDao = db.localidadDao()

    @Provides
    fun provideMunicipioDao(db: GasPriceDatabase): MunicipioDao = db.municipioDao()

    @Provides
    fun providePrecioDao(db: GasPriceDatabase): PrecioDao = db.precioDao()

    @Provides
    fun provideProductoDao(db: GasPriceDatabase): ProductoDao = db.productoDao()

    @Provides
    fun provideProvinciaDao(db: GasPriceDatabase): ProvinciaDao = db.provinciaDao()

    @Provides
    fun provideUsuarioDao(db: GasPriceDatabase): UsuarioDao = db.usuarioDao()

    private fun createImportCallback(context: Context): RoomDatabase.Callback {
        return object : RoomDatabase.Callback() {
            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)

                val prefs = context.getSharedPreferences("db_import_prefs", Context.MODE_PRIVATE)
                val yaImportado = prefs.getBoolean("historial_importado", false)

                if (!yaImportado) {
                    importarHistorialDesdeSql(context, db)
                    prefs.edit().putBoolean("historial_importado", true).apply()
                }
            }
        }
    }

    private fun importarHistorialDesdeSql(context: Context, db: SupportSQLiteDatabase) {
        val inputStream = context.resources.openRawResource(R.raw.historial_precios_16062026)
        val reader = inputStream.bufferedReader()

        db.beginTransaction()
        try {
            reader.useLines { lines ->
                lines.forEach { line ->
                    val sql = line.trim()
                    if (sql.isNotEmpty() && !sql.startsWith("--") && !sql.startsWith("/*")) {
                        db.execSQL(sql)
                    }
                }
            }
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.endTransaction()
        }
    }
}