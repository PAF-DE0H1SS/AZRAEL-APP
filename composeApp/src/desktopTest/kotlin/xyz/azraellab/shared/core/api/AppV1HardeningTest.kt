package xyz.azraellab.shared.core.api

import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import xyz.azraellab.shared.core.crypto.Base64Codec
import xyz.azraellab.shared.core.protocol.AppRuntime
import xyz.azraellab.shared.core.protocol.defaultAppSrvPubB64
import xyz.azraellab.shared.data.b
import xyz.azraellab.shared.data.i
import xyz.azraellab.shared.data.l
import xyz.azraellab.shared.data.model.DeviceDto
import xyz.azraellab.shared.data.s
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Регрессии по контракту клиент↔сервер app-v1.
 *
 * Три группы:
 *  1. L2 обязан быть fail-closed. Сервер определяет L2 не по таблице операций,
 *     а по заголовку `x-azrael-l2: 1` (app/api/app/v1/route.ts, `wantL2`).
 *     Клиент без конверта НЕ получает ошибку - запрос молча обрабатывается
 *     открытым. Значит «ключа L2 нет» нельзя выражать как `useL2 = false`.
 *  2. Коды ошибок клиента не должны совпадать с серверными 107/108, иначе
 *     `errText` покажет чужой текст на «нет ключа на устройстве».
 *  3. Ответ сервера - словарь; любой ключ может оказаться объектом/массивом.
 *     Разбор обязан давать «поля нет», а не исключение.
 */
class AppV1HardeningTest {

    private var originalHome: String? = null
    private var originalSrvXPub: String? = null
    private val homes: MutableList<Path> = mutableListOf()

    // Порт 1 - заведомо неслушаемый: если запрос всё-таки уйдёт в сеть,
    // ошибка будет NETWORK, а не L2_UNAVAILABLE. Так тест отличает «сработал
    // guard до отправки» от «упала сеть».
    private val deadUrl = "http://127.0.0.1:1/api/app/v1"

    private fun isolatedHome() {
        val home = Files.createTempDirectory("azrael-v1hard")
        if (originalHome == null) originalHome = System.getProperty("user.home")
        System.setProperty("user.home", home.toString())
        homes.add(home)
    }

    private fun writeVault(raw: String) {
        val dir = Path.of(System.getProperty("user.home"), ".config/azraellab")
        Files.createDirectories(dir)
        Files.write(dir.resolve("vault.json"), raw.toByteArray())
    }

    @BeforeTest
    fun rememberRuntime() {
        if (originalSrvXPub == null) originalSrvXPub = AppRuntime.srvXPubB64
    }

    @AfterTest
    fun restore() {
        originalHome?.let { System.setProperty("user.home", it) }
        originalHome = null
        AppRuntime.srvXPubB64 = originalSrvXPub
        originalSrvXPub = null
        homes.forEach { dir ->
            runCatching {
                Files.walk(dir).use { s ->
                    s.sorted(Comparator.reverseOrder()).forEach { runCatching { Files.deleteIfExists(it) } }
                }
            }
        }
        homes.clear()
    }

    // ---- 1. L2 fail-closed -------------------------------------------------

    @Test
    fun desktopL2KeyResolvesToRaw32ByDefault() {
        AppRuntime.srvXPubB64 = null
        val b64 = defaultAppSrvPubB64()
        assertTrue(b64 != null, "desktop обязан иметь встроенный X25519-ключ сервера")
        assertEquals(32, Base64Codec.decode(b64!!).size, "ключ L2 - это raw-32 публичного ключа")
    }

    @Test
    fun runtimeKeyIsUsedWhenItIsValid() {
        val good = Base64Codec.encode(ByteArray(32) { it.toByte() })
        AppRuntime.srvXPubB64 = good
        assertTrue(AppClient(deadUrl, ByteArray(32) { 7 }).isL2Available)
    }

    @Test
    fun brokenRuntimeKeyDisablesL2() {
        // Мусор в runtime-конфиге = отсутствие ключа, а не повод слать L2 открытым.
        AppRuntime.srvXPubB64 = "не-base64-мусор"
        assertFalse(AppClient(deadUrl, ByteArray(32) { 7 }).isL2Available)

        // Короткий (не 32 байта) ключ - тоже не ключ.
        AppRuntime.srvXPubB64 = Base64Codec.encode(ByteArray(16) { 1 })
        assertFalse(AppClient(deadUrl, ByteArray(32) { 7 }).isL2Available)
    }

