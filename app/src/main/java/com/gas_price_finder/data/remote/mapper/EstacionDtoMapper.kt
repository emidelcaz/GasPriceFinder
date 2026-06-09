package com.gas_price_finder.data.remote.mapper

import com.gas_price_finder.data.local.entity.*
import com.gas_price_finder.data.remote.dto.EstacionDto

object EstacionDtoMapper {

    private val CCAA_NAMES = mapOf(
        "01" to "Andalucía",
        "02" to "Aragón",
        "03" to "Asturias, Principado de",
        "04" to "Balears, Illes",
        "05" to "Canarias",
        "06" to "Cantabria",
        "07" to "Castilla y León",
        "08" to "Castilla-La Mancha",
        "09" to "Cataluña",
        "10" to "Comunitat Valenciana",
        "11" to "Extremadura",
        "12" to "Galicia",
        "13" to "Madrid, Comunidad de",
        "14" to "Murcia, Región de",
        "15" to "Navarra, Comunidad Foral de",
        "16" to "País Vasco",
        "17" to "Rioja, La",
        "18" to "Ceuta",
        "19" to "Melilla"
    )

    val PRODUCTOS = listOf(
        ProductoEntity(1, "Gasolina 95 E5", "Gasolina"),
        ProductoEntity(3, "Gasolina 98 E5", "Gasolina"),
        ProductoEntity(4, "Gasoleo A", "Gasóleo"),
        ProductoEntity(5, "Gasoleo Premium", "Gasóleo"),
        ProductoEntity(6, "Gasoleo B", "Gasóleo"),
        ProductoEntity(8, "Biodiesel", "Biodiesel"),
        ProductoEntity(16, "Bioetanol", "Bioetanol"),
        ProductoEntity(17, "Gases licuados del petróleo", "GLP"),
        ProductoEntity(18, "Gas Natural Comprimido", "GNC"),
        ProductoEntity(19, "Gas Natural Licuado", "GNL"),
        ProductoEntity(20, "Gasolina 98 E5 Premium", "Gasolina"),
        ProductoEntity(21, "Gasolina 98 E10", "Gasolina"),
        ProductoEntity(22, "Hidrogeno", "Hidrógeno"),
        ProductoEntity(23, "Gasolina 95 E10", "Gasolina"),
        ProductoEntity(25, "Gasolina 95 E5 Premium", "Gasolina"),
        ProductoEntity(26, "AdBlue", "AdBlue")
    )

    fun toEstacionEntity(dto: EstacionDto): EstacionEntity {
        return EstacionEntity(
            ideess = dto.ideess,
            rotulo = dto.rotulo,
            direccion = dto.direccion,
            cp = dto.cp,
            latitud = dto.latitud.replace(',', '.').toDouble(),
            longitud = dto.longitud.replace(',', '.').toDouble(),
            municipioId = dto.idMunicipio,
            localidadId = generateLocalidadId(dto),
            horario = dto.horario,
            tipoEstacion = dto.tipoVenta,
            tipoVenta = dto.tipoVenta,
            fechaActualizacion = System.currentTimeMillis()
        )
    }

    fun toMunicipioEntity(dto: EstacionDto): MunicipioEntity {
        return MunicipioEntity(
            id = dto.idMunicipio,
            nombre = dto.municipio,
            provinciaId = dto.idProvincia
        )
    }

    fun toProvinciaEntity(dto: EstacionDto): ProvinciaEntity {
        return ProvinciaEntity(
            id = dto.idProvincia,
            nombre = dto.provincia,
            ccaaId = dto.idCcaa
        )
    }

    fun toCcaaEntity(dto: EstacionDto): CcaaEntity {
        return CcaaEntity(
            id = dto.idCcaa,
            nombre = CCAA_NAMES[dto.idCcaa] ?: ""
        )
    }

    fun toLocalidadEntity(dto: EstacionDto): LocalidadEntity {
        return LocalidadEntity(
            id = generateLocalidadId(dto),
            nombre = dto.localidad,
            municipioId = dto.idMunicipio
        )
    }

    private fun generateLocalidadId(dto: EstacionDto): String {
        return "${dto.idMunicipio}_${dto.localidad.trim().uppercase()}"
    }

    fun toPrecioEntities(dto: EstacionDto): List<PrecioEntity> {
        val precios = mutableListOf<PrecioEntity>()
        val timestamp = System.currentTimeMillis()

        mapOf(
            1 to dto.precioGasolina95E5,
            3 to dto.precioGasolina98E5,
            4 to dto.precioGasoleoA,
            5 to dto.precioGasoleoPremium,
            6 to dto.precioGasoleoB,
            8 to dto.precioBiodiesel,
            16 to dto.precioBioetanol,
            17 to dto.precioGlp,
            18 to dto.precioGnc,
            19 to dto.precioGnl,
            20 to dto.precioGasolina98E5Premium,
            21 to dto.precioGasolina98E10,
            22 to dto.precioHidrogeno,
            23 to dto.precioGasolina95E10,
            25 to dto.precioGasolina95E5Premium,
            26 to dto.precioAdBlue
        ).forEach { (productoId, precioStr) ->
            precioStr?.takeIf { it.isNotBlank() }?.let {
                val precio = it.replace(',', '.').toDoubleOrNull()
                if (precio != null && precio > 0) {
                    precios.add(
                        PrecioEntity(
                            estacionId = dto.ideess,
                            productoId = productoId,
                            precio = precio,
                            fechaActualizacion = timestamp
                        )
                    )
                }
            }
        }

        return precios
    }

    fun extractAllGeographicAreas(dtos: List<EstacionDto>): Quad<List<CcaaEntity>, List<ProvinciaEntity>, List<MunicipioEntity>, List<LocalidadEntity>> {
        val ccaas = dtos.map { toCcaaEntity(it) }.distinctBy { it.id }
        val provincias = dtos.map { toProvinciaEntity(it) }.distinctBy { it.id }
        val municipios = dtos.map { toMunicipioEntity(it) }.distinctBy { it.id }
        val localidades = dtos.map { toLocalidadEntity(it) }.distinctBy { it.id }
        return Quad(ccaas, provincias, municipios, localidades)
    }

    data class Quad<out A, out B, out C, out D>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D
    )
}