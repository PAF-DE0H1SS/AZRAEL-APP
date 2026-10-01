package xyz.azraellab.shared.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember

/**
 * Навигатор поверх [NavState].
 *
 * Раньше «назад» работал только как «выйти из аккаунта»: `selectedId` был обычным
 * `remember`, и ни стека, ни восстановления после поворота экрана, ни deep link не
 * было. Теперь стек живёт в [NavState] (он переживает пересоздание Activity через
 * [rememberNavState]), а этот класс - тонкий слой «что нажали» поверх него.
 */
class Navigator(private val state: NavState) {

    val destination: Destination get() = state.current

    val canGoBack: Boolean get() = state.canGoBack

    val depth: Int get() = state.depth

    /**
     * Переход в раздел. `false` - раздел недоступен этой роли или мы уже в нём:
     * вызывающий ничего не перерисовывает, и мы не теряем позицию скролла.
     */
    fun openTab(config: TabConfig, id: String): Boolean =
        config.destinationOf(id)?.let { state.navigate(it) } ?: false

    /**
     * Переход в подэкран (комната чата, экран настройки).
     *
     * [config] обязателен: подэкран наследует права своего раздела, и раньше
     * `open(DetailKind.Setting)` пускал обычного пользователя в админский экран
     * настроек, просто потому, что вызов шёл из кнопки, а не из `tabConfig`.
     */
    fun open(config: TabConfig, detail: DetailKind, arg: String? = null): Boolean {
        if (detail.argRequired && arg.isNullOrEmpty()) return false
        if (config.destinationOf(Destination.Detail(detail, arg).parentTab().tabId) == null) return false
        return state.navigate(Destination.Detail(detail, arg))
    }

    /**
     * Переход по внешней ссылке: `azrael://messages/42` откроет комнату чата 42.
     *
     * Ссылка приходит извне - из чужого приложения, из браузера, из QR - поэтому
     * права проверяются здесь, а не доверием к разобранному `Destination`.
     * Проверяется **родительский** раздел: `messages/42` требует `messages`,
     * `settings/devices` - `settings`. Раньше метод не брал [TabConfig] вовсе и
     * `openDeepLink("admin")` открывал админку пользователю без прав.
     *
     * @return `false`, если ссылка мусорная, ведёт в неизвестный раздел или в
     *   раздел, которого нет в `tabConfig` этой роли; вызывающий остаётся на месте.
     */
    fun openDeepLink(config: TabConfig, raw: String): Boolean {
        val target = parseDestination(raw) ?: return false
        // `Placeholder` - это раздел, которого в приложении нет. Для вкладки из
        // `tabConfig` заглушка полезна (видно расхождение с сервером), но во внешней
        // ссылке это просто опечатка или попытка открыть то, чего нет: переходить
        // некуда, поэтому отказываем, а не показываем пустую заглушку.
        if (target is Destination.Placeholder) return false
        if (config.destinationOf(target.parentTab().tabId) == null) return false
        return state.navigate(target)
    }

    /**
     * Смена `tabConfig` (refresh boot, смена роли). Корневые пункты пересобираются,
     * открытый подэкран сохраняется: иначе refresh выкидывал бы из комнаты чата.
     */
    fun rebase(config: TabConfig) {
        state.rebaseRoot(config.root)
        // Если текущий корень закрыли на сервере - возвращаемся на новый корень,
        // иначе пользователь останется на экране, до которого больше нельзя добраться.
        val current = state.current
        if (current !is Destination.Detail && config.destinationOf(tabIdOf(current)) == null) {
            state.backTo(config.root)
        }
    }

    fun back(): Boolean = state.back()

    private fun tabIdOf(destination: Destination): String = when (destination) {
        is Destination.Tab -> destination.spec.tabId
        is Destination.Placeholder -> destination.tabId
        is Destination.Detail -> destination.parentTab().tabId
    }
}

@Composable
fun rememberNavigator(root: Destination): Navigator {
    val state = rememberNavState(root)
    return remember(state) { Navigator(state) }
}
