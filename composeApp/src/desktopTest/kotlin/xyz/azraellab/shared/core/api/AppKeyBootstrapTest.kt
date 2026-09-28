package xyz.azraellab.shared.core.api

import xyz.azraellab.shared.core.crypto.Base64Codec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Проверки первичной выдачи ключа канала (GET /api/app/bootstrap).
 *
 * Логика чистая и обязана совпадать с серверной padKeyBytes() из site/lib/app-secure.ts,
 * поэтому проверяется локально и без сети; сетевой fetch покрыт AuthFlightTest.
 */
class AppKeyBootstrapTest {

    /** Обратная операция к padKeyBytes() на сервере. */
    private fun pad(raw: ByteArray): String {
        val pad = byteArrayOf(0x41, 0x5a, 0x52, 0x41, 0x45, 0x4c, 0x2d, 0x76)
        val out = ByteArray(raw.size)
        for (i in raw.indices) out[i] = (raw[i].toInt() xor pad[i % pad.size].toInt()).toByte()
        return Base64Codec.encode(out)
    }

    private fun sampleKey(): ByteArray = ByteArray(32) { (it * 7 + 3).toByte() }

    @Test
    fun unpadIsInverseOfServerPad() {
        val key = sampleKey()
        assertContentEquals(key, AppKeyBootstrap.unpadKey(pad(key)))
    }

    @Test
    fun unpadToleratesSurroundingWhitespace() {
        val key = sampleKey()
        assertContentEquals(key, AppKeyBootstrap.unpadKey("  ${pad(key)}\n"))
    }

    @Test
    fun unpadRejectsGarbage() {
        assertEquals(null, AppKeyBootstrap.unpadKey(""))
        assertEquals(null, AppKeyBootstrap.unpadKey("не base64 !!!"))
    }

    @Test
    fun bootstrapUrlDerivedFromApiUrl() {
        assertEquals(
            "https://azrael-lab.xyz/api/app/bootstrap?devId=abc123",
            AppKeyBootstrap.bootstrapUrl("https://azrael-lab.xyz/api/app/v1", "abc123")
        )
    }

    @Test
    fun bootstrapUrlToleratesSlashAndTrailingPath() {
        assertEquals(
            "https://azrael-lab.xyz/api/app/bootstrap?devId=abc123",
            AppKeyBootstrap.bootstrapUrl("https://azrael-lab.xyz/api/app/v1/", " abc123 ")
        )
        assertEquals(
            "https://azrael-lab.xyz/api/app/bootstrap?devId=abc123",
            AppKeyBootstrap.bootstrapUrl("  https://azrael-lab.xyz/api/app/bootstrap  ", "abc123")
        )
    }

    @Test
    fun bootstrapUrlFallsBackToSiteRoot() {
        assertEquals(
            "https://azrael-lab.xyz/api/app/bootstrap?devId=abc123",
            AppKeyBootstrap.bootstrapUrl("https://azrael-lab.xyz", "abc123")
        )
    }

    /**
     * devId обязан попадать в адрес: без него сервер не выдаёт ключ этой установки,
     * а общий мастер-ключ выдавать больше нельзя.
     */
    @Test
    fun bootstrapUrlAlwaysCarriesDevId() {
        val devId = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
        val url = AppKeyBootstrap.bootstrapUrl("https://azrael-lab.xyz/api/app/v1", devId)
        assertTrue(url.endsWith("?devId=$devId"), "devId не попал в адрес: $url")
        assertEquals(64, devId.length)
    }

    private fun assertContentEquals(expected: ByteArray, actual: ByteArray?) {
        assertTrue(actual != null && expected.contentEquals(actual), "ключи разошлись")
    }
}
