package com.pingo.navigation

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.pingo.core.auth.SessionState
import com.pingo.core.designsystem.component.AbaInferior
import com.pingo.core.designsystem.component.PingoBottomBar
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.designsystem.theme.rememberReducedMotion
import com.pingo.core.notifications.NotificacoesDeRevisao
import com.pingo.feature.accounts.api.AccountsRoute
import com.pingo.feature.accounts.impl.AccountsScreen
import com.pingo.feature.accounts.impl.ContaFormScreen
import com.pingo.feature.accounts.impl.FaturasScreen
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
import com.pingo.feature.settings.impl.CategoriasERegrasScreen
import com.pingo.feature.settings.impl.SettingsScreen
import com.pingo.feature.transactions.api.TransactionsRoute
import com.pingo.feature.transactions.impl.DetalheDoLancamentoScreen
import com.pingo.feature.transactions.impl.NovoLancamentoScreen
import com.pingo.feature.transactions.impl.TransactionsScreen

@Composable
fun PingoNavHost(session: SessionState, destinoInicial: DestinoInicial?, onConsentAccepted: () -> Unit) {
    if (session == SessionState.Loading || destinoInicial == null) {
        LoadingScreen()
        return
    }

    val activity = LocalActivity.current
    val intencao = activity?.intent
    if (intencao?.hasExtra(NotificacoesDeRevisao.EXTRA_PUBLIC_ID) == true) {
        intencao.removeFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    val navController = rememberNavController()
    RedirectToOnboardingOnLogout(session = session, navController = navController)
    NavegacaoComBarraInferior(
        navController = navController,
        destinoInicial = destinoInicial,
        onConsentAccepted = onConsentAccepted,
    )
    ContextoInicial(
        activity = activity,
        destinoInicial = destinoInicial,
        navController = navController,
    )
}

@Composable
private fun NavegacaoComBarraInferior(navController: NavHostController, destinoInicial: DestinoInicial, onConsentAccepted: () -> Unit) {
    val reducedMotion = rememberReducedMotion()
    val entradaAtual by navController.currentBackStackEntryAsState()
    val abaSelecionada = abaParaRota(entradaAtual?.destination?.route)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PingoTheme.colors.background)
            .windowInsetsPadding(WindowInsets.systemBars),
    ) {
        NavHost(
            navController = navController,
            startDestination = destinoInicial.rota(),
            modifier = Modifier.weight(1f).fillMaxWidth(),
            enterTransition = { enterEmpurravel(reducedMotion) },
            exitTransition = { exitEmpurravel(reducedMotion) },
            popEnterTransition = { popEnterEmpurravel(reducedMotion) },
            popExitTransition = { popExitEmpurravel(reducedMotion) },
        ) {
            onboardingGraph(navController = navController, onConsentAccepted = onConsentAccepted)
            telasDeAba(navController = navController, reducedMotion = reducedMotion)
            telasDeLancamento(navController = navController, reducedMotion = reducedMotion)
            telasDeCadastroDeConta(navController = navController)
            telasDeApoio(navController = navController)
        }
        if (abaSelecionada != null) {
            PingoBottomBar(
                selecionada = abaSelecionada,
                onSelecionar = { aba -> navController.abrirAba(aba) },
                onNovo = { navController.navigate(TransactionsRoute.NOVO_ROUTE) },
            )
        }
    }
}

@Composable
private fun ContextoInicial(activity: Activity?, destinoInicial: DestinoInicial, navController: NavHostController) {
    var tratado by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(navController, destinoInicial) {
        val intencao = activity?.intent
        if (tratado || destinoInicial != DestinoInicial.HOME || intencao == null) {
            return@LaunchedEffect
        }

        tratado = true
        val publicId = intencao.getStringExtra(NotificacoesDeRevisao.EXTRA_PUBLIC_ID)
        if (publicId != null) {
            navController.navigate(TransactionsRoute.detalhe(publicId))
        } else {
            navController.handleDeepLink(intencao)
        }
    }
}

