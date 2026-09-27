package xyz.azraellab.shared

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Cottage
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VpnLock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import xyz.azraellab.shared.core.api.AppClient
import xyz.azraellab.shared.core.api.AppErrorCode
import xyz.azraellab.shared.core.api.AppException
import xyz.azraellab.shared.core.api.AppKeyBootstrap
import xyz.azraellab.shared.core.api.AppSecure
import xyz.azraellab.shared.core.api.ThreatMode
import xyz.azraellab.shared.core.crypto.Base64Codec
import xyz.azraellab.shared.core.api.AppErrorCode.NETWORK
import xyz.azraellab.shared.core.protocol.AppRuntime
import xyz.azraellab.shared.core.protocol.GatewayClient
import xyz.azraellab.shared.core.protocol.defaultAppSrvPubB64
import xyz.azraellab.shared.core.protocol.defaultAppUrl
import xyz.azraellab.shared.core.protocol.defaultGatewayUrl
import xyz.azraellab.shared.ui.AzraelCyan
import xyz.azraellab.shared.ui.AzraelRose
import xyz.azraellab.shared.ui.AzraelTheme
import xyz.azraellab.shared.ui.AzraelViolet
import xyz.azraellab.shared.ui.GlassBackground
import xyz.azraellab.shared.ui.glass

// ---- Данные кастомного API (POST /api/app/v1), контракт APP_DEV_LOG/02/04 ----

private fun JsonObject.s(key: String): String? = this[key]?.jsonPrimitive?.content
private fun JsonObject.l(key: String): Long? = this[key]?.jsonPrimitive?.content?.toLongOrNull()
private fun JsonObject.i(key: String): Int? = this[key]?.jsonPrimitive?.content?.toIntOrNull()
private fun JsonObject.o(key: String): JsonObject? = this[key]?.jsonObject
private fun JsonObject.a(key: String): List<JsonElement> =
    this[key]?.let { runCatching { it.jsonArray }.getOrNull() }?.toList() ?: emptyList()

private fun JsonElement.boolValue(): Boolean {
    val text = runCatching { jsonPrimitive.content }.getOrNull() ?: return false
    return text == "true" || text == "1"
}

/** Настоящий JSON-boolean: сервер шлёт true/false, но в MariaDB-полях встречается 0/1. */
private fun JsonObject.b(key: String): Boolean = this[key]?.boolValue() ?: false

/** HTTPS обязателен; http:// допускается только для локального стенда. */
private fun normalizedEndpoint(raw: String): String? {
    val url = raw.trim()
    val lower = url.lowercase()
    val isHttps = lower.startsWith("https://")
    val isHttp = lower.startsWith("http://")
    if (!isHttps && !isHttp) return null

    val authority = url.substringAfter("://", "")
        .substringBefore('/')
        .substringBefore('?')
        .substringBefore('#')
    if (authority.isEmpty() || '@' in authority) return null

    val host = if (authority.startsWith('[')) {
        val closing = authority.indexOf(']')
        if (closing <= 1) return null
        authority.substring(1, closing)
    } else {
        authority.substringBefore(':')
    }.lowercase()
    if (host.isEmpty() || host.any { it.isWhitespace() }) return null
    if (isHttps) return url

    return if (host == "localhost" || host == "127.0.0.1" || host == "::1") url else null
}

data class AppProfile(
    val id: Long,
    val uid: String,
    val username: String,
    val displayName: String,
    val tag: String?,
    val gender: String,
    val role: String,
    val hasAvatar: Boolean,
    val autoDeleteDays: Int?,
    val tgBound: Boolean,
    val tgUsername: String?,
    /** Язык аккаунта (users.lang) — общий для сайта и всех устройств. */
    val lang: String?
)

private fun parseProfile(o: JsonObject): AppProfile = AppProfile(
    id = o.l("id") ?: 0L,
    uid = o.s("uid") ?: "",
    username = o.s("username") ?: "",
    displayName = o.s("display_name") ?: "",
    tag = o.s("tag"),
    gender = o.s("gender") ?: "",
    role = o.s("role") ?: "standard",
    hasAvatar = o.b("has_avatar"),
    autoDeleteDays = o.i("auto_delete_account_days"),
    tgBound = o.b("tg_bound"),
    tgUsername = o.s("tg_username"),
    lang = o.s("lang")
)

/**
 * Локальная подпись таба: сервер присылает русские label'ы (/main — русский UI),
 * поэтому переводим по id, а серверный label оставляем запасным для новых табов.
 */
private fun tabLabel(id: String, serverLabel: String): String = when (id) {
    "admin" -> t["tab.admin"]
    "messages" -> t["tab.messages"]
    "shortener" -> t["tab.shortener"]
    "vpn_tab", "vpn" -> t["tab.vpn"]
    "settings" -> t["tab.settings"]
    else -> serverLabel
}

private fun parseTabs(cfg: JsonObject?): List<Pair<String, String>> {
    val out = mutableListOf<Pair<String, String>>()
    for (e in cfg?.a("tabs") ?: emptyList()) {
        val t = e as? JsonObject ?: continue
        val id = t.s("id") ?: continue
        if (t.containsKey("visible") && !t.b("visible")) continue
        out += id to tabLabel(id, t.s("label") ?: id)
    }
    return out
}

private fun roleColor(role: String): Color = when (role) {
    "owner" -> AzraelRose
    "admin" -> AzraelViolet
    "rf" -> Color(0xFFFFB74D)
    "premium" -> Color(0xFFE6C86A)
    else -> AzraelCyan
}

/**
 * Текст ошибки для показа пользователю. Русские формулировки сервера приходят
 * в e.message — их не переводим (иначе получим «Ошибка заполнения — Введите
 * код»), поэтому для известных случаев берём свою строку по коду.
 */
private fun errText(e: Throwable): String = if (e is AppException) {
    if (e.message?.startsWith("Лимит устройств") == true) {
        e.message ?: t["error.deviceLimit"]
    } else {
        val base = when (e.code) {
            NETWORK -> t["error.network"]
            AppErrorCode.MALFORMED -> t["error.malformed"]
            AppErrorCode.SESSION -> t["error.session"]
            AppErrorCode.FORBIDDEN -> t["error.forbidden"]
            AppErrorCode.INVALID_CRED -> t["error.credentials"]
            AppErrorCode.USER_EXISTS -> t["error.userExists"]
            AppErrorCode.INVITE_BAD -> t["error.invite"]
            AppErrorCode.VALIDATION -> t["error.validation"]
            else -> t("error.code", e.code)
        }
        val detail = e.message?.trim().orEmpty()
        if (detail.isEmpty() || detail == "err=${e.code}") base else "$base — $detail"
    }
} else {
    t("error.crash", e.message)
}

@Composable
fun App(nativeGreeting: () -> String) {
    AzraelTheme {
        GlassBackground {
            // Язык с устройства — до первой сети, чтобы входной экран тоже был переведён.
            LaunchedEffect(Unit) { I18n.load() }
            AppRoot(nativeGreeting)
        }
    }
}

// Активная сессия: клиент кастомного API + данные главного экрана (home.boot == /main).
private class Session(val client: AppClient, boot: JsonObject) {
    var boot: JsonObject = boot
}

/** Язык аккаунта из home.boot: сервер — источник истины, локальный файл — только кэш. */
private fun applyBootLang(boot: JsonObject) {
    val lang = boot.s("lang") ?: boot.o("profile")?.s("lang")
    I18n.applyServer(lang)
}

/** Состояние автоматического получения ключа канала: пользователь его не вводит. */
private sealed interface ChannelKey {
    data object Loading : ChannelKey
    data class Ready(val keyB64: String) : ChannelKey
    data class Failed(val reason: String) : ChannelKey
}

/**
 * Ключ канала для адреса API: из AppVault, иначе одноразовый запрос
 * `GET /api/app/bootstrap`. Адрес и ключ не вводятся и не показываются.
 *
 * Возвращает состояние и повтор: [ChannelScreen] не может сменить состояние сам,
 * поэтому «Повторить» поднимает счётчик попыток и перезапускает LaunchedEffect.
 */
@Composable
private fun rememberChannelKey(baseUrl: String): Pair<ChannelKey, () -> Unit> {
    var attempt by remember(baseUrl) { mutableStateOf(0) }
    var state by remember(baseUrl) { mutableStateOf<ChannelKey>(ChannelKey.Loading) }
    LaunchedEffect(baseUrl, attempt) {
        state = ChannelKey.Loading
        // Сеть/файл могут бросить исключение — тогда это такой же провал, как пустой ответ.
        val keyB64 = withContext(Dispatchers.IO) {
            runCatching {
                val cached = AppVault.readAppKey()
                if (!cached.isNullOrBlank()) cached
                else AppKeyBootstrap.fetch(baseUrl)?.let { fresh ->
                    Base64Codec.encode(fresh).also { AppVault.writeAppKey(it) }
                }
            }.getOrNull()
        }
        state = if (keyB64.isNullOrBlank()) {
            ChannelKey.Failed(t["channel.key.failed"])
        } else {
            ChannelKey.Ready(keyB64)
        }
    }
    return state to { attempt++ }
}

@Composable
private fun AppRoot(nativeGreeting: () -> String) {
    // Адрес и ключ не редактируются: они приходят из сборки и с сервера автоматически.
    val baseUrl = remember { normalizedEndpoint(defaultAppUrl().orEmpty()) ?: "https://azrael-lab.xyz/api/app/v1" }
    val (channel, retryChannelKey) = rememberChannelKey(baseUrl)
    var session by remember { mutableStateOf<Session?>(null) }
    val scope = rememberCoroutineScope()

    // Режим защиты (могила на устройстве либо серверное «отравление» канала).
    var threatMode by remember { mutableStateOf<ThreatMode?>(if (AppTrap.isProtected()) ThreatMode.TRAPPED else null) }
    val onThreat: (ThreatMode) -> Unit = { mode: ThreatMode -> threatMode = mode }

    val keyB64 = (channel as? ChannelKey.Ready)?.keyB64
    if (keyB64 == null) {
        ChannelScreen(channel, retryChannelKey)
        return
    }

    val currentThreat = threatMode
    if (currentThreat != null) {
        ProtectionScreen(
            mode = currentThreat,
            baseUrl = baseUrl,
            keyB64 = keyB64,
            onReleased = { threatMode = null }
        )
        return
    }

    val current = session
    if (current == null) {
        // Установка уже привязана и токен сохранён в AppVault → восстанавливаем сессию
        // по device-подписи (без повторного ввода ключа привязки).
        val bound = remember(baseUrl, keyB64) { makeClient(baseUrl, keyB64) }
        // Серверные маркеры (trap/release/poison) принимаются сразу, а не только после boot.
        SideEffect { bound.onThreat = onThreat }
        var resumeError by remember(baseUrl, keyB64) { mutableStateOf<String?>(null) }
        LaunchedEffect(baseUrl, keyB64) {
            if (!bound.isDeviceBound() || bound.sessionToken() == null) return@LaunchedEffect
            runCatching {
                withContext(Dispatchers.IO) { bound.homeBoot() }
            }.onSuccess { boot ->
                applyBootLang(boot)
                session = Session(bound, boot)
            }.onFailure { resumeError = errText(it) }
        }
        LoginScreen(
            client = bound,
            onThreat = onThreat,
            initStatus = resumeError,
            onLoggedIn = { session = it }
        )
    } else {
        var bootTick by remember { mutableStateOf(0) }
        val boot = current.boot
        MainShell(
            session = current,
            boot = boot,
            bootTick = bootTick,
            onThreat = onThreat,
            onRefreshBoot = {
                val c = current.client
                scope.launch {
                    runCatching { withContext(Dispatchers.IO) { c.homeBoot() } }
                        .onSuccess {
                            applyBootLang(it)
                            current.boot = it
                            bootTick++
                        }
                }
            },
            onLogout = {
                val c = current.client
                if (c.sessionToken() != null) {
                    runCatching { c.logout() }
                }
                session = null
            },
            nativeGreeting = nativeGreeting
        )
    }
}

