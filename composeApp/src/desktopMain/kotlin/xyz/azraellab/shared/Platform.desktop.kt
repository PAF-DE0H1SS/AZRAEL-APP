package xyz.azraellab.shared

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.Base64
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image

actual fun platformName(): String = "Desktop (JVM)"
/** На десктопе путь хранилища свой (user.home), вызов из мобильной Activity не нужен. */
actual fun initStorageDir(dir: String): Unit = Unit

actual fun logAzraelError(tag: String, message: String, error: Throwable?) {
    System.err.println("[$tag] $message")
    error?.printStackTrace()
}


@Composable
actual fun rememberFilePicker(
    mimeTypes: List<String>,
    maxBytes: Long,
    onResult: (FilePick) -> Unit
): () -> Unit {
    val scope = rememberCoroutineScope()
    val callback = rememberUpdatedState(onResult)
    return {
        scope.launch {
            // JFileChooser блокирующий и не любит EDT — читаем файл в отдельном потоке.
            callback.value(withContext(Dispatchers.IO) { showOpenDialog(maxBytes) })
        }
    }
}

private fun showOpenDialog(maxBytes: Long): FilePick {
    val chooser = JFileChooser()
    chooser.isMultiSelectionEnabled = false
    chooser.isAcceptAllFileFilterUsed = true
    chooser.fileFilter = FileNameExtensionFilter("Изображения, текст, документы", "png", "jpg", "jpeg", "gif", "webp", "bmp", "txt", "md", "pdf", "json", "zip")
    if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) return FilePick.Cancelled
    val file = chooser.selectedFile ?: return FilePick.Cancelled
    if (!file.isFile || file.length() > maxBytes) return FilePick.Unavailable
    return FilePick.Picked(PickedFile(file.name, mimeByName(file.name), Base64.getEncoder().encodeToString(file.readBytes())))
}

actual fun decodeImageBase64(data: String): ImageBitmap? {
    val raw = stripDataUrl(data)
    if (raw.isBlank()) return null
    return runCatching {
        Image.makeFromEncoded(Base64.getDecoder().decode(raw)).use { it.toComposeImageBitmap() }
    }.getOrNull()
}

fun copyToClipboard(text: String): Boolean = runCatching {
    Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
    true
}.getOrDefault(false)

private fun mimeByName(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
    "png" -> "image/png"
    "jpg", "jpeg" -> "image/jpeg"
    "gif" -> "image/gif"
    "webp" -> "image/webp"
    "bmp" -> "image/bmp"
    "pdf" -> "application/pdf"
    "json" -> "application/json"
    "zip" -> "application/zip"
    "md" -> "text/markdown"
    else -> "text/plain"
}

/**
 * «Могила» на десктопе: копии доступны в системных каталогах + immutable-атрибут
 * (chattr +i), который снимается только root-суперпользователем. Программа опирается
 * на наличие ЛЮБОЙ копии — удаление одной не снимет защиту.
 */
actual object AppTrap {
    // Каталоги, не принадлежащие приложению: переживают переустановку.
    private val paths: List<File> = listOf(
        File("/var/lib/azraellab/.device.lock"),
        File("/etc/azraellab/.device.lock"),
        File("/usr/local/share/azraellab/.lock"),
        File(System.getProperty("user.home"), ".config/azraellab/device.lock"),
        File(System.getProperty("java.io.tmpdir"), "azraellab-device.lock"),
    )

    private const val MARK = "AZRAEL-TRAP"

    actual fun deviceId(): String {
        val machineId = runCatching { File("/etc/machine-id").readText().trim() }.getOrNull() ?: ""
        val hostname = runCatching { File("/etc/hostname").readText().trim() }.getOrNull() ?: "host"
        return digestDeviceId(machineId, hostname, System.getProperty("os.name"))
    }

    // Проверяем содержимое, а не сам факт существования файла: иначе любой
    // пустой/поддельный файл с правильным именем «снимает» защиту, и клиент
    // продолжит работу с отравленным каналом.
    actual fun isProtected(): Boolean = paths.any { p ->
        runCatching { p.isFile && p.readText().contains(MARK) }.getOrDefault(false)
    }

    actual fun installProtection(): Boolean {
        var any = false
        for (p in paths) {
            any = write(p, MARK) || any
        }
        for (p in listOf(File("/var/lib/azraellab/.device.lock"), File("/etc/azraellab/.device.lock"))) {
            runCatching {
                val dir = p.parentFile
                if (dir != null && dir.canWrite()) ProcessBuilder("chattr", "+i", p.absolutePath).start().waitFor()
            }
        }
        return any
    }

    actual fun removeProtection() {
        for (p in paths) {
            runCatching { ProcessBuilder("chattr", "-i", p.absolutePath).start().waitFor() }
            runCatching { p.delete() }
        }
        // Файл во временном каталоге может быть пересоздан — вычищаем.
        runCatching { File(System.getProperty("java.io.tmpdir"), "azraellab-device.lock").delete() }
    }

    private fun write(p: File, content: String): Boolean {
        return runCatching {
            val parent = p.parentFile ?: return false
            if (!parent.exists()) parent.mkdirs()
            val tmp = Path.of(p.absolutePath + ".tmp")
            Files.write(tmp, content.toByteArray(Charsets.UTF_8))
            Files.move(tmp, Path.of(p.absolutePath), java.nio.file.StandardCopyOption.REPLACE_EXISTING)
            true
        }.getOrDefault(false)
    }
}

