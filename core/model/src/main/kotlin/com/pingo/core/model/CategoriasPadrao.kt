package com.pingo.core.model

data class CategoriaPadrao(val nome: String, val tipo: TipoCategoria)

object CategoriasPadrao {
    private val despesas = listOf(
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
    )

    private val receitas = listOf("Salário", "Benefício", "Reembolso", "Outros")

    val todas: List<CategoriaPadrao> =
        despesas.map { CategoriaPadrao(it, TipoCategoria.DESPESA) } + receitas.map { CategoriaPadrao(it, TipoCategoria.RECEITA) }
}