// ---- Экран защиты (устройство отозвано / канал скомпрометирован) ----

@Composable
private fun ProtectionScreen(
    mode: ThreatMode,
    baseUrl: String,
    keyB64: String,
    onReleased: () -> Unit
) {
    var probe by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "⛔",
            style = MaterialTheme.typography.displayLarge,
            color = AzraelRose
        )
        Spacer(Modifier.height(16.dp))
        Text(
            if (mode == ThreatMode.TRAPPED) t["protection.trapped"] else t["protection.poisoned"],
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(Modifier.height(12.dp))
        Text(
            t["protection.desc1"] + t["protection.desc2"],
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.75f),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            t["protection.desc3"],
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        // Снятие могилы подтверждается сервером (x-azrael-release в подписанном конверте):
        // запрос идёт с allowProtected, иначе защищённый ответ снова вызвал бы блокировку.
        AccentButton(if (busy) t["protection.check.busy"] else t["protection.check"]) {
            if (busy) return@AccentButton
            busy = true
            probe = ""
            scope.launch {
                try {
                    val client = makeClient(baseUrl, keyB64)
                    val released = withContext(Dispatchers.IO) { client.probeRelease() }
                    probe = if (released) {
                        t["protection.released"]
                    } else {
                        t["protection.still"]
                    }
                    if (released) onReleased()
                } catch (e: Exception) {
                    probe = errText(e)
                } finally {
                    busy = false
                }
            }
        }
        if (probe.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(
                probe,
                style = MaterialTheme.typography.bodyMedium,
                color = if (probe.startsWith(t["protection.released.short"])) AzraelCyan else AzraelRose,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ---- Получение ключа канала: автоматически, без ввода пользователем ----

@Composable
private fun ChannelScreen(channel: ChannelKey, onRetry: () -> Unit) {
    val failed = channel as? ChannelKey.Failed
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            t["app.name"],
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(Modifier.height(14.dp))
        if (failed == null) {
            CircularProgressIndicator(color = AzraelCyan)
            Spacer(Modifier.height(14.dp))
            Text(
                t["channel.key.loading"],
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White
            )
        } else {
            Icon(Icons.Filled.Block, contentDescription = null, tint = AzraelRose, modifier = Modifier.height(44.dp))
            Spacer(Modifier.height(10.dp))
            Text(
                failed.reason,
                style = MaterialTheme.typography.bodyLarge,
                color = AzraelRose,
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            t["channel.key.loading.hint"],
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.65f),
            textAlign = TextAlign.Center
        )
        if (failed != null) {
            Spacer(Modifier.height(16.dp))
            // Повтор: ключ мог не загрузиться из-за сети — перезапускаем получение.
            AccentButton(t["channel.key.retry"], onClick = onRetry)
        }
    }
}

// ---- Вход и регистрация аккаунта (пароль, инвайт, пол, аватар) ----
// Оформление повторяет /login сайта (app/login/page.tsx): узкая карточка по центру,
// сегмент-переключатель вкладок, «стеклянные» поля, чипы выбора, аватар-круг
// и полноширинная кнопка действия. Фирменные цвета и фон — из ui/Theme.kt.

private enum class AuthTab { Login, Register }

@Composable
private fun LoginScreen(
    client: AppClient,
    onThreat: (ThreatMode) -> Unit,
    initStatus: String?,
    onLoggedIn: (Session) -> Unit
) {
    // rememberSaveable, а не remember: поворот экрана, смена темы/языка или смерть
    // процесса пересоздают Activity — форма входа не должна молча очищаться.
    var tab by rememberSaveable { mutableStateOf(AuthTab.Login) }
    var username by rememberSaveable { mutableStateOf(client.rememberedLogin()) }
    var password by rememberSaveable { mutableStateOf("") }
    var password2 by rememberSaveable { mutableStateOf("") }
    var deviceKey by rememberSaveable { mutableStateOf("") }
    var inviteCode by rememberSaveable { mutableStateOf("") }
    var displayName by rememberSaveable { mutableStateOf("") }
    var gender by rememberSaveable { mutableStateOf("") }
    // Аватар остаётся в remember: это base64 до 10 МиБ, в savedInstanceState он не влезет.
    var avatar by remember { mutableStateOf<PickedFile?>(null) }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var rememberDevice by rememberSaveable { mutableStateOf(client.hasRememberedAccount()) }
    var status by rememberSaveable { mutableStateOf(initStatus ?: "") }
    // Флаг «это ошибка»: раньше цвет определялся по русским префиксам, что не переводится.
    var statusError by rememberSaveable { mutableStateOf(initStatus != null && initStatus.isNotBlank()) }
    var busy by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun clearError() {
        if (statusError) {
            status = ""
            statusError = false
        }
    }

    val pickAvatar = rememberFilePicker(listOf("image/*"), MAX_AVATAR_BYTES) { res ->
        when (res) {
            is FilePick.Picked -> {
                avatar = res.file
                clearError()
            }
            FilePick.Cancelled -> Unit
            FilePick.Unavailable -> {
                status = t["file.picker.unavailable"]
                statusError = true
            }
        }
    }

    // Уже привязанная установка: вход по логину/паролю без ключа привязки —
    // подпись устройства уже подтверждена, ключ выдавать не нужно.
    val alreadyBound = remember { client.isDeviceBound() }

    fun fail(message: String) {
        status = message
        statusError = true
        busy = false
    }

    fun switchTab(next: AuthTab) {
        if (busy || tab == next) return
        tab = next
        status = ""
        statusError = false
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .widthIn(max = 460.dp)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BrandMark()
            Spacer(Modifier.height(18.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .glass(corner = 26.dp, borderColor = Color.White.copy(alpha = 0.12f))
                    .padding(horizontal = 18.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        if (tab == AuthTab.Login) t["login.tab.login"] else t["login.tab.register"],
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.95f)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (tab == AuthTab.Login) t["login.access.note"] else t["login.register.hint"],
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.45f),
                        textAlign = TextAlign.Center
                    )
                }

                AuthTabs(selected = tab, enabled = !busy, onSelect = ::switchTab)

                if (status.isNotBlank()) StatusBanner(status, statusError)

                if (tab == AuthTab.Login) {
                    AuthField(
                        value = username,
                        onValueChange = { username = it; clearError() },
                        label = t["login.username"],
                        accent = AzraelViolet,
                        leading = Icons.Filled.Person,
                        enabled = !busy
                    )
                    AuthField(
                        value = password,
                        onValueChange = { password = it; clearError() },
                        label = t["login.password.new"],
                        accent = AzraelViolet,
                        leading = Icons.Filled.Key,
                        isPassword = true,
                        visible = showPassword,
                        onToggleVisibility = { showPassword = !showPassword },
                        enabled = !busy
                    )
                    if (!alreadyBound) {
                        ProvisionKeyCard(
                            value = deviceKey,
                            onValueChange = { deviceKey = it; clearError() },
                            enabled = !busy
                        )
                    }
                    RememberDeviceRow(checked = rememberDevice, enabled = !busy) { rememberDevice = it }
                    PrimaryAuthButton(
                        label = t["login.submit"],
                        busyLabel = t["login.busy"],
                        busy = busy,
                        onClick = {
                            if (busy) return@PrimaryAuthButton
                            val name = username.trim()
                            if (name.isEmpty()) {
                                fail(t["login.need.username"])
                                return@PrimaryAuthButton
                            }
                            if (password.isEmpty()) {
                                fail(t["login.need.password"])
                                return@PrimaryAuthButton
                            }
                            val key = deviceKey.trim()
                            if (!alreadyBound && key.length < 8) {
                                fail(t["login.need.provision.short"])
                                return@PrimaryAuthButton
                            }
                            busy = true
                            status = t["login.busy"]
                            statusError = false
                            scope.launch {
                                try {
                                    val c = client
                                    c.onThreat = onThreat
                                    val boot = withContext(Dispatchers.IO) {
                                        if (alreadyBound) {
                                            c.authLogin(name, password, "")
                                        } else {
                                            c.authLogin(name, password, key)
                                            c.setRememberAccount(
                                                if (rememberDevice) name else null,
                                                if (rememberDevice) password else null
                                            )
                                        }
                                        c.homeBoot()
                                    }
                                    applyBootLang(boot)
                                    busy = false
                                    onLoggedIn(Session(c, boot))
                                } catch (e: Exception) {
                                    fail(errText(e))
                                }
                            }
                        }
                    )
                } else {
                    AuthField(
                        value = inviteCode,
                        // Коды на сайте всегда в верхнем регистре — приводим сразу.
                        onValueChange = { inviteCode = it.uppercase().take(11); clearError() },
                        label = t["login.invite.label"],
                        supporting = t["login.invite.hint"],
                        accent = AzraelCyan,
                        leading = Icons.Filled.Key,
                        spaced = true,
                        enabled = !busy
                    )
                    AuthField(
                        value = username,
                        onValueChange = { username = it; clearError() },
                        label = t["login.username"],
                        accent = AzraelViolet,
                        leading = Icons.Filled.Person,
                        enabled = !busy
                    )
                    AuthField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = t["login.displayName"],
                        accent = AzraelViolet,
                        leading = Icons.Filled.Person,
                        enabled = !busy
                    )
                    AuthField(
                        value = password,
                        onValueChange = { password = it; clearError() },
                        label = t["login.password.new"],
                        accent = AzraelViolet,
                        leading = Icons.Filled.Key,
                        isPassword = true,
                        visible = showPassword,
                        onToggleVisibility = { showPassword = !showPassword },
                        enabled = !busy
                    )
                    PasswordStrengthMeter(password)
                    AuthField(
                        value = password2,
                        onValueChange = { password2 = it; clearError() },
                        label = t["login.password.repeat"],
                        accent = AzraelViolet,
                        leading = Icons.Filled.Key,
                        isPassword = true,
                        visible = showPassword,
                        onToggleVisibility = { showPassword = !showPassword },
                        enabled = !busy
                    )
                    GenderChips(selected = gender, enabled = !busy) { picked ->
                        gender = if (gender == picked) "" else picked
                    }
                    AvatarPicker(
                        avatar = avatar,
                        enabled = !busy,
                        onPick = pickAvatar,
                        onRemove = { avatar = null }
                    )
                    PrimaryAuthButton(
                        label = t["login.submit.register"],
                        busyLabel = t["login.busy.register"],
                        busy = busy,
                        onClick = {
                            if (busy) return@PrimaryAuthButton
                            val invite = inviteCode.trim()
                            if (invite.isEmpty()) {
                                fail(t["login.need.invite"])
                                return@PrimaryAuthButton
                            }
                            val name = username.trim()
                            if (name.isEmpty()) {
                                fail(t["login.need.username"])
                                return@PrimaryAuthButton
                            }
                            if (password.length < 8) {
                                fail(t["login.password.short"])
                                return@PrimaryAuthButton
                            }
                            if (password != password2) {
                                fail(t["login.password.mismatch"])
                                return@PrimaryAuthButton
                            }
                            if (gender.isEmpty()) {
                                fail(t["login.need.gender"])
                                return@PrimaryAuthButton
                            }
                            if (avatar != null && avatar!!.base64.length > MAX_AVATAR_B64) {
                                fail(t["login.avatar.tooBig"])
                                return@PrimaryAuthButton
                            }
                            busy = true
                            status = t["login.busy.register"]
                            statusError = false
                            val picked = avatar
                            scope.launch {
                                try {
                                    val c = client
                                    c.onThreat = onThreat
                                    val boot = withContext(Dispatchers.IO) {
                                        c.authRegister(
                                            username = name,
                                            password = password,
                                            inviteCode = invite,
                                            gender = gender,
                                            avatarData = picked?.let { stripDataUrl(it.base64) },
                                            avatarMime = picked?.mime
                                        )
                                        displayName.trim().ifBlank { null }?.let {
                                            c.profileUpdate(displayName = it)
                                        }
                                        c.setRememberAccount(name, password)
                                        c.homeBoot()
                                    }
                                    applyBootLang(boot)
                                    busy = false
                                    onLoggedIn(Session(c, boot))
                                } catch (e: Exception) {
                                    fail(errText(e))
                                }
                            }
                        }
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Filled.Shield,
                    contentDescription = null,
                    tint = AzraelCyan.copy(alpha = 0.5f),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    t["channel.security"],
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.35f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/** Логотип-марка над карточкой входа: скруглённый квадрат с градиентом фирменных цветов. */
@Composable
private fun BrandMark() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(62.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.linearGradient(listOf(AzraelViolet, AzraelCyan, AzraelRose)))
                .border(1.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "A",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = Color(0xFF0B0B14)
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            t["app.name"],
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.95f),
            letterSpacing = 3.sp
        )
        Spacer(Modifier.height(6.dp))
        Text(
            t["login.intro1"] + t["login.intro2"],
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.42f),
            textAlign = TextAlign.Center
        )
    }
}

