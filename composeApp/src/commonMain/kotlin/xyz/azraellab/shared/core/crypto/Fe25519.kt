package xyz.azraellab.shared.core.crypto

/**
 * Арифметика Ed25519 без `BigInteger`: поле F_p (p = 2^255 - 19) и скаляры по
 * модулю L (порядок базовой точки). Числа — `IntArray` в основании 2^16,
 * младший лимб первым, 16 лимбов = 256 бит.
 *
 * Почему не `java.math.BigInteger`: он живёт только в JVM, а Ed25519 нужен в
 * `commonMain`, чтобы таргеты вроде wasmJs/iOS получили ту же реализацию без
 * отдельного платформенного кода. Основание 2^16 вместо, скажем, 2^25.5 — чтобы
 * переносы и старшие биты не требовали знаковой арифметики: все промежуточные
 * произведения помещаются в Long, а приведение по модулю p сводится к свёртке.
 *
 * Приведение произведения по модулю p — главная хитрость: так как
 * 2^255 ≡ 19 (mod p), 512-битное произведение делится на «низкие 255 бит» и
 * «старшие», а дальше 19 умножается на старшую часть. Параллельно для модуля L
 * (у него нет такого свойства) используется обычное деление столбиком по битам —
 * оно зовётся дважды на подпись, так что на скорость это не влияет.
 */
internal object Fe25519 {

    const val LIMBS = 16
    const val WIDE = 2 * LIMBS

    /** Рабочий буфер: 18 лимбов = 288 бит, хватает и для 2^256+carry, и для 2^262. */
    private const val WORK = LIMBS + 2

    /** p = 2^255 - 19. */
    private val P = intArrayOf(
        0xFFED, 0xFFFF, 0xFFFF, 0xFFFF, 0xFFFF, 0xFFFF, 0xFFFF, 0xFFFF,
        0xFFFF, 0xFFFF, 0xFFFF, 0xFFFF, 0xFFFF, 0xFFFF, 0xFFFF, 0x7FFF
    )

    /** L = 2^252 + 27742317777372353535851937790883648493. */
    private val L = intArrayOf(
        0xD3ED, 0x5CF5, 0x631A, 0x5812, 0x9CD6, 0xA2F7, 0xF9DE, 0x14DE,
        0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x1000
    )

    /** p - 2 — показатель обратного элемента: a^(-1) = a^(p-2) (простое p). */
    private val P_MINUS_2 = intArrayOf(
        0xFFEB, 0xFFFF, 0xFFFF, 0xFFFF, 0xFFFF, 0xFFFF, 0xFFFF, 0xFFFF,
        0xFFFF, 0xFFFF, 0xFFFF, 0xFFFF, 0xFFFF, 0xFFFF, 0xFFFF, 0x7FFF
    )

    // ---------------------------------------------------------------- конструкторы

    fun zero(): IntArray = IntArray(LIMBS)

