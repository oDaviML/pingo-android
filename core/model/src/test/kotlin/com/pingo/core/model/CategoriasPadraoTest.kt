package com.pingo.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class CategoriasPadraoTest {
    @Test
    fun `despesas padrao seguem a RN-10`() {
        assertEquals(
            listOf(
                "Alimentação",
                "Mercado",
                "Transporte",
                "Moradia",
                "Contas da casa",
                "Saúde",
                "Lazer",
                "Compras",
                "Assinaturas",
                "Educação",
                "Outros",
            ),
            nomesDo(TipoCategoria.DESPESA),
        )
    }

    @Test
    fun `receitas padrao seguem a RN-10`() {
        assertEquals(listOf("Salário", "Benefício", "Reembolso", "Outros"), nomesDo(TipoCategoria.RECEITA))
    }

    @Test
    fun `nomes padrao nao se repetem dentro do mesmo tipo`() {
        val todas = CategoriasPadrao.todas

        assertEquals(todas.size, todas.map { it.tipo to it.nome }.toSet().size)
    }

    private fun nomesDo(tipo: TipoCategoria): List<String> = CategoriasPadrao.todas.filter { it.tipo == tipo }.map { it.nome }
}
