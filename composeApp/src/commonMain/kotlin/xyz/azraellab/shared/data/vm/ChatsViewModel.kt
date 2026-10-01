package xyz.azraellab.shared.data.vm

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.JsonObject
import xyz.azraellab.shared.core.api.AppClient
import xyz.azraellab.shared.data.Repos
import xyz.azraellab.shared.data.UiState
import xyz.azraellab.shared.data.model.AiAnswerDto
import xyz.azraellab.shared.data.model.AiChatDto
import xyz.azraellab.shared.data.model.ChatDto
import xyz.azraellab.shared.data.model.RoomDto

/**
 * Раздел «Сообщения»: список диалогов, архив, комната и AI-чат.
 * Сетевые списки живут в StateFlow, однократные действия — suspend-функции.
 */
class ChatsViewModel(
    private val client: AppClient,
    private val myId: Long,
    externalScope: CoroutineScope? = null
) : AppViewModel(externalScope) {

    private val _chats = MutableStateFlow<UiState<List<ChatDto>>>(UiState.Loading)
    val chats: StateFlow<UiState<List<ChatDto>>> = _chats.asStateFlow()

    private val _archived = MutableStateFlow<UiState<List<ChatDto>>>(UiState.Ready(emptyList()))
    val archived: StateFlow<UiState<List<ChatDto>>> = _archived.asStateFlow()

    private val _online = MutableStateFlow<UiState<Map<Long, Boolean>>>(UiState.Ready(emptyMap()))
    val online: StateFlow<UiState<Map<Long, Boolean>>> = _online.asStateFlow()

    private val _room = MutableStateFlow<UiState<RoomDto>>(UiState.Ready(RoomDto()))
    val room: StateFlow<UiState<RoomDto>> = _room.asStateFlow()

    private val _aiChats = MutableStateFlow<UiState<List<AiChatDto>>>(UiState.Loading)
    val aiChats: StateFlow<UiState<List<AiChatDto>>> = _aiChats.asStateFlow()

    fun refresh() {
        loadInto(_chats) { Repos.chats(client) }
        loadInto(_online) { Repos.chatsPresence(client, currentIds()) }
    }

    /** Загрузка (активных или архивных) диалогов с возвратом результата для сообщений. */
    suspend fun loadArchived(): UiState<List<ChatDto>> {
        _archived.value = UiState.Loading
        val st = Repos.archivedChats(client)
        _archived.value = st
        return st
    }

    /** Восстановить диалог из архива, затем обновить список и архив. */
    suspend fun restore(id: Long): UiState<Unit> {
        val st = Repos.restoreChat(client, id)
        if (st is UiState.Ready) {
            refresh()
            loadArchived()
        }
        return st
    }

    suspend fun archiveChat(id: Long): UiState<Unit> = Repos.archiveChat(client, id)

    suspend fun deleteChat(id: Long): UiState<Unit> = Repos.deleteChat(client, id)

    suspend fun search(q: String): UiState<List<JsonObject>> = Repos.searchUsers(client, q)

    suspend fun createChat(uid: Long): UiState<Long> = Repos.createChat(client, uid)

    suspend fun reloadRoom(chatId: Long): UiState<RoomDto> {
        _room.value = UiState.Loading
        val st = Repos.openChat(client, chatId, myId, resolveName(chatId))
        _room.value = st
        return st
    }

    /**
     * Имя собеседника для заголовка комнаты.
     *
     * `chats.open` имя не возвращает (см. [Repos.openChat]), поэтому оно берётся из
     * списка диалогов. Deep link `azrael://messages/<id>` открывает комнату раньше,
     * чем список приходит: на узком экране список вообще не композится, а на
     * широком гонка `LaunchedEffect(chatId)` с `refresh()` решается случайно. Тогда
     * имя оставалось пустым и заголовок комнаты был пустым.
     *
     * Доп. запрос нужен только в этом случае — в обычном пути список уже загружен.
     */
    private suspend fun resolveName(chatId: Long): String =
        chatRoomName(chatId, _chats.value, _archived.value) {
            Repos.chats(client).also { _chats.value = it }
        }

    /** Перечитать сообщения комнаты, не трогая имя и автоудаление. */
    suspend fun reloadMessages(chatId: Long): UiState<Unit> {
        val st = Repos.messages(client, chatId, myId)
        val old = _room.value.getOrNull()
        return when (st) {
            is UiState.Ready -> {
                if (old != null) _room.value = UiState.Ready(old.copy(msgs = st.data))
                UiState.Ready(Unit)
            }
            is UiState.Loading -> st
            is UiState.Error -> st
        }
    }

    suspend fun sendMessage(chatId: Long, text: String?, fileToken: String?): UiState<Unit> =
        Repos.sendMessage(client, chatId, text, fileToken)

    suspend fun deleteMessage(id: Long): UiState<Unit> = Repos.deleteMessage(client, id)

    suspend fun setAutodelete(chatId: Long, days: Int?): UiState<Int?> {
        val st = Repos.autodeleteSet(client, chatId, days)
        if (st is UiState.Ready) {
            val old = _room.value.getOrNull()
            if (old != null) _room.value = UiState.Ready(old.copy(days = st.data))
        }
        return st
    }

    suspend fun typing(chatId: Long): UiState<Unit> = Repos.typing(client, chatId)

    suspend fun uploadFile(base64: String, mime: String, name: String): UiState<String> =
        Repos.chatFileUpload(client, base64, mime, name)

    suspend fun fileUrl(fileToken: String): UiState<String> = Repos.chatFileUrl(client, fileToken)

    suspend fun askAi(text: String, chatId: Long?): UiState<AiAnswerDto> {
        val st = Repos.aiChatAsk(client, text, chatId)
        if (st is UiState.Ready) refreshAiChats()
        return st
    }

    fun refreshAiChats() = loadInto(_aiChats) { Repos.aiChats(client) }

    private fun currentIds(): List<Long> =
        _chats.value.getOrNull()?.map { it.partnerId }?.filter { it != 0L } ?: emptyList()

    fun updateRoomMessage(id: Long) {
        val old = _room.value.getOrNull() ?: return
        _room.value = UiState.Ready(old.copy(msgs = old.msgs.filterNot { it.id == id }))
    }
}

