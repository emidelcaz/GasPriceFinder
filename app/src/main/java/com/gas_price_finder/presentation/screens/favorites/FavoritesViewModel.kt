package com.gas_price_finder.presentation.screens.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gas_price_finder.domain.usecase.favorite.GetFavoritesUseCase
import com.gas_price_finder.domain.usecase.favorite.RemoveFavoriteUseCase
import com.gas_price_finder.domain.usecase.station.GetStationByIdUseCase
import com.gas_price_finder.domain.usecase.user.GetActiveUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val getFavoritesUseCase: GetFavoritesUseCase,
    private val removeFavoriteUseCase: RemoveFavoriteUseCase,
    private val getActiveUserUseCase: GetActiveUserUseCase,
    private val getStationByIdUseCase: GetStationByIdUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesState())
    val uiState: StateFlow<FavoritesState> = _uiState.asStateFlow()

    init {
        loadFavorites()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun loadFavorites() {
        viewModelScope.launch {
            getActiveUserUseCase()
                .flatMapLatest { user ->
                    user?.id?.let { getFavoritesUseCase(it) } ?: flowOf(emptyList())
                }
                .collect { favorites ->
                    val user = getActiveUserUseCase().firstOrNull()
                    val productoId = user?.combustiblePreferidoId
                    val uiModels = favorites.map { favorite ->
                        val station = getStationByIdUseCase(favorite.estacionId).firstOrNull()
                        val precio = productoId?.let { pid ->
                            station?.precios?.find { it.productoId == pid }
                        }
                        FavoriteItemUiModel(
                            estacionId = favorite.estacionId,
                            nombreSnapshot = favorite.nombreSnapshot,
                            rotulo = station?.rotulo,           // ← AÑADIDO
                            municipio = station?.municipio,
                            precioActual = precio?.let { "%.3f €".format(it.precio) },
                            latitudSnapshot = favorite.latitudSnapshot,
                            longitudSnapshot = favorite.longitudSnapshot
                        )
                    }
                    _uiState.update { it.copy(favorites = uiModels) }
                }
        }
    }

    fun removeFavorite(estacionId: String) {
        viewModelScope.launch {
            val userId = getActiveUserUseCase().firstOrNull()?.id ?: return@launch
            removeFavoriteUseCase(userId, estacionId)
        }
    }
}