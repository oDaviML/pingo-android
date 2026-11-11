package com.pingo.core.notifications

import android.os.Build
import javax.inject.Inject
import javax.inject.Singleton

interface FabricanteDoAparelho {
    fun fabricante(): FabricanteDoSistema
}

@Singleton
class FabricanteDoAparelhoDoAndroid @Inject constructor() : FabricanteDoAparelho {
    override fun fabricante(): FabricanteDoSistema = FabricanteDoSistema.de(Build.MANUFACTURER)
}
