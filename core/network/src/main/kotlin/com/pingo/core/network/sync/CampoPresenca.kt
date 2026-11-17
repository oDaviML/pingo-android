package com.pingo.core.network.sync

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.nullable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

@Serializable(with = CampoPresencaSerializer::class)
class CampoPresenca<T : Any> private constructor(val presente: Boolean, val valor: T?) {
    fun resolver(atual: () -> T?): T? = if (presente) valor else atual()

    override fun equals(other: Any?): Boolean = other is CampoPresenca<*> && presente == other.presente && valor == other.valor

    override fun hashCode(): Int = 31 * presente.hashCode() + (valor?.hashCode() ?: 0)

    companion object {
        fun <T : Any> ausente(): CampoPresenca<T> = CampoPresenca(presente = false, valor = null)

        fun <T : Any> de(valor: T?): CampoPresenca<T> = CampoPresenca(presente = true, valor = valor)
    }
}

class CampoPresencaSerializer<T : Any>(private val serializador: KSerializer<T>) : KSerializer<CampoPresenca<T>> {
    override val descriptor: SerialDescriptor = serializador.descriptor.nullable

    override fun deserialize(decoder: Decoder): CampoPresenca<T> {
        val json = decoder as? JsonDecoder ?: throw SerializationException("CampoPresenca exige JSON")

        val elemento = json.decodeJsonElement()

        if (elemento is JsonNull) {
            return CampoPresenca.de(null)
        }

        return CampoPresenca.de(json.json.decodeFromJsonElement(serializador, elemento))
    }

    override fun serialize(encoder: Encoder, value: CampoPresenca<T>) {
        val json = encoder as? JsonEncoder ?: throw SerializationException("CampoPresenca exige JSON")

        val elemento = value.valor?.let { json.json.encodeToJsonElement(serializador, it) } ?: JsonNull

        json.encodeJsonElement(elemento)
    }
}
