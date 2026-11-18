package com.pingo.feature.transactions.impl

import com.pingo.core.model.Categoria
import com.pingo.core.model.Conta
import com.pingo.core.model.Instrumento
import com.pingo.core.model.Lancamento
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoLancamento
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

sealed interface ResultadoExportacaoCsv {
    data class Sucesso(val conteudoCsv: String, val quantidade: Int) : ResultadoExportacaoCsv

    data class Vazio(val mensagem: String) : ResultadoExportacaoCsv
}

internal object ExportadorCsvDeLancamentos {
    const val MENSAGEM_VAZIO = "Não há lançamentos neste filtro para exportar"
    val ZONA_SAO_PAULO: ZoneId = ZoneId.of("America/Sao_Paulo")

    private val LOCALE_BR = Locale.forLanguageTag("pt-BR")
    private val FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy", LOCALE_BR)
    private val FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm", LOCALE_BR)
    private val CABECALHO = listOf(
        "Data",
        "Hora",
        "Tipo",
        "Estabelecimento",
        "Categoria",
        "Conta",
        "Instrumento",
        "Valor",
        "Status",
        "Origem",
    )

    fun exportar(
        lancamentos: List<Lancamento>,
        categoriasPorId: Map<String, Categoria>,
        instrumentosPorId: Map<String, Instrumento>,
        contasPorId: Map<String, Conta>,
        zona: ZoneId = ZONA_SAO_PAULO,
    ): ResultadoExportacaoCsv {
        if (lancamentos.isEmpty()) {
            return ResultadoExportacaoCsv.Vazio(MENSAGEM_VAZIO)
        }

        val ordenados = lancamentos.sortedByDescending { it.dataHora }
        val linhas = buildList {
            add(CABECALHO.joinToString(",") { escaparCampo(it) })
            ordenados.forEach { lancamento ->
                val instrumento = lancamento.instrumentoPublicId?.let(instrumentosPorId::get)
                val conta = instrumento?.contaPublicId?.let(contasPorId::get)
                val categoria = lancamento.categoriaPublicId?.let(categoriasPorId::get)
                val campos = montarLinha(lancamento, categoria, conta, instrumento, zona)
                add(campos.joinToString(",") { escaparCampo(it) })
            }
        }

        return ResultadoExportacaoCsv.Sucesso(
            conteudoCsv = linhas.joinToString("\r\n"),
            quantidade = ordenados.size,
        )
    }

    private fun montarLinha(
        lancamento: Lancamento,
        categoria: Categoria?,
        conta: Conta?,
        instrumento: Instrumento?,
        zona: ZoneId,
    ): List<String> {
        val dataHora = lancamento.dataHora.atZone(zona)
        return listOf(
            FORMATO_DATA.format(dataHora),
            FORMATO_HORA.format(dataHora),
            rotuloTipo(lancamento.tipo),
            protegerFormula(lancamento.estabelecimento.orEmpty()),
            protegerFormula(categoria?.nome.orEmpty()),
            protegerFormula(conta?.apelido.orEmpty()),
            protegerFormula(formatarInstrumento(instrumento)),
            formatarValor(lancamento.valorCentavos, lancamento.tipo),
            rotuloStatus(lancamento.status),
            rotuloOrigem(lancamento.origem),
        )
    }

    private fun rotuloTipo(tipo: TipoLancamento): String = when (tipo) {
        TipoLancamento.DESPESA -> "Despesa"
        TipoLancamento.RECEITA -> "Receita"
        TipoLancamento.TRANSFERENCIA -> "Transferência"
        TipoLancamento.AJUSTE -> "Ajuste"
    }

    private fun rotuloStatus(status: StatusLancamento): String = when (status) {
        StatusLancamento.CONFIRMADO -> "Confirmado"
        StatusLancamento.PENDENTE -> "Pendente"
    }

    private fun rotuloOrigem(origem: OrigemLancamento): String = when (origem) {
        OrigemLancamento.NOTIFICACAO -> "Notificação"
        OrigemLancamento.MANUAL -> "Manual"
    }

    private fun formatarInstrumento(instrumento: Instrumento?): String {
        if (instrumento == null) return ""
        val digitos = instrumento.ultimosDigitos
        return if (digitos.isNullOrBlank()) instrumento.apelido else "${instrumento.apelido} $digitos"
    }

    private fun formatarValor(valorCentavos: Long, tipo: TipoLancamento): String {
        val absoluto = abs(valorCentavos)
        val reais = absoluto / 100
        val centavos = (absoluto % 100).toString().padStart(2, '0')
        val prefixo = if (tipo == TipoLancamento.DESPESA && valorCentavos > 0) "-" else ""
        return "$prefixo$reais,$centavos"
    }

    private fun protegerFormula(valor: String): String {
        if (valor.isEmpty()) return valor
        val primeiro = valor.first()
        val perigoso = primeiro == '=' ||
            primeiro == '+' ||
            primeiro == '-' ||
            primeiro == '@' ||
            primeiro == '\t' ||
            primeiro == '\r'
        return if (perigoso) "'$valor" else valor
    }

    private fun escaparCampo(valor: String): String {
        val precisaAspas = valor.contains(',') || valor.contains('"') || valor.contains('\n') || valor.contains('\r')
        if (!precisaAspas) return valor
        return "\"${valor.replace("\"", "\"\"")}\""
    }
}
