package com.pingo.navigation

import com.pingo.core.notifications.NotificacoesDeRevisao
import com.pingo.feature.accounts.api.AccountsRoute
import com.pingo.feature.capturestatus.api.CaptureStatusRoute
import com.pingo.feature.home.api.HomeRoute
import com.pingo.feature.onboarding.api.OnboardingContasRoute
import com.pingo.feature.onboarding.api.OnboardingPermissoesRoute
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
            OnboardingPermissoesRoute.ROUTE,
            OnboardingContasRoute.ROUTE,
            HomeRoute.ROUTE,
            TransactionsRoute.ROUTE,
            TransactionsRoute.DETALHE_ROUTE,
            AccountsRoute.ROUTE,
            AccountsRoute.NOVA_ROUTE,
            AccountsRoute.EDITAR_ROUTE,
            ReportsRoute.ROUTE,
            ReviewRoute.ROUTE,
            CaptureStatusRoute.ROUTE,
            SettingsRoute.ROUTE,
        )
        assertEquals(routes.size, routes.toSet().size)
    }

    @Test
    fun detalheRouteCarregaPublicId() {
        assertEquals("transactions/detalhe/lanc-1", TransactionsRoute.detalhe("lanc-1"))
    }

    @Test
    fun editarContaRouteCarregaPublicId() {
        assertEquals("accounts/editar/conta-1", AccountsRoute.editar("conta-1"))
    }

    @Test
    fun deepLinkDaNotificacaoApontaParaODetalhe() {
        assertEquals(
            "pingo://detalhe/{${TransactionsRoute.ARG_PUBLIC_ID}}",
            NotificacoesDeRevisao.PADRAO_DEEP_LINK_DETALHE,
        )
    }
}
