package com.gas_price_finder.presentation.screens.detail

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gas_price_finder.R
import com.gas_price_finder.domain.model.Favorite
import com.gas_price_finder.data.local.dao.EstacionRecienteDao
import com.gas_price_finder.data.local.entity.EstacionRecienteEntity
import com.gas_price_finder.domain.model.Station
import com.gas_price_finder.domain.usecase.favorite.AddFavoriteUseCase
import com.gas_price_finder.domain.usecase.favorite.IsFavoriteUseCase
import com.gas_price_finder.domain.usecase.favorite.RemoveFavoriteUseCase
import com.gas_price_finder.domain.usecase.price.CalculateSavingsUseCase
import com.gas_price_finder.domain.usecase.price.GetPriceHistoryUseCase
import com.gas_price_finder.domain.usecase.station.GetStationByIdUseCase
import com.gas_price_finder.domain.usecase.station.GetNearbyStationsUseCase
import com.gas_price_finder.domain.usecase.sync.SyncStationsUseCase
import com.gas_price_finder.domain.usecase.user.GetActiveUserUseCase
import com.gas_price_finder.presentation.screens.map.PriceTerciles
import com.gas_price_finder.util.GeoUtils
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val getStationByIdUseCase: GetStationByIdUseCase,
    private val getNearbyStationsUseCase: GetNearbyStationsUseCase,
    private val calculateSavingsUseCase: CalculateSavingsUseCase,
    private val isFavoriteUseCase: IsFavoriteUseCase,
    private val addFavoriteUseCase: AddFavoriteUseCase,
    private val removeFavoriteUseCase: RemoveFavoriteUseCase,
    private val getActiveUserUseCase: GetActiveUserUseCase,
    private val getPriceHistoryUseCase: GetPriceHistoryUseCase,
    private val syncStationsUseCase: SyncStationsUseCase,
    private val estacionRecienteDao: EstacionRecienteDao,
    private val fusedLocationClient: FusedLocationProviderClient,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailState())
    val uiState: StateFlow<DetailState> = _uiState.asStateFlow()

    private var currentUserId: Int? = null

    init {
        viewModelScope.launch {
            getActiveUserUseCase().collect { user ->
                currentUserId = user?.id
                val station = _uiState.value.station
                val effectiveProductoId = user?.combustiblePreferidoId ?: station?.precios?.firstOrNull()?.productoId
                _uiState.update { it.copy(user = user, effectiveProductoId = effectiveProductoId) }
                if (station != null) {
                    recalcPricesAndSavings()
                    updateDateTexts(station)
                    loadPriceHistory(station)
                    loadSavings(station)
                }
            }
        }
    }

    fun loadStation(stationId: String) {
        viewModelScope.launch {
            getStationByIdUseCase(stationId).collect { station ->
                val user = _uiState.value.user
                val effectiveProductoId = user?.combustiblePreferidoId ?: station?.precios?.firstOrNull()?.productoId
                _uiState.update {
                    it.copy(
                        station = station,
                        fallbackName = station?.rotulo,
                        isLoading = false,
                        effectiveProductoId = effectiveProductoId
                    )
                }

                station?.let { s ->
                    checkFavorite(s.ideess)
                    registerRecentStation(s.ideess)
                    loadLocationAndDistance(s)
                    loadPriceHistory(s)
                    updateDateTexts(s)
                    recalcPricesAndSavings()
                    loadSavings(s)
                }
            }
        }
    }

    private fun loadLocationAndDistance(station: Station) {
        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    val userLatLng = location?.let { LatLng(it.latitude, it.longitude) }
                    val distance = if (userLatLng != null) {
                        GeoUtils.haversine(
                            userLatLng.latitude, userLatLng.longitude,
                            station.latitud, station.longitud
                        )
                    } else 0.0
                    _uiState.update { it.copy(distanceKm = distance) }
                    recalcPricesAndSavings()
                    loadSavings(station)
                }
                .addOnFailureListener {
                    _uiState.update { it.copy(distanceKm = 0.0) }
                    recalcPricesAndSavings()
                    loadSavings(station)
                }
        } catch (_: SecurityException) {
            _uiState.update { it.copy(distanceKm = 0.0) }
            recalcPricesAndSavings()
            loadSavings(station)
        }
    }

    private fun loadSavings(station: Station) {
        viewModelScope.launch {
            val user = _uiState.value.user ?: return@launch
            val productoId = _uiState.value.effectiveProductoId ?: return@launch

            getNearbyStationsUseCase(station.latitud, station.longitud)
                .collect { nearby ->
                    val savings = calculateSavingsUseCase(
                        station,
                        nearby,
                        user.capacidadDepositoL
                    )
                    val terciles = calculatePriceTerciles(nearby, productoId)
                    _uiState.update { it.copy(savings = savings, priceTerciles = terciles) }
                    recalcPricesAndSavings()
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

    private fun loadPriceHistory(station: Station) {
        viewModelScope.launch {
            val productoId = _uiState.value.effectiveProductoId ?: return@launch
            val history = getPriceHistoryUseCase(station.ideess, productoId)
            val price = station.getPrecio(productoId)
            val lastChangedText = history
                .maxByOrNull { it.fechaCaptura }
                ?.let { relativeTime(it.fechaCaptura) }
                ?: (price?.fechaActualizacion?.let { relativeTime(it) } ?: "")
            _uiState.update {
                it.copy(
                    priceHistory = history,
                    lastPriceChangeText = lastChangedText
                )
            }
        }
    }

    private fun updateDateTexts(station: Station){
        val productoId = _uiState.value.effectiveProductoId
        val price = productoId?.let { station.getPrecio(it) }
        val fecha = price?.fechaActualizacion ?: station.fechaActualizacion

        _uiState.update{
            it.copy(
                lastUpdateText = fecha?.let { f -> relativeTime(f) } ?: ""
            )
        }
    }

    private fun recalcPricesAndSavings() {
        val state = _uiState.value
        val station = state.station ?: return
        val user = state.user ?: return
        val productoId = state.effectiveProductoId ?: return
        val price = station.getPrecio(productoId)?.precio ?: return
        val distance = state.distanceKm

        val depositPrice = price * user.capacidadDepositoL
        val depositPriceStr = "%.2f".format(depositPrice).replace('.', ',') + " €"

        val costToArrive = (distance / 100.0) * user.consumoMedioL100 * price
        val costToArriveStr = "%.2f".format(costToArrive).replace('.', ',') + " €"

        val savings = state.savings
        val diffStr = if (savings != null) {
            val diff = -savings.ahorroVsMedia
            val sign = when {
                diff > 0 -> "+"
                diff < 0 -> ""
                else -> ""
            }
            "$sign${"%.2f".format(diff).replace('.', ',')} €"
        } else "—"

        _uiState.update {
            it.copy(
                depositPrice = depositPriceStr,
                costToArrive = costToArriveStr,
                diffVsMedia = diffStr
            )
        }
    }

    private fun relativeTime(timestamp: Long): String {
        val diffMs = System.currentTimeMillis() - timestamp
        val minutes = (diffMs / (1000 * 60)).toInt()
        val hours = minutes / 60
        val days = hours / 24

        return when {
            minutes < 1 -> context.getString(R.string.hace_un_momento)
            minutes < 60 -> context.getString(R.string.hace_min, minutes)
            hours < 24 -> context.getString(R.string.hace_h, hours)
            days == 1 -> context.getString(R.string.hace_1_d)
            else -> context.getString(R.string.hace_d, days)
        }
    }

    private fun registerRecentStation(stationId: String) {
        viewModelScope.launch {
            currentUserId?.let { userId ->
                estacionRecienteDao.insert(
                    EstacionRecienteEntity(
                        estacionId = stationId,
                        usuarioId = userId
                    )
                )
            }
        }
    }

    private fun checkFavorite(stationId: String) {
        viewModelScope.launch {
            currentUserId?.let { userId ->
                val isFav = isFavoriteUseCase(userId, stationId)
                _uiState.update { it.copy(isFavorite = isFav) }
            }
        }
    }

    fun toggleFavorite() {
        val station = _uiState.value.station ?: return
        val userId = currentUserId ?: return

        viewModelScope.launch {
            if (_uiState.value.isFavorite) {
                removeFavoriteUseCase(userId, station.ideess)
                _uiState.update { it.copy(isFavorite = false) }
            } else {
                val favorite = Favorite(
                    usuarioId = userId,
                    estacionId = station.ideess,
                    nombreSnapshot = station.rotulo,
                    direccionSnapshot = station.direccion,
                    latitudSnapshot = station.latitud,
                    longitudSnapshot = station.longitud
                )
                addFavoriteUseCase(favorite)
                _uiState.update { it.copy(isFavorite = true) }
            }
        }
    }

    fun togglePricesExpanded() {
        _uiState.update { it.copy(isPricesExpanded = !it.isPricesExpanded) }
    }

    fun toggleScheduleExpanded() {
        _uiState.update { it.copy(isScheduleExpanded = !it.isScheduleExpanded) }
    }

    fun requestSync() {
        viewModelScope.launch {
            syncStationsUseCase()
        }
    }
}
