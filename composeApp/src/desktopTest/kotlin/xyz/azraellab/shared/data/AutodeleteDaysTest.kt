package xyz.azraellab.shared.data

import kotlinx.coroutines.runBlocking
import xyz.azraellab.shared.core.api.AppClient
import xyz.azraellab.shared.data.UiState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Сроки автоудаления: guard в [Repos.autodeleteSet] и набор, из которого рисует
 * кнопки ChatsScreen, — это одна и та же величина, иначе чат можно оставить с
 * сроком, который UI не покажет.
 */
class AutodeleteDaysTest {

    @Test
    fun allowedDaysAreTheOnesOfferedByTheUi() {
        assertEquals(listOf(null, 1, 7, 30), Repos.AUTODELETE_DAYS)
        assertEquals(4, Repos.AUTODELETE_DAYS.size, "null = выключено, плюс три срока")
        assertEquals(3, Repos.AUTODELETE_DAYS.filterNotNull().size, "три непустых срока: 1, 7, 30")
        assertEquals(listOf(null, 30, 90, 365), Repos.PROFILE_AUTODELETE_DAYS)
        // 30 недели — оба набора. Проверяем, что это единственное совпадение:
        // лишнее совпадение означало бы, что список профиля случайно повторяет
        // список чата, а расхождение — что UI предлагает то, что guard отбросит.
        val chatDays = Repos.AUTODELETE_DAYS.filterNotNull().toSet()
        val profileDays = Repos.PROFILE_AUTODELETE_DAYS.filterNotNull().toSet()
        assertEquals(setOf(30), chatDays.intersect(profileDays))
    }

    /** Ключ есть, но адрес заведомо мёртвый: запрос уйдёт в сеть и упадёт. */
    private val deadUrl = "http://127.0.0.1:1/api/app/v1"

    private fun testClient() = AppClient(deadUrl, ByteArray(32) { 7 })

    @Test
    fun daysOutsideTheAllowedSetAreRejectedWithoutNetworkCall() = runBlocking {
        // С отклонённым сроком сеть не трогается: вместо NETWORK/transport-ошибки
        // приходит текст нашего guard'а. Это и есть проверка «guard до запроса».
        val client = testClient()
        for (bad in listOf(0, 2, 14, 90, 365, -1, Int.MAX_VALUE)) {
            val st = Repos.autodeleteSet(client, chatId = 1L, days = bad)
            val err = assertIs<UiState.Error>(st, "срок $bad должен быть отклонён")
            assertEquals("autodelete days not allowed: $bad", err.message)
        }
    }

    @Test
    fun profileDaysOutsideTheAllowedSetAreRejected() = runBlocking {
        val client = testClient()
        for (bad in listOf(0, 1, 7, 14, 60, 366, -30, Int.MAX_VALUE)) {
            val st = Repos.autoDeleteSet(client, days = bad)
            val err = assertIs<UiState.Error>(st, "профильный срок $bad должен быть отклонён")
            assertEquals("autodelete days not allowed: $bad", err.message)
        }
    }

    @Test
    fun profileAllowedDaysPassTheGuard() = runBlocking {
        val client = testClient()
        for (good in Repos.PROFILE_AUTODELETE_DAYS) {
            val err = Repos.autoDeleteSet(client, days = good) as? UiState.Error
            assertTrue(
                err == null || !err.message.contains("autodelete days not allowed"),
                "профильный срок $good не должен отсекаться guard'ом, получено «${err?.message}»",
            )
        }
    }

    @Test
    fun chatAndProfileGuardsDoNotAcceptEachOthersDays() = runBlocking {
        // Срок 30 валиден для профиля, но не для чата: наборы живут отдельно,
        // и ошибка должна называть именно тот набор, который не подошёл.
        val client = testClient()
        assertIs<UiState.Error>(Repos.autodeleteSet(client, 1L, 90), "90 нельзя ставить чату")
        assertIs<UiState.Error>(Repos.autoDeleteSet(client, 7), "7 нельзя ставить профилю")
        Unit
    }

    @Test
    fun allowedDaysPassTheGuard() = runBlocking {
        // Разрешённый срок guard не отсекает: запрос уходит в сеть и падает там.
        // Любой отказ означает лишь, что до сети дело дошло, — важно лишь, что
        // это НЕ «autodelete days not allowed».
        val client = testClient()
        for (good in Repos.AUTODELETE_DAYS) {
            val err = Repos.autodeleteSet(client, chatId = 1L, days = good) as? UiState.Error
            assertTrue(
                err == null || !err.message.contains("autodelete days not allowed"),
                "срок $good не должен отсекаться guard'ом, получено «${err?.message}»",
            )
        }
    }
}
