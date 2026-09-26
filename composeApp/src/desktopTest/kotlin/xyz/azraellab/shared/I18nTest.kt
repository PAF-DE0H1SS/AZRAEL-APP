package xyz.azraellab.shared

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Регрессия i18n: полнота словарей ru/en/zh, отсутствие непереведённого текста в
 * EN/ZH, совпадение плейсхолдеров и работа выбора языка с сохранением на диск.
 * Тест не трогает секреты: файл языка восстанавливается после прогона.
 */
class I18nTest {

    private fun map(name: String): Map<String, String> {
        val facade = Class.forName("xyz.azraellab.shared.I18nKt")
        val field = facade.declaredFields.first { it.name == name }
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return field.get(null) as Map<String, String>
    }

    private fun langMap(lang: String): Map<String, String> =
        map(lang) + map("${lang}_MESSAGES") + map("${lang}_TOOLS")

    private val placeholders = Regex("""\{\d+}""")

    @Test
    fun `словари ru en zh содержат одинаковые ключи`() {
        val ru = langMap("RU")
        val en = langMap("EN")
        val zh = langMap("ZH")
        assertEquals(ru.keys, en.keys, "EN: ключи отличаются от RU")
        assertEquals(ru.keys, zh.keys, "ZH: ключи отличаются от RU")
        assertTrue(ru.size >= 300, "ожидалось ≥300 ключей, получено ${ru.size}")
    }

    @Test
    fun `в en и zh нет непереведённых русских строк`() {
        val cyrillic = Regex("[А-Яа-яЁё]")
        for (code in listOf("EN", "ZH")) {
            val left = langMap(code).filter { (_, v) -> cyrillic.containsMatchIn(v) }
            assertTrue(left.isEmpty(), "$code: остались русские строки: ${left.keys.take(5)}")
        }
    }

    @Test
    fun `плейсхолдеры совпадают во всех языках`() {
        val ru = langMap("RU")
        for (code in listOf("EN", "ZH")) {
            val other = langMap(code)
            val bad = ru.keys.filter { key ->
                placeholders.findAll(ru.getValue(key)).map { it.value }.toSet() !=
                    placeholders.findAll(other.getValue(key)).map { it.value }.toSet()
            }
            assertTrue(bad.isEmpty(), "$code: расхождение плейсхолдеров: ${bad.take(5)}")
        }
    }

    @Test
    fun `AppLang разбирает код и игнорирует неизвестный`() {
        assertEquals(AppLang.EN, AppLang.of("en"))
        assertEquals(AppLang.EN, AppLang.of(" EN "))
        assertEquals(AppLang.RU, AppLang.of("RU"))
        assertNull(AppLang.of("fr"))
        assertNull(AppLang.of(""))
        assertNull(AppLang.of(null))
    }

    @Test
    fun `выбор языка меняет строки и сохраняется на диск`() {
        val store = File(System.getProperty("user.home"), ".config/azraellab/lang")
        val backup = if (store.isFile) store.readText() else null
        try {
            I18n.set(AppLang.EN)
            assertEquals(AppLang.EN, I18n.lang)
            assertEquals("en", AppLangStore.read())
            assertEquals("Language saved: {0}", t["settings.langSaved"])
            assertEquals("Language saved: English", t("settings.langSaved", "English"))

            I18n.set(AppLang.ZH)
            assertEquals("zh", AppLangStore.read())
            assertEquals("语言已保存：{0}", t["settings.langSaved"])

            // серверный null/мусор не сбрасывают локальный выбор
            I18n.applyServer(null)
            I18n.applyServer("fr")
            assertEquals(AppLang.ZH, I18n.lang)
            assertEquals("zh", AppLangStore.read())

            // серверный язык важнее и сохраняется
            I18n.applyServer("en")
            assertEquals(AppLang.EN, I18n.lang)
            assertEquals("en", AppLangStore.read())

            // перезапуск приложения читает сохранённый язык
            I18n.applyServer("ru")
            I18n.set(AppLang.EN)
            I18n.applyServer("ru")
            assertEquals(AppLang.RU, I18n.lang)
            I18n.set(AppLang.EN)
        } finally {
            I18n.set(AppLang.RU)
            if (backup == null) store.delete() else store.writeText(backup)
        }
    }

    @Test
    fun `неизвестный ключ не ломает интерфейс`() {
        I18n.set(AppLang.EN)
        assertEquals("__no_such_key__", t["__no_such_key__"])
    }
}
