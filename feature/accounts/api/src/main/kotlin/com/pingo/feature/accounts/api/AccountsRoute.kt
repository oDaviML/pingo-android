package com.pingo.feature.accounts.api

object AccountsRoute {
    const val ROUTE = "accounts"

    const val ARG_CONTA_ID = "contaId"

    const val ARG_INSTRUMENTO_ID = "instrumentoId"

    const val NOVA_ROUTE = "accounts/nova"

    const val EDITAR_ROUTE = "accounts/editar/{$ARG_CONTA_ID}"

    const val FATURAS_ROUTE = "accounts/faturas/{$ARG_INSTRUMENTO_ID}"

    fun editar(publicId: String): String = "accounts/editar/$publicId"

    fun faturas(instrumentoPublicId: String): String = "accounts/faturas/$instrumentoPublicId"
}
