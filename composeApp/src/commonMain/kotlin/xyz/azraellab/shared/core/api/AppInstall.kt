package xyz.azraellab.shared.core.api

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import xyz.azraellab.shared.AppTrap
import xyz.azraellab.shared.AppVault
import xyz.azraellab.shared.Hex
import xyz.azraellab.shared.core.crypto.Base64Codec
import xyz.azraellab.shared.core.crypto.Crypto
import xyz.azraellab.shared.data.s
import xyz.azraellab.shared.logAzraelError

/**
 * Личность установки: пара ключей Ed25519 и вычисленный из них [devId].
 *
 * Ключи создаются ДО привязки аккаунта, потому что devId нужен самому первому
 * обращению к серверу — получению ключа канала (`GET /api/app/bootstrap?devId=…`).
 * Сервер выдаёт по devId отдельный ключ канала, привязанный к этой установке, так
 * что общий мастер-ключ не покидает сервер, а перехват ответа на чужом устройстве
 * не даёт ни ключа этой, ни ключей остальных установок.
 *
 * devId с этих ключей НЕ меняется при ротации ([AppClient.rotateDeviceKey]
 * подписывает запрос старым ключом и сохраняет devId), поэтому уже выданный ключ
 * канала остаётся верным. Переустановка программы с очисткой хранилища — это новая
 * установка с новым devId и новым ключом канала; старую привязку снимает сайт.
 *
 * Хранилище то же, что у [AppClient] (`AppVault`): одни и те же поля `devId`,
 * `devPriv`, `devPub` в одном JSON-блобе. Запись — read-modify-write, поэтому
 * session-токен и сохранённые учётные данные не теряются.
 */
object AppInstall {

    private val json = Json { ignoreUnknownKeys = true }

    /** devId = sha256hex("AZRAEL-DEV|<localDeviceId>|<pubB64>") — зеркало lib/app-device.ts. */
    fun devIdFrom(localDeviceId: String, pubB64: String): String =
        Hex.toHex(Crypto.sha256("AZRAEL-DEV|$localDeviceId|$pubB64".toByteArray(Charsets.UTF_8)))

    /**
     * Ключи установки, создавая их при первом обращении. Возвращает devId — он
     * нужен и для bootstrap ключа канала, и для подписи device-запросов.
     */
    fun ensureDeviceKeys(): String {
        try {
            return ensureDeviceKeysInner()
        } catch (e: Throwable) {
            logAzraelError("AzraelInstall", "ensureDeviceKeys failed", e)
            throw e
        }
    }

    private fun ensureDeviceKeysInner(): String {
        val state = readState()
        savedDevId(state)?.let { return it }
        val pair = Crypto.ed25519KeyPair()
        val devId = devIdFrom(AppTrap.deviceId(), Base64Codec.encode(pair.publicKey))
        // Создание ключей — это ещё не привязка к аккаунту, и флаг `bound` обязан
        // стоять явно: иначе AppClient примет такой vault за старый, где ключи
        // появлялись только при привязке, и запретит регистрацию на чистой установке.
        val bound = state.s("bound") == "true"
        val merged = buildJsonObject {
            state.forEach { (k, v) -> put(k, v) }
            put("devId", devId)
            put("devPriv", Base64Codec.encode(pair.privateKey))
            put("devPub", Base64Codec.encode(pair.publicKey))
            put("bound", if (bound) "true" else "false")
        }
        AppVault.write(merged.toString())
        return devId
    }

    /** Сохранённый devId без создания ключей (null — установка ещё не создана). */
    fun devId(): String? = savedDevId(readState())

    /** Готовы ли ключи установки (нужны для подписи device-запросов). */
    fun hasDeviceKeys(): Boolean {
        val state = readState()
        val priv = b64(state, "devPriv")
        val pub = b64(state, "devPub")
        return savedDevId(state) != null && priv?.size == 32 && pub?.size == 32
    }

    private fun readState(): JsonObject {
        val raw = AppVault.read() ?: return JsonObject(emptyMap())
        return runCatching { json.parseToJsonElement(raw).jsonObject }.getOrNull() ?: JsonObject(emptyMap())
    }

    /** devId принимается только вместе с полными ключами: иначе придётся пересоздать. */
    private fun savedDevId(state: JsonObject): String? {
        val devId = state.s("devId")?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        if (b64(state, "devPriv")?.size != 32 || b64(state, "devPub")?.size != 32) return null
        return devId
    }

    private fun b64(state: JsonObject, field: String): ByteArray? {
        val text = state.s(field) ?: return null
        return runCatching { Base64Codec.decode(text) }.getOrNull()
    }
}
