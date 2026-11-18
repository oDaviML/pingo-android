package com.pingo.core.data.impl

import com.pingo.core.data.assinatura.AgendadorDeAvisosOpcional
import com.pingo.core.database.dao.AssinaturaDao
import com.pingo.core.database.dao.CategoriaDao
import com.pingo.core.database.dao.LancamentoDao
import com.pingo.core.database.entity.AssinaturaEntity
import com.pingo.core.database.entity.LancamentoEntity
import com.pingo.core.model.Lancamento
import com.pingo.core.model.StatusAssinatura
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoCategoria
import com.pingo.core.model.TipoLancamento
import com.pingo.core.model.assinatura.DetectorDeAssinaturas
import com.pingo.core.model.assinatura.NormalizadorDeAssinatura
import java.time.Clock
import java.time.LocalDate
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReconciliadorDeAssinaturasLocal @Inject constructor(
    private val assinaturaDao: AssinaturaDao,
    private val lancamentoDao: LancamentoDao,
    private val categoriaDao: CategoriaDao,
    private val agendadorDeAvisos: AgendadorDeAvisosOpcional,
    private val clock: Clock,
) {
    suspend fun reconciliarAlteracoes(pares: List<Pair<Lancamento?, Lancamento?>>) {
        val envolveDespesa = pares.any { (antes, depois) ->
            antes?.tipo == TipoLancamento.DESPESA ||
                depois?.tipo == TipoLancamento.DESPESA ||
                antes?.assinaturaPublicId != null
        }
        if (!envolveDespesa) {
            return
        }
        val confirmadas = assinaturaDao.listarAtivas().filter { it.status == StatusAssinatura.CONFIRMADA }
        if (confirmadas.isEmpty()) {
            return
        }
        val categoriaAssinaturasId = categoriaAssinaturasId()
        val categoriaOutrosId = categoriaOutrosDespesaId()
        val elegiveis = lancamentoDao.listarElegiveisParaDeteccao()
        var houveAtualizacao = false
        for (assinatura in confirmadas) {
            if (reconciliarAssinaturaConfirmada(assinatura, elegiveis, categoriaAssinaturasId, categoriaOutrosId)) {
                houveAtualizacao = true
            }
        }
        if (houveAtualizacao) {
            agendadorDeAvisos.reconciliarTodas()
        }
    }

    private suspend fun reconciliarAssinaturaConfirmada(
        assinatura: AssinaturaEntity,
        elegiveis: List<LancamentoEntity>,
        categoriaAssinaturasId: String?,
        categoriaOutrosId: String?,
    ): Boolean {
        val agora = clock.instant()
        val correspondentes = elegiveis.filter { pertenceAAssinatura(it, assinatura) }
        for (entidade in correspondentes) {
            val precisaVincular = entidade.assinaturaPublicId != assinatura.publicId
            val deveCategorizar = categoriaAssinaturasId != null &&
                (entidade.categoriaPublicId == null || entidade.categoriaPublicId == categoriaOutrosId)
            if (precisaVincular || deveCategorizar) {
                lancamentoDao.upsert(
                    entidade.copy(
                        assinaturaPublicId = assinatura.publicId,
                        categoriaPublicId = if (deveCategorizar) categoriaAssinaturasId else entidade.categoriaPublicId,
                        atualizadoEm = agora,
                    ),
                )
            }
        }
        val desvinculados = lancamentoDao.listarPorAssinatura(assinatura.publicId)
            .filter { item ->
                item.excluidoEm != null ||
                    item.status != StatusLancamento.CONFIRMADO ||
                    item.tipo != TipoLancamento.DESPESA ||
                    !pertenceAAssinatura(item, assinatura)
            }
        for (entidade in desvinculados) {
            if (entidade.assinaturaPublicId != null) {
                lancamentoDao.upsert(entidade.copy(assinaturaPublicId = null, atualizadoEm = agora))
            }
        }
        return atualizarDatasDaAssinatura(assinatura, correspondentes, agora)
    }

    private suspend fun atualizarDatasDaAssinatura(
        assinatura: AssinaturaEntity,
        correspondentes: List<LancamentoEntity>,
        agora: java.time.Instant,
    ): Boolean {
        val ultimaCobranca: LocalDate? = correspondentes
            .maxWithOrNull(compareBy<LancamentoEntity> { it.dataHora }.thenBy { it.publicId })
            ?.let { ZonedDateTime.ofInstant(it.dataHora, DetectorDeAssinaturas.ZONA_SAO_PAULO).toLocalDate() }
            ?: if (assinatura.instrumentoPublicId == null) assinatura.ultimaCobranca else null
        val proxima = ultimaCobranca?.let { DetectorDeAssinaturas.calcularProximaCobranca(it) }
        val alterado = assinatura.ultimaCobranca != ultimaCobranca || assinatura.proximaCobrancaEstimada != proxima
        if (alterado) {
            assinaturaDao.upsert(
                assinatura.copy(
                    ultimaCobranca = ultimaCobranca,
                    proximaCobrancaEstimada = proxima,
                    atualizadoEm = agora,
                ),
            )
        }
        return alterado
    }

    private fun pertenceAAssinatura(entidade: LancamentoEntity, assinatura: AssinaturaEntity): Boolean =
        if (entidade.compraPaiPublicId != null || entidade.totalParcelas > 1) {
            false
        } else if (assinatura.instrumentoPublicId == null) {
            entidade.assinaturaPublicId == assinatura.publicId
        } else {
            val normalizado = entidade.estabelecimentoNormalizado ?: NormalizadorDeAssinatura.normalizar(entidade.estabelecimento)
            val faixaOk = assinatura.valorReferenciaCentavos <= 0L ||
                DetectorDeAssinaturas.valorNaFaixa(assinatura.valorReferenciaCentavos, entidade.valorCentavos)
            entidade.instrumentoPublicId == assinatura.instrumentoPublicId &&
                normalizado == assinatura.estabelecimentoNormalizado &&
                faixaOk
        }

    private suspend fun categoriaAssinaturasId(): String? = categoriaDao.listarAtivas()
        .firstOrNull { it.tipo == TipoCategoria.DESPESA && it.nome.equals(NOME_CATEGORIA_ASSINATURAS, ignoreCase = true) }
        ?.publicId

    private suspend fun categoriaOutrosDespesaId(): String? = categoriaDao.listarAtivas()
        .firstOrNull { it.tipo == TipoCategoria.DESPESA && it.nome.equals(NOME_CATEGORIA_OUTROS, ignoreCase = true) }
        ?.publicId

    private companion object {
        const val NOME_CATEGORIA_ASSINATURAS = "Assinaturas"
        const val NOME_CATEGORIA_OUTROS = "Outros"
    }
}
