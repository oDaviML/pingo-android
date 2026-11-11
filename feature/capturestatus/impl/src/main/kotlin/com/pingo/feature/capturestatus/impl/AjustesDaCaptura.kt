package com.pingo.feature.capturestatus.impl

import android.content.Intent
import com.pingo.core.notifications.IntentsDeSistema

internal fun intencaoPara(item: ItemDePermissao, pacote: String): Intent = when (item) {
    ItemDePermissao.ACESSO_A_NOTIFICACOES -> IntentsDeSistema.acessoAsNotificacoes()
    ItemDePermissao.NOTIFICACOES_DO_PINGO -> IntentsDeSistema.notificacoesDoApp(pacote)
    ItemDePermissao.BATERIA -> IntentsDeSistema.ajustesDeBateria()
}
