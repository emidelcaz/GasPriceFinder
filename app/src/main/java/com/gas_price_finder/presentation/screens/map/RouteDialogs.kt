package com.gas_price_finder.presentation.screens.map

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.gas_price_finder.domain.model.RouteCalculation
import com.gas_price_finder.domain.model.Station
import com.gas_price_finder.util.BrandIconMapper

@Composable
fun RouteDistanceDialog(
    initialKm: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var sliderValue by remember { mutableFloatStateOf(initialKm.toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(sliderValue.toInt()) }) {
                Text("Calcular")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
        title = { Text("Modo en ruta") },
        text = {
            Column {
                Text("¿A qué distancia máxima quieres parar a repostar?")
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "${sliderValue.toInt()} km",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    valueRange = 1f..100f,
                    steps = 98
                )
            }
        }
    )
}

@Composable
fun RouteAdBlueDialog(
    onDismiss: () -> Unit,
    onConfirm: (Boolean) -> Unit
) {
    AlertDialog(
        onDismissRequest = { onConfirm(false) },
        confirmButton = {
            TextButton(onClick = { onConfirm(true) }) {
                Text("Sí")
            }
        },
        dismissButton = {
            TextButton(onClick = { onConfirm(false) }) {
                Text("No")
            }
        },
        title = { Text("AdBlue") },
        text = { Text("¿Necesitas repostar AdBlue también en esta parada?") }
    )
}

@Composable
fun RouteResultsDialog(
    routeCalculations: List<RouteCalculation>,
    onDismiss: () -> Unit,
    onStationClick: (String) -> Unit
) {
    val context = LocalContext.current
    val validCalculations = routeCalculations.filter { it.estacionOptima != null }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        },
        title = { Text("Estaciones recomendadas en ruta") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(validCalculations) { calculation ->
                    val station = calculation.estacionOptima!!
                    RouteResultItem(
                        station = station,
                        costeTotal = calculation.costeTotal,
                        onClick = { onStationClick(station.ideess) },
                        onNavigate = {
                            val uri =
                                "geo:${station.latitud},${station.longitud}?q=${station.latitud},${station.longitud}(${station.rotulo})".toUri()
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                        }
                    )
                }
            }
        }
    )
}

@Composable
private fun RouteResultItem(
    station: Station,
    costeTotal: Double,
    onClick: () -> Unit,
    onNavigate: () -> Unit
) {
    val totalPrice = "%.2f".format(costeTotal).replace('.', ',') + " €"

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = BrandIconMapper.getIconResource(station.rotulo)),
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = Color.Unspecified
            )
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = station.rotulo,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = station.municipio ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = totalPrice,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            IconButton(onClick = onNavigate) {
                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = "Ir",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
