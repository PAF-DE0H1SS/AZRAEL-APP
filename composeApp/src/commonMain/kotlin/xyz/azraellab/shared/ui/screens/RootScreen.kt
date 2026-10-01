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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Palette
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
import xyz.azraellab.shared.core.api.AppInstall
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
import xyz.azraellab.shared.data.configurable.ActionSpec
import xyz.azraellab.shared.data.configurable.ChoiceOption
import xyz.azraellab.shared.data.configurable.ChoiceSpec
import xyz.azraellab.shared.data.configurable.ConfigGroup
import xyz.azraellab.shared.data.configurable.ConfigurableList
import xyz.azraellab.shared.data.configurable.FieldSpec
import xyz.azraellab.shared.data.configurable.ToggleSpec
import xyz.azraellab.shared.ui.AppThemeRoot
import xyz.azraellab.shared.ui.theme.AppThemeMode
import xyz.azraellab.shared.ui.theme.AzraelThemeState
import xyz.azraellab.shared.ui.GlassBackground
import xyz.azraellab.shared.ui.glass
import xyz.azraellab.shared.ui.components.AzraelButton
import xyz.azraellab.shared.ui.components.AzraelButtonTone
import xyz.azraellab.shared.ui.components.AzraelCard
import xyz.azraellab.shared.ui.components.AzraelChoiceChip
import xyz.azraellab.shared.ui.components.AzraelListTile
import xyz.azraellab.shared.ui.components.AzraelTextField
import xyz.azraellab.shared.ui.nav.AzraelDetailLayout
import xyz.azraellab.shared.ui.nav.AzraelNavScaffold
import xyz.azraellab.shared.ui.nav.Destination
import xyz.azraellab.shared.ui.nav.DetailKind
import xyz.azraellab.shared.ui.nav.NavHost
import xyz.azraellab.shared.ui.nav.Navigator
import xyz.azraellab.shared.ui.nav.ServerTab
import xyz.azraellab.shared.ui.nav.ScrollPositions
import xyz.azraellab.shared.ui.nav.TabConfig
import xyz.azraellab.shared.ui.nav.TabSpec
import xyz.azraellab.shared.ui.nav.buildTabConfig
import xyz.azraellab.shared.ui.nav.rememberNavState
import xyz.azraellab.shared.ui.nav.rememberScrollPositions
import xyz.azraellab.shared.ui.nav.rememberSectionScroll
import xyz.azraellab.shared.ui.nav.roomScrollKey
import xyz.azraellab.shared.ui.nav.isWideLayout
import xyz.azraellab.shared.ui.nav.selectedTabId
import xyz.azraellab.shared.ui.theme.AzraelSpace
import xyz.azraellab.shared.data.UiState
import xyz.azraellab.shared.data.model.DeviceDto
import xyz.azraellab.shared.data.model.DeviceListDto
import xyz.azraellab.shared.data.vm.AdminViewModel
import xyz.azraellab.shared.data.vm.ChatsViewModel
import xyz.azraellab.shared.data.vm.SettingsViewModel
import xyz.azraellab.shared.data.vm.ShortenerViewModel
import xyz.azraellab.shared.data.vm.VpnViewModel
import xyz.azraellab.shared.ui.vm.collectAsUiState
import xyz.azraellab.shared.ui.vm.rememberViewModel


// Активная сессия: клиент кастомного API + данные главного экрана (home.boot == /main).
internal class Session(val client: AppClient, boot: JsonObject) {
    var boot: JsonObject = boot
}

/** Язык аккаунта из home.boot: сервер - источник истины, локальный файл - только кэш. */
internal fun applyBootLang(boot: JsonObject) {
    val lang = boot.s("lang") ?: boot.o("profile")?.s("lang")
    I18n.applyServer(lang)
}

/** Состояние автоматического получения ключа канала: пользователь его не вводит. */
internal sealed interface ChannelKey {
    data object Loading : ChannelKey
    data class Ready(val keyB64: String) : ChannelKey
    data class Failed(val reason: String) : ChannelKey
}

/**
 * Ключ канала для адреса API: из AppVault, иначе одноразовый запрос
 * `GET /api/app/bootstrap?devId=…`. Адрес и ключ не вводятся и не показываются.
 *
 * Сервер отдаёт ключ, выведенный из devId этой установки, поэтому ключи установок
 * не совпадают, а общий мастер-ключ не покидает сервер. devId берётся из ключей
 * установки, которые создаются при первом запуске и не меняются при ротации.
 *
 * Возвращает состояние и повтор: [ChannelScreen] не может сменить состояние сам,
 * поэтому «Повторить» поднимает счётчик попыток и перезапускает LaunchedEffect.
 */
@Composable
internal fun rememberChannelKey(baseUrl: String): Pair<ChannelKey, () -> Unit> {
    var attempt by remember(baseUrl) { mutableStateOf(0) }
    var state by remember(baseUrl) { mutableStateOf<ChannelKey>(ChannelKey.Loading) }
    LaunchedEffect(baseUrl, attempt) {
        state = ChannelKey.Loading
        // Сеть/файл могут бросить исключение - тогда это такой же провал, как пустой ответ.
        val keyB64 = withContext(Dispatchers.IO) {
            runCatching {
                val devId = AppInstall.ensureDeviceKeys()
                // Ключ канала выдаётся под конкретную установку, поэтому кэш годен
                // только для того же devId. Файл, записанный старой версией
                // программы, маркера не содержит - значит это прежний общий ключ,
                // который сервер больше не принимает: такой кэш не переиспользуем.
                val cached = AppVault.readAppKey()
                val cachedForUs = cached?.takeIf { AppVault.readAppKeyDevId() == devId }
                if (cachedForUs != null) {
                    cachedForUs
                } else {
                    AppKeyBootstrap.fetch(baseUrl, devId)?.let { fresh ->
                        Base64Codec.encode(fresh).also { AppVault.writeAppKey(devId, it) }
                    }
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
internal fun AppRoot(nativeGreeting: () -> String) {
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
