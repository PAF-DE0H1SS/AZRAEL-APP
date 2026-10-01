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
import androidx.compose.material3.ColorScheme
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
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
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


// ---- Данные кастомного API (POST /api/app/v1), контракт APP_DEV_LOG/02/04 ----

// Дубли data/Json.kt. Раньше здесь стояло `this[key]?.jsonPrimitive?.content`, что
// бросает IllegalArgumentException, если сервер прислал на месте строки объект или
// массив: `jsonPrimitive` на не-primitive не optional. `as?` вместо `.jsonPrimitive`
// даёт на не-primitive null вместо исключения - то же самое, что в data/Json.kt, и
// тот же набор расширений; менять надо оба сразу.
private fun JsonObject.primOrNull(key: String): JsonPrimitive? = this[key] as? JsonPrimitive

internal fun JsonObject.s(key: String): String? = primOrNull(key)?.contentOrNull
internal fun JsonObject.l(key: String): Long? = primOrNull(key)?.longOrNull
internal fun JsonObject.i(key: String): Int? = primOrNull(key)?.intOrNull
internal fun JsonObject.o(key: String): JsonObject? = this[key] as? JsonObject
internal fun JsonObject.a(key: String): List<JsonElement> =
    this[key]?.let { runCatching { it.jsonArray }.getOrNull() }?.toList() ?: emptyList()

internal fun JsonElement.boolValue(): Boolean {
    val p = runCatching { jsonPrimitive }.getOrNull() ?: return false
    return p.booleanOrNull == true || p.contentOrNull == "1"
}

/** Настоящий JSON-boolean: сервер шлёт true/false, но в MariaDB-полях встречается 0/1. */
internal fun JsonObject.b(key: String): Boolean = this[key]?.boolValue() ?: false

/** HTTPS обязателен; http:// допускается только для локального стенда. */
internal fun normalizedEndpoint(raw: String): String? {
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
    /** Язык аккаунта (users.lang) - общий для сайта и всех устройств. */
    val lang: String?
)

