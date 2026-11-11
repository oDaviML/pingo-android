package com.pingo.core.notifications

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.pingo.core.datastore.PermissaoDoPingo
import com.pingo.core.datastore.PermissoesDataSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

interface RevisaoPorNotificacao {
    suspend fun habilitada(): Boolean
}

@Singleton
class RevisaoPorNotificacaoConfigurada @Inject constructor(
    private val permissoes: PermissoesDataSource,
    private val notificacoesDoSistema: NotificacoesDoSistema,
) : RevisaoPorNotificacao {
    override suspend fun habilitada(): Boolean =
        notificacoesDoSistema.habilitadas() && permissoes.notificacoesDoPingo.first() == PermissaoDoPingo.PERMITIDA
}

@Singleton
class NotificacoesDoSistemaDoAndroid @Inject constructor(@param:ApplicationContext private val context: Context) : NotificacoesDoSistema {
    override fun habilitadas(): Boolean = NotificationManagerCompat.from(context).areNotificationsEnabled()
}
