package xyz.azraellab.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import xyz.azraellab.shared.App
import xyz.azraellab.shared.initStorageDir

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // До первого чтения vault: хранилище должно быть в filesDir, а не в
        // cache-каталоге из java.io.tmpdir, который система стирает при нехватке
        // места — вместе с ключом установки и app-key.
        initStorageDir(filesDir.absolutePath)
        setContent {
            App(nativeGreeting = { nativeString() })
        }
    }

    private external fun nativeString(): String

    companion object {
        init {
            System.loadLibrary("app")
        }
    }
}