    @Test
    fun l2OperationFailsClosedWithoutServerKey() {
        AppRuntime.srvXPubB64 = "не-base64-мусор"
        val client = AppClient(deadUrl, ByteArray(32) { 7 })
        val e = assertFailsWith<AppException> { client.chatsList() }
        assertEquals(
            AppErrorCode.L2_UNAVAILABLE, e.code,
            "без ключа L2 чаты обязаны падать локально, а не уходить открытым",
        )
    }

    @Test
    fun nonL2OperationIsNotBlockedByMissingL2Key() {
        AppRuntime.srvXPubB64 = "не-base64-мусор"
        val client = AppClient(deadUrl, ByteArray(32) { 7 })
        val e = assertFailsWith<AppException> { client.profileSetLang("en") }
        assertNotEquals(
            AppErrorCode.L2_UNAVAILABLE, e.code,
            "profile.setLang вне L2-слоя: отсутствие ключа не его дело",
        )
    }

    @Test
    fun everyL2OperationIsGuarded() {
        // Список L2_OPS - зеркало серверного. Если в него попадёт операция,
        // для неё обязана сработать та же защита.
        AppRuntime.srvXPubB64 = "не-base64-мусор"
        val client = AppClient(deadUrl, ByteArray(32) { 7 })
        for (op in AppApi.L2_OPS) {
            val e = assertFailsWith<AppException>("операция $op ушла в сеть без ключа L2") {
                client.call(op)
            }
            assertEquals(AppErrorCode.L2_UNAVAILABLE, e.code, "операция $op")
        }
    }

    // ---- 2. Коды ошибок ----------------------------------------------------

    @Test
    fun l2ClientCodesDoNotCollideWithServerCodes() {
        // Сервер: 107 = 'l2 unavailable', 108 = 'l2 envelope required'.
        // INTERNAL/FORBIDDEN - общие протокольные коды, они и равны 107/108;
        // ловить надо коллизию именно L2-кодов с ними, иначе errText для
        // «нет ключа L2 на устройстве» покажет текст серверной ошибки.
        assertEquals(107, AppErrorCode.INTERNAL)
        assertEquals(108, AppErrorCode.FORBIDDEN)
        for (l2 in listOf(AppErrorCode.L2_REQUIRED, AppErrorCode.L2_UNAVAILABLE)) {
            assertNotEquals(AppErrorCode.INTERNAL, l2, "L2-код не должен совпадать с 107")
            assertNotEquals(AppErrorCode.FORBIDDEN, l2, "L2-код не должен совпадать с 108")
        }
        assertNotEquals(AppErrorCode.L2_REQUIRED, AppErrorCode.L2_UNAVAILABLE)
        assertTrue(
            AppErrorCode.L2_REQUIRED > 119 && AppErrorCode.L2_UNAVAILABLE > 119,
            "клиентские коды берём из свободного диапазона 120+",
        )
    }

    @Test
    fun l2ServerErrorsBecomeClientL2Codes() {
        // Внешний роут (server_appv1.ts, ветка wantL2) - ровно эти пары код/текст
        // доходят до клиента.
        for (msg in listOf("l2 envelope required", "l2 eph required")) {
            assertEquals(AppErrorCode.L2_REQUIRED, l2AwareErrorCode(108, msg), msg)
        }
        for (msg in listOf("l2 unavailable", "l2 gateway error")) {
            assertEquals(AppErrorCode.L2_UNAVAILABLE, l2AwareErrorCode(107, msg), msg)
        }
        // Тексты внутреннего роута ('l2 disabled', 'bad l2 headers', …) клиент не
        // видит НИКОГДА: внешний роут запечатывает только успешный конверт
        // ({iv, ct}) и любое plaintext-ошибку подменяет на 107 'l2 gateway error'.
        // Проверяем это как защиту от ожидания несуществующего пути: 108 + такой
        // текст всё равно должен давать L2_REQUIRED, если когда-нибудь дойдёт.
        for (msg in listOf("l2 disabled", "bad l2 headers", "l2: session required")) {
            assertEquals(AppErrorCode.L2_REQUIRED, l2AwareErrorCode(108, msg), msg)
        }
    }

    @Test
    fun sealedL2PayloadKeepsBusinessErrorsUntouched() {
        // Расшифрованный конверт openL2() несёт НЕ ошибку транспорта L2, а обычный
        // err обработчика (например, гостевой Forbidden в чате). Если бы там
        // переписывали 108, гостю показали бы «нужен L2» вместо «доступ запрещён».
        for (msg in listOf("Forbidden", "Пользователь ограничил общение", "not a member")) {
            assertEquals(108, l2AwareErrorCode(108, msg), msg)
        }
    }

