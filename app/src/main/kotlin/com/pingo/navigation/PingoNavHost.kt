package com.pingo.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
import com.pingo.feature.onboarding.api.OnboardingRoute
import com.pingo.feature.onboarding.impl.OnboardingScreen
import com.pingo.feature.reports.api.ReportsRoute
import com.pingo.feature.reports.impl.ReportsScreen
import com.pingo.feature.review.api.ReviewRoute
import com.pingo.feature.review.impl.ReviewScreen
import com.pingo.feature.settings.api.SettingsRoute
import com.pingo.feature.settings.impl.SettingsScreen
import com.pingo.feature.transactions.api.TransactionsRoute
import com.pingo.feature.transactions.impl.TransactionsScreen

@Composable
fun PingoNavHost(session: SessionState) {
    if (session == SessionState.Loading) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PingoTheme.colors.background),
        )
        return
    }

    val navController = rememberNavController()
    val startDestination = if (session == SessionState.LoggedIn) HomeRoute.ROUTE else OnboardingRoute.ROUTE

    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable(OnboardingRoute.ROUTE) {
            OnboardingScreen(
                onContinue = {
                    navController.navigate(HomeRoute.ROUTE) {
                        popUpTo(OnboardingRoute.ROUTE) { inclusive = true }
                    }
                },
            )
        }
        composable(HomeRoute.ROUTE) { HomeScreen() }
        composable(TransactionsRoute.ROUTE) { TransactionsScreen() }
        composable(AccountsRoute.ROUTE) { AccountsScreen() }
        composable(ReportsRoute.ROUTE) { ReportsScreen() }
        composable(ReviewRoute.ROUTE) { ReviewScreen() }
        composable(CaptureStatusRoute.ROUTE) { CaptureStatusScreen() }
        composable(SettingsRoute.ROUTE) { SettingsScreen() }
    }
}
