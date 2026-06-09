package com.gas_price_finder.domain.usecase.user

import com.gas_price_finder.domain.model.User
import com.gas_price_finder.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetActiveUserUseCase @Inject constructor(
    private val repository: UserRepository
) {

    operator fun invoke(): Flow<User?> {
        return repository.getActiveUser()
    }

}