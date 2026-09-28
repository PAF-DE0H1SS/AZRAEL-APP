package xyz.azraellab.shared.core.protocol

/**
 * Значения конфигурации, заданные пользователем в настроеках приложения.
 * Имеют приоритет над платформенными умолчаниями (env на десктопе, дефолты сборки на Android).
 */
object AppRuntime {
    var gatewayUrl: String? = null
    var appUrl: String? = null
    var appKeyB64: String? = null
    var srvXPubB64: String? = null
}

// Платформенный HTTP POST (JSON). Возвращает строку ответа или null при сетевой/HTTP ошибке.
expect fun httpPostJson(url: String, body: String, timeoutMs: Int = 10_000): String?

// Платформенный HTTP GET (JSON). Нужен для открытой выдачи ключа канала при
// первом запуске (/api/app/bootstrap) — до того, как ключ у программы ещё есть.
expect fun httpGetJson(url: String, timeoutMs: Int = 10_000): String?

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

// Ключ кастомного API (/api/app/v1) из runtime-конфига в base64 (desktop: AZRAEL_APP_KEY;
// android: задаётся в настройках приложения). null — канал без шифрования (только подтверждение).
expect fun defaultAppKeyB64(): String?

// Статичный X25519-ключ (raw-32 base64) внутреннего L2-слоя (сайт НЕ расшифровывает L2 —
// ключ _SRV_X_PUB принадлежит внутреннему роуту /v2/l2). Desktop: AZRAEL_APP_SRV_X_PUB;
// android: задаётся в настройках приложения.
expect fun defaultAppSrvPubB64(): String?