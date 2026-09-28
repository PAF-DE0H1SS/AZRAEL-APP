package xyz.azraellab.shared.core.crypto

expect object Crypto {

    /** Генерация X25519 keypair. Пара [seed] опциональна (детерминированные тесты). */
    fun keyPair(seed: ByteArray? = null): KeyPairData

    /** ECDH shared secret между [priv] (своей стороной) и [pub] (чужой). */
    fun sharedSecret(priv: ByteArray, pub: ByteArray): ByteArray

    /** AEAD-encrypt (AES-256-GCM или аналог платформы). Возвращает ciphertext|null. */
    fun encrypt(key: ByteArray, aad: ByteArray, plain: ByteArray, nonce: ByteArray): ByteArray?

    /** AEAD-decrypt. Возвращает plain|null при неудаче. */
    fun decrypt(key: ByteArray, aad: ByteArray, cipher: ByteArray, nonce: ByteArray): ByteArray?

    /** HMAC-SHA256 для подписи запросов/ответов кастомного API. */
    fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray

    /** SHA-256 для детерминированного хэша тела (анти-подмена). */
    fun sha256(data: ByteArray): ByteArray

    /** HKDF-SHA256 (RFC 5869) — зеркалит lib/app-l2.ts (Extract+Expand, L=32). */
    fun hkdfSha256(ikm: ByteArray, salt: ByteArray, info: ByteArray, length: Int): ByteArray

    /** Ed25519 keypair; публичный и приватный ключи — raw-32 (как в device API сервера). */
    fun ed25519KeyPair(seed: ByteArray? = null): KeyPairData

    /** Ed25519-подпись raw-32 приватным ключом; возвращает 64 байта. */
    fun ed25519Sign(privKey: ByteArray, message: ByteArray): ByteArray

    /** X25519 keypair в raw-32 (публичный и приватный) — для L2 эфемерного ключа. */
    fun x25519KeyPairRaw(seed: ByteArray? = null): KeyPairData

    /** Общий секрет X25519 по raw-32 ключам (RFC 7748 little-endian u-координата). */
    fun x25519SharedRaw(privKey: ByteArray, pubKey: ByteArray): ByteArray

    /** CSPRNG: криптостойкие байты для nonce, id, session token и ключей. */
    fun randomBytes(size: Int): ByteArray
}

data class KeyPairData(val publicKey: ByteArray, val privateKey: ByteArray)

/** Конкатенация байтовых массивов (для HKDF Expand). */
fun concatBytes(vararg arrays: ByteArray): ByteArray {
    var size = 0
    for (a in arrays) size += a.size
    val out = ByteArray(size)
    var pos = 0
    for (a in arrays) {
        a.copyInto(out, pos)
        pos += a.size
    }
    return out
}

/** Все случайные значения в клиенте идут через платформенный SecureRandom. */
fun randomBytes(size: Int): ByteArray = Crypto.randomBytes(size)