package com.pingo.spike.capture

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun SpikeScreen() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            SpikeBody()
        }
    }
}

@Composable
private fun SpikeBody() {
    val context = LocalContext.current
    val store = remember { CorpusStore(File(context.filesDir, CorpusStore.DIRECTORY)) }
    val preferences = remember { SpikePreferences(context) }
    val scope = rememberCoroutineScope()
    var permissionGranted by remember { mutableStateOf(NotificationAccess.isGranted(context)) }
    var captureAll by remember { mutableStateOf(preferences.captureAllPackages) }
    var entries by remember { mutableStateOf(emptyList<CorpusEntry>()) }
    var refreshKey by remember { mutableIntStateOf(0) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        permissionGranted = NotificationAccess.isGranted(context)
        refreshKey += 1
    }

    LaunchedEffect(refreshKey) {
        entries = withContext(Dispatchers.IO) { store.readAll() }
    }

    if (permissionGranted) {
        CorpusContent(
            entries = entries,
            captureAll = captureAll,
            onCaptureAllChange = {
                captureAll = it
                preferences.captureAllPackages = it
            },
            onExport = { scope.launch { shareCorpus(context, store) } },
            onClear = {
                scope.launch {
                    withContext(Dispatchers.IO) { store.clear() }
                    entries = emptyList()
                }
            },
        )
    } else {
        PermissionRequired(onGrant = { context.startActivity(NotificationAccess.settingsIntent()) })
    }
}

@Composable
private fun PermissionRequired(onGrant: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Acesso a notificações necessário", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Sem esse acesso o spike não consegue gravar as notificações dos apps financeiros.")
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onGrant) {
            Text("Conceder acesso")
        }
    }
}

@Composable
private fun CorpusContent(
    entries: List<CorpusEntry>,
    captureAll: Boolean,
    onCaptureAllChange: (Boolean) -> Unit,
    onExport: () -> Unit,
    onClear: () -> Unit,
) {
    var showClearDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Summary(entries)
        Spacer(modifier = Modifier.height(8.dp))
        Actions(
            enabled = entries.isNotEmpty(),
            onExport = onExport,
            onClearRequest = { showClearDialog = true },
        )
        Spacer(modifier = Modifier.height(8.dp))
        DiscoveryToggle(captureAll = captureAll, onCaptureAllChange = onCaptureAllChange)
        Spacer(modifier = Modifier.height(12.dp))
        if (entries.isEmpty()) {
            Text("Nenhuma notificação gravada ainda.")
        } else {
            EntryList(entries)
        }
    }

    if (showClearDialog) {
        ClearCorpusDialog(
            onDismiss = { showClearDialog = false },
            onConfirm = {
                showClearDialog = false
                onClear()
            },
        )
    }
}

@Composable
private fun Summary(entries: List<CorpusEntry>) {
    val counts = remember(entries) { entries.groupingBy { it.packageName }.eachCount() }
    Text("Pingo Spike", style = MaterialTheme.typography.headlineSmall)
    Text("${entries.size} notificação(ões) gravada(s)", style = MaterialTheme.typography.bodyMedium)
    counts.forEach { (packageName, count) ->
        Text(
            text = "${KnownApps.displayName(packageName)} · $count",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun Actions(enabled: Boolean, onExport: () -> Unit, onClearRequest: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onExport, enabled = enabled) {
            Text("Exportar")
        }
        OutlinedButton(onClick = onClearRequest, enabled = enabled) {
            Text("Limpar")
        }
    }
}

@Composable
private fun DiscoveryToggle(captureAll: Boolean, onCaptureAllChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onCaptureAllChange(!captureAll) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Capturar todos os apps")
            Text("Descoberta de pacotes", style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = captureAll, onCheckedChange = onCaptureAllChange)
    }
}

@Composable
private fun EntryList(entries: List<CorpusEntry>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(entries) { entry -> EntryCard(entry) }
    }
}

@Composable
private fun EntryCard(entry: CorpusEntry) {
    var expanded by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "${KnownApps.displayName(entry.packageName)} · ${CorpusFormatter.time(entry.capturedAt)}",
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                text = entry.title.ifBlank { "(sem título)" },
                style = MaterialTheme.typography.titleSmall,
                maxLines = if (expanded) Int.MAX_VALUE else 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = entry.text.ifBlank { "(sem texto)" },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = if (expanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (expanded && entry.bigText.isNotBlank() && entry.bigText != entry.text) {
                Text(text = entry.bigText, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ClearCorpusDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Limpar corpus?") },
        text = { Text("Todas as notificações gravadas serão removidas deste aparelho.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Limpar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
    )
}

private suspend fun shareCorpus(context: Context, store: CorpusStore) {
    val export =
        withContext(Dispatchers.IO) {
            store.exportReadable(File(File(context.cacheDir, EXPORTS_DIRECTORY), EXPORT_FILE_NAME))
        }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", export)
    val share =
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    context.startActivity(Intent.createChooser(share, "Exportar corpus"))
}

private const val EXPORTS_DIRECTORY = "exports"
private const val EXPORT_FILE_NAME = "pingo-corpus.txt"
