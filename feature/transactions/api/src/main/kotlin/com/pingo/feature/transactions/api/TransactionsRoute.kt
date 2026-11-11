package com.pingo.feature.transactions.api

object TransactionsRoute {
    const val ROUTE = "transactions"
    const val ARG_PUBLIC_ID = "publicId"
    const val DETALHE_ROUTE = "$ROUTE/detalhe/{$ARG_PUBLIC_ID}"
    const val NOVO_ROUTE = "$ROUTE/novo"

    fun detalhe(publicId: String): String = "$ROUTE/detalhe/$publicId"
}