    @Test
    fun nonL2ForbiddenAndInternalErrorsKeepServerCodes() {
        // 108 - общий «Forbidden» (например, гостевой chats.archive), 107 - общая
        // внутренняя ошибка. Если бы remap шёл по коду, гостю показали бы «L2
        // недоступен» вместо «доступ запрещён». Плюс слова, где l2 - часть слова.
        for (msg in listOf("Forbidden", "Пользователь ограничил общение")) {
            assertEquals(108, l2AwareErrorCode(108, msg), msg)
        }
        for (msg in listOf(
            "Internal error", "MinIO unavailable", "File claim unavailable",
            "File storage unavailable", "gateway bad response", "gateway error",
        )) {
            assertEquals(107, l2AwareErrorCode(107, msg), msg)
        }
        // l2 внутри слова («mysql2») - не про L2-слой.
        assertEquals(107, l2AwareErrorCode(107, "mysql2 unavailable"))
        for (code in listOf(104, 109, 201, 204)) {
            assertEquals(code, l2AwareErrorCode(code, "l2 envelope required"), "код $code не должен переписываться")
        }
        assertEquals(108, l2AwareErrorCode(108, null))
        assertEquals(107, l2AwareErrorCode(107, ""))
    }

    // ---- 3. Разбор ответа как словаря --------------------------------------

    @Test
    fun primitiveAccessorsSurviveNonPrimitiveValues() {
        val o = buildJsonObject {
            put("nul", kotlinx.serialization.json.JsonNull)
            put("obj", buildJsonObject { put("a", 1) })
            put("arr", buildJsonArray { })
        }
        assertNull(o.s("obj"), "вложенный объект - это «поля нет», а не исключение")
        assertNull(o.s("arr"), "массив - это «поля нет», а не исключение")
        assertNull(o.s("nul"), "JsonNull - это «поля нет»")
        assertNull(o.s("missing"))
        assertNull(o.i("obj"))
        assertNull(o.l("arr"))
        assertFalse(o.b("obj"), "не-boolean не должен читаться как true")
        assertFalse(o.b("nul"))
    }

    @Test
    fun deviceStatusUnboundIsNotMistakenForStatus() {
        // Сервер для непривязанной установки: `{ bound: false, devId: null }`.
        // Поля `status` там нет вовсе - раньше UI показывал «?» вместо «не привязано».
        val d = DeviceDto.from(buildJsonObject {
            put("bound", false)
            put("devId", kotlinx.serialization.json.JsonNull)
        })
        assertFalse(d.bound, "нет ни bound=true, ни status - установка не привязана")
        assertNull(d.status)
        assertEquals("", d.devId)
    }

    @Test
    fun deviceStatusParsesBoundFlagAndLegacyShape() {
        val bound = DeviceDto.from(buildJsonObject {
            put("bound", true)
            put("status", "active")
        })
        assertTrue(bound.bound)
        assertEquals("active", bound.status)

        // Старая форма (без поля bound) - привязка выводится из наличия status,
        // иначе существующие установки выглядели бы непривязанными.
        val legacy = DeviceDto.from(buildJsonObject { put("status", "active") })
        assertTrue(legacy.bound, "legacy-ответ без bound обязан остаться привязанным")
    }

    // ---- 4. Битый vault ----------------------------------------------------

    @Test
    fun corruptedVaultDoesNotCrashStartup() {
        isolatedHome()
        writeVault("{это не json")
        // Разбор vault идёт в инициализаторе клиента, то есть ровно на старте
        // программы. Раньше повреждённый vault ронял запуск на исключении.
        val client = AppClient(deadUrl, ByteArray(32) { 3 })
        assertTrue(client.isSecure, "клиент поднялся: битый vault не должен мешать старту")
    }

    @Test
    fun vaultWithUnexpectedShapesDoesNotCrashStartup() {
        isolatedHome()
        // Поля с чужими типами: server не гарантирует форму каждого ключа.
        writeVault("""{"device":"строка вместо объекта","token":42,"private":"x"}""")
        val client = AppClient(deadUrl, ByteArray(32) { 3 })
        assertTrue(client.isSecure)
    }

    @Test
    fun emptyVaultFileIsTreatedAsFreshInstall() {
        isolatedHome()
        writeVault("   ")
        val client = AppClient(deadUrl, ByteArray(32) { 3 })
        assertTrue(client.isSecure)
    }
}
