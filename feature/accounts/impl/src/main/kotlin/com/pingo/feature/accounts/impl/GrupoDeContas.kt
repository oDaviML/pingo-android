package com.pingo.feature.accounts.impl

import androidx.compose.runtime.Immutable
import com.pingo.core.model.Conta
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.Instrumento
import com.pingo.core.model.StatusFatura
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.model.fatura.CalendarioDeFatura
import com.pingo.core.model.fatura.EstadoFaturaCartao
import com.pingo.core.model.fatura.ResultadoCalculoCiclo
import com.pingo.core.model.organizacao.ContaComIndicadores
import com.pingo.core.model.organizacao.IndicadorDeInstrumento
import com.pingo.core.ui.contas.instituicoesFinanceiras
import com.pingo.core.ui.contas.rotuloDeExibicao
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

@Immutable
data class GrupoDeContas(val instituicao: InstituicaoFinanceira, val contas: List<ContaDaLista>)

@Immutable
data class ContaDaLista(
    val conta: Conta,
    val instrumentos: List<Instrumento>,
    val indicadores: List<IndicadorDeInstrumento> = instrumentos.map { it.indicadorPadrao() },
)

@Immutable
data class ContaArquivadaDaLista(val conta: Conta, val instrumentos: List<Instrumento>)

internal fun agruparContas(contas: List<Conta>, instrumentos: List<Instrumento>): List<GrupoDeContas> = contas
    .filter { conta -> !conta.arquivada && conta.excluidoEm == null }
    .sortedBy { conta -> instituicoesFinanceiras.indexOf(conta.instituicao) }
    .groupBy { conta -> conta.instituicao }
    .map { (instituicao, contasDaInstituicao) ->
        GrupoDeContas(
            instituicao = instituicao,
            contas = contasDaInstituicao.sortedBy { conta -> conta.apelido }.map { conta ->
                val daConta = instrumentosDaConta(conta, instrumentos)
                ContaDaLista(conta = conta, instrumentos = daConta)
            },
        )
    }

internal fun agruparContasFinanceiro(contasAtivas: List<ContaComIndicadores>): List<GrupoDeContas> = contasAtivas
    .filter { !it.conta.arquivada && it.conta.excluidoEm == null }
    .sortedBy { instituicoesFinanceiras.indexOf(it.conta.instituicao) }
    .groupBy { it.conta.instituicao }
    .map { (instituicao, contasDaInstituicao) ->
        GrupoDeContas(
            instituicao = instituicao,
            contas = contasDaInstituicao.sortedBy { it.conta.apelido }.map { item ->
                val ordenados = item.instrumentos.sortedWith(
                    compareBy({ it.instrumento.tipo }, { it.instrumento.apelido }),
                )
                ContaDaLista(
                    conta = item.conta,
                    instrumentos = ordenados.map { it.instrumento },
                    indicadores = ordenados,
                )
            },
        )
    }

private fun instrumentosDaConta(conta: Conta, instrumentos: List<Instrumento>): List<Instrumento> = instrumentos
    .filter { instrumento ->
        instrumento.contaPublicId == conta.publicId && !instrumento.arquivado && instrumento.excluidoEm == null
    }
    .sortedWith(compareBy({ it.tipo }, { it.apelido }))

internal fun contasArquivadas(contas: List<Conta>, instrumentos: List<Instrumento>): List<ContaArquivadaDaLista> = contas
    .filter { conta -> conta.arquivada && conta.excluidoEm == null }
    .sortedWith(compareBy({ conta -> instituicoesFinanceiras.indexOf(conta.instituicao) }, { conta -> conta.apelido }))
    .map { conta ->
        ContaArquivadaDaLista(
            conta = conta,
            instrumentos = instrumentos
                .filter { instrumento -> instrumento.contaPublicId == conta.publicId && instrumento.excluidoEm == null }
                .sortedWith(compareBy({ it.tipo }, { it.apelido })),
        )
    }

internal fun contasArquivadasFinanceiro(contasArquivadas: List<ContaComIndicadores>): List<ContaArquivadaDaLista> = contasArquivadas
    .filter { it.conta.arquivada && it.conta.excluidoEm == null }
    .sortedWith(compareBy({ instituicoesFinanceiras.indexOf(it.conta.instituicao) }, { it.conta.apelido }))
    .map { item ->
        ContaArquivadaDaLista(
            conta = item.conta,
            instrumentos = item.instrumentos
                .map { it.instrumento }
                .sortedWith(compareBy({ it.tipo }, { it.apelido })),
        )
    }

internal fun Instrumento.rotuloNaListaDeContas(): String = when (tipo) {
    TipoInstrumento.VOUCHER -> listOfNotNull(beneficio?.rotuloDeExibicao() ?: "Voucher", apelido).joinToString(" · ")
    TipoInstrumento.CREDITO, TipoInstrumento.DEBITO ->
        listOfNotNull(tipo.rotuloDeExibicao(), apelido, ultimosDigitos?.let { "final $it" }).joinToString(" · ")
}

internal fun Instrumento.indicadorPadrao(agora: Instant = Instant.now()): IndicadorDeInstrumento {
    val hoje = ZonedDateTime.ofInstant(agora, CalendarioDeFatura.ZONA_SAO_PAULO).toLocalDate()
    return when (tipo) {
        TipoInstrumento.CREDITO -> IndicadorDeInstrumento.Credito(
            instrumento = this,
            estadoFatura = estadoFaturaPadrao(hoje, agora),
        )
        TipoInstrumento.DEBITO -> IndicadorDeInstrumento.Debito(
            instrumento = this,
            gastoDoMesCentavos = 0L,
            competenciaMes = YearMonth.from(hoje),
        )
        TipoInstrumento.VOUCHER -> IndicadorDeInstrumento.Voucher(
            instrumento = this,
            beneficio = beneficio,
            saldoCentavos = saldoCentavos ?: 0L,
            ultimaRecarga = null,
        )
    }
}

private fun Instrumento.estadoFaturaPadrao(hoje: LocalDate, agora: Instant): EstadoFaturaCartao =
    when (val calculo = CalendarioDeFatura.cicloParaInstante(agora, fechamentoDia, vencimentoDia)) {
        is ResultadoCalculoCiclo.CicloCalculado -> EstadoFaturaCartao.Configurada(
            faturaPublicId = null,
            competencia = calculo.ciclo.competencia,
            dataFechamento = calculo.ciclo.dataFechamento,
            dataVencimento = calculo.ciclo.dataVencimento,
            totalCentavos = 0L,
            status = StatusFatura.ABERTA,
            diasParaVencimento = ChronoUnit.DAYS.between(hoje, calculo.ciclo.dataVencimento),
            vencida = false,
        )
        ResultadoCalculoCiclo.ConfiguracaoIncompleta -> EstadoFaturaCartao.ConfiguracaoIncompleta
    }
