package com.pingo.core.data.reconciliacao

import com.pingo.core.model.Lancamento

interface ReconciliadorFinanceiro {
    suspend fun aoAlterarLancamentos(pares: List<Pair<Lancamento?, Lancamento?>>)

    object NoOp : ReconciliadorFinanceiro {
        override suspend fun aoAlterarLancamentos(pares: List<Pair<Lancamento?, Lancamento?>>) = Unit
    }
}
