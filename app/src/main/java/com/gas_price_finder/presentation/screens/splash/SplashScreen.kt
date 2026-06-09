package com.gas_price_finder.presentation.screens.splash

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gas_price_finder.R

@Composable
fun SplashScreen(
    onNavigateToMap: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.hasActiveUser) {
        if (uiState.hasActiveUser == true) {
            onNavigateToMap()
        }
    }

    Scaffold { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            when {
                uiState.isLoading -> CircularProgressIndicator()
                uiState.users.isEmpty() -> {
                    FirstTimeSetup(
                        state = uiState,
                        onUpdateNombre = { viewModel.updateNombre(it) },
                        onUpdateRadio = { viewModel.updateRadio(it) },
                        onUpdateConsumo = { viewModel.updateConsumo(it) },
                        onUpdateCapacidadDeposito = { viewModel.updateCapacidadDeposito(it) },
                        onUpdateTema = { viewModel.updateTema(it) },
                        onCreateUser = { viewModel.createUser() }
                    )
                }
                else -> {
                    UserSelector(
                        users = uiState.users,
                        onSelectUser = { userId ->
                            viewModel.selectUser(userId)
                        },
                        onCreateNewUser = { viewModel.showCreateDialog() }
                    )
                }
            }
        }
    }
}

// ==================== TEMAS ====================

private val TEMAS = listOf("SISTEMA", "CLARO", "OSCURO")

// ==================== PRIMERA CONFIGURACION ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FirstTimeSetup(
    state: SplashState,
    onUpdateNombre: (String) -> Unit,
    onUpdateRadio: (Int) -> Unit,
    onUpdateConsumo: (Float) -> Unit,
    onUpdateCapacidadDeposito: (Float) -> Unit,
    onUpdateTema: (String) -> Unit,
    onCreateUser: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))

        Text(
            stringResource(R.string.bienvenido_a_gas_price_finder),
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.configura_tu_perfil_para_comenzar),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.puedes_elegir_tu_combustible_preferido_mas_tarde_en_ajustes),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(24.dp))

        // Nombre
        OutlinedTextField(
            value = state.nombre,
            onValueChange = onUpdateNombre,
            label = { Text(stringResource(R.string.tu_nombre) + " *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = state.errorMessage != null && state.nombre.isBlank()
        )
        Spacer(Modifier.height(16.dp))

        // Radio de busqueda
        SliderSetting(
            title = stringResource(R.string.radio_de_busqueda),
            value = state.radioKm.toFloat(),
            range = 1f..50f,
            unit = "km",
            onValueChange = { onUpdateRadio(it.toInt()) }
        )
        Spacer(Modifier.height(8.dp))

        // Consumo medio
        SliderSetting(
            title = stringResource(R.string.consumo_medio),
            value = state.consumoMedio,
            range = 3f..20f,
            unit = "L/100km",
            onValueChange = onUpdateConsumo
        )
        Spacer(Modifier.height(8.dp))

        // Capacidad depósito
        SliderSetting(
            title = stringResource(R.string.capacidad_del_deposito),
            value = state.capacidadDeposito,
            range = 20f..100f,
            unit = "L",
            onValueChange = onUpdateCapacidadDeposito
        )
        Spacer(Modifier.height(8.dp))

        // Tema
        var temaExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = temaExpanded,
            onExpandedChange = { temaExpanded = !temaExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = state.tema,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.tema_de_la_app)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = temaExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = temaExpanded,
                onDismissRequest = { temaExpanded = false }
            ) {
                TEMAS.forEach { tema ->
                    DropdownMenuItem(
                        text = { Text(tema) },
                        onClick = {
                            onUpdateTema(tema)
                            temaExpanded = false
                        }
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))

        // Mensaje de error
        state.errorMessage?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Boton comenzar
        Button(
            onClick = onCreateUser,
            enabled = state.nombre.isNotBlank() && !state.isCreating,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isCreating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.creando_perfil))
            } else {
                Text(stringResource(R.string.comenzar))
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

// ==================== COMPONENTES REUTILIZABLES ====================

@Composable
internal fun SliderSetting(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    unit: String,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(
                "%.1f".format(value) + " $unit",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ==================== SELECTOR DE USUARIO EXISTENTE ====================

@Composable
private fun UserSelector(
    users: List<UserUiModel>,
    onSelectUser: (Int) -> Unit,
    onCreateNewUser: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Gas Price Finder", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(32.dp))
        Text("Usuarios disponibles", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))

        users.forEach { user ->
            Card(
                onClick = { onSelectUser(user.id) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                ListItem(
                    headlineContent = { Text(user.nombre) },
                    supportingContent = { Text("${user.combustible} · ${user.radioKm}km") }
                )
            }
        }

        Spacer(Modifier.height(16.dp))
/*        OutlinedButton(
            onClick = onCreateNewUser,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("+ Nuevo perfil")
        }*/
    }
}

// ==================== PREVIEW ====================

@Preview(showBackground = true, device = "id:pixel_5")
@Composable
fun PreviewFirstTimeSetup() {
    MaterialTheme {
        FirstTimeSetup(
            state = SplashState(
                nombre = "Emilio",
                radioKm = 15,
                consumoMedio = 6.5f,
                capacidadDeposito = 55f,
                tema = "SISTEMA"
            ),
            onUpdateNombre = {},
            onUpdateRadio = {},
            onUpdateConsumo = {},
            onUpdateCapacidadDeposito = {},
            onUpdateTema = {},
            onCreateUser = {}
        )
    }
}