/** Сегмент-переключатель «Вход / Регистрация» — как на сайте. */
@Composable
private fun AuthTabs(selected: AuthTab, enabled: Boolean, onSelect: (AuthTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        listOf(AuthTab.Login to t["login.tab.login"], AuthTab.Register to t["login.tab.register"])
            .forEach { (item, label) ->
                val active = selected == item
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(11.dp))
                        .background(if (active) Color.White.copy(alpha = 0.15f) else Color.Transparent)
                        .clickable(enabled = enabled) { onSelect(item) }
                        .padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        color = if (active) Color.White.copy(alpha = 0.95f) else Color.White.copy(alpha = 0.4f)
                    )
                }
            }
    }
}

/** Поле входа: скруглённое «стекло», иконка слева, глаз для пароля, подсказка снизу. */
@Composable
private fun AuthField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    accent: Color,
    leading: ImageVector,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    isPassword: Boolean = false,
    visible: Boolean = false,
    onToggleVisibility: (() -> Unit)? = null,
    spaced: Boolean = false,
    maxLength: Int? = null,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = { raw -> onValueChange(if (maxLength == null) raw else raw.take(maxLength)) },
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        visualTransformation = if (isPassword && !visible) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        leadingIcon = {
            Icon(
                leading,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.35f),
                modifier = Modifier.size(18.dp)
            )
        },
        trailingIcon = if (onToggleVisibility == null) {
            null
        } else {
            {
                IconButton(
                    onClick = onToggleVisibility,
                    enabled = enabled,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = t["login.password.show"],
                        tint = Color.White.copy(alpha = if (visible) 0.75f else 0.4f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        supportingText = if (supporting == null) null else {
            { Text(supporting) }
        },
        textStyle = MaterialTheme.typography.bodyLarge.copy(letterSpacing = if (spaced) 2.sp else 0.4.sp),
        modifier = modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White.copy(alpha = 0.07f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.04f),
            disabledContainerColor = Color.White.copy(alpha = 0.02f),
            focusedBorderColor = accent.copy(alpha = 0.7f),
            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
            disabledBorderColor = Color.White.copy(alpha = 0.06f),
            focusedLabelColor = accent,
            unfocusedLabelColor = Color.White.copy(alpha = 0.42f),
            disabledLabelColor = Color.White.copy(alpha = 0.25f),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White.copy(alpha = 0.9f),
            disabledTextColor = Color.White.copy(alpha = 0.4f),
            cursorColor = accent,
            focusedLeadingIconColor = accent.copy(alpha = 0.9f),
            unfocusedLeadingIconColor = Color.White.copy(alpha = 0.35f),
            focusedSupportingTextColor = Color.White.copy(alpha = 0.35f),
            unfocusedSupportingTextColor = Color.White.copy(alpha = 0.35f)
        )
    )
}

/** Полоса надёжности пароля при регистрации — как PasswordStrength на сайте. */
@Composable
private fun PasswordStrengthMeter(password: String) {
    if (password.isEmpty()) return
    var score = 0
    if (password.length >= 8) score++
    if (password.length >= 12) score++
    if (password.any { it.isLowerCase() } && password.any { it.isUpperCase() }) score++
    if (password.any { it.isDigit() }) score++
    if (password.any { !it.isLetterOrDigit() }) score++
    val color = when {
        score < 2 -> Color(0xFFEF4444)
        score < 3 -> Color(0xFFF59E0B)
        score < 4 -> Color(0xFF22C55E)
        else -> Color(0xFF16A34A)
    }
    val label = when {
        score < 2 -> t["login.pw.weak"]
        score < 3 -> t["login.pw.medium"]
        score < 4 -> t["login.pw.good"]
        else -> t["login.pw.strong"]
    }
    val width by animateFloatAsState(
        targetValue = (score / 5f).coerceIn(0.2f, 1f),
        label = "auth-pw-strength"
    )
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color.White.copy(alpha = 0.08f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(width)
                    .height(4.dp)
                    .background(color, RoundedCornerShape(3.dp))
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = color.copy(alpha = 0.85f),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.End
        )
    }
}

/** Чипы выбора пола: свои фирменные цвета вместо буллетов в кнопках. */
@Composable
private fun GenderChips(selected: String, enabled: Boolean, onSelect: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Label(t["login.gender"])
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf(
                Triple("male", t["login.gender.male"], AzraelCyan),
                Triple("female", t["login.gender.female"], AzraelRose)
            ).forEach { (value, label, accent) ->
                val active = selected == value
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (active) accent.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.04f))
                        .border(
                            1.dp,
                            if (active) accent.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.08f),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable(enabled = enabled) { onSelect(value) }
                        .padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        color = if (active) accent else Color.White.copy(alpha = 0.45f)
                    )
                }
            }
        }
    }
}

/** Аватар: круг с превью, имя файла и крестик удаления (как на сайте). */
@Composable
private fun AvatarPicker(
    avatar: PickedFile?,
    enabled: Boolean,
    onPick: () -> Unit,
    onRemove: () -> Unit
) {
    val preview = remember(avatar?.base64) { avatar?.let { avatarBitmap(it.base64) } }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Label(t["login.avatar.pick"])
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.04f))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                .clickable(enabled = enabled, onClick = onPick)
                .padding(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.06f))
                    .border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (preview != null) {
                    Image(
                        bitmap = preview,
                        contentDescription = t["login.avatar.pick"],
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )
                } else {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    avatar?.name ?: t["login.avatar.hint"],
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = if (avatar == null) 0.45f else 0.85f),
                    maxLines = 1
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    if (avatar == null) t["login.avatar.pick"] else t["login.avatar.change"],
                    style = MaterialTheme.typography.labelMedium,
                    color = AzraelCyan.copy(alpha = 0.85f)
                )
            }
            if (avatar != null) {
                IconButton(
                    onClick = onRemove,
                    enabled = enabled,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = t["login.avatar.remove"],
                        tint = AzraelRose.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/** Блок ключа привязки для первого входа с другого устройства. */
@Composable
private fun ProvisionKeyCard(value: String, onValueChange: (String) -> Unit, enabled: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AzraelCyan.copy(alpha = 0.06f))
            .border(1.dp, AzraelCyan.copy(alpha = 0.18f), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.Key,
                contentDescription = null,
                tint = AzraelCyan.copy(alpha = 0.8f),
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                t["login.provision.label"],
                style = MaterialTheme.typography.labelLarge,
                color = AzraelCyan.copy(alpha = 0.95f)
            )
        }
        Text(
            t["login.provision.site1"] + t["login.provision.site2"],
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.5f)
        )
        AuthField(
            value = value,
            onValueChange = onValueChange,
            label = t["login.provision.name"],
            accent = AzraelCyan,
            leading = Icons.Filled.Key,
            spaced = true,
            enabled = enabled
        )
    }
}

/** Чекбокс «Запомнить устройство» — вся строка кликабельна. */
@Composable
private fun RememberDeviceRow(checked: Boolean, enabled: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = CheckboxDefaults.colors(
                checkedColor = AzraelCyan,
                uncheckedColor = Color.White.copy(alpha = 0.35f),
                checkmarkColor = Color(0xFF04212B)
            )
        )
        Spacer(Modifier.width(4.dp))
        Text(
            t["login.remember"],
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}

/** Статус/ошибка над кнопкой действия — как на сайте (красный текст под полями). */
@Composable
private fun StatusBanner(text: String, isError: Boolean) {
    val accent = if (isError) Color(0xFFF87171) else AzraelCyan
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(accent.copy(alpha = 0.1f))
            .border(1.dp, accent.copy(alpha = 0.28f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Icon(
            if (isError) Icons.Filled.Warning else Icons.Filled.Info,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.88f)
        )
    }
}

/** Главная кнопка: полная ширина, градиент фирменных цветов, спиннер в состоянии работы. */
@Composable
private fun PrimaryAuthButton(label: String, busyLabel: String, busy: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    val fill = if (busy) 0.04f else 0.07f
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(shape)
            .background(Color.White.copy(alpha = fill))
            .border(1.dp, Color.White.copy(alpha = 0.1f), shape)
            .clickable(enabled = !busy, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (busy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(10.dp))
            }
            Text(
                if (busy) busyLabel else label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = if (busy) 0.45f else 0.88f)
            )
        }
    }
}

/** Лимит аватара на клиенте: 10 МиБ файла (сервер отвергнет большее). */
private const val MAX_AVATAR_BYTES = 10L * 1024 * 1024

/** Запас под base64-размер того же файла (4/3) + заполнение, чтобы не упереться в 413. */
private const val MAX_AVATAR_B64 = 16L * 1024 * 1024
// ---- Главный экран: повтор /main сайта, табы из home.boot, роли ----

