package xyz.azraellab.shared.core.protocol

// Платформенный HTTP POST (JSON). Возвращает строку ответа или null при сетевой/HTTP ошибке.
expect fun httpPostJson(url: String, body: String, timeoutMs: Int = 10_000): String?

// Результат HTTP-обмена с заголовками (для защищённого конверта кастомного API).
data class HttpResult(
    val body: String?,
    val status: Int,
    val headers: Map<String, String> = emptyMap()
)

// HTTP POST c произвольными заголовками; возвращает тело, статус и заголовки ответа.
expect fun httpPostJsonWithHeaders(url: String, body: String, headers: Map<String, String>, timeoutMs: Int = 10_000): HttpResult

// Базовый URL шлюза из runtime-конфига (не хардкодится в коде).
expect fun defaultGatewayUrl(): String?

// Базовый URL кастомного API приложения (/api/app/v1) из runtime-конфига.
expect fun defaultAppUrl(): String?