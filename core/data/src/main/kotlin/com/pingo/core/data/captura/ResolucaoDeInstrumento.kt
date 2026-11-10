package com.pingo.core.data.captura

import com.pingo.core.model.Instrumento
import com.pingo.core.model.MotivoPendencia

internal sealed interface ResolucaoDeInstrumento {
    val instrumento: Instrumento

    val motivo: MotivoPendencia?

    data class Resolvido(override val instrumento: Instrumento) : ResolucaoDeInstrumento {
        override val motivo: MotivoPendencia? = null
    }

    data class Ambiguo(override val instrumento: Instrumento) : ResolucaoDeInstrumento {
        override val motivo: MotivoPendencia = MotivoPendencia.INSTRUMENTO_AMBIGUO
    }

    data class NaoEncontrado(override val instrumento: Instrumento) : ResolucaoDeInstrumento {
        override val motivo: MotivoPendencia = MotivoPendencia.INSTRUMENTO_NAO_ENCONTRADO
    }

    companion object {
        fun de(digitos: String?, instrumentos: List<Instrumento>): ResolucaoDeInstrumento {
            require(instrumentos.isNotEmpty()) { "resolução de instrumento exige ao menos um instrumento" }

            if (digitos == null) {
                return if (instrumentos.size == 1) Resolvido(instrumentos.single()) else Ambiguo(instrumentos.first())
            }

            val correspondentes = instrumentos.filter { instrumento -> instrumento.ultimosDigitos == digitos }

            return when {
                correspondentes.size == 1 -> Resolvido(correspondentes.single())
                correspondentes.size > 1 -> Ambiguo(correspondentes.first())
                else -> NaoEncontrado(instrumentos.first())
            }
        }
    }
}
