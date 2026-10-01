package xyz.azraellab.shared.data

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull

// `JsonElement.jsonPrimitive` БРОСАЕТ на object/array, а ответ сервера - это
// наш же словарь: любой ключ может оказаться вложенным объектом, если сервер
// расширит контракт. Раньше такой ответ ронял разбор на ИСКЛЮЧЕНИИ, а не
// отдавал «поля нет». Поэтому приводим тип мягко (`as?`): не-primitive даёт
// null/false, и разбор продолжается. JsonNull - подтип JsonPrimitive, его
// отсекают уже *OrNull-аксессоры (contentOrNull/longOrNull/… возвращают null).
private fun JsonObject.prim(key: String): JsonPrimitive? = this[key] as? JsonPrimitive

internal fun JsonObject.s(key: String): String? = prim(key)?.contentOrNull
internal fun JsonObject.l(key: String): Long? = prim(key)?.longOrNull
internal fun JsonObject.i(key: String): Int? = prim(key)?.intOrNull
internal fun JsonObject.b(key: String): Boolean = prim(key)?.booleanOrNull ?: false
internal fun JsonObject.o(key: String): JsonObject? = this[key] as? JsonObject
internal fun JsonObject.a(key: String): List<JsonElement> = this[key]?.let { el ->
    if (el is JsonArray) el.toList() else emptyList()
} ?: emptyList()

@Suppress("unused")
internal fun JsonElement.jsonObjOrNull(): JsonObject? = (this as? JsonObject) ?: runCatching { jsonObject }.getOrNull()