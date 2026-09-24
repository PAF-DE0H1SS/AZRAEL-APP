package xyz.azraellab.shared.core.crypto

import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.spec.XECPublicKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

// Реализация крипто-примитивов для Desktop (JVM) на Java Cryptography Architecture:
// X25519 ECDH, HKDF-подобная деривация заменена прямым shared-secret, AEAD AES-256-GCM.
actual object Crypto {

    private val rnd = SecureRandom()

    actual fun keyPair(seed: ByteArray?): KeyPairData {
        val kpg = KeyPairGenerator.getInstance("X25519")
        val kp = kpg.generateKeyPair()
        val priv = kp.private.encoded ?: ByteArray(32).also { rnd.nextBytes(it) }
        val pub = kp.public.encoded ?: ByteArray(32)
        return KeyPairData(publicKey = pub, privateKey = priv)
    }

    actual fun sharedSecret(priv: ByteArray, pub: ByteArray): ByteArray {
        val kf = KeyFactory.getInstance("X25519")
        val privSpec = java.security.spec.PKCS8EncodedKeySpec(priv)
        val privKey = kf.generatePrivate(privSpec)
        // X25519 кодирует координату little-endian; JCA BigInteger ждёт big-endian.
        val pubSpec = XECPublicKeySpec(java.security.spec.NamedParameterSpec.X25519, java.math.BigInteger(1, pub.reversedArray()))
        val pubKey = kf.generatePublic(pubSpec)
        val ka = KeyAgreement.getInstance("X25519")
        ka.init(privKey)
        ka.doPhase(pubKey, true)
        return ka.generateSecret()
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
}