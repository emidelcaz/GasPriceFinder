package com.gas_price_finder.presentation.screens.pricehistory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gas_price_finder.domain.repository.UserRepository
import com.gas_price_finder.domain.usecase.price.GetPriceHistoryUseCase
import com.gas_price_finder.domain.usecase.station.GetStationByIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PriceHistoryViewModel @Inject constructor(
    private val getStationByIdUseCase: GetStationByIdUseCase,
    private val getPriceHistoryUseCase: GetPriceHistoryUseCase,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PriceHistoryState())
    val uiState: StateFlow<PriceHistoryState> = _uiState.asStateFlow()

    fun load(stationId: String) {
        viewModelScope.launch {
            getStationByIdUseCase(stationId).collect { station ->
                if (station != null) {
                    val activeUser = userRepository.getActiveUserSync()
                    val preferredProductId = activeUser?.combustiblePreferidoId
                    val selectedProduct = when {
                        preferredProductId != null && station.precios.any { it.productoId == preferredProductId } -> preferredProductId
                        else -> station.precios.firstOrNull()?.productoId
                    }
                    _uiState.update {
                        it.copy(
                            station = station,
                            selectedProductoId = selectedProduct,
                            isLoading = false
                        )
                    }
                    selectedProduct?.let { loadHistory(stationId, it) }
                }
            }
        }
    }

    fun selectProduct(productoId: Int) {
        _uiState.update { it.copy(selectedProductoId = productoId) }
        val stationId = _uiState.value.station?.ideess ?: return
        loadHistory(stationId, productoId)
    }

    fun selectTimeFilter(filter: TimeFilter) {
        _uiState.update { it.copy(selectedTimeFilter = filter) }
        applyFilter()
    }

    private fun loadHistory(stationId: String, productoId: Int) {
        viewModelScope.launch {
            val history = getPriceHistoryUseCase(stationId, productoId)
            _uiState.update { it.copy(priceHistory = history) }
            applyFilter()
        }
    }

    private fun applyFilter() {
        val state = _uiState.value
        val now = System.currentTimeMillis()
        val cutoff = if (state.selectedTimeFilter.days == Long.MAX_VALUE) {
            0L
        } else {
            now - (state.selectedTimeFilter.days * 24 * 60 * 60 * 1000)
        }

        val filtered = state.priceHistory.filter { it.fechaCaptura >= cutoff }
            .sortedBy { it.fechaCaptura }

        val prices = filtered.map { it.precio }
        val min = prices.minOrNull()
        val max = prices.maxOrNull()

        _uiState.update {
            it.copy(
                filteredHistory = filtered,
                minPrice = min?.let { p -> "%.3f".format(p).replace('.', ',') + " €/l" } ?: "—",
                maxPrice = max?.let { p -> "%.3f".format(p).replace('.', ',') + " €/l" } ?: "—"
            )
        }
    }
}
