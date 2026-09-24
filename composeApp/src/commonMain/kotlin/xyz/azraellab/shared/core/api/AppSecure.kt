package xyz.azraellab.shared.core.api

import xyz.azraellab.shared.core.crypto.Base64Codec
import xyz.azraellab.shared.core.crypto.Crypto
import xyz.azraellab.shared.core.crypto.randomBytes

/**
 * Защищённый конверт кастомного API (программа ↔ сайт, поверх HTTPS):
 *
 * 1. Шифрование тела — AES-256-GCM по ключу приложения [AppKey] (AAD привязывает ts|nonce).
 * 2. Подпись запроса — HMAC-SHA256("V1|ts|nonce|b64(sha256(ct))").
 * 3. Anti-replay — одноразовый nonce, который сервер погашает через Redis SETNX, + окно ts ±120с.
 * 4. Двусторонняя аутентификация — сайт подписывает ответ тем же ключом, клиент проверяет.
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

    const val RESP_TS = "x-azrael-resp-ts"
    const val RESP_RID = "x-azrael-resp-rid"
    const val RESP_SIG = "x-azrael-resp-sig"

    data class SealedRequest(val ts: Long, val nonce: String, val sig: String, val body: String)

    /** Зашифровать JSON-конверт {v,op,args,session} + собрать заголовки подписи. */
    fun seal(key: ByteArray, plainJson: String): SealedRequest {
        val ts = nowSec()
        val nonce = b64(randomBytes(NONCE_SIZE))
        val iv = randomBytes(IV_SIZE)
        val aad = aad(ts, nonce)
        val ct = Crypto.encrypt(key, aad, plainJson.toByteArray(Charsets.UTF_8), iv) ?: error("app seal failed")
        val ctB64 = b64(ct)
        val body = """{"v":$VERSION,"iv":"${b64(iv)}","ct":"$ctB64"}"""
        val sig = signRequest(key, ts, nonce, ctB64)
        return SealedRequest(ts, nonce, sig, body)
    }

    /** Проверка подписи ответа сайта (аутентификация сервера без доверия к DNS/прокси). */
    fun verifyResponse(key: ByteArray, resp: xyz.azraellab.shared.core.protocol.HttpResult): Boolean {
        val body = resp.body ?: return false
        val ts = resp.headers[RESP_TS]?.toLongOrNull() ?: return false
        val rid = resp.headers[RESP_RID] ?: return false
        val sig = resp.headers[RESP_SIG] ?: return false
        if (Math.abs(nowSec() - ts) > TS_SKEW_SEC) return false
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