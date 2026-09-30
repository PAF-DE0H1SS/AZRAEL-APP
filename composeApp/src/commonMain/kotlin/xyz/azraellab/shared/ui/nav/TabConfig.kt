package xyz.azraellab.shared.ui.nav

import androidx.compose.runtime.Immutable
import xyz.azraellab.shared.ui.components.AzraelNavItem

/** Раздел из `tabConfig` в сыром виде — ровно то, что прислал сервер. */
@Immutable
data class ServerTab(
    val id: String,
    val serverLabel: String,
    val visible: Boolean
)

/**
 * Разделы, которые сервер разрешил этой роли.
 *
 * Раньше вкладки были `List<Pair<String, String>>`, а экран получал `tab: String` и
 * делал `when (tab)`. Теперь id разбирается один раз здесь: неизвестное значение не
 * доходит до `when` и не может уронить UI, а остаётся как `Placeholder` с его
 * настоящим названием — видно, что расходится с сервером, а не что «экран пуст».
 */
@Immutable
data class TabConfig(
    val items: List<AzraelNavItem>,
    /** `defaultTab` из `tabConfig` либо первый доступный раздел. */
    val rootId: String
) {
    fun contains(id: String): Boolean = items.any { it.id == id }

    /** Раздел по id; `null` — id не из `tabConfig` (например, сняли админку на сервере). */
    fun find(id: String): AzraelNavItem? = items.firstOrNull { it.id == id }

    /** Корень стека: настоящий [TabSpec], если id известен, иначе заглушка. */
    val root: Destination
        get() = TabSpec.fromTabId(rootId)?.let { Destination.Tab(it) } ?: Destination.Placeholder(rootId)

    /**
     * Переход к разделу по id. Права проверяются **первым делом**: сервер мог прислать
     * `admin` в `defaultTab` или в deep link, хотя роль админку не видит. Раньше
     * `destinationOf` сначала спрашивал [TabSpec] и возвращал `Destination.Tab` для
     * любого известного id — то есть `openTab` открывал админку всем, кто знал её
     * название (проверка `contains` в тестах это и поймала).
     */
    fun destinationOf(id: String): Destination? {
        if (!contains(id)) return null
        return when (val spec = TabSpec.fromTabId(id)) {
            null -> Destination.Placeholder(id)
            else -> Destination.Tab(spec)
        }
    }
}

/**
 * Сборка `TabConfig` из серверного `tabConfig`.
 *
 * Правила:
 *  - `visible: false` раздел не показываем (роль его не видит);
 *  - `defaultTab` берём только если он реально разрешён, иначе первый доступный;
 *  - у неизвестного id остаётся иконка настроек, но его **настоящее** имя с сервера.
 */
fun buildTabConfig(tabs: List<ServerTab>, defaultTab: String?): TabConfig {
    val items = tabs.filter { it.visible }.map { tab ->
        val spec = TabSpec.fromTabId(tab.id)
        AzraelNavItem(
            id = tab.id,
            label = tab.serverLabel.ifEmpty { tab.id },
            icon = spec?.icon ?: TabSpec.Settings.icon,
            badge = 0
        )
    }
    val rootId = defaultTab?.takeIf { candidate -> items.any { it.id == candidate } }
        ?: items.firstOrNull()?.id
        ?: TabSpec.Settings.tabId
    return TabConfig(items = items, rootId = rootId)
}

/**
 * Что подсветить в панели/рельсе. Для подэкрана подсвечиваем родительский раздел,
 * иначе панель выглядит «без выбора» и пользователь теряет ориентацию.
 *
 * Обычная функция, а не `@Composable`: результат кэшируется в [TabConfig] полями, а
 * вызовы `remember` внутри `when` по пункту раньше ломали правило composable-вызовов
 * в условии.
 */
fun selectedTabId(state: NavState, config: TabConfig): String? {
    val current = when (val c = state.current) {
        is Destination.Tab -> c.spec.tabId
        is Destination.Placeholder -> c.tabId
        is Destination.Detail -> c.parentTab().tabId
    }
    // `current` не null: все три ветви `when` дают `tabId`. Пункт, которого больше
    // нет в `tabConfig`, сюда попасть не может — его закрывает `Navigator.rebase`.
    return current.takeIf { config.contains(it) }
        ?: config.rootId.takeIf { config.contains(it) }
        ?: config.items.firstOrNull()?.id
}
