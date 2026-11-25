package com.pingo.core.data.captura

import com.pingo.core.model.Instrumento
import com.pingo.core.model.MotivoPendencia
import com.pingo.core.model.TipoInstrumento

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
        fun de(digitos: String?, instrumentos: List<Instrumento>, tipoEsperado: TipoInstrumento? = null): ResolucaoDeInstrumento {
            if (digitos == null) {
                val candidatos = if (tipoEsperado != null) {
                    instrumentos.filter { it.tipo == tipoEsperado }
                } else {
                    instrumentos
                }
                return when {
                    candidatos.size == 1 -> Resolvido(candidatos.single())
                    candidatos.size > 1 -> Ambiguo
                    else -> NaoEncontrado
                }
            }

            val porDigitos = instrumentos.filter { instrumento -> instrumento.ultimosDigitos == digitos }
            val correspondentes = if (porDigitos.size > 1 && tipoEsperado != null) {
                porDigitos.filter { it.tipo == tipoEsperado }
            } else {
                porDigitos
            }

            return when {
                correspondentes.size == 1 -> Resolvido(correspondentes.single())
                correspondentes.size > 1 -> Ambiguo
                else -> NaoEncontrado
            }
        }
    }
}
