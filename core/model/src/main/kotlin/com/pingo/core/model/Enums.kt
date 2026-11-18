package com.pingo.core.model

enum class TipoLancamento {
    DESPESA,
    RECEITA,
    TRANSFERENCIA,
    AJUSTE,
}

enum class StatusLancamento {
    CONFIRMADO,
    PENDENTE,
}

enum class OrigemLancamento {
    NOTIFICACAO,
    MANUAL,
}

enum class TipoConta {
    CORRENTE,
    BENEFICIO,
}

enum class TipoInstrumento {
    CREDITO,
    DEBITO,
    VOUCHER,
}

enum class TipoCategoria {
    DESPESA,
    RECEITA,
}

enum class StatusFatura {
    ABERTA,
    FECHADA,
    PAGA,
}

enum class StatusAssinatura {
    SUGERIDA,
    CONFIRMADA,
    RECUSADA,
    CANCELADA,
}

enum class InstituicaoFinanceira {
    NUBANK,
    INTER,
    ITAU,
    MERCADO_PAGO,
    CAJU,
    OUTRA,
}

enum class BeneficioVoucher {
    VR,
    VA,
    VT,
}

enum class TemaDoApp {
    SISTEMA,
    CLARO,
    ESCURO,
}
