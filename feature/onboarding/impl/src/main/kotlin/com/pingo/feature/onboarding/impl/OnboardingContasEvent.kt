package com.pingo.feature.onboarding.impl

import com.pingo.core.model.BeneficioVoucher
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.TipoConta
import com.pingo.core.model.TipoInstrumento

sealed interface OnboardingContasEvent {
    data class AlternarBanco(val instituicao: InstituicaoFinanceira) : OnboardingContasEvent

    data class AlterarApelidoConta(val instituicao: InstituicaoFinanceira, val apelido: String) : OnboardingContasEvent

    data class AlterarTipoConta(val instituicao: InstituicaoFinanceira, val tipo: TipoConta) : OnboardingContasEvent

    data class AlternarInstrumento(val instituicao: InstituicaoFinanceira, val tipo: TipoInstrumento) : OnboardingContasEvent

    data class AlterarApelidoInstrumento(val instituicao: InstituicaoFinanceira, val tipo: TipoInstrumento, val apelido: String) :
        OnboardingContasEvent

    data class AlterarUltimosDigitos(val instituicao: InstituicaoFinanceira, val tipo: TipoInstrumento, val digitos: String) :
        OnboardingContasEvent

    data class AlterarFechamento(override val instituicao: InstituicaoFinanceira, override val tipo: TipoInstrumento, val dia: String) :
        ConfiguracaoDeInstrumento

    data class AlterarVencimento(override val instituicao: InstituicaoFinanceira, override val tipo: TipoInstrumento, val dia: String) :
        ConfiguracaoDeInstrumento

    data class AlterarBeneficio(
        override val instituicao: InstituicaoFinanceira,
        override val tipo: TipoInstrumento,
        val beneficio: BeneficioVoucher,
    ) : ConfiguracaoDeInstrumento

    data class AlterarSaldoInicial(
        override val instituicao: InstituicaoFinanceira,
        override val tipo: TipoInstrumento,
        val saldo: String,
    ) : ConfiguracaoDeInstrumento

    data class AlterarDiaRecarga(override val instituicao: InstituicaoFinanceira, override val tipo: TipoInstrumento, val dia: String) :
        ConfiguracaoDeInstrumento

    data class AlterarValorRecarga(
        override val instituicao: InstituicaoFinanceira,
        override val tipo: TipoInstrumento,
        val valor: String,
    ) : ConfiguracaoDeInstrumento

    data object Continuar : OnboardingContasEvent

    data object Pular : OnboardingContasEvent

    sealed interface ConfiguracaoDeInstrumento : OnboardingContasEvent {
        val instituicao: InstituicaoFinanceira

        val tipo: TipoInstrumento
    }
}
