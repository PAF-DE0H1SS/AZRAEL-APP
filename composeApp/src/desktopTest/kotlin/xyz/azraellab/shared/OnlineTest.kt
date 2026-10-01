package xyz.azraellab.shared

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Правило «есть ли сеть» для desktop-индикатора. Перечисление интерфейсов
 * проверять нечем (зависит от машины), а вот решение - тем более нельзя оставлять
 * непроверенным: ошибка в любом из трёх признаков молча гасит или, наоборот,
 * всегда показывает баннер «нет подключения».
 */
class OnlineTest {

    private fun eth(up: Boolean = true, addr: Boolean = true) =
        NetIf(up = up, loopback = false, hasAddress = addr)

    @Test
    fun обычныйИнтерфейсСчитаетсяСетью() {
        assertTrue(netAvailable(listOf(eth())))
    }

    @Test
    fun пустойСписокИнтерфейсовЭтоОфлайн() {
        assertFalse(netAvailable(emptyList()))
    }

    @Test
    fun толькоLoopbackЭтоОфлайн() {
        // «Провод назад в себя» сетью не является: без него баннер сработал бы
        // на любой машине, запущенной без сети, и врёл бы в обе стороны.
        assertFalse(netAvailable(listOf(NetIf(up = true, loopback = true, hasAddress = true))))
    }

    @Test
    fun упавшийИнтерфейсНеСчитается() {
        assertFalse(netAvailable(listOf(eth(up = false))))
    }

    @Test
    fun ИнтерфейсБезАдресаНеСчитается() {
        // Интерфейс поднят, но адреса нет (например, `ip link add … type dummy`
        // без `ip addr`) - трафика через него не будет.
        assertFalse(netAvailable(listOf(eth(addr = false))))
    }

    @Test
    fun ОдинЖивойИнтерфейсСредиМёртвыхДостаточен() {
        assertTrue(
            netAvailable(
                listOf(
                    NetIf(up = true, loopback = true, hasAddress = true),
                    eth(up = false),
                    eth(addr = false),
                    eth()
                )
            )
        )
    }

    @Test
    fun петляПлюсОбычныйСчитаетсяОбычным() {
        assertTrue(
            netAvailable(
                listOf(NetIf(up = true, loopback = true, hasAddress = true), eth())
            )
        )
    }
}
