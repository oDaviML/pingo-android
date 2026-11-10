package com.pingo.core.data.impl

import com.pingo.core.model.Conta
import com.pingo.core.model.Instrumento

internal data class ContaCriada(val conta: Conta, val instrumentos: List<Instrumento>)
