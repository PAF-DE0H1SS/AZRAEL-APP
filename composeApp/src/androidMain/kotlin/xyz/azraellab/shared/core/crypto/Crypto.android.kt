package xyz.azraellab.shared.core.crypto

import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.Security
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.XECPublicKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

// Реализация крипто-примитивов для Android (JVM/JCA, синхронна с desktop):
// X25519 ECDH, HKDF-SHA256 (RFC 5869), Ed25519 (raw-32), AEAD AES-256-GCM.
actual object Crypto {

    private val rnd = SecureRandom()

    private val PKCS8_X25519 = byteArrayOf(
        0x30, 0x2e, 0x02, 0x01, 0x00, 0x30, 0x05, 0x06, 0x03, 0x2b, 0x65, 0x6e, 0x03, 0x22, 0x04, 0x20
    )
    private val SPKI_X25519 = byteArrayOf(
        0x30, 0x2a, 0x30, 0x05, 0x06, 0x03, 0x2b, 0x65, 0x6e, 0x03, 0x21, 0x00
    )

    /** Порядок провайдеров фиксирован, чтобы выбор не зависел от Security.getProviders(). */
    private val preferredProviders = listOf("AndroidOpenSSL", "Conscrypt")

    // X25519 кодирует координату little-endian; JCA BigInteger ждёт big-endian,
    // поэтому raw-32 (RFC 7748) разворачиваем перед BigInteger (проверено E2E vs Node).
    private fun rawPubSpec(pub: ByteArray): XECPublicKeySpec = XECPublicKeySpec(
        java.security.spec.NamedParameterSpec.X25519,
        java.math.BigInteger(1, pub.reversedArray())
    )

    /**
     * Генератор пары ключей мимо AndroidKeyStore.
     *
     * `KeyPairGenerator.getInstance("Ed25519")` на Android разрешается в
     * AndroidKeyStore, а тот на программную генерацию отвечает
     * `IllegalStateException: Not initialized` — он умеет только аппаратные ключи.
     * Нам же нужен обычный ключ JCA: сырые 32 байта приватного ключа лежат в
     * vault установки и участвуют в подписи запросов, hardware-объект туда не
     * положить. Поэтому KeyStore-провайдеры отбрасываются, а генерация идёт
     * через BoringSSL/Conscrypt в фиксированном порядке.
     */
    private fun keyPairGenerator(algorithm: String): KeyPairGenerator {
        val usable = Security.getProviders().map { it.name }
            .filterNot { it.contains("KeyStore", ignoreCase = true) }
        val order = preferredProviders.filter { it in usable } +
            usable.filterNot { it in preferredProviders }
        for (name in order) {
            runCatching { KeyPairGenerator.getInstance(algorithm, name) }.getOrNull()?.let { return it }
        }
        return KeyPairGenerator.getInstance(algorithm)
    }

    actual fun keyPair(seed: ByteArray?): KeyPairData {
        val kpg = keyPairGenerator("X25519")
        val kp = kpg.generateKeyPair()
        val priv = kp.private.encoded ?: ByteArray(32).also { rnd.nextBytes(it) }
        val pub = kp.public.encoded ?: ByteArray(32)
        return KeyPairData(publicKey = pub, privateKey = priv)
    }

    actual fun sharedSecret(priv: ByteArray, pub: ByteArray): ByteArray {
        val kf = KeyFactory.getInstance("X25519")
        val privSpec = PKCS8EncodedKeySpec(priv)
        val privKey = kf.generatePrivate(privSpec)
        val pubKey = kf.generatePublic(rawPubSpec(pub))
        val ka = KeyAgreement.getInstance("X25519")
        ka.init(privKey)
        ka.doPhase(pubKey, true)
        return ka.generateSecret()
    }

    actual fun x25519KeyPairRaw(seed: ByteArray?): KeyPairData {
        val kpg = keyPairGenerator("X25519")
        val kp = kpg.generateKeyPair()
        return KeyPairData(
            publicKey = stripPrefix(kp.public.encoded, SPKI_X25519),
            privateKey = stripPrefix(kp.private.encoded, PKCS8_X25519)
        )
    }

    actual fun x25519SharedRaw(privKey: ByteArray, pubKey: ByteArray): ByteArray {
        val kf = KeyFactory.getInstance("X25519")
        val priv = kf.generatePrivate(PKCS8EncodedKeySpec(concatBytes(PKCS8_X25519, privKey)))
        val pub = kf.generatePublic(rawPubSpec(pubKey))
        val ka = KeyAgreement.getInstance("X25519")
        ka.init(priv)
        ka.doPhase(pub, true)
        return ka.generateSecret()
    }

    actual fun ed25519KeyPair(seed: ByteArray?): KeyPairData {
        val s = seed ?: ByteArray(32).also { rnd.nextBytes(it) }
        return KeyPairData(publicKey = Ed25519.publicKeyFromSeed(s), privateKey = s.copyOf())
    }

    actual fun ed25519Sign(privKey: ByteArray, message: ByteArray, publicKey: ByteArray?): ByteArray =
        if (publicKey == null) Ed25519.sign(privKey, message)
        else Ed25519.signWithPublicKey(privKey, publicKey, message)

    actual fun hkdfSha256(ikm: ByteArray, salt: ByteArray, info: ByteArray, length: Int): ByteArray = try {
        val s = if (salt.size >= 32) salt else ByteArray(32).also { salt.copyInto(it) }
        val prk = hmacSha256(s, ikm)
        val blocks = (length + 31) / 32
        if (blocks > 255) throw IllegalArgumentException("hkdfSha256: length too large")
        var t = ByteArray(0)
        val chunks = ByteArray(blocks * 32)
        var pos = 0
        for (i in 1..blocks) {
            t = hmacSha256(prk, concatBytes(t, info, byteArrayOf(i.toByte())))
            t.copyInto(chunks, pos)
            pos += t.size
        }
        chunks.copyOf(length)
    } catch (e: IllegalArgumentException) {
        throw e
    } catch (e: Exception) {
        throw IllegalStateException("HKDF-SHA256 unavailable", e)
    }

    actual fun encrypt(key: ByteArray, aad: ByteArray, plain: ByteArray, nonce: ByteArray): ByteArray? = try {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = SecretKeySpec(key, "AES")
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, GCMParameterSpec(128, nonce))
        cipher.updateAAD(aad)
        cipher.doFinal(plain)
    } catch (e: Exception) {
        null
    }

    actual fun decrypt(key: ByteArray, aad: ByteArray, cipher: ByteArray, nonce: ByteArray): ByteArray? = try {
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = SecretKeySpec(key, "AES")
        c.init(Cipher.DECRYPT_MODE, keySpec, GCMParameterSpec(128, nonce))
        c.updateAAD(aad)
        c.doFinal(cipher)
    } catch (e: Exception) {
        null
    }

    actual fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray = try {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        mac.doFinal(data)
    } catch (e: Exception) {
        throw IllegalStateException("HmacSHA256 unavailable", e)
    }

    actual fun sha256(data: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(data)

    actual fun randomBytes(size: Int): ByteArray = ByteArray(size).also { rnd.nextBytes(it) }

    private fun stripPrefix(encoded: ByteArray, prefix: ByteArray): ByteArray {
        if (encoded.size == 32) return encoded
        if (encoded.size == prefix.size + 32 && encoded.copyOfRange(0, prefix.size).contentEquals(prefix)) {
            return encoded.copyOfRange(prefix.size, encoded.size)
        }
        return encoded.copyOf(32)
    }
}