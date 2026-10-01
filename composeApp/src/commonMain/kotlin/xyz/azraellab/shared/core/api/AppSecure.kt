package xyz.azraellab.shared.core.api

import xyz.azraellab.shared.core.crypto.Base64Codec
import xyz.azraellab.shared.core.crypto.Crypto
import xyz.azraellab.shared.core.crypto.randomBytes

/**
 * Защищённый конверт кастомного API (программа ↔ сайт, поверх HTTPS):
 *
 * 1. Шифрование тела - AES-256-GCM по ключу приложения [AppKey] (AAD привязывает ts|nonce).
 * 2. Подпись запроса - HMAC-SHA256("V1|ts|nonce|b64(sha256(ct))").
 * 3. Anti-replay - одноразовый nonce, который сервер погашает через Redis SETNX, + окно ts ±120с.
 * 4. Двусторонняя аутентификация - сайт подписывает ответ тем же ключом, клиент проверяет.
 */
object AppSecure {
    const val VERSION = 1
    const val TS_SKEW_SEC = 120L
    const val IV_SIZE = 12
    const val NONCE_SIZE = 16

    const val HDR_TS = "x-azrael-ts"
    const val HDR_NONCE = "x-azrael-nonce"
    const val HDR_SIG = "x-azrael-sig"
    const val HDR_VERSION = "x-azrael-version"
    const val HDR_KG = "x-azrael-kg"           // поколение ключа (ротация)
    // devId установки, которой выдан ключ канала: сервер по нему пересчитывает
    // per-install ключ вместо того, чтобы держать выданные ключи в базе.
    const val HDR_KID = "x-azrael-kid"

    // Трёхсторонний конверт (L2): клиент ↔ сайт ↔ внутренний роут /v2/l2.
    const val HDR_L2 = "x-azrael-l2"           // "1" - запрос требует вскрытия на внутреннем слое
    const val HDR_EPH = "x-azrael-eph"         // raw-32 X25519 баз64 публичный эфемерный ключ клиента

    // Авторизация установки: Ed25519-подпись raw-текста тела (заголовки кастомного API).
    const val HDR_DEV = "x-azrael-dev"
    const val HDR_DEV_TS = "x-azrael-dev-ts"
    const val HDR_DEV_SIG = "x-azrael-dev-sig"

    // Серверное время в КАЖДОМ ответе - для синхронизации часов клиента (x-azrael-srv-ts).
    const val SRV_TS = "x-azrael-srv-ts"

    const val RESP_TS = "x-azrael-resp-ts"
    const val RESP_RID = "x-azrael-resp-rid"
    const val RESP_SIG = "x-azrael-resp-sig"
    const val RESP_ROT_GEN = "x-azrael-rot-gen"     // сервер передаёт следующее поколение
    const val RESP_ROT_SALT = "x-azrael-rot-salt"   //   (только в подписанном ответе)
    const val RESP_POISON = "x-azrael-poison"       // ключ/поколение скомпрометированы →
    const val RESP_TRAP = "x-azrael-trap"           //   устройство отозвано администратором →
    const val RESP_RELEASE = "x-azrael-release"     // канал возвращён (разблокировка с сайта)

    data class SealedRequest(val ts: Long, val nonce: String, val sig: String, val body: String)

    /** Ключ приложения из base64-строки (пусто → null, невалидная строка → null). */
    fun appKeyFromB64(b64: String?): ByteArray? {
        if (b64.isNullOrBlank()) return null
        return runCatching { Base64Codec.decode(b64.trim()) }.getOrNull()
    }

    /**
     * Ключ поколения g (зеркало сервера): K(g) = HMAC-SHA256(anchor, "AZRAEL-APP|gen|g|s=salt").
     * Для g=1 без соли K(1) = якорь (обратная совместимость). Соль следующего поколения
     * клиент получает ТОЛЬКО из подписанного ответа (RESP_ROT_*) - украденный
     * промежуточный ключ не позволяет выводить будущие поколения вперёд.
     */
    fun deriveGenerationKey(anchor: ByteArray, g: Long, saltB64: String): ByteArray {
        if (g == 1L && saltB64.isEmpty()) return anchor
        val msg = "AZRAEL-APP|gen|$g|s=$saltB64".toByteArray(Charsets.UTF_8)
        return Crypto.hmacSha256(anchor, msg)
    }

