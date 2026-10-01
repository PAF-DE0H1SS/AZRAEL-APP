package xyz.azraellab.shared.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VpnLock
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Разделы приложения. В отличие от строковых id из `tabConfig` это закрытый набор:
 * `when` по нему даёт компиляторскую проверку exhaustiveness, а неизвестный id с
 * сервера превращается в [Destination.Placeholder], а не в «пустой» экран.
 */
enum class TabSpec(
    /** id, который приходит в `tabConfig` от сервера (их исторически два: `vpn_tab` и `vpn`). */
    val tabIds: List<String>,
    val labelKey: String,
    val icon: ImageVector
) {
    Messages(listOf("messages"), "tab.messages", Icons.AutoMirrored.Filled.Chat),
    Admin(listOf("admin"), "tab.admin", Icons.Filled.AdminPanelSettings),
    Shortener(listOf("shortener"), "tab.shortener", Icons.Filled.Link),
    Vpn(listOf("vpn_tab", "vpn"), "tab.vpn", Icons.Filled.VpnLock),
    Settings(listOf("settings"), "tab.settings", Icons.Filled.Settings);

    val tabId: String get() = tabIds.first()

    companion object {
        /**
         * Индекс по **всем** [tabIds], а не только по каноническому [tabId]:
         * `vpn_tab` - это то, что шлёт текущий сервер, а `vpn` приходит от старых
         * установок и от ручных ссылок. Раньше алиас знал [labelKey]-разбор, но не
         * знал навигация, и `azrael://vpn` уходил в [Destination.Placeholder] вместо
         * раздела VPN.
         */
        private val byId: Map<String, TabSpec> = entries.flatMap { spec ->
            spec.tabIds.map { it to spec }
        }.toMap()

        /** `null`, если сервер прислал id, которого нет в приложении. */
        fun fromTabId(id: String): TabSpec? = byId[id]

        /** Канонический id раздела для любого известного алиаса. */
        fun canonicalTabId(id: String): String? = fromTabId(id)?.tabId
    }
}

/**
 * Куда именно перешли. Сейчас это «раздел + раздел», но в P4 сюда же переедет
 * комната чата и админские подэкраны: точка навигации не должна меняться,
 * когда содержимое переезжает из `App.kt` в `screens/`.
 */
sealed interface Destination {
    /** Вкладка верхнего уровня. */
    data class Tab(val spec: TabSpec) : Destination

    /** Экран без вкладки (в P4: комната чата, инвайты, устройства). */
    data class Detail(val kind: DetailKind, val arg: String? = null) : Destination

    /**
     * Раздел с сервера, которого нет в [TabSpec]. Показывается заглушкой с
     * настоящим id - так видно расхождение с сервером, вместо тихого пустого экрана.
     */
    data class Placeholder(val tabId: String) : Destination

    /**
     * Вкладка, в которую надо вернуться по «назад» из этого пункта.
     *
     * Раньше здесь стоял безусловный `TabSpec.Messages` для любого [Detail], и
     * `azrael://settings/devices` уводил в «Сообщения» - по кнопке «назад» из
     * экрана настройки пользователь попадал не туда. Родитель теперь выводится из
     * [DetailKind], а не из типа контейнера.
     */
    fun parentTab(): TabSpec = when (this) {
        is Tab -> spec
        is Detail -> when (kind) {
            DetailKind.ChatRoom, DetailKind.AiChat -> TabSpec.Messages
            DetailKind.Setting -> TabSpec.Settings
        }
        is Placeholder -> TabSpec.Settings
    }

    /**
     * Ключ для сохранения позиции скролла: устойчив к смене порядка и набора вкладок,
     * поэтому refresh `tabConfig` не сбрасывает список.
     */
    fun scrollKey(): String = when (this) {
        is Tab -> "tab:${spec.tabId}"
        is Detail -> "detail:${kind.name}:${arg.orEmpty()}"
        is Placeholder -> "unknown:$tabId"
    }
}

/** Подэкраны, которые появятся в P2-P4. Идентификаторы стабильны: на них ссылается back-stack. */
enum class DetailKind(val argRequired: Boolean) {
    /** Открытый диалог в разделе «Сообщения»; [Destination.arg] - id чата. */
    ChatRoom(true),

    /** Внутренний раздел «Сообщений» с AI-чатом. */
    AiChat(false),

    /** Экран настроек; [Destination.arg] - id настройки для deep link. */
    Setting(false)
}

/**
 * Разбор строки вида `tabId` или `tabId/arg`, как её отдаёт deep link.
 * Мусор на входе даёт `null`, а не исключение: ссылка может прийти извне.
 */
fun parseDestination(raw: String): Destination? {
    val trimmed = raw.trim().trim('/')
    if (trimmed.isEmpty()) return null
    val tabId = trimmed.substringBefore('/')
    val arg = trimmed.substringAfter('/', "").ifEmpty { null }
    if (arg != null && arg.any { it == '/' }) return null
    val spec = TabSpec.fromTabId(tabId) ?: return Destination.Placeholder(tabId)
    // Аргумент несёт смысл только у подэкранов: у «Сообщений» это id чата, у
    // «Настроек» - id конкретной настройки. Раздел без аргумента - это сам
    // раздел, поэтому `settings` открывает список настроек, а `settings/devices` -
    // сразу нужный экран. Права проверяет `openDeepLink`, они одинаковы в обоих
    // случаях (`parentTab()` у обоих - Settings).
    val detail = when {
        spec == TabSpec.Messages && arg != null -> Destination.Detail(DetailKind.ChatRoom, arg)
        spec == TabSpec.Settings && arg != null -> Destination.Detail(DetailKind.Setting, arg)
        else -> Destination.Tab(spec)
    }
    return detail
}

// `parentTab()` и `scrollKey()` - члены `Destination`, а не top-level расширения:
// пунктов назначения мало и они всегда нужны вместе с самим пунктом, а отдельные
// функции легко забыть импортировать и получить «Unresolved reference» в `when`.
