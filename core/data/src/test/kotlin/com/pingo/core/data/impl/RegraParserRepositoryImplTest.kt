package com.pingo.core.data.impl

import com.pingo.core.data.mapper.toEntity
import com.pingo.core.database.dao.RegraParserDao
import com.pingo.core.database.entity.RegraParserEntity
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.RegraParser
import com.pingo.core.parser.RegrasParserSeed
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class RegraParserRepositoryImplTest {
    @Test
    fun `cache vazio devolve as regras v1 embutidas`() = runBlocking {
        val repositorio = repositorioComCache(emptyList())

        assertEquals(RegrasParserSeed.regras, repositorio.observeRegrasAtivas().first())
    }

    @Test
    fun `cache com regras devolve somente as do cache`() = runBlocking {
        val doServidor = regra(publicId = "servidor-nubank-v2", instituicao = InstituicaoFinanceira.NUBANK, versao = 2)

        assertEquals(listOf(doServidor), repositorioComCache(listOf(doServidor)).observeRegrasAtivas().first())
    }

    @Test
    fun `seed vigora ate o cache receber regras e depois o cache substitui o seed`() = runBlocking {
        val cache = MutableStateFlow<List<RegraParserEntity>>(emptyList())
        val repositorio = RegraParserRepositoryImpl(FakeRegraParserDao(cache))
        val doServidor = regra(publicId = "servidor-caju-v2", instituicao = InstituicaoFinanceira.CAJU, versao = 2)

        val antesDoDownload = repositorio.observeRegrasAtivas().first()
        cache.value = listOf(doServidor.toEntity())

        assertEquals(RegrasParserSeed.regras, antesDoDownload)
        assertEquals(listOf(doServidor), repositorio.observeRegrasAtivas().first())
    }

    @Test
    fun `versao vigente do seed e a versao 1 quando o cache esta vazio`() = runBlocking {
        assertEquals(1, repositorioComCache(emptyList()).observeVersaoVigente().first())
    }

    @Test
    fun `versao em cache e vazia antes do primeiro download`() = runBlocking {
        assertTrue(repositorioComCache(emptyList()).versoesEmCache().isEmpty())
    }

    @Test
    fun `substituir vigentes grava as cinco instituicoes e desativa a versao anterior`() = runBlocking {
        val anterior = regra(publicId = "seed-nubank-v1", instituicao = InstituicaoFinanceira.NUBANK, versao = 1)
        val repositorio = repositorioComCache(listOf(anterior))
        val novas = InstituicaoFinanceira.entries
            .filter { instituicao -> instituicao != InstituicaoFinanceira.OUTRA }
            .map { instituicao -> regra(publicId = "servidor-${instituicao.name.lowercase()}-v2", instituicao = instituicao, versao = 2) }

        repositorio.substituirVigentes(novas)

        assertEquals(novas.map { it.publicId }.toSet(), repositorio.observeRegrasAtivas().first().map { it.publicId }.toSet())
        assertEquals(2, repositorio.versoesEmCache().values.maxOrNull())
        assertEquals(5, repositorio.versoesEmCache().size)
        assertEquals(2, repositorio.observeVersaoVigente().first())
        assertFalse(repositorio.buscar(anterior.publicId)!!.ativa)
    }

    private fun repositorioComCache(regras: List<RegraParser>): RegraParserRepositoryImpl {
        val cache = MutableStateFlow(regras.map { it.toEntity() })
        return RegraParserRepositoryImpl(FakeRegraParserDao(cache))
    }

    private fun regra(publicId: String, instituicao: InstituicaoFinanceira, versao: Int) = RegraParser(
        publicId = publicId,
        instituicao = instituicao,
        versao = versao,
        padroes = "[]",
        ativa = true,
        publicadaEm = Instant.EPOCH,
        criadoEm = Instant.EPOCH,
        atualizadoEm = Instant.EPOCH,
    )
}

private class FakeRegraParserDao(private val cache: MutableStateFlow<List<RegraParserEntity>>) : RegraParserDao() {
    override fun observeAtivas(): Flow<List<RegraParserEntity>> = cache.map { regras -> regras.filter { it.ativa } }

    override suspend fun buscar(publicId: String): RegraParserEntity? = cache.value.firstOrNull { it.publicId == publicId }

    override suspend fun buscarAtiva(instituicao: InstituicaoFinanceira): RegraParserEntity? =
        cache.value.filter { it.instituicao == instituicao && it.ativa }.maxByOrNull { it.versao }

    override suspend fun atualizadasDesde(desde: Instant): List<RegraParserEntity> = cache.value.filter { it.atualizadoEm >= desde }

    override suspend fun versaoAtivaMaxima(): Int? = cache.value.filter { it.ativa }.maxOfOrNull { it.versao }

    override suspend fun ativas(): List<RegraParserEntity> = cache.value.filter { it.ativa }

    override suspend fun desativarDas(instituicoes: List<InstituicaoFinanceira>) {
        cache.value = cache.value.map { regra ->
            if (regra.ativa && regra.instituicao in instituicoes) regra.copy(ativa = false) else regra
        }
    }

    override suspend fun upsert(regra: RegraParserEntity) {
        upsertAll(listOf(regra))
    }

    override suspend fun upsertAll(regras: List<RegraParserEntity>) {
        cache.value = cache.value.filter { atual -> regras.none { it.publicId == atual.publicId } } + regras
    }
}
