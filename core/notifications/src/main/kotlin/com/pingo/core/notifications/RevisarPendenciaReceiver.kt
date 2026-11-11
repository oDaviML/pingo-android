package com.pingo.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pingo.core.common.AppDispatchers
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class RevisarPendenciaReceiver : BroadcastReceiver() {
    @Inject lateinit var confirmarPelaNotificacao: ConfirmarPelaNotificacao

    @Inject lateinit var dispatchers: AppDispatchers

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != NotificacoesDeRevisao.ACAO_CONFIRMAR) {
            return
        }

        val publicId = intent.getStringExtra(NotificacoesDeRevisao.EXTRA_PUBLIC_ID) ?: return
        val pendente = goAsync()

        CoroutineScope(SupervisorJob() + dispatchers.io).launch {
            try {
                confirmarPelaNotificacao.confirmar(publicId)
            } finally {
                pendente.finish()
            }
        }
    }
}
