package com.pingo.core.notifications

interface NotificationHealthCheck {
    fun isListenerEnabled(): Boolean

    fun isCaptureServiceRunning(): Boolean
}
