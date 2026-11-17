package com.pingo.core.model

import androidx.compose.runtime.Immutable

@Immutable
data class ConfiguracaoDeInstrumento(
    val tipo: TipoInstrumento,
    val ultimosDigitos: String? = null,
    val fechamentoDia: Int? = null,
    val vencimentoDia: Int? = null,
    val beneficio: BeneficioVoucher? = null,
    val saldoCentavos: Long? = null,
    val diaRecarga: Int? = null,
    val valorRecargaCentavos: Long? = null,
) {
    fun valida(): Boolean = ultimosDigitosValidos(ultimosDigitos) && configuracaoDoTipoValida()

    private fun configuracaoDoTipoValida(): Boolean = when (tipo) {
        TipoInstrumento.CREDITO -> diaDoMesInformado(fechamentoDia) && diaDoMesInformado(vencimentoDia)
        TipoInstrumento.DEBITO -> true
        TipoInstrumento.VOUCHER -> voucherValido()
    }

    private fun voucherValido(): Boolean = beneficio != null && saldoCentavos != null && saldoCentavos >= 0 && recargaValida()

    private fun recargaValida(): Boolean = when {
        diaRecarga == null && valorRecargaCentavos == null -> true
        diaRecarga == null || valorRecargaCentavos == null -> false
        else -> diaDoMesInformado(diaRecarga) && valorRecargaCentavos > 0
    }
}
