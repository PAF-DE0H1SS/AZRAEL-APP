package xyz.azraellab.shared.core.crypto

import java.security.KeyFactory
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertTrue

/**
 * Ed25519 проверяется дважды, потому что код теперь свой, а не JCA:
 * 1. RFC 8032 \u00a77.1 test vectors \u2014 фиксированные байты, ловят ошибки в формулах;
 * 2. кросс-проверка против JCA на случайных seed \u2014 oracle с другой реализацией.
 *
 * В RFC \u00a77.1 secret key \u2014 это 32-байтный seed, сообщения и подписи даны в HEX.
 */
class Ed25519Test {

    private fun hex(s: String) = s.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    /** PKCS#8 v1 для Ed25519: 30 2e 02 01 00 30 05 06 03 2b 65 70 04 22 04 20. */
    private val pkcs8Prefix = hex("302e020100300506032b657004220420")

    /** X.509 SubjectPublicKeyInfo: 30 2a 30 05 06 03 2b 65 70 03 21 00. */
    private val spkiPrefix = hex("302a300506032b6570032100")

    private fun jcaPublicKey(raw32: ByteArray) = KeyFactory.getInstance("Ed25519")
        .generatePublic(X509EncodedKeySpec(spkiPrefix + raw32))

    private fun jcaPrivateKey(raw32: ByteArray) = KeyFactory.getInstance("Ed25519")
        .generatePrivate(PKCS8EncodedKeySpec(pkcs8Prefix + raw32))

    private fun jcaVerify(pub: ByteArray, message: ByteArray, signature: ByteArray): Boolean =
        Signature.getInstance("Ed25519").run {
            initVerify(jcaPublicKey(pub))
            update(message)
            verify(signature)
        }

    // TEST 1: пустое сообщение (0 байт)
    @Test
    fun rfc8032Vector1EmptyMessage() = vector(
        seed = "9d61b19deffd5a60ba844af492ec2cc44449c5697b326919703bac031cae7f60",
        pub = "d75a980182b10ab7d54bfed3c964073a0ee172f3daa62325af021a68f707511a",
        msg = "",
        sig = "e5564300c360ac729086e2cc806e828a84877f1eb8e5d974d873e065224901555fb882" +
            "1590a33bacc61e39701cf9b46bd25bf5f0595bbe24655141438e7a100b"
    )

    // TEST 2: сообщение 1 байт (0x72)
    @Test
    fun rfc8032Vector2OneByte() = vector(
        seed = "4ccd089b28ff96da9db6c346ec114e0f5b8a319f35aba624da8cf6ed4fb8a6fb",
        pub = "3d4017c3e843895a92b70aa74d1b7ebc9c982ccf2ec4968cc0cd55f12af4660c",
        msg = "72",
        sig = "92a009a9f0d4cab8720e820b5f642540a2b27b5416503f8fb3762223ebdb69da085ac1" +
            "e43e15996e458f3613d0f11d8c387b2eaeb4302aeeb00d291612bb0c00"
    )

    // TEST 3: сообщение 2 байта (0xaf82)
    @Test
    fun rfc8032Vector3TwoBytes() = vector(
        seed = "c5aa8df43f9f837bedb7442f31dcb7b166d38535076f094b85ce3a2e0b4458f7",
        pub = "fc51cd8e6218a1a38da47ed00230f0580816ed13ba3303ac5deb911548908025",
        msg = "af82",
        sig = "6291d657deec24024827e69c3abe01a30ce548a284743a445e3680d7db5ac3ac18ff9b" +
            "538d16f290ae67f760984dc6594a7c15e9716ed28dc027beceea1ec40a"
    )

    // TEST SHA(abc): сообщение 64 байта
    @Test
    fun rfc8032VectorShaAbc() = vector(
        seed = "833fe62409237b9d62ec77587520911e9a759cec1d19755b7da901b96dca3d42",
        pub = "ec172b93ad5e563bf4932c70e1245034c35467ef2efd4d64ebf819683467e2bf",
        msg = "ddaf35a193617abacc417349ae20413112e6fa4e89a97ea20a9eeee64b55d39a219299" +
            "2a274fc1a836ba3c23a3feebbd454d4423643ce80e2a9ac94fa54ca49f",
        sig = "dc2a4459e7369633a52b1bf277839a00201009a3efbf3ecb69bea2186c26b58909351f" +
            "c9ac90b3ecfdfbc7c66431e0303dca179c138ac17ad9bef1177331a704"
    )

