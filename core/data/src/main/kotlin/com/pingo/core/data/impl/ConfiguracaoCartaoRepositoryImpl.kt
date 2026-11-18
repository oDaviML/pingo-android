package com.pingo.core.data.impl

import com.pingo.core.data.ConfiguracaoCartaoRepository
import com.pingo.core.data.mapper.toVigencia
import com.pingo.core.database.dao.ConfiguracaoCartaoDao
import com.pingo.core.model.fatura.VigenciaDeCartao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConfiguracaoCartaoRepositoryImpl @Inject constructor(private val configuracaoCartaoDao: ConfiguracaoCartaoDao) :
    ConfiguracaoCartaoRepository {
    override fun observePorInstrumento(instrumentoPublicId: String): Flow<List<VigenciaDeCartao>> =
        configuracaoCartaoDao.observePorInstrumento(instrumentoPublicId).map { lista ->
            lista.map { it.toVigencia() }
        }

    override suspend fun listarPorInstrumento(instrumentoPublicId: String): List<VigenciaDeCartao> =
        configuracaoCartaoDao.listarPorInstrumento(instrumentoPublicId).map { it.toVigencia() }

    override suspend fun listarAtivas(): List<VigenciaDeCartao> = configuracaoCartaoDao.listarAtivas().map { it.toVigencia() }
}
