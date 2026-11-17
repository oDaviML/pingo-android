package com.pingo.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.pingo.core.designsystem.theme.PingoMotion
import com.pingo.feature.accounts.api.AccountsRoute
import com.pingo.feature.home.api.HomeRoute
import com.pingo.feature.reports.api.ReportsRoute
import com.pingo.feature.transactions.api.TransactionsRoute

private const val EXIT_MS = 200
private const val HORIZONTAL_ENTER_FRACTION = 6
private const val HORIZONTAL_EXIT_FRACTION = 12
private const val VERTICAL_MODAL_FRACTION = 5

internal val TAB_ROUTES: Set<String> = setOf(
    HomeRoute.ROUTE,
    TransactionsRoute.ROUTE,
    AccountsRoute.ROUTE,
    ReportsRoute.ROUTE,
)

internal fun NavGraphBuilder.abaComposable(route: String, reducedMotion: Boolean, content: @Composable (NavBackStackEntry) -> Unit) {
    composable(
        route = route,
        enterTransition = { enterDaAba(initialState.destination.route, reducedMotion) },
        exitTransition = { exitDaAba(targetState.destination.route, reducedMotion) },
        popEnterTransition = { popEnterDaAba(initialState.destination.route, reducedMotion) },
        popExitTransition = { popExitDaAba(targetState.destination.route, reducedMotion) },
        content = { entry -> content(entry) },
    )
}

internal fun enterDaAba(origem: String?, reducedMotion: Boolean): EnterTransition = when {
    reducedMotion -> fadeReduzidoEnter()
    origem in TAB_ROUTES -> fadeIn(tween(durationMillis = PingoMotion.SHORT_MS, easing = PingoMotion.StandardEasing))
    origem == TransactionsRoute.NOVO_ROUTE -> fadeIn(
        tween(durationMillis = PingoMotion.MEDIUM_MS, easing = PingoMotion.DecelerateEasing),
    )
    else -> enterEmpurravel(false)
}

internal fun exitDaAba(destino: String?, reducedMotion: Boolean): ExitTransition = when {
    reducedMotion -> fadeReduzidoExit()
    destino in TAB_ROUTES -> fadeOut(tween(durationMillis = PingoMotion.SHORT_MS, easing = PingoMotion.StandardEasing))
    destino == TransactionsRoute.NOVO_ROUTE -> fadeOut(tween(durationMillis = EXIT_MS, easing = PingoMotion.AccelerateEasing))
    else -> exitEmpurravel(false)
}

internal fun popEnterDaAba(origem: String?, reducedMotion: Boolean): EnterTransition = when {
    reducedMotion -> fadeReduzidoEnter()
    origem in TAB_ROUTES -> fadeIn(tween(durationMillis = PingoMotion.SHORT_MS, easing = PingoMotion.StandardEasing))
    origem == TransactionsRoute.NOVO_ROUTE -> fadeIn(
        tween(durationMillis = PingoMotion.MEDIUM_MS, easing = PingoMotion.DecelerateEasing),
    )
    else -> popEnterEmpurravel(false)
}

internal fun popExitDaAba(destino: String?, reducedMotion: Boolean): ExitTransition = when {
    reducedMotion -> fadeReduzidoExit()
    destino in TAB_ROUTES -> fadeOut(tween(durationMillis = PingoMotion.SHORT_MS, easing = PingoMotion.StandardEasing))
    else -> popExitEmpurravel(false)
}

internal fun enterEmpurravel(reducedMotion: Boolean): EnterTransition = if (reducedMotion) {
    fadeReduzidoEnter()
} else {
    slideInHorizontally(
        tween(durationMillis = PingoMotion.MEDIUM_MS, easing = PingoMotion.DecelerateEasing),
    ) { width -> width / HORIZONTAL_ENTER_FRACTION } + fadeIn(
        tween(durationMillis = PingoMotion.MEDIUM_MS, easing = PingoMotion.DecelerateEasing),
    )
}

internal fun exitEmpurravel(reducedMotion: Boolean): ExitTransition = if (reducedMotion) {
    fadeReduzidoExit()
} else {
    slideOutHorizontally(
        tween(durationMillis = EXIT_MS, easing = PingoMotion.AccelerateEasing),
    ) { width -> -width / HORIZONTAL_EXIT_FRACTION } + fadeOut(
        tween(durationMillis = EXIT_MS, easing = PingoMotion.AccelerateEasing),
    )
}

internal fun popEnterEmpurravel(reducedMotion: Boolean): EnterTransition = if (reducedMotion) {
    fadeReduzidoEnter()
} else {
    slideInHorizontally(
        tween(durationMillis = PingoMotion.MEDIUM_MS, easing = PingoMotion.DecelerateEasing),
    ) { width -> -width / HORIZONTAL_EXIT_FRACTION } + fadeIn(
        tween(durationMillis = PingoMotion.MEDIUM_MS, easing = PingoMotion.DecelerateEasing),
    )
}

internal fun popExitEmpurravel(reducedMotion: Boolean): ExitTransition = if (reducedMotion) {
    fadeReduzidoExit()
} else {
    slideOutHorizontally(
        tween(durationMillis = EXIT_MS, easing = PingoMotion.AccelerateEasing),
    ) { width -> width / HORIZONTAL_ENTER_FRACTION } + fadeOut(
        tween(durationMillis = EXIT_MS, easing = PingoMotion.AccelerateEasing),
    )
}

internal fun enterModal(reducedMotion: Boolean): EnterTransition = if (reducedMotion) {
    fadeReduzidoEnter()
} else {
    slideInVertically(
        tween(durationMillis = PingoMotion.MEDIUM_MS, easing = PingoMotion.DecelerateEasing),
    ) { height -> height / VERTICAL_MODAL_FRACTION } + fadeIn(
        tween(durationMillis = PingoMotion.MEDIUM_MS, easing = PingoMotion.DecelerateEasing),
    )
}

internal fun popExitModal(reducedMotion: Boolean): ExitTransition = if (reducedMotion) {
    fadeReduzidoExit()
} else {
    slideOutVertically(
        tween(durationMillis = EXIT_MS, easing = PingoMotion.AccelerateEasing),
    ) { height -> height / VERTICAL_MODAL_FRACTION } + fadeOut(
        tween(durationMillis = EXIT_MS, easing = PingoMotion.AccelerateEasing),
    )
}

internal fun enterReduzidoOuFade(reducedMotion: Boolean): EnterTransition = if (reducedMotion) {
    fadeReduzidoEnter()
} else {
    fadeIn(tween(durationMillis = PingoMotion.MEDIUM_MS, easing = PingoMotion.DecelerateEasing))
}

internal fun exitReduzidoOuFade(reducedMotion: Boolean): ExitTransition = if (reducedMotion) {
    fadeReduzidoExit()
} else {
    fadeOut(tween(durationMillis = EXIT_MS, easing = PingoMotion.AccelerateEasing))
}

private fun fadeReduzidoEnter(): EnterTransition =
    fadeIn(tween(durationMillis = PingoMotion.REDUCED_FADE_MS, easing = PingoMotion.LinearEasing))

private fun fadeReduzidoExit(): ExitTransition =
    fadeOut(tween(durationMillis = PingoMotion.REDUCED_FADE_MS, easing = PingoMotion.LinearEasing))
