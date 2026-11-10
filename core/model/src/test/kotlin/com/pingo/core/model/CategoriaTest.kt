package com.pingo.core.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class CategoriaTest {
    private val agora = Instant.parse("2026-11-10T12:00:00Z")

    @Test
    fun `categoria ativa pode ser escolhida`() {
        assertTrue(categoria().podeSerEscolhida)
    }

    @Test
    fun `categoria arquivada nao pode ser escolhida para lancamentos novos`() {
        assertFalse(categoria(arquivada = true).podeSerEscolhida)
    }

    @Test
    fun `categoria excluida nao pode ser escolhida`() {
        assertFalse(categoria(excluidoEm = agora).podeSerEscolhida)
    }

    private fun categoria(arquivada: Boolean = false, excluidoEm: Instant? = null) = Categoria(
        publicId = "lazer",
        nome = "Lazer",
        tipo = TipoCategoria.DESPESA,
        arquivada = arquivada,
        criadoEm = agora,
        atualizadoEm = agora,
        excluidoEm = excluidoEm,
    )
}
