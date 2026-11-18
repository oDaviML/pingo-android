package com.pingo.core.network.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FormatadorDeChangelogTest {
    @Test
    fun `remove rodape tecnico marcadores markdown e resume itens verbosos`() {
        val bruto = """
            APK release da versão `0.4.0` com melhorias levantadas após os testes da `0.3.0`.

            ### Novidades e melhorias
            - **Compras parceladas no cartão (inclusive retroativas)**: possibilidade de lançar compras em 2x até 99x escolhendo informar o valor total ou da parcela, com alocação automática nas faturas anteriores já fechadas e exclusão completa no detalhe.
            - **Logos dos bancos** (`STR-292`): ícones vetoriais próprios para `Nubank` e `Itaú`.

            ### Instalação / Atualização
            - Atualização direta via `adb install -r Pingo-0.4.0-release.apk`.

            ### Verificação
            - SHA-256 do APK: `d9b25c23c81f`

            Build a partir do commit `ac17324`.
        """.trimIndent()

        val formatada = FormatadorDeChangelog.formatar(bruto)

        assertFalse(formatada.vazia)
        assertEquals(1, formatada.secoes.size)
        val secao = formatada.secoes.single()
        assertEquals("Novidades e melhorias", secao.titulo)
        assertEquals(2, secao.itens.size)
        assertEquals("Compras parceladas no cartão (inclusive retroativas)", secao.itens[0].destaque)
        assertTrue(secao.itens[0].descricao.length <= 145)
        assertFalse(secao.itens[0].descricao.contains("`"))
        assertEquals("Logos dos bancos", secao.itens[1].destaque)
        assertEquals("ícones vetoriais próprios para Nubank e Itaú.", secao.itens[1].descricao)
    }

    @Test
    fun `formata texto simples sem lista quando release nao possui marcadores`() {
        val formatada = FormatadorDeChangelog.formatar("Correção de **estabilidade** no parser.")

        assertEquals(1, formatada.secoes.size)
        assertNull(formatada.secoes.single().titulo)
        assertEquals("Correção de estabilidade no parser.", formatada.secoes.single().itens.single().descricao)
    }

    @Test
    fun `retorna vazia quando houver apenas texto em branco`() {
        val formatada = FormatadorDeChangelog.formatar("   \n  ")
        assertTrue(formatada.vazia)
    }
}
