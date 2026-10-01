package xyz.azraellab.shared.core.protocol

import xyz.azraellab.shared.core.crypto.Base64Codec
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.contentOrNull

// Сырой ответ handshake от шлюза: srv_pub + токен сессии + роль RBAC (payload — base64(JSON), не зашифрован).
data class HandshakeReply(val serverPubB64: String, val sessionToken: String, val role: String = "guest")

class GatewayClient(private val baseUrl: String, private val box: SessionBox = SessionBox()) {

    /**
     * Устанавливает защищённый канал. Поле `auth` (site-сессия) необязательно:
     * без него роль guest, с валидной сессией — standard/admin (решает сервер).
     */
    fun connect(auth: String? = null): HandshakeReply? =
        runCatching {
            val reply = httpPostJson(baseUrl, json.encodeToString(Envelope.serializer(), box.startHandshake(auth)))
                ?: return null
            val env = json.decodeFromString(Envelope.serializer(), reply)
            if (env.err != Protocol.ERR_OK) return null
            val payloadJson = String(Base64Codec.decode(env.payload))
            val obj = runCatching { json.parseToJsonElement(payloadJson).jsonObject }.getOrNull() ?: return null
            val pub = (obj["srv_pub"] as? JsonPrimitive)?.contentOrNull ?: return null
            val token = (obj["session"] as? JsonPrimitive)?.contentOrNull ?: return null
            val role = (obj["role"] as? JsonPrimitive)?.contentOrNull ?: "guest"
            if (!box.acceptServer(pub, token)) return null
            HandshakeReply(pub, token, role)
        }.getOrNull()

    // Generic-вызов: seal(body) → шлёт зашифрованный конверт → возвращает расшифрованный payload (или null).
    fun call(op: String, requestJson: String): String? {
        val reply = box.seal(op, requestJson.toByteArray()) ?: return null
        return openBytes(reply)
    }

    fun sayStatus(): String? = call(Protocol.OP_STATUS, """{"want":"status"}""")

    fun sayHello(): String? = call(Protocol.OP_HELLO, """{"want":"echo"}""")

    // ai.chat — проксируется сервером на llama-server; правда требует роль >= standard.
    fun aiChat(userMessage: String): String? =
        call(Protocol.OP_AI_CHAT, """{"messages":[{"role":"user","content":${jsonQuote(userMessage)}}]}""")

    // cmd.ping / cmd.info — административные команды (роль admin).
    fun cmdPing(): String? = call(Protocol.OP_CMD_PING, """{"cmd":"ping"}""")
    fun cmdInfo(): String? = call(Protocol.OP_CMD_INFO, """{"cmd":"info"}""")

    fun disconnect(): Boolean {
        val reply = box.seal(Protocol.OP_SESSION_BYE, "{}".toByteArray()) ?: return false
        return openBytes(reply) != null
    }

    private fun openBytes(request: Envelope): String? {
        val respRaw = httpPostJson(baseUrl, json.encodeToString(Envelope.serializer(), request)) ?: return null
        val resp = json.decodeFromString(Envelope.serializer(), respRaw)
        if (resp.err != Protocol.ERR_OK) return null
        val plain = box.open(resp) ?: return null
        return String(plain)
    }

    private fun jsonQuote(s: String): String =
        "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\""

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
}