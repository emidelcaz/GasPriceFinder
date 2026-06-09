package com.gas_price_finder.presentation.screens.map

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.gas_price_finder.domain.model.Station
import com.gas_price_finder.domain.model.ThemeMode
import com.gas_price_finder.util.BrandIconMapper
import com.gas_price_finder.util.RequestLocationPermission
import com.gas_price_finder.util.hasLocationPermission
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.*
import androidx.core.net.toUri
import com.gas_price_finder.R
import com.gas_price_finder.domain.model.Price
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapColorScheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onNavigateToDetail: (String) -> Unit,
    viewModel: MapViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val cameraPositionState = rememberCameraPositionState {
        position = uiState.savedCameraPosition ?: CameraPosition.fromLatLngZoom(
            LatLng(40.4168, -3.7038), 5f
        )
    }

    var isMapLoaded by remember { mutableStateOf(false) }
    var skipNextSave by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Bottom sheet states
    var showFilterSheet by remember { mutableStateOf(false) }
    var showListSheet by remember { mutableStateOf(false) }
    val detailSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val listSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val filterSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    val scope = rememberCoroutineScope()

    // Solicitar permiso al entrar si no lo tiene
    if (!uiState.locationPermissionGranted && !uiState.permissionRequested) {
        RequestLocationPermission { granted ->
            viewModel.onPermissionResult(granted)
        }
    }

    // Animación de zoom hacia la ubicación del usuario al iniciar la app
    // Solo se ejecuta cuando el mapa está cargado, hay ubicación y no hay posición guardada.
    LaunchedEffect(uiState.userLocation, isMapLoaded) {
        if (isMapLoaded && uiState.savedCameraPosition == null) {
            uiState.userLocation?.let { location ->
                skipNextSave = true
                delay(300)
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(
                        location,
                        13f
                    ),
                    durationMs = 1000
                )
            }
        }
    }

    // Guardar posición de la cámara cuando el usuario deja de mover el mapa.
    // NO guarda nada hasta que el mapa nativo haya terminado de cargar (isMapLoaded).
    LaunchedEffect(cameraPositionState.isMoving) {
        if (!isMapLoaded) return@LaunchedEffect
        if (!cameraPositionState.isMoving) {
            if (skipNextSave) {
                skipNextSave = false
            } else {
                viewModel.saveCameraPosition(cameraPositionState.position)
            }
        }
    }

    LaunchedEffect(detailSheetState.targetValue) {
        if (detailSheetState.targetValue == SheetValue.Expanded) {
            uiState.selectedStation?.let { station ->
                onNavigateToDetail(station.ideess)
                viewModel.deselectStation()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = uiState.locationPermissionGranted,
                mapType = uiState.mapType
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = false,
                compassEnabled = false
            ),
            onMapLoaded = { isMapLoaded = true }
        ) {
            MapEffect(uiState.activeUser?.tema) { map ->
                val colorScheme = when (uiState.activeUser?.tema) {
                    ThemeMode.OSCURO -> MapColorScheme.DARK
                    ThemeMode.CLARO -> MapColorScheme.LIGHT
                    else -> MapColorScheme.FOLLOW_SYSTEM
                }
                map.setMapColorScheme(colorScheme)
            }

            val stationsToShow =
                if (uiState.filters.activeCount > 0) uiState.filteredStations else uiState.nearbyStations
            stationsToShow.forEach { station ->
                MarkerComposable(
                    keys = arrayOf(
                        station.ideess,
                        uiState.combustiblePreferidoId,
                        uiState.priceTerciles?.hashCode() ?: 0
                    ),
                    state = MarkerState(
                        position = LatLng(
                            station.latitud,
                            station.longitud
                        )
                    ),
                    onClick = {
                        viewModel.selectStation(station)
                        true
                    }
                ) {
                    StationMarkerChip(
                        station = station,
                        combustiblePreferidoId = uiState.combustiblePreferidoId,
                        priceTerciles = uiState.priceTerciles
                    )
                }
            }
        }

        // Top overlay
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp)
                .statusBarsPadding()
        ) {
            // Search bar row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Menu button (3 lines)
                IconButton(
                    onClick = { showListSheet = true },
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surface, shape = CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.Menu,
                        contentDescription = "Listado",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Buscar en esta zona
                Button(
                    onClick = { viewModel.searchInZone(cameraPositionState) },
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.buscar_en_esta_zona))
                }

                // Filter button con badge
                BadgedBox(
                    badge = {
                        if (uiState.filters.activeCount > 0) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ) {
                                Text("${uiState.filters.activeCount}")
                            }
                        }
                    }
                ) {
                    IconButton(
                        onClick = { showFilterSheet = true },
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surface, shape = CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = "Filtros",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // FABs Parte derecha
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.End
        ) {
            // Layers / map type
            SmallFloatingActionButton(
                onClick = { viewModel.toggleMapType() },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Icon(
                    Icons.Default.Layers,
                    contentDescription = "Cambiar capa",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            // Compass Reset
            SmallFloatingActionButton(
                onClick = {
                    if (context.hasLocationPermission()) {
                        scope.launch {
                            cameraPositionState.position =
                                CameraPosition.fromLatLngZoom(
                                    cameraPositionState.position.target,
                                    cameraPositionState.position.zoom
                                )
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(
                                    cameraPositionState.position.target,
                                    cameraPositionState.position.zoom
                                ), durationMs = 600
                            )
                        }
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Icon(
                    Icons.Default.Explore,
                    contentDescription = "Orientar al norte",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            // Mi ubidacion
            FloatingActionButton(
                onClick = {
                    if (context.hasLocationPermission()) {
                        uiState.userLocation?.let {
                            scope.launch {
                                cameraPositionState.animate(
                                    CameraUpdateFactory.newLatLngZoom(it, 13f),
                                    durationMs = 800
                                )
                                viewModel.searchInZone(cameraPositionState)
                            }
                        }
                    } else {
                        viewModel.requestPermissionAgain()
                    }
                }
            ) {
                Icon(Icons.Default.MyLocation, "Mi ubicación")
            }
        }
    }

    // Filter Bottom Sheet
    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = filterSheetState
        ) {
            FilterSheetContent(
                filters = uiState.filters,
                onToggle = { viewModel.toggleFilter(it) },
                onClear = {
                    viewModel.clearFilters()
                },
                onSave = {
                    showFilterSheet = false
                }
            )
        }
    }

    // List Bottom Sheet
    if (showListSheet) {
        ModalBottomSheet(
            onDismissRequest = { showListSheet = false },
            sheetState = listSheetState
        ) {
            StationListSheetContent(
                stations = uiState.filteredStations,
                sortByPrice = uiState.sortByPrice,
                onSortChange = { viewModel.setSortByPrice(it) },
                onStationClick = { station ->
                    showListSheet = false
                    onNavigateToDetail(station.ideess)
                },
                getDistance = { viewModel.getDistance(it.ideess) ?: 0.0 },
                getPrice = { station -> viewModel.getPrice(station) ?: "—" },
                getDepositPrice = { station -> viewModel.getDepositPrice(station) ?: "—" },
                getIcon = { BrandIconMapper.getIconResource(it.rotulo) }
            )
        }
    }

    // Detail Bottom Sheet
    if (uiState.selectedStation != null) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.deselectStation() },
            sheetState = detailSheetState
        ) {
            StationDetailSheetContent(
                station = uiState.selectedStation!!,
                isFavorite = viewModel.isFavorite(uiState.selectedStation!!.ideess),
                distance = viewModel.getDistance(uiState.selectedStation!!.ideess) ?: 0.0,
                price = viewModel.getPrice(uiState.selectedStation!!) ?: "—",
                daysSinceUpdate = viewModel.daysSinceUpdate(uiState.selectedStation!!),
                onNavigate = {
                    onNavigateToDetail(uiState.selectedStation!!.ideess)
                    viewModel.deselectStation()
                },
                onDismiss = { viewModel.deselectStation() }
            )
        }
    }
}

