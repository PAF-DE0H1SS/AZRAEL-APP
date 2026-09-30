package xyz.azraellab.shared.ui.nav

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList

/**
 * Back-stack приложения. Раньше «назад» работал только как «выйти из аккаунта»,
 * а разделами управлял одиночный `selectedId` — вернуться на предыдущую вкладку
 * было нельзя, и состояние вкладки терялось при перерисовке.
 *
 * Стек хранится строкой через [Saver], поэтому переживает пересоздание Activity
 * (поворот экрана) — раньше `selectedId` был `remember`, то есть терялся.
 */
@Stable
class NavState private constructor(initial: List<Destination>) {

    /**
     * Стек хранится в snapshot-списке, а не в `ArrayDeque` или `MutableList`.
     *
     * Это не оптимизация, а условие работоспособности: `MainShell` читает
     * `state.current` во время композиции, и если данные лежат в обычной коллекции,
     * нажатие на вкладку меняло стек **без** invalidation — экран оставался на старом
     * разделе, пока не случилось другое перерисовывание (смена языка, приход сети).
     * Именно так было с `selectedId` до P1. `SnapshotStateList` даёт и то, и другое:
     * вне композиции (тесты, `rememberSaveable`) работает как обычный список.
     */
    private val entries = SnapshotStateList<Destination>().apply { addAll(initial) }

    val current: Destination get() = entries.last()

    val canGoBack: Boolean get() = entries.size > 1

    val depth: Int get() = entries.size

    val stack: List<Destination> get() = entries.toList()

    /** Возвращает `true`, если переход состоялся. */
    fun navigate(destination: Destination): Boolean {
        if (destination == current) return false
        // Переход в уже открытый раздел не должен плодить дубли в стеке:
        // выбрал вкладку, вернулся на неё — стек снова как был.
        val existing = entries.indexOfLast { it == destination }
        if (existing >= 0) {
            while (entries.size > existing + 1) entries.removeAt(entries.lastIndex)
            return existing == entries.lastIndex
        }
        entries.add(destination)
        return true
    }

    /** Переход в корневой раздел: стек обнуляется, «назад» из корня ничего не делает. */
    fun selectRoot(spec: TabSpec): Boolean = navigate(Destination.Tab(spec))

    fun back(): Boolean {
        if (!canGoBack) return false
        entries.removeAt(entries.lastIndex)
        return true
    }

    fun backTo(destination: Destination): Boolean {
        val index = entries.indexOfLast { it == destination }
        if (index < 0) return false
        while (entries.size > index + 1) entries.removeAt(entries.lastIndex)
        return true
    }

    fun replaceAll(destination: Destination) {
        entries.clear()
        entries.add(destination)
    }

    /** Состояние переживает смену `tabConfig` (refresh boot): корень всегда есть. */
    fun rebaseRoot(destination: Destination) {
        entries.removeAll { it is Destination.Tab || it is Destination.Placeholder }
        if (entries.isEmpty()) {
            entries.add(destination)
        } else {
            // Подэкран открыт — новый корень кладём под ним, иначе refresh `tabConfig`
            // выкидывал бы из комнаты чата.
            entries.add(0, destination)
        }
    }

    fun encode(): String = entries.joinToString("|") { encodeOne(it) }

    companion object {
        fun of(vararg destinations: Destination): NavState {
            require(destinations.isNotEmpty()) { "нужен хотя бы один пункт" }
            return NavState(destinations.toList())
        }

        fun decode(value: String): NavState? {
            if (value.isBlank()) return null
            val parts = value.split("|").mapNotNull { decodeOne(it) }
            return if (parts.isEmpty()) null else NavState(parts)
        }
    }
}

private fun encodeOne(destination: Destination): String = when (destination) {
    is Destination.Tab -> "t:${destination.spec.tabId}"
    is Destination.Detail -> "d:${destination.kind.name}:${destination.arg.orEmpty()}"
    is Destination.Placeholder -> "x:${destination.tabId}"
}

private fun decodeOne(raw: String): Destination? = when {
    raw.startsWith("t:") -> TabSpec.fromTabId(raw.removePrefix("t:"))?.let { Destination.Tab(it) }
    raw.startsWith("x:") -> raw.removePrefix("x:").ifEmpty { null }?.let { Destination.Placeholder(it) }
    raw.startsWith("d:") -> {
        val rest = raw.removePrefix("d:")
        val kind = rest.substringBefore(':').let { name ->
            DetailKind.entries.firstOrNull { it.name == name }
        } ?: return null
        val arg = rest.substringAfter(':', "").ifEmpty { null }
        if (kind.argRequired && arg == null) null else Destination.Detail(kind, arg)
    }
    else -> null
}

