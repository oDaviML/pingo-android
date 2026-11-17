package com.pingo.core.ui.contas

import com.pingo.core.model.BeneficioVoucher
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.TAMANHO_ULTIMOS_DIGITOS
import com.pingo.core.model.TipoConta
import com.pingo.core.model.TipoInstrumento

sealed interface EventoDoFormularioDeConta {
    data class AlterarInstituicao(val instituicao: InstituicaoFinanceira) : EventoDoFormularioDeConta

    data class AlterarApelidoConta(val apelido: String) : EventoDoFormularioDeConta

    data class AlterarTipoConta(val tipo: TipoConta) : EventoDoFormularioDeConta

    data class AdicionarInstrumento(val tipo: TipoInstrumento) : EventoDoFormularioDeConta

    data class RemoverInstrumento(val chave: String) : EventoDoFormularioDeConta

    data class AlterarApelidoInstrumento(val chave: String, val apelido: String) : EventoDoFormularioDeConta

    data class AlterarUltimosDigitos(val chave: String, val digitos: String) : EventoDoFormularioDeConta

    data class AlterarFechamento(val chave: String, val dia: String) : EventoDoFormularioDeConta

    data class AlterarVencimento(val chave: String, val dia: String) : EventoDoFormularioDeConta

    data class AlterarBeneficio(val chave: String, val beneficio: BeneficioVoucher) : EventoDoFormularioDeConta

    data class AlterarSaldoInicial(val chave: String, val saldo: String) : EventoDoFormularioDeConta

    data class AlterarDiaRecarga(val chave: String, val dia: String) : EventoDoFormularioDeConta

    data class AlterarValorRecarga(val chave: String, val valor: String) : EventoDoFormularioDeConta
}

fun ContaFormulario.aplicar(evento: EventoDoFormularioDeConta): ContaFormulario = when (evento) {
    is EventoDoFormularioDeConta.AlterarInstituicao -> trocarInstituicao(evento.instituicao)
    is EventoDoFormularioDeConta.AlterarApelidoConta -> copy(apelido = evento.apelido)
    is EventoDoFormularioDeConta.AlterarTipoConta -> copy(tipoConta = evento.tipo)
    is EventoDoFormularioDeConta.AdicionarInstrumento -> adicionarInstrumento(evento.tipo)
    is EventoDoFormularioDeConta.RemoverInstrumento -> removerInstrumento(evento.chave)
    is EventoDoFormularioDeConta.AlterarApelidoInstrumento ->
        alterarInstrumento(evento.chave) { it.copy(apelido = evento.apelido) }

    is EventoDoFormularioDeConta.AlterarUltimosDigitos ->
        alterarInstrumento(evento.chave) { it.copy(ultimosDigitos = normalizarDigitos(evento.digitos)) }

    is EventoDoFormularioDeConta.AlterarFechamento ->
        alterarInstrumento(evento.chave) { it.copy(fechamentoDia = normalizarDia(evento.dia)) }

    is EventoDoFormularioDeConta.AlterarVencimento ->
        alterarInstrumento(evento.chave) { it.copy(vencimentoDia = normalizarDia(evento.dia)) }

    is EventoDoFormularioDeConta.AlterarBeneficio ->
        alterarInstrumento(evento.chave) { it.copy(beneficio = evento.beneficio) }

    is EventoDoFormularioDeConta.AlterarSaldoInicial ->
        alterarInstrumento(evento.chave) { it.copy(saldoInicial = normalizarValor(evento.saldo)) }

    is EventoDoFormularioDeConta.AlterarDiaRecarga ->
        alterarInstrumento(evento.chave) { it.copy(diaRecarga = normalizarDia(evento.dia)) }

    is EventoDoFormularioDeConta.AlterarValorRecarga ->
        alterarInstrumento(evento.chave) { it.copy(valorRecarga = normalizarValor(evento.valor)) }
}

private fun ContaFormulario.trocarInstituicao(nova: InstituicaoFinanceira): ContaFormulario {
    if (nova == instituicao) {
        return this
    }

    val apelidoAnterior = apelido
    val apelidoAtualizado = if (apelidoAnterior.isBlank() || apelidoAnterior == instituicao.nomeDeExibicao()) {
        nova.nomeDeExibicao()
    } else {
        apelidoAnterior
    }
    val instrumentosAtualizados = if (instrumentosPadraoIntocados()) {
        listOf(InstrumentoFormulario.novo(nova.tipoDeInstrumentoPadrao()))
    } else {
        instrumentos
    }

    return copy(instituicao = nova, apelido = apelidoAtualizado, instrumentos = instrumentosAtualizados)
}

private fun ContaFormulario.instrumentosPadraoIntocados(): Boolean = instrumentos.size == 1 && instrumentos.single().let { instrumento ->
    instrumento.publicId == null &&
        instrumento.apelido == instrumento.tipo.apelidoPadraoDeInstrumento() &&
        instrumento.ultimosDigitos.isBlank() &&
        instrumento.fechamentoDia.isBlank() &&
        instrumento.vencimentoDia.isBlank() &&
        instrumento.beneficio == null &&
        instrumento.saldoInicial.isBlank() &&
        instrumento.diaRecarga.isBlank() &&
        instrumento.valorRecarga.isBlank()
}

private fun normalizarDigitos(texto: String): String = texto.filter { it in '0'..'9' }.take(TAMANHO_ULTIMOS_DIGITOS)

private fun normalizarDia(texto: String): String = texto.filter { it in '0'..'9' }.take(TAMANHO_DO_DIA)

private fun normalizarValor(texto: String): String = texto.filter { it in '0'..'9' || it == ',' || it == '.' }.take(TAMANHO_DO_VALOR)

private const val TAMANHO_DO_DIA = 2
private const val TAMANHO_DO_VALOR = 12