@Composable
private fun FilterSheetContent(
    filters: FilterState,
    onToggle: (FilterType) -> Unit,
    onClear: () -> Unit,
    onSave: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Text(
            stringResource(R.string.filtros),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        FilterCheckboxRow(
            label = stringResource(R.string.abiertas_ahora),
            checked = filters.openNow,
            onCheckedChange = { onToggle(FilterType.OPEN_NOW) }
        )
        FilterCheckboxRow(
            label = stringResource(R.string.mi_tipo_de_combustible),
            checked = filters.myFuelType,
            onCheckedChange = { onToggle(FilterType.MY_FUEL_TYPE) }
        )
        FilterCheckboxRow(
            label = stringResource(R.string.solo_mis_favoritas),
            checked = filters.onlyFavorites,
            onCheckedChange = { onToggle(FilterType.ONLY_FAVORITES) }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onClear) {
                Text(stringResource(R.string.borrar_filtros))
            }
            /*Button(onClick = onSave) {
                Text(stringResource(R.string.guardar_filtros))
            }*/
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun FilterCheckboxRow(
    label: String,
    checked: Boolean,
    onCheckedChange: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = { onCheckedChange() }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun StationListSheetContent(
    stations: List<Station>,
    sortByPrice: Boolean,
    onSortChange: (Boolean) -> Unit,
    onStationClick: (Station) -> Unit,
    getDistance: (Station) -> Double,
    getPrice: (Station) -> String,
    getDepositPrice: (Station) -> String,
    getIcon: (Station) -> Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.94f)
            .padding(horizontal = 16.dp)
    ) {

        // Sort chips
        Row(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = sortByPrice,
                onClick = { onSortChange(true) },
                label = { Text(stringResource(R.string.por_precio)) }
            )
            FilterChip(
                selected = !sortByPrice,
                onClick = { onSortChange(false) },
                label = { Text(stringResource(R.string.por_distancia)) }
            )
        }

        HorizontalDivider()

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(stations) { station ->
                ListStationItem(
                    station = station,
                    distance = getDistance(station),
                    price = getPrice(station),
                    depositPrice = getDepositPrice(station),
                    iconRes = getIcon(station),
                    onClick = { onStationClick(station) }
                )
            }
        }
    }
}

