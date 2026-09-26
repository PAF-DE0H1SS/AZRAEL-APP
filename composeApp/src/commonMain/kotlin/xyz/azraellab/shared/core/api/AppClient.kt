package xyz.azraellab.shared.core.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import xyz.azraellab.shared.AppTrap
import xyz.azraellab.shared.AppVault
import xyz.azraellab.shared.Hex
import xyz.azraellab.shared.core.crypto.Base64Codec
import xyz.azraellab.shared.core.crypto.Crypto
import xyz.azraellab.shared.core.crypto.randomBytes
import xyz.azraellab.shared.core.protocol.HttpResult
import xyz.azraellab.shared.core.protocol.Protocol
import xyz.azraellab.shared.core.protocol.defaultAppSrvPubB64
import xyz.azraellab.shared.core.protocol.httpPostJson
import xyz.azraellab.shared.core.protocol.httpPostJsonWithHeaders
import xyz.azraellab.shared.platformName

// Имена операций кастомного API приложения (POST /api/app/v1).
object AppApi {
    const val VERSION = 1

    // system
    const val SYSTEM_HEALTH = "system.health"

    // device: привязка установки по индивидуальному ключу (provision-ключу аккаунта)
    const val DEVICE_BIND = "device.bind"
    const val DEVICE_ROTATE = "device.rotate"
    const val DEVICE_STATUS = "device.status"
    const val DEVICE_LIST = "device.list"
    const val DEVICE_REVOKE = "device.revoke"

    // auth: вход и регистрация по логину/паролю (и зеркально на сайте). Сессия
    // выдаётся сервером; при регистрации он же выдаёт новый provision-ключ.
    const val AUTH_LOGIN = "auth.login"
    const val AUTH_REGISTER = "auth.register"
    const val AUTH_LOGOUT = "auth.logout"
    const val AUTH_VERIFY = "auth.verify"
    const val AUTH_CHANGE_PASSWORD = "auth.changePassword"
    const val SESSION_REVOKE = "session.revoke"

    // profile
    const val PROFILE_UPDATE = "profile.update"
    const val PROFILE_AVATAR_SET = "profile.avatar.set"
    const val PROFILE_AVATAR_URL = "profile.avatar.url"
    const val PROFILE_DELETE = "profile.delete"
    const val PROFILE_AUTO_DELETE = "profile.autoDelete"
    const val PROFILE_PRIVACY_GET = "profile.privacy.get"
    const val PROFILE_PRIVACY_SET = "profile.privacy.set"
    const val PROFILE_SEARCH_USERS = "profile.searchUsers"

    /**
     * Язык аккаунта (users.lang) — общий для сайта и всех устройств.
     * Намеренно НЕ входит в L2_OPS: на сервере операция тоже вне L2-слоя.
     */
    const val PROFILE_SET_LANG = "profile.setLang"

    // home
    const val HOME_BOOT = "home.boot"

    // chats / messages
    const val CHATS_LIST = "chats.list"
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
    const val CHATS_LIST_ARCHIVED = "chats.listArchived"
    const val CHATS_AUTODELETE_GET = "chats.autodelete.get"
    const val CHATS_AUTODELETE_SET = "chats.autodelete.set"
    const val CHATS_FILE_UPLOAD = "chats.file.upload"
    const val CHATS_FILE_URL = "chats.file.url"

    // shortener
    const val SHORTENER_LIST = "shortener.list"
    const val SHORTENER_CREATE = "shortener.create"
    const val SHORTENER_DELETE = "shortener.delete"
    const val SHORTENER_QR = "shortener.qr"

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

    // provision (выдача/ротация ключа привязки, видима в /main → Настройки)
    const val PROVISION_KEY = "provision.key"
    const val PROVISION_INFO = "provision.info"
    const val PROVISION_REGENERATE = "provision.regenerate"

    // gateway-обёртки
    const val GATEWAY_HANDSHAKE = "gateway.handshake"
    const val GATEWAY_CALL = "gateway.call"

    // admin (разморозка аккаунтов и установок)
    const val ADMIN_FREEZE_LIST = "admin.freezeList"
    const val ADMIN_UNFREEZE = "admin.unfreeze"

    // ai
    const val AI_CHAT_LIST = "ai.chat.list"
    const val AI_CHAT_SEND = "ai.chat.send"

    // otp
    const val OTP_GENERATE_SECRET = "otp.generateSecret"
    const val OTP_VALIDATE = "otp.validate"

