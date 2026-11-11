package com.pingo.core.notifications

import com.pingo.core.datastore.PermissaoDoPingo
import com.pingo.core.datastore.PermissoesDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RevisaoPorNotificacaoTest {
    @Test
    fun `negacao registrada desabilita a revisao por notificacao`() = runTest {
        val revisao = RevisaoPorNotificacaoConfigurada(
            permissoes = FakePermissoes(PermissaoDoPingo.NEGADA),
            notificacoesDoSistema = FakeNotificacoesDoSistema(habilitadas = true),
        )

        assertFalse(revisao.habilitada())
    }

    @Test
    fun `permissao concedida mantem a revisao habilitada`() = runTest {
        val revisao = RevisaoPorNotificacaoConfigurada(
            permissoes = FakePermissoes(PermissaoDoPingo.PERMITIDA),
            notificacoesDoSistema = FakeNotificacoesDoSistema(habilitadas = true),
        )

        assertTrue(revisao.habilitada())
    }

    @Test
    fun `sistema sem notificacoes desabilita a revisao`() = runTest {
        val revisao = RevisaoPorNotificacaoConfigurada(
            permissoes = FakePermissoes(PermissaoDoPingo.PERMITIDA),
            notificacoesDoSistema = FakeNotificacoesDoSistema(habilitadas = false),
        )

        assertFalse(revisao.habilitada())
    }

    @Test
    fun `permissao nao perguntada nao habilita a revisao`() = runTest {
        val revisao = RevisaoPorNotificacaoConfigurada(
            permissoes = FakePermissoes(PermissaoDoPingo.NAO_PERGUNTADA),
            notificacoesDoSistema = FakeNotificacoesDoSistema(habilitadas = true),
        )

        assertFalse(revisao.habilitada())
    }

    private class FakePermissoes(permissao: PermissaoDoPingo) : PermissoesDataSource {
        private val escolha = MutableStateFlow(permissao)

        override val acessoAsNotificacoes: Flow<PermissaoDoPingo> = escolha
        override val notificacoesDoPingo: Flow<PermissaoDoPingo> = escolha

        override suspend fun registrarAcessoAsNotificacoes(permissao: PermissaoDoPingo) {
            escolha.value = permissao
        }

        override suspend fun registrarNotificacoesDoPingo(permissao: PermissaoDoPingo) {
            escolha.value = permissao
        }

        override suspend fun clear() = Unit
    }

    private class FakeNotificacoesDoSistema(private val habilitadas: Boolean) : NotificacoesDoSistema {
        override fun habilitadas(): Boolean = habilitadas
    }
}
