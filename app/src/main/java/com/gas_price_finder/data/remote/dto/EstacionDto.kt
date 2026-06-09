package com.gas_price_finder.data.remote.dto

import com.google.gson.annotations.SerializedName

data class EstacionDto(

    // Identificación
    @SerializedName("IDEESS") val ideess: String,
    @SerializedName("IDMunicipio") val idMunicipio: String,
    @SerializedName("IDProvincia") val idProvincia: String,
    @SerializedName("IDCCAA") val idCcaa: String,

    // Ubicación
    @SerializedName("Rótulo") val rotulo: String, //Marca de la Estación
    @SerializedName("Dirección") val direccion: String,
    @SerializedName("C.P.") val cp: String,
    @SerializedName("Municipio") val municipio: String,
    @SerializedName("Provincia") val provincia: String,
    @SerializedName("Localidad") val localidad: String,
    @SerializedName("Latitud") val latitud: String, // "39,211417"
    @SerializedName("Longitud (WGS84)") val longitud: String,

    // Operativa
    @SerializedName("Horario") val horario: String,
    @SerializedName("Tipo Venta") val tipoVenta: String,
    @SerializedName("Remisión") val remision: String,
    @SerializedName("Margen") val margen: String,  // Posición de la estación (I = izquierda, D = Derecha, C = Centro)

    // Precios (String con coma decimal, vacío si no aplica)
    @SerializedName("Precio Gasoleo A") val precioGasoleoA: String?,
    @SerializedName("Precio Gasoleo B") val precioGasoleoB: String?,
    @SerializedName("Precio Gasoleo Premium") val precioGasoleoPremium: String?,
    @SerializedName("Precio Gasolina 95 E5") val precioGasolina95E5: String?,
    @SerializedName("Precio Gasolina 95 E10") val precioGasolina95E10: String?,
    @SerializedName("Precio Gasolina 98 E5") val precioGasolina98E5: String?,
    @SerializedName("Precio Gasolina 98 E10") val precioGasolina98E10: String?,
    @SerializedName("Precio Gasolina 95 E5 Premium") val precioGasolina95E5Premium: String?,
    @SerializedName("Precio Gasolina 98 E5 Premium") val precioGasolina98E5Premium: String?,
    @SerializedName("Precio Biodiesel") val precioBiodiesel: String?,
    @SerializedName("Precio Bioetanol") val precioBioetanol: String?,
    @SerializedName("Precio Gases licuados del petróleo") val precioGlp: String?,
    @SerializedName("Precio Gas Natural Comprimido") val precioGnc: String?,
    @SerializedName("Precio Gas Natural Licuado") val precioGnl: String?,
    @SerializedName("Precio Hidrogeno") val precioHidrogeno: String?,
    @SerializedName("Precio Adblue") val precioAdBlue: String?,

    // Porcentajes (no usados en MVP)
    @SerializedName("% BioEtanol") val bioEtanol: String?,
    @SerializedName("% Éster metílico") val esterMetilico: String?

)
