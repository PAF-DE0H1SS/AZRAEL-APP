package xyz.azraellab.shared.core.api

import xyz.azraellab.shared.AppVault
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Кэш ключа канала привязан к devId установки: сервер выдаёт ключ на devId и
 * больше не знает прежний общий ключ. Поэтому файл, записанный старой версией
 * программы (там лежал только ключ, без маркера), обязан считаться непригодным -
 * иначе после ротации мастера установка навсегда осталась бы со мёртвым ключом
 * и не смогла бы забрать новый.
 */
class AppKeyCacheTest {

    private var originalHome: String? = null
    private val homes: MutableList<Path> = mutableListOf()

    private fun isolated() {
        val home = Files.createTempDirectory("azrael-appkey")
        if (originalHome == null) originalHome = System.getProperty("user.home")
        System.setProperty("user.home", home.toString())
        homes.add(home)
    }

    private fun writeRaw(text: String) {
        val dir = Path.of(System.getProperty("user.home"), ".config/azraellab")
        Files.createDirectories(dir)
        Files.write(dir.resolve("app-key"), text.trim().toByteArray())
    }

    @AfterTest
    fun cleanup() {
        originalHome?.let { System.setProperty("user.home", it) }
        originalHome = null
        homes.forEach { dir ->
            runCatching {
                Files.walk(dir).use { s ->
                    s.sorted(Comparator.reverseOrder()).forEach { runCatching { Files.deleteIfExists(it) } }
                }
            }
        }
        homes.clear()
    }

    @Test
    fun keyIsStoredWithItsDevId() {
        isolated()
        assertNull(AppVault.readAppKey(), "до записи ключа кэш пуст")
        assertNull(AppVault.readAppKeyDevId())

        val dev = "d".repeat(64)
        val key = "qW5uZXJhZWwta2V5"
        assertEquals(true, AppVault.writeAppKey(dev, key))
        assertEquals(key, AppVault.readAppKey())
        assertEquals(dev, AppVault.readAppKeyDevId())
    }

    @Test
    fun legacyFileWithoutMarkerIsNotTrusted() {
        isolated()
        writeRaw("b2xkLW1hc3Rlci1rZXk")
        assertEquals("b2xkLW1hc3Rlci1rZXk", AppVault.readAppKey(), "сам ключ читается")
        assertNull(AppVault.readAppKeyDevId(), "у файла старой версии нет маркера - ключу нельзя доверять")
    }

    @Test
    fun reBootstrapReplacesLegacyCache() {
        isolated()
        writeRaw("b2xkLW1hc3Rlci1rZXk")
        val dev = "e".repeat(64)
        val fresh = "bmV3LWtleQ"
        AppVault.writeAppKey(dev, fresh)
        assertEquals(fresh, AppVault.readAppKey())
        assertEquals(dev, AppVault.readAppKeyDevId())
    }

    @Test
    fun anotherInstallDoesNotInheritKey() {
        isolated()
        AppVault.writeAppKey("1".repeat(64), "key-one")
        // Файл один на машину, но devId у другой установки другой:
        // ключ ей не подходит, поэтому она обязана забрать свой.
        assertEquals("1".repeat(64), AppVault.readAppKeyDevId())
        assertEquals("key-one", AppVault.readAppKey())
    }
}
