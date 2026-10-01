package xyz.azraellab.shared.core.protocol

import xyz.azraellab.shared.core.crypto.Base64Codec
import xyz.azraellab.shared.core.crypto.Crypto
import xyz.azraellab.shared.core.crypto.KeyPairData
import xyz.azraellab.shared.core.crypto.randomBytes

/**
 * Локальная сессия защищённого канала. Не содержит никаких секретов/адресов -
 * только криптографическое состояние. Транспорт (URL/HTTP) инжектится снаружи.
 */
class SessionBox {
    val keyPair: KeyPairData = Crypto.keyPair()
    private var serverPublicKey: ByteArray? = null
    private var sessionToken: String = ""

    val hasSession: Boolean get() = sessionToken.isNotEmpty() && serverPublicKey != null

    /**
     * Собирает конверт рукопожатия. Необязательное поле `auth` - site-сессия:
     * если она валидна, сервер повышает роль канала (guest → standard/admin)
     * и отдаёт её в ответе init.
     */
    fun startHandshake(auth: String? = null): Envelope {
        val eph = b64(keyPair.publicKey)
        val payloadJson = if (auth.isNullOrBlank()) """{"eph_pub":"$eph"}"""
        else """{"eph_pub":"$eph","auth":"${auth.replace("\"", "\\\"")}"}"""
        return Envelope(
            id = newId(),
            ts = nowSec(),
            op = Protocol.OP_SESSION_INIT,
            session = "",
            payload = b64(payloadJson.toByteArray())
        )
    }

    fun acceptServer(serverPubB64: String, token: String): Boolean {
        if (serverPubB64.isEmpty() || token.isEmpty()) return false
        if (token.length > 512) return false
        val raw = try {
            Base64Codec.decode(serverPubB64)
        } catch (e: Exception) { return false }
        // Сервер шлёт SPKI-DER (44 байта); X25519 нужен raw-32 (хвост ключа).
        val pub = if (raw.size > 32) raw.copyOfRange(raw.size - 32, raw.size) else raw
        // Принимаем только raw-32: иначе мусорный публичный ключ уходит в ECDH
        // и ошибка всплывает позже, внутри seal(), в невнятном виде.
        if (pub.size != 32) return false
        // All-zero публичный ключ X25519 даёт нулевой shared secret (RFC 7748).
        if (pub.all { it == 0.toByte() }) return false
        serverPublicKey = pub
        sessionToken = token
        return true
    }

    // Зашифрованный запрос: AAD детерминирован из полей конверта (op|id|ts|version),
    // поэтому сервер может проверить целостность атрибутов без знания счётчика клиента.
    fun seal(op: String, plainBody: ByteArray): Envelope? {
        val serverPub = serverPublicKey ?: return null
        if (!hasSession) return null
        // Крипто-примитивы бросают исключения на некорректном вводе; наружу
        // должен уходить null, а не падение на потоке вызова.
        val shared = runCatching { Crypto.sharedSecret(keyPair.privateKey, serverPub) }.getOrNull() ?: return null
        if (shared.isEmpty() || shared.all { it == 0.toByte() }) return null
        val nonce = randomBytes(12)
        val id = newId()
        val ts = nowSec()
        val aad = aad(op, id, ts)
        val tag = Crypto.encrypt(shared, aad, plainBody, nonce) ?: return null
        return Envelope(
            id = id,
            ts = ts,
            op = op,
            session = sessionToken,
            payload = b64(tag),
            nonce = b64(nonce)
        )
    }

    // Расшифровка ответа сервера: тот же детерминированный AAD из полей конверта.
    fun open(env: Envelope): ByteArray? {
        val serverPub = serverPublicKey ?: return null
        val shared = runCatching { Crypto.sharedSecret(keyPair.privateKey, serverPub) }.getOrNull() ?: return null
        if (shared.isEmpty() || shared.all { it == 0.toByte() }) return null
        val cipher = try { Base64Codec.decode(env.payload) } catch (e: Exception) { return null }
        val nonce = try { Base64Codec.decode(env.nonce) } catch (e: Exception) { return null }
        val aad = aad(env.op, env.id, env.ts)
        return Crypto.decrypt(shared, aad, cipher, nonce)
    }

    private fun aad(op: String, id: String, ts: Long): ByteArray =
        listOf(op, id, ts.toString(), Protocol.VERSION.toString()).joinToString("|").toByteArray()

    private fun newId(): String {
        val r = randomBytes(16)
        return b64(r).replace("+", "-").replace("/", "_").substring(0, 22)
    }

    private fun nowSec(): Long = (System.currentTimeMillis() / 1000)
}

private fun b64(bytes: ByteArray): String = Base64Codec.encode(bytes)