package com.pingo.feature.onboarding.impl

import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.TipoConta
import com.pingo.core.model.TipoInstrumento

internal data class BancoOferecido(
    val instituicao: InstituicaoFinanceira,
    val nome: String,
    val descricao: String,
    val letras: String,
    val tipoConta: TipoConta,
    val tipoInstrumento: TipoInstrumento,
) {
    fun escolhaInicial(): EscolhaBanco = EscolhaBanco(
        instituicao = instituicao,
        apelido = nome,
        tipoConta = tipoConta,
        instrumentos = listOf(InstrumentoEscolhido(tipo = tipoInstrumento, apelido = tipoInstrumento.apelidoPadrao(), ultimosDigitos = "")),
    )

    companion object {
        val todos: List<BancoOferecido> = listOf(
            BancoOferecido(
                instituicao = InstituicaoFinanceira.NUBANK,
                nome = "Nubank",
                descricao = "Cartão de crédito e conta",
                letras = "N",
                tipoConta = TipoConta.CORRENTE,
                tipoInstrumento = TipoInstrumento.CREDITO,
            ),
            BancoOferecido(
                instituicao = InstituicaoFinanceira.INTER,
                nome = "Inter",
                descricao = "Cartão de crédito e conta",
                letras = "i",
                tipoConta = TipoConta.CORRENTE,
                tipoInstrumento = TipoInstrumento.CREDITO,
            ),
            BancoOferecido(
                instituicao = InstituicaoFinanceira.ITAU,
                nome = "Itaú",
                descricao = "Conta-salário e débito",
                letras = "it",
                tipoConta = TipoConta.CORRENTE,
                tipoInstrumento = TipoInstrumento.DEBITO,
            ),
            BancoOferecido(
                instituicao = InstituicaoFinanceira.MERCADO_PAGO,
                nome = "Mercado Pago",
                descricao = "Compras online",
                letras = "mp",
                tipoConta = TipoConta.CORRENTE,
                tipoInstrumento = TipoInstrumento.CREDITO,
            ),
            BancoOferecido(
                instituicao = InstituicaoFinanceira.CAJU,
                nome = "Caju",
                descricao = "Benefícios VR, VA e VT",
                letras = "c",
                tipoConta = TipoConta.BENEFICIO,
                tipoInstrumento = TipoInstrumento.VOUCHER,
            ),
            BancoOferecido(
                instituicao = InstituicaoFinanceira.OUTRA,
                nome = "Outra",
                descricao = "Para lançamentos manuais",
                letras = "?",
                tipoConta = TipoConta.CORRENTE,
                tipoInstrumento = TipoInstrumento.DEBITO,
            ),
        )

        fun de(instituicao: InstituicaoFinanceira): BancoOferecido = todos.first { it.instituicao == instituicao }
    }
}
