package com.pingo.feature.review.impl

import kotlin.math.roundToInt

internal object RotulosDaRevisao {
    const val TITULO: String = "Revisão de pendentes"
    const val SUBTITULO: String = "O Pingo não teve certeza destes."
    const val CONFIRMAR_TODOS: String = "Confirmar todos"
    const val VAZIO: String = "Tudo em dia 💧"
    const val VAZIO_DETALHE: String = "Nenhum lançamento esperando revisão."
    const val TEXTO_INDISPONIVEL: String = "Texto indisponível neste aparelho"
    const val INSTRUMENTO_NECESSARIO: String = "Escolha um instrumento no detalhe para confirmar."
    const val BENEFICIO_AMBIGUO_ORIENTACAO: String =
        "Não foi possível identificar qual benefício (VR, VA ou VT) recebeu a recarga."
    const val DIVERGENCIA_RECARGA_ORIENTACAO: String =
        "Valor ou data diferente da recarga prevista. Escolha como registrar."
    const val SEM_OPCOES_DE_TRANSFERENCIA: String = "Nenhum instrumento de outra conta disponível."

    fun quantidade(total: Int): String = if (total == 1) "1 para revisar" else "$total para revisar"

    fun motivo(pendencia: PendenciaDaRevisao): String = when (pendencia.motivo) {
        MotivoDaRevisao.BAIXA_CONFIANCA -> "Baixa confiança · ${percentual(pendencia.confianca)}"
        MotivoDaRevisao.POSSIVEL_DUPLICATA -> "Possível duplicata"
        MotivoDaRevisao.TRANSFERENCIA_AMBIGUA -> "Transferência ambígua"
        MotivoDaRevisao.ESTABELECIMENTO_NOVO -> "Estabelecimento novo"
        MotivoDaRevisao.INSTRUMENTO_AMBIGUO -> "Instrumento ambíguo"
        MotivoDaRevisao.INSTRUMENTO_NAO_ENCONTRADO -> "Instrumento não encontrado"
        MotivoDaRevisao.MOEDA_ESTRANGEIRA -> "Moeda estrangeira"
        MotivoDaRevisao.DADOS_INCOMPLETOS -> "Dados incompletos"
        MotivoDaRevisao.BENEFICIO_AMBIGUO -> "Benefício ambíguo"
        MotivoDaRevisao.DIVERGENCIA_RECARGA -> "Divergência na recarga"
        MotivoDaRevisao.OUTRO -> "Para revisar"
    }

    fun orientacao(pendencia: PendenciaDaRevisao): String = when (pendencia.motivo) {
        MotivoDaRevisao.BENEFICIO_AMBIGUO -> BENEFICIO_AMBIGUO_ORIENTACAO
        MotivoDaRevisao.DIVERGENCIA_RECARGA -> DIVERGENCIA_RECARGA_ORIENTACAO
        else -> INSTRUMENTO_NECESSARIO
    }

    fun mensagem(mensagem: MensagemDaRevisao): String = when (mensagem) {
        MensagemDaRevisao.InstrumentoNecessario -> INSTRUMENTO_NECESSARIO
        MensagemDaRevisao.TransferenciaBloqueada -> "Não foi possível marcar como transferência. Corrija o instrumento no detalhe."
        MensagemDaRevisao.MesmaConta -> "Escolha um instrumento de outra conta."
        MensagemDaRevisao.ParAmbigua -> "Há mais de um lançamento compatível. Corrija no detalhe."
        MensagemDaRevisao.FalhaAoSalvar -> "Não foi possível salvar. Tente novamente."
        is MensagemDaRevisao.LoteComPendentes -> loteComPendentes(mensagem.restantes)
        is MensagemDaRevisao.VoucherPendenteManual -> voucherPendenteManual(mensagem.restantes, mensagem.apenasVouchers)
    }

    private fun voucherPendenteManual(restantes: Int, apenasVouchers: Boolean): String = when {
        restantes == 1 && apenasVouchers -> "1 recarga de voucher precisa de escolha manual antes de confirmar."
        apenasVouchers -> "$restantes recargas de voucher precisam de escolha manual antes de confirmar."
        else -> "$restantes lançamentos continuam pendentes. Recargas ambíguas ou divergentes precisam de escolha manual."
    }

    private fun loteComPendentes(restantes: Int): String = if (restantes == 1) {
        "1 lançamento continua pendente. Corrija o instrumento no detalhe."
    } else {
        "$restantes lançamentos continuam pendentes. Corrija o instrumento no detalhe."
    }

    private fun percentual(confianca: Double): String = "${(confianca.coerceIn(0.0, 1.0) * 100).roundToInt()}%"
}
