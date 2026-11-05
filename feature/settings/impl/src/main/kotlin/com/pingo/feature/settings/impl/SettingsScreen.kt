package com.pingo.feature.settings.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pingo.core.designsystem.component.PingoCard
import com.pingo.core.designsystem.component.PingoDestructiveDialog
import com.pingo.core.designsystem.component.PingoTopBar
import com.pingo.core.designsystem.component.SectionHeader
import com.pingo.core.designsystem.component.SettingsRow
import com.pingo.core.designsystem.component.SettingsRowStyle
import com.pingo.core.designsystem.icon.PingoIcons
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.ui.PrivacyTermDialog

@Composable
fun SettingsScreen(onBack: () -> Unit, onAccountDeleted: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showTerm by remember { mutableStateOf(false) }

    LaunchedEffect(state.accountDeleted) {
        if (state.accountDeleted) {
            onAccountDeleted()
        }
    }

    SettingsContent(
        onBack = onBack,
        onShowTerm = { showTerm = true },
        onDeleteAccount = viewModel::showDeleteDialog,
    )

    if (showTerm) {
        PrivacyTermDialog(onDismiss = { showTerm = false })
    }

    if (state.showDeleteDialog) {
        PingoDestructiveDialog(
            title = "Excluir conta e dados?",
            description = deleteDescription(failed = state.deleteFailed),
            confirmText = "Excluir",
            onConfirm = viewModel::deleteAccount,
            onDismissRequest = viewModel::dismissDeleteDialog,
            confirmLoading = state.deleting,
        )
    }
}

@Composable
fun SettingsContent(onBack: () -> Unit, onShowTerm: () -> Unit, onDeleteAccount: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PingoTheme.colors.background),
    ) {
        PingoTopBar(title = "Configurações", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PingoSpacing.lg, vertical = PingoSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(PingoSpacing.md),
        ) {
            SectionHeader("Privacidade e dados")

            PingoCard(contentPadding = 0.dp, gap = 0.dp) {
                SettingsRow(
                    title = "Termo de privacidade",
                    subtitle = "Como o Pingo trata seus dados",
                    icon = PingoIcons.FileText,
                    onClick = onShowTerm,
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(PingoTheme.colors.outlineVariant),
                )

                SettingsRow(
                    title = "Excluir conta e dados",
                    subtitle = "Apaga tudo no aparelho e na nuvem",
                    icon = PingoIcons.Trash2,
                    onClick = onDeleteAccount,
                    style = SettingsRowStyle.Danger,
                )
            }
        }
    }
}

private fun deleteDescription(failed: Boolean): String {
    val base = "Isso apaga tudo no celular e na nuvem e não dá para desfazer."

    return if (failed) {
        "$base\n\nNão foi possível excluir. Verifique a conexão e tente de novo."
    } else {
        base
    }
}
