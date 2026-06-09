package com.gas_price_finder.data.repository

import androidx.room.withTransaction
import com.gas_price_finder.data.local.dao.CcaaDao
import com.gas_price_finder.data.local.dao.EstacionDao
import com.gas_price_finder.data.local.dao.LocalidadDao
import com.gas_price_finder.data.local.dao.MunicipioDao
import com.gas_price_finder.data.local.dao.PrecioDao
import com.gas_price_finder.data.local.dao.HistorialPrecioDao
import com.gas_price_finder.data.local.dao.ProductoDao
import com.gas_price_finder.data.local.dao.ProvinciaDao
import com.gas_price_finder.data.local.dao.UsuarioDao
import com.gas_price_finder.data.local.database.GasPriceDatabase
import com.gas_price_finder.data.local.entity.EstacionEntity
import com.gas_price_finder.data.local.entity.HistorialPrecioEntity
import com.gas_price_finder.data.local.entity.PrecioEntity
import com.gas_price_finder.data.remote.api.MitecoApiService
import com.gas_price_finder.data.remote.mapper.EstacionDtoMapper
import com.gas_price_finder.domain.model.Price
import com.gas_price_finder.domain.model.Station
import com.gas_price_finder.domain.repository.StationRepository
import com.gas_price_finder.util.AppLogger
import com.gas_price_finder.util.GeoUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.collections.emptyList

private const val TAG = "StationRepository"
private const val DIAG_TAG = "DIAG_SYNC"