/**
 * Vault устройства на десктопе: файл вне рабочего каталога (~/.config/azraellab/vault.json),
 * атомарная запись через tmp+rename, как у могилы. Переживает переустановку программы.
 */
actual object AppVault {
    private fun file(): File {
        val home = System.getProperty("user.home") ?: "."
        return File(home, ".config/azraellab/vault.json")
    }

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
            val parent = f.parentFile
            if (!parent.exists()) parent.mkdirs()
            // Секреты (ключ установки, device seed) лежат в этом каталоге,
            // поэтому закрываем и каталог, и файл от других пользователей системы.
            if (!parent.setReadable(false, false) ||
                !parent.setWritable(false, false) ||
                !parent.setExecutable(false, false)
            ) return@runCatching false
            parent.setReadable(true, true)
            parent.setWritable(true, true)
            parent.setExecutable(true, true)
            val tmp = Path.of(f.absolutePath + ".tmp")
            Files.write(tmp, data.toByteArray(Charsets.UTF_8))
            restrictToOwner(tmp.toFile())
            Files.move(tmp, Path.of(f.absolutePath), java.nio.file.StandardCopyOption.REPLACE_EXISTING)
            restrictToOwner(f)
            true
        }.getOrDefault(false)
    }

    /** 0600: содержимое vault читается только владельцем. */
    private fun restrictToOwner(f: File): Boolean =
        f.setReadable(false, false) && f.setWritable(false, false) &&
            f.setReadable(true, true) && f.setWritable(true, true)

    // Ключ канала лежит рядом с vault, отдельным файлом: им читает AppVault ещё
    // до того, как собран AppClient (в конструкторе нужен ключ, а не наоборот).
    private fun appKeyFile(): File {
        val home = System.getProperty("user.home") ?: "."
        return File(home, ".config/azraellab/app-key")
    }

    actual fun readAppKey(): String? = appKeyFile().takeIf { it.isFile }?.let { f ->
        runCatching { f.readText().trim() }.getOrNull()
    }?.let(::parseAppKey)?.second

    // devId, которому принадлежит ключ. null у файла, записанного старой
    // версией программы (там был только ключ) — такой ключ нельзя переиспользовать.
    actual fun readAppKeyDevId(): String? = appKeyFile().takeIf { it.isFile }?.let { f ->
        runCatching { f.readText().trim() }.getOrNull()
    }?.let(::parseAppKey)?.first

    // Формат файла: "devId\nkeyB64". Старый однострочный файл читается как
    // (null, key) — с пометкой, что маркера нет.
    private fun parseAppKey(raw: String): Pair<String?, String>? {
        if (raw.isBlank()) return null
        val nl = raw.indexOf('\n')
        if (nl <= 0) return null to raw.trim()
        val dev = raw.substring(0, nl).trim()
        val key = raw.substring(nl + 1).trim()
        if (dev.isEmpty() || key.isEmpty()) return null
        return dev to key
    }

    actual fun writeAppKey(devId: String, keyB64: String): Boolean = runCatching {
        val f = appKeyFile()
        val parent = f.parentFile
        if (!parent.exists()) parent.mkdirs()
        val tmp = Path.of(f.absolutePath + ".tmp")
        Files.write(tmp, "$devId\n${keyB64.trim()}".toByteArray(Charsets.UTF_8))
        Files.move(tmp, Path.of(f.absolutePath), java.nio.file.StandardCopyOption.REPLACE_EXISTING)
        try {
            f.setReadable(false, false)
            f.setReadable(true, true)
            f.setWritable(false, false)
            f.setWritable(true, true)
        } catch (_: Exception) {
        }
        true
    }.getOrDefault(false)
}

/** Язык интерфейса на десктопе: ~/.config/azraellab/lang, рядом с vault (не секрет). */
actual object AppLangStore {
    private fun file(): File {
        val home = System.getProperty("user.home") ?: "."
        return File(home, ".config/azraellab/lang")
    }

    actual fun read(): String? = runCatching {
        val f = file()
        if (!f.isFile) return null
        f.readText().trim().takeIf { it.isNotBlank() }
    }.getOrNull()

    actual fun write(code: String): Boolean = runCatching {
        val f = file()
        val parent = f.parentFile
        if (!parent.exists()) parent.mkdirs()
        val tmp = Path.of(f.absolutePath + ".tmp")
        Files.write(tmp, code.toByteArray(Charsets.UTF_8))
        Files.move(tmp, Path.of(f.absolutePath), java.nio.file.StandardCopyOption.REPLACE_EXISTING)
        true
    }.getOrDefault(false)
}