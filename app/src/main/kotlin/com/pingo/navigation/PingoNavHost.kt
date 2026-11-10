package com.pingo.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pingo.core.auth.SessionState
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.feature.accounts.api.AccountsRoute
import com.pingo.feature.accounts.impl.AccountsScreen
import com.pingo.feature.capturestatus.api.CaptureStatusRoute
import com.pingo.feature.capturestatus.impl.CaptureStatusScreen
import com.pingo.feature.home.api.HomeRoute
import com.pingo.feature.home.impl.HomeScreen
import com.pingo.feature.onboarding.api.OnboardingContasRoute
import com.pingo.feature.onboarding.api.OnboardingRoute
import com.pingo.feature.reports.api.ReportsRoute
import com.pingo.feature.reports.impl.ReportsScreen
import com.pingo.feature.review.api.ReviewRoute
import com.pingo.feature.review.impl.ReviewScreen
import com.pingo.feature.settings.api.SettingsRoute
import com.pingo.feature.settings.impl.SettingsScreen
import com.pingo.feature.transactions.api.TransactionsRoute
import com.pingo.feature.transactions.impl.TransactionsScreen

@Composable
fun PingoNavHost(session: SessionState, destinoInicial: DestinoInicial?, onConsentAccepted: () -> Unit) {
    if (session == SessionState.Loading || destinoInicial == null) {
        LoadingScreen()
        return
    }

    val navController = rememberNavController()

    RedirectToOnboardingOnLogout(session = session, navController = navController)

    NavHost(
        navController = navController,
        startDestination = destinoInicial.rota(),
        modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars),
    ) {
        onboardingGraph(navController = navController, onConsentAccepted = onConsentAccepted)
        composable(HomeRoute.ROUTE) {
            HomeScreen(onOpenSettings = { navController.navigate(SettingsRoute.ROUTE) })
        }
        composable(TransactionsRoute.ROUTE) { TransactionsScreen() }
        composable(AccountsRoute.ROUTE) { AccountsScreen() }
        composable(ReportsRoute.ROUTE) { ReportsScreen() }
        composable(ReviewRoute.ROUTE) { ReviewScreen() }
        composable(CaptureStatusRoute.ROUTE) { CaptureStatusScreen() }
        composable(SettingsRoute.ROUTE) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onAccountDeleted = { navController.toOnboarding() },
            )
        }
    }
}

private fun DestinoInicial.rota(): String = when (this) {
    DestinoInicial.ONBOARDING -> OnboardingRoute.ROUTE
    DestinoInicial.CONTAS -> OnboardingContasRoute.ROUTE
    DestinoInicial.HOME -> HomeRoute.ROUTE
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PingoTheme.colors.background),
    )
}

@Composable
private fun RedirectToOnboardingOnLogout(session: SessionState, navController: NavHostController) {
    LaunchedEffect(session) {
        if (session == SessionState.LoggedOut && navController.isPastOnboarding()) {
            navController.toOnboarding()
        }
    }
}

private fun NavHostController.isPastOnboarding(): Boolean {
    val route = currentDestination?.route

    return route != null && route != OnboardingRoute.ROUTE
}

private fun NavHostController.toOnboarding() {
    navigate(OnboardingRoute.ROUTE) {
        popUpTo(graph.id) { inclusive = true }
    }
}