@Composable
private fun MainShell(
    session: Session,
    boot: JsonObject,
    bootTick: Int,
    onThreat: (ThreatMode) -> Unit,
    onRefreshBoot: () -> Unit,
    onLogout: () -> Unit,
    nativeGreeting: () -> String
) {
    val profile = parseProfile(boot.o("profile") ?: JsonObject(emptyMap()))
    val tabs = parseTabs(boot.o("tabConfig"))
    if (tabs.isEmpty()) {
        // Сервер не вернул ни одного доступного раздела (роль/конфиг) — не молчим, а объясняем.
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Filled.Block, contentDescription = null, tint = AzraelRose, modifier = Modifier.height(48.dp))
            Spacer(Modifier.height(12.dp))
            Text(
                t["main.noTabs"],
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(8.dp))
            Text(
                t("main.noTabs.hint", profile.role),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            AccentButton(t["action.refresh"]) { onRefreshBoot() }
            Spacer(Modifier.height(8.dp))
            AccentButton(t["action.logout"]) { onLogout() }
        }
        return
    }
    val defaultId = boot.o("tabConfig")?.s("defaultTab") ?: tabs.first().first
    var selectedId by remember(bootTick) { mutableStateOf(defaultId) }
    val selected = tabs.indexOfFirst { it.first == selectedId }.let { if (it < 0) 0 else it }
    val client = session.client

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val wide = maxWidth >= 700.dp

        if (wide) {
            Row(modifier = Modifier.fillMaxSize()) {
                SideRail(
                    tabs = tabs,
                    selected = selected,
                    onSelect = { selectedId = tabs[it].first },
                    onLogout = onLogout,
                    modifier = Modifier.fillMaxHeight()
                )
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(12.dp, 12.dp, 12.dp, 0.dp)) {
                        TriStateHeader(profile, onRefreshBoot)
                    }
                    MainContent(
                        tab = tabs[selected].first,
                        tabs = tabs,
                        client = client,
                        profile = profile,
                        onThreat = onThreat,
                        onRefreshBoot = onRefreshBoot,
                        onLogout = onLogout,
                        nativeGreeting = nativeGreeting,
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp)
                    )
                }
            }
        } else {
            Scaffold(
                containerColor = Color.Transparent,
                bottomBar = {
                    SurfaceGlass(shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)) {
                        NavigationBar(containerColor = Color.Transparent) {
                            tabs.forEachIndexed { i, tab ->
                                NavigationBarItem(
                                    selected = selected == i,
                                    onClick = { selectedId = tab.first },
                                    icon = { Icon(tabIcon(tab.first), contentDescription = tab.second) },
                                    label = { Text(tab.second, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                            NavigationBarItem(
                                selected = false,
                                onClick = onLogout,
                                icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = t["action.logout"]) },
                                label = { Text(t["action.logout"], style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            ) { padding ->
                Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                    Box(modifier = Modifier.fillMaxWidth().padding(12.dp, 8.dp)) { TriStateHeader(profile, onRefreshBoot) }
                    MainContent(
                        tab = tabs[selected].first,
                        tabs = tabs,
                        client = client,
                        profile = profile,
                        onThreat = onThreat,
                        onRefreshBoot = onRefreshBoot,
                        onLogout = onLogout,
                        nativeGreeting = nativeGreeting,
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TriStateHeader(profile: AppProfile, onRefreshBoot: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AvatarCircle(profile)
            Column {
                Text(
                    profile.displayName.ifBlank { profile.username },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "@${profile.username}${profile.role.uppercase().let { " · $it" }}",
                    style = MaterialTheme.typography.labelMedium,
                    color = roleColor(profile.role)
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            RoleBadge(profile.role)
            IconButton(onClick = onRefreshBoot) {
                Icon(Icons.Filled.Refresh, contentDescription = t["main.refresh"], tint = AzraelCyan)
            }
        }
    }
}

@Composable
private fun RoleBadge(role: String) {
    Box(
        modifier = Modifier
            .background(roleColor(role).copy(alpha = 0.22f), RoundedCornerShape(10.dp))
            .border(1.dp, roleColor(role).copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(role, color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AvatarCircle(profile: AppProfile) {
    val letter = (profile.displayName.ifBlank { profile.username }).take(1).uppercase()
    Box(
        modifier = Modifier
            .width(40.dp)
            .height(40.dp)
            .background(roleColor(profile.role).copy(alpha = 0.35f), CircleShape)
            .border(1.dp, AzraelCyan.copy(alpha = 0.4f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(letter, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

private fun tabIcon(id: String): ImageVector = when (id) {
    "admin" -> Icons.Filled.AdminPanelSettings
    "messages" -> Icons.AutoMirrored.Filled.Chat
    "shortener" -> Icons.Filled.Link
    "vpn_tab" -> Icons.Filled.VpnLock
    else -> Icons.Filled.Settings
}

/** Состояние автоудаления в виде локализованной строки: «выключено» или «N дн.». */
private fun autoDeleteState(days: Int?): String =
    if (days == null) t["profile.autoDelete.offState"] else t("unit.days", days)

@Composable
private fun RowScope.SideRail(
    tabs: List<Pair<String, String>>,
    selected: Int,
    onSelect: (Int) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    SurfaceGlass(shape = RoundedCornerShape(24.dp), modifier = modifier.padding(12.dp)) {
        NavigationRail(containerColor = Color.Transparent) {
            Spacer(Modifier.height(10.dp))
            Text(
                t["app.name"],
                modifier = Modifier.padding(horizontal = 8.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AzraelViolet
            )
            Spacer(Modifier.height(14.dp))
            tabs.forEachIndexed { i, tab ->
                NavigationRailItem(
                    selected = selected == i,
                    onClick = { onSelect(i) },
                    icon = { Icon(tabIcon(tab.first), contentDescription = tab.second) },
                    label = { Text(tab.second, style = MaterialTheme.typography.labelSmall) }
                )
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onLogout) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = t["action.logout"], tint = AzraelRose)
            }
        }
    }
}

@Composable
private fun MainContent(
    tab: String,
    tabs: List<Pair<String, String>>,
    client: AppClient,
    profile: AppProfile,
    onThreat: (ThreatMode) -> Unit,
    onRefreshBoot: () -> Unit,
    onLogout: () -> Unit,
    nativeGreeting: () -> String,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        when (tab) {
            "admin" -> AdminView(client, profile)
            "messages" -> ChatsView(client, profile)
            "shortener" -> ShortenerView(client)
            "vpn_tab" -> VpnView(client)
            "settings" -> SettingsView(
                client = client, profile = profile,
                onThreat = onThreat, onRefreshBoot = onRefreshBoot,
                onLogout = onLogout, nativeGreeting = nativeGreeting
            )
            else -> PlaceholderScreen(
                tab,
                t("tab.soon", tabs.firstOrNull { it.first == tab }?.second ?: tab)
            )
        }
    }
}

// ---- Админка (только владелец; таб сервер отдаёт только для owner) ----

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdminView(client: AppClient, profile: AppProfile) {
    var status by remember { mutableStateOf("") }
    var health by remember { mutableStateOf("") }
    var frozen by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    suspend fun loadFrozen() {
        try {
            val d = withContext(Dispatchers.IO) { client.adminFreezeList() }
            frozen = d.a("frozen").mapNotNull { runCatching { it.jsonObject }.getOrNull() }
        } catch (e: Exception) { status = errText(e) }
    }
    LaunchedEffect(Unit) { loadFrozen() }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text(t["tab.admin"], style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            t["admin.hint"],
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f)
        )
        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Label(t["profile.uid"])
                Text(profile.uid, style = MaterialTheme.typography.bodySmall, color = AzraelCyan)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AccentButton(t["admin.health"]) {
                        scope.launch {
                            status = "…"
                            health = withContext(Dispatchers.IO) {
                                runCatching { client.systemHealth().toString() }.getOrElse { errText(it) }
                            }
                            status = ""
                        }
                    }
                }
                Text(status, style = MaterialTheme.typography.bodyMedium, color = AzraelViolet)
                Text(health, style = MaterialTheme.typography.bodyMedium, color = AzraelCyan)
            }
        }
        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(t["admin.frozen"], style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { scope.launch { busy = true; loadFrozen(); busy = false } }) {
                        Icon(Icons.Filled.Refresh, contentDescription = t["action.refresh"], tint = AzraelCyan)
                    }
                }
                if (frozen.isEmpty()) {
                    Label(if (busy) t["common.loading"] else t["admin.frozen.none"])
                }
                frozen.forEach { u ->
                    val name = u.s("username") ?: "?"
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("@$name", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        Label(t("admin.frozen.state", u.s("frozen_at") ?: "—"))
                        AccentButton(t("admin.unfreeze", name)) {
                            scope.launch {
                                status = t("admin.unfreezing", name)
                                try {
                                    withContext(Dispatchers.IO) { client.adminUnfreeze(name) }
                                    status = t("admin.unfrozen", name)
                                    loadFrozen()
                                } catch (e: Exception) { status = errText(e) }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---- Сообщения: чаты, вложения, архив, автоудаление, AI-чат ----

private data class ChatRow(
    val id: Long,
    val name: String,
    val partnerId: Long,
    val lastText: String?,
    val unread: Int,
    val ts: String?
)

private fun parseChats(data: JsonObject): List<ChatRow> =
    data.a("items").mapNotNull { e ->
        val c = runCatching { e.jsonObject }.getOrNull() ?: return@mapNotNull null
        val p = c.o("partner")
        val name = p?.s("display_name")?.ifBlank { null } ?: p?.s("username") ?: "?"
        val pid = p?.l("id") ?: p?.l("uid") ?: 0L
        ChatRow(
            id = c.l("id") ?: 0L,
            name = name,
            partnerId = pid,
            lastText = c.o("last")?.s("text"),
            unread = c.i("unread") ?: 0,
            ts = c.s("ts")?.take(19)
        )
    }

private data class ChatMsg(
    val id: Long,
    val text: String?,
    val mine: Boolean,
    val fileName: String?,
    val fileToken: String?,
    val createdAt: String?
)

private fun parseMessages(data: JsonObject, myId: Long): List<ChatMsg> =
    data.a("messages").mapNotNull { e ->
        val m = runCatching { e.jsonObject }.getOrNull() ?: return@mapNotNull null
        val sender = m.l("sender_id") ?: 0L
        ChatMsg(
            id = m.l("id") ?: 0L,
            text = m.s("text"),
            mine = myId != 0L && sender == myId,
            fileName = m.s("file_name"),
            fileToken = m.s("file_token"),
            createdAt = m.s("created_at")?.take(19)
        )
    }

@Composable
private fun ChatsView(client: AppClient, profile: AppProfile) {
    var section by remember { mutableStateOf(0) }
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(t["tab.messages"], style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            if (section == 0) {
                AccentButton(t["chats.ai"]) { section = 1 }
            } else {
                AccentButton(t["chats.dialogs"]) { section = 0 }
            }
        }
        if (section == 0) {
            ChatsListSection(client, profile, Modifier.weight(1f).fillMaxWidth())
        } else {
            AiChatSection(client, Modifier.weight(1f).fillMaxWidth())
        }
    }
}

@Composable
private fun ChatsListSection(client: AppClient, profile: AppProfile, modifier: Modifier = Modifier) {
    var chats by remember { mutableStateOf<List<ChatRow>>(emptyList()) }
    var archived by remember { mutableStateOf<List<ChatRow>>(emptyList()) }
    var msgs by remember { mutableStateOf<List<ChatMsg>>(emptyList()) }
    var online by remember { mutableStateOf<Map<Long, Boolean>>(emptyMap()) }
    var openId by remember { mutableStateOf<Long?>(null) }
    var openName by remember { mutableStateOf("") }
    var input by remember { mutableStateOf("") }
    var pendingFile by remember { mutableStateOf<Pair<String, String>?>(null) }
    var status by remember { mutableStateOf("") }
    var days by remember { mutableStateOf<Int?>(null) }
    var busy by remember { mutableStateOf(false) }
    var searchQ by remember { mutableStateOf("") }
    var found by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    suspend fun reload() {
        val d = withContext(Dispatchers.IO) { client.chatsList() }
        val list = parseChats(d)
        chats = list
        runCatching {
            val p = withContext(Dispatchers.IO) { client.chatsPresence(list.map { it.partnerId }.filter { it != 0L }) }
            p.o("online")?.entries.orEmpty().mapNotNull { e ->
                e.key.toLongOrNull()?.let { it to e.value.boolValue() }
            }.toMap()
        }.onSuccess { online = it }
    }
    fun refresh() = scope.launch {
        status = t["common.loading"]
        try {
            reload()
            status = if (chats.isEmpty()) t["chats.none"] else t("chats.count", chats.size)
        } catch (e: Exception) {
            status = errText(e)
        }
    }
    LaunchedEffect(Unit) { refresh() }

    suspend fun openChat(chat: ChatRow) {
        val d = withContext(Dispatchers.IO) { client.chatsOpen(chat.id) }
        msgs = parseMessages(d, profile.id)
        openId = chat.id
        openName = chat.name
        days = withContext(Dispatchers.IO) { client.chatsAutodeleteGet(chat.id) }.i("days")
    }
    suspend fun refreshOpen() {
        val cid = openId ?: return
        msgs = parseMessages(withContext(Dispatchers.IO) { client.chatsMessages(cid, limit = 200) }, profile.id)
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier.verticalScroll(rememberScrollState())
    ) {
        Text(status, style = MaterialTheme.typography.bodyMedium, color = AzraelCyan)

        if (openId == null) {
            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Label(t["chats.new"])
                    OutlinedTextField(
                        value = searchQ,
                        onValueChange = { searchQ = it },
                        label = { Text(t["login.login.hint"]) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors(AzraelCyan)
                    )
                    AccentButton(if (searching) t["chats.searching"] else t["chats.find"]) {
                        val q = searchQ.trim()
                        if (q.length < 2) {
                            status = t["chats.find.hint"]
                            return@AccentButton
                        }
                        scope.launch {
                            searching = true
                            try {
                                val d = withContext(Dispatchers.IO) { client.searchUsers(q) }
                                found = d.a("users").mapNotNull { runCatching { it.jsonObject }.getOrNull() }
                                status = t("chats.found", found.size)
                            } catch (e: Exception) {
                                status = errText(e)
                            } finally {
                                searching = false
                            }
                        }
                    }
                    found.forEach { u ->
                        val uid = u.l("id") ?: return@forEach
                        Row(
                            modifier = Modifier.fillMaxWidth().clickableNoRipple {
                                scope.launch {
                                    try {
                                        val created = withContext(Dispatchers.IO) { client.chatsCreate(uid.toString()) }
                                        val cid = created.o("chat")?.l("id") ?: return@launch
                                        openChat(ChatRow(cid, u.s("username") ?: "?", uid, null, 0, null))
                                        found = emptyList(); searchQ = ""
                                        status = t["chats.open"]
                                    } catch (e: Exception) {
                                        status = errText(e)
                                    }
                                }
                            },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(u.s("display_name")?.ifBlank { null } ?: u.s("username") ?: "?", fontWeight = FontWeight.Bold)
                                Text("@${u.s("username") ?: "?"} · uid $uid", style = MaterialTheme.typography.labelSmall, color = AzraelCyan)
                            }
                            Icon(Icons.Filled.PersonAdd, contentDescription = null, tint = AzraelViolet)
                        }
                    }
                }
            }

            if (chats.isEmpty()) {
                GlassCard { Label(t["chats.none.hint"]) }
            }
            chats.forEach { c ->
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().clickableNoRipple {
                                scope.launch {
                                    try {
                                        openChat(c)
                                        status = ""
                                    } catch (e: Exception) {
                                        status = errText(e)
                                    }
                                }
                            },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(
                                        modifier = Modifier.height(8.dp).width(8.dp).background(
                                            if (online[c.partnerId] == true) Color(0xFF4CD964) else AzraelRose,
                                            CircleShape
                                        )
                                    )
                                    Text(c.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                                }
                                Text(
                                    c.lastText ?: t["empty"],
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.7f),
                                    maxLines = 1
                                )
                            }
                            if (c.unread > 0) {
                                Box(
                                    modifier = Modifier.background(AzraelRose, CircleShape).padding(horizontal = 8.dp, vertical = 2.dp)
                                ) { Text("${c.unread}", color = Color.White, style = MaterialTheme.typography.labelSmall) }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AccentButton(t["chats.archiveSection"]) {
                                scope.launch {
                                    try {
                                        withContext(Dispatchers.IO) { client.chatsArchive(c.id) }
                                        status = t["chats.archived"]
                                        refresh()
                                    } catch (e: Exception) { status = errText(e) }
                                }
                            }
                            AccentButton(t["chats.delete"]) {
                                scope.launch {
                                    try {
                                        withContext(Dispatchers.IO) { client.chatsDeleteChat(c.id) }
                                        status = t["chats.deleted"]
                                        refresh()
                                    } catch (e: Exception) { status = errText(e) }
                                }
                            }
                        }
                    }
                }
            }

            if (archived.isNotEmpty()) {
                Label(t("chats.archiveCount", archived.size))
            }
            archived.forEach { c ->
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(c.name, fontWeight = FontWeight.Bold)
                            Text(c.lastText ?: t["empty"], maxLines = 1, color = Color.White.copy(alpha = 0.6f))
                        }
                        AccentButton(t["chats.restore"]) {
                            scope.launch {
                                try {
                                    withContext(Dispatchers.IO) { client.chatsRestore(c.id) }
                                    archived = emptyList()
                                    status = t["chats.archiveRestore"]
                                    refresh()
                                } catch (e: Exception) { status = errText(e) }
                            }
                        }
                    }
                }
            }
            AccentButton(t["chats.showArchive"]) {
                scope.launch {
                    try {
                        val d = withContext(Dispatchers.IO) { client.chatsListArchived() }
                        archived = parseChats(d)
                        status = t("chats.archivedCount", archived.size)
                    } catch (e: Exception) { status = errText(e) }
                }
            }
        } else {
            val cid = openId ?: return@Column
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                AccentButton(t["vpn.backToList"]) {
                    openId = null; msgs = emptyList(); days = null; pendingFile = null
                }
                Text(openName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = { scope.launch { try { refreshOpen() } catch (e: Exception) { status = errText(e) } } }) {
                    Icon(Icons.Filled.Refresh, contentDescription = t["action.refresh"], tint = AzraelCyan)
                }
            }
            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Label(t["chats.autoDelete"])
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(null to t["off"], 1 to t["d1"], 7 to t["d7"], 30 to t["d30"]).forEach { (d, title) ->
                            AccentButton(if (d == days) "• $title" else title) {
                                scope.launch {
                                    try {
                                        withContext(Dispatchers.IO) { client.chatsAutodeleteSet(cid, d) }
                                        days = d
                                        status = if (d == null) t["chats.autoDelete.off"] else t("chats.autoDelete.days", d)
                                    } catch (e: Exception) { status = errText(e) }
                                }
                            }
                        }
                    }
                }
            }
            if (msgs.isEmpty()) {
                Label(t["chats.noMessages"])
            }
            msgs.forEach { m ->
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = if (m.mine) Alignment.CenterEnd else Alignment.CenterStart) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .background(
                                if (m.mine) AzraelViolet.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.08f),
                                RoundedCornerShape(16.dp)
                            )
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (!m.text.isNullOrBlank()) {
                            Text(m.text, style = MaterialTheme.typography.bodyMedium)
                        }
                        m.fileName?.let { name ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Filled.AttachFile, contentDescription = null, tint = AzraelCyan)
                                Text(name, style = MaterialTheme.typography.bodyMedium, color = AzraelCyan, maxLines = 1)
                            }
                            // Ссылка на вложение сервер отдаёт только для свежего fileToken.
                            m.fileToken?.let { token ->
                                AccentButton(t["short.copyLink"]) {
                                    scope.launch {
                                        val url = runCatching {
                                            withContext(Dispatchers.IO) { client.chatsFileUrl(token) }.s("url")
                                        }.getOrNull()
                                        if (url.isNullOrBlank()) status = t["vpn.sub.unavailable"] else {
                                            clipboard.setText(AnnotatedString(url))
                                            status = t["short.linkCopied"]
                                        }
                                    }
                                }
                            }
                        }
                        m.createdAt?.let {
                            Text(it, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.35f))
                        }
                        if (m.mine) {
                            AccentButton(t["chats.deleteMessage"]) {
                                scope.launch {
                                    try {
                                        withContext(Dispatchers.IO) { client.chatsDelete(m.id) }
                                        msgs = msgs.filterNot { it.id == m.id }
                                        status = t["chats.messageDeleted"]
                                    } catch (e: Exception) { status = errText(e) }
                                }
                            }
                        }
                    }
                }
            }
            pendingFile?.let { (token, name) ->
                GlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(t("chats.fileReady.msg", name), style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                        AccentButton(t["cancelVerb"]) { pendingFile = null }
                    }
                }
            }
            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { next ->
                            input = next
                            val cid2 = openId
                            if (next.isNotBlank() && cid2 != null) {
                                scope.launch {
                                    runCatching { withContext(Dispatchers.IO) { client.chatsTyping(cid2) } }
                                }
                            }
                        },
                        label = { Text(t["chats.message.hint"]) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors(AzraelViolet)
                    )
                    AccentButton(if (busy) t["chats.sending"] else t["action.send"]) {
                        if (busy) return@AccentButton
                        val text = input.trim()
                        val file = pendingFile
                        if (text.isEmpty() && file == null) {
                            status = t["chats.emptyMessage"]
                            return@AccentButton
                        }
                        busy = true
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) {
                                    client.chatsSend(cid, text = text.ifBlank { null }, fileToken = file?.first)
                                }
                                input = ""
                                pendingFile = null
                                refreshOpen()
                                status = ""
                            } catch (e: Exception) {
                                status = errText(e)
                            } finally {
                                busy = false
                            }
                        }
                    }
                    val pickAttach = rememberFilePicker(listOf("*/*"), 20L * 1024 * 1024) { res ->
                        when (res) {
                            is FilePick.Picked -> {
                                val picked = res.file
                                scope.launch {
                                    status = t("file.uploadBusy", picked.name)
                                    try {
                                        val d = withContext(Dispatchers.IO) {
                                            client.chatsFileUpload(picked.base64, picked.mime, picked.name)
                                        }
                                        pendingFile = (d.s("fileToken") ?: "") to picked.name
                                        status = t["chats.fileReady"]
                                    } catch (e: Exception) {
                                        status = errText(e)
                                    }
                                }
                            }
                            FilePick.Cancelled -> Unit
                            FilePick.Unavailable -> status = t["file.picker.unavailable"]
                        }
                    }
                    AccentButton(t["chats.attach"]) { pickAttach() }
                }
            }
        }
    }
}

