package xyz.azraellab.shared.data.vm

import kotlinx.coroutines.runBlocking
import xyz.azraellab.shared.data.UiState
import xyz.azraellab.shared.data.model.ChatDto
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Регрессия: заголовок комнаты был пустым, если диалог открыт не из списка.
 *
 * `chats.open` имя не возвращает, оно берётся из списка. Deep link
 * `azrael://messages/<id>` открывает комнату раньше, чем список приходит (на
 * узком экране список вообще не композится), и `knownName` отдавал "".
 */
class ChatRoomNameTest {

    private fun chat(id: Long, name: String) = ChatDto(id = id, partnerId = 7L, name = name)

    private fun ready(vararg chats: ChatDto) = UiState.Ready(chats.toList())

    @Test
    fun nameComesFromLoadedList() = runBlocking {
        var loads = 0
        val name = chatRoomName(2L, ready(chat(1L, "A"), chat(2L, "B")), UiState.Ready(emptyList())) {
            loads++
            ready(chat(2L, "B"))
        }
        assertEquals("B", name)
        assertEquals(0, loads, "список уже загружен — лишнего запроса быть не должно")
    }

    @Test
    fun deepLinkBeforeListArrivesLoadsItOnce() = runBlocking {
        var loads = 0
        val name = chatRoomName(2L, UiState.Loading, UiState.Ready(emptyList())) {
            loads++
            ready(chat(2L, "B"))
        }
        assertEquals("B", name, "deep link обязан догрузить список и найти имя")
        assertEquals(1, loads)
    }

    @Test
    fun archivedNameUsedWithoutExtraRequest() = runBlocking {
        var loads = 0
        val name = chatRoomName(9L, ready(chat(1L, "A")), ready(chat(9L, "Архив"))) {
            loads++
            ready()
        }
        assertEquals("Архив", name)
        assertEquals(0, loads, "имя нашлось в архиве — сеть не трогаем")
    }

    @Test
    fun readyListWithoutChatDoesNotRefetch() = runBlocking {
        var loads = 0
        val name = chatRoomName(9L, ready(chat(1L, "A")), ready()) {
            loads++
            ready(chat(1L, "A"))
        }
        assertEquals("", name)
        assertEquals(0, loads, "список готов, чата в нём нет — повторный запрос бессмыслен")
    }

    @Test
    fun failedListIsRetriedOnce() = runBlocking {
        var loads = 0
        val name = chatRoomName(2L, UiState.Error("boom"), UiState.Ready(emptyList())) {
            loads++
            ready(chat(2L, "B"))
        }
        assertEquals("B", name, "после ошибки списка deep link всё равно должен показать имя")
        assertEquals(1, loads)
    }

    @Test
    fun blankNameInReadyListIsNotAcceptedAsFound() = runBlocking {
        var loads = 0
        val name = chatRoomName(2L, UiState.Ready(listOf(chat(2L, "   "))), UiState.Ready(emptyList())) {
            loads++
            ready(chat(2L, "B"))
        }
        assertEquals("", name, "пробельное имя из готового списка — не имя")
        assertEquals(0, loads, "список готов — повторный запрос не нужен, UI покажет заголовок по умолчанию")
    }

    @Test
    fun blankNameBeforeListArrivesIsRefetched() = runBlocking {
        var loads = 0
        val name = chatRoomName(2L, UiState.Loading, UiState.Ready(emptyList())) {
            loads++
            ready(chat(2L, "B"))
        }
        assertEquals("B", name, "пробельное имя не должно маскировать недогруженный список")
        assertEquals(1, loads)
    }

    @Test
    fun chatRemovedEverywhereYieldsEmptyName() = runBlocking {
        val name = chatRoomName(5L, UiState.Loading, UiState.Ready(emptyList())) { ready() }
        assertEquals("", name, "нет данных — пустая строка, UI рисует нейтральный заголовок")
    }
}
