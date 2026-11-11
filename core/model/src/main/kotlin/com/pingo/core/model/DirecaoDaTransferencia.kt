package com.pingo.core.model

val TipoLancamento.direcaoDaTransferencia: TipoLancamento?
    get() = when (this) {
        TipoLancamento.DESPESA, TipoLancamento.RECEITA -> this
        TipoLancamento.TRANSFERENCIA, TipoLancamento.AJUSTE -> null
    }

val TipoLancamento.direcaoOposta: TipoLancamento?
    get() = when (this) {
        TipoLancamento.DESPESA -> TipoLancamento.RECEITA
        TipoLancamento.RECEITA -> TipoLancamento.DESPESA
        TipoLancamento.TRANSFERENCIA, TipoLancamento.AJUSTE -> null
    }
