package com.gas_price_finder.util

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.gas_price_finder.R

object ProductNameResolver {

    private val nameMap = mapOf(
        1 to R.string.fuel_gasolina_95_e5,
        3 to R.string.fuel_gasolina_98_e5,
        4 to R.string.fuel_gasoleo_a,
        5 to R.string.fuel_gasoleo_premium,
        6 to R.string.fuel_gasoleo_b,
        8 to R.string.fuel_biodiesel,
        16 to R.string.fuel_bioetanol,
        17 to R.string.fuel_glp,
        18 to R.string.fuel_gnc,
        19 to R.string.fuel_gnl,
        20 to R.string.fuel_gasolina_98_e5_premium,
        21 to R.string.fuel_gasolina_98_e10,
        22 to R.string.fuel_hidrogeno,
        23 to R.string.fuel_gasolina_95_e10,
        25 to R.string.fuel_gasolina_95_e5_premium,
        26 to R.string.fuel_adblue
    )

    @Composable
    fun resolve(productoId: Int, fallback: String = ""): String{
        val resId = nameMap[productoId]
        return if (resId != null) stringResource(resId) else fallback
    }

    fun resolve(context: Context, productoId: Int, fallback: String = ""): String{
        val resId = nameMap[productoId]
        return if (resId != null) context.getString(resId) else fallback
    }
}