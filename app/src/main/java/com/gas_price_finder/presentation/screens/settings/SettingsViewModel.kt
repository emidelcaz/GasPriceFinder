package com.gas_price_finder.presentation.screens.settings

import android.content.Context
import com.gas_price_finder.util.AppLogger
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gas_price_finder.R
import com.gas_price_finder.data.remote.mapper.EstacionDtoMapper
import com.gas_price_finder.domain.model.ThemeMode
import com.gas_price_finder.domain.model.User
import com.gas_price_finder.domain.usecase.sync.SyncStationsUseCase
import com.gas_price_finder.domain.usecase.user.CreateUserUseCase
import com.gas_price_finder.domain.usecase.user.DeleteUserUseCase
import com.gas_price_finder.domain.usecase.user.GetActiveUserUseCase
import com.gas_price_finder.domain.usecase.user.GetAllUsersUseCase
import com.gas_price_finder.domain.usecase.user.SwitchUserUseCase
import com.gas_price_finder.domain.usecase.user.UpdateUserUseCase
import com.gas_price_finder.presentation.screens.splash.UserUiModel
import com.gas_price_finder.util.ProductNameResolver
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "SettingsViewModel"

val COMBUSTIBLES_COMUNES_IDS = listOf(4, 1, 5, 3, 6)
val GASOLEO_IDS = listOf(4, 5, 6)

