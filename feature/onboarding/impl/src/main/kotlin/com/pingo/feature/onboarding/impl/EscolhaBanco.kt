package com.pingo.feature.onboarding.impl

import androidx.compose.runtime.Immutable
import com.pingo.core.model.BeneficioVoucher
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.NovaConta
import com.pingo.core.model.NovoInstrumento
import com.pingo.core.model.TipoConta
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.model.apelidoValido
import com.pingo.core.model.diaDoMesInformado
import com.pingo.core.model.reaisParaCentavos
import com.pingo.core.model.ultimosDigitosValidos

@Immutable
data class EscolhaBanco(
    val instituicao: InstituicaoFinanceira,
    val apelido: String,
    val tipoConta: TipoConta,
    val instrumentos: List<InstrumentoEscolhido>,
) {
    fun temInstrumento(tipo: TipoInstrumento): Boolean = instrumentos.any { it.tipo == tipo }

    fun adicionarInstrumento(tipo: TipoInstrumento): EscolhaBanco =
        copy(instrumentos = instrumentos + InstrumentoEscolhido(tipo = tipo, apelido = tipo.apelidoPadrao(), ultimosDigitos = ""))

    fun removerInstrumento(tipo: TipoInstrumento): EscolhaBanco =
        if (instrumentos.size > 1) copy(instrumentos = instrumentos.filterNot { it.tipo == tipo }) else this

    fun apelidoEhValido(): Boolean = apelidoValido(apelido)

    fun valida(): Boolean = paraNovaConta().valida()

    fun paraNovaConta(): NovaConta = NovaConta(
        instituicao = instituicao,
        apelido = apelido,
        tipo = tipoConta,
        instrumentos = instrumentos.map(InstrumentoEscolhido::paraNovoInstrumento),
    )
}

@Immutable
data class InstrumentoEscolhido(
    val tipo: TipoInstrumento,
    val apelido: String,
    val ultimosDigitos: String,
    val fechamentoDia: String = "",
    val vencimentoDia: String = "",
    val beneficio: BeneficioVoucher? = null,
    val saldoInicial: String = "",
    val diaRecarga: String = "",
    val valorRecarga: String = "",
) {
    fun apelidoEhValido(): Boolean = apelidoValido(apelido)

    fun ultimosDigitosSaoValidos(): Boolean = ultimosDigitosValidos(digitosInformados())

    fun fechamentoEhValido(): Boolean = diaDoMesInformado(fechamentoDia.toIntOrNull())

    fun vencimentoEhValido(): Boolean = diaDoMesInformado(vencimentoDia.toIntOrNull())

    fun saldoEhValido(): Boolean = reaisParaCentavos(saldoInicial) != null

    fun recargaEhValida(): Boolean = recargaVazia() || recargaCompleta()

    fun diaRecargaEhValido(): Boolean = diaRecarga.isBlank() || diaDoMesInformado(diaRecarga.toIntOrNull())

    fun valorRecargaEhValido(): Boolean = valorRecarga.isBlank() || reaisParaCentavos(valorRecarga) != null

    fun paraNovoInstrumento(): NovoInstrumento = NovoInstrumento(
        tipo = tipo,
        apelido = apelido,
        ultimosDigitos = digitosInformados(),
        fechamentoDia = fechamentoDia.takeIf { tipo == TipoInstrumento.CREDITO }?.toIntOrNull(),
        vencimentoDia = vencimentoDia.takeIf { tipo == TipoInstrumento.CREDITO }?.toIntOrNull(),
        beneficio = beneficio.takeIf { tipo == TipoInstrumento.VOUCHER },
        saldoCentavos = reaisParaCentavos(saldoInicial).takeIf { tipo == TipoInstrumento.VOUCHER },
        diaRecarga = diaRecarga.takeIf { tipo == TipoInstrumento.VOUCHER }?.toIntOrNull(),
        valorRecargaCentavos = reaisParaCentavos(valorRecarga).takeIf { tipo == TipoInstrumento.VOUCHER },
    )

    private fun recargaVazia(): Boolean = diaRecarga.isBlank() && valorRecarga.isBlank()

    private fun recargaCompleta(): Boolean = diaDoMesInformado(diaRecarga.toIntOrNull()) && reaisParaCentavos(valorRecarga) != null

    private fun digitosInformados(): String? = ultimosDigitos.takeIf { it.isNotBlank() }
}
