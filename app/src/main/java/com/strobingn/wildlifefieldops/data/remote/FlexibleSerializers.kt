package com.strobingn.wildlife.data.remote

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

/**
 * Supabase/PostgREST often returns numbers as JSON numbers, but some rows store
 * them as strings. Crashes during sync decode if the type is strict.
 */
object FlexibleStringSerializer : KSerializer<String?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FlexibleString", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String? {
        val jsonDecoder = decoder as? JsonDecoder ?: return decoder.decodeString()
        val element = jsonDecoder.decodeJsonElement()
        if (element is JsonNull) return null
        val prim = element as? JsonPrimitive ?: return element.toString()
        if (prim.isString) return prim.content
        return prim.content // number/boolean → text
    }

    override fun serialize(encoder: Encoder, value: String?) {
        val jsonEncoder = encoder as? JsonEncoder
        if (jsonEncoder != null) {
            if (value == null) jsonEncoder.encodeJsonElement(JsonNull)
            else jsonEncoder.encodeJsonElement(JsonPrimitive(value))
        } else if (value != null) {
            encoder.encodeString(value)
        }
    }
}

object FlexibleDoubleSerializer : KSerializer<Double?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FlexibleDouble", PrimitiveKind.DOUBLE)

    override fun deserialize(decoder: Decoder): Double? {
        val jsonDecoder = decoder as? JsonDecoder ?: return try {
            decoder.decodeDouble()
        } catch (_: Exception) {
            null
        }
        val element = jsonDecoder.decodeJsonElement()
        if (element is JsonNull) return null
        val prim = element as? JsonPrimitive ?: return null
        return prim.doubleOrNull
            ?: prim.longOrNull?.toDouble()
            ?: prim.content.toDoubleOrNull()
    }

    override fun serialize(encoder: Encoder, value: Double?) {
        val jsonEncoder = encoder as? JsonEncoder
        if (jsonEncoder != null) {
            if (value == null) jsonEncoder.encodeJsonElement(JsonNull)
            else jsonEncoder.encodeJsonElement(JsonPrimitive(value))
        } else if (value != null) {
            encoder.encodeDouble(value)
        }
    }
}
