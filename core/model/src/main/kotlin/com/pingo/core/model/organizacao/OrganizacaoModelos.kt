package com.pingo.core.model.organizacao

import androidx.compose.runtime.Immutable
import com.pingo.core.model.BeneficioVoucher
import com.pingo.core.model.Conta
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.Instrumento
import com.pingo.core.model.StatusFatura
import com.pingo.core.model.fatura.EstadoFaturaCartao
import com.pingo.core.model.voucher.UltimaRecargaVoucher
import java.time.LocalDate
import java.time.YearMonth

sealed interface IndicadorDeInstrumento {
    val instrumento: Instrumento

    @Immutable
    data class Credito(override val instrumento: Instrumento, val estadoFatura: EstadoFaturaCartao) : IndicadorDeInstrumento

    @Immutable
    data class Debito(override val instrumento: Instrumento, val gastoDoMesCentavos: Long, val competenciaMes: YearMonth) :
        IndicadorDeInstrumento

    @Immutable
    data class Voucher(
        override val instrumento: Instrumento,
        val beneficio: BeneficioVoucher?,
        val saldoCentavos: Long,
        val ultimaRecarga: UltimaRecargaVoucher?,
    ) : IndicadorDeInstrumento
}

@Immutable
data class ContaComIndicadores(val conta: Conta, val instrumentos: List<IndicadorDeInstrumento>)

@Immutable
data class ResumoContasFinanceiro(val contasAtivas: List<ContaComIndicadores>, val contasArquivadas: List<ContaComIndicadores>)

sealed interface EstadoBlocoFinanceiro<out T> {
    @Immutable
    data object Carregando : EstadoBlocoFinanceiro<Nothing>

    @Immutable
    data class Sucesso<T>(val dados: T) : EstadoBlocoFinanceiro<T>

    @Immutable
    data object Erro : EstadoBlocoFinanceiro<Nothing>
}

@Immutable
data class FaturaAbertaInicio(
    val instrumentoPublicId: String,
    val contaPublicId: String,
    val instituicao: InstituicaoFinanceira,
    val apelidoCartao: String,
    val ultimosDigitos: String?,
    val faturaPublicId: String?,
    val competencia: LocalDate,
    val dataFechamento: LocalDate,
    val dataVencimento: LocalDate,
    val valorCentavos: Long,
    val diasParaVencimento: Long,
    val status: StatusFatura,
)

@Immutable
data class SaldoVoucherInicio(
    val instrumentoPublicId: String,
    val contaPublicId: String,
    val instituicao: InstituicaoFinanceira,
    val apelido: String,
    val beneficio: BeneficioVoucher,
    val saldoCentavos: Long,
    val ultimaRecarga: UltimaRecargaVoucher?,
)
