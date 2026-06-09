package com.gas_price_finder.presentation.screens.map

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gas_price_finder.domain.model.Station
import com.gas_price_finder.presentation.theme.PriceHigh
import com.gas_price_finder.presentation.theme.PriceLow
import com.gas_price_finder.presentation.theme.PriceMedium
import com.gas_price_finder.util.BrandIconMapper

@Composable
fun StationMarkerChip(
    station: Station,
    combustiblePreferidoId: Int,
    priceTerciles: PriceTerciles?,
    modifier: Modifier = Modifier
) {
    val precio = station.getPrecio(combustiblePreferidoId)
    val borderColor = resolvePriceColor(precio?.precio, priceTerciles)
    val iconRes = BrandIconMapper.getIconResource(station.rotulo)

    Row(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 2.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = station.rotulo,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = precio?.let { "%.3f".format(it.precio).replace('.', ',') + " €" } ?: "—",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

fun resolvePriceColor(precio: Double?, terciles: PriceTerciles?): Color {
    if (precio == null || terciles == null) return Color.Gray
    return when {
        precio <= terciles.lowThreshold -> PriceLow
        precio >= terciles.highThreshold -> PriceHigh
        else -> PriceMedium
    }
}
