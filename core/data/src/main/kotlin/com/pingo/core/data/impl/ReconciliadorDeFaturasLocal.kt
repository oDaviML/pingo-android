package com.pingo.core.data.impl

import com.pingo.core.data.mapper.toModel
import com.pingo.core.data.mapper.toVigencia
import com.pingo.core.database.dao.ConfiguracaoCartaoDao
import com.pingo.core.database.dao.FaturaDao
import com.pingo.core.database.dao.InstrumentoDao
import com.pingo.core.database.dao.LancamentoDao
import com.pingo.core.database.entity.FaturaEntity
import com.pingo.core.database.entity.LancamentoEntity
import com.pingo.core.model.Instrumento
import com.pingo.core.model.Lancamento
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.model.fatura.CalendarioDeFatura
import com.pingo.core.model.fatura.CicloDeFatura
import com.pingo.core.model.fatura.ResultadoCalculoCiclo
import com.pingo.core.model.fatura.ResultadoResolucaoFatura
import com.pingo.core.model.fatura.VigenciaDeCartao
import java.time.Clock
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReconciliadorDeFaturasLocal @Inject constructor(
    private val instrumentoDao: InstrumentoDao,
    private val configuracaoCartaoDao: ConfiguracaoCartaoDao,
    private val faturaDao: FaturaDao,
    private val lancamentoDao: LancamentoDao,
    private val clock: Clock,
) {
    suspend fun reconciliarAlteracoes(pares: List<Pair<Lancamento?, Lancamento?>>) {
        val faturasAfetadas = linkedSetOf<String>()
        for ((antes, depois) in pares) {
            antes?.faturaPublicId?.let { faturasAfetadas.add(it) }
            depois?.faturaPublicId?.let { faturasAfetadas.add(it) }
            if (depois != null) {
                val novaFaturaId = reconciliarLancamento(depois)
                if (novaFaturaId != null) {
                    faturasAfetadas.add(novaFaturaId)
                }
            }
        }
        recalcularTotais(faturasAfetadas)
    }

    suspend fun reconciliarCartaoCompleto(instrumentoPublicId: String) {
        val lancamentos = lancamentoDao.listarPorInstrumento(instrumentoPublicId).map { it.toModel() }
        reconciliarAlteracoes(lancamentos.map { it to it })
    }

    suspend fun recalcularTotais(faturasIds: Collection<String>) {
        if (faturasIds.isEmpty()) {
            return
        }
        val agora = clock.instant()
        for (faturaId in faturasIds) {
            val fatura = faturaDao.buscar(faturaId) ?: continue
            val itens = lancamentoDao.listarPorFatura(faturaId).map { it.toModel() }
            val novoTotal = CalendarioDeFatura.calcularTotalFaturaCentavos(itens, faturaId)
            if (fatura.totalCentavos != novoTotal) {
                faturaDao.upsert(fatura.copy(totalCentavos = novoTotal, atualizadoEm = agora))
            }
        }
    }

    private suspend fun reconciliarLancamento(lancamento: Lancamento): String? {
        val entidadeAtual = lancamentoDao.buscar(lancamento.publicId) ?: return null
        val modeloAtual = entidadeAtual.toModel()
        val instrumento = modeloAtual.instrumentoPublicId?.let { instrumentoDao.buscar(it)?.toModel() }
        val destinoFaturaId = if (elegivelParaFatura(modeloAtual, instrumento)) {
            obterOuCriarFatura(requireNotNull(instrumento), modeloAtual)
        } else {
            null
        }
        if (entidadeAtual.faturaPublicId != destinoFaturaId) {
            lancamentoDao.upsert(entidadeAtual.copy(faturaPublicId = destinoFaturaId, atualizadoEm = clock.instant()))
        }
        return destinoFaturaId
    }

    private fun elegivelParaFatura(lancamento: Lancamento, instrumento: Instrumento?): Boolean = instrumento != null &&
        instrumento.excluidoEm == null &&
        instrumento.tipo == TipoInstrumento.CREDITO &&
        CalendarioDeFatura.entraNaFatura(lancamento)

    private suspend fun obterOuCriarFatura(instrumento: Instrumento, lancamento: Lancamento): String? {
        val vigencias = configuracaoCartaoDao.listarPorInstrumento(instrumento.publicId).map { it.toVigencia() }
        val faturasExistentes = faturaDao.listarPorInstrumento(instrumento.publicId).map { it.toModel() }
        val resolucao = CalendarioDeFatura.resolverFaturaDaCompra(
            instrumento = instrumento,
            dataHora = lancamento.dataHora,
            vigencias = vigencias,
            faturasExistentes = faturasExistentes,
        )
        return when (resolucao) {
            ResultadoResolucaoFatura.ConfiguracaoIncompleta -> null
            is ResultadoResolucaoFatura.NovoCiclo -> criarFaturaParaCiclo(instrumento.publicId, resolucao.ciclo)
            is ResultadoResolucaoFatura.FaturaExistente -> resolverComCicloExistente(
                instrumento = instrumento,
                lancamento = lancamento,
                vigencias = vigencias,
                existente = resolucao.fatura,
                faturasExistentes = faturasExistentes,
            )
        }
    }

    private suspend fun resolverComCicloExistente(
        instrumento: Instrumento,
        lancamento: Lancamento,
        vigencias: List<VigenciaDeCartao>,
        existente: com.pingo.core.model.Fatura,
        faturasExistentes: List<com.pingo.core.model.Fatura>,
    ): String {
        val dataLocal = ZonedDateTime.ofInstant(lancamento.dataHora, CalendarioDeFatura.ZONA_SAO_PAULO).toLocalDate()
        if (dataLocal.isBefore(existente.dataFechamento)) {
            return existente.publicId
        }
        val proximaCompetencia = existente.competencia.plusMonths(1)
        val existenteProxima = faturasExistentes.firstOrNull {
            it.excluidoEm == null && it.competencia == proximaCompetencia
        }
        if (existenteProxima != null) {
            return existenteProxima.publicId
        }
        val vigencia = CalendarioDeFatura.resolverVigencia(instrumento.publicId, lancamento.dataHora, vigencias)
        val fechamentoDia = if (vigencia != null) vigencia.fechamentoDia else instrumento.fechamentoDia
        val vencimentoDia = if (vigencia != null) vigencia.vencimentoDia else instrumento.vencimentoDia
        val ciclo = CalendarioDeFatura.cicloDaCompetencia(
            competencia = proximaCompetencia,
            fechamentoDia = fechamentoDia,
            vencimentoDia = vencimentoDia,
            vigenciaPublicId = vigencia?.publicId,
        )
        return when (ciclo) {
            is ResultadoCalculoCiclo.CicloCalculado -> criarFaturaParaCiclo(instrumento.publicId, ciclo.ciclo)
            ResultadoCalculoCiclo.ConfiguracaoIncompleta -> existente.publicId
        }
    }

    private suspend fun criarFaturaParaCiclo(instrumentoPublicId: String, ciclo: CicloDeFatura): String {
        val jaExistente = faturaDao.buscarPorInstrumentoECompetencia(instrumentoPublicId, ciclo.competencia)
        if (jaExistente != null) {
            return jaExistente.publicId
        }
        val agora = clock.instant()
        val hojeLocal: LocalDate = ZonedDateTime.ofInstant(agora, CalendarioDeFatura.ZONA_SAO_PAULO).toLocalDate()
        val nova = FaturaEntity(
            publicId = UUID.randomUUID().toString(),
            instrumentoPublicId = instrumentoPublicId,
            competencia = ciclo.competencia,
            dataFechamento = ciclo.dataFechamento,
            dataVencimento = ciclo.dataVencimento,
            totalCentavos = 0L,
            totalPagoCentavos = 0L,
            status = CalendarioDeFatura.statusDoCiclo(ciclo.dataFechamento, hojeLocal),
            dataPagamento = null,
            pagamentoLancamentoPublicId = null,
            criadoEm = agora,
            atualizadoEm = agora,
            excluidoEm = null,
        )
        faturaDao.upsert(nova)
        return nova.publicId
    }
}
