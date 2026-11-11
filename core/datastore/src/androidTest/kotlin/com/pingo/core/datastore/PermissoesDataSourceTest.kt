package com.pingo.core.datastore

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PermissoesDataSourceTest {
    private val dataSource = PermissoesDataSourceImpl(ApplicationProvider.getApplicationContext<Context>())

    @Test
    fun comecaSemEscolhas() = runBlocking {
        dataSource.clear()

        assertEquals(PermissaoDoPingo.NAO_PERGUNTADA, dataSource.acessoAsNotificacoes.first())
        assertEquals(PermissaoDoPingo.NAO_PERGUNTADA, dataSource.notificacoesDoPingo.first())
    }

    @Test
    fun registraNegacaoEConcessaoDeCadaPermissao() = runBlocking {
        dataSource.clear()

        dataSource.registrarAcessoAsNotificacoes(PermissaoDoPingo.NEGADA)
        dataSource.registrarNotificacoesDoPingo(PermissaoDoPingo.NEGADA)

        assertEquals(PermissaoDoPingo.NEGADA, dataSource.acessoAsNotificacoes.first())
        assertEquals(PermissaoDoPingo.NEGADA, dataSource.notificacoesDoPingo.first())

        dataSource.registrarAcessoAsNotificacoes(PermissaoDoPingo.PERMITIDA)
        dataSource.registrarNotificacoesDoPingo(PermissaoDoPingo.PERMITIDA)

        assertEquals(PermissaoDoPingo.PERMITIDA, dataSource.acessoAsNotificacoes.first())
        assertEquals(PermissaoDoPingo.PERMITIDA, dataSource.notificacoesDoPingo.first())
    }

    @Test
    fun limpaAsEscolhasAoExcluirConta() = runBlocking {
        dataSource.registrarAcessoAsNotificacoes(PermissaoDoPingo.NEGADA)
        dataSource.registrarNotificacoesDoPingo(PermissaoDoPingo.PERMITIDA)

        dataSource.clear()

        assertEquals(PermissaoDoPingo.NAO_PERGUNTADA, dataSource.acessoAsNotificacoes.first())
        assertEquals(PermissaoDoPingo.NAO_PERGUNTADA, dataSource.notificacoesDoPingo.first())
    }
}
