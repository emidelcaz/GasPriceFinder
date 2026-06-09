package com.gas_price_finder.domain.repository

import com.gas_price_finder.domain.model.Station
import kotlinx.coroutines.flow.Flow

interface StationRepository {
    fun getNearbyStations(
        latitud: Double,
        longitud: Double,
        radioKm: Int,
        productoId: Int
    ): Flow<List<Station>>

    fun getStationById(ideess: String): Flow<Station?>

    suspend fun getStationsInBoundingBox(
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double
    ): List<Station>

    suspend fun syncStations(): Result<Unit>

    suspend fun getLastSyncTime(): Long?
}