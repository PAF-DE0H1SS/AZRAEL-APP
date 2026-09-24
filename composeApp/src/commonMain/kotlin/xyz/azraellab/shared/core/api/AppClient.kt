package xyz.azraellab.shared.core.api

import xyz.azraellab.shared.core.protocol.Protocol
import xyz.azraellab.shared.core.protocol.httpPostJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

// Имена операций кастомного API приложения (POST /api/app/v1, контракт APP_DEV_LOG/02).
object AppApi {
    const val VERSION = 1

    // auth
    const val AUTH_LOGIN = "auth.login"
    const val AUTH_REGISTER = "auth.register"
    const val AUTH_LOGOUT = "auth.logout"
    const val AUTH_VERIFY = "auth.verify"
    const val AUTH_CHANGE_PASSWORD = "auth.changePassword"
    const val SESSION_REVOKE = "session.revoke"

    // profile
    const val PROFILE_GET = "profile.get"
    const val PROFILE_UPDATE = "profile.update"
    const val PROFILE_AVATAR_SET = "profile.avatar.set"
    const val PROFILE_AVATAR_URL = "profile.avatar.url"
    const val PROFILE_DELETE = "profile.delete"
    const val PROFILE_AUTO_DELETE = "profile.autoDelete"
    const val PROFILE_PRIVACY_GET = "profile.privacy.get"
    const val PROFILE_PRIVACY_SET = "profile.privacy.set"
    const val PROFILE_SEARCH_USERS = "profile.searchUsers"

    // home
    const val HOME_BOOT = "home.boot"

    // chats / messages
    const val CHATS_LIST = "chats.list"
    const val CHATS_LIST_ARCHIVED = "chats.listArchived"
    const val CHATS_CREATE = "chats.create"
    const val CHATS_OPEN = "chats.open"
    const val CHATS_MESSAGES = "chats.messages"
    const val CHATS_SEND = "chats.send"
    const val CHATS_TYPING = "chats.typing"
    const val CHATS_PRESENCE = "chats.presence"
    const val CHATS_ARCHIVE = "chats.archive"
    const val CHATS_RESTORE = "chats.restore"
    const val CHATS_DELETE = "chats.delete"
    const val CHATS_DELETE_CHAT = "chats.deleteChat"
    const val CHATS_AUTODELETE_GET = "chats.autodelete.get"
    const val CHATS_AUTODELETE_SET = "chats.autodelete.set"
    const val CHATS_FILE_UPLOAD = "chats.file.upload"
    const val CHATS_FILE_URL = "chats.file.url"

    // shortener
    const val SHORTENER_LIST = "shortener.list"
    const val SHORTENER_CREATE = "shortener.create"
    const val SHORTENER_DELETE = "shortener.delete"

    // invites
    const val INVITES_MY_CODE = "invites.myCode"
    const val INVITES_LIST = "invites.list"
    const val INVITES_GENERATE = "invites.generate"
    const val INVITES_ARCHIVE = "invites.archive"
    const val INVITES_UNARCHIVE = "invites.unarchive"

    // vpn
    const val VPN_FREE_SUMMARY = "vpn.free.summary"
    const val VPN_FREE_SERVERS = "vpn.free.servers"
    const val VPN_FREE_REFRESH = "vpn.free.refresh"
    const val VPN_AWG_STATUS = "vpn.awg.status"
    const val VPN_INCYS_DOWNLOADS = "vpn.incys.downloads"

    // gateway-обёртки
    const val GATEWAY_HANDSHAKE = "gateway.handshake"
    const val GATEWAY_CALL = "gateway.call"

    // ai
    const val AI_CHAT_LIST = "ai.chat.list"
    const val AI_CHAT_SEND = "ai.chat.send"

    // otp
    const val OTP_GENERATE_SECRET = "otp.generateSecret"
    const val OTP_VALIDATE = "otp.validate"

    // system
    const val SYSTEM_HEALTH = "system.health"
}

