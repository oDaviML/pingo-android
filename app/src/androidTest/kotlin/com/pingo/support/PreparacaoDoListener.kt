package com.pingo.support

internal const val PACOTE_DO_APP = "com.pingo"

internal const val COMPONENTE_DO_LISTENER =
    "$PACOTE_DO_APP/com.pingo.core.notifications.PingoNotificationListenerService"

internal fun prepararListenerDeCaptura(shell: (String) -> String) {
    val appOpAtual = shell("appops get $PACOTE_DO_APP RECEIVE_SENSITIVE_NOTIFICATIONS")

    shell("appops set $PACOTE_DO_APP RECEIVE_SENSITIVE_NOTIFICATIONS allow")

    if (!appOpAtual.contains(": allow")) {
        shell("cmd notification disallow_listener $COMPONENTE_DO_LISTENER")
    }

    shell("cmd notification allow_listener $COMPONENTE_DO_LISTENER")
}
