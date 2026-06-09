package com.gas_price_finder.domain.usecase.user

import com.gas_price_finder.domain.repository.UserRepository
import javax.inject.Inject

class SwitchUserUseCase @Inject constructor(
    private val repository: UserRepository
) {

    suspend operator fun invoke(userId: Int): Result<Unit> {
        return repository.switchUser(userId)
    }

}