// Коды ошибок: переиспользуем звучание Gateway-протокола + новые коды кастомного API.
object AppErrorCode {
    const val OK = Protocol.ERR_OK
    const val MALFORMED = Protocol.ERR_MALFORMED
    const val SESSION = Protocol.ERR_SESSION
    const val RATE = Protocol.ERR_RATE
    const val INTERNAL = Protocol.ERR_INTERNAL
    const val FORBIDDEN = Protocol.ERR_FORBIDDEN
    const val INVALID_CRED = 201
    const val USER_EXISTS = 202
    const val INVITE_BAD = 203
    const val VALIDATION = 204

    // Локальная (клиентская) ошибка транспорта, сервером не выставляется.
    const val NETWORK = -1
}

class AppException(val code: Int, message: String) : Exception(message) {
    val isNetworkError: Boolean get() = code == AppErrorCode.NETWORK
}

@Serializable
data class AppRequest(
    val v: Int = AppApi.VERSION,
    val op: String,
    val args: JsonObject = JsonObject(emptyMap()),
    val session: String? = null
)

/**
 * Единый клиент кастомного API приложения: один POST /api/app/v1, JSON,
 * коды ошибок как в Protocol. Состояния: auth, profile, chats, shortener,
 * invites, vpn, gateway, ai, otp, home, system.
 */