@Composable
private fun ListStationItem(
    station: Station,
    distance: Double,
    price: String,
    depositPrice: String,
    iconRes: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = station.rotulo,
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                station.rotulo,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                "%.1f".format(distance).replace('.', ',') + " km · ${station.municipio ?: ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                price,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                depositPrice,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    HorizontalDivider()
}

@Composable
private fun StationDetailSheetContent(
    station: Station,
    isFavorite: Boolean,
    distance: Double,
    price: String,
    daysSinceUpdate: String,
    onNavigate: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val iconRes = BrandIconMapper.getIconResource(station.rotulo)
    val isOpen = station.estaAbierto()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Drag handle
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                .align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = station.rotulo,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    station.rotulo,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "%.1f".format(distance).replace('.', ',') + " km",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    when (isOpen) {
                        true -> Text(
                            "Abierto",
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )

                        false -> Text(
                            "Cerrado",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )

                        null -> {}
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    price,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    daysSinceUpdate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { /* TODO toggle favorite */ }) {
                Icon(
                    if (isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = "Favorito",
                    tint = if (isFavorite) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = {
                    val uri =
                        "geo:${station.latitud},${station.longitud}?q=${station.latitud},${station.longitud}(${station.rotulo})".toUri()
                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.DirectionsCar, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.como_llegar))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ============================ PREVIEWS ============================

@Preview(showBackground = true, heightDp = 320)
@Composable
private fun PreviewFilterSheetContent() {
    MaterialTheme {
        Surface {
            FilterSheetContent(
                filters = FilterState(openNow = true, onlyFavorites = true),
                onToggle = {},
                onClear = {},
                onSave = {}
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 280)
@Composable
private fun PreviewStationDetailSheetContent() {
    MaterialTheme {
        Surface {
            StationDetailSheetContent(
                station = previewStationRepsol,
                isFavorite = true,
                distance = 2.4,
                price = "1,542 €/l",
                daysSinceUpdate = "Cambio hoy",
                onNavigate = {},
                onDismiss = {}
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 600)
@Composable
private fun PreviewStationListSheetContent() {
    MaterialTheme {
        Surface {
            StationListSheetContent(
                stations = previewStations,
                sortByPrice = true,
                onSortChange = {},
                onStationClick = {},
                getDistance = { 1.5 },
                getPrice = { "1,542 €/l" },
                getDepositPrice = { "77,10 €/dep" },
                getIcon = { android.R.drawable.ic_dialog_info }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewListStationItem() {
    MaterialTheme {
        Surface {
            ListStationItem(
                station = previewStationRepsol,
                distance = 1.8,
                price = "1,542 €/l",
                depositPrice = "77,10 €/dep",
                iconRes = android.R.drawable.ic_dialog_info,
                onClick = {}
            )
        }
    }
}

// ============================ DATOS MOCK ============================

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

private val previewStationRepsol = Station(
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

private val previewStationCepsa = Station(
    ideess = "1002",
    rotulo = "CEPSA",
    direccion = "Paseo de la Castellana, 120",
    cp = "28046",
    latitud = 40.4400,
    longitud = -3.6900,
    municipio = "Madrid",
    provincia = "Madrid",
    horario = "L-D: 24H",
    tipoEstacion = "P",
    precios = listOf(
        previewPrecioGasolina.copy(precio = 1.589, fechaActualizacion = System.currentTimeMillis()),
        previewPrecioGasoleo.copy(precio = 1.515)
    ),
    fechaActualizacion = System.currentTimeMillis()
)

private val previewStationBp = Station(
    ideess = "1003",
    rotulo = "BP",
    direccion = "Avenida de America, 8",
    cp = "28028",
    latitud = 40.4380,
    longitud = -3.6750,
    municipio = "Madrid",
    provincia = "Madrid",
    horario = "L-V: 06:00-22:00; S-D: 07:00-21:00",
    tipoEstacion = "P",
    precios = listOf(
        previewPrecioGasolina.copy(
            precio = 1.489,
            fechaActualizacion = System.currentTimeMillis() - 86400000
        ),
        previewPrecioGasoleo.copy(precio = 1.425)
    ),
    fechaActualizacion = System.currentTimeMillis() - 86400000
)

private val previewStations = listOf(previewStationRepsol, previewStationCepsa, previewStationBp)