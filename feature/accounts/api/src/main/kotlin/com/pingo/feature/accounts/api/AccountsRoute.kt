package com.pingo.feature.accounts.api

object AccountsRoute {
    const val ROUTE = "accounts"

    const val ARG_CONTA_ID = "contaId"

    const val NOVA_ROUTE = "accounts/nova"

    const val EDITAR_ROUTE = "accounts/editar/{$ARG_CONTA_ID}"

    fun editar(publicId: String): String = "accounts/editar/$publicId"
}
