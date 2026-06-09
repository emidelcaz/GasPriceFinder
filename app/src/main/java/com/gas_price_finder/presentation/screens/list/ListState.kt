package com.gas_price_finder.presentation.screens.list

import com.gas_price_finder.domain.model.Station

data class ListState(
    val stations: List<Station> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)