internal fun parseProfile(o: JsonObject): AppProfile = AppProfile(
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
 * Локальная подпись таба: сервер присылает русские label'ы (/main - русский UI),
 * поэтому переводим по id, а серверный label оставляем запасным для новых табов.
 */
/** Локализуем id раздела, но только если он есть в приложении: иначе берём серверную строку. */
internal fun tabLabel(id: String, serverLabel: String): String {
    val spec = TabSpec.fromTabId(id) ?: return serverLabel
    return t[spec.labelKey]
}

/**
 * Разбор серверного `tabConfig` в [ServerTab]. Раньше здесь сразу отбрасывались
 * `visible: false` и терялся факт, что раздел существует: при несовпадении версий
 * клиента и сервера было нельзя понять, «закрыт по роли» или «неизвестен». Теперь
 * флаг сохраняется, а решение принимает [buildTabConfig].
 */
internal fun parseTabs(cfg: JsonObject?): List<ServerTab> {
    val out = mutableListOf<ServerTab>()
    for (e in cfg?.a("tabs") ?: emptyList()) {
        val obj = e as? JsonObject ?: continue
        val id = obj.s("id") ?: continue
        val visible = !obj.containsKey("visible") || obj.b("visible")
        out += ServerTab(id = id, serverLabel = tabLabel(id, obj.s("label") ?: id), visible = visible)
    }
    return out
}

/**
 * Цвет роли в конкретной схеме. Раньше здесь стояли зашитые литералы и токены
 * тёмной палитры: на светлой теме «золото» и «янтарь» превращались в блёклые
 * пятна, а обычная роль вообще не читалась. Роли - это смысловые слоты темы,
 * поэтому берём их из [ColorScheme], а не из палитры.
 */
internal fun roleColor(role: String, scheme: ColorScheme): Color = when (role) {
    "owner" -> scheme.error
    "admin" -> scheme.primary
    "rf" -> scheme.tertiary
    "premium" -> scheme.tertiary
    else -> scheme.secondary
}

@Composable
internal fun roleColor(role: String): Color = roleColor(role, MaterialTheme.colorScheme)

/**
 * Текст ошибки для показа пользователю. Русские формулировки сервера приходят
 * в e.message - их не переводим (иначе получим «Ошибка заполнения - Введите
 * код»), поэтому для известных случаев берём свою строку по коду.
 */
internal fun errText(e: Throwable): String = if (e is AppException) {
    if (e.message?.startsWith("Лимит устройств") == true) {
        e.message ?: t["error.deviceLimit"]
    } else {
        val base = when (e.code) {
            NETWORK -> t["error.network"]
            AppErrorCode.MALFORMED -> t["error.malformed"]
            AppErrorCode.SESSION -> t["error.session"]
            AppErrorCode.FORBIDDEN -> t["error.forbidden"]
            AppErrorCode.L2_REQUIRED -> t["error.l2.required"]
            AppErrorCode.L2_UNAVAILABLE -> t["error.l2.unavailable"]
            AppErrorCode.INVALID_CRED -> t["error.credentials"]
            AppErrorCode.USER_EXISTS -> t["error.userExists"]
            AppErrorCode.INVITE_BAD -> t["error.invite"]
            AppErrorCode.VALIDATION -> t["error.validation"]
            else -> t("error.code", e.code)
        }
        val detail = e.message?.trim().orEmpty()
        // Для кодов L2 detail не дописываем: серверные тексты там технические
        // и английские ('l2 gateway error'), а своя строка уже объясняет причину
        // и что делать. Остальные коды оставляем как есть - потеря контекста
        // сервера полезнее, чем «Ошибка 204 - validation failed».
        if (e.code == AppErrorCode.L2_REQUIRED || e.code == AppErrorCode.L2_UNAVAILABLE) base
        else if (detail.isEmpty() || detail == "err=${e.code}") base else "$base - $detail"
    }
} else {
    t("error.crash", e.message)
}
internal fun autoDeleteState(days: Int?): String =
    if (days == null) t["profile.autoDelete.offState"] else t("unit.days", days)

/**
 * Содержимое по [Destination]. Переход со строковых id на sealed-тип сделан в P1:
 * `when` теперь проверяется компилятором, а неизвестный серверный раздел приходит
 * сюда как [Destination.Placeholder] и показывается с настоящим названием, а не
 * падает и не показывает пустоту.
 */
internal fun qrBitmap(dataUrl: String?): ImageBitmap? {
    val raw = stripDataUrl(dataUrl ?: "").ifBlank { return null }
    return decodeImageBase64(raw)
}

/** Превью выбранного аватара: base64 из pickFile (с data:-префиксом или без). */
internal fun avatarBitmap(base64: String): ImageBitmap? {
    val raw = stripDataUrl(base64).ifBlank { return null }
    return decodeImageBase64(raw)
}
internal fun makeClient(baseUrl: String, keyB64: String): AppClient {
    val url = normalizedEndpoint(baseUrl)
        ?: normalizedEndpoint(defaultAppUrl().orEmpty())
        ?: "https://azrael-lab.xyz/api/app/v1"
    return AppClient(url, AppSecure.appKeyFromB64(keyB64))
}

@Composable
/**
 * Цвета рамки поля. Схема передаётся явно, а не берётся из `MaterialTheme`
 * внутри: функцию дёргают из мест, где уже есть готовая схема, и так её можно
 * проверить тестом без композиции.
 */
internal fun fieldColors(accent: Color, scheme: ColorScheme) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = accent,
    unfocusedBorderColor = scheme.secondary.copy(alpha = 0.25f)
)

@Composable
internal fun fieldColors(accent: Color) = fieldColors(accent, MaterialTheme.colorScheme)

@Composable
internal fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = this.clickable(
    interactionSource = remember { MutableInteractionSource() },
    indication = null
) { onClick() }

@Composable
internal fun SurfaceGlass(shape: RoundedCornerShape, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier = modifier.glass()) {
        content()
    }
}

@Composable
internal fun GlassCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier = modifier.glass().padding(18.dp)) {
        content()
    }
}

@Composable
internal fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f))
}

@Composable
internal fun AccentButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
        contentColor = MaterialTheme.colorScheme.secondary
        )
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
internal fun PlaceholderScreen(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f))
    }
}
