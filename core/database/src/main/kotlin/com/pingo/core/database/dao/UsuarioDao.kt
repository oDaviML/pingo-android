package com.pingo.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.pingo.core.database.entity.UsuarioEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UsuarioDao {
    @Query("SELECT * FROM usuarios ORDER BY criadoEm ASC LIMIT 1")
    fun observeUsuario(): Flow<UsuarioEntity?>

    @Query("SELECT * FROM usuarios WHERE publicId = :publicId")
    suspend fun buscar(publicId: String): UsuarioEntity?

    @Query("SELECT * FROM usuarios WHERE sub = :sub LIMIT 1")
    suspend fun buscarPorSub(sub: String): UsuarioEntity?

    @Upsert
    suspend fun upsert(usuario: UsuarioEntity)

    @Query("DELETE FROM usuarios WHERE publicId = :publicId")
    suspend fun deletar(publicId: String)

    @Query("DELETE FROM usuarios")
    suspend fun limpar()
}
