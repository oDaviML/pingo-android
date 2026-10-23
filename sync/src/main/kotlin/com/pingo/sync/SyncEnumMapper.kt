package com.pingo.sync

import com.pingo.core.model.BeneficioVoucher
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.StatusAssinatura
import com.pingo.core.model.StatusFatura
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoCategoria
import com.pingo.core.model.TipoConta
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.model.TipoLancamento

internal fun InstituicaoFinanceira.toApiValue(): String = when (this) {
    InstituicaoFinanceira.NUBANK -> "Nubank"
    InstituicaoFinanceira.INTER -> "Inter"
    InstituicaoFinanceira.ITAU -> "Itau"
    InstituicaoFinanceira.MERCADO_PAGO -> "MercadoPago"
    InstituicaoFinanceira.CAJU -> "Caju"
    InstituicaoFinanceira.OUTRA -> "Outra"
}

internal fun instituicaoFromApi(value: String): InstituicaoFinanceira = when (value) {
    "Nubank" -> InstituicaoFinanceira.NUBANK
    "Inter" -> InstituicaoFinanceira.INTER
    "Itau" -> InstituicaoFinanceira.ITAU
    "MercadoPago" -> InstituicaoFinanceira.MERCADO_PAGO
    "Caju" -> InstituicaoFinanceira.CAJU
    else -> InstituicaoFinanceira.OUTRA
}

internal fun TipoConta.toApiValue(): String = when (this) {
    TipoConta.CORRENTE -> "Corrente"
    TipoConta.BENEFICIO -> "Beneficio"
}

internal fun tipoContaFromApi(value: String): TipoConta = when (value) {
    "Beneficio" -> TipoConta.BENEFICIO
    else -> TipoConta.CORRENTE
}

internal fun TipoInstrumento.toApiValue(): String = when (this) {
    TipoInstrumento.CREDITO -> "Credito"
    TipoInstrumento.DEBITO -> "Debito"
    TipoInstrumento.VOUCHER -> "Voucher"
}

internal fun tipoInstrumentoFromApi(value: String): TipoInstrumento = when (value) {
    "Credito" -> TipoInstrumento.CREDITO
    "Voucher" -> TipoInstrumento.VOUCHER
    else -> TipoInstrumento.DEBITO
}

internal fun BeneficioVoucher.toApiValue(): String = when (this) {
    BeneficioVoucher.VR -> "VR"
    BeneficioVoucher.VA -> "VA"
    BeneficioVoucher.VT -> "VT"
}

internal fun beneficioFromApi(value: String): BeneficioVoucher? = when (value) {
    "VR" -> BeneficioVoucher.VR
    "VA" -> BeneficioVoucher.VA
    "VT" -> BeneficioVoucher.VT
    else -> null
}

internal fun TipoCategoria.toApiValue(): String = when (this) {
    TipoCategoria.DESPESA -> "Despesa"
    TipoCategoria.RECEITA -> "Receita"
}

internal fun tipoCategoriaFromApi(value: String): TipoCategoria = when (value) {
    "Receita" -> TipoCategoria.RECEITA
    else -> TipoCategoria.DESPESA
}

internal fun StatusAssinatura.toApiValue(): String = when (this) {
    StatusAssinatura.SUGERIDA -> "Sugerida"
    StatusAssinatura.CONFIRMADA -> "Confirmada"
    StatusAssinatura.RECUSADA -> "Recusada"
    StatusAssinatura.CANCELADA -> "Cancelada"
}

internal fun statusAssinaturaFromApi(value: String): StatusAssinatura = when (value) {
    "Confirmada" -> StatusAssinatura.CONFIRMADA
    "Recusada" -> StatusAssinatura.RECUSADA
    "Cancelada" -> StatusAssinatura.CANCELADA
    else -> StatusAssinatura.SUGERIDA
}

internal fun StatusFatura.toApiValue(): String = when (this) {
    StatusFatura.ABERTA -> "Aberta"
    StatusFatura.FECHADA -> "Fechada"
    StatusFatura.PAGA -> "Paga"
}

internal fun statusFaturaFromApi(value: String): StatusFatura = when (value) {
    "Fechada" -> StatusFatura.FECHADA
    "Paga" -> StatusFatura.PAGA
    else -> StatusFatura.ABERTA
}

internal fun TipoLancamento.toApiValue(): String = when (this) {
    TipoLancamento.DESPESA -> "Despesa"
    TipoLancamento.RECEITA -> "Receita"
    TipoLancamento.TRANSFERENCIA -> "Transferencia"
    TipoLancamento.AJUSTE -> "Ajuste"
}

internal fun tipoLancamentoFromApi(value: String): TipoLancamento = when (value) {
    "Receita" -> TipoLancamento.RECEITA
    "Transferencia" -> TipoLancamento.TRANSFERENCIA
    "Ajuste" -> TipoLancamento.AJUSTE
    else -> TipoLancamento.DESPESA
}

internal fun StatusLancamento.toApiValue(): String = when (this) {
    StatusLancamento.CONFIRMADO -> "Confirmado"
    StatusLancamento.PENDENTE -> "Pendente"
}

internal fun statusLancamentoFromApi(value: String): StatusLancamento = when (value) {
    "Pendente" -> StatusLancamento.PENDENTE
    else -> StatusLancamento.CONFIRMADO
}

internal fun OrigemLancamento.toApiValue(): String = when (this) {
    OrigemLancamento.NOTIFICACAO -> "Notificacao"
    OrigemLancamento.MANUAL -> "Manual"
}

internal fun origemLancamentoFromApi(value: String): OrigemLancamento = when (value) {
    "Manual" -> OrigemLancamento.MANUAL
    else -> OrigemLancamento.NOTIFICACAO
}
