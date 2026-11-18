package com.pingo.feature.settings.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.pingo.core.designsystem.component.PingoBottomSheet
import com.pingo.core.designsystem.component.PingoButton
import com.pingo.core.designsystem.component.PingoButtonType
import com.pingo.core.designsystem.component.PingoChoiceChip
import com.pingo.core.designsystem.component.PingoDestructiveDialog
import com.pingo.core.designsystem.component.PingoIcon
import com.pingo.core.designsystem.component.PingoText
import com.pingo.core.designsystem.component.PingoTextField
import com.pingo.core.designsystem.icon.PingoIcons
import com.pingo.core.designsystem.theme.PingoLayout
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.model.Categoria
import com.pingo.core.model.TipoCategoria

@Composable
internal fun CategoriasERegrasDialogosHost(state: CategoriasERegrasUiState, viewModel: CategoriasERegrasViewModel) {
    state.dialogoCriar?.let { dialogo ->
        NovaCategoriaSheet(
            dialogo = dialogo,
            onNomeChange = { viewModel.atualizarNovaCategoria(nome = it) },
            onTipoChange = { viewModel.atualizarNovaCategoria(tipo = it) },
            onConfirm = viewModel::confirmarCriarCategoria,
            onDismiss = viewModel::fecharDialogoCriar,
        )
    }
    state.dialogoRenomear?.let { dialogo ->
        RenomearCategoriaSheet(
            dialogo = dialogo,
            onNomeChange = viewModel::atualizarNomeRenomeacao,
            onConfirm = viewModel::confirmarRenomear,
            onDismiss = viewModel::fecharDialogoRenomear,
        )
    }
    state.dialogoExcluirCategoria?.let { dialogo ->
        ExcluirCategoriaHost(
            dialogo = dialogo,
            onSelecionarDestino = viewModel::selecionarDestinoExclusao,
            onConfirm = viewModel::confirmarExcluirCategoria,
            onDismiss = viewModel::fecharDialogoExcluirCategoria,
        )
    }
    state.dialogoExcluirRegra?.let { regraItem ->
        PingoDestructiveDialog(
            title = "Excluir regra aprendida?",
            description = "Novos lançamentos de ${regraItem.regra.estabelecimentoNormalizado} não usarão mais esta regra.",
            confirmText = "Excluir",
            onConfirm = viewModel::confirmarExcluirRegra,
            onDismissRequest = viewModel::fecharDialogoExcluirRegra,
        )
    }
}

@Composable
private fun NovaCategoriaSheet(
    dialogo: DialogoNovaCategoria,
    onNomeChange: (String) -> Unit,
    onTipoChange: (TipoCategoria) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    PingoBottomSheet(onDismissRequest = onDismiss, title = "Nova categoria") {
        PingoTextField(
            value = dialogo.nome,
            onValueChange = onNomeChange,
            label = "Nome da categoria",
            supportingText = dialogo.erro,
            isError = dialogo.erro != null,
            enabled = !dialogo.salvando,
            modifier = Modifier.testTag("campo_nome_nova_categoria"),
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(PingoSpacing.sm)) {
            PingoChoiceChip(
                text = "Despesa",
                selected = dialogo.tipo == TipoCategoria.DESPESA,
                onClick = { onTipoChange(TipoCategoria.DESPESA) },
                modifier = Modifier.weight(1f),
            )
            PingoChoiceChip(
                text = "Receita",
                selected = dialogo.tipo == TipoCategoria.RECEITA,
                onClick = { onTipoChange(TipoCategoria.RECEITA) },
                modifier = Modifier.weight(1f),
            )
        }
        SheetAcoes(confirmText = "Criar", carregando = dialogo.salvando, onConfirm = onConfirm, onDismiss = onDismiss)
    }
}

@Composable
private fun RenomearCategoriaSheet(
    dialogo: DialogoRenomearCategoria,
    onNomeChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    PingoBottomSheet(onDismissRequest = onDismiss, title = "Renomear categoria") {
        PingoTextField(
            value = dialogo.nome,
            onValueChange = onNomeChange,
            label = "Novo nome",
            supportingText = dialogo.erro,
            isError = dialogo.erro != null,
            enabled = !dialogo.salvando,
            modifier = Modifier.testTag("campo_renomear_categoria"),
        )
        SheetAcoes(confirmText = "Salvar", carregando = dialogo.salvando, onConfirm = onConfirm, onDismiss = onDismiss)
    }
}

@Composable
private fun ExcluirCategoriaHost(
    dialogo: DialogoExcluirCategoria,
    onSelecionarDestino: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (dialogo.item.quantidadeLancamentos == 0) {
        PingoDestructiveDialog(
            title = "Excluir ${dialogo.item.categoria.nome}?",
            description = "Esta categoria não tem lançamentos vinculados.",
            confirmText = "Excluir",
            onConfirm = onConfirm,
            onDismissRequest = onDismiss,
            confirmLoading = dialogo.excluindo,
        )
        return
    }
    PingoBottomSheet(
        onDismissRequest = onDismiss,
        title = "Excluir ${dialogo.item.categoria.nome}",
        subtitle = "${dialogo.item.quantidadeLancamentos} lançamentos serão movidos para a categoria escolhida:",
    ) {
        ListaDestinosMigracao(
            destinos = dialogo.destinosDisponiveis,
            selecionadoId = dialogo.destinoSelecionadoId,
            onSelecionarDestino = onSelecionarDestino,
            modifier = Modifier.weight(1f, fill = false),
        )
        dialogo.erro?.let {
            PingoText(text = it, style = PingoTheme.typography.bodySmall, color = PingoTheme.colors.expense)
        }
        val habilitado = dialogo.destinoSelecionadoId != null && !dialogo.excluindo
        PingoButton(
            text = "Excluir e mover",
            onClick = onConfirm,
            type = PingoButtonType.Danger,
            enabled = habilitado,
            loading = dialogo.excluindo,
            modifier = Modifier.fillMaxWidth().testTag("botao_confirmar_exclusao_migracao"),
        )
    }
}

@Composable
private fun ListaDestinosMigracao(
    destinos: List<Categoria>,
    selecionadoId: String?,
    onSelecionarDestino: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        destinos.forEach { destino ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = PingoLayout.minTouchTarget)
                    .selectable(
                        selected = destino.publicId == selecionadoId,
                        role = Role.RadioButton,
                        onClick = { onSelecionarDestino(destino.publicId) },
                    )
                    .padding(vertical = PingoSpacing.xs),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PingoText(text = destino.nome, style = PingoTheme.typography.body)
                if (destino.publicId == selecionadoId) {
                    PingoIcon(
                        imageVector = PingoIcons.Check,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = PingoTheme.colors.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun SheetAcoes(confirmText: String, carregando: Boolean, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(PingoSpacing.sm)) {
        PingoButton(text = "Cancelar", onClick = onDismiss, type = PingoButtonType.Text, modifier = Modifier.weight(1f))
        PingoButton(
            text = confirmText,
            onClick = onConfirm,
            enabled = !carregando,
            loading = carregando,
            modifier = Modifier.weight(1f),
        )
    }
}
