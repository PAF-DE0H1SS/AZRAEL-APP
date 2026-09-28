package xyz.azraellab.shared.core.crypto

import java.security.MessageDigest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

/**
 * SHA-512 нужен Ed25519, а реализация своя — значит её надо проверять отдельно
 * от Ed25519: если ошибится именно дайджест, кросс-проверка Ed25519 против JCA
 * это поймает, но с непонятным сообщением.
 *
 * Отдельно гоняем длины вокруг границ дополнения (111/112/127/128/239/240 байт):
 * именно там ломается padding — 128-битная длина и блок по 128 байт.
 */
class Sha512Test {

    private fun jca(message: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-512").digest(message)

    private fun hex(s: String) = s.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    @Test
    fun knownAnswersFromFips() {
        assertContentEquals(
            hex("cf83e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d36ce9ce" +
                "47d0d13c5d85f2b0ff8318d2877eec2f63b931bd47417a81a538327af927da3e"),
            Sha512.digest(ByteArray(0)),
            "empty message"
        )
        assertContentEquals(
            hex("ddaf35a193617abacc417349ae20413112e6fa4e89a97ea20a9eeee64b55d39a" +
                "2192992a274fc1a836ba3c23a3feebbd454d4423643ce80e2a9ac94fa54ca49f"),
            Sha512.digest("abc".toByteArray()),
            "abc"
        )
        assertContentEquals(
            hex("8e959b75dae313da8cf4f72814fc143f8f7779c6eb9f7fa17299aeadb6889018" +
                "501d289e4900f7e4331b99dec4b5433ac7d329eeb6dd26545e96e55b874be909"),
            Sha512.digest(
                ("abcdefghbcdefghicdefghijdefghijkefghijklfghijklmghijklmnhijklmno" +
                    "ijklmnopjklmnopqklmnopqrlmnopqrsmnopqrstnopqrstu").toByteArray()
            ),
            "448-bit message"
        )
    }

    @Test
    fun matchesJcaAroundPaddingBoundaries() {
        val rnd = Random(20260928)
        for (len in listOf(0, 1, 55, 56, 63, 64, 110, 111, 112, 113, 119, 120, 127, 128, 129, 239, 240, 241, 1000)) {
            val data = ByteArray(len).also { r -> rnd.nextBytes(r) }
            assertContentEquals(jca(data), Sha512.digest(data), "len=$len")
        }
    }

    @Test
    fun matchesJcaOnRandomInputs() {
        val rnd = Random(11)
        repeat(200) {
            val data = ByteArray(rnd.nextInt(0, 400)).also { r -> rnd.nextBytes(it) }
            assertContentEquals(jca(data), Sha512.digest(data), "iteration=$it")
        }
    }

    @Test
    fun digestIsAlways64Bytes() {
        assertEquals(64, Sha512.digest(ByteArray(0)).size)
        assertEquals(64, Sha512.digest(ByteArray(1000)).size)
    }
}
