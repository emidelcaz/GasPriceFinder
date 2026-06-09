package com.gas_price_finder.presentation.screens.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gas_price_finder.domain.usecase.station.GetNearbyStationsUseCase
import com.gas_price_finder.domain.usecase.user.GetActiveUserUseCase
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ListViewModel @Inject constructor(
    private val getNearbyStationsUseCase: GetNearbyStationsUseCase,
    private val getActiveUserUseCase: GetActiveUserUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ListState())
    val uiState: StateFlow<ListState> = _uiState.asStateFlow()

    init {
        loadStations()
    }

    private fun loadStations() {
        viewModelScope.launch {
            // Usar ubicación real en producción
            val location = LatLng(40.4168, -3.7038)
            getNearbyStationsUseCase(location.latitude, location.longitude)
                .collect { stations ->
                    _uiState.update { it.copy(stations = stations) }
                }
        }
    }
}