package com.gas_price_finder.util

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wrapper de logging que escribe simultáneamente a logcat (android.util.Log)
 * y al fichero persistente (FileLogger), garantizando que las trazas de
 * depuración estén disponibles tanto en desarrollo como en dispositivos físicos.
 */
@Singleton
class AppLogger @Inject constructor(
    private val fileLogger: FileLogger
) {

    fun d(tag: String, message: String) {
        Log.d(tag, message)
        fileLogger.d(tag, message)
    }

    fun i(tag: String, message: String) {
        Log.i(tag, message)
        fileLogger.i(tag, message)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        Log.w(tag, message, throwable)
        fileLogger.w(tag, message, throwable)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e(tag, message, throwable)
        fileLogger.e(tag, message, throwable)
    }

    suspend fun exportToPublicDownloads(onlyToday: Boolean): Result<String> {
        return fileLogger.exportToPublicDownloads(onlyToday)
    }
}
