package com.gas_price_finder.data.remote.api

import com.gas_price_finder.data.remote.dto.EstacionesResponseDto
import retrofit2.Response
import retrofit2.http.GET

interface MitecoApiService {

    /**
     * Endpoint principal: devuelve TODAS las estaciones de España (~12.000)
     * Se consume cada 60 minutos vía WorkManager
     */
    @GET("ServiciosRESTCarburantes/PreciosCarburantes/EstacionesTerrestres/")
    suspend fun getEstacionesTerrestres(): Response<EstacionesResponseDto>

    /**
     * Filtro por producto (para optimización futura, no MVP)
     */
    @GET("ServiciosRESTCarburantes/PreciosCarburantes/EstacionesTerrestres/FiltroProducto/{IDProducto}")
    suspend fun getEstacionesByProducto(productoId: Int): Response<EstacionesResponseDto>
}