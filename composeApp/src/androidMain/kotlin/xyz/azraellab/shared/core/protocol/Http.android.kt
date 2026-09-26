package xyz.azraellab.shared.core.protocol

import java.net.HttpURLConnection
import java.net.URL

// HTTP POST на Android: HttpURLConnection с таймаутами; при не-2xx возвращается null.
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

// HTTP GET на Android: открытая выдача ключа канала при первом запуске.
actual fun httpGetJson(url: String, timeoutMs: Int): String? = try {
    val conn = URL(url).openConnection() as HttpURLConnection
    conn.requestMethod = "GET"
    conn.connectTimeout = timeoutMs
    conn.readTimeout = timeoutMs
    conn.setRequestProperty("Accept", "application/json")
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

// На Android URL шлюза задаётся на странице настроек (не хардкодится).
actual fun defaultGatewayUrl(): String? = AppRuntime.gatewayUrl?.takeIf { it.isNotBlank() }

// На Android адрес API приложения: настройки пользователя, иначе дефолт сборки.
actual fun defaultAppUrl(): String? =
    AppRuntime.appUrl?.takeIf { it.isNotBlank() } ?: "https://azrael-lab.xyz/api/app/v1"

// На Android ключ API приложения задаётся в настройках (секрет, в сборку не вшивается).
actual fun defaultAppKeyB64(): String? = AppRuntime.appKeyB64?.takeIf { it.isNotBlank() }

// Публичный X25519-ключ внутреннего L2-слоя: настройки пользователя, иначе дефолт сборки.
actual fun defaultAppSrvPubB64(): String? =
    AppRuntime.srvXPubB64?.takeIf { it.isNotBlank() } ?: "SIfcrfqv9ri+UhTnQnhi7qEHa3sw+9SnbVpJQRvPTlM="