    /** Проверка маркеров «отравления» в ответе: poison/trap - вызов защиты. */
    fun isResponsePoisoned(resp: xyz.azraellab.shared.core.protocol.HttpResult): Boolean {
        return resp.headers[RESP_POISON] == "1" || resp.headers[RESP_TRAP] == "1"
    }

    /** Маркер разблокировки (возврат канала администратором с сайта). */
    fun isResponseRelease(resp: xyz.azraellab.shared.core.protocol.HttpResult): Boolean =
        resp.headers[RESP_RELEASE] == "1"

    /** Следующее поколение из заголовков ротации (gen + salt), либо null. */
    fun nextGeneration(resp: xyz.azraellab.shared.core.protocol.HttpResult): Pair<Long, String>? {
        val g = resp.headers[RESP_ROT_GEN]?.toLongOrNull() ?: return null
        val salt = resp.headers[RESP_ROT_SALT] ?: return null
        return g to salt
    }

    /** Зашифровать JSON-конверт {v,op,args,session} + собрать заголовки подписи. */
    fun seal(key: ByteArray, plainJson: String): SealedRequest = seal(key, plainJson, nowSec(), b64(randomBytes(NONCE_SIZE)))

    /**
     * Зашифровать конверт с ЯВНОЙ меткой времени [ts] (может быть смещена синхронизацией
     * часов с сервера). Envelope наружу та же, ts из [SealedRequest.ts] попадает в заголовок.
     */
    fun seal(key: ByteArray, plainJson: String, ts: Long): SealedRequest =
        seal(key, plainJson, ts, b64(randomBytes(NONCE_SIZE)))

    /**
     * Продвинутая печать с фиксированными ts и nonce: нужна трёхстороннему конверту
     * (L2), где nonce обязан быть известен ДО сборки inner-аргументов, а время - общим
     * для всех слоёв. Вызывающий обязан передавать криптостойкий уникальный nonce.
     */
    fun seal(key: ByteArray, plainJson: String, ts: Long, nonce: String): SealedRequest {
        val iv = randomBytes(IV_SIZE)
        val aad = aad(ts, nonce)
        val ct = Crypto.encrypt(key, aad, plainJson.toByteArray(Charsets.UTF_8), iv) ?: error("app seal failed")
        val ctB64 = b64(ct)
        val body = """{"v":$VERSION,"iv":"${b64(iv)}","ct":"$ctB64"}"""
        val sig = signRequest(key, ts, nonce, ctB64)
        return SealedRequest(ts, nonce, sig, body)
    }

    /** Проверка подписи ответа сайта (аутентификация сервера без доверия к DNS/прокси). */
    fun verifyResponse(key: ByteArray, resp: xyz.azraellab.shared.core.protocol.HttpResult): Boolean =
        verifyResponse(key, resp, nowSec())

    /** То же, но с переданным текущим временем (учёт смещения часов x-azrael-srv-ts). */
    fun verifyResponse(key: ByteArray, resp: xyz.azraellab.shared.core.protocol.HttpResult, now: Long): Boolean {
        val body = resp.body ?: return false
        val ts = resp.headers[RESP_TS]?.toLongOrNull() ?: return false
        val rid = resp.headers[RESP_RID] ?: return false
        val sig = resp.headers[RESP_SIG] ?: return false
        if (Math.abs(now - ts) > TS_SKEW_SEC) return false
        val expect = b64(Crypto.hmacSha256(key, "V1|$ts|$rid|${b64(Crypto.sha256(body.toByteArray(Charsets.UTF_8)))}".toByteArray()))
        return constEq(expect, sig)
    }

    private fun signRequest(key: ByteArray, ts: Long, nonce: String, ctB64: String): String =
        b64(Crypto.hmacSha256(key, "V1|$ts|$nonce|${b64(Crypto.sha256(ctB64.toByteArray(Charsets.UTF_8)))}".toByteArray()))

    private fun aad(ts: Long, nonce: String): ByteArray =
        "AZRAEL-APP|$ts|$nonce".toByteArray(Charsets.UTF_8)

    private fun nowSec(): Long = System.currentTimeMillis() / 1000

    private fun b64(b: ByteArray): String = Base64Codec.encode(b)

    // Константное сравнение (без раннего выхода) для подписей.
    private fun constEq(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var r = 0
        for (i in a.indices) r = r or (a[i].code xor b[i].code)
        return r == 0
    }
}