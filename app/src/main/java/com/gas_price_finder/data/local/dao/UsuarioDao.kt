package com.gas_price_finder.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.gas_price_finder.data.local.entity.UsuarioEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UsuarioDao {
    @Query("SELECT * FROM usuarios WHERE activo = 1 LIMIT 1")
    fun getActive(): Flow<UsuarioEntity?>

    @Query("SELECT * FROM usuarios")
    fun getAll(): Flow<List<UsuarioEntity>>

    @Query("SELECT * FROM usuarios WHERE id = :id")
    suspend fun getById(id: Int): UsuarioEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(usuario: UsuarioEntity): Long

    @Update
    suspend fun update(usuario: UsuarioEntity)

    @Delete
    suspend fun delete(usuario: UsuarioEntity)

    @Query("UPDATE usuarios SET activo = 0")
    suspend fun deactivateAll()

    @Query("UPDATE usuarios SET activo = 1 WHERE id = :id")
    suspend fun activateId(id: Int)

    @Transaction
    suspend fun switchUser(id: Int) {
        deactivateAll()
        activateId(id)
    }

}