@Composable
private fun AiChatSection(client: AppClient, modifier: Modifier = Modifier) {
    var chats by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var prompt by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }
    var activeId by remember { mutableStateOf<Long?>(null) }
    var model by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    fun refresh() = scope.launch {
        try {
            chats = withContext(Dispatchers.IO) { client.aiChatList() }
                .a("chats").mapNotNull { runCatching { it.jsonObject }.getOrNull() }
        } catch (e: Exception) { status = errText(e) }
    }
    LaunchedEffect(Unit) { refresh() }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier.verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(t["chats.ai"], style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            IconButton(onClick = { refresh() }) { Icon(Icons.Filled.Refresh, contentDescription = t["action.refresh"], tint = AzraelCyan) }
        }
        Text(status, style = MaterialTheme.typography.bodyMedium, color = AzraelCyan)
        if (chats.isEmpty()) {
            Label(t["ai.history.empty"])
        }
        chats.forEach { c ->
            val id = c.l("id") ?: return@forEach
            GlassCard(modifier = Modifier.fillMaxWidth().clickableNoRipple {
                activeId = if (activeId == id) null else id
                answer = c.s("last_message") ?: answer
            }) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(c.s("title") ?: t("ai.chatTitle", id), fontWeight = FontWeight.Bold)
                    Text(
                        c.s("last_message") ?: t["chats.noMessagesParens"],
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.65f),
                        maxLines = 2
                    )
                    Text(
                        t("chats.messagesCount", c.i("msg_count") ?: 0) +
                        (c.s("updated_at")?.let { " · $it" } ?: ""),
                        style = MaterialTheme.typography.labelSmall,
                        color = AzraelViolet
                    )
                }
            }
        }
        if (activeId != null) {
            AccentButton(t["ai.newChat"]) { activeId = null; answer = "" }
        }
        if (answer.isNotBlank()) {
            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(answer, style = MaterialTheme.typography.bodyMedium)
                    if (model.isNotBlank()) Label(t("ai.model", model))
                    AccentButton(t["ai.copyAnswer"]) {
                        clipboard.setText(AnnotatedString(answer))
                        status = t["ai.copied"]
                    }
                }
            }
        }
        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text(t["ai.hint"]) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors(AzraelCyan)
                )
                AccentButton(if (busy) t["ai.thinking"] else t["ai.ask"]) {
                    val text = prompt.trim()
                    if (text.isEmpty() || busy) return@AccentButton
                    busy = true
                    status = t["ai.request"]
                    scope.launch {
                        try {
                            val d = withContext(Dispatchers.IO) { client.aiChatSend(text, activeId) }
                            answer = d.s("answer") ?: t["ai.emptyAnswer"]
                            model = d.s("model") ?: ""
                            prompt = ""
                            status = t["ai.ready"]
                            refresh()
                        } catch (e: Exception) {
                            status = errText(e)
                        } finally {
                            busy = false
                        }
                    }
                }
            }
        }
    }
}

