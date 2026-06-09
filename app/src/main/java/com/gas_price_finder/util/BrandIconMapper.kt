package com.gas_price_finder.util

import com.gas_price_finder.R

object BrandIconMapper {

    fun getIconResource(rotulo: String?): Int {
        if (rotulo.isNullOrBlank()) return R.drawable.ic_gas_station_generic

        return when {
            rotulo.contains("REPSOL", ignoreCase = true) -> R.drawable.ic_repsol
            rotulo.contains("CEPSA", ignoreCase = true) -> R.drawable.ic_cepsa
            rotulo.contains("MOEVE", ignoreCase = true) -> R.drawable.ic_cepsa
            rotulo.contains("BP", ignoreCase = true) -> R.drawable.ic_bp
            rotulo.contains("SHELL", ignoreCase = true) -> R.drawable.ic_shell
            rotulo.contains("GALP", ignoreCase = true) -> R.drawable.ic_galp
            rotulo.contains("AVIA", ignoreCase = true) -> R.drawable.ic_avia
            rotulo.contains("BALLENOIL", ignoreCase = true) -> R.drawable.ic_ballenoil
            rotulo.contains("PLENOIL", ignoreCase = true) -> R.drawable.ic_plenoil
            rotulo.contains("ALCAMPO", ignoreCase = true) -> R.drawable.ic_alcampo
            rotulo.contains("EROSKI", ignoreCase = true) -> R.drawable.ic_eroski
            rotulo.contains("GASEXPRESS", ignoreCase = true) -> R.drawable.ic_gasexpress
            rotulo.contains("PETROPRIX", ignoreCase = true) -> R.drawable.ic_pretroprix
            rotulo.contains("PLENERGY", ignoreCase = true) -> R.drawable.ic_plenergy
            rotulo.contains("CAMPSA", ignoreCase = true) -> R.drawable.ic_campsa
            rotulo.contains("CARREFOUR", ignoreCase = true) -> R.drawable.ic_carrefour
            rotulo.contains("Q8", ignoreCase = true) -> R.drawable.ic_q8
            rotulo.contains("PETRONOR", ignoreCase = true) -> R.drawable.ic_petronor
            else -> R.drawable.ic_gas_station_generic
        }
    }
}