    /**
     * Операции, которые сервер проводит через внутренний L2-слой
     * (программа ↔ сайт ↔ /api/app/v2/l2). Зеркало L2_OPS в app/api/app/v1/route.ts.
     * Сайт эти аргументы не видит: конверт зашифрован ключом СЕРВЕРА.
     */
    val L2_OPS: Set<String> = setOf(
        CHATS_LIST, CHATS_SEND, CHATS_MESSAGES, CHATS_CREATE, CHATS_OPEN,
        CHATS_DELETE, CHATS_ARCHIVE, CHATS_RESTORE, CHATS_TYPING, CHATS_PRESENCE,
        CHATS_FILE_UPLOAD, CHATS_FILE_URL, CHATS_AUTODELETE_SET, CHATS_AUTODELETE_GET,
        PROFILE_UPDATE, PROFILE_PRIVACY_SET,
        OTP_GENERATE_SECRET, OTP_VALIDATE, SESSION_REVOKE,
        INVITES_GENERATE, INVITES_ARCHIVE,
        AI_CHAT_SEND, AI_CHAT_LIST, GATEWAY_CALL, GATEWAY_HANDSHAKE
    )

    /**
     * Операции без device-подписи: ключа установки ещё нет либо подпись не нужна.
     * Вход/регистрация идут без подписи всегда — иначе вход на новую установку
     * был бы невозможен, а на уже привязанной подпись сделала бы аккаунт
     * неразличимым между «кем вошли» и «кем привязана установка».
     */
    val DEVICE_UNSIGNED_OPS: Set<String> = setOf(
        DEVICE_BIND, SYSTEM_HEALTH, AUTH_LOGIN, AUTH_REGISTER
    )
}

// Коды ошибок: звучание Gateway-протокола + коды кастомного API.
object AppErrorCode {
    const val OK = Protocol.ERR_OK
    const val MALFORMED = Protocol.ERR_MALFORMED
    const val SESSION = Protocol.ERR_SESSION
    const val RATE = Protocol.ERR_RATE
    const val INTERNAL = Protocol.ERR_INTERNAL
    const val FORBIDDEN = Protocol.ERR_FORBIDDEN
    const val TS_SKEW = Protocol.ERR_TIME
    const val UNKNOWN_OP = Protocol.ERR_UNKNOWN_OP
    const val INVALID_CRED = 201
    const val USER_EXISTS = 202
    const val INVITE_BAD = 203
    const val VALIDATION = 204
    const val L2_REQUIRED = 108
    const val L2_UNAVAILABLE = 107
    const val FROZEN = 109

    // Локальная (клиентская) ошибка транспорта, сервером не выставляется.
    const val NETWORK = -1
}

class AppException(val code: Int, message: String) : Exception(message) {
    val isNetworkError: Boolean get() = code == AppErrorCode.NETWORK
}

/** Режим угрозы, в который перешло приложение (сервер сообщил маркерами). */
enum class ThreatMode { POISONED, TRAPPED }

@Serializable
data class AppRequest(
    val v: Int = AppApi.VERSION,
    val op: String,
    val args: JsonObject = JsonObject(emptyMap()),
    val session: String? = null,
    val deviceId: String? = null
)

/**
 * Единый клиент кастомного API приложения: один POST /api/app/v1, JSON,
 * коды ошибок как в Protocol. Состояния: device, provision, profile, chats,
 * shortener, invites, vpn, gateway, ai, otp, home, system.
 *
 * Слои защиты поверх HTTPS:
 *  1. Внешний конверт (AppSecure): AES-256-GCM ключом канала + HMAC-подпись + anti-replay.
 *  2. L2 (трёхсторонний) конверт для чувствительных операций: эфемерный X25519 +
 *     HKDF-SHA256 к статическому ключу сервера; сайт переносит конверт вслепую.
 *  3. Device-подпись (Ed25519) поверх сырого тела запроса — привязка установки
 *     к аккаунту и отзыв устройства через /admin.
 *  4. Синхронизация часов по x-azrael-srv-ts (с retry при рассинхроне ts).
 */
