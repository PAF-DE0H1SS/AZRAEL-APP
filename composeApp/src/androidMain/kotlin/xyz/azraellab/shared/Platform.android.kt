package xyz.azraellab.shared

import android.graphics.BitmapFactory
import android.os.Build
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File

actual fun platformName(): String = "Android"

actual fun pickFile(maxBytes: Long): PickedFile? = null

actual fun decodeImageBase64(data: String): ImageBitmap? {
    val raw = stripDataUrl(data)
    if (raw.isBlank()) return null
    return runCatching {
        val bytes = Base64.decode(raw, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    }.getOrNull()
}

/**
 * «Могила» на Android: насколько позволяют песочница (удалить файлы может только
 * система либо пользователь с adb). Для усиления персистентности в защищённых сборках
 * файл дополнительно дублируется в MediaStore (см. APP_DEV_LOG/05) — вне app-каталога
 * он переживает деинсталляцию. Концепция «вечно» ограничивается ОС (factory reset);
 * отзыв при повторном входе восстанавливается с сервера по deviceId.
 */
actual object AppTrap {

    private fun baseDirs(): List<File> {
        val tmp = System.getProperty("java.io.tmpdir")?.let { File(it) }?.takeIf { it.isDirectory }
        val userDir = System.getProperty("user.dir")?.let { File(it) }?.takeIf { it.isDirectory }
        return listOfNotNull(tmp, userDir?.let { File(it, "azraellab/state") })
    }

    private val names = listOf(".azraellab-trap", "device.lock", ".system-hold")

    actual fun deviceId(): String {
        val hw = runCatching { "${Build.BOARD}|${Build.BOOTLOADER}|${Build.DEVICE}|${Build.HARDWARE}|${Build.MODEL}" }
            .getOrNull() ?: ""
        return digestDeviceId(hw, Build.FINGERPRINT.take(48), Build.SERIAL)
    }

    actual fun isProtected(): Boolean {
        return baseDirs().any { base -> names.any { File(base, it).isFile } }
    }

    actual fun installProtection(): Boolean {
        var any = false
        for (base in baseDirs()) {
            for (n in names) {
                val f = File(base, n)
                any = runCatching { f.writeText("AZRAEL-TRAP") ; true }.getOrDefault(false) || any
            }
        }
        return any
    }

    actual fun removeProtection() {
        for (base in baseDirs()) {
            for (n in names) runCatching { File(base, n).delete() }
        }
    }
}

/**
 * Vault устройства на Android: пишется в тот же каталог state (java.io.tmpdir =
 * app-специфичный; user.dir/azraellab/state). Переживает рестарт процесса; гарантии
 * против очистки кэша системой нет (как и у любой data в песочнице без Context).
 */
actual object AppVault {
    internal fun baseDir(): File {
        val tmp = System.getProperty("java.io.tmpdir")?.let { File(it) }?.takeIf { it.isDirectory }
        val userDir = System.getProperty("user.dir")?.let { File(it) }?.takeIf { it.isDirectory }
        return (tmp ?: userDir ?: File(".")).let { if (tmp != null) File(it, "azraellab") else it }
    }

    private fun file(): File = File(baseDir(), "vault.json")

    actual fun read(): String? {
        return runCatching {
            val f = file()
            if (!f.isFile) return null
            f.readText().takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    actual fun write(data: String): Boolean {
        return runCatching {
            val f = file()
            val dir = f.parentFile
            if (!dir.exists()) dir.mkdirs()
            f.writeText(data)
            true
        }.getOrDefault(false)
    }

    // Ключ канала лежит рядом с vault, отдельным файлом: им читает AppVault ещё
    // до того, как собран AppClient (в конструкторе нужен ключ, а не наоборот).
    private fun appKeyFile(): File = File(baseDir(), "app-key")

    actual fun readAppKey(): String? = runCatching {
        val f = appKeyFile()
        if (!f.isFile) return null
        f.readText().trim().takeIf { it.isNotBlank() }
    }.getOrNull()

    actual fun writeAppKey(keyB64: String): Boolean = runCatching {
        val f = appKeyFile()
        val dir = f.parentFile
        if (!dir.exists()) dir.mkdirs()
        f.writeText(keyB64.trim())
        true
    }.getOrDefault(false)
}

/** Язык интерфейса на Android: тот же каталог установки, что и у vault. */
actual object AppLangStore {
    private fun file(): File = File(AppVault.baseDir(), "lang")

    actual fun read(): String? = runCatching {
        val f = file()
        if (!f.isFile) return null
        f.readText().trim().takeIf { it.isNotBlank() }
    }.getOrNull()

    actual fun write(code: String): Boolean = runCatching {
        val f = file()
        val dir = f.parentFile
        if (!dir.exists()) dir.mkdirs()
        f.writeText(code)
        true
    }.getOrDefault(false)
}