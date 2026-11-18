package com.pingo.feature.accounts.impl

import com.pingo.core.data.voucher.ValorAjusteSaldoVoucher
import com.pingo.core.designsystem.component.PingoInstitution
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.Instrumento
import com.pingo.core.model.StatusFatura
import com.pingo.core.model.diaDoMesInformado
import com.pingo.core.model.reaisParaCentavos
import com.pingo.core.model.voucher.UltimaRecargaVoucher
import com.pingo.core.ui.Valores
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

internal object FormatacaoFinanceiraContas {
    private val LOCALE_PT_BR: Locale = Locale.forLanguageTag("pt-BR")
    private val FORMATO_DIA_MES: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM", LOCALE_PT_BR)
    private val FORMATO_MES_COMPLETO: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM", LOCALE_PT_BR)
    private val FORMATO_MES_ANO_CURTO: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM/yyyy", LOCALE_PT_BR)
    private val FORMATO_DATA_CURTA: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM", LOCALE_PT_BR)
    private val FORMATO_HORA: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", LOCALE_PT_BR)

    fun InstituicaoFinanceira.paraDesignSystem(): PingoInstitution = when (this) {
        InstituicaoFinanceira.NUBANK -> PingoInstitution.Nubank
        InstituicaoFinanceira.INTER -> PingoInstitution.Inter
        InstituicaoFinanceira.ITAU -> PingoInstitution.Itau
        InstituicaoFinanceira.MERCADO_PAGO -> PingoInstitution.MercadoPago
        InstituicaoFinanceira.CAJU -> PingoInstitution.Caju
        InstituicaoFinanceira.OUTRA -> PingoInstitution.Outra
    }

    fun formatarDiaMes(data: LocalDate): String = FORMATO_DIA_MES.format(data)

    fun formatarMesMaiusculo(competencia: LocalDate): String = FORMATO_MES_COMPLETO.format(competencia).uppercase(LOCALE_PT_BR)

    fun formatarMesAnoCurto(competencia: LocalDate): String =
        FORMATO_MES_ANO_CURTO.format(competencia).replace(".", "").replaceFirstChar { primeiro ->
            if (primeiro.isLowerCase()) primeiro.titlecase(LOCALE_PT_BR) else primeiro.toString()
        }

    fun formatarDataHoraCurta(instante: Instant, zona: ZoneId): String {
        val zoned = instante.atZone(zona)
        val data = FORMATO_DATA_CURTA.format(zoned).replace(".", "").lowercase(LOCALE_PT_BR)
        val hora = FORMATO_HORA.format(zoned)
        return "$data · $hora"
    }

    fun formatarContagemVencimento(status: StatusFatura, diasParaVencimento: Long, vencida: Boolean): String = when {
        status == StatusFatura.PAGA -> "Paga"
        vencida || diasParaVencimento < 0L -> {
            val diasAtraso = -diasParaVencimento
            if (diasAtraso == 1L) "Vencida há 1 dia" else "Vencida há $diasAtraso dias"
        }
        diasParaVencimento == 0L -> "Vence hoje"
        diasParaVencimento == 1L -> "Vence em 1 dia"
        else -> "Vence em $diasParaVencimento dias"
    }

    fun formatarPeriodoCiclo(status: StatusFatura, dataFechamento: LocalDate, dataVencimento: LocalDate): String =
        if (status == StatusFatura.ABERTA) {
            "Fecha dia ${dataFechamento.dayOfMonth} · vence dia ${dataVencimento.dayOfMonth}"
        } else {
            "Fechou dia ${dataFechamento.dayOfMonth} · pagar até ${formatarDiaMes(dataVencimento)}"
        }

    fun calcularProgressoCiclo(dataFechamento: LocalDate, hoje: LocalDate): Float {
        val inicio = dataFechamento.minusMonths(1)
        val totalDias = ChronoUnit.DAYS.between(inicio, dataFechamento).coerceAtLeast(1L)
        val decorridos = ChronoUnit.DAYS.between(inicio, hoje).coerceIn(0L, totalDias)
        return decorridos.toFloat() / totalDias.toFloat()
    }

    fun formatarResumoRecarga(instrumento: Instrumento, ultimaRecarga: UltimaRecargaVoucher?): String? {
        if (ultimaRecarga != null) {
            val data = formatarDiaMes(ultimaRecarga.data)
            val valor = Valores.formatar(ultimaRecarga.valorCentavos)
            return if (ultimaRecarga.prevista) {
                "Recarga prevista $data · $valor"
            } else {
                "Última recarga $data · $valor"
            }
        }
        val dia = instrumento.diaRecarga
        val valor = instrumento.valorRecargaCentavos
        if (diaDoMesInformado(dia) && valor != null && valor > 0L) {
            return "Próxima recarga dia $dia · ${Valores.formatar(valor)}"
        }
        return null
    }

    fun parsearValorAjuste(modo: ModoAjusteVoucher, texto: String): ValorAjusteSaldoVoucher? {
        val limpo = texto.trim()
        if (limpo.isEmpty()) {
            return null
        }
        return when (modo) {
            ModoAjusteVoucher.SALDO_ALVO ->
                reaisParaCentavos(limpo)?.let { ValorAjusteSaldoVoucher.SaldoAlvo(it) }
            ModoAjusteVoucher.DELTA -> parsearDelta(limpo)
        }
    }

    private fun parsearDelta(limpo: String): ValorAjusteSaldoVoucher.Delta? {
        val negativo = limpo.startsWith("-")
        val semSinal = limpo.removePrefix("-").removePrefix("+").trim()
        val magnitude = reaisParaCentavos(semSinal) ?: return null
        val centavos = if (negativo) -magnitude else magnitude
        return ValorAjusteSaldoVoucher.Delta(centavos)
    }
}