// ---- Сократитель ссылок: создание, QR, копирование, удаление ----

private data class ShortRow(val code: String, val url: String, val clicks: Int?, val created: String?)

private fun parseShortener(data: JsonObject): List<ShortRow> =
    data.a("items").mapNotNull { e ->
        val r = runCatching { e.jsonObject }.getOrNull() ?: return@mapNotNull null
        val code = r.s("code") ?: return@mapNotNull null
        ShortRow(code, r.s("url") ?: "", r.i("clicks"), r.s("created_at"))
    }

/** QR приходит с сервера как data:image/png;base64,… */
private fun qrBitmap(dataUrl: String?): ImageBitmap? {
    val raw = stripDataUrl(dataUrl ?: "").ifBlank { return null }
    return decodeImageBase64(raw)
}

/** Превью выбранного аватара: base64 из pickFile (с data:-префиксом или без). */
private fun avatarBitmap(base64: String): ImageBitmap? {
    val raw = stripDataUrl(base64).ifBlank { return null }
    return decodeImageBase64(raw)
}

@Composable
private fun ShortenerView(client: AppClient) {
    var rows by remember { mutableStateOf<List<ShortRow>>(emptyList()) }
    var url by remember { mutableStateOf("") }
    var custom by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var lastShort by remember { mutableStateOf("") }
    var qrCode by remember { mutableStateOf("") }
    var qrError by remember { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    fun refresh() {
        scope.launch {
            status = t["common.loading"]
            try {
                rows = parseShortener(withContext(Dispatchers.IO) { client.shortenerList() })
                status = if (rows.isEmpty()) t["short.none"] else t("short.count", rows.size)
            } catch (e: Exception) { status = errText(e) }
        }
    }
    LaunchedEffect(Unit) { refresh() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(t["tab.shortener"], style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            IconButton(onClick = { refresh() }) {
                Icon(Icons.Filled.Refresh, contentDescription = t["action.refresh"], tint = AzraelCyan)
            }
        }
        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text(t["short.urlLabel"]) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors(AzraelViolet)
                )
                OutlinedTextField(
                    value = custom,
                    onValueChange = { custom = it },
                    label = { Text(t["short.codeLabel"]) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors(AzraelCyan)
                )
                AccentButton(t["action.create"]) {
                    val u = url.trim()
                    if (u.isEmpty()) {
                        status = t["short.urlHint"]
                        return@AccentButton
                    }
                    scope.launch {
                        status = t["short.creating"]
                        try {
                            val d = withContext(Dispatchers.IO) {
                                client.shortenerCreate(u, custom.trim().ifBlank { null })
                            }
                            lastShort = d.s("shortUrl") ?: d.s("url") ?: u
                            qrCode = ""
                            qrError = ""
                            status = t("short.createdOne", lastShort)
                            url = ""
                            custom = ""
                            rows = parseShortener(withContext(Dispatchers.IO) { client.shortenerList() })
                        } catch (e: Exception) { status = errText(e) }
                    }
                }
                Text(status, style = MaterialTheme.typography.bodyMedium, color = AzraelCyan)
            }
        }

        if (lastShort.isNotBlank()) {
            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(lastShort, style = MaterialTheme.typography.titleMedium, color = AzraelCyan, fontWeight = FontWeight.Bold)
                    AccentButton(t["short.copyLink"]) {
                        clipboard.setText(AnnotatedString(lastShort))
                        status = t["short.linkCopied"]
                    }
                }
            }
        }

        rows.forEach { r ->
            val shortUrl = "https://azrael-lab.xyz/s/${r.code}"
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(shortUrl, style = MaterialTheme.typography.titleSmall, color = AzraelCyan, fontWeight = FontWeight.Bold)
                    Text(r.url, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f), maxLines = 2)
                    Label(t("short.clicks", r.clicks ?: 0) + (r.created?.let { " · $it" } ?: ""))
                    val shownQr = if (qrCode == r.code) qrBitmap(qrCode) else null
                    if (qrCode == r.code && shownQr == null) {
                        Text(qrError.ifBlank { t["short.qrUnavailable"] }, style = MaterialTheme.typography.bodySmall, color = AzraelRose)
                    }
                    shownQr?.let { bmp ->
                        Image(
                            bitmap = bmp,
                            contentDescription = "QR $shortUrl",
                            modifier = Modifier.height(180.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AccentButton(if (qrCode == r.code) t["short.qrHide"] else "QR") {
                            scope.launch {
                                if (qrCode == r.code) {
                                    qrCode = ""
                                    return@launch
                                }
                                try {
                                    val d = withContext(Dispatchers.IO) { client.shortenerQr(r.code) }
                                    qrCode = r.code
                                    qrError = if (qrBitmap(d.s("qr")) == null) t["short.qrEmpty"] else ""
                                    status = t("short.qr", shortUrl)
                                } catch (e: Exception) {
                                    qrError = errText(e)
                                    status = qrError
                                }
                            }
                        }
                        AccentButton(t["action.copy"]) {
                            clipboard.setText(AnnotatedString(shortUrl))
                            status = t["otp.copied"]
                        }
                        IconButton(onClick = {
                            scope.launch {
                                status = t["common.deleting"]
                                try {
                                    withContext(Dispatchers.IO) { client.shortenerDelete(r.code) }
                                    rows = parseShortener(withContext(Dispatchers.IO) { client.shortenerList() })
                                    status = t["short.deleted"]
                                } catch (e: Exception) { status = errText(e) }
                            }
                        }) { Icon(Icons.Filled.Delete, contentDescription = t["action.delete"], tint = AzraelRose) }
                    }
                }
            }
        }
    }
}

// ---- VPN: реальные данные free-VPN/AWG/Incy, ошибки видны ----

@Composable
private fun VpnView(client: AppClient) {
    var summary by remember { mutableStateOf<JsonObject?>(null) }
    var awg by remember { mutableStateOf<JsonObject?>(null) }
    var incys by remember { mutableStateOf<JsonObject?>(null) }
    var status by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var folder by remember { mutableStateOf("") }
    var servers by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    fun refreshAll() {
        scope.launch {
            status = t["common.loading"]
            error = ""
            try {
                summary = withContext(Dispatchers.IO) { client.vpnFreeSummary() }
            } catch (e: Exception) {
                error = t("vpn.summaryError", errText(e))
            }
            try {
                awg = withContext(Dispatchers.IO) { client.vpnAwgStatus() }
            } catch (e: Exception) {
                error = if (error.isBlank()) "A-WG: ${errText(e)}" else error
            }
            try {
                incys = withContext(Dispatchers.IO) { client.vpnIncysDownloads() }
            } catch (e: Exception) {
                error = if (error.isBlank()) "Incy: ${errText(e)}" else error
            }
            status = if (error.isBlank()) t["short.updated"] else ""
        }
    }
    LaunchedEffect(Unit) { refreshAll() }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("VPN", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            IconButton(onClick = { refreshAll() }) {
                Icon(Icons.Filled.Refresh, contentDescription = t["action.refresh"], tint = AzraelCyan)
            }
        }
        if (status.isNotBlank()) Text(status, style = MaterialTheme.typography.bodyMedium, color = AzraelCyan)
        if (error.isNotBlank()) {
            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(t["vpn.error"], style = MaterialTheme.typography.titleSmall, color = AzraelRose, fontWeight = FontWeight.Bold)
                    Text(error, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                }
            }
        }

        val s = summary
        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(t["vpn.freeServers"], style = MaterialTheme.typography.titleMedium)
                if (s == null) {
                    Label(t["vpn.summaryNotLoaded"])
                } else {
                    Text(t("vpn.totalAlive", s.i("total") ?: 0, s.i("alive") ?: 0), style = MaterialTheme.typography.bodyMedium, color = AzraelCyan)
                    val sub = s.s("subUrl") ?: ""
                    Label(t["vpn.sub.hint"])
                    OutlinedTextField(
                        value = sub,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("subUrl") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors(AzraelViolet)
                    )
                    if (sub.isNotBlank()) {
                        AccentButton(t["vpn.sub.copy"]) {
                            clipboard.setText(AnnotatedString(sub))
                            status = t["vpn.sub.copied"]
                        }
                    }
                }
            }
        }

        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(t["vpn.servers"], style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AccentButton(t["all"]) {
                        folder = ""
                        scope.launch {
                            runCatching { withContext(Dispatchers.IO) { client.vpnFreeServers(null) } }
                                .onSuccess { d -> servers = d.a("servers").mapNotNull { runCatching { it.jsonObject }.getOrNull() }; status = t("vpn.serversCount", servers.size) }
                                .onFailure { error = t("vpn.serversError", errText(it)) }
                        }
                    }
                    listOf("fast", "ok", "reality", "russia", "europe").forEach { f ->
                        AccentButton(f) {
                            folder = f
                            scope.launch {
                                runCatching { withContext(Dispatchers.IO) { client.vpnFreeServers(f) } }
                                    .onSuccess { d -> servers = d.a("servers").mapNotNull { runCatching { it.jsonObject }.getOrNull() }; status = "$f: ${servers.size}" }
                                    .onFailure { error = t("vpn.folderError", f, errText(it)) }
                            }
                        }
                    }
                }
                AccentButton(t["short.updateServer"]) {
                    scope.launch {
                        status = t["vpn.downloading"]
                        try {
                            val d = withContext(Dispatchers.IO) { client.vpnFreeRefresh() }
                            status = t("vpn.listUpdatedFull", d.i("alive") ?: 0, d.i("total") ?: 0)
                        } catch (e: Exception) {
                            status = t("short.updateFailed", errText(e))
                        }
                        refreshAll()
                    }
                }
                if (servers.isEmpty()) {
                    Label(t["short.listNotLoaded"])
                }
                servers.take(25).forEach { v ->
                    val alive = v.b("alive")
                    val link = v.s("link") ?: ""
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "${v.s("flag") ?: ""} ${v.s("name") ?: "?"} · ${v.s("host") ?: "?"}:${v.s("port") ?: "?"} " +
                                v.s("latency")?.let { "· ${t("vpn.ms", it)}" } ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (alive) Color(0xFF4CD964) else Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.weight(1f),
                            maxLines = 1
                        )
                        if (link.isNotBlank()) {
                            IconButton(onClick = {
                                clipboard.setText(AnnotatedString(link))
                                status = t["vpn.configCopied"]
                            }) { Icon(Icons.Filled.ContentCopy, contentDescription = t["vpn.copyConfig"], tint = AzraelCyan) }
                        }
                    }
                }
            }
        }

        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("A-WG (Amnezia WireGuard)", style = MaterialTheme.typography.titleMedium)
                val a = awg
                if (a == null) {
                    Label(t["vpn.awg.unknown"])
                } else {
                    Text(
                        if (a.b("awgEnabled")) t["on"] else t["notConfigured"],
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (a.b("awgEnabled")) AzraelCyan else Color.White.copy(alpha = 0.6f)
                    )
                    a.s("endpoint")?.let { Label("endpoint: $it") }
                    val conf = a.s("vpnConfig")
                    if (conf != null) {
                        OutlinedTextField(
                            value = conf,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("vpnConfig") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = fieldColors(AzraelCyan)
                        )
                        AccentButton(t["vpn.awg.copy"]) {
                            clipboard.setText(AnnotatedString(conf))
                            status = t["vpn.awg.copied"]
                        }
                    } else if (a.b("awgEnabled")) {
                        Label(t["vpn.awg.configHint"])
                    }
                }
            }
        }

        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(t["vpn.incy"], style = MaterialTheme.typography.titleMedium)
                val platforms = incys?.a("platforms") ?: emptyList()
                if (platforms.isEmpty()) {
                    Label(t["vpn.downloadsHint"])
                }
                platforms.forEach { e ->
                    val p = runCatching { e.jsonObject }.getOrNull() ?: return@forEach
                    Text("${p.s("name") ?: "?"} — ${p.s("filename") ?: "?"}", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                }
            }
        }
    }
}