private fun NavGraphBuilder.telasDeAba(navController: NavHostController, reducedMotion: Boolean) {
    abaInicio(navController = navController, reducedMotion = reducedMotion)
    abaComposable(route = TransactionsRoute.ROUTE, reducedMotion = reducedMotion) {
        TransactionsScreen(
            onOpenDetalhe = { publicId -> navController.navigate(TransactionsRoute.detalhe(publicId)) },
            onOpenNovo = { navController.navigate(TransactionsRoute.NOVO_ROUTE) },
            onSelecionarAba = { aba -> navController.abrirAba(aba) },
            mostrarBarraInferior = false,
        )
    }
    abaComposable(route = AccountsRoute.ROUTE, reducedMotion = reducedMotion) {
        AccountsScreen(
            onSelecionarAba = { aba -> navController.abrirAba(aba) },
            onNovaConta = { navController.navigate(AccountsRoute.NOVA_ROUTE) },
            onAbrirConta = { contaPublicId -> navController.navigate(AccountsRoute.editar(contaPublicId)) },
            onOpenNovo = { navController.navigate(TransactionsRoute.NOVO_ROUTE) },
            mostrarBarraInferior = false,
            onAbrirFaturas = { instrumentoId -> navController.navigate(AccountsRoute.faturas(instrumentoId)) },
        )
    }
    abaRelatorios(navController = navController, reducedMotion = reducedMotion)
}

private fun NavGraphBuilder.abaInicio(navController: NavHostController, reducedMotion: Boolean) {
    abaComposable(route = HomeRoute.ROUTE, reducedMotion = reducedMotion) {
        HomeScreen(
            onOpenSettings = { navController.navigate(SettingsRoute.ROUTE) },
            onOpenCaptura = { navController.navigate(CaptureStatusRoute.ROUTE) },
            onOpenRevisao = { navController.navigate(ReviewRoute.ROUTE) },
            onOpenNovo = { navController.navigate(TransactionsRoute.NOVO_ROUTE) },
            onSelecionarAba = { aba -> navController.abrirAba(aba) },
            mostrarBarraInferior = false,
            onAbrirFaturas = { instrumentoId -> navController.navigate(AccountsRoute.faturas(instrumentoId)) },
        )
    }
}

private fun NavGraphBuilder.abaRelatorios(navController: NavHostController, reducedMotion: Boolean) {
    abaComposable(route = ReportsRoute.ROUTE, reducedMotion = reducedMotion) {
        ReportsScreen(
            onSelecionarAba = { aba -> navController.abrirAba(aba) },
            onOpenNovo = { navController.navigate(TransactionsRoute.NOVO_ROUTE) },
            mostrarBarraInferior = false,
            onAbrirCategoriaNoMes = { categoriaId, mes ->
                navController.navigate(TransactionsRoute.porCategoria(categoriaId, mes.toString()))
            },
        )
    }
}

private fun NavGraphBuilder.telasDeLancamento(navController: NavHostController, reducedMotion: Boolean) {
    composable(
        route = TransactionsRoute.NOVO_ROUTE,
        enterTransition = { enterModal(reducedMotion) },
        exitTransition = { exitReduzidoOuFade(reducedMotion) },
        popEnterTransition = { enterReduzidoOuFade(reducedMotion) },
        popExitTransition = { popExitModal(reducedMotion) },
    ) {
        NovoLancamentoScreen(onVoltar = { navController.popBackStack() })
    }
    composable(
        route = TransactionsRoute.DETALHE_ROUTE,
        arguments = listOf(navArgument(TransactionsRoute.ARG_PUBLIC_ID) { type = NavType.StringType }),
        deepLinks = listOf(navDeepLink { uriPattern = NotificacoesDeRevisao.PADRAO_DEEP_LINK_DETALHE }),
    ) { entrada ->
        DetalheDoLancamentoScreen(
            publicId = entrada.arguments?.getString(TransactionsRoute.ARG_PUBLIC_ID).orEmpty(),
            onBack = { navController.popBackStack() },
        )
    }
    composable(
        route = TransactionsRoute.POR_CATEGORIA_ROUTE,
        arguments = listOf(
            navArgument(TransactionsRoute.ARG_CATEGORIA_ID) { type = NavType.StringType },
            navArgument(TransactionsRoute.ARG_MES) { type = NavType.StringType },
        ),
    ) {
        TransactionsScreen(
            onOpenDetalhe = { publicId -> navController.navigate(TransactionsRoute.detalhe(publicId)) },
            onOpenNovo = { navController.navigate(TransactionsRoute.NOVO_ROUTE) },
            onSelecionarAba = { aba -> navController.abrirAba(aba) },
            mostrarBarraInferior = false,
            onVoltar = { navController.popBackStack() },
        )
    }
}

