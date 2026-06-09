package com.gas_price_finder.data.local.worker

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.gas_price_finder.util.AppLogger
import androidx.work.*
import com.gas_price_finder.data.local.dao.EstacionRecienteDao
import com.gas_price_finder.domain.repository.PriceHistoryRepository
import com.gas_price_finder.domain.repository.StationRepository
import com.gas_price_finder.domain.repository.UserRepository
import com.gas_price_finder.util.Constants
import com.gas_price_finder.util.NotificationHelper
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.firstOrNull
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

private const val TAG = "SyncWorker"

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SyncWorkerEntryPoint {
        fun stationRepository(): StationRepository
        fun userRepository(): UserRepository
        fun priceHistoryRepository(): PriceHistoryRepository
        fun estacionRecienteDao(): EstacionRecienteDao
        fun notificationHelper(): NotificationHelper
        fun appLogger(): AppLogger
    }

    private val entryPoint by lazy {
        EntryPointAccessors.fromApplication(
            applicationContext,
            SyncWorkerEntryPoint::class.java
        )
    }

    override suspend fun doWork(): Result {
        val logger = entryPoint.appLogger()
        val stationRepository = entryPoint.stationRepository()
        val userRepository = entryPoint.userRepository()
        val priceHistoryRepository = entryPoint.priceHistoryRepository()
        val estacionRecienteDao = entryPoint.estacionRecienteDao()
        val notificationHelper = entryPoint.notificationHelper()

        logger.i(TAG, "Empezando sincronización periódica (intento ${runAttemptCount + 1})")
        logger.e("SyncWorker", ">>> doWork() ENTER")

        // Límite de reintentos: si fallamos más de 3 veces consecutivas,
        // consideramos que es un error persistente y dejamos de reintentar.
        if (runAttemptCount >= MAX_RETRIES) {
            logger.e(TAG, "Se alcanzó el maximo de reintentos ($MAX_RETRIES). Abortando.")
            return Result.failure()
        }

        return try {
            val user = userRepository.getActiveUser().firstOrNull()
            val soloWifi = user?.syncWifi ?: false

            if (soloWifi && !isWifiConnected(logger)) {
                logger.i(TAG, "Saltando sincronización: WiFi no conectado")
                // Usamos retry() porque las condiciones de red pueden cambiar pronto
                return Result.retry()
            }

            val result = stationRepository.syncStations()

            result.fold(
                onSuccess = {
                    logger.i(TAG, "Sincronización completada con exito")
                    //cleanupOldData(estacionRecienteDao, logger) // -> Por el momento sin priceHistoryRepository
                    Result.success()
                },
                onFailure = { error ->
                    handleFailure(error, logger)
                }
            )
        } catch (e: Exception) {
            handleUnexpectedException(e, logger)
        }
    }

    /**
     * Limpia datos antiguos después de una sincronización exitosa.
     */
    private suspend fun cleanupOldData(
        //priceHistoryRepository: PriceHistoryRepository,
        estacionRecienteDao: EstacionRecienteDao,
        logger: AppLogger
    ) {
        try {
            //priceHistoryRepository.cleanOldHistory(Constants.HISTORY_MAX_DAYS)
            val limiteRecientes = System.currentTimeMillis() -
                    (Constants.HISTORY_MAX_DAYS * 24 * 60 * 60 * 1000)
            estacionRecienteDao.deleteOlderThan(limiteRecientes)
        } catch (e: Exception) {
            // Los errores de limpieza no deben marcar el sync como fallido
            logger.w(TAG, "Error durante limpieza de datos antiguos: ${e.message}")
        }
    }

    /**
     * Clasifica el error para decidir si reintentar o no.
     */
    private fun handleFailure(error: Throwable, logger: AppLogger): Result {
        return when (error) {
            is UnknownHostException,
            is SocketTimeoutException -> {
                // Errores de red transitorios: reintentar
                logger.w(TAG, "Error de red transitorio: ${error.message}")
                Result.retry()
            }

            is SecurityException,
            is IllegalStateException,
            is IllegalArgumentException -> {
                // Errores permanentes: no reintentar (configuracion, permisos, etc.)
                logger.e(TAG, "Error permanente, no se reintentara", error)
                Result.failure()
            }

            else -> {
                logger.e(TAG, "Error en sincronizacion", error)
                // Para errores desconocidos, reintentar con backoff exponencial
                Result.retry()
            }
        }
    }

    private fun handleUnexpectedException(e: Exception, logger: AppLogger): Result {
        return when (e) {
            is SecurityException,
            is IllegalStateException -> {
                logger.e(TAG, "Error inesperado permanente", e)
                Result.failure()
            }

            else -> {
                logger.e(TAG, "Error inesperado (intento ${runAttemptCount + 1})", e)
                Result.retry()
            }
        }
    }

    /**
     * Verifica si hay conexion WiFi
     */
    private fun isWifiConnected(logger: AppLogger): Boolean {
        return try {
            val cm = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE)
                    as ConnectivityManager
            val network = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(network) ?: return false
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        } catch (e: Exception) {
            logger.w(TAG, "Error al verificar WiFi: ${e.message}")
            // Por seguridad, asumimos que no hay WiFi si no podemos verificar
            false
        }
    }

    companion object {
        const val WORK_NAME = "station_sync_work"
        private const val MAX_RETRIES = 3

        /**
         * Programa el trabajo de sincronizacion periodica.
         * Usa ExistingPeriodicWorkPolicy.REPLACE para limpiar posibles estados corruptos anteriores.
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncWork = PeriodicWorkRequestBuilder<SyncWorker>(
                Constants.SYNC_INTERVAL_MINUTES, TimeUnit.MINUTES,
                Constants.SYNC_FLEX_MINUTES, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setInitialDelay(0, TimeUnit.MINUTES)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.REPLACE,
                syncWork
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}