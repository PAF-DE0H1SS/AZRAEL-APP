package xyz.azraellab.shared.core.protocol

import java.net.HttpURLConnection
import java.net.URL

/**
 * Соединение без keep-alive: HttpURLConnection переиспользует сокет из пула, но
 * Cloudflare/HTTP2 закрывает его сам, и следующий запрос уходит в уже мёртвый сокет —
 * запись проходит «в никуда», а чтение висит до readTimeout (20 с) и выглядит как
 * «network: no response». Каждый запрос канала — отдельный подписанный конверт, выигрыша
 * от keep-alive здесь нет, поэтому соединение закрываем явно.
 */
private fun openOnce(url: String, timeoutMs: Int): HttpURLConnection =
    (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = timeoutMs
        readTimeout = timeoutMs
        setRequestProperty("Connection", "close")
        // Редиректы не следуем: подпись привязана к телу и полям конверта,
        // а ответ, полученный с другого хоста, доверия не заслуживает.
        instanceFollowRedirects = false
    }

/** Потолок ответа: подпись проверяется целиком, а мусорный/huge-body нам не нужен. */
private const val MAX_RESPONSE_BYTES = 1 shl 20

/** Читает тело с жёстким потолком, чтобы «ответ на 2 ГБ» не съел память. */
private fun readLimited(conn: HttpURLConnection): String? = runCatching {
    val declared = conn.contentLengthLong
    if (declared > MAX_RESPONSE_BYTES) return@runCatching null
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

// HTTP POST на JVM (desktop): стандартный HttpURLConnection, ответ читается как UTF-8.
actual fun httpPostJson(url: String, body: String, timeoutMs: Int): String? = try {
    val conn = openOnce(url, timeoutMs)
    conn.requestMethod = "POST"
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

// HTTP GET на JVM (desktop): открытая выдача ключа канала при первом запуске.
actual fun httpGetJson(url: String, timeoutMs: Int): String? = try {
    val conn = openOnce(url, timeoutMs)
    conn.requestMethod = "GET"
    conn.setRequestProperty("Accept", "application/json")
    val code = conn.responseCode
    val text = readLimited(conn)
    conn.disconnect()
    if (code in 200..299) text else null
} catch (e: Exception) {
    null
}

// HTTP POST с произвольными заголовками (защищённый конверт кастомного API).
actual fun httpPostJsonWithHeaders(url: String, body: String, headers: Map<String, String>, timeoutMs: Int): HttpResult = try {
    val conn = openOnce(url, timeoutMs)
    conn.requestMethod = "POST"
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

// URL шлюза берётся из переменной окружения AZRAEL_GATEWAY_URL (runtime-конфиг вне репозитория).
actual fun defaultGatewayUrl(): String? =
    AppRuntime.gatewayUrl?.takeIf { it.isNotBlank() } ?: System.getenv("AZRAEL_GATEWAY_URL")?.takeIf { it.isNotBlank() }

// URL кастомного API приложения из AZRAEL_APP_URL; дефолт — как на Android,
// чтобы адрес не приходилось вводить вручную (в сборку он не секрет).
actual fun defaultAppUrl(): String? =
    AppRuntime.appUrl?.takeIf { it.isNotBlank() }
        ?: System.getenv("AZRAEL_APP_URL")?.takeIf { it.isNotBlank() }
        ?: "https://azrael-lab.xyz/api/app/v1"

// Ключ кастомного API приложения из AZRAEL_APP_KEY (base64, как в .env сайта).
actual fun defaultAppKeyB64(): String? =
    AppRuntime.appKeyB64?.takeIf { it.isNotBlank() } ?: System.getenv("AZRAEL_APP_KEY")?.takeIf { it.isNotBlank() }

// Статичный X25519-ключ внутреннего L2-слоя из AZRAEL_APP_SRV_X_PUB (raw-32 base64).
actual fun defaultAppSrvPubB64(): String? =
    AppRuntime.srvXPubB64?.takeIf { it.isNotBlank() } ?: System.getenv("AZRAEL_APP_SRV_X_PUB")?.takeIf { it.isNotBlank() }