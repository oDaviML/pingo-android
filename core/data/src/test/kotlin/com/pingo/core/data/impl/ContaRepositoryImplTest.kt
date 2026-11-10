package com.pingo.core.data.impl

import com.pingo.core.data.mapper.toModel
import com.pingo.core.database.entity.ContaEntity
import com.pingo.core.database.entity.InstrumentoEntity
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.NovaConta
import com.pingo.core.model.NovoInstrumento
import com.pingo.core.model.TipoConta
import com.pingo.core.model.TipoInstrumento
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ContaRepositoryImplTest {
    private val contaDao = FakeContaDao()
    private val instrumentoDao = FakeInstrumentoDao()
    private val transacoes = FakeTransacaoLocal()
    private val repositorio = ContaRepositoryImpl(contaDao, instrumentoDao, transacoes)

    @Test
    fun `criar grava conta e instrumentos ligados a ela com o instante informado`() = runBlocking {
        val novaConta = NovaConta(
            instituicao = InstituicaoFinanceira.NUBANK,
            apelido = "  Nubank  ",
            tipo = TipoConta.CORRENTE,
            instrumentos = listOf(NovoInstrumento(TipoInstrumento.CREDITO, " Cartão ", "1234")),
        )

        repositorio.criarComInstrumentos(listOf(novaConta), INSTANTE_DA_CRIACAO)

        val conta = contaDao.gravadas().single().toModel()
        val instrumento = instrumentoDao.gravados().single().toModel()
        assertEquals(InstituicaoFinanceira.NUBANK, conta.instituicao)
        assertEquals("Nubank", conta.apelido)
        assertEquals(TipoConta.CORRENTE, conta.tipo)
        assertEquals(INSTANTE_DA_CRIACAO, conta.criadoEm)
        assertEquals(INSTANTE_DA_CRIACAO, conta.atualizadoEm)
        assertEquals(conta.publicId, instrumento.contaPublicId)
        assertEquals(TipoInstrumento.CREDITO, instrumento.tipo)
        assertEquals("Cartão", instrumento.apelido)
        assertEquals("1234", instrumento.ultimosDigitos)
        assertEquals(INSTANTE_DA_CRIACAO, instrumento.criadoEm)
        assertEquals(INSTANTE_DA_CRIACAO, instrumento.atualizadoEm)
    }

    @Test
    fun `cada conta e cada instrumento recebe identificador proprio`() = runBlocking {
        val instrumentos = listOf(NovoInstrumento(TipoInstrumento.DEBITO, "Débito"), NovoInstrumento(TipoInstrumento.VOUCHER, "VR"))

        repositorio.criarComInstrumentos(
            listOf(
                novaConta(instituicao = InstituicaoFinanceira.ITAU, instrumentos = instrumentos),
                novaConta(instituicao = InstituicaoFinanceira.CAJU, instrumentos = instrumentos),
            ),
            INSTANTE_DA_CRIACAO,
        )

        val identificadores = contaDao.gravadas().map(ContaEntity::publicId) + instrumentoDao.gravados().map(InstrumentoEntity::publicId)
        assertEquals(6, identificadores.size)
        assertEquals(identificadores.size, identificadores.toSet().size)
        assertTrue(identificadores.none(String::isBlank))
    }

    @Test
    fun `criar recusa conta sem instrumentos e nao grava nada`() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repositorio.criarComInstrumentos(listOf(novaConta(instrumentos = emptyList())), INSTANTE_DA_CRIACAO) }
        }

        assertTrue(contaDao.gravadas().isEmpty())
        assertTrue(instrumentoDao.gravados().isEmpty())
    }

    @Test
    fun `criar recusa o lote inteiro quando uma conta e invalida`() {
        val valida = novaConta(instituicao = InstituicaoFinanceira.NUBANK)
        val invalida = novaConta(instituicao = InstituicaoFinanceira.INTER, apelido = " ")

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repositorio.criarComInstrumentos(listOf(valida, invalida), INSTANTE_DA_CRIACAO) }
        }

        assertTrue(contaDao.gravadas().isEmpty())
        assertTrue(instrumentoDao.gravados().isEmpty())
        assertEquals(0, transacoes.execucoes)
    }

    @Test
    fun `criar grava o lote em uma unica transacao`() = runBlocking {
        repositorio.criarComInstrumentos(
            listOf(novaConta(instituicao = InstituicaoFinanceira.NUBANK), novaConta(instituicao = InstituicaoFinanceira.INTER)),
            INSTANTE_DA_CRIACAO,
        )

        assertEquals(1, transacoes.execucoes)
    }

    @Test
    fun `contas criadas aparecem no fluxo de contas ativas`() = runBlocking {
        repositorio.criarComInstrumentos(
            listOf(novaConta(instituicao = InstituicaoFinanceira.INTER, apelido = "Inter")),
            INSTANTE_DA_CRIACAO,
        )

        assertEquals(listOf("Inter"), repositorio.observeContas().first().map { it.apelido })
    }

    private fun novaConta(
        instituicao: InstituicaoFinanceira = InstituicaoFinanceira.NUBANK,
        apelido: String = "Nubank",
        instrumentos: List<NovoInstrumento> = listOf(NovoInstrumento(TipoInstrumento.CREDITO, "Cartão de crédito", "1234")),
    ) = NovaConta(instituicao = instituicao, apelido = apelido, tipo = TipoConta.CORRENTE, instrumentos = instrumentos)
}
