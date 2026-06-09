package com.gas_price_finder.presentation.screens.splash

import android.content.Context
import com.gas_price_finder.util.AppLogger
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gas_price_finder.R
import com.gas_price_finder.data.remote.mapper.EstacionDtoMapper
import com.gas_price_finder.domain.model.ThemeMode
import com.gas_price_finder.domain.model.User
import com.gas_price_finder.domain.usecase.user.CreateUserUseCase
import com.gas_price_finder.domain.usecase.user.GetAllUsersUseCase
import com.gas_price_finder.domain.usecase.user.SwitchUserUseCase
import com.gas_price_finder.util.Constants
import com.gas_price_finder.util.ProductNameResolver
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "SplashViewModel"

@HiltViewModel
open class SplashViewModel @Inject constructor(
    private val getAllUsersUseCase: GetAllUsersUseCase,
    private val createUserUseCase: CreateUserUseCase,
    private val switchUserUseCase: SwitchUserUseCase,
    private val logger: AppLogger,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SplashState())
    val uiState: StateFlow<SplashState> = _uiState.asStateFlow()

    init {
        loadUsers()
    }

    private fun loadUsers() {
        viewModelScope.launch {
            getAllUsersUseCase().collect { users ->
                val productos = EstacionDtoMapper.PRODUCTOS.filter { it.id != 26 }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isCreating = false,
                        users = users.map { u ->
                            UserUiModel(
                                u.id,
                                u.nombre,
                                productos.find { combustible -> combustible.id == u.combustiblePreferidoId }
                                    ?.let { p ->
                                        ProductNameResolver.resolve(context, p.id, p.nombre)
                                    } ?: context.getString(R.string.fuel_sin_definir),
                                u.radioBusquedaKm
                            )
                        },
                        hasActiveUser = users.any { it.activo }
                    )
                }
            }
        }
    }

    // ==================== UPDATES DEL FORMULARIO ====================

    fun updateNombre(nombre: String) {
        _uiState.update { it.copy(nombre = nombre, errorMessage = null) }
    }

    fun updateRadio(radioKm: Int) {
        _uiState.update { it.copy(radioKm = radioKm) }
    }

    fun updateConsumo(consumo: Float) {
        _uiState.update { it.copy(consumoMedio = consumo) }
    }

    fun updateCapacidadDeposito(capacidad: Float) {
        _uiState.update { it.copy(capacidadDeposito = capacidad) }
    }

    fun updateTema(tema: String) {
        _uiState.update { it.copy(tema = tema) }
    }

    // ==================== CREACION DE USUARIO ====================

    /**
     * Crea el primer usuario con todos los valores del formulario.
     * IMPORTANTE: combustiblePreferidoId se deja como null porque la tabla
     * productos puede estar vacía al inicio y la ForeignKey lo rechazaría.
     * El usuario seleccionará su combustible posteriormente desde Ajustes.
     */
    fun createUser() {
        val state = _uiState.value
        if (state.nombre.isBlank()) return

        _uiState.update { it.copy(isCreating = true, errorMessage = null) }

        viewModelScope.launch {
            val user = User(
                id = 0,
                nombre = state.nombre.trim(),
                combustiblePreferidoId = null,          // Se define luego en Ajustes
                radioBusquedaKm = state.radioKm,
                consumoMedioL100 = state.consumoMedio.toDouble(),
                capacidadDepositoL = state.capacidadDeposito.toDouble(),
                usaAdBlue = false,
                capacidadAdBlueL = Constants.DEFAULT_ADBLUE_L,
                umbralNotificacionBajada = Constants.DEFAULT_UMBRAL_BAJADA,
                notificacionesActivas = true,
                syncWifi = false,
                tema = ThemeMode.valueOf(state.tema),
                activo = true
            )

            createUserUseCase(user)
                .onSuccess { insertedId ->
                    logger.d(TAG, "Usuario creado con id=$insertedId")
                    // El flow de getAllUsers se actualizara automaticamente
                    // y hasActiveUser pasara a true -> navegacion via LaunchedEffect
                }
                .onFailure { e ->
                    logger.e(TAG, "Error al crear usuario: ${e.message}", e)
                    _uiState.update {
                        it.copy(
                            isCreating = false,
                            errorMessage = "No se pudo crear el perfil. Inténtalo de nuevo."
                        )
                    }
                }
        }
    }

    fun selectUser(usuarioId: Int) {
        viewModelScope.launch {
            switchUserUseCase(usuarioId)
        }
    }

    fun showCreateDialog() {
        // TODO: Overlay dialog para crear usuario adicional desde selector
    }
}