class AppClient(private val baseUrl: String, private val appKey: ByteArray? = null) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    // true, если защищённый конверт (шифрование + подпись + anti-replay) включён.
    val isSecure: Boolean get() = appKey != null

    // Токен сессии сайта; хранится между вызовами (кладутся в session).
    private var token: String? = null
    fun sessionToken(): String? = token
    fun setToken(t: String?) {
        token = t
    }

    /** Базовый запрос: возвращает data-объект, бросает AppException при err или нарушении подписи. */
    fun call(op: String, args: JsonObject = JsonObject(emptyMap()), session: String? = token): JsonObject {
        val plainJson = json.encodeToString(AppRequest.serializer(), AppRequest(op = op, args = args, session = session))
        val key = appKey
        val resp: xyz.azraellab.shared.core.protocol.HttpResult
        if (key != null) {
            val sealed = AppSecure.seal(key, plainJson)
            resp = xyz.azraellab.shared.core.protocol.httpPostJsonWithHeaders(
                baseUrl, sealed.body, mapOf(
                    AppSecure.HDR_VERSION to AppSecure.VERSION.toString(),
                    AppSecure.HDR_TS to sealed.ts.toString(),
                    AppSecure.HDR_NONCE to sealed.nonce,
                    AppSecure.HDR_SIG to sealed.sig
                ), timeoutMs = 20_000
            )
            // Тройная проверка ответа: сайт должен знать тот же ключ (двусторонняя аутентификация).
            if (!AppSecure.verifyResponse(key, resp)) {
                if (resp.status == 0) throw AppException(AppErrorCode.NETWORK, "network: no response")
                throw AppException(AppErrorCode.FORBIDDEN, "bad server signature")
            }
        } else {
            val raw = xyz.azraellab.shared.core.protocol.httpPostJson(baseUrl, plainJson, timeoutMs = 20_000)
                ?: throw AppException(AppErrorCode.NETWORK, "network: no response")
            resp = xyz.azraellab.shared.core.protocol.HttpResult(raw, 200, emptyMap())
        }
        val root = try {
            json.parseToJsonElement(resp.body ?: throw AppException(AppErrorCode.MALFORMED, "empty response")).jsonObject
        } catch (e: Exception) {
            throw AppException(AppErrorCode.MALFORMED, "malformed response")
        }
        val err = root["err"]?.jsonPrimitive?.content?.toIntOrNull() ?: AppErrorCode.MALFORMED
        if (err != AppErrorCode.OK) {
            val msg = root["error"]?.jsonPrimitive?.content ?: "err=$err"
            throw AppException(err, msg)
        }
        return root["data"]?.jsonObject ?: JsonObject(emptyMap())
    }

    // --- auth ---
    fun login(username: String, password: String): JsonObject =
        call(AppApi.AUTH_LOGIN, buildJsonObject {
            put("username", username); put("password", password)
        }).also { token = it["token"]?.jsonPrimitive?.content }

    fun register(username: String, password: String, displayName: String? = null, gender: String? = null, inviteCode: String? = null): JsonObject =
        call(AppApi.AUTH_REGISTER, buildJsonObject {
            put("username", username); put("password", password)
            displayName?.let { put("displayName", it) }
            gender?.let { put("gender", it) }
            inviteCode?.let { put("inviteCode", it) }
        }).also { token = it["token"]?.jsonPrimitive?.content }

    fun logout(): JsonObject = call(AppApi.AUTH_LOGOUT)
    fun verify(): JsonObject = call(AppApi.AUTH_VERIFY)
    fun changePassword(oldPassword: String, newPassword: String): JsonObject =
        call(AppApi.AUTH_CHANGE_PASSWORD, buildJsonObject {
            put("oldPassword", oldPassword); put("newPassword", newPassword)
        })
    fun revokeSession(otherToken: String): JsonObject =
        call(AppApi.SESSION_REVOKE, buildJsonObject { put("token", otherToken) })

    // --- profile ---
    fun profileGet(): JsonObject = call(AppApi.PROFILE_GET)
    fun profileUpdate(username: String? = null, displayName: String? = null, tag: String? = null, gender: String? = null): JsonObject =
        call(AppApi.PROFILE_UPDATE, buildJsonObject {
            username?.let { put("username", it) }
            displayName?.let { put("displayName", it) }
            tag?.let { put("tag", it) }
            gender?.let { put("gender", it) }
        })
    fun profileAvatarSet(avatarData: String, avatarMime: String): JsonObject =
        call(AppApi.PROFILE_AVATAR_SET, buildJsonObject {
            put("avatarData", avatarData); put("avatarMime", avatarMime)
        })
    fun profileAvatarUrl(): JsonObject = call(AppApi.PROFILE_AVATAR_URL)
    fun profileDelete(): JsonObject = call(AppApi.PROFILE_DELETE)
    fun profileAutoDelete(days: Int?): JsonObject =
        call(AppApi.PROFILE_AUTO_DELETE, buildJsonObject { days?.let { put("days", it) } })
    fun profilePrivacyGet(): JsonObject = call(AppApi.PROFILE_PRIVACY_GET)
    fun profilePrivacySet(values: Map<String, String>): JsonObject =
        call(AppApi.PROFILE_PRIVACY_SET, JsonObject(values.entries.associate { it.key to kotlinx.serialization.json.JsonPrimitive(it.value) }))
    fun searchUsers(q: String): JsonObject =
        call(AppApi.PROFILE_SEARCH_USERS, buildJsonObject { put("q", q) })

    // --- home ---
    fun homeBoot(): JsonObject = call(AppApi.HOME_BOOT)

    // --- chats ---
    fun chatsList(): JsonObject = call(AppApi.CHATS_LIST)
    fun chatsListArchived(): JsonObject = call(AppApi.CHATS_LIST_ARCHIVED)
    fun chatsCreate(partnerUid: String): JsonObject =
        call(AppApi.CHATS_CREATE, buildJsonObject { put("partnerUid", partnerUid) })
    fun chatsOpen(partnerUid: String? = null, chatId: Long? = null): JsonObject =
        call(AppApi.CHATS_OPEN, buildJsonObject {
            partnerUid?.let { put("partnerUid", it) }
            chatId?.let { put("chatId", it) }
        })
    fun chatsMessages(chatId: Long, before: Long? = null, limit: Int? = 50): JsonObject =
        call(AppApi.CHATS_MESSAGES, buildJsonObject {
            put("chatId", chatId)
            before?.let { put("before", it) }
            limit?.let { put("limit", it) }
        })
    fun chatsSend(chatId: Long, text: String? = null, fileToken: String? = null): JsonObject =
        call(AppApi.CHATS_SEND, buildJsonObject {
            put("chatId", chatId)
            text?.let { put("text", it) }
            fileToken?.let { put("fileToken", it) }
        })
    fun chatsTyping(chatId: Long): JsonObject =
        call(AppApi.CHATS_TYPING, buildJsonObject { put("chatId", chatId) })
    fun chatsPresence(): JsonObject = call(AppApi.CHATS_PRESENCE)
    fun chatsArchive(chatId: Long): JsonObject =
        call(AppApi.CHATS_ARCHIVE, buildJsonObject { put("chatId", chatId) })
    fun chatsRestore(chatId: Long): JsonObject =
        call(AppApi.CHATS_RESTORE, buildJsonObject { put("chatId", chatId) })
    fun chatsDeleteChat(chatId: Long): JsonObject =
        call(AppApi.CHATS_DELETE_CHAT, buildJsonObject { put("chatId", chatId) })
    fun chatsAutodeleteGet(chatId: Long): JsonObject =
        call(AppApi.CHATS_AUTODELETE_GET, buildJsonObject { put("chatId", chatId) })
    fun chatsAutodeleteSet(chatId: Long, days: Int?): JsonObject =
        call(AppApi.CHATS_AUTODELETE_SET, buildJsonObject {
            put("chatId", chatId); days?.let { put("days", it) }
        })
    fun chatsFileUpload(base64: String, mime: String, name: String): JsonObject =
        call(AppApi.CHATS_FILE_UPLOAD, buildJsonObject {
            put("base64", base64); put("mime", mime); put("name", name)
        })
    fun chatsFileUrl(fileToken: String): JsonObject =
        call(AppApi.CHATS_FILE_URL, buildJsonObject { put("fileToken", fileToken) })

    // --- shortener ---
    fun shortenerList(): JsonObject = call(AppApi.SHORTENER_LIST)
    fun shortenerCreate(url: String, customCode: String? = null): JsonObject =
        call(AppApi.SHORTENER_CREATE, buildJsonObject {
            put("url", url); customCode?.let { put("customCode", it) }
        })
    fun shortenerDelete(code: String): JsonObject =
        call(AppApi.SHORTENER_DELETE, buildJsonObject { put("code", code) })

    // --- invites ---
    fun invitesMyCode(): JsonObject = call(AppApi.INVITES_MY_CODE)
    fun invitesList(active: Boolean? = null): JsonObject =
        call(AppApi.INVITES_LIST, buildJsonObject { active?.let { put("active", it) } })
    fun invitesGenerate(): JsonObject = call(AppApi.INVITES_GENERATE)
    fun invitesArchive(id: Long): JsonObject = call(AppApi.INVITES_ARCHIVE, buildJsonObject { put("id", id) })
    fun invitesUnarchive(id: Long): JsonObject = call(AppApi.INVITES_UNARCHIVE, buildJsonObject { put("id", id) })

    // --- vpn ---
    fun vpnFreeSummary(): JsonObject = call(AppApi.VPN_FREE_SUMMARY)
    fun vpnFreeServers(folder: String? = null): JsonObject =
        call(AppApi.VPN_FREE_SERVERS, buildJsonObject { folder?.let { put("folder", it) } })
    fun vpnFreeRefresh(): JsonObject = call(AppApi.VPN_FREE_REFRESH)
    fun vpnAwgStatus(): JsonObject = call(AppApi.VPN_AWG_STATUS)
    fun vpnIncysDownloads(): JsonObject = call(AppApi.VPN_INCYS_DOWNLOADS)

    // --- gateway (прозрачный конверт: байты base64 передаются как есть) ---
    fun gatewayHandshake(pubB64: String, auth: String? = null): JsonObject =
        call(AppApi.GATEWAY_HANDSHAKE, buildJsonObject {
            put("pubB64", pubB64); auth?.let { put("auth", it) }
        })
    fun gatewayCall(session: String, op: String, payloadB64: String, id: String, ts: Long, nonce: String): JsonObject =
        call(AppApi.GATEWAY_CALL, buildJsonObject {
            put("session", session); put("op", op); put("payloadB64", payloadB64)
            put("id", id); put("ts", ts); put("nonce", nonce)
        })

    // --- ai ---
    fun aiChatList(): JsonObject = call(AppApi.AI_CHAT_LIST)
    fun aiChatSend(message: String, chatId: Long? = null): JsonObject =
        call(AppApi.AI_CHAT_SEND, buildJsonObject {
            put("message", message); chatId?.let { put("chatId", it) }
        })

    // --- otp ---
    fun otpGenerateSecret(): JsonObject = call(AppApi.OTP_GENERATE_SECRET)
    fun otpValidate(code: String): JsonObject =
        call(AppApi.OTP_VALIDATE, buildJsonObject { put("code", code) })

    // --- system ---
    fun systemHealth(): JsonObject = call(AppApi.SYSTEM_HEALTH)
}