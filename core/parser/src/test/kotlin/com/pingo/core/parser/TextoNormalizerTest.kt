package com.pingo.core.parser

import org.junit.Assert.assertEquals
import org.junit.Test

class TextoNormalizerTest {
    @Test
    fun `NBSP vira espaco comum`() {
        assertEquals("R$ 37,90", TextoNormalizer.normalizar("R$\u00A037,90"))
    }

    @Test
    fun `emojis e variation selectors saem do texto`() {
        assertEquals("Sua compra foi aprovada", TextoNormalizer.normalizar("Sua compra foi aprovada 💳"))
        assertEquals("Aviso", TextoNormalizer.normalizar("⚠️ Aviso 🗓️"))
    }

    @Test
    fun `caracteres invisiveis saem e espacos repetidos colapsam`() {
        assertEquals("Compra aprovada", TextoNormalizer.normalizar("Com\u200Bpra \t  aprovada\u200D"))
    }

    @Test
    fun `separador de linha Unicode vira espaco e nao quebra a linha`() {
        assertEquals("Mais cashback! 8% Booking", TextoNormalizer.normalizar("Mais cashback!\u2028🏨 8% Booking"))
    }

    @Test
    fun `linhas vazias saem e quebras de linha padronizam para LF`() {
        assertEquals("Titulo\nTexto", TextoNormalizer.normalizar("Titulo\r\n\r\n  Texto  \r\n"))
    }

    @Test
    fun `normalizar um texto ja normalizado nao o altera`() {
        val normalizado = TextoNormalizer.normalizar("Olá\u00A0 💳 Compra\u200B  de R$ 1.251,74\nBigText")

        assertEquals(normalizado, TextoNormalizer.normalizar(normalizado))
    }
}