class AppClient(private val baseUrl: String, private val appKey: ByteArray? = null) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    /** Ключ якоря (поколение 1), задаётся пользователем/окружением. */
    private val anchor: ByteArray? = appKey

    /** Текущее поколение ключа и сам ключ (стартует с якоря; ротация через x-azrael-rot-*). */
    private var gen: Long = 1L
    private var currentKey: ByteArray? = anchor

    /** Статический X25519 публичный ключ сервера (raw-32) для L2-конверта. */
    private val srvXPub: ByteArray? = runCatching {
        defaultAppSrvPubB64()?.let { Base64Codec.decode(it.trim()) }
    }.getOrNull()?.takeIf { it.size == 32 }

    /**
     * Колбэк серьёзной угрозы: канал «отравлен» (ключ/поколение скомпрометированы)
     * либо устройство отозвано администратором. При значении != null уже записана
     * «могила» (AppTrap.installProtection). После этого клиент перестаёт общаться с API.
     */
    var onThreat: ((ThreatMode) -> Unit)? = null

    /** Полное число успешных ротаций поколений за текущий запуск. */
    var rotationsApplied: Int = 0
        private set

    /** Включён ли защищённый конверт (шифрование + подпись + anti-replay). */
    val isSecure: Boolean get() = anchor != null

    /** Доступен ли L2-конверт (сконфигурирован статический ключ сервера). */
    val isL2Available: Boolean get() = srvXPub != null

    // ---- Синхронизация часов с сервером (x-azrael-srv-ts в каждом ответе) ----
    private var srvOffsetSec: Long = 0L

    /** Текущее время с поправкой на часы сервера (окно коррекции ±1 час). */
    fun nowAdjusted(): Long = System.currentTimeMillis() / 1000 + srvOffsetSec

    private fun applyServerTime(resp: HttpResult) {
        val srv = resp.headers[AppSecure.SRV_TS]?.trim()?.toLongOrNull() ?: return
        val delta = srv - System.currentTimeMillis() / 1000
        if (delta in -3600L..3600L) srvOffsetSec = delta
    }

    // ---- Токен сессии и ключи установки (Ed25519) ----
    private var token: String? = null

    /** Сохранённые учётные данные аккаунта (галочка «Запомнить аккаунт»). */
    private var savedLogin: String? = null
    private var savedPassword: String? = null

    /** Ключи установки: приватный+публичный raw-32 Ed25519 и вычисленный devId. */
    private class DeviceKeys(val devId: String, val priv: ByteArray, val pub: ByteArray)

    private var device: DeviceKeys? = loadPersistedState()

    private fun loadPersistedState(): DeviceKeys? {
        val raw = AppVault.read() ?: return null
        val obj = runCatching { json.parseToJsonElement(raw).jsonObject }.getOrNull() ?: return null
        token = obj["token"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
        savedLogin = obj["login"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
        savedPassword = obj["pass"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
        val devId = obj["devId"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() } ?: return null
        val priv = obj["devPriv"]?.jsonPrimitive?.content
            ?.let { runCatching { Base64Codec.decode(it) }.getOrNull() } ?: return null
        val pub = obj["devPub"]?.jsonPrimitive?.content
            ?.let { runCatching { Base64Codec.decode(it) }.getOrNull() } ?: return null
        if (priv.size != 32 || pub.size != 32) return null
        return DeviceKeys(devId, priv, pub)
    }

    private fun persistState() {
        val dev = device
        val state = if (dev == null) buildJsonObject {
            token?.let { put("token", it) }
            savedLogin?.let { put("login", it) }
            savedPassword?.let { put("pass", it) }
        } else buildJsonObject {
            put("devId", dev.devId)
            put("devPriv", Base64Codec.encode(dev.priv))
            put("devPub", Base64Codec.encode(dev.pub))
            token?.let { put("token", it) }
            savedLogin?.let { put("login", it) }
            savedPassword?.let { put("pass", it) }
        }
        AppVault.write(state.toString())
    }

    fun sessionToken(): String? = token

    /** Логин сохранённой учётной записи (пусто — ничего не сохранено). */
    fun rememberedLogin(): String = savedLogin ?: ""

    /** Есть ли сохранённые учётные данные для автозаполнения формы входа. */
    fun hasRememberedAccount(): Boolean = !savedLogin.isNullOrBlank()

    /** Галочка «Запомнить аккаунт»: сохранить или стереть логин/пароль локально. */
    fun setRememberAccount(login: String?, password: String?) {
        val l = login?.trim()?.takeIf { it.isNotBlank() }
        savedLogin = l
        savedPassword = if (l == null) null else password?.takeIf { it.isNotBlank() }
        persistState()
    }

    fun setToken(t: String?) {
        token = t
        persistState()
    }

    /** Привязана ли установка (есть ключи + devId). */
    fun isDeviceBound(): Boolean = device != null

    /** Идентификатор установки для сервера (sha256 от deviceId + публичного ключа). */
    fun deviceId(): String? = device?.devId

    /**
     * Первый запуск программы: привязка установки по одноразовому ключу (recKey),
     * который пользователь видит на сайте (/main → Настройки) ровно один раз.
     * Сервер возвращает session-токен; ключи установки и токен уходят в AppVault.
     */
    fun bindDevice(
        provisionKey: String,
        label: String = platformName().take(32),
        expectedUsername: String? = null
    ): JsonObject {
        val data = callBind(provisionKey, label, expectedUsername)
        persistBinding()
        return data
    }

    /**
     * device.bind без записи состояния: вызывающий сам решает, принять ли привязку.
     *
     * [expectedUsername] — логин, под которым открывается форма входа. Сервер
     * сверяет его с владельцем ключа ДО записи, поэтому ключ чужого аккаунта
     * не создаёт привязку и не включает анти-угон (заморозку обоих аккаунтов).
     */
    private fun callBind(
        provisionKey: String,
        label: String = platformName().take(32),
        expectedUsername: String? = null
    ): JsonObject {
        val kp = device ?: Crypto.ed25519KeyPair().let {
            val pubB64 = Base64Codec.encode(it.publicKey)
            DeviceKeys(devIdFrom(AppTrap.deviceId(), pubB64), it.privateKey, it.publicKey)
        }
        val data = call(AppApi.DEVICE_BIND, buildJsonObject {
            put("key", provisionKey.trim())
            put("devId", kp.devId)
            put("devPub", Base64Codec.encode(kp.pub))
            put("platform", platformName().take(32))
            put("label", label.trim().take(64))
            expectedUsername?.trim()?.takeIf { it.isNotEmpty() }?.let { put("username", it) }
        }, session = null)
        pendingBind = kp
        token = data["token"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() } ?: token
        return data
    }

    /** Ключи установки, принятые привязкой, но ещё не записанные в AppVault. */
    private var pendingBind: DeviceKeys? = null

    private fun persistBinding() {
        val kp = pendingBind ?: return
        device = kp
        pendingBind = null
        persistState()
    }

    private fun str(o: JsonObject, key: String): String =
        o[key]?.jsonPrimitive?.content?.trim().orEmpty()

    private fun obj(o: JsonObject, key: String): JsonObject =
        o[key]?.let { runCatching { it.jsonObject }.getOrNull() } ?: JsonObject(emptyMap())

    /**
     * Регистрация нового аккаунта и привязка установки в один шаг.
     *
     * Сервер сам выдаёт новому аккаунту индивидуальный ключ привязки
     * (provisionKey) — пользователь его не вводит и не видит, программа
     * привязывается сразу. Возвращает {username, role, lang, uid, new_user}.
     */
    fun authRegister(
        username: String,
        password: String,
        inviteCode: String,
        gender: String,
        avatarData: String? = null,
        avatarMime: String? = null
    ): JsonObject {
        if (isDeviceBound()) {
            throw AppException(AppErrorCode.VALIDATION, "device already bound")
        }
        val data = call(AppApi.AUTH_REGISTER, buildJsonObject {
            put("username", username.trim())
            put("password", password)
            put("inviteCode", inviteCode.trim())
            put("gender", gender.trim())
            avatarData?.let { put("avatarData", it) }
            avatarMime?.let { put("avatarMime", it) }
        }, session = null)
        val prov = str(data, "provisionKey").takeIf { it.isNotBlank() }
            ?: throw AppException(AppErrorCode.MALFORMED, "provision key missing")
        // Регистрация создала аккаунт и выдала сессию; привязываем установку его ключом.
        try {
            callBind(prov, expectedUsername = username)
            persistBinding()
        } catch (e: Throwable) {
            pendingBind = null
            throw e
        }
        return data
    }

    /**
     * Вход по логину/паролю + привязка установки индивидуальным ключом аккаунта.
     *
     * Ключ и логин обязаны принадлежать одному аккаунту: сервер при `device.bind`
     * с чужим ключом заморозил бы оба аккаунта (анти-угон), поэтому логин передаётся
     * на сервер как ожидаемый владелец ключа, и сверка идёт ДО записи привязки.
     * Дублируем проверку на клиенте и откатываем временный токен при любой ошибке,
     * чтобы не остаться с «полувходом» в памяти.
     */
    fun authLogin(username: String, password: String, provisionKey: String): JsonObject {
        val prevToken = token
        val data = call(AppApi.AUTH_LOGIN, buildJsonObject {
            put("username", username.trim())
            put("password", password)
        }, session = null)
        val loginName = str(data, "username")
        token = str(data, "token").takeIf { it.isNotBlank() } ?: prevToken

        try {
            if (isDeviceBound()) {
                // Установка уже за аккаунтом: подпись устройства сильнее сессии, поэтому
                // сначала узнаём, кому она принадлежит, и не даём войти под чужим именем.
                val boundName = runCatching { str(obj(homeBoot(), "profile"), "username") }
                    .getOrDefault("")
                if (boundName.isNotBlank() && loginName.isNotBlank() && !boundName.equals(loginName, true)) {
                    throw AppException(AppErrorCode.VALIDATION, "device bound to another account")
                }
                return data
            }

            val bound = callBind(provisionKey, expectedUsername = username)
            val boundName = str(bound, "username")
            if (boundName.isNotBlank() && loginName.isNotBlank() && !boundName.equals(loginName, true)) {
                throw AppException(AppErrorCode.VALIDATION, "provision key belongs to another account")
            }
            persistBinding()
            return data
        } catch (e: Throwable) {
            token = prevToken
            pendingBind = null
            throw e
        }
    }

    /** Текущее состояние привязки на сервере. */
    fun deviceStatus(): JsonObject = call(
        AppApi.DEVICE_STATUS,
        buildJsonObject { device?.devId?.let { put("devId", it) } }
    )

    /** Все установки аккаунта: {devices:[{devId,platform,label,status,rotations,boundAt,lastSeen,revokedAt}], active, max}. */
    fun deviceList(): JsonObject = call(AppApi.DEVICE_LIST)

    /** Отозвать установку (сайт или другое устройство): её сессии обрываются, клиент блокируется. */
    fun deviceRevoke(devId: String): JsonObject = call(
        AppApi.DEVICE_REVOKE,
        buildJsonObject { put("devId", devId) }
    )

    /**
     * Ротация ключа установки (клиент сам выбирает момент): запрос подписан СТАРЫМ
     * ключом, дальше действует новый. devId на сервере не меняется.
     */
    fun rotateDeviceKey(): JsonObject {
        val kp = device ?: throw AppException(AppErrorCode.VALIDATION, "device is not bound")
        val fresh = Crypto.ed25519KeyPair()
        val data = call(AppApi.DEVICE_ROTATE, buildJsonObject {
            put("devId", kp.devId)
            put("newPub", Base64Codec.encode(fresh.publicKey))
        })
        device = DeviceKeys(kp.devId, fresh.privateKey, fresh.publicKey)
        persistState()
        return data
    }

    /** devId = sha256hex("AZRAEL-DEV|<localDeviceId>|<pubB64>") — зеркало lib/app-device.ts. */
    private fun devIdFrom(localDeviceId: String, pubB64: String): String =
        Hex.toHex(Crypto.sha256("AZRAEL-DEV|$localDeviceId|$pubB64".toByteArray(Charsets.UTF_8)))

    /** Ed25519-подпись device-запроса: "AZRAEL-DEV|ts|nonce|sha256hex(body)". */
    private fun deviceHeaders(ts: Long, nonce: String, body: String): Map<String, String> {
        val kp = device ?: return emptyMap()
        val tsStr = ts.toString()
        val msg = "AZRAEL-DEV|$tsStr|$nonce|${Hex.toHex(Crypto.sha256(body.toByteArray(Charsets.UTF_8)))}"
        val sig = Crypto.ed25519Sign(kp.priv, msg.toByteArray(Charsets.UTF_8))
        return mapOf(
            AppSecure.HDR_DEV to kp.devId,
            AppSecure.HDR_DEV_TS to tsStr,
            AppSecure.HDR_DEV_SIG to Base64Codec.encode(sig)
        )
    }

    // ---- L2: трёхсторонний конверт (программа ↔ сайт ↔ внутренний роут) ----
    private class L2Seal(
        val args: JsonObject,
        val ephPubB64: String,
        val respKey: ByteArray,
        val ts: Long,
        val nonce: String
    )

    private fun sealL2(realArgs: JsonObject, ts: Long, nonce: String, op: String): L2Seal {
        val srvPub = srvXPub ?: throw AppException(AppErrorCode.L2_UNAVAILABLE, "l2 server key not configured")
        val eph = Crypto.x25519KeyPairRaw()
        val shared = Crypto.x25519SharedRaw(eph.privateKey, srvPub)
        val reqKey = Crypto.hkdfSha256(
            shared, L2_SALT, "AZRAEL-APP|L2|v1|req|$ts|$nonce|$op".toByteArray(Charsets.UTF_8), 32
        )
        val respKey = Crypto.hkdfSha256(
            shared, L2_SALT, "AZRAEL-APP|L2|v1|resp|$ts|$nonce".toByteArray(Charsets.UTF_8), 32
        )
        val inner = json.encodeToString(JsonObject.serializer(), buildJsonObject { put("args", realArgs) })
        val iv = randomBytes(AppSecure.IV_SIZE)
        val aad = "AZRAEL-SRV|$ts|$nonce|$op".toByteArray(Charsets.UTF_8)
        val ct = Crypto.encrypt(reqKey, aad, inner.toByteArray(Charsets.UTF_8), iv)
            ?: throw AppException(AppErrorCode.L2_UNAVAILABLE, "l2 encrypt failed")
        val env = buildJsonObject {
            put("iv", Base64Codec.encode(iv))
            put("ct", Base64Codec.encode(ct))
        }
        return L2Seal(
            args = buildJsonObject { put("__l2__", env) },
            ephPubB64 = Base64Codec.encode(eph.publicKey),
            respKey = respKey,
            ts = ts,
            nonce = nonce
        )
    }

    /** Расшифровать внутренний L2-ответ: {err, data|error}. */
    private fun openL2(data: JsonObject, respKey: ByteArray, ts: Long, nonce: String): JsonObject {
        val env = data["__l2__"]?.jsonObject
            ?: throw AppException(AppErrorCode.MALFORMED, "l2 response envelope missing")
        val iv = env["iv"]?.jsonPrimitive?.content?.let { runCatching { Base64Codec.decode(it) }.getOrNull() }
        val ct = env["ct"]?.jsonPrimitive?.content?.let { runCatching { Base64Codec.decode(it) }.getOrNull() }
        if (iv == null || ct == null || iv.size != AppSecure.IV_SIZE || ct.size < 16) {
            throw AppException(AppErrorCode.MALFORMED, "l2 envelope invalid")
        }
        val aad = "AZRAEL-SRV-RESP|$ts|$nonce".toByteArray(Charsets.UTF_8)
        val plain = Crypto.decrypt(respKey, aad, ct, iv)
            ?: throw AppException(AppErrorCode.MALFORMED, "l2 decrypt failed")
        val inner = runCatching { json.parseToJsonElement(String(plain, Charsets.UTF_8)).jsonObject }.getOrNull()
            ?: throw AppException(AppErrorCode.MALFORMED, "l2 malformed response")
        val err = inner["err"]?.jsonPrimitive?.content?.toIntOrNull() ?: AppErrorCode.MALFORMED
        if (err != AppErrorCode.OK) {
            throw AppException(err, inner["error"]?.jsonPrimitive?.content ?: "err=$err")
        }
        return inner["data"]?.jsonObject ?: JsonObject(emptyMap())
    }

    /**
     * Базовый запрос: возвращает data-объект, бросает AppException при err или нарушении подписи.
     * [timeoutMs] — сетевой таймаут (LLM-операциям нужен больший), [allowProtected] —
     * единственный пропуск tombstone-проверки: так снимается блокировка по подписанной метке release.
     */
    fun call(
        op: String,
        args: JsonObject = JsonObject(emptyMap()),
        session: String? = token,
        timeoutMs: Int = DEFAULT_TIMEOUT_MS,
        allowProtected: Boolean = false
    ): JsonObject {
        if (AppTrap.isProtected() && !allowProtected) {
            throw AppException(AppErrorCode.FORBIDDEN, "device is in protected mode")
        }
        try {
            return callOnce(op, args, session, timeoutMs)
        } catch (e: AppException) {
            // Рассинхрон часов: сервер вернул 102, а x-azrael-srv-ts уже обновил offset — повтор.
            if (e.code == AppErrorCode.TS_SKEW) return callOnce(op, args, session, timeoutMs)
            throw e
        }
    }

    private fun callOnce(op: String, args: JsonObject, session: String?, timeoutMs: Int): JsonObject {
        val key = currentKey
        val ts = nowAdjusted()
        val nonce = Base64Codec.encode(randomBytes(AppSecure.NONCE_SIZE))
        val useL2 = key != null && op in AppApi.L2_OPS && session != null && srvXPub != null
        val l2 = if (useL2) sealL2(args, ts, nonce, op) else null
        val plainJson = json.encodeToString(
            AppRequest.serializer(),
            AppRequest(op = op, args = l2?.args ?: args, session = session, deviceId = AppTrap.deviceId())
        )
        val resp: HttpResult
        if (key != null) {
            val sealed = AppSecure.seal(key, plainJson, ts, nonce)
            val headers = buildMap {
                put(AppSecure.HDR_VERSION, AppSecure.VERSION.toString())
                put(AppSecure.HDR_TS, sealed.ts.toString())
                put(AppSecure.HDR_NONCE, sealed.nonce)
                put(AppSecure.HDR_SIG, sealed.sig)
                put(AppSecure.HDR_KG, gen.toString())
                if (l2 != null) {
                    put(AppSecure.HDR_L2, "1")
                    put(AppSecure.HDR_EPH, l2.ephPubB64)
                }
                if (op !in AppApi.DEVICE_UNSIGNED_OPS) putAll(deviceHeaders(sealed.ts, sealed.nonce, sealed.body))
            }
            resp = httpPostJsonWithHeaders(baseUrl, sealed.body, headers, timeoutMs = timeoutMs)
            applyServerTime(resp)
            if (resp.body == null) throw AppException(AppErrorCode.NETWORK, "network: no response")
            // Двусторонняя аутентификация: сайт должен знать тот же ключ (никакого доверия DNS/прокси).
            if (!AppSecure.verifyResponse(key, resp, nowAdjusted())) {
                throw AppException(AppErrorCode.FORBIDDEN, "bad server signature")
            }
            // Канал может быть «отравлен» (скомпрометированный ключ/отзыв устройства): конверт
            // валиден, но содержимое — белеберда. Настоящий клиент уходит в защиту: пишет
            // «могилу» и блокируется, чтобы не «вылечиться» переустановкой.
            if (AppSecure.isResponsePoisoned(resp)) {
                AppTrap.installProtection()
                val mode = if (resp.headers[AppSecure.RESP_TRAP] == "1") ThreatMode.TRAPPED else ThreatMode.POISONED
                onThreat?.invoke(mode)
                throw AppException(AppErrorCode.FORBIDDEN, "channel compromised: $mode")
            }
            // Разблокировка приходит ТОЛЬКО подписанной с сайта меткой (release).
            if (AppSecure.isResponseRelease(resp)) AppTrap.removeProtection()
            // Ротация ключа: соль следующего поколения приходит в подписанном ответе.
            val next = AppSecure.nextGeneration(resp)
            if (next != null && next.first == gen + 1L && anchor != null) {
                gen = next.first
                currentKey = AppSecure.deriveGenerationKey(anchor, next.first, next.second)
                rotationsApplied += 1
            }
        } else {
            val raw = httpPostJson(baseUrl, plainJson, timeoutMs = timeoutMs)
                ?: throw AppException(AppErrorCode.NETWORK, "network: no response")
            resp = HttpResult(raw, 200, emptyMap())
        }
        val root = runCatching {
            json.parseToJsonElement(resp.body ?: "").jsonObject
        }.getOrElse { throw AppException(AppErrorCode.MALFORMED, "malformed response") }
        val err = root["err"]?.jsonPrimitive?.content?.toIntOrNull() ?: AppErrorCode.MALFORMED
        if (err != AppErrorCode.OK) {
            throw AppException(err, root["error"]?.jsonPrimitive?.content ?: "err=$err")
        }
        val data = root["data"]?.jsonObject ?: JsonObject(emptyMap())
        return if (l2 != null) openL2(data, l2.respKey, l2.ts, l2.nonce) else data
    }

    // --- system / device / provision ---
    fun systemHealth(): JsonObject = call(AppApi.SYSTEM_HEALTH)

    /**
     * Единственная операция, доступная при tombstone-защите: подписанный ответ сервера
     * может нести метку release — тогда AppTrap.removeProtection() снимает блокировку.
     * Вернёт true, если защита снята этим вызовом.
     */
    fun probeRelease(): Boolean {
        val wasProtected = AppTrap.isProtected()
        try {
            call(AppApi.SYSTEM_HEALTH, session = null, timeoutMs = 15_000, allowProtected = true)
        } catch (e: AppException) {
            if (e.isNetworkError) throw e
        }
        return wasProtected && !AppTrap.isProtected()
    }

    fun bind(provisionKey: String, label: String = platformName().take(32)): JsonObject =
        bindDevice(provisionKey, label)

    /** Выдать ключ привязки (виден ровно один раз) / узнать, есть ли он. */
    fun provisionKey(): JsonObject = call(AppApi.PROVISION_KEY)
    fun provisionInfo(): JsonObject = call(AppApi.PROVISION_INFO)
    fun provisionRegenerate(): JsonObject = call(AppApi.PROVISION_REGENERATE)

    // --- auth ---
    /**
     * Выход: сервер гасит сессию (best-effort), затем токен стирается локально.
     * Даже если сеть недоступна или канал защищён, локальный выход всегда завершается.
     */
    fun logout() {
        val current = token
        if (current != null && !AppTrap.isProtected()) {
            runCatching { call(AppApi.AUTH_LOGOUT, session = current) }
        }
        setToken(null)
    }

    fun verify(): JsonObject = call(AppApi.AUTH_VERIFY)
    fun changePassword(oldPassword: String, newPassword: String): JsonObject =
        call(AppApi.AUTH_CHANGE_PASSWORD, buildJsonObject {
            put("oldPassword", oldPassword); put("newPassword", newPassword)
        })

    /** Отозвать сессию: чужой токен или текущий (null — разорвать собственный доступ). */
    fun revokeSession(otherToken: String? = null): JsonObject =
        call(AppApi.SESSION_REVOKE, buildJsonObject { otherToken?.let { put("token", it) } })

    // --- profile ---
    fun profileUpdate(
        username: String? = null,
        displayName: String? = null,
        tag: String? = null,
        gender: String? = null
    ): JsonObject = call(AppApi.PROFILE_UPDATE, buildJsonObject {
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

    /**
     * Настройки приватности. Значения уходят НАСТОЯЩИМИ JSON-типами: булево — как boolean
     * (строка "0" на сервере истинна), остальное — строкой.
     */
    fun profilePrivacySet(values: Map<String, Any>): JsonObject =
        call(
            AppApi.PROFILE_PRIVACY_SET,
            JsonObject(values.mapValues { (_, v) ->
                if (v is Boolean) JsonPrimitive(v) else JsonPrimitive(v.toString())
            })
        )
    fun searchUsers(q: String): JsonObject =
        call(AppApi.PROFILE_SEARCH_USERS, buildJsonObject { put("q", q) })

    /**
     * Сменить язык аккаунта. Сервер принимает только ru/en/zh и отвечает
     * {ok:true, lang:"<код>"} — тем же кодом, который придёт в home.boot.lang.
     * Значение приходит и на сайт, и в другие устройства.
     */
    fun profileSetLang(lang: String): JsonObject =
        call(AppApi.PROFILE_SET_LANG, buildJsonObject { put("lang", lang.trim().lowercase()) })

    // --- home ---
    fun homeBoot(): JsonObject = call(AppApi.HOME_BOOT)

    // --- chats ---
    fun chatsList(): JsonObject = call(AppApi.CHATS_LIST)
    fun chatsListArchived(): JsonObject = call(AppApi.CHATS_LIST_ARCHIVED)
    fun chatsCreate(partnerUid: String): JsonObject =
        call(AppApi.CHATS_CREATE, buildJsonObject { put("partnerUid", partnerUid) })
    fun chatsOpen(chatId: Long): JsonObject =
        call(AppApi.CHATS_OPEN, buildJsonObject { put("chatId", chatId) })
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
    /** Онлайн/печатает по списку собеседников: {ok, online:{uid:true}, typing:{}}. */
    fun chatsPresence(userIds: List<Long>): JsonObject =
        call(AppApi.CHATS_PRESENCE, buildJsonObject {
            put("userIds", buildJsonArray { userIds.forEach { add(JsonPrimitive(it)) } })
        })
    fun chatsArchive(chatId: Long): JsonObject =
        call(AppApi.CHATS_ARCHIVE, buildJsonObject { put("chatId", chatId) })
    fun chatsRestore(chatId: Long): JsonObject =
        call(AppApi.CHATS_RESTORE, buildJsonObject { put("chatId", chatId) })

    /** Удалить СВОЁ сообщение по id. */
    fun chatsDelete(messageId: Long): JsonObject =
        call(AppApi.CHATS_DELETE, buildJsonObject { put("messageId", messageId) })

    /** Удалить чат целиком вместе с сообщениями и файлами. */
    fun chatsDeleteChat(chatId: Long): JsonObject =
        call(AppApi.CHATS_DELETE_CHAT, buildJsonObject { put("chatId", chatId) })

    fun chatsAutodeleteGet(chatId: Long? = null): JsonObject =
        call(AppApi.CHATS_AUTODELETE_GET, buildJsonObject { chatId?.let { put("chatId", it) } })

    /** Таймер удаления сообщений в чате: days = null — выключить. */
    fun chatsAutodeleteSet(chatId: Long?, days: Int?): JsonObject =
        call(AppApi.CHATS_AUTODELETE_SET, buildJsonObject {
            chatId?.let { put("chatId", it) }
            if (days == null) put("days", JsonNull) else put("days", days)
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

    /** QR-код короткой ссылки: {qr:"data:image/png;base64,…", url, original}. */
    fun shortenerQr(code: String): JsonObject =
        call(AppApi.SHORTENER_QR, buildJsonObject { put("code", code) })

    // --- invites ---
    fun invitesMyCode(): JsonObject = call(AppApi.INVITES_MY_CODE)
    fun invitesList(active: Boolean? = null): JsonObject =
        call(AppApi.INVITES_LIST, buildJsonObject { active?.let { put("active", it) } })
    fun invitesGenerate(): JsonObject = call(AppApi.INVITES_GENERATE)
    fun invitesArchive(id: Long): JsonObject =
        call(AppApi.INVITES_ARCHIVE, buildJsonObject { put("id", id) })
    fun invitesUnarchive(id: Long): JsonObject =
        call(AppApi.INVITES_UNARCHIVE, buildJsonObject { put("id", id) })

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
        call(
            AppApi.AI_CHAT_SEND,
            buildJsonObject {
                put("message", message); chatId?.let { put("chatId", it) }
            },
            timeoutMs = AI_TIMEOUT_MS
        )

    // --- admin (только для аккаунта с ролью admin/owner) ---
    /** {frozen:[{id,username,frozen_at}], devices:[…]} — замороженные аккаунты и установки. */
    fun adminFreezeList(): JsonObject = call(AppApi.ADMIN_FREEZE_LIST)

    /** Разморозить аккаунт по логину и вернуть его установки в активные. */
    fun adminUnfreeze(username: String): JsonObject =
        call(AppApi.ADMIN_UNFREEZE, buildJsonObject { put("username", username.trim()) })

    // --- otp ---
    fun otpGenerateSecret(): JsonObject = call(AppApi.OTP_GENERATE_SECRET)
    fun otpValidate(code: String): JsonObject =
        call(AppApi.OTP_VALIDATE, buildJsonObject { put("code", code) })

    private companion object {
        /** HKDF-Extract соль L2-слоя — зеркало HKDF_SALT в серверном lib/app-l2.ts. */
        val L2_SALT = "AZRAEL-APP|L2|v1|salt".toByteArray(Charsets.UTF_8)

        const val DEFAULT_TIMEOUT_MS = 20_000
        const val AI_TIMEOUT_MS = 150_000
    }
}
