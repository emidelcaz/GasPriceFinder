package com.gas_price_finder.data.remote.dto

import com.google.gson.annotations.SerializedName

data class EstacionesResponseDto(
    @SerializedName("Fecha") val fecha: String,
    @SerializedName("Nota") val nota: String,
    @SerializedName("ResultadoConsulta") val resultadoConsulta: String,
    @SerializedName("ListaEESSPrecio") val listaEESSPrecio: List<EstacionDto>
)
