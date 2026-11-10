package com.pingo.core.parser

import com.pingo.core.model.InstituicaoFinanceira.INTER
import com.pingo.core.model.InstituicaoFinanceira.ITAU
import com.pingo.core.model.InstituicaoFinanceira.NUBANK
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RegrasVigentesTest {
    @Test
    fun `escolhe a versao ativa mais recente de cada instituicao`() {
        val vigentes = RegrasVigentes.selecionar(
            listOf(
                regraParser(ITAU, versao = 1, padroes = PADRAO_VALIDO),
                regraParser(ITAU, versao = 2, padroes = PADRAO_VALIDO),
                regraParser(ITAU, versao = 3, padroes = PADRAO_VALIDO, ativa = false),
                regraParser(INTER, versao = 1, padroes = PADRAO_VALIDO),
            ),
        )

        assertEquals(2, vigentes.getValue(ITAU).versao)
        assertEquals(1, vigentes.getValue(INTER).versao)
    }

    @Test
    fun `instituicao sem versao ativa fica fora da selecao`() {
        val vigentes = RegrasVigentes.selecionar(listOf(regraParser(NUBANK, versao = 1, padroes = PADRAO_VALIDO, ativa = false)))

        assertNull(vigentes[NUBANK])
    }

    @Test
    fun `versao mais recente com padrao invalido cai para a anterior valida`() {
        val vigentes = RegrasVigentes.selecionar(
            listOf(
                regraParser(ITAU, versao = 1, padroes = PADRAO_VALIDO),
                regraParser(ITAU, versao = 2, padroes = """[{"id":"quebrado","ancoras":[]}]"""),
            ),
        )

        assertEquals(1, vigentes.getValue(ITAU).versao)
    }

    @Test
    fun `json corrompido na versao mais recente nao derruba a selecao`() {
        val vigentes = RegrasVigentes.selecionar(
            listOf(
                regraParser(ITAU, versao = 1, padroes = PADRAO_VALIDO),
                regraParser(ITAU, versao = 2, padroes = "isto não é json"),
            ),
        )

        assertEquals(1, vigentes.getValue(ITAU).versao)
    }
}
