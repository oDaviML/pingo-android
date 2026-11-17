package com.pingo.feature.accounts.impl

import com.pingo.core.model.Conta
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.Instrumento
import com.pingo.core.model.TipoConta
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.testing.TestDispatcherRule
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class AccountsViewModelTest {
    @get:Rule
    val dispatcherRule = TestDispatcherRule(UnconfinedTestDispatcher())

    private val contas = FakeContaRepository()
    private val instrumentos = FakeInstrumentoRepository()

    @Test
    fun `sem contas mostra estado vazio`() {
        val viewModel = AccountsViewModel(contas, instrumentos)

        assertFalse(viewModel.uiState.value.carregando)
        assertFalse(viewModel.uiState.value.falha)
        assertTrue(viewModel.uiState.value.vazia)
    }

    @Test
    fun `contas ativas aparecem agrupadas por instituicao com seus instrumentos`() {
        contas.contas.value = listOf(
            conta("conta-nu", InstituicaoFinanceira.NUBANK, "Nubank"),
            conta("conta-itau", InstituicaoFinanceira.ITAU, "Itaú"),
            conta("conta-arquivada", InstituicaoFinanceira.NUBANK, "Antiga", arquivada = true),
        )
        instrumentos.instrumentos.value = listOf(
            instrumento("nu-credito", "conta-nu", TipoInstrumento.CREDITO, "Cartão", arquivado = false),
            instrumento("nu-arquivado", "conta-nu", TipoInstrumento.DEBITO, "Débito antigo", arquivado = true),
            instrumento("itau-debito", "conta-itau", TipoInstrumento.DEBITO, "Débito", arquivado = false),
        )

        val viewModel = AccountsViewModel(contas, instrumentos)
        val grupos = viewModel.uiState.value.grupos

        assertEquals(listOf(InstituicaoFinanceira.NUBANK, InstituicaoFinanceira.ITAU), grupos.map { it.instituicao })
        assertEquals(listOf("Cartão"), grupos[0].contas.single().instrumentos.map { it.apelido })
        assertEquals(listOf("Débito"), grupos[1].contas.single().instrumentos.map { it.apelido })
        assertFalse(viewModel.uiState.value.vazia)
    }

    @Test
    fun `falha de leitura mostra erro e tentar de novo recarrega`() {
        contas.falhaAoObservar = IllegalStateException("banco indisponível")

        val viewModel = AccountsViewModel(contas, instrumentos)

        assertTrue(viewModel.uiState.value.falha)
        assertFalse(viewModel.uiState.value.carregando)
        assertFalse(viewModel.uiState.value.vazia)

        contas.falhaAoObservar = null
        contas.contas.value = listOf(conta("conta-nu", InstituicaoFinanceira.NUBANK, "Nubank"))
        viewModel.tentarDeNovo()

        assertFalse(viewModel.uiState.value.falha)
        assertEquals(1, viewModel.uiState.value.grupos.size)
    }

    private fun conta(publicId: String, instituicao: InstituicaoFinanceira, apelido: String, arquivada: Boolean = false): Conta = Conta(
        publicId = publicId,
        instituicao = instituicao,
        apelido = apelido,
        tipo = TipoConta.CORRENTE,
        arquivada = arquivada,
        criadoEm = INSTANTE,
        atualizadoEm = INSTANTE,
    )

    private fun instrumento(
        publicId: String,
        contaPublicId: String,
        tipo: TipoInstrumento,
        apelido: String,
        arquivado: Boolean,
    ): Instrumento = Instrumento(
        publicId = publicId,
        contaPublicId = contaPublicId,
        tipo = tipo,
        apelido = apelido,
        arquivado = arquivado,
        criadoEm = INSTANTE,
        atualizadoEm = INSTANTE,
    )

    private companion object {
        val INSTANTE: Instant = Instant.parse("2026-11-10T12:00:00Z")
    }
}
