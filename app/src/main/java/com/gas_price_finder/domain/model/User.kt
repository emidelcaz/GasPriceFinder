package com.gas_price_finder.domain.model

data class User(
    val id: Int = 0,
    val nombre: String,
    val combustiblePreferidoId: Int? = null,
    val radioBusquedaKm: Int = 10, // Por defecto 10 kms
    val consumoMedioL100: Double = 7.0,
    val capacidadDepositoL: Double = 50.0,
    val usaAdBlue: Boolean = false,
    val capacidadAdBlueL: Double = 15.0,
    val umbralNotificacionBajada: Double = 0.10, //Umbral bajada de precio, en céntimos
    val notificacionesActivas: Boolean = true,
    val syncWifi: Boolean = false, // Sincronización solo con Wifi, por defecto falso
    val tema: ThemeMode = ThemeMode.SISTEMA,
    val activo: Boolean = false
)

enum class ThemeMode{
        CLARO, OSCURO, SISTEMA
}