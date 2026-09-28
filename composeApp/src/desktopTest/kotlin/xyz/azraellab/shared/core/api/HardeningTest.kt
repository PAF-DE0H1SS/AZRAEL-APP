package xyz.azraellab.shared.core.api

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import xyz.azraellab.shared.core.crypto.Base64Codec
import xyz.azraellab.shared.core.crypto.Crypto
import xyz.azraellab.shared.core.crypto.randomBytes
import xyz.azraellab.shared.core.protocol.HttpResult
import xyz.azraellab.shared.core.protocol.SessionBox
import xyz.azraellab.shared.core.protocol.httpGetJson
import xyz.azraellab.shared.core.protocol.httpPostJsonWithHeaders
import java.net.InetSocketAddress
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Регрессии по hardening-правкам 1.2.1: CSPRNG, fail-closed без ключа канала,
 * валидация серверного ключа шлюза и потолок на размер HTTP-ответа.
 */
class HardeningTest {

    private lateinit var server: HttpServer
    private var port: Int = -1

    private fun url(path: String) = "http://127.0.0.1:$port$path"

    private fun serve(path: String, handler: (HttpExchange) -> Unit) {
        server.createContext(path) { ex -> runCatching { handler(ex) } }
    }

    private fun HttpExchange.reply(code: Int, body: String, header: Pair<String, String>? = null) {
        if (header != null) responseHeaders.add(header.first, header.second)
        val bytes = body.toByteArray(Charsets.UTF_8)
        sendResponseHeaders(code, bytes.size.toLong())
        responseBody.use { it.write(bytes) }
    }

    @BeforeTest
    fun start() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        port = server.address.port
        server.start()
    }

    @AfterTest
    fun stop() {
        server.stop(0)
    }

    @Test
    fun randomBytesGoesThroughPlatformCsprng() {
        // Top-level randomBytes обязан идти в SecureRandom, а не в kotlin.random.Default:
        // от предсказуемости nonce/id/session-token зависят все конверты канала.
        assertEquals(32, randomBytes(32).size)
        assertEquals(24, Crypto.randomBytes(24).size)
        val seen = HashSet<String>()
        repeat(32) { seen += Base64Codec.encode(randomBytes(16)) }
        assertEquals(32, seen.size, "CSPRNG обязан давать разные значения")
    }

    @Test
    fun sessionBoxRejectsMalformedServerKey() {
        val good = Base64Codec.encode(ByteArray(31) + 1)
        val box = SessionBox()
        assertTrue(!box.acceptServer("", "tok"), "пустой ключ не принимаем")
        assertTrue(!box.acceptServer(Base64Codec.encode(ByteArray(32)), "tok"), "all-zero даёт нулевой shared secret")
        assertTrue(!box.acceptServer(Base64Codec.encode(ByteArray(16)), "tok"), "нужен ровно raw-32")
        assertTrue(!box.acceptServer(good, "t".repeat(600)), "раздутый токен не принимаем")
        assertTrue(!box.acceptServer(good, ""), "пустой токен не принимаем")
        assertTrue(box.acceptServer(good, "tok"), "валидный raw-32 принимаем")
    }

    @Test
    fun clientWithoutChannelKeyRefusesToSendPlaintext() {
        // Fail-closed: раньше при appKey == null уходил неподписанный plaintext-запрос,
        // и ответ принимался без проверки подписи. Теперь отказ до сети.
        val c = AppClient("https://example.invalid/api/app/v1", null)
        val e = assertFailsWith<AppException> { c.systemHealth() }
        assertEquals(AppErrorCode.FORBIDDEN, e.code)
    }

    @Test
    fun responseOverLimitIsRejected() {
        // Тело крупнее потолка (1 МиБ) не должно ни приниматься, ни съедать память.
        serve("/big") { ex ->
            val chunk = ByteArray(64 * 1024) { 'A'.code.toByte() }
            ex.sendResponseHeaders(200, (chunk.size * 48).toLong())
            ex.responseBody.use { out -> repeat(48) { out.write(chunk) } }
        }
        assertNull(httpGetJson(url("/big"), 10_000))
    }

    @Test
    fun responseInsideLimitIsRead() {
        serve("/ok") { it.reply(200, """{"v":1,"ok":true}""") }
        assertEquals("""{"v":1,"ok":true}""", httpGetJson(url("/ok"), 5000))
    }

    @Test
    fun redirectIsNotFollowed() {
        // Подпись привязана к телу конверта: ответ с другого хоста доверия не заслуживает.
        serve("/start") { it.reply(302, "", "Location" to url("/ok")) }
        serve("/ok") { it.reply(200, """{"v":1}""") }
        assertNull(httpGetJson(url("/start"), 5000), "302 не должен выглядеть как успех")
    }

    @Test
    fun statusAndHeadersAreSurfaced() {
        serve("/hdr") { it.reply(429, """{"err":1}""", "X-Azrael-Test" to "v1") }
        val r: HttpResult = httpPostJsonWithHeaders(url("/hdr"), "{}", emptyMap(), 5000)
        assertEquals(429, r.status)
        assertEquals("v1", r.headers["x-azrael-test"])
    }

    @Test
    fun differingEntropyAcrossCalls() {
        val a = randomBytes(32).toList()
        val b = randomBytes(32).toList()
        assertNotEquals(a, b)
    }
}