class StationRepositoryImpl @Inject constructor(
    private val api: MitecoApiService,
    private val estacionDao: EstacionDao,
    private val precioDao: PrecioDao,
    private val ccaaDao: CcaaDao,
    private val provinciaDao: ProvinciaDao,
    private val municipioDao: MunicipioDao,
    private val localidadDao: LocalidadDao,
    private val productoDao: ProductoDao,
    private val historialPrecioDao: HistorialPrecioDao,
    private val logger: AppLogger,
    private val usuarioDao: UsuarioDao,
    private val database: GasPriceDatabase
) : StationRepository {

    override fun getNearbyStations(
        latitud: Double,
        longitud: Double,
        radioKm: Int,
        productoId: Int
    ): Flow<List<Station>> = flow<List<Station>> {
        val boundingBox = GeoUtils.calculateBoundingBox(latitud, longitud, radioKm)
        val estaciones = estacionDao.getInBoundingBox(
            boundingBox.minLat,
            boundingBox.maxLat,
            boundingBox.minLon,
            boundingBox.maxLon
        )

        val municipiosMap = municipioDao.getAll().associateBy { it.id }

        val stations = estaciones.map { entity ->
            val precios = precioDao.getByEstacionId(entity.ideess)
            val municipioNombre = entity.municipioId?.let { municipiosMap[it]?.nombre }
            entity.toDomain(precios, municipioNombre)
        }.filter { station ->
            val distancia = GeoUtils.haversine(
                latitud,
                longitud,
                station.latitud,
                station.longitud
            )
            distancia <= radioKm
        }.sortedBy { station ->
            GeoUtils.haversine(
                latitud,
                longitud,
                station.latitud,
                station.longitud
            )
        }

        emit(stations)
    }.catch { e ->
        logger.e(TAG, "Error al obtener las estaciones cercanas", e)
        emit(emptyList())
    }

    override fun getStationById(ideess: String): Flow<Station?> = flow {
        val entity = estacionDao.getById(ideess)
        val municipiosMap = municipioDao.getAll().associateBy { it.id }
        val station = entity?.let {
            val precios = precioDao.getByEstacionId(it.ideess)
            val municipioNombre = it.municipioId?.let { municipiosMap[it]?.nombre }
            it.toDomain(precios, municipioNombre)
        }
        emit(station)
    }

    override suspend fun getStationsInBoundingBox(
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double
    ): List<Station> {
        val entities = estacionDao.getInBoundingBox(minLat, maxLat, minLon, maxLon)
        val municipiosMap = municipioDao.getAll().associateBy { it.id }
        return entities.map { entity ->
            val precios = precioDao.getByEstacionId(entity.ideess)
            val municipioNombre = entity.municipioId?.let { municipiosMap[it]?.nombre }
            entity.toDomain(precios, municipioNombre)
        }
    }

    override suspend fun getLastSyncTime(): Long? {
        return if (estacionDao.getCount() > 0) System.currentTimeMillis() else null
    }

    private fun EstacionEntity.toDomain(precios: List<PrecioEntity>, municipioNombre: String?): Station {
        return Station(
            ideess = this.ideess,
            rotulo = this.rotulo,
            direccion = this.direccion,
            cp = this.cp,
            latitud = this.latitud,
            longitud = this.longitud,
            municipio = municipioNombre,
            provincia = null,
            horario = this.horario,
            tipoEstacion = this.tipoEstacion,
            precios = precios.map { it.toDomain() },
            fechaActualizacion = this.fechaActualizacion
        )
    }

    private val productoNames: Map<Int, String> by lazy {
        EstacionDtoMapper.PRODUCTOS.associate { it.id to it.nombre }
    }

    private fun PrecioEntity.toDomain(): Price {
        return Price(
            productoId = this.productoId,
            nombreProducto = productoNames[this.productoId] ?: "Producto ${this.productoId}",
            precio = this.precio ?: 0.0,
            fechaActualizacion = this.fechaActualizacion
        )
    }

    /**
     * Sincroniza estaciones con la API del MITECO.
     * Corre en Dispatchers.IO para no bloquear el hilo principal.
     * El sync tarda ~10 segundos; en el hilo main causaria ANR.
     *
     * CRÍTICO: Toodo el reemplazo de datos (delete + insert) se ejecuta
     * dentro de una transacción de Room (withTransaction). Si cualquier
     * paso falla, se hace rollback completo y los datos anteriores
     * permanecen intactos, garantizando modo offline.
     */
    override suspend fun syncStations(): Result<Unit> = withContext(Dispatchers.IO) {
        val syncStartTime = System.currentTimeMillis()
        logger.d(DIAG_TAG, ">>> syncStations START. thread=${Thread.currentThread().name}")

        try {
            logger.i(TAG, "Empezando sincronizacion con la API del MITECO")

            // ===== PHASE 1: API CALL =====
            logger.d(DIAG_TAG, ">>> Phase 1: API call start")
            val apiStart = System.currentTimeMillis()
            val response = api.getEstacionesTerrestres()
            logger.d(DIAG_TAG, ">>> API response: code=${response.code()}, time=${System.currentTimeMillis() - apiStart}ms")

            if (!response.isSuccessful) {
                logger.e(DIAG_TAG, ">>> API ERROR: ${response.code()}")
                return@withContext Result.failure(Exception("API error: ${response.code()}"))
            }

            val body = response.body()
                ?: return@withContext Result.failure(Exception("Cuerpo del response vacio"))

            if (body.resultadoConsulta != "OK") {
                logger.e(DIAG_TAG, ">>> API resultadoConsulta=${body.resultadoConsulta}")
                return@withContext Result.failure(Exception("Resultado API: ${body.resultadoConsulta}"))
            }
            val dtos = body.listaEESSPrecio
            logger.d(DIAG_TAG, ">>> DTOs recibidos: ${dtos.size}")

            // ===== PHASE 2: MAPPING =====
            logger.d(DIAG_TAG, ">>> Phase 2: Mapping start")
            val mapStart = System.currentTimeMillis()

            val (ccaas, provincias, municipios, localidades) = EstacionDtoMapper.extractAllGeographicAreas(dtos)
            val estaciones = dtos.map { EstacionDtoMapper.toEstacionEntity(it) }
            val precios = dtos.flatMap { EstacionDtoMapper.toPrecioEntities(it) }

            logger.d(DIAG_TAG, ">>> Mapping done in ${System.currentTimeMillis() - mapStart}ms. " +
                    "estaciones=${estaciones.size}, precios=${precios.size}, " +
                    "ccaas=${ccaas.size}, provincias=${provincias.size}, " +
                    "municipios=${municipios.size}, localidades=${localidades.size}")

            // ===== PHASE 3: VERIFICAR USUARIO ANTES DE DESTRUIR =====
            val usuariosAntes = usuarioDao.getAll().firstOrNull() ?: emptyList()
            logger.d(DIAG_TAG, ">>> Usuarios ANTES de deleteAll: count=${usuariosAntes.size}, " +
                    usuariosAntes.joinToString { "id=${it.id},nombre=${it.nombre},activo=${it.activo}" })

            // ===== PHASE 4 & 5: REEMPLAZO ATÓMICO =====
            logger.d(DIAG_TAG, ">>> Phase 4+5: ATOMIC REPLACE start")
            val replaceStart = System.currentTimeMillis()

            database.withTransaction {
                // Delete
                estacionDao.deleteAll()
                precioDao.deleteAll()

                // Insert geográficos y datos
                productoDao.insertAll(EstacionDtoMapper.PRODUCTOS)
                ccaaDao.insertAll(ccaas)
                provinciaDao.insertAll(provincias)
                municipioDao.insertAll(municipios)
                localidadDao.insertAll(localidades)
                estacionDao.insertAll(estaciones)
                precioDao.insertAll(precios)
            }

            logger.d(DIAG_TAG, ">>> ATOMIC REPLACE done in ${System.currentTimeMillis() - replaceStart}ms")

            // ===== PHASE 6: HISTORIAL (fuera de la transacción principal) =====
            logger.d(DIAG_TAG, ">>> Phase 6: Historial precios")
            val latestPrices = historialPrecioDao.getLatestPrices()
                .associateBy { it.estacionId to it.productoId }
            val historiales = precios.mapNotNull { precio ->
                val key = precio.estacionId to precio.productoId
                val last = latestPrices[key]

                if (last == null || last.precio != (precio.precio ?: 0.0)){
                    HistorialPrecioEntity(
                        estacionId = precio.estacionId,
                        productoId = precio.productoId,
                        precio = precio.precio ?: 0.0
                    )
                } else null
            }

            if (historiales.isNotEmpty()){
                historialPrecioDao.insertAll(historiales)
                logger.d(DIAG_TAG, ">>> HISTORIAL INSERTADO. count= ~${historiales.size} (de ${precios.size} precios filtrados.)")
            } else {
                logger.d(DIAG_TAG, ">>> inserción de historial saltado: sin actualización de precios")
            }

            // ===== PHASE 7: VERIFICAR USUARIO DESPUES =====
            val usuariosDespues = usuarioDao.getAll().firstOrNull() ?: emptyList()
            logger.d(DIAG_TAG, ">>> Usuarios DESPUES de insertAll: count=${usuariosDespues.size}, " +
                    usuariosDespues.joinToString { "id=${it.id},nombre=${it.nombre},activo=${it.activo}" })

            val estacionesCountAfter = estacionDao.getCount()
            val preciosCountAfter = precioDao.getCount()
            logger.d(DIAG_TAG, ">>> Counts despues: estaciones=$estacionesCountAfter, precios=$preciosCountAfter")

            val totalTime = System.currentTimeMillis() - syncStartTime
            logger.d(DIAG_TAG, ">>> syncStations END. Total time=${totalTime}ms")

            logger.i(TAG, "Sincronizacion completa en ${totalTime}ms: " +
                    "Estaciones (${estaciones.size}), Precios: (${precios.size}), Historial: (${historiales.size})")

            Result.success(Unit)
        } catch (e: Exception) {
            val totalTime = System.currentTimeMillis() - syncStartTime
            logger.e(DIAG_TAG, ">>> syncStations CRASH after ${totalTime}ms")
            logger.e(DIAG_TAG, ">>> CRASH_DETAILS: ${e.javaClass.simpleName}: ${e.message}")
            e.stackTrace.take(8).forEachIndexed { i, st ->
                logger.e(DIAG_TAG, ">>>   at $st")
            }
            logger.e(TAG, "Error de sincronizacion: ${e.message}", e)
            Result.failure(e)
        }
    }
}