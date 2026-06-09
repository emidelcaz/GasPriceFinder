package com.gas_price_finder.presentation.screens.splash

import com.gas_price_finder.util.Constants.DEFAULT_CONSUMO_L100
import com.gas_price_finder.util.Constants.DEFAULT_DEPOSITO_L
import com.gas_price_finder.util.Constants.DEFAULT_RADIO_KM

data class SplashState(
    val isLoading: Boolean = true,
    val isCreating: Boolean = false,
    val errorMessage: String? = null,
    val users: List<UserUiModel> = emptyList(),
    val hasActiveUser: Boolean? = null,

    // Formulario de configuracion inicial (primer usuario)
    val nombre: String = "",
    val radioKm: Int = DEFAULT_RADIO_KM,
    val consumoMedio: Float = DEFAULT_CONSUMO_L100,
    val capacidadDeposito: Float = DEFAULT_DEPOSITO_L,
    val tema: String = "SISTEMA"
)