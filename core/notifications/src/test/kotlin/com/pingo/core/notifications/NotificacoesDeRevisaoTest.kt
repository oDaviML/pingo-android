package com.pingo.core.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificacoesDeRevisaoTest {
    @Test
    fun `conteudo traz estabelecimento valor e categoria sugerida`() {
        val conteudo = NotificacoesDeRevisao.conteudo(
            PendenciaParaNotificar(
                publicId = "pendencia-1",
                estabelecimento = "Padaria do Zé",
                valorCentavos = 4290,
                categoria = "Alimentação",
            ),
            precisaCorrigir = false,
        )

        assertEquals("Lançamento para revisar", conteudo.titulo)
        assertTrue(conteudo.texto.contains("Padaria do Zé"))
        assertTrue(conteudo.texto.contains("R$ 42,90"))
        assertTrue(conteudo.texto.contains("Categoria sugerida: Alimentação"))
        assertNull(conteudo.aviso)
        assertEquals(conteudo.resumo, conteudo.textoLongo)
    }

    @Test
    fun `conteudo sem estabelecimento e sem categoria usa rotulos neutros`() {
        val conteudo = NotificacoesDeRevisao.conteudo(
            PendenciaParaNotificar(publicId = "pendencia-1", estabelecimento = " ", valorCentavos = 1990, categoria = null),
            precisaCorrigir = false,
        )

        assertTrue(conteudo.texto.contains("Lançamento"))
        assertTrue(conteudo.texto.contains("R$ 19,90"))
        assertTrue(conteudo.texto.contains("Categoria sugerida: Sem categoria"))
    }

    @Test
    fun `conteudo de correcao pede correcao e preserva o resumo no texto longo`() {
        val pendencia = PendenciaParaNotificar("pendencia-1", "Padaria do Zé", 4290, "Alimentação")

        val conteudo = NotificacoesDeRevisao.conteudo(pendencia, precisaCorrigir = true)

        assertEquals(NotificacoesDeRevisao.AVISO_CORRIGIR, conteudo.texto)
        assertTrue(conteudo.texto.contains("Corrigir"))
        assertTrue(conteudo.textoLongo.contains(NotificacoesDeRevisao.AVISO_CORRIGIR))
        assertTrue(conteudo.textoLongo.contains("Padaria do Zé"))
    }

    @Test
    fun `id de notificacao e estavel por pendencia e distinto entre pendencias`() {
        assertEquals(
            NotificacoesDeRevisao.idDe("pendencia-1"),
            NotificacoesDeRevisao.idDe("pendencia-1"),
        )
        assertTrue(NotificacoesDeRevisao.idDe("pendencia-1") != NotificacoesDeRevisao.idDe("pendencia-2"))
    }

    @Test
    fun `padrao de deep link do detalhe usa o argumento publicId`() {
        assertEquals("pingo://detalhe/{publicId}", NotificacoesDeRevisao.PADRAO_DEEP_LINK_DETALHE)
        assertNotNull(NotificacoesDeRevisao.CANAL)
        assertEquals("Confirmar", NotificacoesDeRevisao.ACAO_CONFIRMAR_LABEL)
        assertEquals("Corrigir", NotificacoesDeRevisao.ACAO_CORRIGIR_LABEL)
    }
}
