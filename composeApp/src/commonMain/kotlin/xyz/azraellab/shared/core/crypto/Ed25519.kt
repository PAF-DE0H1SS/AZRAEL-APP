package xyz.azraellab.shared.core.crypto

/**
 * Ed25519 (RFC 8032) — своя реализация на чистом Kotlin, в `commonMain`.
 *
 * Зачем она, а не JCA: на Android программного Ed25519 в провайдерах нет вовсе.
 * `KeyPairGenerator.getInstance("Ed25519")` там попадает в AndroidKeyStore
 * (`KeyPairGenerator/ED25519@AndroidKeyStore` — единственная регистрация Ed25519),
 * а он умеет только аппаратные ключи и на программной генерации отвечает
 * `IllegalStateException: Not initialized`. Провайдеры `AndroidOpenSSL`, `BC`,
 * `HarmonyJSSE` Ed25519 не регистрируют ни в `KeyPairGenerator`, ни в `KeyFactory`.
 *
 * Значит и сырые 32 байта приватного ключа, и подпись приходится считать самим.
 * Заодно сразу снимается и вторая проблема: `BigInteger`/`MessageDigest` живут
 * только в JVM, а эта реализация честно лежит в `commonMain` и потому годится
 * будущим не-JVM таргетам (wasmJs/iOS) без платформенного дубля.
 *
 * Арифметика — в [Fe25519], дайджест — в [Sha512].
 *
 * Проверяется RFC 8032 test vectors и кросс-проверкой против JCA (см. `Ed25519Test`).
 */
internal object Ed25519 {

    /** Точка в расширенных координатах: x = X/Z, y = Y/Z, T = XY/Z. */
    private class Point(val x: IntArray, val y: IntArray, val z: IntArray, val t: IntArray)

    /** d = -121665/121666 mod p — параметр кривой. */
    private val D: IntArray = Fe25519.neg(
        Fe25519.mul(Fe25519.fromInt(121665), Fe25519.invert(Fe25519.fromInt(121666)))
    )

    /** 2d — в формуле сложения он всегда умножается на T1*T2. */
    private val D2: IntArray = Fe25519.add(D, D)

    private val BX_BYTES = byteArrayOf(
        0x1a.toByte(), 0xd5.toByte(), 0x25.toByte(), 0x8f.toByte(), 0x60.toByte(), 0x2d.toByte(), 0x56.toByte(), 0xc9.toByte(),
        0xb2.toByte(), 0xa7.toByte(), 0x25.toByte(), 0x95.toByte(), 0x60.toByte(), 0xc7.toByte(), 0x2c.toByte(), 0x69.toByte(),
        0x5c.toByte(), 0xdc.toByte(), 0xd6.toByte(), 0xfd.toByte(), 0x31.toByte(), 0xe2.toByte(), 0xa4.toByte(), 0xc0.toByte(),
        0xfe.toByte(), 0x53.toByte(), 0x6e.toByte(), 0xcd.toByte(), 0xd3.toByte(), 0x36.toByte(), 0x69.toByte(), 0x21.toByte()
    )

    private val BY_BYTES = ByteArray(32).also {
        it[0] = 0x58.toByte()
        for (i in 1 until 32) it[i] = 0x66.toByte()
    }

    private val BASE: Point = run {
        val bx = Fe25519.fromLe(BX_BYTES)
        val by = Fe25519.fromLe(BY_BYTES)
        Point(bx, by, Fe25519.one(), Fe25519.mul(bx, by))
    }

    private val IDENTITY = Point(Fe25519.zero(), Fe25519.one(), Fe25519.one(), Fe25519.zero())

    /**
     * Таблица `2^i · B` для базовой точки: [BASE] и она сама неизменны, поэтому
     * все 256 промежуточных пунктов считаются один раз за жизнь процесса.
     *
     * Обычный «удвоение и прибавление» делает 256 удвоений плюс сложения — 384
     * операции с точками по 9 полевых умножений, и это на каждый запрос к API.
     * С таблицей удвоений нет вовсе, а сложений в среднем 128 (столько битов
     * установлено в случайном `r`) — примерно втрое меньше работы. Сборка
     * таблицы — 255 удвоений один раз, около 20 мс, лениво при первом
     * использовании; [lazy] по умолчанию синхронизирован, а после сборки
     * таблица только читается, поэтому конкурентные подписи её не портят.
     */
    private val BASE_TABLE: Array<Point> by lazy {
        val table = Array<Point>(BITS) { IDENTITY }
        table[0] = BASE
        val ws = Fe25519.newWorkspace()
        for (i in 1 until BITS) table[i] = add(table[i - 1], table[i - 1], ws)
        table
    }

    /** Скаляр в 256 бит — ровно столько пунктов в [BASE_TABLE]. */
    private const val BITS = 256

    /** Публичный ключ (raw-32) из 32-байтового seed приватного ключа. */
    fun publicKeyFromSeed(seed: ByteArray): ByteArray {
        require(seed.size == 32) { "ed25519 seed must be 32 bytes, got ${seed.size}" }
        val h = Sha512.digest(seed)
        val a = clampScalar(h.copyOfRange(0, 32))
        val ws = Fe25519.newWorkspace()
        // Здесь намеренно обычное умножение «удвоением и прибавлением», а не
        // [scalarMultiplyBase]: вызывается один раз за ключ, и независимый путь
        // служит перекрёстной проверкой таблицы в кросс-тестах против JCA.
        return encodePoint(scalarMultiply(a, BASE, ws), ws)
    }

    /** Подпись (64 байта: R||S) сообщения [message] seed-ом [seed]. */
    fun sign(seed: ByteArray, message: ByteArray): ByteArray =
        signWithPublicKey(seed, publicKeyFromSeed(seed), message)

