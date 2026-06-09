package com.gas_price_finder.presentation.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.os.Build
import android.widget.Toast
import androidx.compose.ui.res.stringResource
import com.gas_price_finder.R
import com.gas_price_finder.presentation.screens.splash.UserUiModel
import com.gas_price_finder.util.RequestStoragePermission
import com.gas_price_finder.util.hasStoragePermission

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToUserConfig: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var pendingExportAfterPermission by remember { mutableStateOf(false) }

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // ====== TITULO ======
        Text(
            text = stringResource(R.string.ajustes),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        // ====== USUARIOS ======
        SectionTitle(stringResource(R.string.usuarios))

        uiState.users.forEach { user ->
            val isActive = user.id == uiState.activeUserId
            UserCard(
                user = user,
                isActive = isActive,
                combustible = user.combustible,
                onSelect = { viewModel.switchUser(user.id) },
                onEdit = {
                    if (isActive) {
                        onNavigateToUserConfig()
                    }
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Botón añadir usuario
        OutlinedButton(
            onClick = {
                viewModel.startCreatingUser()
                onNavigateToUserConfig()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.anadir_usuario))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ====== CONFIGURACION ======
        SectionTitle(stringResource(R.string.configuracion))

        // -- Gasolineras: Slider de rango KM --
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LocalGasStation,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.gasolineras),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = stringResource(R.string.radio_de_busqueda_km, uiState.radioKm),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Slider(
            value = uiState.radioKm.toFloat(),
            onValueChange = { viewModel.updateRadio(it.toInt()) },
            valueRange = 1f..50f,
            steps = 48,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // -- Tema --
        TemaDropdown(
            temaActual = uiState.tema,
            onTemaChange = { viewModel.updateTema(it) }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ====== LIMPIAR CACHE ======
        Button(
            onClick = { viewModel.cleanCache() },
            enabled = !uiState.isSyncing,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (uiState.isSyncing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.actualizando))
            } else {
                Text(stringResource(R.string.limpiar_cache))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ====== EXPORTAR LOGS ======
        OutlinedButton(
            onClick = {
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P || context.hasStoragePermission()) {
                    viewModel.showExportDialog()
                } else {
                    pendingExportAfterPermission = true
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.exportar_logs))
        }

        // Dialogo de exportacion
        if (uiState.showExportLogsDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissExportDialog() },
                title = { Text(stringResource(R.string.exportar_logs)) },
                text = {
                    Text(stringResource(R.string.dialog_export_logs))
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.exportLogs(onlyToday = false) }) {
                        Text(stringResource(R.string.todos))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.exportLogs(onlyToday = true) }) {
                        Text(stringResource(R.string.solo_hoy))
                    }
                }
            )
        }

        // Solicitud de permiso de almacenamiento (API <= 28)
        if (pendingExportAfterPermission) {
            RequestStoragePermission { granted ->
                pendingExportAfterPermission = false
                if (granted) {
                    viewModel.showExportDialog()
                } else {
                    Toast.makeText(
                        context,
                        context.getString(R.string.permiso_export_logs),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ==================== USER CARD ====================

@Composable
private fun UserCard(
    user: UserUiModel,
    isActive: Boolean,
    combustible: String,
    onSelect: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        onClick = onSelect,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkmark si es activo
            if (isActive) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Usuario activo",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
            } else {
                Spacer(modifier = Modifier.width(32.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isActive) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$combustible · ${user.radioKm}km",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Icono editar solo para el activo
            if (isActive) {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar usuario",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ==================== COMPONENTES AUXILIARES ====================

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

private val TEMAS = listOf("SISTEMA", "CLARO", "OSCURO")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TemaDropdown(
    temaActual: String,
    onTemaChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Palette,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.weight(1f)
        ) {
            OutlinedTextField(
                value = temaActual,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.tema)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                TEMAS.forEach { tema ->
                    DropdownMenuItem(
                        text = { Text(tema) },
                        onClick = {
                            onTemaChange(tema)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}