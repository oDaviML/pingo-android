package com.pingo.core.notifications

import android.content.Context
import com.pingo.core.model.Assinatura
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

interface NotificadorDeAssinatura {
    fun podeNotificar(): Boolean

    fun postar(assinatura: Assinatura, dataPrevista: LocalDate): Boolean
}

@Singleton
class NotificadorDeAssinaturaDoAndroid @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val notificacoesDoSistema: NotificacoesDoSistema,
) : NotificadorDeAssinatura {
    override fun podeNotificar(): Boolean =
        notificacoesDoSistema.habilitadas() && NotificacoesDeAssinatura.permissaoPostNotificationsConcedida(context)

    override fun postar(assinatura: Assinatura, dataPrevista: LocalDate): Boolean {
        if (!podeNotificar()) {
            return false
        }
        return NotificacoesDeAssinatura.postar(context, assinatura, dataPrevista)
    }
}
