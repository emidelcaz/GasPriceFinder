package com.gas_price_finder.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.gas_price_finder.R
import com.gas_price_finder.presentation.MainActivity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    private val context: Context
) {

    companion object {
        const val CHANNEL_ID = "price_drop_alerts"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alertas de precio",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Notificaciones cuando baja el precio en gasolineras favoritas"
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showPriceDropNotification(
        stationName: String,
        oldPrice: Double,
        newPrice: Double,
        stationId: String
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("station_id", stationId)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            stationId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_gas_station_generic) // TODO() ic_notification
            .setContentTitle("¡Bajada de precio en $stationName!")
            .setContentText("Antes: ${String.format("%.3f", oldPrice)}€ - " +
                    "Ahora: ${String.format("%.3f", newPrice)}€")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(stationId.hashCode(), notification) //TODO() checkPermission
    }
}