package xyz.azraellab.shared.core.protocol

import java.net.HttpURLConnection
import java.net.URL
import xyz.azraellab.shared.logAzraelError

// Редиректы не следуем: подпись привязана к телу и полям конверта, а ответ
// с другого хоста доверия не заслуживает. Плюс потолок на размер тела.
private const val MAX_RESPONSE_BYTES = 1 shl 20

private fun openOnce(url: String, method: String, timeoutMs: Int): HttpURLConnection =
    (URL(url).openConnection() as HttpURLConnection).apply {
        requestMethod = method
        connectTimeout = timeoutMs
        readTimeout = timeoutMs
        instanceFollowRedirects = false
    }

/** Читает тело с жёстким потолком, чтобы «ответ на 2 ГБ» не съел память. */
private fun readLimited(conn: HttpURLConnection): String? = runCatching {
    if (conn.contentLengthLong > MAX_RESPONSE_BYTES) return@runCatching null
    val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
        ?: return@runCatching ""
    stream.use { input ->
        val buf = java.io.ByteArrayOutputStream()
        val chunk = ByteArray(8 * 1024)
        var total = 0
        while (true) {
            val n = input.read(chunk)
            if (n < 0) break
            total += n
            if (total > MAX_RESPONSE_BYTES) return@runCatching null
            buf.write(chunk, 0, n)
        }
        String(buf.toByteArray(), Charsets.UTF_8)
    }
}.getOrNull()

// HTTP POST на Android: HttpURLConnection с таймаутами; при не-2xx возвращается null.
actual fun httpPostJson(url: String, body: String, timeoutMs: Int): String? = try {
    val conn = openOnce(url, "POST", timeoutMs)
    conn.setRequestProperty("Content-Type", "application/json")
    conn.doOutput = true
    conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
    val code = conn.responseCode
    val text = readLimited(conn)
    conn.disconnect()
    if (code in 200..299) text else null
} catch (e: Exception) {
    null
}

// HTTP GET на Android: открытая выдача ключа канала при первом запуске.
actual fun httpGetJson(url: String, timeoutMs: Int): String? = try {
    val conn = openOnce(url, "GET", timeoutMs)
    conn.setRequestProperty("Accept", "application/json")
    val code = conn.responseCode
    val text = readLimited(conn)
    conn.disconnect()
    if (code in 200..299) text else null
} catch (e: Exception) {
    logAzraelError("AzraelHttp", "GET failed: ${e.javaClass.simpleName}: ${e.message}", e)
    null
}

// HTTP POST с произвольными заголовками (защищённый конверт кастомного API).
actual fun httpPostJsonWithHeaders(url: String, body: String, headers: Map<String, String>, timeoutMs: Int): HttpResult = try {
    val conn = openOnce(url, "POST", timeoutMs)
    conn.setRequestProperty("Content-Type", "application/json")
    headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }
    conn.doOutput = true
    conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
    val status = conn.responseCode
    val map = LinkedHashMap<String, String>()
    conn.headerFields.forEach { (name, values) ->
        if (name != null && values.isNotEmpty()) map[name.lowercase()] = values[0]
    }
    val text = readLimited(conn)
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