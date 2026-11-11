package com.pingo.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.pingo.feature.home.api.HomeRoute
import com.pingo.feature.onboarding.api.OnboardingContasRoute
import com.pingo.feature.onboarding.api.OnboardingPermissoesRoute
import com.pingo.feature.onboarding.api.OnboardingRoute
import com.pingo.feature.onboarding.impl.OnboardingContasScreen
import com.pingo.feature.onboarding.impl.OnboardingPermissoesScreen
import com.pingo.feature.onboarding.impl.OnboardingScreen

internal fun NavGraphBuilder.onboardingGraph(navController: NavHostController, onConsentAccepted: () -> Unit) {
    composable(OnboardingRoute.ROUTE) {
        OnboardingScreen(
            onContinue = {
                onConsentAccepted()
                navController.navigate(OnboardingPermissoesRoute.ROUTE) {
                    popUpTo(OnboardingRoute.ROUTE) { inclusive = true }
                }
            },
        )
    }
    composable(OnboardingPermissoesRoute.ROUTE) {
        OnboardingPermissoesScreen(
            onConcluir = {
                navController.navigate(OnboardingContasRoute.ROUTE) {
                    popUpTo(OnboardingPermissoesRoute.ROUTE) { inclusive = true }
                }
            },
        )
    }
    composable(OnboardingContasRoute.ROUTE) {
        OnboardingContasScreen(
            onConcluir = {
                navController.navigate(HomeRoute.ROUTE) {
                    popUpTo(OnboardingContasRoute.ROUTE) { inclusive = true }
                }
            },
        )
    }
}