/**
 * Имя для заголовка комнаты — чистая функция, без сети внутри.
 *
 * `chats.open` на сервере возвращает только `{ chat: { id }, messages }`, ни `items`,
 * ни `partner`, поэтому имя собеседника приходится брать из уже загруженного списка
 * диалогов. Deep link `azrael://messages/<id>` открывает комнату раньше списка:
 * на узком экране список вообще не композится, на широком `LaunchedEffect(chatId)`
 * обгоняет `refresh()`. Тогда имя оставалось пустым.
 *
 * Правило: сначала ищем в активном списке, потом в архиве, и только если имя не
 * найдено И список ещё грузится — один раз догружаем его через [loadChats]. Доп.
 * запрос появляется только на этом пути; в обычном [loadChats] не вызывается.
 * Пустая строка — осознанный ответ «имени нет» (чат удалён или чужой), UI рисует
 * нейтральный заголовок.
 */
internal suspend fun chatRoomName(
    chatId: Long,
    chats: UiState<List<ChatDto>>,
    archived: UiState<List<ChatDto>>,
    loadChats: suspend () -> UiState<List<ChatDto>>
): String {
    fun pick(state: UiState<List<ChatDto>>) =
        state.getOrNull()?.firstOrNull { it.id == chatId }?.name?.takeIf { it.isNotBlank() }
    pick(chats)?.let { return it }
    pick(archived)?.let { return it }
    if (chats !is UiState.Ready) pick(loadChats())?.let { return it }
    return ""
}