package xyz.azraellab.shared.core.api

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import xyz.azraellab.shared.AppTrap
import xyz.azraellab.shared.AppVault
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Проверки личности установки ([AppInstall]): ключи Ed25519 и devId, которые нужны
 * ДО привязки аккаунта, потому что сервер выдаёт ключ канала по devId.
 *
 * Требования, из-за которых объект выделен отдельно от [AppClient]:
 *  - devId стабилен: повторный вызов не создаёт новых ключей (иначе уже выданный
 *    ключ канала перестал бы подходить, а сервер счёл бы установку другой);
 *  - запись ключей не затирает session-токен и сохранённый логин (общий блоб AppVault);
 *  - devId — ровно 64 hex-символа, как ждёт сервер (sanitizeKeyId/isDevId).
 *
 * desktop-AppVault читает user.home, поэтому каждый тест работает в своём временном
 * каталоге и не трогает настоящее хранилище.
 */
class AppInstallTest {

    private val json = Json { ignoreUnknownKeys = true }
    private var originalHome: String? = null
    private val homes: MutableList<Path> = mutableListOf()

    private fun isolatedVault() {
        val home = Files.createTempDirectory("azrael-install")
        if (originalHome == null) originalHome = System.getProperty("user.home")
        System.setProperty("user.home", home.toString())
        homes.add(home)
    }

    @AfterTest
    fun cleanupVaults() {
        originalHome?.let { System.setProperty("user.home", it) }
        originalHome = null
        homes.forEach { dir ->
            runCatching {
                Files.walk(dir).use { stream ->
                    stream.sorted(Comparator.reverseOrder()).forEach {
                        runCatching { Files.deleteIfExists(it) }
                    }
                }
            }
        }
        homes.clear()
    }

    private fun state(): kotlinx.serialization.json.JsonObject {
        val raw = assertNotNull(AppVault.read(), "состояние установки должно быть записано")
        return json.parseToJsonElement(raw).jsonObject
    }

    @Test
    fun ensureDeviceKeysCreatesHexDevIdAndKeyPair() {
        isolatedVault()
        val devId = AppInstall.ensureDeviceKeys()
        assertEquals(64, devId.length, "devId = sha256hex, ожидалось 64 символа")
        assertTrue(devId.all { it in "0123456789abcdef" }, "devId должен быть lowercase hex: $devId")
        val s = state()
        assertEquals(devId, s["devId"]?.jsonPrimitive?.content)
        assertEquals(44, s["devPub"]?.jsonPrimitive?.content?.length, "raw-32 публичный ключ в base64")
        assertEquals(44, s["devPriv"]?.jsonPrimitive?.content?.length, "raw-32 приватный ключ в base64")
    }

    /**
     * Ключи созданы ≠ привязка к аккаунту. Флаг `bound` обязан быть записан явно
     * как false, иначе AppClient примет такой vault за старый (где ключи означали
     * привязку) и запретит регистрацию на только что установленной программе.
     */
    @Test
    fun freshInstallIsNotMarkedAsBound() {
        isolatedVault()
        AppInstall.ensureDeviceKeys()
        assertEquals("false", state()["bound"]?.jsonPrimitive?.content)
    }

    @Test
    fun boundFlagSurvivesKeyRecreation() {
        isolatedVault()
        AppVault.write("""{"devId":"deadbeef","devPub":"AAAA","devPriv":"AAAA","bound":"true"}""")
        AppInstall.ensureDeviceKeys()
        assertEquals("true", state()["bound"]?.jsonPrimitive?.content, "привязка не должна теряться")
    }

    @Test
    fun ensureDeviceKeysIsIdempotentSoChannelKeyStaysValid() {
        isolatedVault()
        val first = AppInstall.ensureDeviceKeys()
        val second = AppInstall.ensureDeviceKeys()
        assertEquals(first, second, "повторный вызов обязан вернуть тот же devId")
        assertEquals(first, AppInstall.devId(), "devId должен читаться из того же блоба")
        assertTrue(AppInstall.hasDeviceKeys())
    }

    @Test
    fun devIdBeforeKeysIsNull() {
        isolatedVault()
        assertEquals(null, AppInstall.devId(), "до создания ключей devId неизвестен")
        assertTrue(!AppInstall.hasDeviceKeys())
    }

    @Test
    fun ensureDeviceKeysKeepsSessionTokenAndLogin() {
        isolatedVault()
        AppVault.write(
            """{"token":"tok-123","login":"azrael","pass":"secret-pass"}"""
        )
        AppInstall.ensureDeviceKeys()
        val s = state()
        assertEquals("tok-123", s["token"]?.jsonPrimitive?.content, "session-токен не должен затираться")
        assertEquals("azrael", s["login"]?.jsonPrimitive?.content, "сохранённый логин не должен затираться")
        assertEquals("secret-pass", s["pass"]?.jsonPrimitive?.content)
    }

    /**
     * Битые ключи (не 32 байта) не должны приводиться в молчаливое «всё хорошо»:
     * сервер отверг бы такой devId, поэтому клиент пересоздаёт пару целиком.
     */
    @Test
    fun brokenKeyPairIsRecreatedInsteadOfReused() {
        isolatedVault()
        AppVault.write("""{"devId":"deadbeef","devPub":"AAAA","devPriv":"AAAA","token":"tok-9","bound":"false"}""")
        val devId = AppInstall.ensureDeviceKeys()
        assertTrue(devId != "deadbeef" && devId.length == 64, "ожидалась новая пара ключей, получено $devId")
        val s = state()
        assertEquals("tok-9", s["token"]?.jsonPrimitive?.content, "токен переживает пересоздание ключей")
        assertEquals("false", s["bound"]?.jsonPrimitive?.content)
    }

    @Test
    fun devIdDerivationIsStableAndDependsOnPubKey() {
        val local = AppTrap.deviceId()
        val pubA = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
        val pubB = "BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB="
        assertEquals(AppInstall.devIdFrom(local, pubA), AppInstall.devIdFrom(local, pubA))
        assertTrue(AppInstall.devIdFrom(local, pubA) != AppInstall.devIdFrom(local, pubB))
        assertEquals(64, AppInstall.devIdFrom(local, pubA).length)
    }

    private val raw32B64 = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="

    private fun client(): AppClient = AppClient("https://example.invalid/api/app/v1", null)

    /**
     * Регрессия: после появления per-install ключей установка получает ключи ДО
     * привязки. Если считать это привязкой, клиент отказывает в регистрации
     * («device already bound») на только что установленной программе.
     */
    @Test
    fun clientWithKeysButNoBindingIsNotDeviceBound() {
        isolatedVault()
        AppInstall.ensureDeviceKeys()
        val c = client()
        assertTrue(c.hasDeviceKeys(), "ключи установки должны быть на месте")
        assertTrue(!c.isDeviceBound(), "чистая установка обязана допускать регистрацию")
        assertEquals(AppInstall.devId(), c.deviceId(), "клиент и AppInstall обязаны видеть один devId")
    }

    /** Vault, созданный до появления per-install ключей: там ключи = привязка. */
    @Test
    fun legacyVaultWithoutBoundFlagCountsAsBound() {
        isolatedVault()
        AppVault.write(
            """{"devId":"${"a".repeat(64)}","devPub":"$raw32B64","devPriv":"$raw32B64","token":"tok-legacy"}"""
        )
        val c = client()
        assertTrue(c.hasDeviceKeys())
        assertTrue(c.isDeviceBound(), "старый vault без флага должен считаться привязанным")
        assertEquals("tok-legacy", c.sessionToken(), "токен из старого vault не теряется")
    }
}
