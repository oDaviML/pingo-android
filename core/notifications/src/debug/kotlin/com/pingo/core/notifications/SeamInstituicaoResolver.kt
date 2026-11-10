package com.pingo.core.notifications

import com.pingo.core.model.InstituicaoFinanceira
import javax.inject.Inject

class SeamInstituicaoResolver @Inject constructor() : InstituicaoResolver {
    private val producao = PacoteInstituicaoResolver()

    override fun resolver(pacote: String, tag: String?): InstituicaoFinanceira? {
        producao.resolver(pacote, tag)?.let { return it }

        if (!aceitaSeam(pacote)) {
            return null
        }

        return instituicaoDaTag(tag)
    }

    private fun aceitaSeam(pacote: String): Boolean = pacote == PACOTE_SHELL || pacote.endsWith(SUFIXO_PACOTE_TESTE)

    private fun instituicaoDaTag(tag: String?): InstituicaoFinanceira? {
        val sufixo = tag?.takeIf { it.startsWith(PREFIXO_TAG) }?.removePrefix(PREFIXO_TAG)?.uppercase() ?: return null
        val instituicao = sufixo.substringBefore('-')

        return ALIASES[instituicao] ?: InstituicaoFinanceira.entries.firstOrNull { it.name == instituicao }
    }

    companion object {
        const val PREFIXO_TAG = "pingo-teste-"

        private const val PACOTE_SHELL = "com.android.shell"
        private const val SUFIXO_PACOTE_TESTE = ".test"

        private val ALIASES = mapOf(
            "MERCADOPAGO" to InstituicaoFinanceira.MERCADO_PAGO,
            "MERCADO_PAGO" to InstituicaoFinanceira.MERCADO_PAGO,
        )
    }
}
