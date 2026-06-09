package com.gas_price_finder.domain.usecase.user

import com.gas_price_finder.domain.model.User
import com.gas_price_finder.domain.repository.UserRepository
import javax.inject.Inject

class UpdateUserUseCase @Inject constructor(
    private val repository: UserRepository
) {
    suspend operator fun invoke(user: User): Result<Unit> {
        return repository.updateUser(user)
    }
}