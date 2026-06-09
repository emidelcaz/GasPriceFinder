package com.gas_price_finder.presentation.screens.detail

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gas_price_finder.domain.model.Price
import com.gas_price_finder.domain.model.Station
import com.gas_price_finder.domain.model.User
import com.gas_price_finder.presentation.screens.map.PriceTerciles
import com.gas_price_finder.presentation.screens.map.resolvePriceColor
import com.gas_price_finder.util.BrandIconMapper
import com.gas_price_finder.util.HorarioParser
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import androidx.core.net.toUri
import com.gas_price_finder.R
import com.gas_price_finder.util.ProductNameResolver

@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    stationId: String,
    onNavigateBack: () -> Unit,
    onNavigateToPriceHistory: (String) -> Unit,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(stationId) {
        viewModel.loadStation(stationId)
    }

    DetailScreenContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onNavigateToPriceHistory = onNavigateToPriceHistory,
        onToggleFavorite = { viewModel.toggleFavorite() },
        onTogglePricesExpanded = { viewModel.togglePricesExpanded() },
        onToggleScheduleExpanded = { viewModel.toggleScheduleExpanded() },
        onRequestSync = { viewModel.requestSync() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailScreenContent(
    uiState: DetailState,
    onNavigateBack: () -> Unit,
    onNavigateToPriceHistory: (String) -> Unit,
    onToggleFavorite: () -> Unit,
    onTogglePricesExpanded: () -> Unit,
    onToggleScheduleExpanded: () -> Unit,
    onRequestSync: () -> Unit
) {
    val context = LocalContext.current
    val station = uiState.station

    Box(modifier = Modifier.fillMaxSize()) {
        // Header flotante fijo
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp)
                .align(Alignment.TopStart),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onNavigateBack) {
                Text(stringResource(R.string.cerrar), color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
            }
            /*IconButton(onClick = {
                station?.let {
                    val shareText = "${it.rotulo} - ${it.direccion ?: ""}. Precio: ${uiState.depositPrice}"
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    context.startActivity(Intent.createChooser(intent, "Compartir"))
                }
            }) {
                Icon(Icons.Outlined.Share, contentDescription = "Compartir")
            }*/
        }
        if (station != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 56.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Map + Logo overlay
                Box(modifier = Modifier.fillMaxWidth()) {
                    GoogleMap(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        cameraPositionState = rememberCameraPositionState {
                            position = CameraPosition.fromLatLngZoom(
                                LatLng(station.latitud, station.longitud), 18f
                            )
                        },
                        properties = MapProperties(mapType = MapType.SATELLITE),
                        uiSettings = MapUiSettings(
                            zoomControlsEnabled = false,
                            scrollGesturesEnabled = false,
                            zoomGesturesEnabled = false,
                            tiltGesturesEnabled = false,
                            rotationGesturesEnabled = false
                        )
                    ) {
                        val markerState = remember(station.latitud, station.longitud) {
                            MarkerState(position = LatLng(station.latitud, station.longitud))
                        }
                        Marker(
                            state = markerState,
                            onClick = { false }
                        )
                    }

                    // Brand logo
                    val iconRes = BrandIconMapper.getIconResource(station.rotulo)
                    val preferredPrice = uiState.effectiveProductoId?.let { station.getPrecio(it) }
                    val borderColor = resolvePriceColor(
                        preferredPrice?.precio,
                        uiState.priceTerciles
                    )
                    Image(
                        painter = painterResource(id = iconRes),
                        contentDescription = station.rotulo,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(start = 16.dp, bottom = 16.dp, end = 16.dp)
                            .size(56.dp)
                            .background(Color.White, CircleShape)
                            .border(2.dp, borderColor, CircleShape)
                            .padding(4.dp)
                    )
                }

                // Main info
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            station.rotulo,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        val preferredPrice =
                            uiState.effectiveProductoId?.let { station.getPrecio(it) }
                        Text(
                            preferredPrice?.let {
                                "%.3f".format(it.precio).replace('.', ',') + " €/l"
                            } ?: "—",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "%.1f".format(uiState.distanceKm).replace('.', ',') + " km",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        val isOpen = station.estaAbierto()
                        when (isOpen) {
                            true -> Text(
                                stringResource(R.string.abierto),
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,

                                )

                            false -> Text(
                                stringResource(R.string.cerrado),
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )

                            null -> {}
                        }
                    }
                }

                // Action buttons row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            val uri =
                                "geo:${station.latitud},${station.longitud}?q=${station.latitud},${station.longitud}(${station.rotulo})".toUri()
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.DirectionsCar, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.como_llegar))
                    }

                    IconButton(
                        onClick = { onToggleFavorite() },
                        modifier = Modifier
                            .size(30.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                CircleShape
                            )
                    ) {
                        Icon(
                            if (uiState.isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                            contentDescription = stringResource(R.string.favorito),
                            tint = if (uiState.isFavorite) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedButton(
                        onClick = { onNavigateToPriceHistory(station.ideess) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ShowChart, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.historico))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Stats row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatColumn(
                        label = stringResource(R.string.llenar_deposito),
                        value = uiState.depositPrice,
                        modifier = Modifier.weight(1f)
                    )
                    StatColumn(
                        label = stringResource(R.string.respecto_a_media),
                        value = uiState.diffVsMedia,
                        color = when {
                            uiState.diffVsMedia.startsWith("-") -> Color(0xFF4CAF50)
                            uiState.diffVsMedia.startsWith("+") -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.weight(1f)
                    )
                    StatColumn(
                        label = stringResource(R.string.llegar_cuesta),
                        value = uiState.costToArrive,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(modifier = Modifier.height(16.dp))

                // Price section
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Precio",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = { onTogglePricesExpanded() }) {
                            Text(stringResource(R.string.ver_todos))
                            Icon(
                                if (uiState.isPricesExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null
                            )
                        }
                    }

                    val user = uiState.user
                    val preferredId = user?.combustiblePreferidoId

                    if (!uiState.isPricesExpanded) {
                        val priceToShow = preferredId?.let { station.getPrecio(it) }
                            ?: station.precios.firstOrNull()
                        priceToShow?.let { price ->
                            PriceRow(
                                name = ProductNameResolver.resolve(price.productoId, price.nombreProducto),
                                price = "%.3f".format(price.precio).replace('.', ',') + " €/l"
                            )
                        }
                    } else {
                        station.precios.forEach { price ->
                            PriceRow(
                                name = ProductNameResolver.resolve(price.productoId, price.nombreProducto),
                                price = "%.3f".format(price.precio).replace('.', ',') + " €/l"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            stringResource(R.string.ultima_comprobacion_de_precios),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            uiState.lastUpdateText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.TrendingUp,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            stringResource(R.string.esta_gasolinera_cambio_los_precios),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            uiState.lastPriceChangeText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(modifier = Modifier.height(16.dp))

                // Schedule and location
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        stringResource(R.string.horario_y_ubicacion),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        val isOpen = station.estaAbierto()
                        when (isOpen) {
                            true -> Text(
                                stringResource(R.string.abierto),
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Medium
                            )

                            false -> Text(
                                stringResource(R.string.cerrado),
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Medium
                            )

                            null -> Text(stringResource(R.string.horario_no_disponible))
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        if (!station.horario.isNullOrBlank()) {
                            IconButton(onClick = { onToggleScheduleExpanded() }) {
                                Icon(
                                    if (uiState.isScheduleExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null
                                )
                            }
                        }
                    }

                    if (uiState.isScheduleExpanded) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val schedule = HorarioParser.parseSchedule(context, station.horario)
                        schedule.forEach { day ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    day.dayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (day.ranges.isNotEmpty() && day.ranges.first() != "24h") FontWeight.Normal else FontWeight.Normal
                                )
                                Text(
                                    if (day.ranges.isEmpty()) stringResource(R.string.cerrado)
                                    else day.ranges.joinToString(", "),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        Text(
                            stringResource(R.string.los_horarios_pueden_variar_en_dias_festivos),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                station.direccion ?: "",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                "${station.municipio ?: ""} ${station.cp ?: ""}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        IconButton(onClick = {
                            val uri = "geo:${station.latitud},${station.longitud}?q=${station.latitud},${station.longitud}(${station.rotulo})".toUri()
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                        }) {
                            Icon(Icons.Default.ChevronRight, contentDescription = stringResource(R.string.abrir_en_mapas))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Disclaimer
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        stringResource(R.string.disclaimer),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Station address footer
/*                val addressFooter = buildString {
                    if (!station.direccion.isNullOrBlank()) append(station.direccion)
                    if (!station.cp.isNullOrBlank()) {
                        if (isNotEmpty()) append(", ")
                        append(station.cp)
                    }
                    if (!station.municipio.isNullOrBlank()) {
                        if (isNotEmpty()) append(" ")
                        append(station.municipio)
                    }
                }
                  if (addressFooter.isNotBlank()) {
                      Text(
                          addressFooter,
                          style = MaterialTheme.typography.bodySmall,
                          color = MaterialTheme.colorScheme.onSurfaceVariant,
                          modifier = Modifier.align(Alignment.CenterHorizontally)
                      )
                  }*/

                Spacer(modifier = Modifier.height(24.dp))
            }
        } else {
            // Fallback
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(top = 56.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CloudOff, null, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.sin_datos_recientes_de_la_estacion))
                    Text(uiState.fallbackName ?: "", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { onRequestSync() }) {
                        Text(stringResource(R.string.sincronizar))
                    }
                }
            }
        }
    }
}

@Composable
private fun StatColumn(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 8.dp, vertical = 12.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun PriceRow(name: String, price: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(name, style = MaterialTheme.typography.bodyLarge)
        Text(
            price,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

// ============================ PREVIEW ============================

private val previewPrecioGasolina = Price(
    productoId = 1,
    nombreProducto = "Gasolina 95 E5",
    precio = 1.542,
    fechaActualizacion = System.currentTimeMillis() - 172800000
)

private val previewPrecioGasoleo = Price(
    productoId = 4,
    nombreProducto = "Gasoleo A",
    precio = 1.489,
    fechaActualizacion = System.currentTimeMillis()
)

private val previewStation = Station(
    ideess = "1001",
    rotulo = "REPSOL",
    direccion = "Calle Mayor, 15",
    cp = "28013",
    latitud = 40.4168,
    longitud = -3.7038,
    municipio = "Madrid",
    provincia = "Madrid",
    horario = "L-D: 06:00-23:00",
    tipoEstacion = "P",
    precios = listOf(previewPrecioGasolina, previewPrecioGasoleo),
    fechaActualizacion = System.currentTimeMillis()
)

private val previewUser = User(
    id = 1,
    nombre = "Usuario",
    combustiblePreferidoId = 1,
    capacidadDepositoL = 50.0,
    consumoMedioL100 = 7.0
)

private val previewDetailState = DetailState(
    isLoading = false,
    station = previewStation,
    isFavorite = true,
    distanceKm = 2.4,
    depositPrice = "77,10 €",
    costToArrive = "0,26 €",
    diffVsMedia = "-2,15 €",
    isPricesExpanded = true,
    isScheduleExpanded = true,
    lastUpdateText = "Hace 2 d",
    lastPriceChangeText = "Hace 2 d",
    user = previewUser,
    priceTerciles = PriceTerciles(lowThreshold = 1.450, highThreshold = 1.600),
    effectiveProductoId = 1
)

@Preview(showBackground = true, heightDp = 1200)
@Composable
private fun PreviewDetailScreen() {
    MaterialTheme {
        Surface {
            DetailScreenContent(
                uiState = previewDetailState,
                onNavigateBack = {},
                onNavigateToPriceHistory = {},
                onToggleFavorite = {},
                onTogglePricesExpanded = {},
                onToggleScheduleExpanded = {},
                onRequestSync = {}
            )
        }
    }
}