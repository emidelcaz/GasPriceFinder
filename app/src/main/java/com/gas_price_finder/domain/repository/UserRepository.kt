package com.gas_price_finder.domain.repository

import com.gas_price_finder.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getActiveUser(): Flow<User?>
    suspend fun createUser(user: User): Result<Int>
    suspend fun updateUser(user: User): Result<Unit>
    suspend fun switchUser(userId: Int): Result<Unit>
    suspend fun deleteUser(userId: Int): Result<Unit>
    fun getAllUsers(): Flow<List<User>>

    suspend fun getActiveUserSync(): User?

}