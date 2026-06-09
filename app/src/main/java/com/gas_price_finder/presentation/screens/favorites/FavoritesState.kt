package com.gas_price_finder.presentation.screens.favorites

data class FavoritesState(
    val favorites: List<FavoriteItemUiModel> = emptyList()
)

data class FavoriteItemUiModel(
    val estacionId: String,
    val nombreSnapshot: String,
    val rotulo: String?,
    val municipio: String?,
    val precioActual: String?,
    val latitudSnapshot: Double?,
    val longitudSnapshot: Double?
)