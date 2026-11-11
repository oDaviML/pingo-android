package com.pingo.feature.transactions.impl

import com.pingo.core.data.detalhe.CampoDoDetalhe
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.ui.Valores
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

internal object RotulosDoDetalhe {
    private val LOCALE = Locale("pt", "BR")
    private val DATA_E_HORA: DateTimeFormatter = DateTimeFormatter.ofPattern("d 'de' MMMM, HH:mm", LOCALE)
    private val DIA_CURTO_E_HORA: DateTimeFormatter = DateTimeFormatter.ofPattern("d 'de' MMM, HH:mm", LOCALE)
    private val HORA: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", LOCALE)

    fun valorDoLancamento(transferencia: Boolean): String = if (transferencia) "Valor transferido" else "Valor do lançamento"

    fun legendaDoValor(erro: Boolean, transferencia: Boolean): String =
        if (erro) "O valor precisa ser maior que zero" else valorDoLancamento(transferencia)

    fun rotuloDoEstabelecimento(origem: OrigemDoLancamentoUi?): String =
        if (origem is OrigemDoLancamentoUi.Manual) "Descrição" else "Estabelecimento"

    fun dataEHora(dataHora: Instant, zona: ZoneId): String = DATA_E_HORA.format(dataHora.atZone(zona))

    fun mensagemDoCampo(campo: CampoDoDetalhe, state: DetalheDoLancamentoUiState): String? = when (campo) {
        CampoDoDetalhe.VALOR -> "O valor precisa ser maior que zero"
        CampoDoDetalhe.INSTRUMENTO -> mensagemDoInstrumento(state)
        CampoDoDetalhe.DESTINO -> "Escolha uma conta de destino diferente da origem"
        CampoDoDetalhe.CATEGORIA -> "Escolha uma categoria para este lançamento"
    }

    private fun mensagemDoInstrumento(state: DetalheDoLancamentoUiState): String = when {
        state.semInstrumentos -> "Nenhum instrumento cadastrado. Cadastre um banco para editar."
        state.ehTransferencia -> "Escolha o instrumento de origem"
        else -> "Escolha o instrumento deste lançamento"
    }

    fun lidaEm(lidoEm: Instant, agora: Instant, zona: ZoneId): String {
        val dia = lidoEm.atZone(zona).toLocalDate()
        val hoje = agora.atZone(zona).toLocalDate()
        val hora = HORA.format(lidoEm.atZone(zona))

        return when (dia) {
            hoje -> "Lido hoje às $hora"
            hoje.minusDays(1) -> "Lido ontem às $hora"
            else -> "Lido em ${DIA_CURTO_E_HORA.format(lidoEm.atZone(zona))}"
        }
    }

    fun registradoEm(registradoEm: Instant, zona: ZoneId): String =
        "Lançado por você em ${DIA_CURTO_E_HORA.format(registradoEm.atZone(zona))}"

    fun confianca(valor: Double): String = "Confiança ${(valor * 100).roundToInt()}%"

    fun textoInterpretadoDisponivel(texto: String?): String = texto?.takeIf { it.isNotBlank() } ?: TEXTO_INDISPONIVEL

    fun tituloDoPar(entrada: Boolean?, rotuloDoVinculo: String): String {
        val instituicao = rotuloDoVinculo.substringBefore(" · ")

        return when (entrada) {
            true -> "Entrada em $instituicao"
            false -> "Saída de $instituicao"
            null -> "Par da transferência"
        }
    }

    fun horarioDoPar(dataHora: Instant, zona: ZoneId): String = HORA.format(dataHora.atZone(zona))

    fun valorDoPar(valorCentavos: Long): String = Valores.formatar(valorCentavos)

    fun mensagem(mensagem: MensagemDoDetalhe): String = when (mensagem) {
        MensagemDoDetalhe.INSTRUMENTO_PARA_TRANSFERENCIA -> "Escolha o instrumento antes de marcar como transferência."
        MensagemDoDetalhe.DESTINO_OBRIGATORIO -> "Escolha uma conta de destino para a transferência."
        MensagemDoDetalhe.MESMA_CONTA -> "Origem e destino precisam ser contas diferentes."
        MensagemDoDetalhe.PAR_AMBIGUO -> "Há mais de um lançamento candidato nessa conta."
        MensagemDoDetalhe.PAR_NAO_RESOLVIDO -> "Um lançamento que combina com este ainda está sem instrumento."
        MensagemDoDetalhe.DIRECAO_DESCONHECIDA ->
            "Este par antigo não tem a direção registrada. Desfazer poderia inverter despesas e receitas."
        MensagemDoDetalhe.PAR_INCONSISTENTE -> "As direções deste par estão inconsistentes. Não é possível desfazer."
        MensagemDoDetalhe.FALHA_AO_SALVAR -> "Não foi possível salvar. Tente de novo."
    }

    fun nomeDaInstituicao(instituicao: InstituicaoFinanceira?): String = when (instituicao) {
        InstituicaoFinanceira.NUBANK -> "Nubank"
        InstituicaoFinanceira.INTER -> "Inter"
        InstituicaoFinanceira.ITAU -> "Itaú"
        InstituicaoFinanceira.MERCADO_PAGO -> "Mercado Pago"
        InstituicaoFinanceira.CAJU -> "Caju"
        InstituicaoFinanceira.OUTRA -> "Outra"
        null -> "Origem não informada"
    }

    fun letrasDaInstituicao(instituicao: InstituicaoFinanceira?): String = when (instituicao) {
        InstituicaoFinanceira.NUBANK -> "N"
        InstituicaoFinanceira.INTER -> "i"
        InstituicaoFinanceira.ITAU -> "it"
        InstituicaoFinanceira.MERCADO_PAGO -> "mp"
        InstituicaoFinanceira.CAJU -> "c"
        InstituicaoFinanceira.OUTRA, null -> "?"
    }

    private const val TEXTO_INDISPONIVEL = "Texto indisponível neste aparelho"
}
