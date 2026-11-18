package com.pingo.core.data.voucher

import com.pingo.core.data.TransacaoLocal
import com.pingo.core.data.mapper.toModel
import com.pingo.core.database.dao.InstrumentoDao
import com.pingo.core.database.dao.LancamentoDao
import com.pingo.core.database.dao.ReferenciaSaldoVoucherDao
import com.pingo.core.database.entity.LancamentoEntity
import com.pingo.core.database.entity.ReferenciaSaldoVoucherEntity
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.model.TipoLancamento
import com.pingo.core.model.voucher.AjusteSaldoCalculado
import com.pingo.core.model.voucher.CalculadoraDeSaldoVoucher
import com.pingo.core.model.voucher.TipoReferenciaSaldoVoucher
import java.time.Clock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

sealed interface ValorAjusteSaldoVoucher {
    data class SaldoAlvo(val centavos: Long) : ValorAjusteSaldoVoucher

    data class Delta(val centavos: Long) : ValorAjusteSaldoVoucher
}

sealed interface ResultadoAjusteVoucher {
    data class Aplicado(
        val instrumentoPublicId: String,
        val ajuste: AjusteSaldoCalculado,
        val lancamentoPublicId: String,
        val referenciaPublicId: String,
    ) : ResultadoAjusteVoucher

    data object NaoEncontrado : ResultadoAjusteVoucher

    data object ValorInvalido : ResultadoAjusteVoucher
}

@Singleton
class AjustarSaldoVoucherUseCase @Inject constructor(
    private val transacaoLocal: TransacaoLocal,
    private val instrumentoDao: InstrumentoDao,
    private val referenciaDao: ReferenciaSaldoVoucherDao,
    private val lancamentoDao: LancamentoDao,
    private val clock: Clock,
) {
    suspend operator fun invoke(
        instrumentoPublicId: String,
        entrada: ValorAjusteSaldoVoucher,
        observacao: String? = null,
    ): ResultadoAjusteVoucher = transacaoLocal.executar {
        val entidade = instrumentoDao.buscarAtivo(instrumentoPublicId)
            ?.takeIf { it.tipo == TipoInstrumento.VOUCHER }
            ?: return@executar ResultadoAjusteVoucher.NaoEncontrado
        val instrumento = entidade.toModel()
        val referencias = referenciaDao.listarPorInstrumento(instrumentoPublicId).map { it.toModel() }
        val lancamentos = lancamentoDao.listarPorInstrumento(instrumentoPublicId).map { it.toModel() }
        val agora = clock.instant()
        val saldoAlvo = when (entrada) {
            is ValorAjusteSaldoVoucher.SaldoAlvo -> entrada.centavos
            is ValorAjusteSaldoVoucher.Delta ->
                CalculadoraDeSaldoVoucher.calcularSaldoAteCentavos(instrumento, referencias, lancamentos, agora) + entrada.centavos
        }
        if (saldoAlvo < 0L) {
            return@executar ResultadoAjusteVoucher.ValorInvalido
        }
        val ajuste = CalculadoraDeSaldoVoucher.calcularAjuste(instrumento, referencias, lancamentos, saldoAlvo, agora)
        val lancamentoId = UUID.randomUUID().toString()
        val referenciaId = UUID.randomUUID().toString()
        val observacaoLimpa = observacao?.trim()?.takeIf { it.isNotEmpty() }
        persistirAjuste(instrumentoPublicId, instrumento.beneficio, ajuste, lancamentoId, referenciaId, observacaoLimpa, agora)
        ResultadoAjusteVoucher.Aplicado(
            instrumentoPublicId = instrumentoPublicId,
            ajuste = ajuste,
            lancamentoPublicId = lancamentoId,
            referenciaPublicId = referenciaId,
        )
    }

    private suspend fun persistirAjuste(
        instrumentoPublicId: String,
        beneficio: com.pingo.core.model.BeneficioVoucher?,
        ajuste: AjusteSaldoCalculado,
        lancamentoId: String,
        referenciaId: String,
        observacaoLimpa: String?,
        agora: java.time.Instant,
    ) {
        lancamentoDao.upsert(
            novaEntidadeAjuste(instrumentoPublicId, beneficio, ajuste, lancamentoId, observacaoLimpa, agora),
        )
        referenciaDao.upsert(
            ReferenciaSaldoVoucherEntity(
                publicId = referenciaId,
                instrumentoPublicId = instrumentoPublicId,
                tipoReferencia = TipoReferenciaSaldoVoucher.AJUSTE,
                saldoCentavos = ajuste.saldoAlvoCentavos,
                deltaCentavos = ajuste.deltaCentavos,
                instanteReferencia = agora,
                lancamentoPublicId = lancamentoId,
                criadoEm = agora,
                atualizadoEm = agora,
                excluidoEm = null,
            ),
        )
    }

    private fun novaEntidadeAjuste(
        instrumentoPublicId: String,
        beneficio: com.pingo.core.model.BeneficioVoucher?,
        ajuste: AjusteSaldoCalculado,
        lancamentoId: String,
        observacaoLimpa: String?,
        agora: java.time.Instant,
    ): LancamentoEntity = LancamentoEntity(
        publicId = lancamentoId,
        instrumentoPublicId = instrumentoPublicId,
        categoriaPublicId = null,
        faturaPublicId = null,
        assinaturaPublicId = null,
        compraPaiPublicId = null,
        estornoDePublicId = null,
        transferenciaId = null,
        valorCentavos = ajuste.valorLancamentoCentavos,
        dataHora = agora,
        tipo = TipoLancamento.AJUSTE,
        tipoSemTransferencia = null,
        estabelecimento = ESTABELECIMENTO_AJUSTE,
        estabelecimentoNormalizado = ESTABELECIMENTO_AJUSTE_NORMALIZADO,
        beneficio = beneficio,
        saldoObservadoCentavos = ajuste.saldoAlvoCentavos,
        status = StatusLancamento.CONFIRMADO,
        origem = OrigemLancamento.MANUAL,
        instituicaoOrigem = null,
        confianca = 1.0,
        motivoPendencia = null,
        hashDuplicata = null,
        observacao = observacaoLimpa,
        parcelaNumero = 1,
        totalParcelas = 1,
        valorTotalCompraCentavos = null,
        criadoEm = agora,
        atualizadoEm = agora,
        excluidoEm = null,
        textoBruto = null,
    )

    private companion object {
        const val ESTABELECIMENTO_AJUSTE = "Ajuste de saldo"
        const val ESTABELECIMENTO_AJUSTE_NORMALIZADO = "AJUSTE DE SALDO"
    }
}
