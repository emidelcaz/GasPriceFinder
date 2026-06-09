package com.gas_price_finder.presentation.screens.pricehistory

import com.gas_price_finder.domain.model.PriceHistory
import com.gas_price_finder.domain.model.Station

data class PriceHistoryState(
    val station: Station? = null,
    val priceHistory: List<PriceHistory> = emptyList(),
    val filteredHistory: List<PriceHistory> = emptyList(),
    val selectedProductoId: Int? = null,
    val selectedTimeFilter: TimeFilter = TimeFilter.ALL,
    val isLoading: Boolean = true,
    val minPrice: String = "—",
    val maxPrice: String = "—"
)

enum class TimeFilter(val label: String, val days: Long) {
    WEEK("1S", 7),
    MONTH("1M", 30),
    MONTHS_3("3M", 90),
    MONTHS_6("6M", 180),
    YEAR("1A", 365),
    ALL("Todas", Long.MAX_VALUE)
}
