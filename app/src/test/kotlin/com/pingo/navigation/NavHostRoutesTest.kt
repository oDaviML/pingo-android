package com.pingo.navigation

import com.pingo.feature.accounts.api.AccountsRoute
import com.pingo.feature.capturestatus.api.CaptureStatusRoute
import com.pingo.feature.home.api.HomeRoute
import com.pingo.feature.onboarding.api.OnboardingContasRoute
import com.pingo.feature.onboarding.api.OnboardingRoute
import com.pingo.feature.reports.api.ReportsRoute
import com.pingo.feature.review.api.ReviewRoute
import com.pingo.feature.settings.api.SettingsRoute
import com.pingo.feature.transactions.api.TransactionsRoute
import org.junit.Assert.assertEquals
import org.junit.Test

class NavHostRoutesTest {
    @Test
    fun featureRoutesAreUnique() {
        val routes = listOf(
            OnboardingRoute.ROUTE,
            OnboardingContasRoute.ROUTE,
            HomeRoute.ROUTE,
            TransactionsRoute.ROUTE,
            AccountsRoute.ROUTE,
            ReportsRoute.ROUTE,
            ReviewRoute.ROUTE,
            CaptureStatusRoute.ROUTE,
            SettingsRoute.ROUTE,
        )
        assertEquals(routes.size, routes.toSet().size)
    }
}
