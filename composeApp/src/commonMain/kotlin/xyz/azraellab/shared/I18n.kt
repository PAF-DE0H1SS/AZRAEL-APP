package xyz.azraellab.shared

import androidx.compose.runtime.mutableStateOf

/**
 * Язык интерфейса. Тот же набор, что и на сайте (SITE lib/i18n.ts), плюс он же
 * хранится в users.lang, поэтому выбор синхронизируется между сайтом и приложением.
 */
enum class AppLang(val code: String, val title: String) {
    RU("ru", "Русский"),
    EN("en", "English"),
    ZH("zh", "中文");

    companion object {
        fun of(code: String?): AppLang? =
            entries.firstOrNull { it.code == code?.trim()?.lowercase() }
    }
}

/**
 * Текущий язык приложения. Хранится в snapshot-state, поэтому чтение [t] внутри
 * composable-функции подписывает её на смену языка — переключение мгновенно
 * перерисовывает весь UI без рестарта.
 */
object I18n {
    private val state = mutableStateOf(AppLang.RU)

    val lang: AppLang get() = state.value

    /** Язык, сохранённый на устройстве (виден до входа — входной экран тоже переведён). */
    fun load() {
        AppLang.of(AppLangStore.read())?.let { if (state.value != it) state.value = it }
    }

    /** Выбор пользователя: применяем и сохраняем локально (сервер шлёт его отдельно). */
    fun set(next: AppLang) {
        if (state.value == next) return
        state.value = next
        AppLangStore.write(next.code)
    }

    /**
     * Язык с сервера (home.boot.lang / profile.lang). Неизвестное значение
     * игнорируем — тогда остаётся локальный выбор, а не сброс на ru.
     */
    fun applyServer(code: String?) {
        val next = AppLang.of(code) ?: return
        if (state.value == next) return
        state.value = next
        AppLangStore.write(next.code)
    }
}

/**
 * Строки UI. Хранятся картами: ключи не компилируются, зато невозможно «сломать»
 * перевод добавлением строки в App.kt — ключ просто отсутствует в en/zh и берётся
 * русский оригинал. Подстановки: {0}, {1}, …
 */
class Strings internal constructor(private val m: Map<String, String>) {
    operator fun get(key: String): String = m[key] ?: RU[key] ?: key

    operator fun invoke(key: String, vararg args: Any?): String {
        var out = m[key] ?: RU[key] ?: key
        for (i in args.indices) out = out.replace("{$i}", args[i]?.toString() ?: "")
        return out
    }
}

/** Актуальные строки текущего языка. Читать в composable — подписка на смену языка. */
val t: Strings get() = I18N[I18n.lang] ?: I18N[AppLang.RU]!!


// ---- Словари разнесены по файлам доменов ----
// I18nApp.kt — общие/вход/профиль/устройства, I18nChats.kt — чаты и AI-чат,
// I18nTools.kt — сократитель/VPN/админка/OTP/приглашения.
// Карты читаются из I18nTest рефлексией по facade I18nKt и полям I18N-фасада,
// поэтому здесь остаются приватные прокси-декларации с прежними именами.
private val RU: Map<String, String> = APP_RU
private val EN: Map<String, String> = APP_EN
private val ZH: Map<String, String> = APP_ZH
private val RU_MESSAGES: Map<String, String> = CHATS_RU
private val EN_MESSAGES: Map<String, String> = CHATS_EN
private val ZH_MESSAGES: Map<String, String> = CHATS_ZH
private val RU_TOOLS: Map<String, String> = TOOLS_RU
private val EN_TOOLS: Map<String, String> = TOOLS_EN
private val ZH_TOOLS: Map<String, String> = TOOLS_ZH

private val I18N: Map<AppLang, Strings> = mapOf(
    AppLang.RU to Strings(RU + RU_MESSAGES + RU_TOOLS),
    AppLang.EN to Strings(EN + EN_MESSAGES + EN_TOOLS),
    AppLang.ZH to Strings(ZH + ZH_MESSAGES + ZH_TOOLS)
)