// ---- Настройки: профиль, приватность, устройства, канал, шлюз, выход ----

@Composable
private fun SettingsView(
    client: AppClient,
    profile: AppProfile,
    onThreat: (ThreatMode) -> Unit,
    onRefreshBoot: () -> Unit,
    onLogout: () -> Unit,
    nativeGreeting: () -> String
) {
    var status by remember { mutableStateOf("") }
    // Профиль
    var displayName by remember { mutableStateOf(profile.displayName) }
    var tag by remember { mutableStateOf(profile.tag ?: "") }
    var gender by remember { mutableStateOf(profile.gender) }
    var avatarUrl by remember { mutableStateOf("") }
    // Приватность
    var hiddenFromSearch by remember { mutableStateOf(false) }
    var whoCanSearch by remember { mutableStateOf("all") }
    var whoCanWrite by remember { mutableStateOf("all") }
    var privacyLoaded by remember { mutableStateOf(false) }
    // Пароль
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    // Provision-ключ
    var provisionKey by remember { mutableStateOf("") }
    var provisionInfo by remember { mutableStateOf("") }
    // OTP
    var otpSecret by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    // Приглашения
    var myCode by remember { mutableStateOf("") }
    var invites by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var invitesAvailable by remember { mutableStateOf<Int?>(null) }
    // Устройства аккаунта
    var devices by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var devicesActive by remember { mutableStateOf(0) }
    var devicesMax by remember { mutableStateOf(7) }
    var devicesBusy by remember { mutableStateOf(false) }
    var revokeTarget by remember { mutableStateOf<JsonObject?>(null) }
    // Канал
    var l2Pub by remember { mutableStateOf(AppRuntime.srvXPubB64 ?: defaultAppSrvPubB64() ?: "") }
    var gatewayUrl by remember { mutableStateOf(AppRuntime.gatewayUrl ?: defaultGatewayUrl() ?: "") }
    var gwStatus by remember { mutableStateOf("") }
    // Автоудаление аккаунта
    var autoDeleteDays by remember { mutableStateOf<Int?>(profile.autoDeleteDays) }
    var deleteConfirm by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    suspend fun refreshDevices() {
        devicesBusy = true
        try {
            val d = withContext(Dispatchers.IO) { client.deviceList() }
            devices = d.a("devices").mapNotNull { runCatching { it.jsonObject }.getOrNull() }
            devicesActive = d.i("active") ?: 0
            devicesMax = d.i("max") ?: 7
        } catch (e: Exception) { status = errText(e) } finally { devicesBusy = false }
    }
    suspend fun refreshPrivacy() {
        try {
            val d = withContext(Dispatchers.IO) { client.profilePrivacyGet() }
            val p = d.o("privacy") ?: return
            hiddenFromSearch = p.b("hidden_from_search")
            whoCanSearch = p.s("privacy_who_can_search")?.takeIf { it.isNotBlank() } ?: "all"
            whoCanWrite = p.s("privacy_who_can_write")?.takeIf { it.isNotBlank() } ?: "all"
            privacyLoaded = true
        } catch (e: Exception) { status = t("privacy.error", errText(e)) }
    }
    suspend fun refreshInvites() {
        try {
            val d = withContext(Dispatchers.IO) { client.invitesList(true) }
            invites = d.a("items").mapNotNull { runCatching { it.jsonObject }.getOrNull() }
            invitesAvailable = d.i("available")
        } catch (e: Exception) { status = t("admin.invitesError", errText(e)) }
    }
    suspend fun refreshProvision() {
        try {
            val d = withContext(Dispatchers.IO) { client.provisionInfo() }
            provisionInfo = if (d.b("hasKey")) t["login.provision.ready"] else t["login.provision.none"]
        } catch (e: Exception) { status = "Provision: ${errText(e)}" }
    }
    LaunchedEffect(Unit) {
        refreshDevices()
        refreshPrivacy()
        refreshInvites()
        refreshProvision()
        runCatching {
            myCode = withContext(Dispatchers.IO) { client.invitesMyCode() }.s("code") ?: ""
        }
        runCatching {
            avatarUrl = withContext(Dispatchers.IO) { client.profileAvatarUrl() }.s("url") ?: ""
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text(t["settings.title"], style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(status, style = MaterialTheme.typography.bodyMedium, color = AzraelCyan)

        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(t["account.title"], style = MaterialTheme.typography.titleMedium)
                Text("@${profile.username} · ${profile.role}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Label("UID: ${profile.uid}")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AccentButton(t["channel.check.session"]) {
                        scope.launch {
                            status = "…"
                            try {
                                val d = withContext(Dispatchers.IO) { client.verify() }
                                status = t("channel.session.ok", (d.s("username") ?: "?").toString() + " · " + (d.s("role") ?: "?").toString())
                            } catch (e: Exception) { status = errText(e) }
                        }
                    }
                    AccentButton(t["settings.refreshAccount"]) { onRefreshBoot() }
                }
                Label(t("settings.platform", platformName(), nativeGreeting()))
            }
        }

        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(t["settings.lang"], style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppLang.entries.forEach { lang ->
                        val title = lang.code.uppercase()
                        AccentButton(if (I18n.lang == lang) "• $title" else title) {
                            scope.launch {
                                val prev = I18n.lang
                                I18n.set(lang)
                                try {
                                    withContext(Dispatchers.IO) { client.profileSetLang(lang.code) }
                                    status = t("settings.langSaved", title)
                                } catch (e: Exception) {
                                    I18n.set(prev)
                                    status = errText(e)
                                }
                            }
                        }
                    }
                }
            }
        }

        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(t["profile.title"], style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AvatarCircle(profile)
                    Column {
                        Text("@${profile.username}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Label("TG: " + if (profile.tgBound) t("tg.bound", profile.tgUsername ?: "?") else t["notBound"])
                    }
                }
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text(t["profile.name"]) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors(AzraelViolet)
                )
                OutlinedTextField(
                    value = tag,
                    onValueChange = { tag = it },
                    label = { Text(t["profile.tag"]) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors(AzraelViolet)
                )
                OutlinedTextField(
                    value = gender,
                    onValueChange = { gender = it },
                    label = { Text(t["profile.gender"]) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors(AzraelViolet)
                )
                AccentButton(t["profile.save"]) {
                    scope.launch {
                        status = t["common.saving"]
                        try {
                            withContext(Dispatchers.IO) {
                                client.profileUpdate(
                                    displayName = displayName.trim().ifBlank { null },
                                    tag = tag.trim().ifBlank { null },
                                    gender = gender.trim().ifBlank { null }
                                )
                            }
                            status = t["profile.saved"]
                            onRefreshBoot()
                        } catch (e: Exception) { status = errText(e) }
                    }
                }
                if (avatarUrl.isNotBlank()) {
                    Label(t("profile.avatar.info", avatarUrl))
                }
                val pickAvatarUpload = rememberFilePicker(listOf("image/*"), 5L * 1024 * 1024) { res ->
                    when (res) {
                        is FilePick.Picked -> {
                            val picked = res.file
                            scope.launch {
                                status = t["profile.avatar.busy"]
                                try {
                                    withContext(Dispatchers.IO) {
                                        client.profileAvatarSet(stripDataUrl(picked.base64), picked.mime)
                                    }
                                    avatarUrl = withContext(Dispatchers.IO) { client.profileAvatarUrl() }.s("url") ?: avatarUrl
                                    status = t["profile.avatar.updated"]
                                } catch (e: Exception) { status = errText(e) }
                            }
                        }
                        FilePick.Cancelled -> Unit
                        FilePick.Unavailable -> status = t["file.picker.unavailable"]
                    }
                }
                AccentButton(t["profile.avatar.upload"]) { pickAvatarUpload() }
            }
        }

        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(t["privacy.title"], style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = hiddenFromSearch,
                        onCheckedChange = { hiddenFromSearch = it },
                        colors = CheckboxDefaults.colors(checkedColor = AzraelCyan)
                    )
                    Text(t["privacy.hidden"], style = MaterialTheme.typography.bodyMedium)
                }
                Label(t["privacy.whoSearch"])
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("all" to t["all.lower"], "inviter" to t["chats.allInviting"], "invited" to t["chats.allInvited"], "custom" to t["chats.myList"]).forEach { (v, t) ->
                        AccentButton(if (whoCanSearch == v) "• $t" else t) { whoCanSearch = v }
                    }
                }
                Label(t["privacy.whoWrite"])
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("all" to t["all.lower"], "inviter" to t["chats.allInviting"], "invited" to t["chats.allInvited"], "custom" to t["chats.myList"]).forEach { (v, t) ->
                        AccentButton(if (whoCanWrite == v) "• $t" else t) { whoCanWrite = v }
                    }
                }
                AccentButton(if (privacyLoaded) t["privacy.save"] else t["privacy.loadSave"]) {
                    scope.launch {
                        status = t["common.saving"]
                        try {
                            withContext(Dispatchers.IO) {
                                client.profilePrivacySet(
                                    mapOf(
                                        "hiddenFromSearch" to hiddenFromSearch,
                                        "whoCanSearch" to whoCanSearch,
                                        "whoCanWrite" to whoCanWrite
                                    )
                                )
                            }
                            status = t["privacy.saved"]
                        } catch (e: Exception) { status = errText(e) }
                    }
                }
            }
        }

        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(t["profile.autoDelete"], style = MaterialTheme.typography.titleMedium)
                Text(
                    t["profile.autoDelete.hint1"] +
                        t("profile.autoDelete.now", autoDeleteState(autoDeleteDays)),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(null to t["off"], 30 to t["d30s"], 90 to t["d90"], 365 to t["d365"]).forEach { (d, lbl) ->
                        AccentButton(if (autoDeleteDays == d) "• $lbl" else lbl) {
                            scope.launch {
                                try {
                                    withContext(Dispatchers.IO) { client.profileAutoDelete(d) }
                                    autoDeleteDays = d
                                    status = if (d == null) t["chats.autoDelete.off"]
                                    else t("chats.autoDelete.in", d)
                                } catch (e: Exception) { status = errText(e) }
                            }
                        }
                    }
                }
            }
        }

        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(t["login.password"], style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = oldPassword,
                    onValueChange = { oldPassword = it },
                    label = { Text(t["login.currentPassword"]) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors(AzraelViolet)
                )
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text(t["login.newPassword"]) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors(AzraelViolet)
                )
                AccentButton(t["login.changePassword"]) {
                    if (oldPassword.isEmpty() || newPassword.length < 8) {
                        status = t["login.need.passwords"]
                        return@AccentButton
                    }
                    scope.launch {
                        status = t["common.changing"]
                        try {
                            withContext(Dispatchers.IO) { client.changePassword(oldPassword, newPassword) }
                            oldPassword = ""; newPassword = ""
                            status = t["login.passwordChanged"]
                        } catch (e: Exception) { status = errText(e) }
                    }
                }
                AccentButton(t["channel.revokeSession"]) {
                    scope.launch {
                        try {
                            withContext(Dispatchers.IO) { client.revokeSession() }
                            status = t["channel.session.revoked"]
                            onLogout()
                        } catch (e: Exception) { status = errText(e) }
                    }
                }
                Label(provisionInfo)
                AccentButton(t["devices.provision.show"]) {
                    scope.launch {
                        status = "…"
                        try {
                            val d = withContext(Dispatchers.IO) { client.provisionKey() }
                            val key = d.s("key")
                            provisionKey = key ?: ""
                            status = if (key != null) t["devices.provision.once"] else t["devices.provision.issued"]
                            refreshProvision()
                        } catch (e: Exception) { status = errText(e) }
                    }
                }
                if (provisionKey.isNotBlank()) {
                    GlassCard {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(provisionKey, color = AzraelCyan, fontWeight = FontWeight.Bold)
                            AccentButton(t["otp.copy"]) {
                                clipboard.setText(AnnotatedString(provisionKey))
                                status = t["devices.key.copied"]
                            }
                        }
                    }
                }
                AccentButton(t["devices.provision.regen"]) {
                    scope.launch {
                        status = "…"
                        try {
                            val d = withContext(Dispatchers.IO) { client.provisionRegenerate() }
                            provisionKey = d.s("key") ?: ""
                            status = t["channel.key.old"]
                            refreshProvision()
                        } catch (e: Exception) { status = errText(e) }
                    }
                }
                Label(t("otp.secret", if (otpSecret.isBlank()) t["otp.secret.empty"] else otpSecret))
                OutlinedTextField(
                    value = otpCode,
                    onValueChange = { otpCode = it },
                    label = { Text(t["otp.code"]) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors(AzraelCyan)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AccentButton(t["otp.issue"]) {
                        scope.launch {
                            try {
                                val d = withContext(Dispatchers.IO) { client.otpGenerateSecret() }
                                otpSecret = d.s("secret") ?: ""
                                status = t["otp.got"]
                            } catch (e: Exception) { status = errText(e) }
                        }
                    }
                    AccentButton(t["devices.checkCode"]) {
                        val code = otpCode.trim()
                        if (code.isEmpty()) {
                            status = t["otp.enter"]
                            return@AccentButton
                        }
                        scope.launch {
                            try {
                                val d = withContext(Dispatchers.IO) { client.otpValidate(code) }
                                status = if (d.b("ok")) t["otp.accepted"] else t["otp.rejected"]
                            } catch (e: Exception) { status = errText(e) }
                        }
                    }
                }
            }
        }

        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(t["admin.invites"], style = MaterialTheme.typography.titleMedium)
                if (myCode.isNotBlank()) {
                    Text(t("admin.myCode", myCode), color = AzraelCyan, fontWeight = FontWeight.Bold)
                    AccentButton(t["admin.copyMyCode"]) {
                        clipboard.setText(AnnotatedString(myCode))
                        status = t["admin.codeCopied"]
                    }
                }
                AccentButton(if (invitesAvailable == null) t["admin.generate"] else t("admin.generateFree", invitesAvailable)) {
                    scope.launch {
                        status = t["admin.generating"]
                        try {
                            val d = withContext(Dispatchers.IO) { client.invitesGenerate() }
                            invitesAvailable = d.i("available")
                            status = t("admin.createdCodes", d.a("codes").size)
                            refreshInvites()
                        } catch (e: Exception) { status = errText(e) }
                    }
                }
                if (invites.isEmpty()) Label(t["admin.activeNone"])
                invites.forEach { it0 ->
                    val id = it0.l("id") ?: return@forEach
                    val code = it0.s("code") ?: "?"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(code + (it0.s("used_by")?.let { t("admin.usedBy", it) } ?: ""), style = MaterialTheme.typography.bodyMedium)
                        Row {
                            IconButton(onClick = {
                                clipboard.setText(AnnotatedString(code))
                                status = t["admin.codeCopied"]
                            }) { Icon(Icons.Filled.ContentCopy, contentDescription = t["action.copy"], tint = AzraelCyan) }
                            IconButton(onClick = {
                                scope.launch {
                                    try {
                                        withContext(Dispatchers.IO) { client.invitesArchive(id) }
                                        status = t["admin.codeArchive"]
                                        refreshInvites()
                                    } catch (e: Exception) { status = errText(e) }
                                }
                            }) { Icon(Icons.Filled.Archive, contentDescription = t["chats.archive"], tint = AzraelViolet) }
                        }
                    }
                }
                AccentButton(t["admin.showCodeArchive"]) {
                    scope.launch {
                        try {
                            val d = withContext(Dispatchers.IO) { client.invitesList(false) }
                            invites = d.a("items").mapNotNull { runCatching { it.jsonObject }.getOrNull() }
                            status = t("admin.archivedCodes", invites.size)
                        } catch (e: Exception) { status = errText(e) }
                    }
                }
            }
        }

        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(t["devices.title"], style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "$devicesActive / $devicesMax",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (devicesActive >= devicesMax) AzraelRose else AzraelCyan
                )
                Text(
                    t("devices.limit1", devicesMax) +
                        t["devices.limit2"],
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
                if (devices.isEmpty()) {
                    Label(if (devicesBusy) t["common.loading"] else t["short.listNotLoaded"])
                }
                devices.forEach { d ->
                    val devId = d.s("devId").orEmpty()
                    val st = d.s("status") ?: "?"
                    val title = d.s("label")?.takeIf { it.isNotBlank() }
                        ?: d.s("platform")?.takeIf { it.isNotBlank() }
                        ?: t["devices.title.col"]
                    val isCurrent = devId == client.deviceId()
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            Text(
                                when (st) {
                                    "active" -> t["active.lower"]
                                    "revoked" -> t["devices.revoked.lower"]
                                    "frozen" -> t["admin.frozen.lower"]
                                    else -> st
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = if (st == "active") AzraelCyan else AzraelRose
                            )
                            if (isCurrent) {
                                Text(t["devices.thisDevice.lower"], style = MaterialTheme.typography.labelMedium, color = AzraelViolet)
                            }
                        }
                        Label(
                            t("devices.short", devId.take(12), d.s("lastSeen") ?: "—") +
                                t("channel.rotations", d.i("rotations") ?: 0)
                        )
                    }
                    if (st == "active") {
                        if (revokeTarget?.s("devId") == devId) {
                            Text(
                                if (isCurrent) t["devices.thisDevice"]
                                else t("devices.revoke.warn", title),
                                style = MaterialTheme.typography.bodySmall,
                                color = AzraelRose
                            )
                            AccentButton(t["devices.revoke.yes"]) {
                                scope.launch {
                                    status = t["devices.revoking"]
                                    try {
                                        withContext(Dispatchers.IO) { client.deviceRevoke(devId) }
                                        revokeTarget = null
                                        if (isCurrent) {
                                            devices = emptyList()
                                            status = t["devices.current.revoked"]
                                        } else {
                                            refreshDevices()
                                            status = t["devices.revoked"]
                                        }
                                    } catch (e: Exception) { status = errText(e) }
                                }
                            }
                            AccentButton(t["action.cancel"]) { revokeTarget = null }
                        } else {
                            AccentButton(t["devices.revoke"]) { revokeTarget = d }
                        }
                    }
                }
                AccentButton(if (devicesBusy) t["short.updating"] else t["short.update"]) {
                    scope.launch { refreshDevices() }
                }
                OutlinedTextField(
                    value = provisionKey,
                    onValueChange = { provisionKey = it },
                    label = { Text(t["login.provision.label"]) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors(AzraelCyan)
                )
                AccentButton(t["devices.check"]) {
                    scope.launch {
                        status = "…"
                        try {
                            val d = withContext(Dispatchers.IO) { client.deviceStatus() }
                            status = t("devices.statusRow", d.s("status") ?: "?")
                        } catch (e: Exception) { status = errText(e) }
                    }
                }
                if (client.isL2Available) {
                    AccentButton(t["devices.rotate"]) {
                        scope.launch {
                            status = t["channel.rotating"]
                            try {
                                withContext(Dispatchers.IO) { client.rotateDeviceKey() }
                                status = t["devices.provision.ready"]
                                refreshDevices()
                            } catch (e: Exception) { status = errText(e) }
                        }
                    }
                } else {
                    Text(
                        t["channel.l2.unavailable"],
                        style = MaterialTheme.typography.bodySmall,
                        color = AzraelRose
                    )
                }
            }
        }

        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(t["channel.endpoint.label"], style = MaterialTheme.typography.titleMedium)
                // Адрес и ключ канала не показываются и не редактируются: ключ приходит
                // с сервера автоматически (AppVault), пользователь его не знает.
                Text(
                    t["channel.key.ready"],
                    style = MaterialTheme.typography.bodySmall,
                    color = AzraelCyan
                )
                AccentButton(t["channel.check.health"]) {
                    scope.launch {
                        status = "…"
                        runCatching { withContext(Dispatchers.IO) { client.systemHealth() } }
                            .onSuccess { status = t("channel.health.responds", it.toString().take(120)) }
                            .onFailure { status = errText(it) }
                    }
                }
                OutlinedTextField(
                    value = l2Pub,
                    onValueChange = { l2Pub = it },
                    label = { Text(t["channel.l2.key"]) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors(AzraelCyan)
                )
                AccentButton(t["channel.l2.apply"]) {
                    AppRuntime.srvXPubB64 = l2Pub.trim().ifBlank { null }
                    status = t["channel.l2.applied"]
                }
                Label(t["profile.autoDelete.hint2"])
            }
        }

        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(t["channel.l2.gateway"], style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = gatewayUrl,
                    onValueChange = { gatewayUrl = it },
                    label = { Text("https://…/api/gateway/v1") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors(AzraelCyan)
                )
                AccentButton(t["vpn.connect"]) {
                    val url = normalizedEndpoint(gatewayUrl)
                    if (url == null) {
                        gwStatus = t["login.need.https"]
                        return@AccentButton
                    }
                    AppRuntime.gatewayUrl = url
                    scope.launch {
                        gwStatus = t["vpn.connecting"]
                        try {
                            val gc = GatewayClient(url)
                            val reply = withContext(Dispatchers.IO) { gc.connect(client.sessionToken()) }
                            val statusText = withContext(Dispatchers.IO) { gc.sayStatus() }
                            gwStatus = "handshake=" + (if (reply != null) t["gateway.ok"] else t["noResponse"]) +
                                " · " + t["gateway.status"] + "=" + statusText
                        } catch (e: Exception) {
                            gwStatus = t("admin.gatewayError", errText(e))
                        }
                    }
                }
                if (gwStatus.isNotBlank()) Label(gwStatus)
                Label(t["channel.l2.privacy"])
            }
        }

        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(t["channel.title"], style = MaterialTheme.typography.titleMedium)
                Text(
                    t("tab.role", profile.role) +
                        t["login.protect"],
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
                AccentButton(t["channel.logout"]) { onLogout() }
                Label(t["profile.delete.warn"])
                if (deleteConfirm) {
                    Text(t("profile.delete.confirm", profile.username), color = AzraelRose)
                    AccentButton(t["profile.delete.yes"]) {
                        scope.launch {
                            status = t["common.deleting"]
                            try {
                                withContext(Dispatchers.IO) { client.profileDelete() }
                                status = t["profile.deleted"]
                                onLogout()
                            } catch (e: Exception) { status = errText(e) }
                        }
                    }
                    AccentButton(t["action.cancel"]) { deleteConfirm = false }
                } else {
                    AccentButton(t["profile.delete"]) { deleteConfirm = true }
                }
            }
        }
    }
}

// ---- Общие элементы ----

private fun makeClient(baseUrl: String, keyB64: String): AppClient {
    val url = normalizedEndpoint(baseUrl)
        ?: normalizedEndpoint(defaultAppUrl().orEmpty())
        ?: "https://azrael-lab.xyz/api/app/v1"
    return AppClient(url, AppSecure.appKeyFromB64(keyB64))
}

@Composable
private fun fieldColors(accent: Color) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = accent,
    unfocusedBorderColor = Color.White.copy(alpha = 0.25f)
)

@Composable
private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = this.clickable(
    interactionSource = remember { MutableInteractionSource() },
    indication = null
) { onClick() }

@Composable
private fun SurfaceGlass(shape: RoundedCornerShape, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier = modifier.glass()) {
        content()
    }
}

@Composable
private fun GlassCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier = modifier.glass().padding(18.dp)) {
        content()
    }
}

@Composable
private fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.6f))
}

@Composable
private fun AccentButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = AzraelViolet.copy(alpha = 0.3f),
            contentColor = Color.White
        )
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun PlaceholderScreen(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f))
    }
}