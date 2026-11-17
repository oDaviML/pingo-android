package com.pingo.core.ui.contas

import com.pingo.core.model.BeneficioVoucher
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.TipoConta
import com.pingo.core.model.TipoInstrumento

val instituicoesFinanceiras: List<InstituicaoFinanceira> = listOf(
    InstituicaoFinanceira.NUBANK,
    InstituicaoFinanceira.INTER,
    InstituicaoFinanceira.ITAU,
    InstituicaoFinanceira.MERCADO_PAGO,
    InstituicaoFinanceira.CAJU,
    InstituicaoFinanceira.OUTRA,
)

fun InstituicaoFinanceira.nomeDeExibicao(): String = when (this) {
    InstituicaoFinanceira.NUBANK -> "Nubank"
    InstituicaoFinanceira.INTER -> "Inter"
    InstituicaoFinanceira.ITAU -> "Itaú"
    InstituicaoFinanceira.MERCADO_PAGO -> "Mercado Pago"
    InstituicaoFinanceira.CAJU -> "Caju"
    InstituicaoFinanceira.OUTRA -> "Outra"
}

fun InstituicaoFinanceira.descricaoDeExibicao(): String = when (this) {
    InstituicaoFinanceira.NUBANK -> "Cartão de crédito e conta"
    InstituicaoFinanceira.INTER -> "Cartão de crédito e conta"
    InstituicaoFinanceira.ITAU -> "Conta-salário e débito"
    InstituicaoFinanceira.MERCADO_PAGO -> "Compras online"
    InstituicaoFinanceira.CAJU -> "Benefícios VR, VA e VT"
    InstituicaoFinanceira.OUTRA -> "Só lançamentos manuais"
}

fun InstituicaoFinanceira.letrasDeExibicao(): String = when (this) {
    InstituicaoFinanceira.NUBANK -> "N"
    InstituicaoFinanceira.INTER -> "i"
    InstituicaoFinanceira.ITAU -> "it"
    InstituicaoFinanceira.MERCADO_PAGO -> "mp"
    InstituicaoFinanceira.CAJU -> "c"
    InstituicaoFinanceira.OUTRA -> "?"
}

val InstituicaoFinanceira.somenteManual: Boolean
    get() = this == InstituicaoFinanceira.OUTRA

fun InstituicaoFinanceira.tipoDeContaPadrao(): TipoConta =
    if (this == InstituicaoFinanceira.CAJU) TipoConta.BENEFICIO else TipoConta.CORRENTE

fun InstituicaoFinanceira.tipoDeInstrumentoPadrao(): TipoInstrumento = when (this) {
    InstituicaoFinanceira.CAJU -> TipoInstrumento.VOUCHER
    InstituicaoFinanceira.ITAU, InstituicaoFinanceira.OUTRA -> TipoInstrumento.DEBITO
    InstituicaoFinanceira.NUBANK, InstituicaoFinanceira.INTER, InstituicaoFinanceira.MERCADO_PAGO -> TipoInstrumento.CREDITO
}

fun TipoConta.rotuloDeExibicao(): String = when (this) {
    TipoConta.CORRENTE -> "Corrente"
    TipoConta.BENEFICIO -> "Benefício"
}

fun TipoInstrumento.rotuloDeExibicao(): String = when (this) {
    TipoInstrumento.CREDITO -> "Crédito"
    TipoInstrumento.DEBITO -> "Débito"
    TipoInstrumento.VOUCHER -> "Voucher"
}

fun TipoInstrumento.apelidoPadraoDeInstrumento(): String = when (this) {
    TipoInstrumento.CREDITO -> "Cartão de crédito"
    TipoInstrumento.DEBITO -> "Cartão de débito"
    TipoInstrumento.VOUCHER -> "Voucher"
}

fun BeneficioVoucher.rotuloDeExibicao(): String = name

fun BeneficioVoucher.descricaoDeExibicao(): String = when (this) {
    BeneficioVoucher.VR -> "Vale-refeição"
    BeneficioVoucher.VA -> "Vale-alimentação"
    BeneficioVoucher.VT -> "Vale-transporte"
}
