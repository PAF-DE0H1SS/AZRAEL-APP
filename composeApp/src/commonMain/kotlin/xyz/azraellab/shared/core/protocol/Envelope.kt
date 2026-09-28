package xyz.azraellab.shared.core.protocol

import kotlinx.serialization.Serializable

@Serializable
data class Envelope(
    val v: Int = Protocol.VERSION,
    val op: String,
    val id: String,
    val ts: Long,
    val session: String = "",
    val payload: String = "",
    val nonce: String = "",
    val err: Int = 0
)

object Protocol {
    const val VERSION = 1

    const val OP_SESSION_INIT = "session.init"
    const val OP_SESSION_ROTATE = "session.rotate"
    const val OP_SESSION_BYE = "session.bye"
    const val OP_STATUS = "status"
    const val OP_HELLO = "hello"
    const val OP_AI_CHAT = "ai.chat"
    const val OP_CMD_INFO = "cmd.info"
    const val OP_CMD_PING = "cmd.ping"

    const val ERR_OK = 0
    const val ERR_MALFORMED = 100
    const val ERR_VERSION = 101
    const val ERR_TIME = 102
    const val ERR_REPLAY = 103
    const val ERR_SESSION = 104
    const val ERR_UNKNOWN_OP = 105
    const val ERR_RATE = 106
    const val ERR_INTERNAL = 107
    const val ERR_FORBIDDEN = 108

    const val TS_SKEW_SEC = 120L
}