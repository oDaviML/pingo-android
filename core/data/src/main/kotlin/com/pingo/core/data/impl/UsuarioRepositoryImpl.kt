package com.pingo.core.data.impl

import com.pingo.core.data.UsuarioRepository
import com.pingo.core.data.mapper.toEntity
import com.pingo.core.data.mapper.toModel
import com.pingo.core.database.dao.UsuarioDao
import com.pingo.core.model.Usuario
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class UsuarioRepositoryImpl @Inject constructor(private val usuarioDao: UsuarioDao) : UsuarioRepository {
    override fun observeUsuario(): Flow<Usuario?> = usuarioDao.observeUsuario().map { usuario -> usuario?.toModel() }

    override suspend fun buscarPorSub(sub: String): Usuario? = usuarioDao.buscarPorSub(sub)?.toModel()

    override suspend fun salvar(usuario: Usuario) {
        usuarioDao.upsert(usuario.toEntity())
    }

    override suspend fun remover(publicId: String) {
        usuarioDao.deletar(publicId)
    }
}