    /**
     * Подпись с уже вычисленным публичным ключом [publicKey].
     *
     * [publicKey] обязан быть ровно `publicKeyFromSeed(seed)` — по RFC 8032 он
     * зависит только от seed, поэтому вызывающая сторона (у неё ключи установки
     * уже лежат в хранилище) может передать его повторно и не платить за второе
     * умножение на базовую точку. Ошибка здесь дала бы подпись, которую сервер
     * отвергнет, а не «слабую» подпись: приватный ключ в вычислении `S` тот же.
     */
    fun signWithPublicKey(seed: ByteArray, publicKey: ByteArray, message: ByteArray): ByteArray {
        require(seed.size == 32) { "ed25519 seed must be 32 bytes, got ${seed.size}" }
        require(publicKey.size == 32) { "ed25519 public key must be 32 bytes, got ${publicKey.size}" }
        val h = Sha512.digest(seed)
        val a = clampScalar(h.copyOfRange(0, 32))
        val prefix = h.copyOfRange(32, 64)

        // Буферы полевой арифметики — на одну операцию подписи, а не на весь
        // объект: `Ed25519` singleton используется конкурентно из корутин, и
        // общий изменяемый scratch здесь означал бы гонку между подписями.
        val ws = Fe25519.newWorkspace()

        val r = Fe25519.modL(Fe25519.fromLeWide(Sha512.digest(concatBytes(prefix, message))))
        val rEncoded = encodePoint(scalarMultiplyBase(r, ws), ws)

        val k = Fe25519.modL(
            Fe25519.fromLeWide(Sha512.digest(concatBytes(rEncoded, publicKey, message)))
        )
        // S = (r + k*a) mod L. k*a не берём по модулю L на месте — произведение
        // целиком шириной 512 бит, и modL сразу даёт нужный остаток.
        val s = Fe25519.addModL(Fe25519.modL(Fe25519.mulWide(k, a)), r)

        return concatBytes(rEncoded, Fe25519.toLe(s, 32))
    }

    /**
     * RFC 8032 §5.1.5: a = h[0..32], где младшие 3 бита отбрасываются, бит 254
     * ставится, бит 255 очищается.
     */
    private fun clampScalar(h: ByteArray): IntArray {
        val a = h.copyOf(32)
        a[0] = (a[0].toInt() and 248).toByte()
        a[31] = ((a[31].toInt() and 127) or 64).toByte()
        return Fe25519.fromLe(a)
    }

    private fun scalarMultiply(scalar: IntArray, point: Point, ws: Fe25519.Workspace): Point {
        var result = IDENTITY
        for (i in Fe25519.LIMBS - 1 downTo 0) {
            for (bit in 15 downTo 0) {
                result = add(result, result, ws)
                if (((scalar[i] ushr bit) and 1) != 0) result = add(result, point, ws)
            }
        }
        return result
    }

    /**
     * Умножение на базовую точку через предвычисленную [BASE_TABLE]: складываем
     * только пункты `2^i · B` для установленных битов скаляра, без удвоений.
     * Первый найденный пункт становится результатом сразу — складывать его с
     * единицей не нужно. [Point] неизменяемы, поэтому пункты таблицы можно
     * отдавать наружу и переиспользовать.
     */
    private fun scalarMultiplyBase(scalar: IntArray, ws: Fe25519.Workspace): Point {
        val table = BASE_TABLE
        var result = IDENTITY
        var started = false
        for (i in 0 until Fe25519.LIMBS) {
            for (bit in 0 until 16) {
                if (((scalar[i] ushr bit) and 1) == 0) continue
                val point = table[i * 16 + bit]
                result = if (started) add(result, point, ws) else point
                started = true
            }
        }
        return result
    }

    /**
     * Сложение в расширенных координатах twisted Edwards (a = -1).
     * Формула полная: корректна и для P + P, поэтому удвоение идёт через неё же.
     */
    private fun add(p: Point, q: Point, ws: Fe25519.Workspace): Point {
        val a = Fe25519.mul(ws, Fe25519.sub(p.y, p.x), Fe25519.sub(q.y, q.x))
        val b = Fe25519.mul(ws, Fe25519.add(p.y, p.x), Fe25519.add(q.y, q.x))
        val c = Fe25519.mul(ws, Fe25519.mul(ws, p.t, D2), q.t)
        val d = Fe25519.mul(ws, Fe25519.add(p.z, p.z), q.z)
        val e = Fe25519.sub(b, a)
        val f = Fe25519.sub(d, c)
        val g = Fe25519.add(d, c)
        val h = Fe25519.add(b, a)
        return Point(
            x = Fe25519.mul(ws, e, f),
            y = Fe25519.mul(ws, g, h),
            t = Fe25519.mul(ws, e, h),
            z = Fe25519.mul(ws, f, g)
        )
    }

    /** Сжатая точка: 32 байта little-endian из y, старший бит хранит знак x. */
    private fun encodePoint(point: Point, ws: Fe25519.Workspace): ByteArray {
        val zInv = Fe25519.invert(ws, point.z)
        val x = Fe25519.mul(ws, point.x, zInv)
        val y = Fe25519.mul(ws, point.y, zInv)
        val out = Fe25519.toLe(y, 32)
        if (Fe25519.isOdd(x)) out[31] = (out[31].toInt() or 0x80).toByte()
        return out
    }

    private fun concatBytes(vararg parts: ByteArray): ByteArray {
        var size = 0
        for (p in parts) size += p.size
        val out = ByteArray(size)
        var at = 0
        for (p in parts) {
            p.copyInto(out, at)
            at += p.size
        }
        return out
    }
}
