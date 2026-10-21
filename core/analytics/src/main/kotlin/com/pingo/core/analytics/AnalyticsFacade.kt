package com.pingo.core.analytics

interface AnalyticsFacade {
    fun setAnalyticsEnabled(enabled: Boolean)

    fun logEvent(name: String, attributes: Map<String, String>)

    fun recordError(throwable: Throwable)
}
