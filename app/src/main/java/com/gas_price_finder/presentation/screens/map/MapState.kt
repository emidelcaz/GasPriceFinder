package com.gas_price_finder.presentation.screens.map

import com.gas_price_finder.domain.model.RouteCalculation
import com.gas_price_finder.domain.model.Station
import com.gas_price_finder.domain.model.User
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.MapType

data class MapState(
    val isLoading: Boolean = false,
    val userLocation: LatLng? = null,
    val nearbyStations: List<Station> = emptyList(),
    val filteredStations: List<Station> = emptyList(),
    val radioKm: Int = 10,
    val combustiblePreferidoId: Int = 1,
    val locationPermissionGranted: Boolean = false,
    val permissionRequested: Boolean = false,
    val error: String? = null,
    val priceTerciles: PriceTerciles? = null,
    val mapType: MapType = MapType.NORMAL,
    val selectedStation: Station? = null,
    val filters: FilterState = FilterState(),
    val sortByPrice: Boolean = true,
    val stationDistances: Map<String, Double> = emptyMap(),
    val favoriteStationIds: Set<String> = emptySet(),
    val activeUser: User? = null,
    val savedCameraPosition: CameraPosition? = null,

    // Modo en ruta
    val isRouteModeActive: Boolean = false,
    val showRouteDistanceDialog: Boolean = false,
    val showRouteAdBlueDialog: Boolean = false,
    val showRouteResultsDialog: Boolean = false,
    val routeDistanceKm: Int = 20,
    val routeCalculations: List<RouteCalculation> = emptyList(),
    val routeError: String? = null
)

data class PriceTerciles(
    val lowThreshold: Double,
    val highThreshold: Double
)

data class FilterState(
    val openNow: Boolean = false,
    val myFuelType: Boolean = false,
    val onlyFavorites: Boolean = false
) {
    val activeCount: Int
        get() = listOf(openNow, myFuelType, onlyFavorites).count { it }
}