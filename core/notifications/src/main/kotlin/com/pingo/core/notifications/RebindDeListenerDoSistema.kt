package com.pingo.core.notifications

import android.content.ComponentName
import android.content.Context
import android.service.notification.NotificationListenerService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RebindDeListenerDoSistema @Inject constructor(@param:ApplicationContext private val context: Context) : RebindDeListener {
    override fun solicitar(): Boolean = runCatching {
        NotificationListenerService.requestRebind(ComponentName(context, PingoNotificationListenerService::class.java))
    }.isSuccess
}
