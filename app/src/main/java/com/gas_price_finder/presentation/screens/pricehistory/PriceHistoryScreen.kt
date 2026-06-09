package com.gas_price_finder.presentation.screens.pricehistory

import android.graphics.PathMeasure
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gas_price_finder.R
import com.gas_price_finder.domain.model.PriceHistory
import com.gas_price_finder.util.ProductNameResolver
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceHistoryScreen(
    stationId: String,
    onNavigateBack: () -> Unit,
    viewModel: PriceHistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    LaunchedEffect(stationId) {
        viewModel.load(stationId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.historico_de_precios)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // Product dropdown
            val products = uiState.station?.precios ?: emptyList()
            var expanded by remember { mutableStateOf(false) }
            val selectedProduct = products.find { it.productoId == uiState.selectedProductoId }

            if (products.isNotEmpty()) {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedProduct?.let {
                            "${
                                ProductNameResolver.resolve(
                                    it.productoId,
                                    it.nombreProducto
                                )
                            } ${"%.3f".format(it.precio).replace('.', ',')} €/L"
                        } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        products.forEach { product ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(ProductNameResolver.resolve(product.productoId, product.nombreProducto))
                                        Text("${"%.3f".format(product.precio).replace('.', ',')} €/L")
                                    }
                                },
                                onClick = {
                                    viewModel.selectProduct(product.productoId)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Time filters
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TimeFilter.entries.filter { it != TimeFilter.ALL }.forEach { filter ->
                    FilterChip(
                        selected = uiState.selectedTimeFilter == filter,
                        onClick = { viewModel.selectTimeFilter(filter) },
                        label = { Text(filter.label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Chart
            if (uiState.filteredHistory.isNotEmpty()) {
                PriceHistoryChart(
                    history = uiState.filteredHistory,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.sin_datos_grafico))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // Min / Max
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        stringResource(R.string.precio_minimo),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        uiState.minPrice,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        stringResource(R.string.precio_maximo),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        uiState.maxPrice,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun PriceHistoryChart(
    history: List<PriceHistory>,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant

    val textMeasurer = rememberTextMeasurer()
    val dateFormat = remember { SimpleDateFormat("dd MMM", Locale("es")) }

    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(history) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(1f, tween(1000))
    }

    var selectedIndex by remember(history) { mutableStateOf(if (history.size == 1) 0 else null) }

    Canvas(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .pointerInput(history) {
                detectTapGestures { offset ->
                    val size = this.size
                    val paddingLeft = 56.dp.toPx()
                    val paddingRight = 16.dp.toPx()
                    val paddingTop = 16.dp.toPx()
                    val paddingBottom = 40.dp.toPx()
                    val chartWidth = size.width - paddingLeft - paddingRight
                    val chartHeight = size.height - paddingTop - paddingBottom

                    val prices = history.map { it.precio }
                    val minPrice = (prices.minOrNull() ?: 0.0) * 0.98
                    val maxPrice = (prices.maxOrNull() ?: 1.0) * 1.02
                    val range = maxPrice - minPrice

                    if (range <= 0 || history.size <= 1) {
                        if (history.size == 1) selectedIndex = 0
                        return@detectTapGestures
                    }

                    val stepX = chartWidth / (history.size - 1)
                    val touchX = offset.x - paddingLeft
                    val index = (touchX / stepX).toInt().coerceIn(0, history.lastIndex)
                    selectedIndex = index
                }
            }
    ) {
        val paddingLeft = 56.dp.toPx()
        val paddingRight = 16.dp.toPx()
        val paddingTop = 16.dp.toPx()
        val paddingBottom = 40.dp.toPx()
        val chartWidth = size.width - paddingLeft - paddingRight
        val chartHeight = size.height - paddingTop - paddingBottom

        val prices = history.map { it.precio }
        val minPrice = (prices.minOrNull() ?: 0.0) * 0.98
        val maxPrice = (prices.maxOrNull() ?: 1.0) * 1.02
        val range = maxPrice - minPrice

        if (range <= 0) return@Canvas

        // --- Puntos de datos ---
        val points = if (history.size == 1) {
            listOf(
                Offset(
                    paddingLeft + chartWidth / 2f,
                    paddingTop + chartHeight * (1 - ((history[0].precio - minPrice) / range).toFloat())
                )
            )
        } else {
            history.mapIndexed { index, item ->
                val x = paddingLeft + (index / (history.size - 1f)) * chartWidth
                val y =
                    paddingTop + chartHeight * (1 - ((item.precio - minPrice) / range).toFloat())
                Offset(x, y)
            }
        }

        // --- Eje Y: labels y guías ---
        val ySteps = 5
        val yLabelStyle = TextStyle(
            color = onSurfaceVariant,
            fontSize = 10.sp
        )
        for (i in 0..ySteps) {
            val ratio = i / ySteps.toFloat()
            val yValue = minPrice + range * ratio
            val y = paddingTop + chartHeight * (1 - ratio)

            drawLine(
                color = outlineVariant.copy(alpha = 0.5f),
                start = Offset(paddingLeft, y),
                end = Offset(paddingLeft + chartWidth, y),
                strokeWidth = 1f
            )

            val label = "%.3f".format(yValue).replace('.', ',') + " €"
            val textLayout = textMeasurer.measure(label, yLabelStyle)
            drawText(
                textMeasurer,
                label,
                topLeft = Offset(
                    paddingLeft - textLayout.size.width - 8.dp.toPx(),
                    y - textLayout.size.height / 2
                ),
                style = yLabelStyle
            )
        }

        // --- Eje X: fechas ---
        val xSteps = when {
            history.size == 1 -> 0
            history.size <= 6 -> history.size - 1
            else -> 5
        }
        val xLabelStyle = TextStyle(
            color = onSurfaceVariant,
            fontSize = 10.sp
        )
        for (i in 0..xSteps) {
            val index = when {
                history.size == 1 -> 0
                xSteps == 0 -> 0
                else -> (i * (history.size - 1) / xSteps).coerceIn(history.indices)
            }
            val x = paddingLeft + (index / (history.size - 1f).coerceAtLeast(1f)) * chartWidth
            val label = dateFormat.format(Date(history[index].fechaCaptura))
            val textLayout = textMeasurer.measure(label, xLabelStyle)
            drawText(
                textMeasurer,
                label,
                topLeft = Offset(
                    x - textLayout.size.width / 2,
                    paddingTop + chartHeight + 8.dp.toPx()
                ),
                style = xLabelStyle
            )
        }

        // --- Área bajo curva con gradiente ---
        if (points.size >= 2) {
            val areaPath = Path().apply {
                moveTo(points.first().x, points.first().y)
                addCubicLine(points)
                lineTo(points.last().x, paddingTop + chartHeight)
                lineTo(points.first().x, paddingTop + chartHeight)
                close()
            }
            drawPath(
                path = areaPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.3f),
                        Color.Transparent
                    ),
                    startY = paddingTop,
                    endY = paddingTop + chartHeight
                )
            )
        }

        // --- Línea cúbica ---
        val linePath = Path().apply {
            moveTo(points.first().x, points.first().y)
            addCubicLine(points)
        }
        drawPath(
            path = linePath,
            color = primaryColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // --- Animación de entrada: recortar línea ---
        val animatedPath = Path()
        if (history.size > 1) {
            val pathMeasure = PathMeasure(linePath.asAndroidPath(), false)
            val length = pathMeasure.length
            pathMeasure.getSegment(
                0f,
                length * animationProgress.value,
                animatedPath.asAndroidPath(),
                true
            )
        }
        drawPath(
            path = animatedPath,
            color = primaryColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // --- Puntos visibles ---
        points.forEach { point ->
            drawCircle(
                color = primaryColor,
                radius = 8.dp.toPx(),
                center = point
            )
            drawCircle(
                color = Color.White,
                radius = 6.dp.toPx(),
                center = point
            )
        }

        // --- Marker al tocar ---
        selectedIndex?.let { idx ->
            val point = points[idx.coerceIn(points.indices)]
            if (history.size > 1) {
                drawLine(
                    color = outlineVariant,
                    start = Offset(point.x, paddingTop),
                    end = Offset(point.x, paddingTop + chartHeight),
                    strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f))
                )
            }
            drawCircle(
                color = primaryColor,
                radius = 7.dp.toPx(),
                center = point
            )
            drawCircle(
                color = Color.White,
                radius = 3.5.dp.toPx(),
                center = point
            )

            val item = history[idx.coerceIn(history.indices)]
            val priceText = "%.3f".format(item.precio).replace('.', ',') + " €/L"
            val dateText = dateFormat.format(Date(item.fechaCaptura))
            val tooltipText = "$dateText\n$priceText"
            val tooltipStyle = TextStyle(
                color = Color.White,
                fontSize = 11.sp
            )
            val textLayout = textMeasurer.measure(tooltipText, tooltipStyle)
            val tooltipWidth = textLayout.size.width + 16.dp.toPx()
            val tooltipHeight = textLayout.size.height + 12.dp.toPx()
            val tooltipTop = (point.y - tooltipHeight - 12.dp.toPx()).coerceAtLeast(paddingTop)
            val tooltipLeft = (point.x - tooltipWidth / 2).coerceIn(
                paddingLeft,
                size.width - paddingRight - tooltipWidth
            )

            drawRoundRect(
                color = primaryColor,
                topLeft = Offset(tooltipLeft, tooltipTop),
                size = Size(tooltipWidth, tooltipHeight),
                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
            )
            drawText(
                textMeasurer,
                tooltipText,
                topLeft = Offset(tooltipLeft + 8.dp.toPx(), tooltipTop + 6.dp.toPx()),
                style = tooltipStyle
            )
        }
    }
}

/**
 * Extiende un Path con líneas cúbicas suavizadas entre los puntos.
 */
private fun Path.addCubicLine(points: List<Offset>) {
    if (points.size < 2) return
    for (i in 0 until points.size - 1) {
        val p0 = points.getOrElse(i - 1) { points[i] }
        val p1 = points[i]
        val p2 = points[i + 1]
        val p3 = points.getOrElse(i + 2) { p2 }

        val cp1x = p1.x + (p2.x - p0.x) / 6f
        val cp1y = p1.y + (p2.y - p0.y) / 6f
        val cp2x = p2.x - (p3.x - p1.x) / 6f
        val cp2y = p2.y - (p3.y - p1.y) / 6f

        cubicTo(cp1x, cp1y, cp2x, cp2y, p2.x, p2.y)
    }
}

private fun relativeTimeShort(timestamp: Long): String {
    val diffMs = System.currentTimeMillis() - timestamp
    val minutes = (diffMs / (1000 * 60)).toInt()
    val hours = minutes / 60
    val days = hours / 24
    return when {
        minutes < 1 -> "un momento"
        minutes < 60 -> "$minutes min"
        hours < 24 -> "$hours h"
        days == 1 -> "1 d"
        else -> "$days d"
    }
}
