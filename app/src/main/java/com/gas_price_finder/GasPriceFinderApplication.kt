package com.gas_price_finder

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import com.gas_price_finder.data.local.worker.SyncWorker
import com.gas_price_finder.util.AppLogger
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class GasPriceFinderApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var logger: AppLogger

    override fun onCreate() {
        super.onCreate()

        try {
            logger.i("ApplicationGPF", "---------- GasPrice Finder START ----------")
            SyncWorker.schedule(this)
        } catch (e: Exception) {
            logger.e("ApplicationGPF", "Error al inicializar aplicacion", e)
        }

        logger.d("APP_LIFECYCLE", "Application.onCreate()")

        // Log temporal: verificar estado de WorkManager TODO() eliminar o al menos cambiar el forever por solo observer
        val wm = WorkManager.getInstance(this)
        wm.getWorkInfosForUniqueWorkLiveData(SyncWorker.WORK_NAME)
            .observeForever { works ->
                works?.forEach { work ->
                    logger.d("APP_LIFECYCLE", "Work state: ${work.state}, id=${work.id}")
                }
            }

    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}