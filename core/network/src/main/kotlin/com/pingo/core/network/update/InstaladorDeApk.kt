package com.pingo.core.network.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InstaladorDeApk @Inject constructor(@ApplicationContext private val context: Context) {
    fun podeInstalarPacotes(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    fun intentPermissaoFontesDesconhecidas(): Intent = Intent(
        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
        Uri.parse("package:${context.packageName}"),
    )

    fun intentInstalacao(arquivoApk: File): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}$SUFIXO_FILEPROVIDER",
            arquivoApk,
        )
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, MIME_TYPE_APK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    companion object {
        private const val SUFIXO_FILEPROVIDER = ".apkupdate.fileprovider"
        private const val MIME_TYPE_APK = "application/vnd.android.package-archive"
    }
}
