package xyz.azraellab.shared.data

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

internal fun JsonObject.s(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull
internal fun JsonObject.l(key: String): Long? = this[key]?.jsonPrimitive?.longOrNull
internal fun JsonObject.i(key: String): Int? = this[key]?.jsonPrimitive?.intOrNull
internal fun JsonObject.b(key: String): Boolean = this[key]?.jsonPrimitive?.booleanOrNull ?: false
internal fun JsonObject.o(key: String): JsonObject? = this[key]?.jsonObject
internal fun JsonObject.a(key: String): List<JsonElement> = this[key]?.let { el ->
    if (el is JsonArray) el.toList() else emptyList()
} ?: emptyList()

@Suppress("unused")
internal fun JsonElement.jsonObjOrNull(): JsonObject? = (this as? JsonObject) ?: runCatching { jsonObject }.getOrNull()