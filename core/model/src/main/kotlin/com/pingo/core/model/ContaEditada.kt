package com.pingo.core.model

import androidx.compose.runtime.Immutable

@Immutable
data class ContaEditada(val publicId: String, val apelido: String, val tipo: TipoConta, val instrumentos: List<InstrumentoEditado>) {
    fun valida(): Boolean = publicId.isNotBlank() &&
        apelidoValido(apelido) &&
        instrumentos.isNotEmpty() &&
        instrumentos.all(InstrumentoEditado::valido) &&
        publicIdsUnicos()
}

@Immutable
data class InstrumentoEditado(
    val tipo: TipoInstrumento,
    val apelido: String,
    val publicId: String? = null,
    val ultimosDigitos: String? = null,
    val fechamentoDia: Int? = null,
    val vencimentoDia: Int? = null,
    val beneficio: BeneficioVoucher? = null,
    val saldoCentavos: Long? = null,
    val diaRecarga: Int? = null,
    val valorRecargaCentavos: Long? = null,
) {
    fun valido(): Boolean = apelidoValido(apelido) && configuracao().valida()

    fun configuracao(): ConfiguracaoDeInstrumento = ConfiguracaoDeInstrumento(
        tipo = tipo,
        ultimosDigitos = ultimosDigitos,
        fechamentoDia = fechamentoDia,
        vencimentoDia = vencimentoDia,
        beneficio = beneficio,
        saldoCentavos = saldoCentavos,
        diaRecarga = diaRecarga,
        valorRecargaCentavos = valorRecargaCentavos,
    )
}

private fun ContaEditada.publicIdsUnicos(): Boolean {
    val informados = instrumentos.mapNotNull(InstrumentoEditado::publicId)

    return informados.distinct().size == informados.size
}