    // TEST 1024: сообщение 1023 байта
    @Test
    fun rfc8032Vector1024() = vector(
        seed = "f5e5767cf153319517630f226876b86c8160cc583bc013744c6bf255f5cc0ee5",
        pub = "278117fc144c72340f67d0f2316e8386ceffbf2b2428c9c51fef7c597f1d426e",
        msg = "08b8b2b733424243760fe426a4b54908632110a66c2f6591eabd3345e3e4eb98fa6e26" +
            "4bf09efe12ee50f8f54e9f77b1e355f6c50544e23fb1433ddf73be84d879de7c0046dc" +
            "4996d9e773f4bc9efe5738829adb26c81b37c93a1b270b20329d658675fc6ea534e081" +
            "0a4432826bf58c941efb65d57a338bbd2e26640f89ffbc1a858efcb8550ee3a5e1998b" +
            "d177e93a7363c344fe6b199ee5d02e82d522c4feba15452f80288a821a579116ec6dad" +
            "2b3b310da903401aa62100ab5d1a36553e06203b33890cc9b832f79ef80560ccb9a39c" +
            "e767967ed628c6ad573cb116dbefefd75499da96bd68a8a97b928a8bbc103b6621fcde" +
            "2beca1231d206be6cd9ec7aff6f6c94fcd7204ed3455c68c83f4a41da4af2b74ef5c53" +
            "f1d8ac70bdcb7ed185ce81bd84359d44254d95629e9855a94a7c1958d1f8ada5d0532e" +
            "d8a5aa3fb2d17ba70eb6248e594e1a2297acbbb39d502f1a8c6eb6f1ce22b3de1a1f40" +
            "cc24554119a831a9aad6079cad88425de6bde1a9187ebb6092cf67bf2b13fd65f27088" +
            "d78b7e883c8759d2c4f5c65adb7553878ad575f9fad878e80a0c9ba63bcbcc2732e694" +
            "85bbc9c90bfbd62481d9089beccf80cfe2df16a2cf65bd92dd597b0707e0917af48bbb" +
            "75fed413d238f5555a7a569d80c3414a8d0859dc65a46128bab27af87a71314f318c78" +
            "2b23ebfe808b82b0ce26401d2e22f04d83d1255dc51addd3b75a2b1ae0784504df543a" +
            "f8969be3ea7082ff7fc9888c144da2af58429ec96031dbcad3dad9af0dcbaaaf268cb8" +
            "fcffead94f3c7ca495e056a9b47acdb751fb73e666c6c655ade8297297d07ad1ba5e43" +
            "f1bca32301651339e22904cc8c42f58c30c04aafdb038dda0847dd988dcda6f3bfd15c" +
            "4b4c4525004aa06eeff8ca61783aacec57fb3d1f92b0fe2fd1a85f6724517b65e614ad" +
            "6808d6f6ee34dff7310fdc82aebfd904b01e1dc54b2927094b2db68d6f903b68401ade" +
            "bf5a7e08d78ff4ef5d63653a65040cf9bfd4aca7984a74d37145986780fc0b16ac4516" +
            "49de6188a7dbdf191f64b5fc5e2ab47b57f7f7276cd419c17a3ca8e1b939ae49e488ac" +
            "ba6b965610b5480109c8b17b80e1b7b750dfc7598d5d5011fd2dcc5600a32ef5b52a1e" +
            "cc820e308aa342721aac0943bf6686b64b2579376504ccc493d97e6aed3fb0f9cd71a4" +
            "3dd497f01f17c0e2cb3797aa2a2f256656168e6c496afc5fb93246f6b1116398a346f1" +
            "a641f3b041e989f7914f90cc2c7fff357876e506b50d334ba77c225bc307ba537152f3" +
            "f1610e4eafe595f6d9d90d11faa933a15ef1369546868a7f3a45a96768d40fd9d03412" +
            "c091c6315cf4fde7cb68606937380db2eaaa707b4c4185c32eddcdd306705e4dc1ffc8" +
            "72eeee475a64dfac86aba41c0618983f8741c5ef68d3a101e8a3b8cac60c905c15fc91" +
            "0840b94c00a0b9d0",
        sig = "0aab4c900501b3e24d7cdf4663326a3a87df5e4843b2cbdb67cbf6e460fec350aa5371" +
            "b1508f9f4528ecea23c436d94b5e8fcd4f681e30a6ac00a9704a188a03"
    )

    private fun vector(seed: String, pub: String, msg: String, sig: String) {
        val s = hex(seed)
        val m = hex(msg)
        val expectedPub = hex(pub)
        assertContentEquals(expectedPub, Ed25519.publicKeyFromSeed(s), "public key")
        val signature = Ed25519.sign(s, m)
        assertContentEquals(hex(sig), signature, "signature")
        assertTrue(jcaVerify(expectedPub, m, signature), "JCA must accept our signature")
    }

    @Test
    fun matchesJcaOnRandomSeedsAndMessages() {
        val rnd = Random(20260928)
        repeat(64) {
            val seed = ByteArray(32).also { r -> rnd.nextBytes(r) }
            val msgLen = rnd.nextInt(0, 200)
            val msg = ByteArray(msgLen).also { r -> rnd.nextBytes(r) }
            val pub = Ed25519.publicKeyFromSeed(seed)
            val sig = Ed25519.sign(seed, msg)
            assertTrue(jcaVerify(pub, msg, sig), "JCA rejected our signature (seed #$it)")

            val jcaSig = Signature.getInstance("Ed25519").run {
                initSign(jcaPrivateKey(seed))
                update(msg)
                sign()
            }
            assertContentEquals(jcaSig, sig, "signature must equal JCA (seed #$it)")
        }
    }

    @Test
    fun rejectsTamperedSignature() {
        val seed = ByteArray(32) { it.toByte() }
        val msg = "AZRAEL-APP".toByteArray()
        val sig = Ed25519.sign(seed, msg)
        val pub = Ed25519.publicKeyFromSeed(seed)
        assertTrue(jcaVerify(pub, msg, sig))
        sig[10] = (sig[10].toInt() xor 0x40).toByte()
        assertTrue(!jcaVerify(pub, msg, sig), "tampered signature must not verify")
    }

    @Test
    fun cryptoFacadeUsesSameKeysAsReference() {
        val seed = ByteArray(32).also { r -> Random(7).nextBytes(r) }
        val kp = Crypto.ed25519KeyPair(seed)
        assertContentEquals(seed, kp.privateKey)
        assertContentEquals(Ed25519.publicKeyFromSeed(seed), kp.publicKey)
        val msg = "op|dev-1|1234567890".toByteArray()
        assertContentEquals(Ed25519.sign(seed, msg), Crypto.ed25519Sign(seed, msg))
        assertTrue(jcaVerify(kp.publicKey, msg, Crypto.ed25519Sign(seed, msg)))
    }
}