private fun NavGraphBuilder.telasDeCadastroDeConta(navController: NavHostController) {
    composable(AccountsRoute.NOVA_ROUTE) {
        ContaFormScreen(onVoltar = { navController.popBackStack() })
    }
    composable(
        route = AccountsRoute.EDITAR_ROUTE,
        arguments = listOf(navArgument(AccountsRoute.ARG_CONTA_ID) { type = NavType.StringType }),
    ) {
        ContaFormScreen(onVoltar = { navController.popBackStack() })
    }
    composable(
        route = AccountsRoute.FATURAS_ROUTE,
        arguments = listOf(navArgument(AccountsRoute.ARG_INSTRUMENTO_ID) { type = NavType.StringType }),
    ) {
        FaturasScreen(
            onVoltar = { navController.popBackStack() },
            onAbrirLancamento = { publicId -> navController.navigate(TransactionsRoute.detalhe(publicId)) },
        )
    }
}

private fun NavGraphBuilder.telasDeApoio(navController: NavHostController) {
    composable(ReviewRoute.ROUTE) {
        ReviewScreen(
            onCorrigir = { publicId -> navController.navigate(TransactionsRoute.detalhe(publicId)) },
            onVoltar = { navController.popBackStack() },
        )
    }
    composable(CaptureStatusRoute.ROUTE) {
        CaptureStatusScreen(onBack = { navController.popBackStack() })
    }
    composable(SettingsRoute.ROUTE) {
        SettingsScreen(
            onBack = { navController.popBackStack() },
            onAccountDeleted = { navController.toOnboarding() },
            onLoggedOut = { navController.toOnboarding() },
            onAbrirCategoriasERegras = { navController.navigate(SettingsRoute.CATEGORIAS_E_REGRAS_ROUTE) },
        )
    }
    composable(SettingsRoute.CATEGORIAS_E_REGRAS_ROUTE) {
        CategoriasERegrasScreen(onBack = { navController.popBackStack() })
    }
}

internal fun abaParaRota(rota: String?): AbaInferior? = when (rota) {
    HomeRoute.ROUTE -> AbaInferior.INICIO
    TransactionsRoute.ROUTE -> AbaInferior.LANCAMENTOS
    AccountsRoute.ROUTE -> AbaInferior.CONTAS
    ReportsRoute.ROUTE -> AbaInferior.RELATORIOS
    else -> null
}

private fun DestinoInicial.rota(): String = when (this) {
    DestinoInicial.ONBOARDING -> OnboardingRoute.ROUTE
    DestinoInicial.CONTAS -> OnboardingContasRoute.ROUTE
    DestinoInicial.HOME -> HomeRoute.ROUTE
}

private fun NavHostController.abrirAba(aba: AbaInferior) {
    val rota = when (aba) {
        AbaInferior.INICIO -> HomeRoute.ROUTE
        AbaInferior.LANCAMENTOS -> TransactionsRoute.ROUTE
        AbaInferior.CONTAS -> AccountsRoute.ROUTE
        AbaInferior.RELATORIOS -> ReportsRoute.ROUTE
    }

    if (currentDestination?.route == rota) {
        return
    }

    navigate(rota) {
        popUpTo(HomeRoute.ROUTE) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
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
