package com.gas_price_finder.domain.usecase.station

import com.gas_price_finder.data.local.dao.MunicipioDao
import javax.inject.Inject


class GetMunicipioNameByIdUseCase @Inject constructor(
    private val municipioDao: MunicipioDao
) {
    operator suspend fun invoke(id: Int): String {
        return municipioDao.getMunicipioNameById(id)
    }
}