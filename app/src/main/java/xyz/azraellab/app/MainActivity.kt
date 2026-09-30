package xyz.azraellab.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import xyz.azraellab.shared.App
import xyz.azraellab.shared.AppDeepLink
import xyz.azraellab.shared.initStorageDir

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // До первого чтения vault: хранилище должно быть в filesDir, а не в
        // cache-каталоге из java.io.tmpdir, который система стирает при нехватке
        // места — вместе с ключом установки и app-key.
        initStorageDir(filesDir.absolutePath)
        // Ссылка azrael://messages/42 из intent'а — до setContent, потому что
        // App читает её один раз при первой композиции.
        AppDeepLink.offer(intent?.data)
        setContent {
            App(nativeGreeting = { nativeString() })
        }
    }

    /**
     * Ссылка может прийти в уже открытое приложение (`singleTask` из manifest).
     * Флаг `singleTask` в манифесте не стоит, поэтому новый экземпляр не создаётся,
     * а существующий получает `onNewIntent`.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        AppDeepLink.offer(intent.data)
    }

    private external fun nativeString(): String

    companion object {
        init {
            System.loadLibrary("app")
        }
    }
}