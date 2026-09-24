package xyz.azraellab.shared.core.protocol

import java.net.HttpURLConnection
import java.net.URL

// HTTP POST на JVM (desktop): стандартный HttpURLConnection, ответ читается как UTF-8.
actual fun httpPostJson(url: String, body: String, timeoutMs: Int): String? = try {
    val conn = URL(url).openConnection() as HttpURLConnection
    conn.requestMethod = "POST"
    conn.connectTimeout = timeoutMs
    conn.readTimeout = timeoutMs
    conn.setRequestProperty("Content-Type", "application/json")
    conn.doOutput = true
    conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
    val code = conn.responseCode
    val stream = if (code in 200..299) conn.inputStream else conn.errorStream
    val text = stream?.use { String(it.readBytes(), Charsets.UTF_8) }.orEmpty()
    conn.disconnect()
    if (code in 200..299) text else null
} catch (e: Exception) {
    null
}

// HTTP POST с произвольными заголовками (защищённый конверт кастомного API).
actual fun httpPostJsonWithHeaders(url: String, body: String, headers: Map<String, String>, timeoutMs: Int): HttpResult = try {
    val conn = URL(url).openConnection() as HttpURLConnection
    conn.requestMethod = "POST"
    conn.connectTimeout = timeoutMs
    conn.readTimeout = timeoutMs
    conn.setRequestProperty("Content-Type", "application/json")
    headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }
    conn.doOutput = true
    conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
    val status = conn.responseCode
    val map = LinkedHashMap<String, String>()
    conn.headerFields.forEach { (name, values) ->
        if (name != null && values.isNotEmpty()) map[name.lowercase()] = values[0]
    }
    val stream = if (status in 200..299) conn.inputStream else conn.errorStream
    val text = stream?.use { String(it.readBytes(), Charsets.UTF_8) }.orEmpty()
    conn.disconnect()
    HttpResult(text, status, map)
} catch (e: Exception) {
    HttpResult(null, 0, emptyMap())
}

// URL шлюза берётся из переменной окружения AZRAEL_GATEWAY_URL (runtime-конфиг вне репозитория).
actual fun defaultGatewayUrl(): String? = System.getenv("AZRAEL_GATEWAY_URL")?.takeIf { it.isNotBlank() }

// URL кастомного API приложения из AZRAEL_APP_URL.
actual fun defaultAppUrl(): String? = System.getenv("AZRAEL_APP_URL")?.takeIf { it.isNotBlank() }