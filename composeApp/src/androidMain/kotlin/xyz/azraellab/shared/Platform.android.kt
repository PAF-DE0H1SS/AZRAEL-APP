package xyz.azraellab.shared

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import java.io.ByteArrayOutputStream
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

actual fun platformName(): String = "Android"

/**
 * Выбор файла через системный диалог (Storage Access Framework). Раньше здесь был
 * stub `= null`, из-за чего на Android не работали ни аватар, ни вложение в чат.
 * GetContent отдаёт временный доступ к содержимому — хватает прочитать файл сразу,
 * persistable-разрешение не нужно.
 */
@Composable
actual fun rememberFilePicker(
    mimeTypes: List<String>,
    maxBytes: Long,
    onResult: (FilePick) -> Unit
): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val callback = rememberUpdatedState(onResult)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) {
            callback.value(FilePick.Cancelled)
        } else {
            scope.launch {
                callback.value(withContext(Dispatchers.IO) { readPickedFile(context, uri, maxBytes) })
            }
        }
    }
    val filter = mimeTypes.firstOrNull { it.isNotBlank() } ?: "*/*"
    return { launcher.launch(filter) }
}

private fun readPickedFile(context: Context, uri: Uri, maxBytes: Long): FilePick {
    val resolver = context.contentResolver
    val mime = runCatching { resolver.getType(uri) }.getOrNull() ?: "application/octet-stream"
    val name = runCatching {
        resolver.query(uri, null, null, null, null)?.use { c ->
            val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
        }
    }.getOrNull()?.takeIf { it.isNotBlank() } ?: uri.lastPathSegment?.substringAfterLast('/') ?: "file"
    val bytes = runCatching {
        resolver.openInputStream(uri)?.use { input ->
            val out = ByteArrayOutputStream()
            val buf = ByteArray(32 * 1024)
            var total = 0L
            while (true) {
                val n = input.read(buf)
                if (n <= 0) break
                total += n
                // Обрываем раньше, чем файл целиком ляжет в память.
                if (total > maxBytes) return FilePick.Unavailable
                out.write(buf, 0, n)
            }
            out.toByteArray()
        }
    }.getOrNull()
    if (bytes == null || bytes.isEmpty()) return FilePick.Unavailable
    return FilePick.Picked(PickedFile(name, mime, Base64.encodeToString(bytes, Base64.NO_WRAP)))
}

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