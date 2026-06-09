package com.gas_price_finder.presentation.screens.settings

import com.gas_price_finder.presentation.screens.splash.UserUiModel

data class SettingsState(
    // -- Datos del usuario activo / en edicion --
    val nombre: String = "",
    val combustiblePreferidoId: Int = 1,
    val combustibleNombre: String = "",
    val radioKm: Int = 10,
    val consumoMedio: Float = 7.0f,
    val capacidadDeposito: Float = 50.0f,
    val usaAdBlue: Boolean = false,
    val capacidadAdBlue: Float = 15.0f,
    val tema: String = "SISTEMA",

    // -- Estado de UI --
    val isSyncing: Boolean = false,
    val showAdBlueDialog: Boolean = false,
    val showExportLogsDialog: Boolean = false,
    val pendingCombustibleId: Int? = null,
    val activeUserId: Int = 0,
    val showDeleteConfirmDialog: Boolean = false,

    // -- Listado de usuarios --
    val users: List<UserUiModel> = emptyList(),
    val isCreatingUser: Boolean = false
)