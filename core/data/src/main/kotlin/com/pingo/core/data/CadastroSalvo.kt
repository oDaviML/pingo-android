package com.pingo.core.data

import com.pingo.core.model.Conta
import com.pingo.core.model.Instrumento

data class CadastroSalvo(val conta: Conta, val instrumentos: List<Instrumento>)
