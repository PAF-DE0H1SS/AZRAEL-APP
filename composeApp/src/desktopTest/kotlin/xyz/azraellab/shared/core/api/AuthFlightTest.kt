package xyz.azraellab.shared.core.api

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Flight-тест новой авторизации против живого сервера `/api/app/v1`.
 *
 * Адрес берётся из env AZRAEL_APP_URL (по умолчанию прод:
 * https://azrael-lab.xyz/api/app/v1). Если сервер недоступен или ключевой env
 * не задан вовсе — тест тихо пропускается, чтобы CI без сети оставался зелёным.
 *
 * Сценарий:
 *  - ключ канала берётся с открытой точки /api/app/bootstrap (его не вводит пользователь);
 *  - с этим ключом клиент работает в защищённом режиме (isSecure);
 *  - auth.login с неверным паролем отклоняется кодом 201;
 *  - auth.register с несуществующим инвайт-кодом отклоняется кодом 203, аккаунт не создан;
 *  - device.bind с чужим ключом привязки отклоняется кодом 201 и НЕ создаёт привязку
 *    (анти-угон с заморозкой обоих аккаунтов не срабатывает);
 *  - полный путь нового экрана: регистрация по инвайт-коду (AZRAEL_FLIGHT_INVITE) с аватаром
 *    и полом → home.boot → выход → вход по логину/паролю с ключом привязки.
 *
 * Каждый клиент получает СВОЙ временный vault (desktop-AppVault читает user.home), поэтому
 * тесты не зависят от порядка и не трогают настоящее хранилище разработчика.
 *
 * Серверный E2E (e2e_authui.cjs) дополнительно проверяет лимиты и негативные случаи,
 * которым нужен прямой доступ к БД.
 */
class AuthFlightTest {

    private fun apiUrl(): String =
        System.getenv("AZRAEL_APP_URL")?.takeIf { it.isNotBlank() } ?: DEFAULT_URL

    /** Настоящий user.home запоминается один раз, временные каталоги чистятся после теста. */
    private var originalHome: String? = null
    private val homes: MutableList<Path> = mutableListOf()

    /**
     * Клиент с реально полученным ключом и изолированным хранилищем;
     * null — сервер недоступен, тест пропускается. Хранилище удаляется после теста.
     */
    private fun liveClient(): AppClient? {
        val url = apiUrl()
        val key = AppKeyBootstrap.fetch(url) ?: run {
            println("[flight] $url недоступен — тест пропускается")
            return null
        }
        val home = Files.createTempDirectory("azrael-flight")
        if (originalHome == null) originalHome = System.getProperty("user.home")
        System.setProperty("user.home", home.toString())
        homes.add(home)
        return AppClient(url, key)
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

    private fun <T> skipOr(block: (AppClient) -> T) {
        val c = liveClient() ?: return
        block(c)
    }

    @Test
    fun bootstrapKeyIsFetchedAndUsedForSecureChannel() {
        val url = apiUrl()
        val key = AppKeyBootstrap.fetch(url) ?: run {
            println("[flight] $url недоступен — тест пропущен")
            return
        }
        assertEquals(32, key.size, "ключ канала должен быть 32 байта (AES-256)")
        assertTrue(AppKeyBootstrap.bootstrapUrl(url).endsWith("/api/app/bootstrap"))
        assertTrue(AppClient(url, key).isSecure, "клиент с bootstrap-ключом обязан быть в защищённом режиме")
    }

    @Test
    fun wrongPasswordRejected() = skipOr { c ->
        val err = runCatching {
            c.authLogin("azrael_flight_missing", "definitely-not-a-password-1", "")
        }.exceptionOrNull()
        assertNotNull(err, "вход с неверным паролем обязан упасть")
        assertEquals(AppErrorCode.INVALID_CRED, (err as AppException).code, "ожидался код 201: ${(err as AppException).message}")
    }

    @Test
    fun registerWithForeignInviteRejected() = skipOr { c ->
        val err = runCatching {
            c.authRegister(
                username = "azrael_flight_missing",
                password = "definitely-not-a-password-1",
                inviteCode = "FLIGHT-INVITE-NOT-EXIST",
                gender = "male"
            )
        }.exceptionOrNull()
        assertNotNull(err, "регистрация с несуществующим инвайт-кодом обязана упасть")
        assertEquals(AppErrorCode.INVITE_BAD, (err as AppException).code, "ожидался код 203: ${(err as AppException).message}")
    }

    @Test
    fun bindWithForeignKeyRejected() = skipOr { c ->
        val foreignKey = "F" + "0".repeat(31)
        val err = runCatching {
            c.bindDevice(foreignKey, expectedUsername = "azrael_flight_missing")
        }.exceptionOrNull()
        assertNotNull(err, "чужой ключ привязки обязан быть отклонён")
        assertEquals(AppErrorCode.INVALID_CRED, (err as AppException).code, "ожидался код 201: ${(err as AppException).message}")
    }

    /**
     * Полный сценарий нового экрана: регистрация с инвайт-кодом, аватаром и полом →
     * home.boot → выход → повторный вход по логину/паролю с тем же ключом привязки.
     *
     * Инвайт-код берётся из env AZRAEL_FLIGHT_INVITE: без него тест пропускается, так как
     * создать код может только владелец (или вставка напрямую в БД — это серверный E2E).
     */
    @Test
    fun registerThenLogoutAndLoginRoundTrip() {
        val invite = System.getenv("AZRAEL_FLIGHT_INVITE")?.takeIf { it.isNotBlank() } ?: run {
            println("[flight] нет AZRAEL_FLIGHT_INVITE — регистрация пропущена")
            return
        }
        val tag = System.nanoTime().toString().takeLast(6)
        val name = "azrael_flt$tag"
        val pass = "Flight-$tag-pw"
        val first = liveClient() ?: return

        // 1. Чистая установка не привязана; ключ привязки выдаёт сервер — его не вводят.
        assertTrue(!first.isDeviceBound(), "чистый клиент не должен быть привязан")

        // 2. Регистрация: логин, пароль, инвайт, пол, аватар (1×1 PNG).
        val data = first.authRegister(
            username = name,
            password = pass,
            inviteCode = invite,
            gender = "female",
            avatarData = PNG_1PX,
            avatarMime = "image/png"
        )
        assertEquals(name, data["username"]?.toString()?.trim('"'), "сервер вернул другой логин")
        assertTrue(first.isDeviceBound(), "регистрация обязана привязать установку")
        val provision = data["provisionKey"]?.toString()?.trim('"').orEmpty()
        assertTrue(provision.length >= 8, "сервер обязан вернуть ключ привязки один раз")

        // 3. Профиль и главный экран доступны сразу; язык приходит в home.boot.
        val boot = first.homeBoot()
        assertTrue(boot.toString().contains("tabConfig"), "home.boot без tabConfig: ${boot.keys}")
        first.profileUpdate(displayName = "Flight $tag", gender = "female")

        // 4. Выход и повторный вход тем же аккаунтом с ключом привязки (как у пользователя,
        //    который вошёл на втором устройстве) — главный экран снова доступен.
        first.logout()
        val second = liveClient() ?: return
        second.authLogin(name, pass, provision)
        assertTrue(second.homeBoot().toString().contains("tabConfig"), "повторный вход не открыл главный экран")

        // 5. Уборка: отзываем ТОЛЬКО другие установки теста. Отзыв собственной приводит к
        //    poison+trap от сервера, а могила ставится на всю машину — на dev-хосте это ломает
        //    остальные тесты. Пользователя и его одну установку удаляет mkinvite.cjs cleanup.
        runCatching {
            val own = second.deviceId()
            val devices = (second.deviceList()["devices"] as? JsonArray)
                ?.mapNotNull { it as? JsonObject } ?: emptyList()
            devices.forEach { d ->
                val id = d["devId"]?.jsonPrimitive?.contentOrNull
                if (id != null && id != own) runCatching { second.deviceRevoke(id) }
            }
            second.logout()
        }
        println("[flight] регистрация $name (инвайт $invite) — для чистки БД: mkinvite.cjs cleanup")
    }

    private companion object {
        const val DEFAULT_URL = "https://azrael-lab.xyz/api/app/v1"

        /** 1×1 прозрачный PNG в base64 — минимальный валидный аватар. */
        const val PNG_1PX =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg=="
    }
}
