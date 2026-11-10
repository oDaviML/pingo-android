package com.pingo.core.data.impl

import com.pingo.core.data.RegraParserRepository
import com.pingo.core.data.mapper.toEntity
import com.pingo.core.data.mapper.toModel
import com.pingo.core.database.dao.RegraParserDao
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.RegraParser
import com.pingo.core.parser.RegrasParserSeed
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RegraParserRepositoryImpl @Inject constructor(private val regraParserDao: RegraParserDao) : RegraParserRepository {
    override fun observeRegrasAtivas(): Flow<List<RegraParser>> =
        regraParserDao.observeAtivas().map { regras -> regras.map { it.toModel() }.ifEmpty { RegrasParserSeed.regras } }

    override fun observeVersaoVigente(): Flow<Int?> = observeRegrasAtivas().map { regras ->
        regras.filter(RegraParser::ativa).maxOfOrNull(RegraParser::versao)
    }

    override suspend fun versoesEmCache(): Map<InstituicaoFinanceira, Int> =
        regraParserDao.ativas().associate { regra -> regra.instituicao to regra.versao }

    override suspend fun buscar(publicId: String): RegraParser? = regraParserDao.buscar(publicId)?.toModel()

    override suspend fun salvar(regra: RegraParser) {
        regraParserDao.upsert(regra.toEntity())
    }

    override suspend fun salvarTodas(regras: List<RegraParser>) {
        regraParserDao.upsertAll(regras.map { it.toEntity() })
    }

    override suspend fun substituirVigentes(regras: List<RegraParser>) {
        regraParserDao.substituirVigentes(regras.map { it.toEntity() })
    }
}
