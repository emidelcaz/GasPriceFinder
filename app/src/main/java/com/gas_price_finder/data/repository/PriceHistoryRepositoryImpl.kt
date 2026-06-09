package com.gas_price_finder.data.repository

import com.gas_price_finder.data.local.dao.HistorialPrecioDao
import com.gas_price_finder.data.local.entity.HistorialPrecioEntity
import com.gas_price_finder.domain.model.PriceHistory
import com.gas_price_finder.domain.repository.PriceHistoryRepository
import javax.inject.Inject


class PriceHistoryRepositoryImpl @Inject constructor(
    private val historialDao: HistorialPrecioDao
) : PriceHistoryRepository {

    override suspend fun getHistory(estacionId: String, productoId: Int): List<PriceHistory> {
        return historialDao.getByEstacionAndProducto(estacionId, productoId, 100)
            .map { it.toDomain() }
    }

    /**
     * Inserta un nuevo precio solo si difiere del último registro almacenado
     * para la misma estación y producto, evitando duplicados consecutivos.
     */
    override suspend fun savePriceSnapshot(
        estacionId: String,
        productoId: Int,
        precio: Double
    ): Result<Unit> {
        return try {
            val ultimo = historialDao.getByEstacionAndProducto(estacionId, productoId, 1)
                .firstOrNull()

            if (ultimo != null && ultimo.precio == precio) {
                return Result.success(Unit)
            }

            historialDao.insert(
                HistorialPrecioEntity(
                    estacionId = estacionId,
                    productoId = productoId,
                    precio = precio
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun cleanOldHistory(daysToKeep: Long): Result<Unit> {
        return try {
            val limit = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000)
            historialDao.deleteOlderThan(limit)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun HistorialPrecioEntity.toDomain(): PriceHistory {
        return PriceHistory(
            id = this.id,
            estacionId = this.estacionId,
            productoId = this.productoId,
            precio = this.precio,
            fechaCaptura = this.fechaCaptura
        )
    }

}