    fun one(): IntArray = intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)

    fun fromInt(v: Int): IntArray = intArrayOf(v, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)

    /**
     * Little-endian байты → лимбы. `maskTopBit` сбрасывает бит 255: у сжатой
     * точки в нём лежит знак x, а не часть координаты.
     */
    fun fromLe(bytes: ByteArray, maskTopBit: Boolean = false): IntArray {
        val limbs = IntArray(LIMBS)
        for (i in 0 until LIMBS) {
            limbs[i] = (bytes[2 * i].toInt() and 0xFF) or ((bytes[2 * i + 1].toInt() and 0xFF) shl 8)
        }
        if (maskTopBit) limbs[LIMBS - 1] = limbs[LIMBS - 1] and 0x7FFF
        return limbs
    }

    /** 64 байта little-endian → 32 лимба (512 бит): полный дайджест SHA-512. */
    fun fromLeWide(bytes: ByteArray): IntArray {
        val limbs = IntArray(WIDE)
        for (i in 0 until WIDE) {
            limbs[i] = (bytes[2 * i].toInt() and 0xFF) or ((bytes[2 * i + 1].toInt() and 0xFF) shl 8)
        }
        return limbs
    }

    /** Лимбы → little-endian байты: 32 для поля/скаляра Ed25519, 64 для SHA-512. */
    fun toLe(x: IntArray, size: Int): ByteArray {
        val out = ByteArray(size)
        for (i in 0 until size / 2) {
            out[2 * i] = (x[i] and 0xFF).toByte()
            out[2 * i + 1] = (x[i] ushr 8).toByte()
        }
        return out
    }

    // ------------------------------------------------------------------ поле: mod p

    fun add(a: IntArray, b: IntArray): IntArray = addMod(a, b, P)

    fun sub(a: IntArray, b: IntArray): IntArray = subMod(a, b, P)

    fun neg(a: IntArray): IntArray = subMod(zero(), a, P)

    fun mul(a: IntArray, b: IntArray): IntArray = mul(newWorkspace(), a, b)

    /**
     * Умножение с переиспользованием буферов [ws]. Возвращает новый массив,
     * входы [a] и [b] не изменяются — поэтому результат можно свободно держать
     * дальше, переиспользуя [ws] под следующие промежуточные значения.
     */
    fun mul(ws: Workspace, a: IntArray, b: IntArray): IntArray {
        mulWideInto(ws, a, b)
        val out = IntArray(LIMBS)
        reduceInto(ws, out)
        return out
    }

    fun invert(a: IntArray): IntArray = pow(a, P_MINUS_2)

    fun invert(ws: Workspace, a: IntArray): IntArray = pow(ws, a, P_MINUS_2)

    fun isZero(a: IntArray): Boolean {
        for (i in 0 until LIMBS) if (a[i] != 0) return false
        return true
    }

    /** Младший бит — знак координаты x в сжатой точке (RFC 8032 §5.1.2). */
    fun isOdd(a: IntArray): Boolean = (a[0] and 1) != 0

    fun equals(a: IntArray, b: IntArray): Boolean {
        for (i in 0 until LIMBS) if (a[i] != b[i]) return false
        return true
    }

    // ---------------------------------------------------------------- скаляры: mod L

    fun addModL(a: IntArray, b: IntArray): IntArray = addMod(a, b, L)

    fun subModL(a: IntArray, b: IntArray): IntArray = subMod(a, b, L)

    /** 512-битное значение (32 лимба) по модулю L делением столбиком по битам. */
    fun modL(x: IntArray): IntArray {
        val rem = IntArray(LIMBS)
        for (i in WIDE - 1 downTo 0) {
            for (bit in 15 downTo 0) {
                // rem = rem * 2 + бит x. Приводить нужно после КАЖДОГО бита: L лишь
                // чуть больше 2^252, так что 16 сдвигов подряд без приведения унесли
                // бы остаток за 256 бит. Одного условного вычитания достаточно,
                // потому что до сдвига rem < L, а значит rem * 2 + 1 < 2 * L.
                var carry = (x[i] ushr bit) and 1
                for (j in 0 until LIMBS) {
                    val t = (rem[j] shl 1) or carry
                    rem[j] = t and 0xFFFF
                    carry = (t ushr 16) and 1
                }
                check(carry == 0) { "modL: остаток не влез в 256 бит" }
                if (!less(rem, L)) subtractInPlace(rem, L)
            }
        }
        return rem
    }

    // ------------------------------------------------------------------- внутреннее

    /**
     * Буферы горячего пути. Создаётся один раз на операцию верхнего уровня
     * (`Ed25519.publicKeyFromSeed`/`sign`) и переиспользуется, поэтому умножение
     * в поле не аллоцирует ничего, кроме возвращаемого результата.
     *
     * Общего изменяемого состояния здесь намеренно нет: буферы принадлежат
     * объекту, который создаёт вызывающий, поэтому две одновременные подписи из
     * разных корутин не затирают друг другу промежуточные значения. Менять
     * `wide`/`acc` местами можно только внутри [foldOnce].
     */
    class Workspace {
        internal var wide = IntArray(WIDE)
        internal var acc = IntArray(WIDE)
    }

    fun newWorkspace(): Workspace = Workspace()

    /** Школьное умножение 16x16 лимбов в 32 лимба, без приведения, в буфер [ws]. */
    fun mulWideInto(ws: Workspace, a: IntArray, b: IntArray) {
        val prod = ws.wide
        for (i in 0 until WIDE) prod[i] = 0
        for (i in 0 until LIMBS) {
            if (a[i] == 0) continue
            val ai = a[i].toLong()
            var carry = 0L
            for (j in 0 until LIMBS) {
                val v = prod[i + j] + ai * b[j] + carry
                prod[i + j] = (v and 0xFFFFL).toInt()
                carry = v ushr 16
            }
            var k = i + LIMBS
            while (carry != 0L) {
                val v = prod[k] + carry
                prod[k] = (v and 0xFFFFL).toInt()
                carry = v ushr 16
                k++
            }
        }
    }

    /** Школьное умножение в свежий массив — для холодных путей и тестов. */
    fun mulWide(a: IntArray, b: IntArray): IntArray {
        val ws = newWorkspace()
        mulWideInto(ws, a, b)
        return ws.wide.copyOf()
    }

    /** 32 лимба из буфера [ws] → канонический остаток по модулю p в [out]. */
    fun reduceInto(ws: Workspace, out: IntArray) {
        // 2^255 ≡ 19 (mod p): чем короче «хвост» старших битов, тем меньше он
        // становится. Трёх свёрток достаточно, чтобы получить < 2^255 + 19,
        // после чего одного условного вычитания p хватает (p = 2^255 - 19).
        foldOnce(ws)
        foldOnce(ws)
        foldOnce(ws)
        // `less`/`subtractInPlace` смотрят только на LIMBS лимбов, поэтому выше
        // лимба LIMBS-1 должно быть строго пусто — иначе остаток уехал бы в
        // p-редьюкшен, а его старшие биты молча отбросились бы.
        check(ws.wide[LIMBS] == 0 && ws.wide[LIMBS + 1] == 0) { "reduce: значение не влезло в 255 бит" }
        if (!less(ws.wide, P)) subtractInPlace(ws.wide, P)
        for (i in 0 until LIMBS) out[i] = ws.wide[i]
    }

    /** Канонический остаток 32 лимбов по модулю p — для холодных путей и тестов. */
    fun reduce(x: IntArray): IntArray {
        val ws = newWorkspace()
        for (i in 0 until WIDE) ws.wide[i] = x[i]
        val out = IntArray(LIMBS)
        reduceInto(ws, out)
        return out
    }

    /**
     * t = (t mod 2^255) + 19 * (t div 2^255) — свёртка по 2^255 ≡ 19 (mod p).
     *
     * Пишет результат в `ws.acc` и меняет буферы местами, так что после вызова
     * свёрнутое значение всегда в `ws.wide`. Отдельный аккумулятор обязателен:
     * младшие 255 бит и результат складываются в одном проходе, и перезапись
     * «на месте» затирала бы старые биты раньше, чем они прочитаны. Каждая
     * свёртка срезает хвост почти целиком, поэтому перенос за [WORK] лимбов
     * невозможен.
     */
    private fun foldOnce(ws: Workspace) {
        val src = ws.wide
        val dst = ws.acc
        // Младшие 255 бит — это ровно LIMBS лимбов с обрезанным старшим битом.
        // Лимбы LIMBS и выше — это уже «хвост» (t div 2^255), их в сумму не берём.
        // Лимб i числа t div 2^255 = (t[LIMBS-1+i] >> 15) | (t[LIMBS+i] & 0x7FFF) << 1,
        // потому что 255 = 16 * 15 + 15: бит 255 — старший бит лимба 15.
        var carry = 0L
        for (i in 0 until WORK) {
            val cur = if (i + LIMBS - 1 < WIDE) src[i + LIMBS - 1] else 0
            val nxt = if (i + LIMBS < WIDE) src[i + LIMBS] else 0
            val hi = ((cur ushr 15) and 1) or ((nxt and 0x7FFF) shl 1)
            val low = if (i < LIMBS) {
                if (i == LIMBS - 1) src[i] and 0x7FFF else src[i]
            } else 0
            val v = low.toLong() + hi.toLong() * 19L + carry
            dst[i] = (v and 0xFFFFL).toInt()
            carry = v ushr 16
        }
        var c = carry
        for (i in 0 until WORK) {
            val v = dst[i].toLong() + c
            dst[i] = (v and 0xFFFFL).toInt()
            c = v ushr 16
        }
        check(c == 0L) { "fold: перенос за пределы буфера" }
        for (i in WORK until WIDE) dst[i] = 0
        ws.wide = dst
        ws.acc = src
    }

    /**
     * Сумма по модулю [m]. Перенос держим в переменной, а не в 17-м лимбе: если
     * a, b < m, то a + b < 2m, поэтому одного условного вычитания [m] всегда
     * достаточно, а результат гарантированно меньше m и помещается в [LIMBS]
     * лимбов. Поэтому одного массива хватает — без промежуточного WORK и копии.
     */
    private fun addMod(a: IntArray, b: IntArray, m: IntArray): IntArray {
        val t = IntArray(LIMBS)
        var carry = 0
        for (i in 0 until LIMBS) {
            val s = a[i] + b[i] + carry
            t[i] = s and 0xFFFF
            carry = s ushr 16
        }
        if (carry != 0 || !less(t, m)) subtractInPlace(t, m)
        return t
    }

    private fun subMod(a: IntArray, b: IntArray, m: IntArray): IntArray {
        val t = IntArray(LIMBS)
        var borrow = 0
        for (i in 0 until LIMBS) {
            val d = a[i] - b[i] - borrow
            t[i] = d and 0xFFFF
            borrow = (d ushr 16) and 1
        }
        if (borrow != 0) {
            var carry = 0
            for (i in 0 until LIMBS) {
                val s = t[i] + m[i] + carry
                t[i] = s and 0xFFFF
                carry = s ushr 16
            }
        }
        return t
    }

    private fun subtractInPlace(t: IntArray, m: IntArray) {
        var borrow = 0
        for (i in 0 until LIMBS) {
            val d = t[i] - m[i] - borrow
            t[i] = d and 0xFFFF
            borrow = (d ushr 16) and 1
        }
        check(borrow == 0) { "subtract: результат отрицателен" }
    }

    private fun less(a: IntArray, b: IntArray): Boolean {
        for (i in LIMBS - 1 downTo 0) {
            if (a[i] != b[i]) return a[i] < b[i]
        }
        return false
    }

    /** Возведение в степень по основанию 2, старшие биты первыми. */
    fun pow(base: IntArray, exp: IntArray): IntArray = pow(newWorkspace(), base, exp)

    fun pow(ws: Workspace, base: IntArray, exp: IntArray): IntArray {
        var result = one()
        for (i in LIMBS - 1 downTo 0) {
            for (bit in 15 downTo 0) {
                result = mul(ws, result, result)
                if (((exp[i] ushr bit) and 1) != 0) result = mul(ws, result, base)
            }
        }
        return result
    }
}
