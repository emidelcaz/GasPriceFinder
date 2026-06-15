package com.gas_price_finder.presentation.screens.map

import android.Manifest
import android.content.Context
import com.gas_price_finder.util.AppLogger
import androidx.annotation.RequiresPermission
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gas_price_finder.R
import com.gas_price_finder.domain.model.Station
import com.gas_price_finder.domain.usecase.favorite.GetFavoritesUseCase
import com.gas_price_finder.domain.usecase.station.GetNearbyStationsUseCase
import com.gas_price_finder.domain.usecase.station.GetOptimalStationInRouteUseCase
import com.gas_price_finder.domain.usecase.station.GetStationsInZoneUseCase
import com.gas_price_finder.domain.usecase.sync.SyncStationsUseCase
import com.gas_price_finder.domain.usecase.user.GetActiveUserUseCase
import com.gas_price_finder.util.GeoUtils
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import android.os.Looper
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.MapType
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "MapViewModel"

@HiltViewModel
class MapViewModel @Inject constructor(
    private val getNearbyStationsUseCase: GetNearbyStationsUseCase,
    private val getStationsInZoneUseCase: GetStationsInZoneUseCase,
    private val getOptimalStationInRouteUseCase: GetOptimalStationInRouteUseCase,
    private val getActiveUserUseCase: GetActiveUserUseCase,
    private val getFavoritesUseCase: GetFavoritesUseCase,
    private val syncStationsUseCase: SyncStationsUseCase,
    private val fusedLocationClient: FusedLocationProviderClient,
    private val logger: AppLogger,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapState())
    val uiState: StateFlow<MapState> = _uiState.asStateFlow()

    private var _lastBearing: Float? = null
    private var _locationCallback: LocationCallback? = null

    init {

        logger.d("MAP_DIAG", ">>> MapViewModel INIT instance=${System.identityHashCode(this)}")

        viewModelScope.launch {
            try {
                logger.d("MAP_DIAG", ">>> syncStationsUseCase START instance=${System.identityHashCode(this@MapViewModel)}")
                syncStationsUseCase()
                logger.d("MAP_DIAG", ">>> syncStationsUseCase END instance=${System.identityHashCode(this@MapViewModel)}")
            } catch (e: Exception) {
                logger.e("MAP_DIAG", ">>> syncStationsUseCase FAILED", e)
            }
        }
        loadUserAndStations()
    }

    private fun loadUserAndStations() {
        viewModelScope.launch {
            getActiveUserUseCase()
                .catch { e -> logger.e(TAG, "Error en flow de usuario activo", e) }
                .collect { user ->
                    logger.d("MAP_DIAG", ">>> getActiveUserUseCase collect: user=${user?.id}:${user?.nombre}, activo=${user?.activo}")
                    _uiState.update {
                        it.copy(
                            combustiblePreferidoId = user?.combustiblePreferidoId ?: 1,
                            radioKm = user?.radioBusquedaKm ?: 10,
                            activeUser = user
                        )
                    }
                    user?.let { loadFavorites(it.id) }
                }
        }

        fetchLocation()
    }

    // Obtener ubicacion: manejar SecurityException (permiso denegado)
    // y cualquier otra excepcion que pueda lanzar el FusedLocationProvider
    private fun fetchLocation() {
        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        updateLocationAndLoadStations(LatLng(location.latitude, location.longitude))
                    } else {
                        requestCurrentLocation()
                    }
                }
                .addOnFailureListener {
                    logger.w(TAG, "Error al obtener ubicacion", it)
                    requestCurrentLocation()
                }
        } catch (e: SecurityException) {
            logger.w(TAG, "Permiso de ubicación no concedido", e)
            updateLocationAndLoadStations(LatLng(40.4168, -3.7038))
        } catch (e: Exception) {
            logger.e(TAG, "Error inesperado al obtener ubicacion", e)
            updateLocationAndLoadStations(LatLng(40.4168, -3.7038))
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun requestCurrentLocation() {
        try {
            val cancellationToken = CancellationTokenSource()
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationToken.token
            ).addOnSuccessListener { location ->
                val latLng = location?.let { LatLng(it.latitude, it.longitude) }
                    ?: LatLng(40.4168, -3.7038)
                updateLocationAndLoadStations(latLng)
            }.addOnFailureListener {
                logger.w(TAG, "Error al obtener ubicación actual", it)
                updateLocationAndLoadStations(LatLng(40.4168, -3.7038))
            }
        } catch (e: Exception) {
            logger.e(TAG, "Error inesperado al obtener ubicación actual", e)
            updateLocationAndLoadStations(LatLng(40.4168, -3.7038))
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun startLocationUpdates() {
        try {
            stopLocationUpdates()

            val request = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                1000L
            ).apply {
                setMinUpdateIntervalMillis(500L)
                setWaitForAccurateLocation(false)
            }.build()

            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.lastLocation?.let { location ->
                        _uiState.update { it.copy(userLocation = LatLng(location.latitude, location.longitude)) }
                        if (location.hasBearing()) {
                            _lastBearing = location.bearing
                        }
                    }
                }
            }

            _locationCallback = callback
            fusedLocationClient.requestLocationUpdates(
                request,
                callback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            logger.w(TAG, "Permiso de ubicación no concedido", e)
        } catch (e: Exception) {
            logger.e(TAG, "Error al iniciar actualizaciones de ubicación", e)
        }
    }

    private fun stopLocationUpdates() {
        _locationCallback?.let { callback ->
            fusedLocationClient.removeLocationUpdates(callback)
            _locationCallback = null
        }
    }

    private fun loadFavorites(userId: Int) {
        viewModelScope.launch {
            getFavoritesUseCase(userId)
                .catch { e ->
                    logger.e(TAG, "Error al cargar favoritos", e)
                }
                .collect { favorites ->
                    _uiState.update {
                        it.copy(favoriteStationIds = favorites.map { f -> f.estacionId }.toSet())
                    }
                    applyFilters()
                }
        }
    }

    private fun updateLocationAndLoadStations(latLng: LatLng) {
        _uiState.update { it.copy(userLocation = latLng) }
        viewModelScope.launch {
            getNearbyStationsUseCase(latLng.latitude, latLng.longitude)
                .catch { e ->
                    logger.e(TAG, "Error al cargar estaciones cercanas", e)
                }
                .collect { stations ->
                    val terciles = calculatePriceTerciles(stations, _uiState.value.combustiblePreferidoId)
                    val distances = calculateDistances(stations, latLng)
                    _uiState.update {
                        it.copy(
                            nearbyStations = stations,
                            priceTerciles = terciles,
                            stationDistances = distances
                        )
                    }
                    applyFilters()
                }
        }
    }

    fun onPermissionResult(granted: Boolean) {
        _uiState.update {
            it.copy(
                locationPermissionGranted = granted,
                permissionRequested = true
            )
        }
        if (granted) {
            fetchLocation()
        }
    }

    fun requestPermissionAgain() {
        _uiState.update { it.copy(permissionRequested = false) }
    }

    fun updateRadio(radioKm: Int) {
        _uiState.update { it.copy(radioKm = radioKm) }
        refreshStations()
    }

    fun searchInZone(cameraPositionState: CameraPositionState) {
        logger.d("DIAGNOSTICO", ">>> ANTES de getStationsInZone. userId=${_uiState.value.activeUser?.id}")
        val position = cameraPositionState.position.target
        val radius = _uiState.value.radioKm.toDouble()

        // Guardar la posicion actual de la camara antes de buscar
        saveCameraPosition(cameraPositionState.position)

        viewModelScope.launch {
            getStationsInZoneUseCase(
                position.latitude,
                position.longitude,
                radius
            )
                .catch { e ->
                    logger.e(TAG, "Error al buscar estaciones en zona", e)
                }
                .collect { stations ->
                    val terciles = calculatePriceTerciles(stations, _uiState.value.combustiblePreferidoId)
                    val distanceOrigin = _uiState.value.userLocation ?: position
                    val distances = calculateDistances(stations, distanceOrigin)

                    logger.d("DIAGNOSTICO", ">>> DESPUES de getStationsInZone. stations=${stations.size}")

                    val user = getActiveUserUseCase().firstOrNull()
                    logger.d("DIAGNOSTICO", ">>> Usuario despues de zona: id=${user?.id}, nombre=${user?.nombre}")

                    _uiState.update {
                        it.copy(
                            nearbyStations = stations,
                            priceTerciles = terciles,
                            stationDistances = distances
                        )
                    }
                    applyFilters()
                }
        }
    }

    fun saveCameraPosition(position: com.google.android.gms.maps.model.CameraPosition) {
        _uiState.update { it.copy(savedCameraPosition = position) }
    }

    fun toggleMapType() {
        _uiState.update {
            it.copy(
                mapType = if (it.mapType == MapType.NORMAL) MapType.HYBRID else MapType.NORMAL
            )
        }
    }

    fun selectStation(station: Station) {
        _uiState.update { it.copy(selectedStation = station) }
    }

    fun deselectStation() {
        _uiState.update { it.copy(selectedStation = null) }
    }

    fun toggleFilter(filter: FilterType) {
        val current = _uiState.value.filters
        val updated = when (filter) {
            FilterType.OPEN_NOW -> current.copy(openNow = !current.openNow)
            FilterType.MY_FUEL_TYPE -> current.copy(myFuelType = !current.myFuelType)
            FilterType.ONLY_FAVORITES -> current.copy(onlyFavorites = !current.onlyFavorites)
        }
        _uiState.update { it.copy(filters = updated) }
        applyFilters()
    }

    fun clearFilters() {
        _uiState.update { it.copy(filters = FilterState()) }
        applyFilters()
    }

    fun setSortByPrice(sortByPrice: Boolean) {
        _uiState.update { it.copy(sortByPrice = sortByPrice) }
        applyFilters()
    }

    private fun applyFilters() {
        val state = _uiState.value
        var result = state.nearbyStations

        if (state.filters.openNow) {
            result = result.filter { it.estaAbierto() == true }
        }
        if (state.filters.myFuelType) {
            result = result.filter { it.getPrecio(state.combustiblePreferidoId) != null }
        }
        if (state.filters.onlyFavorites) {
            result = result.filter { state.favoriteStationIds.contains(it.ideess) }
        }

        result = if (state.sortByPrice) {
            result.sortedBy {
                it.getPrecio(state.combustiblePreferidoId)?.precio ?: Double.MAX_VALUE
            }
        } else {
            result.sortedBy { state.stationDistances[it.ideess] ?: Double.MAX_VALUE }
        }

        _uiState.update { it.copy(filteredStations = result) }
    }

    private fun refreshStations() {
        viewModelScope.launch {
            _uiState.value.userLocation?.let { location ->
                getNearbyStationsUseCase(location.latitude, location.longitude)
                    .catch { e ->
                        logger.e(TAG, "Error al refrescar estaciones", e)
                    }
                    .collect { stations ->
                        val terciles = calculatePriceTerciles(stations, _uiState.value.combustiblePreferidoId)
                        val distances = calculateDistances(stations, location)
                        _uiState.update {
                            it.copy(
                                nearbyStations = stations,
                                priceTerciles = terciles,
                                stationDistances = distances
                            )
                        }
                        applyFilters()
                    }
            }
        }
    }

    private fun calculatePriceTerciles(stations: List<Station>, productoId: Int): PriceTerciles? {
        val prices = stations.mapNotNull { it.getPrecio(productoId)?.precio }.filter { it > 0 }
        if (prices.size < 3) return null
        val sorted = prices.sorted()
        val lowIndex = (sorted.size * 0.33).toInt().coerceIn(0, sorted.size - 1)
        val highIndex = (sorted.size * 0.66).toInt().coerceIn(0, sorted.size - 1)
        return PriceTerciles(sorted[lowIndex], sorted[highIndex])
    }

    private fun calculateDistances(stations: List<Station>, userLocation: LatLng): Map<String, Double> {
        return stations.associate { station ->
            val distance = GeoUtils.haversine(
                userLocation.latitude, userLocation.longitude,
                station.latitud, station.longitud
            )
            station.ideess to distance
        }
    }

    fun getDistance(stationId: String): Double? = _uiState.value.stationDistances[stationId]

    fun getDepositPrice(station: Station): String? {
        val user = _uiState.value.activeUser ?: return null
        val price = station.getPrecio(_uiState.value.combustiblePreferidoId)?.precio ?: return null
        val total = price * user.capacidadDepositoL
        return "%.2f".format(total).replace('.', ',') + context.getString(R.string.euros_dep)
    }

    fun getPrice(station: Station): String? {
        val price = station.getPrecio(_uiState.value.combustiblePreferidoId)?.precio ?: return null
        return "%.3f".format(price).replace('.', ',') + " €/L"
    }

    fun isFavorite(stationId: String): Boolean = _uiState.value.favoriteStationIds.contains(stationId)

    fun daysSinceUpdate(station: Station): String {
        val price = station.getPrecio(_uiState.value.combustiblePreferidoId)
        val fecha = price?.fechaActualizacion ?: station.fechaActualizacion ?: return ""
        val diffMs = System.currentTimeMillis() - fecha
        val days = (diffMs / (1000 * 60 * 60 * 24)).toInt()
        return when {
            days <= 0 -> "Cambió hoy"
            days == 1 -> "Cambió hace 1d"
            else -> "Cambió hace $days d"
        }
    }

    // ============================================================
    // MODO EN RUTA
    // ============================================================

    fun toggleRouteMode() {
        val currentlyActive = _uiState.value.isRouteModeActive
        if (currentlyActive) {
            stopLocationUpdates()
            _lastBearing = null
            _uiState.update {
                it.copy(
                    isRouteModeActive = false,
                    showRouteDistanceDialog = false,
                    showRouteAdBlueDialog = false,
                    showRouteResultsDialog = false,
                    routeCalculations = emptyList(),
                    routeError = null
                )
            }
        } else {
            val user = _uiState.value.activeUser
            when {
                user == null -> {
                    _uiState.update { it.copy(routeError = "Debes configurar un usuario activo para usar el modo en ruta.") }
                }
                user.combustiblePreferidoId == null -> {
                    _uiState.update { it.copy(routeError = "Debes seleccionar un combustible preferido en ajustes.") }
                }
                _uiState.value.userLocation == null -> {
                    _uiState.update { it.copy(routeError = "No se ha podido obtener tu ubicación actual.") }
                }
                else -> {
                    _lastBearing = null
                    if (_uiState.value.locationPermissionGranted) {
                        startLocationUpdates()
                    }
                    _uiState.update {
                        it.copy(
                            isRouteModeActive = true,
                            showRouteDistanceDialog = true,
                            routeDistanceKm = 20,
                            routeError = null
                        )
                    }
                }
            }
        }
    }

    fun onRouteDistanceSelected(km: Int) {
        val user = _uiState.value.activeUser
        _uiState.update { it.copy(routeDistanceKm = km, showRouteDistanceDialog = false) }

        if (user?.usaAdBlue == true) {
            _uiState.update { it.copy(showRouteAdBlueDialog = true) }
        } else {
            calculateRoute(requiresAdBlue = false)
        }
    }

    fun onRouteAdBlueConfirmed(requiresAdBlue: Boolean) {
        _uiState.update { it.copy(showRouteAdBlueDialog = false) }
        calculateRoute(requiresAdBlue = requiresAdBlue)
    }

    private fun calculateRoute(requiresAdBlue: Boolean) {
        val location = _uiState.value.userLocation ?: return
        val heading = _lastBearing ?: run {
            stopLocationUpdates()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    routeError = "No se ha detectado la dirección de movimiento. Desplázate unos metros e inténtalo de nuevo.",
                    isRouteModeActive = false
                )
            }
            return
        }

        _uiState.update { it.copy(isLoading = true, routeError = null) }

        viewModelScope.launch {
            try {
                val resultados = getOptimalStationInRouteUseCase(
                    origenLat = location.latitude,
                    origenLon = location.longitude,
                    heading = heading,
                    autonomiaKm = _uiState.value.routeDistanceKm.toDouble(),
                    requiereAdBlue = requiresAdBlue
                )

                stopLocationUpdates()

                if (resultados.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            routeError = "No se han encontrado estaciones en la ruta seleccionada.",
                            isRouteModeActive = false
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            routeCalculations = resultados,
                            showRouteResultsDialog = true
                        )
                    }
                }
            } catch (e: Exception) {
                logger.e(TAG, "Error al calcular ruta", e)
                stopLocationUpdates()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        routeError = "Error al calcular la ruta: ${e.localizedMessage}",
                        isRouteModeActive = false
                    )
                }
            }
        }
    }

    fun dismissRouteDialogs() {
        _uiState.update {
            it.copy(
                showRouteDistanceDialog = false,
                showRouteAdBlueDialog = false
            )
        }
    }

    fun dismissRouteResultsDialog() {
        _uiState.update {
            it.copy(
                showRouteResultsDialog = false,
                routeCalculations = emptyList()
            )
        }
    }

    fun clearRouteError() {
        _uiState.update { it.copy(routeError = null) }
    }

    override fun onCleared() {
        super.onCleared()
        stopLocationUpdates()
    }
}

enum class FilterType {
    OPEN_NOW, MY_FUEL_TYPE, ONLY_FAVORITES
}
