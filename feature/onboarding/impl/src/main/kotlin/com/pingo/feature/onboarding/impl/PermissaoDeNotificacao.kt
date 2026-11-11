package com.pingo.feature.onboarding.impl

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

interface PermissaoDeNotificacao {
    fun concedida(): Boolean

    fun solicitar(aoResultado: (Boolean) -> Unit)
}

class PermissaoDeNotificacaoDoAndroid(
    private val context: Context,
    private val precisaPedir: () -> Boolean = { semPermissaoDeNotificacao(context) },
) : PermissaoDeNotificacao {
    private var lancador: ((String) -> Unit)? = null
    private var pendente: ((Boolean) -> Unit)? = null

    fun conectarLancador(lancador: (String) -> Unit) {
        this.lancador = lancador
    }

    override fun concedida(): Boolean = NotificationManagerCompat.from(context).areNotificationsEnabled()

    override fun solicitar(aoResultado: (Boolean) -> Unit) {
        if (!precisaPedir()) {
            aoResultado(concedida())
            return
        }

        val lancar = lancador

        if (lancar == null) {
            aoResultado(concedida())
            return
        }

        pendente = aoResultado
        lancar(Manifest.permission.POST_NOTIFICATIONS)
    }

    fun aoResultadoDaSolicitacao(concedida: Boolean) {
        val resultado = pendente
        pendente = null
        resultado?.invoke(concedida && NotificationManagerCompat.from(context).areNotificationsEnabled())
    }
}

private fun semPermissaoDeNotificacao(context: Context): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED

@Composable
fun rememberPermissaoDeNotificacao(): PermissaoDeNotificacao {
    val context = LocalContext.current
    val permissao = remember(context) { PermissaoDeNotificacaoDoAndroid(context) }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = permissao::aoResultadoDaSolicitacao,
    )

    LaunchedEffect(permissao, launcher) { permissao.conectarLancador(launcher::launch) }

    return permissao
}
