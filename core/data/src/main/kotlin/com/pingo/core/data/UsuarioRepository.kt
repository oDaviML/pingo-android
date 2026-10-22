package com.pingo.core.data

import com.pingo.core.model.Usuario
import kotlinx.coroutines.flow.Flow

interface UsuarioRepository {
    fun observeUsuario(): Flow<Usuario?>

    suspend fun buscarPorSub(sub: String): Usuario?

    suspend fun salvar(usuario: Usuario)

    suspend fun remover(publicId: String)
}
