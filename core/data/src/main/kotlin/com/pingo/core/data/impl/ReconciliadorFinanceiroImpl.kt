package com.pingo.core.data.impl

import com.pingo.core.data.reconciliacao.ReconciliadorFinanceiro
import com.pingo.core.model.Lancamento
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReconciliadorFinanceiroImpl @Inject constructor(
    private val faturas: ReconciliadorDeFaturasLocal,
    private val vouchers: ReconciliadorDeVouchersLocal,
    private val assinaturas: ReconciliadorDeAssinaturasLocal,
) : ReconciliadorFinanceiro {
    override suspend fun aoAlterarLancamentos(pares: List<Pair<Lancamento?, Lancamento?>>) {
        if (pares.isEmpty()) {
            return
        }
        faturas.reconciliarAlteracoes(pares)
        vouchers.reconciliarAlteracoes(pares)
        assinaturas.reconciliarAlteracoes(pares)
    }
}
