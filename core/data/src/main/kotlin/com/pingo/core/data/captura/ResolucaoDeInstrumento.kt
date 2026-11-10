package com.pingo.core.data.captura

import com.pingo.core.model.Instrumento
import com.pingo.core.model.MotivoPendencia

internal sealed interface ResolucaoDeInstrumento {
    val instrumento: Instrumento?

    val motivo: MotivoPendencia?

    data class Resolvido(override val instrumento: Instrumento) : ResolucaoDeInstrumento {
        override val motivo: MotivoPendencia? = null
    }

    data object Ambiguo : ResolucaoDeInstrumento {
        override val instrumento: Instrumento? = null

        override val motivo: MotivoPendencia = MotivoPendencia.INSTRUMENTO_AMBIGUO
    }

    data object NaoEncontrado : ResolucaoDeInstrumento {
        override val instrumento: Instrumento? = null

        override val motivo: MotivoPendencia = MotivoPendencia.INSTRUMENTO_NAO_ENCONTRADO
    }

    companion object {
        fun de(digitos: String?, instrumentos: List<Instrumento>): ResolucaoDeInstrumento {
            if (digitos == null) {
                return when {
                    instrumentos.size == 1 -> Resolvido(instrumentos.single())
                    instrumentos.size > 1 -> Ambiguo
                    else -> NaoEncontrado
                }
            }

            val correspondentes = instrumentos.filter { instrumento -> instrumento.ultimosDigitos == digitos }

            return when {
                correspondentes.size == 1 -> Resolvido(correspondentes.single())
                correspondentes.size > 1 -> Ambiguo
                else -> NaoEncontrado
            }
        }
    }
}
