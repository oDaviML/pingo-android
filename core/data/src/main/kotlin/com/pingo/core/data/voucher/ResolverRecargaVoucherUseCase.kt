package com.pingo.core.data.voucher

import com.pingo.core.data.TransacaoLocal
import com.pingo.core.data.mapper.toModel
import com.pingo.core.data.reconciliacao.ReconciliadorFinanceiro
import com.pingo.core.database.dao.CategoriaDao
import com.pingo.core.database.dao.InstrumentoDao
import com.pingo.core.database.dao.LancamentoDao
import com.pingo.core.database.dao.RecargaProgramadaVoucherDao
import com.pingo.core.database.entity.LancamentoEntity
import com.pingo.core.model.BeneficioVoucher
import com.pingo.core.model.Instrumento
import com.pingo.core.model.MotivoPendencia
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoCategoria
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.model.voucher.CandidatosRevisaoVoucher
import com.pingo.core.model.voucher.DecisaoRecargaVoucher
import com.pingo.core.model.voucher.MotivoRevisaoRecarga
import com.pingo.core.model.voucher.RecargaProgramada
import com.pingo.core.model.voucher.ResultadoResolucaoRecarga
import com.pingo.core.model.voucher.StatusRecargaProgramada
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ResolverRecargaVoucherUseCase @Inject constructor(
    private val transacaoLocal: TransacaoLocal,
    private val lancamentoDao: LancamentoDao,
    private val instrumentoDao: InstrumentoDao,
    private val recargaDao: RecargaProgramadaVoucherDao,
    private val categoriaDao: CategoriaDao,
    private val reconciliador: ReconciliadorFinanceiro,
    private val clock: Clock,
) {
    suspend fun listarCandidatos(lancamentoPublicId: String): CandidatosRevisaoVoucher? {
        val entidade = lancamentoDao.buscar(lancamentoPublicId)?.takeIf { it.excluidoEm == null } ?: return null
        val motivo = motivoRevisaoDe(entidade.motivoPendencia) ?: return null
        val lancamento = entidade.toModel()
        val vouchersAtivos = instrumentoDao.listarAtivosPorTipo(TipoInstrumento.VOUCHER).map { it.toModel() }
        val elegiveis = if (motivo == MotivoRevisaoRecarga.DIVERGENCIA_RECARGA && lancamento.beneficio != null) {
            vouchersAtivos.filter { it.beneficio == lancamento.beneficio }
        } else {
            vouchersAtivos
        }
        val candidatos = listarOcorrenciasSubstituiveis(elegiveis)
        return CandidatosRevisaoVoucher(
            lancamento = lancamento,
            motivo = motivo,
            vouchersElegiveis = elegiveis,
            ocorrenciasCandidatas = candidatos,
        )
    }

    suspend fun resolver(
        lancamentoPublicId: String,
        instrumentoEscolhidoPublicId: String,
        decisao: DecisaoRecargaVoucher,
    ): ResultadoResolucaoRecarga = transacaoLocal.executar {
        val entidade = lancamentoDao.buscar(lancamentoPublicId)?.takeIf { it.excluidoEm == null }
            ?: return@executar ResultadoResolucaoRecarga.NaoEncontrado
        if (entidade.status == StatusLancamento.CONFIRMADO && entidade.motivoPendencia == null) {
            return@executar ResultadoResolucaoRecarga.Resolvido(entidade.toModel())
        }
        val instrumento = instrumentoDao.buscarAtivo(instrumentoEscolhidoPublicId)
            ?.takeIf { it.tipo == TipoInstrumento.VOUCHER }
            ?: return@executar ResultadoResolucaoRecarga.Invalido
        val agora = clock.instant()
        if (decisao is DecisaoRecargaVoucher.SubstituirPrevisao) {
            val substituida = aplicarSubstituicao(decisao.ocorrenciaPublicId, entidade, instrumento.publicId, instrumento.beneficio, agora)
            if (!substituida) {
                return@executar ResultadoResolucaoRecarga.Invalido
            }
        }
        val atualizado = entidade.copy(
            instrumentoPublicId = instrumento.publicId,
            beneficio = instrumento.beneficio ?: entidade.beneficio,
            categoriaPublicId = resolverCategoriaBeneficioSeNecessario(entidade.categoriaPublicId),
            status = StatusLancamento.CONFIRMADO,
            motivoPendencia = null,
            atualizadoEm = agora,
        )
        lancamentoDao.upsert(atualizado)
        reconciliador.aoAlterarLancamentos(listOf(entidade.toModel() to atualizado.toModel()))
        ResultadoResolucaoRecarga.Resolvido(atualizado.toModel())
    }

    private suspend fun aplicarSubstituicao(
        ocorrenciaPublicId: String,
        lancamento: LancamentoEntity,
        instrumentoPublicId: String,
        beneficioInstrumento: BeneficioVoucher?,
        agora: Instant,
    ): Boolean {
        val ocorrencia = recargaDao.buscar(ocorrenciaPublicId)
            ?.takeIf { it.excluidoEm == null && it.instrumentoPublicId == instrumentoPublicId }
        val jaSubstituidaParaEste = ocorrencia?.status == StatusRecargaProgramada.SUBSTITUIDA &&
            ocorrencia.lancamentoPublicId == lancamento.publicId
        val podeSubstituir = ocorrencia?.status == StatusRecargaProgramada.PENDENTE ||
            ocorrencia?.status == StatusRecargaProgramada.EFETIVADA
        if (ocorrencia == null || (!jaSubstituidaParaEste && !podeSubstituir)) {
            return false
        }
        if (podeSubstituir) {
            excluirPrevisaoEfetivadaAnterior(ocorrencia, lancamento.publicId, agora)
            recargaDao.upsert(
                ocorrencia.copy(
                    beneficio = beneficioInstrumento ?: ocorrencia.beneficio,
                    status = StatusRecargaProgramada.SUBSTITUIDA,
                    lancamentoPublicId = lancamento.publicId,
                    atualizadoEm = agora,
                ),
            )
        }
        return true
    }

    private suspend fun excluirPrevisaoEfetivadaAnterior(
        ocorrencia: com.pingo.core.database.entity.RecargaProgramadaVoucherEntity,
        lancamentoAtualId: String,
        agora: Instant,
    ) {
        val previsaoLancId = ocorrencia.lancamentoPublicId ?: return
        if (ocorrencia.status != StatusRecargaProgramada.EFETIVADA || previsaoLancId == lancamentoAtualId) {
            return
        }
        val lancPrevisao = lancamentoDao.buscar(previsaoLancId)
        if (lancPrevisao != null && lancPrevisao.excluidoEm == null && lancPrevisao.origem == OrigemLancamento.MANUAL) {
            lancamentoDao.marcarExcluido(previsaoLancId, agora)
        }
    }

    private suspend fun listarOcorrenciasSubstituiveis(vouchers: List<Instrumento>): List<RecargaProgramada> =
        vouchers.flatMap { voucher -> listarSubstituiveisDoVoucher(voucher) }.sortedByDescending { it.dataPrevista }

    private suspend fun listarSubstituiveisDoVoucher(voucher: Instrumento): List<RecargaProgramada> {
        val padrao = voucher.beneficio ?: BeneficioVoucher.VA
        return recargaDao.listarPorInstrumento(voucher.publicId).mapNotNull { rec ->
            when {
                rec.status == StatusRecargaProgramada.PENDENTE -> rec.toModel(padrao)
                rec.status == StatusRecargaProgramada.EFETIVADA && previsaoManualAtiva(rec.lancamentoPublicId) -> rec.toModel(padrao)
                else -> null
            }
        }
    }

    private suspend fun previsaoManualAtiva(lancamentoPublicId: String?): Boolean {
        val lancPrev = lancamentoPublicId?.let { lancamentoDao.buscar(it) } ?: return false
        return lancPrev.excluidoEm == null && lancPrev.origem == OrigemLancamento.MANUAL
    }

    private suspend fun resolverCategoriaBeneficioSeNecessario(atualId: String?): String? {
        val receitas = categoriaDao.listarAtivas().filter { it.tipo == TipoCategoria.RECEITA && !it.arquivada }
        val beneficioId = receitas.firstOrNull { it.nome.equals(NOME_CATEGORIA_BENEFICIO, ignoreCase = true) }?.publicId
        val outrosId = receitas.firstOrNull { it.nome.equals(NOME_CATEGORIA_OUTROS, ignoreCase = true) }?.publicId
        return if (atualId == null || atualId == outrosId) beneficioId ?: atualId else atualId
    }

    private fun motivoRevisaoDe(motivo: String?): MotivoRevisaoRecarga? = when (motivo) {
        MotivoPendencia.BENEFICIO_AMBIGUO.name -> MotivoRevisaoRecarga.BENEFICIO_AMBIGUO
        MotivoPendencia.DIVERGENCIA_RECARGA.name -> MotivoRevisaoRecarga.DIVERGENCIA_RECARGA
        else -> null
    }

    private companion object {
        const val NOME_CATEGORIA_BENEFICIO = "Benefício"
        const val NOME_CATEGORIA_OUTROS = "Outros"
    }
}
