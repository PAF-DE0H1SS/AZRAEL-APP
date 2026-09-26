package xyz.azraellab.shared

import androidx.compose.ui.graphics.ImageBitmap
import xyz.azraellab.shared.core.crypto.Crypto

expect fun platformName(): String

/** Выбранный пользователем файл (для вложений и аватара). */
data class PickedFile(val name: String, val mime: String, val base64: String)

/** Диалог выбора файла. null — отмена или платформа без файлового диалога. */
expect fun pickFile(maxBytes: Long = 20L * 1024 * 1024): PickedFile?

/** Декод base64-картинки (data:image/png;base64,…) в ImageBitmap; null при ошибке. */
expect fun decodeImageBase64(data: String): ImageBitmap?

/**
 * «Могила» (tombstone) устройства — режим защиты при отзыве/скомпрометированном канале.
 *
 * Принцип: обычный флаг («не давать работать») обязан переживать переустановку и
 * не заметаться приложением вручную, поэтому пишется НЕ в данные программы, а во
 * «внешнюю» точку (root-каталоги/блокировщик системы на десктопе; медиа-/shared-
 * хранилище на Android). Программа на старте проверяет могилу ДО любого обращения
 * к сети: если она есть — приложение в режиме защиты, даже офлайн.
 *
 * Снять можно ТОЛЬКО с сайта через /admin (x-azrael-release в подписанном ответе).
 */
expect object AppTrap {
    /** Стабильный идентификатор устройства (для отзыва через /admin). */
    fun deviceId(): String

    /** Есть ли «могила» (режим защиты) на этом устройстве. */
    fun isProtected(): Boolean

    /** Записать «могилу» (максимум точек, какие удастся). Вернёт успех хотя бы одной. */
    fun installProtection(): Boolean

    /** Снять «могилу» — вызывается ТОЛЬКО по команде с сайта (x-azrael-release). */
    fun removeProtection(): Unit
}

/**
 * Персистентное хранилище данных установки (sealed JSON) — ключь устройства,
 * его идентификатор и session-токен. Живёт вне каталога программы (переживает
 * переустановку/перезагрузку, пока это позволяет платформа). Данные защищены
 * правами файловой системы; встроенного шифрования нет — как и у всех локальных
 * секретов приложения (токены/ключи только локально, правило проекта).
 */
expect object AppVault {
    /** Прочитать сохранённый blob (null — ещё не было привязки). */
    fun read(): String?

    /** Сохранить blob (перезаписывает). */
    fun write(data: String): Boolean

    /**
     * Ключ канала (/api/app/v1), полученный при первом запуске из открытой
     * точки /api/app/bootstrap. Пользователь его не вводит и не видит; здесь он
     * лежит до первой привязки, после чего остаётся тем же файлом при рестартах.
     * null — ключ ещё не получен (первый запуск без сети).
     */
    fun readAppKey(): String?

    /** Сохранить ключ канала (base64). Перезаписывает. */
    fun writeAppKey(keyB64: String): Boolean
}

/**
 * Локальное сохранение языка интерфейса (обычный файл, не секрет). Нужно, чтобы
 * входной экран был переведён до первого обращения к сети, а язык аккаунта
 * (users.lang) подхватывался на этом же устройстве.
 */
expect object AppLangStore {
    /** Сохранённый код языка (ru/en/zh) или null. */
    fun read(): String?

    /** Сохранить код языка. */
    fun write(code: String): Boolean
}

internal fun stripDataUrl(data: String): String =
    if (data.startsWith("data:")) data.substringAfter("base64,", "") else data

internal fun digestDeviceId(vararg parts: String): String {
    val joined = parts.joinToString("|")
    return Hex.toHex(Crypto.sha256(joined.toByteArray(Charsets.UTF_8)).copyOf(16))
}

internal object Hex {
    private const val CHARS = "0123456789abcdef"
    fun toHex(b: ByteArray): String {
        val sb = StringBuilder(b.size * 2)
        for (x in b) { sb.append(CHARS[(x.toInt() ushr 4) and 0xf]); sb.append(CHARS[x.toInt() and 0xf]) }
        return sb.toString()
    }
}