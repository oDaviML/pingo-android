package com.pingo.support

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File

fun capturarTelaSeSolicitado(contexto: Context, composeRule: ComposeTestRule, nome: String) {
    val tema = InstrumentationRegistry.getArguments().getString("pingo.capturarScreenshots")
        ?.takeIf { it.isNotBlank() }
        ?: return
    composeRule.waitForIdle()
    val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot() ?: return
    val pasta = File(contexto.getExternalFilesDir(null), "screenshots").apply { mkdirs() }
    File(pasta, "$nome-$tema.png").outputStream().use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    }
    bitmap.recycle()
}
