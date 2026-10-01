package xyz.azraellab.shared.core.api

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import xyz.azraellab.shared.core.crypto.Base64Codec
import xyz.azraellab.shared.core.protocol.httpGetJson
import xyz.azraellab.shared.data.s

/**
 * Первичная выдача ключа канала при первом запуске.
 *
 * Пользователь ключ не вводит и не видит: программа сама забирает его один раз
 * с `GET /api/app/bootstrap?devId=…` и кладёт в AppVault. Благодаря этому ключ не
 * лежит в сборке, а его ротация на сервере не требует нового релиза APK.
 *
 * Сервер отдаёт НЕ общий ключ, а производный для этой установки:
 * `A_dev = HMAC(APP_V1_KEY, "AZRAEL-APP|dev|v1|<devId>")`. Мастер остаётся на
 * сервере, поэтому перехват ответа на чужом устройстве не даёт ни этот ключ, ни
 * ключи остальных установок. Тот же devId клиент затем передаёт в заголовке
 * `x-azrael-kid` каждого запроса, а поверх A_dev работает штатная ротация
 * поколений - клиенту мастер не нужен.
 *
 * Зеркало `padKeyBytes()` из site/lib/app-secure.ts. XOR-pad - обфускация, а не
 * криптография: смысл в том, чтобы ключ не лежал в JSON открытым текстом. Секрет
 * канала держит TLS плюс привязка значения к ключу устройства.
 */
object AppKeyBootstrap {

    /** "AZRAEL-v" - байт-в-байт как BOOTSTRAP_PAD на сервере. */
    private val PAD = byteArrayOf(0x41, 0x5a, 0x52, 0x41, 0x45, 0x4c, 0x2d, 0x76)

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Адрес точки выдачи по адресу API: `https://host/api/app/v1` →
     * `https://host/api/app/bootstrap`. Если адрес уже указывает на саму точку или
     * не содержит сегмента API - берём корень сайта.
     *
     * [devId] обязателен: без него сервер не может выдать ключ этой установки и
     * отвечает 400 (выдать общий ключ он больше не вправе).
     */
    fun bootstrapUrl(apiUrl: String, devId: String): String {
        val base = apiUrl.trim().substringBefore("/api/app/").trimEnd('/')
        return "$base/api/app/bootstrap?devId=${devId.trim()}"
    }

    /** base64(XOR pad) → сырые байты ключа; null, если строка не декодируется. */
    fun unpadKey(paddedB64: String): ByteArray? {
        val raw = runCatching { Base64Codec.decode(paddedB64.trim()) }.getOrNull() ?: return null
        if (raw.isEmpty()) return null
        val out = ByteArray(raw.size)
        for (i in raw.indices) out[i] = (raw[i].toInt() xor PAD[i % PAD.size].toInt()).toByte()
        return out
    }

    /**
     * Забрать ключ канала. Возвращает сырые 32 байта или null, если сервер не
     * настроен (`configured: false`), ответил не-200 или ключ не распознан.
     */
    fun fetch(apiUrl: String, devId: String, timeoutMs: Int = 8_000): ByteArray? {
        val text = httpGetJson(bootstrapUrl(apiUrl, devId), timeoutMs) ?: return null
        val obj = runCatching { json.parseToJsonElement(text).jsonObject }.getOrNull() ?: return null
        if (obj.s("ok") != "true") return null
        if (obj.s("configured") != "true") return null
        val key = obj.s("key")?.takeIf { it.isNotBlank() } ?: return null
        val bytes = unpadKey(key) ?: return null
        // Якорь канала - ровно 32 байта (AES-256). Меньше/больше - не наш ключ.
        return bytes.takeIf { it.size == 32 }
    }
}