/**
 * Состояние навигации, переживающее пересоздание Activity. Saver пишет стек одной
 * строкой — формат версионируется префиксом (`t:`/`d:`/`x:`), иначе старый
 * сохранённый стек после обновления схемы экранов упал бы на `decode`.
 */
@Composable
fun rememberNavState(root: Destination): NavState {
    val saver = Saver<NavState, String>(
        save = { it.encode() },
        restore = { NavState.decode(it) }
    )
    return rememberSaveable(saver = saver) { NavState.of(root) }
}

/**
 * Позиции скролла по ключу пункта — раньше при возврате на вкладку список прыгал наверх.
 *
 * Словарь внутри — `mutableStateOf`, поэтому чтение `[get]` в композиции
 * подписывает экран на изменение, а запись из `LaunchedEffect`/`snapshotFlow`
 * перерисовывает только его.
 */
@Stable
class ScrollPositions(initial: Map<String, Int> = emptyMap()) {
    private val state: MutableState<Map<String, Int>> = mutableStateOf(initial)

    operator fun get(key: String): Int = state.value[key] ?: 0

    operator fun set(key: String, value: Int) {
        if (state.value[key] == value) return
        state.value = state.value + (key to value)
    }

    fun encoded(): String = state.value.entries.joinToString(",") { "${it.key}=${it.value}" }

    companion object {
        fun decode(raw: String): Map<String, Int> = raw.split(",").mapNotNull { part ->
            val key = part.substringBefore('=')
            val value = part.substringAfter('=', "").toIntOrNull()
            if (key.isEmpty() || value == null) null else key to value
        }.toMap()
    }
}

/**
 * Позиции скролла переживают пересоздание Activity.
 *
 * Именно `rememberSaveable`, а не `remember`: при повороте экрана список возвращался
 * наверх, потому что карта значений жила только в памяти composable. Формат —
 * `key=offset,key=offset`; повреждённые элементы отбрасываются поштучно.
 */
@Composable
fun rememberScrollPositions(): ScrollPositions {
    val saver = Saver<ScrollPositions, String>(
        save = { it.encoded() },
        restore = { ScrollPositions(ScrollPositions.decode(it)) }
    )
    return rememberSaveable(saver = saver) { ScrollPositions() }
}

/**
 * Ключ скролла открытого диалога — свой на каждый чат.
 *
 * Раньше комната и список жили в одной `Column` и делили одну позицию, поэтому
 * возвращение в диалог всегда показывало его с начала. Теперь комната приходит
 * как `Destination.Detail(ChatRoom, id)`, и у неё своя история: открыли второй
 * чат — вернулись в первый, он остался там же, где остановились.
 */
fun roomScrollKey(chatId: Long): String = "messages.room.$chatId"

/**
 * Скролл раздела, который помнит позицию при возврате на вкладку.
 *
 * Ключ — [Destination.scrollKey], то есть id раздела, а не позиция в списке:
 * refresh `tabConfig` может вернуть разделы в другом порядке, и по позиции список
 * прыгал бы наверх.
 *
 * Пишем в [ScrollPositions] один раз — при уходе с экрана, а не на каждый кадр
 * прокрутки: держать `snapshotFlow` на пиксели бессмысленно, восстанавливать
 * надо ровно одну величину. [ScrollState] сам переживает поворот экрана через
 * свойSaver, карта — через saver [ScrollPositions].
 *
 * Сам [key] передан в `rememberSaveable` входом, а не только в [DisposableEffect].
 * Иначе один и тот же объект [ScrollState] жил бы на все диалоги: `DisposableEffect`
 * сохранил бы старую позицию под старым ключом, но новый экран получил бы тот же
 * state — и второй чат открылся бы на середине первого.
 */
@Composable
fun rememberSectionScroll(positions: ScrollPositions, key: String): ScrollState {
    val state = rememberSaveable(key, saver = ScrollState.Saver) { ScrollState(positions[key]) }
    DisposableEffect(positions, key) {
        onDispose { positions[key] = state.value }
    }
    return state
}
