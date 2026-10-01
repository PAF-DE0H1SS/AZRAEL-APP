package xyz.azraellab.shared.core.protocol

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Flight-тест: гоняет REAL GatewayClient против живого шлюза.
 * Включается только если задана env AZRAEL_GATEWAY_URL (локально через SSH-туннель к контейнеру SITE).
 * Без env - тест пропускается (succeeds silently), чтобы CI без сети оставался зелёным.
 *
 * Сценарий:
 *  - guest (без auth): connect → role=guest; hello работает; ai.chat/cmd.ping → отказ (108 → call() вернёт null).
 *  - с AZRAEL_AUTH_TOKEN: connect → role >= standard; cmd.ping работает, возвращает JSON с "pong".
 *  - канал не идемпотентен: повторный connect со старым токеном не требуется - проверяем внутри одной сессии.
 */
class GatewayFlightTest {
    private fun gatewayUrl(): String? = System.getenv("AZRAEL_GATEWAY_URL")?.takeIf { it.isNotBlank() }

    private fun authToken(): String? = System.getenv("AZRAEL_AUTH_TOKEN")?.takeIf { it.isNotBlank() }

    @Test
    fun guestThenPrivilegedRealGateway() {
        val base = gatewayUrl() ?: return println("AZRAEL_GATEWAY_URL не задан - flight-тест пропущен (CI без сети)")
        val token = authToken()
        println("[flight] gateway=$base auth=${if (token != null) "given" else "none"}")

        // 1) Guest-сессия: роль без auth = guest
        val guest = GatewayClient(base)
        val hs = assertNotNull(guest.connect(null), "guest handshake должен пройти")
        assertEquals("guest", hs.role, "без auth сервер обязан вернуть role=guest")

        // hello доступен guest-у
        assertTrue(guest.sayHello()?.isNotBlank() == true, "guest hello должен вернуть ответ")

        // ai.chat/cmd.* сверх роли: сервер отвечает err=108, call() возвращает null
        assertNull(guest.aiChat("привет"), "guest ai.chat должен быть запрещён (108)")
        assertNull(guest.cmdPing(), "guest cmd.ping должен быть запрещён (108)")
        assertNull(guest.cmdInfo(), "guest cmd.info должен быть запрещён (108)")
        guest.disconnect()
        println("[flight] guest: ok (role=guest, hello, отказы 108 на ai.chat/cmd.*)")

        // 2) Привилегированная сессия: только если задан site-токен (иначе пропускаем)
        if (token != null) {
            val priv = GatewayClient(base)
            val hsp = assertNotNull(priv.connect(token), "авторизованный handshake не должен падать")
            assertTrue(hsp.role == "standard" || hsp.role == "admin", "ожидали standard/admin, получили ${hsp.role}")
            println("[flight] роль с auth=${hsp.role}")

            val ping = assertNotNull(priv.cmdPing(), "admin cmd.ping должен работать")
            assertTrue(ping.contains("\"pong\""), "cmd.ping должен вернуть JSON с pong, получили: $ping")
            println("[flight] cmd.ping: $ping")

            if (hsp.role == "admin") {
                val info = assertNotNull(priv.cmdInfo(), "admin cmd.info должен работать")
                assertTrue(info.contains("host") || info.contains("uptime"), "cmd.info должен содержать host/uptime: $info")
                println("[flight] cmd.info: $info")

                val reply = assertNotNull(priv.aiChat("ответь коротко: ОК"), "admin ai.chat должен работать")
                assertTrue(reply.isNotBlank(), "ai.chat не должен вернуть пустоту")
                println("[flight] ai.chat: $reply")
            }
            priv.disconnect()
            println("[flight] privileged: ok")
        } else {
            println("[flight] AZRAEL_AUTH_TOKEN не задан - привилегированная ветка пропущена")
        }

        println("[flight] PASS")
    }
}