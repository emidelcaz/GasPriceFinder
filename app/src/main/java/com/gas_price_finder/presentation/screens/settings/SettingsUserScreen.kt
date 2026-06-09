package com.gas_price_finder.presentation.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gas_price_finder.R

// ====== CHIPS PREDEFINIDOS ======
private val CHIPS_DEPOSITO = listOf(40, 45, 50, 55, 60, 65)
private val CHIPS_ADBLUE = listOf(5, 10, 15, 20, 25, 30)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsUserScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToFuel: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // Al entrar: activar modo batch
    LaunchedEffect(Unit) {
        viewModel.startBatchMode()
    }

    // Al salir: guardar cambios automaticamente (si no se descartó)
    DisposableEffect(Unit) {
        onDispose {
            viewModel.onBatchScreenExiting()
        }
    }

    //Escuchar evento de navegación del ViewModel
    LaunchedEffect(Unit){
        viewModel.navigateBackEvent.collect {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (uiState.isCreatingUser) stringResource(R.string.nuevo_usuario)
                        else stringResource(R.string.configuracion_de_usuario)
                    )
                },
                navigationIcon = {
                    TextButton(
                        onClick = {
                            if (uiState.isCreatingUser) {
                                viewModel.cancelCreatingUser()
                            } else {
                                viewModel.discardBatchChanges()
                            }
                            onNavigateBack()
                        }
                    ) {
                        Text(stringResource(R.string.cancelar))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // ====== NOMBRE ======
            SettingLabel(stringResource(R.string.nombre_del_usuario))
            OutlinedTextField(
                value = uiState.nombre,
                onValueChange = { viewModel.updateNombre(it) },
                placeholder = { Text(stringResource(R.string.tu_nombre)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // ====== COMBUSTIBLE ======
            SettingLabel(stringResource(R.string.tipo_de_combustible))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = uiState.combustibleNombre.ifBlank { stringResource(R.string.seleccionar) },
                    style = MaterialTheme.typography.bodyLarge
                )
                IconButton(onClick = onNavigateToFuel) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = stringResource(R.string.seleccionar_combustible)
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // ====== CAPACIDAD DEPOSITO ======
            SettingLabel(stringResource(R.string.capacidad_de_deposito))
            Text(
                text = stringResource(R.string.litros, uiState.capacidadDeposito.toInt()),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            DepositoChips(
                valores = CHIPS_DEPOSITO,
                seleccionado = uiState.capacidadDeposito.toInt(),
                onSelect = { viewModel.updateCapacidadDeposito(it.toFloat()) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Slider(
                value = uiState.capacidadDeposito,
                onValueChange = { viewModel.updateCapacidadDeposito(it) },
                valueRange = 20f..100f,
                steps = 50,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // ====== CONSUMO ======
            SettingLabel(stringResource(R.string.consumo_de_carburante))
            Text(
                text = "${"%.1f".format(uiState.consumoMedio)} l/100km",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Slider(
                value = uiState.consumoMedio,
                onValueChange = { viewModel.updateConsumo(it) },
                valueRange = 3f..20f,
                steps = 50,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            // ====== ADBLUE (solo si es gasóleo) ======
            if (uiState.combustiblePreferidoId in GASOLEO_IDS) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "AdBlue",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = stringResource(R.string.activalo_si_tu_vehiculo_utiliza_adblue),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.usaAdBlue,
                        onCheckedChange = { viewModel.updateUsaAdBlue(it) }
                    )
                }

                if (uiState.usaAdBlue) {
                    Spacer(modifier = Modifier.height(16.dp))
                    SettingLabel(stringResource(R.string.capacidad_del_deposito_de_adblue))
                    Text(
                        text = stringResource(
                            R.string.litros_adblue,
                            uiState.capacidadAdBlue.toInt()
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    DepositoChips(
                        valores = CHIPS_ADBLUE,
                        seleccionado = uiState.capacidadAdBlue.toInt(),
                        onSelect = { viewModel.updateCapacidadAdBlue(it.toFloat()) }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Slider(
                        value = uiState.capacidadAdBlue,
                        onValueChange = { viewModel.updateCapacidadAdBlue(it) },
                        valueRange = 5f..30f,
                        steps = 50,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ====== BOTONES EDICION (solo en modo edicion) ======
            if (!uiState.isCreatingUser) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        viewModel.saveBatchChanges()
                        onNavigateBack()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.guardar))
                }

                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { viewModel.showDeleteConfirmDialog() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = SolidColor(MaterialTheme.colorScheme.error)
                    )
                ) {
                    Text(stringResource(R.string.eliminar_usuario))
                }
            }

            // ====== BOTON CREAR (solo en modo creacion) ======
            if (uiState.isCreatingUser) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        viewModel.createUser()
                        onNavigateBack()
                    },
                    enabled = uiState.nombre.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.crear_usuario))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
    // ====== DIALOGO CONFIRMACION ELIMINAR ======
    if (uiState.showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteConfirmDialog() },
            title = { Text(stringResource(R.string.eliminar_usuario)) },
            text = { Text(stringResource(R.string.dialog_eliminar_user)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteUser()
                        viewModel.dismissDeleteConfirmDialog()
                    }
                ) {
                    Text(stringResource(R.string.eliminar), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDeleteConfirmDialog() }) {
                    Text(stringResource(R.string.cancelar))
                }
            }
        )
    }
}

// ==================== COMPONENTES ====================

@Composable
private fun SettingLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
private fun DepositoChips(
    valores: List<Int>,
    seleccionado: Int,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        valores.forEach { valor ->
            val isSelected = valor == seleccionado
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(valor) },
                label = {
                    Text(
                        text = "${valor}l",
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                modifier = Modifier.padding(horizontal = 2.dp)
            )
        }
    }
}