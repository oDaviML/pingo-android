package com.pingo.feature.transactions.api

object TransactionsRoute {
    const val ROUTE = "transactions"
    const val ARG_PUBLIC_ID = "publicId"
    const val ARG_CATEGORIA_ID = "categoriaId"
    const val ARG_MES = "mes"
    const val DETALHE_ROUTE = "$ROUTE/detalhe/{$ARG_PUBLIC_ID}"
    const val NOVO_ROUTE = "$ROUTE/novo"
    const val POR_CATEGORIA_ROUTE = "$ROUTE/categoria/{$ARG_CATEGORIA_ID}/{$ARG_MES}"

    fun detalhe(publicId: String): String = "$ROUTE/detalhe/$publicId"

    fun porCategoria(categoriaPublicId: String, mes: String): String = "$ROUTE/categoria/$categoriaPublicId/$mes"
}
