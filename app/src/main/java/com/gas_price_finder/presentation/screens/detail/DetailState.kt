package com.gas_price_finder.presentation.screens.detail

import com.gas_price_finder.domain.model.PriceHistory
import com.gas_price_finder.domain.model.SavingsCalculation
import com.gas_price_finder.domain.model.Station
import com.gas_price_finder.domain.model.User
import com.gas_price_finder.presentation.screens.map.PriceTerciles

data class DetailState(
    val isLoading: Boolean = true,
    val station: Station? = null,
    val isFavorite: Boolean = false,
    val savings: SavingsCalculation? = null,
    val fallbackName: String? = null,
    val priceHistory: List<PriceHistory> = emptyList(),
    val distanceKm: Double = 0.0,
    val depositPrice: String = "—",
    val costToArrive: String = "—",
    val diffVsMedia: String = "—",
    val isPricesExpanded: Boolean = false,
    val isScheduleExpanded: Boolean = false,
    val lastUpdateText: String = "",
    val lastPriceChangeText: String = "",
    val user: User? = null,
    val priceTerciles: PriceTerciles? = null,
    val effectiveProductoId: Int? = null
)
