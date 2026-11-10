package com.pingo.core.notifications

import com.pingo.core.data.RegraParserRepository
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.RegraParser
import com.pingo.core.parser.RegrasParserSeed
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow

internal class FakeRegraParserRepository(regras: List<RegraParser> = emptyList()) : RegraParserRepository {
    private val cache = MutableStateFlow(regras)
    var coletas: Int = 0
        private set
    var substituicoes: Int = 0
        private set

    override fun observeRegrasAtivas(): Flow<List<RegraParser>> = flow {
        coletas += 1
        emit(cache.value.ifEmpty { RegrasParserSeed.regras })
    }

    override fun observeVersaoVigente(): Flow<Int?> = flow {
        emit(cache.value.filter { it.ativa }.maxOfOrNull { it.versao } ?: RegrasParserSeed.regras.maxOf { it.versao })
    }

    override suspend fun versoesEmCache(): Map<InstituicaoFinanceira, Int> =
        cache.value.filter { it.ativa }.associate { regra -> regra.instituicao to regra.versao }

    override suspend fun buscar(publicId: String): RegraParser? = cache.value.firstOrNull { it.publicId == publicId }

    override suspend fun salvar(regra: RegraParser) {
        cache.value = cache.value.filter { it.publicId != regra.publicId } + regra
    }

    override suspend fun salvarTodas(regras: List<RegraParser>) {
        cache.value = cache.value.filter { atual -> regras.none { it.publicId == atual.publicId } } + regras
    }

    override suspend fun substituirVigentes(regras: List<RegraParser>) {
        substituicoes += 1
        val instituicoes = regras.map { it.instituicao }.toSet()
        val publicIds = regras.map { it.publicId }.toSet()
        cache.value = cache.value
            .filter { it.publicId !in publicIds }
            .map { regra -> if (regra.ativa && regra.instituicao in instituicoes) regra.copy(ativa = false) else regra } +
            regras
    }
}