@HiltViewModel
open class SettingsViewModel @Inject constructor(
    private val getActiveUserUseCase: GetActiveUserUseCase,
    private val getAllUsersUseCase: GetAllUsersUseCase,
    private val updateUserUseCase: UpdateUserUseCase,
    private val createUserUseCase: CreateUserUseCase,
    private val deleteUserUseCase: DeleteUserUseCase,
    private val switchUserUseCase: SwitchUserUseCase,
    private val syncStationsUseCase: SyncStationsUseCase,
    private val logger: AppLogger,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsState())
    val uiState: StateFlow<SettingsState> = _uiState.asStateFlow()

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    private val _navigateBackEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val navigateBackEvent: SharedFlow<Unit> = _navigateBackEvent.asSharedFlow()

    private var _hasChanges = false
    private var _batchMode = false
    private var _discardOnExit = false

    private val productos = EstacionDtoMapper.PRODUCTOS.filter { it.id != 26 }

    init {
        loadUser()
        loadAllUsers()
    }

    // ==================== CARGA DE USUARIOS ====================

    private fun loadUser() {
        viewModelScope.launch {
            getActiveUserUseCase()
                .catch { e -> logger.e(TAG, "Error en flow de usuario activo", e) }
                .collect { user ->
                    user?.let { updateStateFromUser(it) }
                        ?: logger.d(TAG, "No hay usuario activo")
                }
        }
    }

    private fun loadAllUsers() {
        viewModelScope.launch {
            getAllUsersUseCase()
                .catch { e -> logger.e(TAG, "Error cargando usuarios", e) }
                .collect { users ->
                    _uiState.update { state ->
                        state.copy(
                            users = users.map { u ->
                                UserUiModel(
                                    id = u.id,
                                    nombre = u.nombre,
                                    combustible = productos.find { p -> p.id == u.combustiblePreferidoId }?.let {p ->
                                        ProductNameResolver.resolve(context, p.id, p.nombre)
                                    }
                                        ?: context.getString(R.string.fuel_sin_definir),
                                    radioKm = u.radioBusquedaKm
                                )
                            }
                        )
                    }
                }
        }
    }

    private fun updateStateFromUser(user: User) {
        val nombreCombustible = productos.find { p -> p.id == user.combustiblePreferidoId }?.let { p ->
            ProductNameResolver.resolve(context, p.id, p.nombre)
        } ?: ""
        _uiState.update { state ->
            state.copy(
                activeUserId = user.id,
                nombre = user.nombre,
                combustiblePreferidoId = user.combustiblePreferidoId ?: 1,
                combustibleNombre = nombreCombustible,
                radioKm = user.radioBusquedaKm,
                consumoMedio = user.consumoMedioL100.toFloat(),
                capacidadDeposito = user.capacidadDepositoL.toFloat(),
                usaAdBlue = user.usaAdBlue,
                capacidadAdBlue = user.capacidadAdBlueL.toFloat(),
                tema = user.tema.name
            )
        }
    }

    // ==================== BATCH MODE (configuración de usuario) ====================

    fun startBatchMode() {
        _batchMode = true
        _discardOnExit = false
        _hasChanges = false
        logger.d(TAG, "Batch mode iniciado")
    }

    fun discardBatchChanges() {
        val currentId = _uiState.value.activeUserId
        _discardOnExit = true
        _batchMode = false
        _hasChanges = false
        // Recargar datos del usuario activo desde Room para restaurar valores originales
        viewModelScope.launch {
            getActiveUserUseCase().firstOrNull()?.let { updateStateFromUser(it) }
                ?: logger.w(TAG, "No se pudo restaurar usuario activo")
        }
    }

    fun saveBatchChanges() {
        val state = _uiState.value
        if (state.activeUserId == 0 || state.isCreatingUser) return

        viewModelScope.launch {
            try {
                val current = getActiveUserUseCase().firstOrNull() ?: return@launch
                val updated = current.copy(
                    nombre = state.nombre.trim(),
                    combustiblePreferidoId = state.combustiblePreferidoId,
                    consumoMedioL100 = state.consumoMedio.toDouble(),
                    capacidadDepositoL = state.capacidadDeposito.toDouble(),
                    usaAdBlue = state.usaAdBlue,
                    capacidadAdBlueL = state.capacidadAdBlue.toDouble()
                )
                updateUserUseCase(updated)
                _batchMode = false
                _discardOnExit = false
                _hasChanges = false
                _toastEvent.emit("Cambios guardados correctamente")
            } catch (e: Exception) {
                logger.e(TAG, "Error guardando cambios batch", e)
                _toastEvent.emit("Error al guardar los cambios")
            }
        }
    }

    fun onBatchScreenExiting() {
        if (_discardOnExit) {
            _discardOnExit = false
            _batchMode = false
            return
        }
        if (_hasChanges && _batchMode && !_uiState.value.isCreatingUser) {
            saveBatchChanges()
        }
        _batchMode = false
    }

    // ==================== CREACIÓN DE USUARIO ====================

    fun startCreatingUser() {
        _hasChanges = false
        _uiState.update {
            it.copy(
                isCreatingUser = true,
                nombre = "",
                combustiblePreferidoId = 1,
                combustibleNombre = productos.find { p -> p.id == 1 }?.let { p ->
                    ProductNameResolver.resolve(context, p.id, p.nombre)
                } ?: "",
                consumoMedio = 7.0f,
                capacidadDeposito = 50.0f,
                usaAdBlue = false,
                capacidadAdBlue = 15.0f
            )
        }
    }

    fun cancelCreatingUser() {
        _hasChanges = false
        _uiState.update { it.copy(isCreatingUser = false) }
        viewModelScope.launch {
            getActiveUserUseCase().firstOrNull()?.let { updateStateFromUser(it) }
        }
    }

    fun createUser() {
        val state = _uiState.value

        viewModelScope.launch {
            val user = User(
                id = 0,
                nombre = state.nombre.trim(),
                combustiblePreferidoId = state.combustiblePreferidoId,
                radioBusquedaKm = 10,
                consumoMedioL100 = state.consumoMedio.toDouble(),
                capacidadDepositoL = state.capacidadDeposito.toDouble(),
                usaAdBlue = state.usaAdBlue,
                capacidadAdBlueL = state.capacidadAdBlue.toDouble(),
                umbralNotificacionBajada = 0.10,
                notificacionesActivas = true,
                syncWifi = false,
                tema = ThemeMode.SISTEMA,
                activo = true
            )

            createUserUseCase(user)
                .onSuccess { insertedId ->
                    logger.d(TAG, "Usuario creado con id=$insertedId")
                    _uiState.update { it.copy(isCreatingUser = false) }
                    _hasChanges = false
                    _toastEvent.emit("Usuario creado correctamente")
                }
                .onFailure { e ->
                    logger.e(TAG, "Error al crear usuario: ${e.message}", e)
                    _toastEvent.emit("No se pudo crear el usuario")
                }
        }
    }

    // ==================== ELIMINACIÓN DE USUARIO ====================

    fun deleteUser() {
        val userId = _uiState.value.activeUserId

        viewModelScope.launch {
            deleteUserUseCase(userId)
                .onSuccess {
                    logger.d(TAG, "Usuario eliminado id=$userId")
                    _hasChanges = false
                    _toastEvent.emit("Usuario eliminado")
                    delay(150)
                    _navigateBackEvent.emit(Unit)
                }
                .onFailure { e ->
                    logger.e(TAG, "Error al eliminar usuario", e)
                    _toastEvent.emit("No se pudo eliminar el usuario")
                }
        }
    }

    fun showDeleteConfirmDialog(){
        _uiState.update { it.copy(showDeleteConfirmDialog = true) }
    }

    fun dismissDeleteConfirmDialog(){
        _uiState.update { it.copy(showDeleteConfirmDialog = false) }
    }

    // ==================== CAMBIO DE USUARIO ACTIVO ====================

    fun switchUser(userId: Int) {
        viewModelScope.launch {
            switchUserUseCase(userId)
                .onSuccess {
                    logger.d(TAG, "Usuario activo cambiado a id=$userId")
                    _toastEvent.emit("Usuario cambiado")
                }
                .onFailure { e ->
                    logger.e(TAG, "Error cambiando usuario", e)
                }
        }
    }

    // ==================== UPDATES ====================
    // En batch mode solo tocan _uiState. Fuera de batch mode guardan en Room.

    fun updateNombre(nombre: String) {
        _hasChanges = true
        _uiState.update { it.copy(nombre = nombre) }
        if (!_uiState.value.isCreatingUser && !_batchMode) {
            updateUser { it.copy(nombre = nombre) }
        }
    }

    fun updateRadio(radioKm: Int) {
        _hasChanges = true
        _uiState.update { it.copy(radioKm = radioKm) }
        if (!_batchMode) {
            updateUser { it.copy(radioBusquedaKm = radioKm) }
        }
    }

    fun updateConsumo(consumo: Float) {
        _hasChanges = true
        _uiState.update { it.copy(consumoMedio = consumo) }
        if (!_uiState.value.isCreatingUser && !_batchMode) {
            updateUser { it.copy(consumoMedioL100 = consumo.toDouble()) }
        }
    }

    fun updateCapacidadDeposito(capacidad: Float) {
        _hasChanges = true
        _uiState.update { it.copy(capacidadDeposito = capacidad) }
        if (!_uiState.value.isCreatingUser && !_batchMode) {
            updateUser { it.copy(capacidadDepositoL = capacidad.toDouble()) }
        }
    }

    fun updateCapacidadAdBlue(capacidad: Float) {
        _hasChanges = true
        _uiState.update { it.copy(capacidadAdBlue = capacidad) }
        if (!_uiState.value.isCreatingUser && !_batchMode) {
            updateUser { it.copy(capacidadAdBlueL = capacidad.toDouble()) }
        }
    }

    fun updateCombustiblePreferido(id: Int) {
        val nombreCombustible = productos.find { it.id == id }?.let { p ->
            ProductNameResolver.resolve(context, p.id, p.nombre)
        } ?: ""
        val esGasoleo = id in GASOLEO_IDS
        val eraGasoleo = _uiState.value.combustiblePreferidoId in GASOLEO_IDS

        if (esGasoleo && !eraGasoleo) {
            _hasChanges = true
            _uiState.update {
                it.copy(
                    pendingCombustibleId = id,
                    showAdBlueDialog = true
                )
            }
        } else {
            _hasChanges = true
            _uiState.update {
                it.copy(
                    combustiblePreferidoId = id,
                    combustibleNombre = nombreCombustible,
                    usaAdBlue = if (!esGasoleo) false else it.usaAdBlue
                )
            }
            if (!_uiState.value.isCreatingUser && !_batchMode) {
                updateUser {
                    it.copy(
                        combustiblePreferidoId = id,
                        usaAdBlue = if (!esGasoleo) false else it.usaAdBlue
                    )
                }
            }
            viewModelScope.launch {
                delay(150)
                _navigateBackEvent.emit(Unit)
            }
        }
    }

    fun onAdBlueDialogResponse(usaAdBlue: Boolean) {
        val pendingId = _uiState.value.pendingCombustibleId ?: return
        val nombreCombustible = productos.find { it.id == pendingId }?.let { p ->
            ProductNameResolver.resolve(context, p.id, p.nombre)
        } ?: ""

        _hasChanges = true
        _uiState.update {
            it.copy(
                combustiblePreferidoId = pendingId,
                combustibleNombre = nombreCombustible,
                usaAdBlue = usaAdBlue,
                showAdBlueDialog = false,
                pendingCombustibleId = null
            )
        }
        if (!_uiState.value.isCreatingUser && !_batchMode) {
            updateUser {
                it.copy(
                    combustiblePreferidoId = pendingId,
                    usaAdBlue = usaAdBlue
                )
            }
        }
        viewModelScope.launch {
            delay(150)
            _navigateBackEvent.emit(Unit)
        }
    }

    fun dismissAdBlueDialog() {
        _uiState.update {
            it.copy(
                showAdBlueDialog = false,
                pendingCombustibleId = null
            )
        }
    }

    fun updateUsaAdBlue(usa: Boolean) {
        _hasChanges = true
        _uiState.update { it.copy(usaAdBlue = usa) }
        if (!_uiState.value.isCreatingUser && !_batchMode) {
            updateUser { it.copy(usaAdBlue = usa) }
        }
    }

    fun updateTema(tema: String) {
        _hasChanges = true
        _uiState.update { it.copy(tema = tema) }
        if (!_batchMode) {
            updateUser { it.copy(tema = ThemeMode.valueOf(tema)) }
        }
    }

    fun cleanCache() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            try {
                syncStationsUseCase()
            } catch (e: Exception) {
                logger.e(TAG, "Error al sincronizar", e)
            } finally {
                _uiState.update { it.copy(isSyncing = false) }
            }
        }
    }

    // ==================== EXPORTAR LOGS ====================

    fun showExportDialog() {
        _uiState.update { it.copy(showExportLogsDialog = true) }
    }

    fun dismissExportDialog() {
        _uiState.update { it.copy(showExportLogsDialog = false) }
    }

    fun exportLogs(onlyToday: Boolean) {
        dismissExportDialog()
        viewModelScope.launch {
            logger.exportToPublicDownloads(onlyToday)
                .onSuccess { message ->
                    _toastEvent.emit(message)
                }
                .onFailure { e ->
                    _toastEvent.emit("Error al exportar: ${e.message}")
                }
        }
    }

    // ==================== TOAST AL SALIR (pantalla principal) ====================

    fun onScreenExiting() {
        if (_hasChanges && !_uiState.value.isCreatingUser && !_batchMode) {
            viewModelScope.launch {
                _toastEvent.emit("Cambios guardados correctamente")
                _hasChanges = false
            }
        }
    }

    // ==================== PRIVADO ====================

    private fun updateUser(transform: (User) -> User) {
        viewModelScope.launch {
            try {
                val current = getActiveUserUseCase().firstOrNull()
                if (current == null) {
                    logger.w(TAG, "No hay usuario activo para actualizar")
                    return@launch
                }
                updateUserUseCase(transform(current))
            } catch (e: Exception) {
                logger.e(TAG, "Error al actualizar usuario", e)
            }
        }
    }
}