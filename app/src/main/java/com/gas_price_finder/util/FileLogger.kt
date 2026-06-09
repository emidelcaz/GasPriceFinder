package com.gas_price_finder.util

import android.content.ContentValues
import android.content.Context
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import com.gas_price_finder.util.Constants.LOG_MAX_FILE_SIZE_MB
import com.gas_price_finder.util.Constants.LOG_RETENTION_DAYS

private const val TAG = "FileLogger"

@Singleton
class FileLogger @Inject constructor(
    private val context: Context
) {

    private val logDir = File(context.filesDir, "logs")

    // DateTimeFormatter es inmutable y thread-safe (a diferencia de SimpleDateFormat)
    private val dateFormat = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss.SSS")
    private val fileDateFormat = DateTimeFormatter.ofPattern("dd-MM-yyyy")

    private val isDebugBuild =
        context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0

    // Cacheamos la fecha actual del archivo para evitar inconsistencias
    // entre appendText() y rotateIfNeeded() si cruzamos la medianoche
    @Volatile
    private var cachedLogFile: File? = null

    @Volatile
    private var cachedDate: LocalDate? = null

    companion object {
        private const val MAX_FILE_SIZE = LOG_MAX_FILE_SIZE_MB * 1024 * 1024 // 5MB
    }

    init {
        try {
            logDir.mkdirs()
            cleanOldLogs()
        } catch (e: Exception) {
            Log.e(TAG, "Error en init del logger", e)
        }
    }

    /**
     * Obtiene el archivo de log actual con cache por fecha.
     * Esto evita que appendText() y rotateIfNeeded() operen sobre archivos diferentes
     * si se cruza la medianoche durante la ejecucion.
     */
    private fun getCurrentLogFile(): File {
        val today = LocalDate.now(ZoneId.systemDefault())
        val cached = cachedLogFile
        if (cached != null && cachedDate == today && cached.exists()) {
            return cached
        }
        val newFile = File(logDir, "app_${fileDateFormat.format(today)}.txt")
        cachedLogFile = newFile
        cachedDate = today
        return newFile
    }

    /**
     * Escribe un log al archivo de forma sincronizada.
     * El synchronized evita condiciones de carrera entre multiples threads/coroutines
     * (UI thread, Workers, IO coroutines) que pueden llamar al logger simultaneamente.
     */
    private fun log(level: String, tag: String, message: String) {
        val timestamp = try {
            dateFormat.format(LocalDateTime.now(ZoneId.systemDefault()))
        } catch (e: Exception) {
            System.currentTimeMillis().toString()
        }
        val logLine = "[$timestamp] $level/$tag: $message\n"

        synchronized(this) {
            try {
                val logFile = getCurrentLogFile()
                if (!logDir.exists()) {
                    logDir.mkdirs()
                }
                logFile.appendText(logLine)
                rotateIfNeeded(logFile)
            } catch (e: IOException) {
                Log.e(TAG, "Error al escribir en log (IO)", e)
            } catch (e: SecurityException) {
                Log.e(TAG, "Error al escribir en log (Security)", e)
            } catch (e: Exception) {
                Log.e(TAG, "Error inesperado al escribir log: ${e.message}")
            }
        }
    }

    /**
     * Rota el archivo si excede el tamano maximo.
     * Sincronizado por el llamador (metodo log).
     */
    private fun rotateIfNeeded(logFile: File) {
        try {
            if (logFile.length() > MAX_FILE_SIZE) {
                val rotated = File(logDir, "${logFile.name}.1")
                // Si ya existe un archivo rotado, lo borramos primero
                if (rotated.exists()) {
                    rotated.delete()
                }
                logFile.renameTo(rotated)
                // Invalidamos cache para que se cree uno nuevo
                cachedLogFile = null
                cachedDate = null
            }
        } catch (e: Exception) {
            Log.e("FileLogger", "Error al rotar log", e)
        }
    }

    /**
     * Elimina logs mas antiguos que LOG_RETENTION_DIAS.
     */
    private fun cleanOldLogs() {
        try {
            val retentionDaysAgo = System.currentTimeMillis() -
                    (LOG_RETENTION_DAYS * 24 * 60 * 60 * 1000)
            logDir.listFiles()?.forEach { file ->
                if (file.lastModified() < retentionDaysAgo) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            Log.e("FileLogger", "Error al limpiar logs antiguos", e)
        }
    }

    /**
     * Exporta todos los logs a un ZIP en el cache directory.
     */
    suspend fun exportLogs(): File? = withContext(Dispatchers.IO) {
        try {
            val logFiles = logDir.listFiles()?.filter { it.isFile && it.canRead() }
                ?: return@withContext null

            if (logFiles.isEmpty()) return@withContext null

            val zipFile = File(context.cacheDir, "gasprice_logs.zip")
            ZipOutputStream(zipFile.outputStream()).use { zos ->
                logFiles.forEach { file ->
                    try {
                        zos.putNextEntry(ZipEntry(file.name))
                        file.inputStream().use { it.copyTo(zos) }
                        zos.closeEntry()
                    } catch (e: IOException) {
                        Log.e("FileLogger", "Error al agregar ${file.name} al zip", e)
                    }
                }
            }
            zipFile
        } catch (e: Exception) {
            Log.e("FileLogger", "Error al exportar logs", e)
            null
        }
    }

    /**
     * Exporta logs a la carpeta Downloads pública del sistema.
     *
     * @param onlyToday true -> solo el log del día actual;
     *                  false -> ZIP con todos los logs.
     * @return Result con mensaje descriptivo del destino.
     */
    suspend fun exportToPublicDownloads(onlyToday: Boolean): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val sourceFile = if (onlyToday) {
                    getCurrentLogFile().takeIf { it.exists() && it.length() > 0 }
                } else {
                    exportLogs()
                } ?: return@withContext Result.failure(
                    Exception("No hay logs para exportar")
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // API 29+: MediaStore Downloads (no requiere permiso de escritura)
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, sourceFile.name)
                        put(
                            MediaStore.Downloads.MIME_TYPE,
                            if (onlyToday) "text/plain" else "application/zip"
                        )
                        put(
                            MediaStore.Downloads.RELATIVE_PATH,
                            Environment.DIRECTORY_DOWNLOADS
                        )
                        put(MediaStore.Downloads.IS_PENDING, 1)
                    }

                    val uri: Uri? = context.contentResolver.insert(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        contentValues
                    )

                    uri?.let {
                        context.contentResolver.openOutputStream(it)?.use { out ->
                            sourceFile.inputStream().use { input ->
                                input.copyTo(out)
                            }
                        }
                        contentValues.clear()
                        contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                        context.contentResolver.update(it, contentValues, null, null)
                    }
                        ?: return@withContext Result.failure(
                            Exception("No se pudo crear el archivo en Descargas")
                        )

                    Result.success("Guardado en Descargas: ${sourceFile.name}")
                } else {
                    // API 26-28: escritura directa en Downloads público
                    val downloadsDir =
                        Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DOWNLOADS
                        )
                    val destFile = File(downloadsDir, sourceFile.name)
                    sourceFile.inputStream().use { input ->
                        destFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    Result.success("Guardado en: ${destFile.absolutePath}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al exportar logs a Descargas", e)
                Result.failure(e)
            }
        }

    // ==================== NIVELES DE LOG ====================

    fun d(tag: String, message: String) {
        if (isDebugBuild) log("DEBUG", tag, message)
    }

    fun i(tag: String, message: String) {
        log("INFO", tag, message)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        val fullMessage = if (throwable != null) {
            "$message\n${throwable.stackTraceToString()}"
        } else {
            message
        }
        log("WARN", tag, fullMessage)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        val fullMessage = if (throwable != null) {
            "$message\n${throwable.stackTraceToString()}"
        } else {
            message
        }
        log("ERROR", tag, fullMessage)
    }
}