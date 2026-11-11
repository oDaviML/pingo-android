package com.pingo.feature.capturestatus.impl

import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.notifications.SituacaoDaCaptura
import java.time.Instant
import java.time.ZoneId

internal fun tituloDoStatus(state: CaptureStatusUiState, zona: ZoneId): String = when (state.situacao) {
    SituacaoDaCaptura.ATIVA -> "Lendo as notificações dos seus apps financeiros"
    SituacaoDaCaptura.SEM_LEITURA ->
        state.semLeituraDesde
            ?.let { desde -> "Sem leitura ${TempoRelativo.formatar(desde, state.agora, zona)}" }
            ?: "Sem leitura recente"

    SituacaoDaCaptura.PARADA -> when (state.motivo) {
        com.pingo.core.datastore.MotivoParada.PERMISSAO_REVOGADA -> "Sem acesso às notificações"
        com.pingo.core.datastore.MotivoParada.DESCONECTADO -> "O Android encerrou o serviço"
        null -> "A captura está parada"
    }
}

internal fun descricaoDoStatus(state: CaptureStatusUiState, zona: ZoneId): String = when (state.situacao) {
    SituacaoDaCaptura.ATIVA -> descricaoAtiva(state, zona)
    SituacaoDaCaptura.SEM_LEITURA -> "Compras feitas agora podem não aparecer. Confira as permissões abaixo e reconecte."
    SituacaoDaCaptura.PARADA -> when (state.motivo) {
        com.pingo.core.datastore.MotivoParada.PERMISSAO_REVOGADA -> "O Pingo está no modo manual até você permitir o acesso de novo."
        else -> "Isso costuma acontecer por economia de bateria. Toque em Reconectar."
    }
}

internal fun nomeDaInstituicao(instituicao: InstituicaoFinanceira): String = when (instituicao) {
    InstituicaoFinanceira.NUBANK -> "Nubank"
    InstituicaoFinanceira.INTER -> "Inter"
    InstituicaoFinanceira.ITAU -> "Itaú"
    InstituicaoFinanceira.MERCADO_PAGO -> "Mercado Pago"
    InstituicaoFinanceira.CAJU -> "Caju"
    InstituicaoFinanceira.OUTRA -> "Outra"
}

internal fun letrasDaInstituicao(instituicao: InstituicaoFinanceira): String = when (instituicao) {
    InstituicaoFinanceira.NUBANK -> "N"
    InstituicaoFinanceira.INTER -> "i"
    InstituicaoFinanceira.ITAU -> "it"
    InstituicaoFinanceira.MERCADO_PAGO -> "mp"
    InstituicaoFinanceira.CAJU -> "c"
    InstituicaoFinanceira.OUTRA -> "?"
}

internal fun descricaoDaPermissao(item: ItemDePermissao, concedida: Boolean): String = when (item) {
    ItemDePermissao.ACESSO_A_NOTIFICACOES -> if (concedida) {
        "Ativo — lendo notificações dos bancos"
    } else {
        "Desativado — o Pingo não consegue ler"
    }

    ItemDePermissao.NOTIFICACOES_DO_PINGO -> if (concedida) {
        "Ativadas — você recebe os avisos"
    } else {
        "Desativadas — sem avisos de revisão"
    }

    ItemDePermissao.BATERIA -> if (concedida) {
        "O Android não pausa a leitura"
    } else {
        "O Android está limitando o Pingo"
    }
}

internal fun tituloDaPermissao(item: ItemDePermissao): String = when (item) {
    ItemDePermissao.ACESSO_A_NOTIFICACOES -> "Acesso a notificações"
    ItemDePermissao.NOTIFICACOES_DO_PINGO -> "Notificações do Pingo"
    ItemDePermissao.BATERIA -> "Restrição de bateria"
}

internal fun subtituloDasRegras(state: CaptureStatusUiState, agora: Instant, zona: ZoneId): String {
    val versao = state.versaoDasRegras?.let { numero -> "Versão $numero" } ?: "Versão desconhecida"
    val atualizacao = state.regrasAtualizadasEm
        ?.let { em -> "atualizadas ${TempoRelativo.formatar(em, agora, zona)}" }
        ?: "nunca atualizadas"
    val falha = state.falhaAoAtualizarRegras?.let { motivo -> " · falha: ${motivoDaFalha(motivo)}" }.orEmpty()

    return "$versao · $atualizacao$falha"
}

internal fun motivoDaFalha(motivo: String): String = when {
    motivo == "sem_conexao" -> "sem conexão"
    motivo == "regras_incompletas" -> "resposta incompleta"
    motivo == "regras_invalidas" -> "regras inválidas"
    motivo == "http_401" -> "sessão expirada"
    motivo.startsWith("http_") -> "erro no servidor"
    else -> motivo
}

private fun descricaoAtiva(state: CaptureStatusUiState, zona: ZoneId): String {
    val ultimaLeitura = state.ultimaLeituraEm
    val instituicao = state.ultimaLeituraInstituicao
    if (ultimaLeitura == null || instituicao == null) {
        return "Nenhuma notificação lida ainda. O Pingo está pronto para os seus bancos."
    }

    return "Última leitura ${TempoRelativo.formatar(ultimaLeitura, state.agora, zona)}, do ${nomeDaInstituicao(instituicao)}."
}
