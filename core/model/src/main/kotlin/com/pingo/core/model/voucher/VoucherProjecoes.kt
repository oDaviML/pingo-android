package com.pingo.core.model.voucher

import androidx.compose.runtime.Immutable
import com.pingo.core.model.BeneficioVoucher
import com.pingo.core.model.Conta
import com.pingo.core.model.Instrumento
import com.pingo.core.model.Lancamento
import java.text.Normalizer
import java.time.Instant
import java.time.LocalDate
import java.util.Locale

@Immutable
data class UltimaRecargaVoucher(val data: LocalDate, val valorCentavos: Long, val prevista: Boolean)

@Immutable
data class HistoricoAjusteVoucher(
    val referenciaPublicId: String,
    val lancamentoPublicId: String,
    val saldoAnteriorCentavos: Long,
    val saldoAlvoCentavos: Long,
    val deltaCentavos: Long,
    val instante: Instant,
    val observacao: String? = null,
)

@Immutable
data class DetalheVoucher(
    val instrumento: Instrumento,
    val conta: Conta?,
    val saldoAtualCentavos: Long,
    val ultimaRecarga: UltimaRecargaVoucher?,
    val recargasProgramadas: List<RecargaProgramada>,
    val referencias: List<ReferenciaSaldoVoucher>,
    val historicoAjustes: List<HistoricoAjusteVoucher>,
)

sealed interface DecisaoRecargaVoucher {
    @Immutable
    data class SubstituirPrevisao(val ocorrenciaPublicId: String) : DecisaoRecargaVoucher

    @Immutable
    data object RegistrarExtra : DecisaoRecargaVoucher
}

@Immutable
data class CandidatosRevisaoVoucher(
    val lancamento: Lancamento,
    val motivo: MotivoRevisaoRecarga,
    val vouchersElegiveis: List<Instrumento>,
    val ocorrenciasCandidatas: List<RecargaProgramada>,
)

sealed interface ResultadoResolucaoRecarga {
    @Immutable
    data class Resolvido(val lancamento: Lancamento) : ResultadoResolucaoRecarga

    @Immutable
    data object NaoEncontrado : ResultadoResolucaoRecarga

    @Immutable
    data object Invalido : ResultadoResolucaoRecarga
}

object BeneficioCajuMapper {
    private val MARCAS_DIACRITICAS = Regex("\\p{M}+")
    private val SEPARADORES = Regex("[-_\\s\\p{Zs}]+")

    fun mapear(bruto: String?): BeneficioVoucher? {
        val normalizado = normalizar(bruto) ?: return null
        return when (normalizado) {
            "VR", "REFEICAO", "AUXILIO REFEICAO", "VALE REFEICAO" -> BeneficioVoucher.VR
            "VA", "ALIMENTACAO", "AUXILIO ALIMENTACAO", "VALE ALIMENTACAO" -> BeneficioVoucher.VA
            "VT", "MOBILIDADE", "AUXILIO MOBILIDADE", "TRANSPORTE", "AUXILIO TRANSPORTE", "VALE TRANSPORTE" -> BeneficioVoucher.VT
            else -> null
        }
    }

    private fun normalizar(bruto: String?): String? {
        if (bruto == null) {
            return null
        }
        val decomposto = Normalizer.normalize(bruto, Normalizer.Form.NFD)
        return MARCAS_DIACRITICAS.replace(decomposto, "")
            .replace(SEPARADORES, " ")
            .trim()
            .uppercase(Locale.ROOT)
            .takeIf { it.isNotEmpty() }
    }
}
