package com.gas_price_finder.presentation.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.widget.Toast
import androidx.compose.ui.res.stringResource
import com.gas_price_finder.R
import com.gas_price_finder.data.remote.mapper.EstacionDtoMapper
import com.gas_price_finder.util.ProductNameResolver

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsFuelScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val productos = remember { EstacionDtoMapper.PRODUCTOS.filter { it.id != 26 } }

    val comunes = remember(productos) {
        COMBUSTIBLES_COMUNES_IDS.mapNotNull { id -> productos.find { it.id == id } }
    }
    val otros = remember(productos) {
        productos.filter { it.id !in COMBUSTIBLES_COMUNES_IDS }
    }

    // Escuchar evento de navegación del ViewModel
    LaunchedEffect(Unit) {
        viewModel.navigateBackEvent.collect {
            onNavigateBack()
        }
    }

    // Toast al salir
    DisposableEffect(Unit) {
        onDispose { viewModel.onScreenExiting() }
    }

    // Toast events
    LaunchedEffect(Unit) {
        viewModel.toastEvent.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tipo_de_combustible)) },
                navigationIcon = {
                    TextButton(
                        onClick = {
                            viewModel.dismissAdBlueDialog()
                            onNavigateBack()
                        }
                    ) {
                        Text(stringResource(R.string.atras))
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
        ) {
            CombustibleSectionHeader(stringResource(R.string.mas_comunes))
            comunes.forEach { producto ->
                CombustibleListItem(
                    nombre = ProductNameResolver.resolve(producto.id, producto.nombre),
                    isSelected = producto.id == uiState.combustiblePreferidoId,
                    onClick = { viewModel.updateCombustiblePreferido(producto.id) }
                )
            }

            CombustibleSectionHeader(stringResource(R.string.otros))
            otros.forEach { producto ->
                CombustibleListItem(
                    nombre = ProductNameResolver.resolve(producto.id, producto.nombre),
                    isSelected = producto.id == uiState.combustiblePreferidoId,
                    onClick = { viewModel.updateCombustiblePreferido(producto.id) }
                )
            }
        }
    }

    // ====== DIALOGO ADBLUE ======
    if (uiState.showAdBlueDialog) {
        AdBlueConfirmDialog(
            onConfirm = { usaAdBlue ->
                viewModel.onAdBlueDialogResponse(usaAdBlue)
            },
            onDismiss = {
                viewModel.dismissAdBlueDialog()
            }
        )
    }
}

// ==================== COMPONENTES ====================

@Composable
internal fun CombustibleSectionHeader(title: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        )
    }
}

@Composable
internal fun CombustibleListItem(
    nombre: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = nombre,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Seleccionado",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
    HorizontalDivider(
        modifier = Modifier.padding(start = 16.dp),
        thickness = 0.5.dp
    )
}

@Composable
internal fun AdBlueConfirmDialog(
    onConfirm: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.adblue)) },
        text = {
            Text(stringResource(R.string.dialog_adblue))
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(true) }) {
                Text(stringResource(R.string.si))
            }
        },
        dismissButton = {
            TextButton(onClick = { onConfirm(false) }) {
                Text(stringResource(R.string.no))
            }
        }
    )
}
