package com.pingo.core.notifications

import com.pingo.core.model.InstituicaoFinanceira
import javax.inject.Inject

class PacoteInstituicaoResolver @Inject constructor() : InstituicaoResolver {
    override fun resolver(pacote: String, tag: String?): InstituicaoFinanceira? = PACOTES[pacote]

    companion object {
        val PACOTES: Map<String, InstituicaoFinanceira> = mapOf(
            "com.nu.production" to InstituicaoFinanceira.NUBANK,
            "br.com.intermedium" to InstituicaoFinanceira.INTER,
            "com.itau" to InstituicaoFinanceira.ITAU,
            "br.com.itau.internet" to InstituicaoFinanceira.ITAU,
            "com.mercadopago.wallet" to InstituicaoFinanceira.MERCADO_PAGO,
            "com.caju.employeeApp" to InstituicaoFinanceira.CAJU,
        )
    }
}
