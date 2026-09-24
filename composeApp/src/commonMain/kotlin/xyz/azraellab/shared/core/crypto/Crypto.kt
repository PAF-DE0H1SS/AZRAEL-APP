package xyz.azraellab.shared.core.crypto

import kotlin.random.Random

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
}

data class KeyPairData(val publicKey: ByteArray, val privateKey: ByteArray)

fun randomBytes(size: Int): ByteArray = ByteArray(size).also { b ->
    Random.Default.nextBytes(b)
}