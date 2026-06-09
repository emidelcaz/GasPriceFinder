package com.gas_price_finder.data.repository

import com.gas_price_finder.data.local.dao.UsuarioDao
import com.gas_price_finder.data.local.entity.UsuarioEntity
import com.gas_price_finder.domain.model.ThemeMode
import com.gas_price_finder.domain.model.User
import com.gas_price_finder.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val usuarioDao: UsuarioDao
) : UserRepository {

    override fun getActiveUser(): Flow<User?> {
        return usuarioDao.getActive().map { entity -> entity?.toDomain() }
    }

    override suspend fun getActiveUserSync(): User? {
        return usuarioDao.getActive().firstOrNull()?.toDomain()
    }

    override suspend fun createUser(user: User): Result<Int> {
        return try {
            val id = usuarioDao.insert(user.toEntity())
            Result.success(id.toInt())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateUser(user: User): Result<Unit> {
        return try {
            usuarioDao.update(user.toEntity())

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun switchUser(userId: Int): Result<Unit> {
        return try {
            usuarioDao.switchUser(userId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteUser(userId: Int): Result<Unit> {
        return try {
            val user = usuarioDao.getById(userId)
            user?.let { usuarioDao.delete(it) }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getAllUsers(): Flow<List<User>> {
        return usuarioDao.getAll().map { list ->
            list.map { it.toDomain() }
        }
    }

    private fun UsuarioEntity.toDomain(): User {
        return User(
            id = this.id,
            nombre = this.nombre,
            combustiblePreferidoId = this.combustiblePreferidoId,
            radioBusquedaKm = this.radioBusquedaKm,
            consumoMedioL100 = this.consumoMedioL100,
            capacidadDepositoL = this.capacidadDepositoL,
            usaAdBlue = this.usaAdBlue,
            capacidadAdBlueL = this.capacidadAdBlueL,
            umbralNotificacionBajada = this.umbralNotificacionBajada,
            notificacionesActivas = this.notificacionesActivas,
            syncWifi = this.syncWifi,
            tema = ThemeMode.valueOf(this.tema),
            activo = this.activo
        )
    }

    private fun User.toEntity(): UsuarioEntity {
        return UsuarioEntity(
            id = this.id,
            nombre = this.nombre,
            combustiblePreferidoId = this.combustiblePreferidoId,
            radioBusquedaKm = this.radioBusquedaKm,
            consumoMedioL100 = this.consumoMedioL100,
            capacidadDepositoL = this.capacidadDepositoL,
            usaAdBlue = this.usaAdBlue,
            capacidadAdBlueL = this.capacidadAdBlueL,
            umbralNotificacionBajada = this.umbralNotificacionBajada,
            notificacionesActivas = this.notificacionesActivas,
            syncWifi = this.syncWifi,
            tema = this.tema.name,
            activo = this.activo
        )
    }
}