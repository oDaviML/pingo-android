package com.pingo.feature.transactions.impl

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pingo.core.designsystem.component.AbaInferior
import java.io.File

@Composable
fun TransactionsScreen(
    onOpenDetalhe: (String) -> Unit,
    onOpenNovo: () -> Unit,
    onSelecionarAba: (AbaInferior) -> Unit,
    viewModel: TransactionsViewModel = hiltViewModel(),
    mostrarBarraInferior: Boolean = true,
    onVoltar: (() -> Unit)? = null,
    compartilharCsv: ((ExportacaoCsvPronta) -> Unit)? = null,
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.exportacaoCsvPronta) {
        val exportacao = state.exportacaoCsvPronta ?: return@LaunchedEffect
        runCatching {
            val manipulador = compartilharCsv ?: { dados -> iniciarCompartilhamentoCsv(context, dados) }
            manipulador(exportacao)
        }
        viewModel.onEvent(TransactionsEvent.ConsumirExportacaoCsv)
    }

    TransactionsContent(
        state = state,
        onEvent = viewModel::onEvent,
        onOpenDetalhe = onOpenDetalhe,
        onOpenNovo = onOpenNovo,
        onSelecionarAba = onSelecionarAba,
        mostrarBarraInferior = mostrarBarraInferior,
        onVoltar = onVoltar,
    )
}

internal fun prepararArquivoCsv(diretorioCache: File, exportacao: ExportacaoCsvPronta): File {
    val pasta = File(diretorioCache, SUBDIRETORIO_CSV_EXPORTS).apply { mkdirs() }
    val arquivo = File(pasta, exportacao.nomeArquivo)
    arquivo.writeText(exportacao.conteudoCsv, Charsets.UTF_8)
    return arquivo
}

internal fun iniciarCompartilhamentoCsv(context: Context, exportacao: ExportacaoCsvPronta) {
    val arquivo = prepararArquivoCsv(context.cacheDir, exportacao)
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}$SUFIXO_FILEPROVIDER_CSV",
        arquivo,
    )
    val envio = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, exportacao.nomeArquivo)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val seletor = Intent.createChooser(envio, "Exportar lançamentos").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(seletor)
}

private const val SUBDIRETORIO_CSV_EXPORTS = "csv_exports"
private const val SUFIXO_FILEPROVIDER_CSV = ".csvexport.fileprovider"
