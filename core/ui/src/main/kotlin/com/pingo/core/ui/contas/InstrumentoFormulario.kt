package com.pingo.core.ui.contas

import androidx.compose.runtime.Immutable
import com.pingo.core.model.BeneficioVoucher
import com.pingo.core.model.Instrumento
import com.pingo.core.model.InstrumentoEditado
import com.pingo.core.model.NovoInstrumento
import com.pingo.core.model.TAMANHO_ULTIMOS_DIGITOS
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.model.apelidoValido
import com.pingo.core.model.diaDoMesInformado
import com.pingo.core.model.reaisParaCentavos
import com.pingo.core.model.ultimosDigitosValidos
import java.util.UUID

@Immutable
data class InstrumentoFormulario(
    val chave: String,
    val tipo: TipoInstrumento,
    val apelido: String,
    val publicId: String? = null,
    val ultimosDigitos: String = "",
    val fechamentoDia: String = "",
    val vencimentoDia: String = "",
    val beneficio: BeneficioVoucher? = null,
    val saldoInicial: String = "",
    val diaRecarga: String = "",
    val valorRecarga: String = "",
    val arquivado: Boolean = false,
) {
    fun apelidoValido(): Boolean = apelidoValido(apelido)

    fun ultimosDigitosValidos(): Boolean = ultimosDigitosValidos(digitos())

    fun fechamentoValido(): Boolean = diaDoMesInformado(fechamentoDia.toIntOrNull())

    fun vencimentoValido(): Boolean = diaDoMesInformado(vencimentoDia.toIntOrNull())

    fun saldoValido(): Boolean = reaisParaCentavos(saldoInicial) != null

    fun diaRecargaValido(): Boolean = diaRecarga.isBlank() || diaDoMesInformado(diaRecarga.toIntOrNull())

    fun valorRecargaValido(): Boolean = valorRecarga.isBlank() || reaisParaCentavos(valorRecarga) != null

    fun recargaValida(): Boolean = recargaVazia() || recargaCompleta()

    fun valido(): Boolean = paraNovoInstrumento().valido()

    fun paraNovoInstrumento(): NovoInstrumento = NovoInstrumento(
        tipo = tipo,
        apelido = apelido.trim(),
        ultimosDigitos = digitos(),
        fechamentoDia = inteiroDoCredito(fechamentoDia),
        vencimentoDia = inteiroDoCredito(vencimentoDia),
        beneficio = beneficio.takeIf { tipo == TipoInstrumento.VOUCHER },
        saldoCentavos = centavosDoVoucher(saldoInicial),
        diaRecarga = inteiroDoVoucher(diaRecarga),
        valorRecargaCentavos = centavosDoVoucher(valorRecarga),
    )

    fun paraEdicao(): InstrumentoEditado = InstrumentoEditado(
        tipo = tipo,
        apelido = apelido.trim(),
        publicId = publicId,
        ultimosDigitos = digitos(),
        fechamentoDia = inteiroDoCredito(fechamentoDia),
        vencimentoDia = inteiroDoCredito(vencimentoDia),
        beneficio = beneficio.takeIf { tipo == TipoInstrumento.VOUCHER },
        saldoCentavos = centavosDoVoucher(saldoInicial),
        diaRecarga = inteiroDoVoucher(diaRecarga),
        valorRecargaCentavos = centavosDoVoucher(valorRecarga),
    )

    private fun digitos(): String? = ultimosDigitos.takeIf { tipo != TipoInstrumento.VOUCHER && it.isNotBlank() }

    private fun inteiroDoCredito(texto: String): Int? = texto.takeIf { tipo == TipoInstrumento.CREDITO }?.toIntOrNull()

    private fun inteiroDoVoucher(texto: String): Int? = texto.takeIf { tipo == TipoInstrumento.VOUCHER && it.isNotBlank() }?.toIntOrNull()

    private fun centavosDoVoucher(texto: String): Long? = texto.takeIf { tipo == TipoInstrumento.VOUCHER }?.let(::reaisParaCentavos)

    private fun recargaVazia(): Boolean = diaRecarga.isBlank() && valorRecarga.isBlank()

    private fun recargaCompleta(): Boolean = diaDoMesInformado(diaRecarga.toIntOrNull()) && reaisParaCentavos(valorRecarga) != null

    companion object {
        fun novo(tipo: TipoInstrumento, chave: String = UUID.randomUUID().toString()): InstrumentoFormulario =
            InstrumentoFormulario(chave = chave, tipo = tipo, apelido = tipo.apelidoPadraoDeInstrumento())

        fun de(instrumento: Instrumento): InstrumentoFormulario = InstrumentoFormulario(
            chave = instrumento.publicId,
            tipo = instrumento.tipo,
            apelido = instrumento.apelido,
            publicId = instrumento.publicId,
            ultimosDigitos = instrumento.ultimosDigitos.orEmpty(),
            fechamentoDia = instrumento.fechamentoDia?.toString().orEmpty(),
            vencimentoDia = instrumento.vencimentoDia?.toString().orEmpty(),
            beneficio = instrumento.beneficio,
            saldoInicial = instrumento.saldoCentavos?.let(::centavosParaTexto).orEmpty(),
            diaRecarga = instrumento.diaRecarga?.toString().orEmpty(),
            valorRecarga = instrumento.valorRecargaCentavos?.let(::centavosParaTexto).orEmpty(),
            arquivado = instrumento.arquivado,
        )
    }
}

internal const val TAMANHO_DIGITOS_DO_INSTRUMENTO = TAMANHO_ULTIMOS_DIGITOS

fun centavosParaTexto(centavos: Long): String {
    val reais = centavos / 100
    val resto = (centavos % 100).toString().padStart(2, '0')

    return "$reais,$resto"
}
