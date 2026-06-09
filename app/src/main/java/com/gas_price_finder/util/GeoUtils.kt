package com.gas_price_finder.util

import kotlin.math.*

object GeoUtils {

    private const val EARTH_RADIUS_KM = 6371.0

    fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        return 2 * EARTH_RADIUS_KM * atan2(sqrt(a), sqrt(1 - a))
    }

    fun calculateBoundingBox(lat: Double, lon: Double, radiusKm: Int): BoundingBox {
        val latDelta = radiusKm / 111.0
        val lonDelta = radiusKm / (111.0 * cos(Math.toRadians(lat)))
        return BoundingBox(
            minLat = lat - latDelta,
            maxLat = lat + latDelta,
            minLon = lon - lonDelta,
            maxLon = lon + lonDelta
        )
    }

    fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val dLon = Math.toRadians(lon2 - lon1)
        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)
        val y = sin(dLon) * cos(lat2Rad)
        val x = cos(lat1Rad) * sin(lat2Rad) - sin(lat1Rad) * cos(lat2Rad) * cos(dLon)
        var bearing = Math.toDegrees(atan2(y, x)).toFloat()
        bearing = (bearing + 360) % 360
        return bearing
    }

    fun zoomLevelForRadius(radiusKm: Int): Float {
        return when {
            radiusKm <= 1 -> 15f
            radiusKm <= 2 -> 14f
            radiusKm <= 5 -> 13f
            radiusKm <= 10 -> 12f
            radiusKm <= 20 -> 11f
            radiusKm <= 50 -> 10f
            else -> 9f
        }
    }
}

data class BoundingBox(
    val minLat: Double,
    val maxLat: Double,
    val minLon: Double,
    val maxLon: Double
)