package com.pingo.core.data.voucher

import com.pingo.core.data.TransacaoLocal
import com.pingo.core.data.mapper.toModel
import com.pingo.core.database.dao.CategoriaDao
import com.pingo.core.database.dao.InstrumentoDao
import com.pingo.core.database.dao.LancamentoDao
import com.pingo.core.database.dao.RecargaProgramadaVoucherDao
import com.pingo.core.database.entity.LancamentoEntity
import com.pingo.core.database.entity.RecargaProgramadaVoucherEntity
import com.pingo.core.model.BeneficioVoucher
import com.pingo.core.model.Instrumento
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoCategoria
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.model.TipoLancamento
import com.pingo.core.model.assinatura.NormalizadorDeAssinatura
import com.pingo.core.model.voucher.ReconciliadorDeRecarga
import com.pingo.core.model.voucher.StatusRecargaProgramada
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MaterializarRecargasVencidasUseCase @Inject constructor(
    private val transacaoLocal: TransacaoLocal,
    private val instrumentoDao: InstrumentoDao,
    private val recargaDao: RecargaProgramadaVoucherDao,
    private val lancamentoDao: LancamentoDao,
    private val categoriaDao: CategoriaDao,
    private val clock: Clock,
) {
    private val mutex = Mutex()

    suspend operator fun invoke(): Int = mutex.withLock {
        transacaoLocal.executar {
            val agora = clock.instant()
            val hoje = ZonedDateTime.ofInstant(agora, ReconciliadorDeRecarga.ZONA_SAO_PAULO).toLocalDate()
            val vouchers = instrumentoDao.listarAtivosPorTipo(TipoInstrumento.VOUCHER).map { it.toModel() }
            if (vouchers.isEmpty()) {
                return@executar 0
            }
            val categoriaBeneficioId = categoriaBeneficioReceitaId()
            var materializadas = 0
            for (voucher in vouchers) {
                materializadas += materializarDoInstrumento(voucher, hoje, agora, categoriaBeneficioId)
            }
            materializadas
        }
    }

    private suspend fun materializarDoInstrumento(
        voucher: Instrumento,
        hoje: LocalDate,
        agora: Instant,
        categoriaBeneficioId: String?,
    ): Int {
        val beneficioPadrao = voucher.beneficio ?: return 0
        val existentesBanco = recargaDao.listarPorInstrumento(voucher.publicId)
        var contagem = 0
        for (pendente in existentesBanco) {
            if (pendente.status == StatusRecargaProgramada.PENDENTE &&
                !pendente.dataPrevista.isAfter(hoje) &&
                pendente.lancamentoPublicId == null
            ) {
                efetivarExistente(voucher, pendente, beneficioPadrao, categoriaBeneficioId, agora)
                contagem++
            }
        }
        val atualizadas = recargaDao.listarPorInstrumento(voucher.publicId).map { it.toModel(beneficioPadrao) }
        val devidas = ReconciliadorDeRecarga.gerarOcorrenciasDevidas(
            instrumento = voucher,
            desdeDataInclusiva = dataInicioDoVoucher(voucher),
            hoje = hoje,
            existentes = atualizadas,
        )
        for (ocorrencia in devidas) {
            if (recargaDao.buscarPorCompetencia(voucher.publicId, ocorrencia.competenciaMes) == null) {
                criarEEfetivar(
                    voucher = voucher,
                    competenciaMes = ocorrencia.competenciaMes,
                    dataPrevista = ocorrencia.dataPrevista,
                    valorCentavos = ocorrencia.valorPrevistoCentavos,
                    beneficio = beneficioPadrao,
                    categoriaBeneficioId = categoriaBeneficioId,
                    agora = agora,
                )
                contagem++
            }
        }
        return contagem
    }

    private suspend fun efetivarExistente(
        voucher: Instrumento,
        pendente: RecargaProgramadaVoucherEntity,
        beneficio: BeneficioVoucher,
        categoriaBeneficioId: String?,
        agora: Instant,
    ) {
        val lancId = criarLancamentoDeRecarga(
            instrumento = voucher,
            beneficio = pendente.beneficio ?: beneficio,
            dataPrevista = pendente.dataPrevista,
            valorCentavos = pendente.valorPrevistoCentavos,
            categoriaBeneficioId = categoriaBeneficioId,
            agora = agora,
        )
        recargaDao.upsert(
            pendente.copy(
                status = StatusRecargaProgramada.EFETIVADA,
                lancamentoPublicId = lancId,
                atualizadoEm = agora,
            ),
        )
    }

    private suspend fun criarEEfetivar(
        voucher: Instrumento,
        competenciaMes: LocalDate,
        dataPrevista: LocalDate,
        valorCentavos: Long,
        beneficio: BeneficioVoucher,
        categoriaBeneficioId: String?,
        agora: Instant,
    ) {
        val lancId = criarLancamentoDeRecarga(voucher, beneficio, dataPrevista, valorCentavos, categoriaBeneficioId, agora)
        recargaDao.upsert(
            RecargaProgramadaVoucherEntity(
                publicId = UUID.randomUUID().toString(),
                instrumentoPublicId = voucher.publicId,
                beneficio = beneficio,
                competenciaMes = competenciaMes,
                dataPrevista = dataPrevista,
                valorPrevistoCentavos = valorCentavos,
                status = StatusRecargaProgramada.EFETIVADA,
                lancamentoPublicId = lancId,
                criadoEm = agora,
                atualizadoEm = agora,
                excluidoEm = null,
            ),
        )
    }

    private suspend fun criarLancamentoDeRecarga(
        instrumento: Instrumento,
        beneficio: BeneficioVoucher,
        dataPrevista: LocalDate,
        valorCentavos: Long,
        categoriaBeneficioId: String?,
        agora: Instant,
    ): String {
        val lancId = UUID.randomUUID().toString()
        val dataHora = maxOf(
            dataPrevista.atStartOfDay(ReconciliadorDeRecarga.ZONA_SAO_PAULO).toInstant(),
            instrumento.criadoEm.plusSeconds(1),
        )
        val entidade = novaEntidadeRecarga(
            lancId = lancId,
            instrumentoPublicId = instrumento.publicId,
            beneficio = beneficio,
            valorCentavos = valorCentavos,
            dataHora = dataHora,
            categoriaBeneficioId = categoriaBeneficioId,
            agora = agora,
        )
        lancamentoDao.upsert(entidade)
        return lancId
    }

    private fun novaEntidadeRecarga(
        lancId: String,
        instrumentoPublicId: String,
        beneficio: BeneficioVoucher,
        valorCentavos: Long,
        dataHora: Instant,
        categoriaBeneficioId: String?,
        agora: Instant,
    ): LancamentoEntity {
        val estabelecimento = "$PREFIXO_ESTABELECIMENTO ${beneficio.name}"
        return LancamentoEntity(
            publicId = lancId,
            instrumentoPublicId = instrumentoPublicId,
            categoriaPublicId = categoriaBeneficioId,
            faturaPublicId = null,
            assinaturaPublicId = null,
            compraPaiPublicId = null,
            estornoDePublicId = null,
            transferenciaId = null,
            valorCentavos = valorCentavos,
            dataHora = dataHora,
            tipo = TipoLancamento.RECEITA,
            tipoSemTransferencia = null,
            estabelecimento = estabelecimento,
            estabelecimentoNormalizado = NormalizadorDeAssinatura.normalizar(estabelecimento),
            beneficio = beneficio,
            saldoObservadoCentavos = null,
            status = StatusLancamento.CONFIRMADO,
            origem = OrigemLancamento.MANUAL,
            instituicaoOrigem = null,
            confianca = 1.0,
            motivoPendencia = null,
            hashDuplicata = null,
            observacao = null,
            parcelaNumero = 1,
            totalParcelas = 1,
            valorTotalCompraCentavos = null,
            criadoEm = agora,
            atualizadoEm = agora,
            excluidoEm = null,
            textoBruto = null,
        )
    }

    private fun dataInicioDoVoucher(voucher: Instrumento): LocalDate {
        val criadoLocal = ZonedDateTime.ofInstant(voucher.criadoEm, ReconciliadorDeRecarga.ZONA_SAO_PAULO).toLocalDate()
        return if ((voucher.saldoCentavos ?: 0L) > 0L) criadoLocal.plusDays(1) else criadoLocal
    }

    private suspend fun categoriaBeneficioReceitaId(): String? {
        val ativas = categoriaDao.listarAtivas().filter { it.tipo == TipoCategoria.RECEITA && !it.arquivada }
        return ativas.firstOrNull { it.nome.equals(NOME_CATEGORIA_BENEFICIO, ignoreCase = true) }?.publicId
            ?: ativas.firstOrNull { it.nome.equals(NOME_CATEGORIA_OUTROS, ignoreCase = true) }?.publicId
    }

    private companion object {
        const val PREFIXO_ESTABELECIMENTO = "Recarga programada"
        const val NOME_CATEGORIA_BENEFICIO = "Benefício"
        const val NOME_CATEGORIA_OUTROS = "Outros"
    }
}
