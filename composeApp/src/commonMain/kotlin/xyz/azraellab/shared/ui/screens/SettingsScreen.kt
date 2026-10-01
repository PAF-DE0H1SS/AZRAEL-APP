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
import xyz.azraellab.shared.ui.components.AzraelEmptyState
import xyz.azraellab.shared.ui.components.AzraelErrorState
import xyz.azraellab.shared.ui.components.AzraelListTile
import xyz.azraellab.shared.ui.components.AzraelLoadingState
import xyz.azraellab.shared.ui.components.AzraelSkeletonList
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
import xyz.azraellab.shared.data.Repos
import xyz.azraellab.shared.data.vm.AdminViewModel
import xyz.azraellab.shared.data.vm.ChatsViewModel
import xyz.azraellab.shared.data.vm.SettingsViewModel
import xyz.azraellab.shared.data.vm.ShortenerViewModel
import xyz.azraellab.shared.data.vm.VpnViewModel
import xyz.azraellab.shared.ui.vm.collectAsUiState
import xyz.azraellab.shared.ui.vm.rememberViewModel


private class SettingsTile(
    val id: String,
    val icon: ImageVector,
    val title: String,
    val subtitle: String
)

private val SETTINGS_TILES = listOf(
    SettingsTile("account", Icons.Filled.Person, t["account.title"], t["settings.section.account"]),
    SettingsTile("security", Icons.Filled.Shield, t["security.title"], t["settings.section.security"]),
    SettingsTile("devices", Icons.Filled.Devices, t["devices.title"], t["settings.section.devices"]),
    SettingsTile("privacy", Icons.Filled.Search, t["privacy.title"], t["settings.section.privacy"]),
    SettingsTile("appearance", Icons.Filled.Palette, t["appearance.title"], t["settings.section.appearance"]),
    SettingsTile("channel", Icons.Filled.Settings, t["channel.title"], t["settings.section.channel"])
)

@Composable
internal fun SettingsView(
    client: AppClient,
    profile: AppProfile,
    scrolls: ScrollPositions,
    onRefreshBoot: () -> Unit,
    onLogout: () -> Unit,
    nativeGreeting: () -> String,
    modifier: Modifier = Modifier,
    arg: String? = null,
    onOpenSetting: (String) -> Unit = {}
) {
    var status by remember { mutableStateOf("") }
    val vm = rememberViewModel { SettingsViewModel(client) }
    val devicesState by vm.devices.collectAsUiState()
    val privacyState by vm.privacy.collectAsUiState()
    val provisionState by vm.provision.collectAsUiState()
    val avatarState by vm.avatarUrl.collectAsUiState()
    // Профиль
    var displayName by remember { mutableStateOf(profile.displayName) }
    var tag by remember { mutableStateOf(profile.tag ?: "") }
    var gender by remember { mutableStateOf(profile.gender) }
    // Приватность (локальная форма, сидится из состояния один раз)
    var hiddenFromSearch by remember { mutableStateOf(false) }
    var whoCanSearch by remember { mutableStateOf("all") }
    var whoCanWrite by remember { mutableStateOf("all") }
    var privacyLoaded by remember { mutableStateOf(false) }
    if (!privacyLoaded) {
        val p = (privacyState as? UiState.Ready)?.data
        if (p != null) {
            hiddenFromSearch = p.hiddenFromSearch
            whoCanSearch = p.whoCanSearch.ifBlank { "all" }
            whoCanWrite = p.whoCanWrite.ifBlank { "all" }
            privacyLoaded = true
        }
    }
    // Пароль
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    // Provision-ключ (выданный ключ показываем в текстовом поле)
    var provisionKey by remember { mutableStateOf("") }
    // OTP
    var otpSecret by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    // Устройства аккаунта
    val deviceList = (devicesState as? UiState.Ready)?.data
    val devices = deviceList?.devices ?: emptyList()
    val devicesActive = deviceList?.active ?: 0
    val devicesMax = deviceList?.max ?: 0
    val devicesBusy = devicesState is UiState.Loading
    var revokeTarget by remember { mutableStateOf<DeviceDto?>(null) }
    // Канал
    var l2Pub by remember { mutableStateOf(AppRuntime.srvXPubB64 ?: defaultAppSrvPubB64() ?: "") }
    var gatewayUrl by remember { mutableStateOf(AppRuntime.gatewayUrl ?: defaultGatewayUrl() ?: "") }
    var gwStatus by remember { mutableStateOf("") }
    // Автоудаление аккаунта
    var autoDeleteDays by remember { mutableStateOf<Int?>(profile.autoDeleteDays) }
    var deleteConfirm by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { vm.refreshAll() }

    val avatarUrl = (avatarState as? UiState.Ready)?.data.orEmpty()

    val selected = arg?.let { a -> SETTINGS_TILES.firstOrNull { it.id == a } }
    val scroll = rememberSectionScroll(scrolls, if (selected == null) "settings" else "settings.${selected.id}")
    Column(
        verticalArrangement = Arrangement.spacedBy(AzraelSpace.cardGap),
        modifier = modifier.fillMaxSize().verticalScroll(scroll)
    ) {
        if (selected == null) {
            Text(t["settings.title"], style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            if (status.isNotBlank()) {
                Text(status, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
            }
            SETTINGS_TILES.forEach { tile ->
                AzraelListTile(
                    title = tile.title,
                    subtitle = tile.subtitle,
                    leading = {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(tile.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    trailing = {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.40f)
                        )
                    },
                    onClick = { onOpenSetting(tile.id) }
                )
            }
        } else {
            Text(t["settings.section.${selected.id}"], style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            if (status.isNotBlank()) {
                Text(status, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
            }
            when (selected.id) {
                "account" -> {
                    GlassCard {
                        Column(verticalArrangement = Arrangement.spacedBy(AzraelSpace.listGap)) {
                            Text(t["account.title"], style = MaterialTheme.typography.titleMedium)
                            Text("@${profile.username} · ${profile.role}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Label("UID: ${profile.uid}")
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AccentButton(t["channel.check.session"]) {
                                    scope.launch {
                                        status = "…"
                                        when (val r = vm.verifySession()) {
                                            is UiState.Ready -> status = t("channel.session.ok", r.data)
                                            is UiState.Error -> status = r.message
                                            UiState.Loading -> Unit
                                        }
                                    }
                                }
                                AccentButton(t["settings.refreshAccount"]) { onRefreshBoot() }
                            }
                            Label(t("settings.platform", platformName(), nativeGreeting()))
                        }
                    }
                    GlassCard {
                        Column(verticalArrangement = Arrangement.spacedBy(AzraelSpace.listGap)) {
                            Text(t["settings.lang.title"], style = MaterialTheme.typography.titleMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AppLang.entries.forEach { lang ->
                                    val langTitle = lang.code.uppercase()
                                    AccentButton(if (I18n.lang == lang) "• $langTitle" else langTitle) {
                                        scope.launch {
                                            val prev = I18n.lang
                                            I18n.set(lang)
                                            when (val r = vm.setLang(lang.code)) {
                                                is UiState.Ready -> status = t("settings.langSaved", langTitle)
                                                is UiState.Error -> {
                                                    I18n.set(prev)
                                                    status = r.message
                                                }
                                                UiState.Loading -> Unit
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    GlassCard {
                        Column(verticalArrangement = Arrangement.spacedBy(AzraelSpace.listGap)) {
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
                                colors = fieldColors(MaterialTheme.colorScheme.primary)
                            )
                            OutlinedTextField(
                                value = tag,
                                onValueChange = { tag = it },
                                label = { Text(t["profile.tag"]) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = fieldColors(MaterialTheme.colorScheme.primary)
                            )
                            OutlinedTextField(
                                value = gender,
                                onValueChange = { gender = it },
                                label = { Text(t["profile.gender"]) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = fieldColors(MaterialTheme.colorScheme.primary)
                            )
                            AccentButton(t["profile.save"]) {
                                scope.launch {
                                    status = t["common.saving"]
                                    when (val r = vm.saveProfile(
                                        displayName.trim().ifBlank { null },
                                        tag.trim().ifBlank { null },
                                        gender.trim().ifBlank { null }
                                    )) {
                                        is UiState.Ready -> { status = t["profile.saved"]; onRefreshBoot() }
                                        is UiState.Error -> status = r.message
                                        UiState.Loading -> Unit
                                    }
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
                                            when (val r = vm.avatarSet(stripDataUrl(picked.base64), picked.mime)) {
                                                is UiState.Ready -> status = t["profile.avatar.updated"]
                                                is UiState.Error -> status = r.message
                                                UiState.Loading -> Unit
                                            }
                                        }
                                    }
                                    FilePick.Cancelled -> Unit
                                    FilePick.Unavailable -> status = t["file.picker.unavailable"]
                                }
                            }
                            AccentButton(t["profile.avatar.upload"]) { pickAvatarUpload() }
                        }
                    }
                }
                "security" -> {
                    ConfigGroup(t["security.title"]) {
                        ConfigurableList(
                            listOf(
                                FieldSpec(
                                    key = "oldPassword",
                                    title = t["login.currentPassword"],
                                    label = t["login.currentPassword"],
                                    value = oldPassword,
                                    secret = true,
                                    onSet = { oldPassword = it }
                                ),
                                FieldSpec(
                                    key = "newPassword",
                                    title = t["login.newPassword"],
                                    label = t["login.newPassword"],
                                    value = newPassword,
                                    secret = true,
                                    onSet = { newPassword = it }
                                ),
                                ActionSpec(
                                    key = "password.change",
                                    title = t["login.password"],
                                    label = t["login.changePassword"]
                                ) {
                                    if (oldPassword.isEmpty() || newPassword.length < 8) {
                                        status = t["login.need.passwords"]
                                    } else {
                                        scope.launch {
                                            status = t["common.changing"]
                                            when (val r = vm.changePassword(oldPassword, newPassword)) {
                                                is UiState.Ready -> {
                                                    oldPassword = ""; newPassword = ""
                                                    status = t["login.passwordChanged"]
                                                }
                                                is UiState.Error -> status = r.message
                                                UiState.Loading -> Unit
                                            }
                                        }
                                    }
                                },
                                ActionSpec(
                                    key = "revokeSession",
                                    title = t["channel.revokeSession"],
                                    label = t["channel.revokeSession"]
                                ) {
                                    scope.launch {
                                        when (val r = vm.revokeSession()) {
                                            is UiState.Ready -> { status = t["channel.session.revoked"]; onLogout() }
                                            is UiState.Error -> status = r.message
                                            UiState.Loading -> Unit
                                        }
                                    }
                                }
                            )
                        )
                    }
                    ConfigurableList(
                        listOf(
                            ChoiceSpec(
                                key = "autoDelete",
                                title = t["profile.autoDelete"],
                                desc = t["profile.autoDelete.hint1"] + t("profile.autoDelete.now", autoDeleteState(autoDeleteDays)),
                                // Сроки берутся из Repos.PROFILE_AUTODELETE_DAYS: UI и
                                // проверка в Repos.autoDeleteSet не должны разъезжаться
                                // по набору, иначе можно выбрать срок, который потом
                                // не снимется.
                                options = Repos.PROFILE_AUTODELETE_DAYS.map { d ->
                                    val label = when (d) {
                                        null -> t["off"]
                                        30 -> t["d30s"]
                                        365 -> t["d365"]
                                        else -> t["d$d"]
                                    }
                                    ChoiceOption(d?.toString() ?: "", label)
                                },
                                value = autoDeleteDays?.toString() ?: "",
                                onSet = { raw ->
                                    val d = raw.toIntOrNull()
                                    scope.launch {
                                        when (val r = vm.setAutoDelete(d)) {
                                            is UiState.Ready -> {
                                                autoDeleteDays = r.data
                                                status = if (d == null) t["chats.autoDelete.off"]
                                                else t("chats.autoDelete.in", d)
                                            }
                                            is UiState.Error -> status = r.message
                                            UiState.Loading -> Unit
                                        }
                                    }
                                }
                            )
                        )
                    )
                    if (otpSecret.isNotBlank()) {
                        GlassCard {
                            Label(t("otp.secret", otpSecret))
                        }
                    }
                    ConfigurableList(
                        listOf(
                            FieldSpec(
                                key = "otp.code",
                                title = t["otp.code"],
                                label = t["otp.code"],
                                value = otpCode,
                                onSet = { otpCode = it }
                            ),
                            ActionSpec(
                                key = "otp.issue",
                                title = t["otp.issue"],
                                label = t["otp.issue"]
                            ) {
scope.launch {
                                        when (val r = vm.otpGenerateSecret()) {
                                            is UiState.Ready -> { otpSecret = r.data; status = t["otp.got"] }
                                            is UiState.Error -> status = r.message
                                            UiState.Loading -> Unit
                                        }
                                    }
                            },
                            ActionSpec(
                                key = "otp.verify",
                                title = t["otp.verify"],
                                label = t["otp.verify"]
                            ) {
                                val code = otpCode.trim()
                                if (code.isEmpty()) {
                                    status = t["otp.enter"]
                                } else {
                                    scope.launch {
                                        when (val r = vm.otpValidate(code)) {
                                            is UiState.Ready -> status = if (r.data) t["otp.accepted"] else t["otp.rejected"]
                                            is UiState.Error -> status = r.message
                                            UiState.Loading -> Unit
                                        }
                                    }
                                }
                            }
                        )
                    )
                }
                "devices" -> {
                    ConfigGroup(t["devices.title"]) {
                        ConfigurableList(
                            listOf(
                                ActionSpec(
                                    key = "provision.show",
                                    title = t["devices.provision.show"],
                                    label = t["devices.provision.show"]
                                ) {
                                    scope.launch {
                                        status = "…"
                                        when (val r = vm.showProvisionKey()) {
                                            is UiState.Ready -> {
                                                provisionKey = r.data
                                                status = if (r.data.isNotBlank()) t["devices.provision.once"] else t["devices.provision.issued"]
                                            }
                                            is UiState.Error -> status = r.message
                                            UiState.Loading -> Unit
                                        }
                                    }
                                },
                                ActionSpec(
                                    key = "provision.regen",
                                    title = t["devices.provision.regen"],
                                    label = t["devices.provision.regen"]
                                ) {
                                    scope.launch {
                                        status = "…"
                                        when (val r = vm.regenerateProvisionKey()) {
                                            is UiState.Ready -> {
                                                provisionKey = r.data
                                                status = t["channel.key.old"]
                                            }
                                            is UiState.Error -> status = r.message
                                            UiState.Loading -> Unit
                                        }
                                    }
                                }
                            )
                        )
                    }
                    if (provisionKey.isNotBlank()) {
                        GlassCard {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(provisionKey, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                                AccentButton(t["otp.copy"]) {
                                    clipboard.setText(AnnotatedString(provisionKey))
                                    status = t["devices.key.copied"]
                                }
                            }
                        }
                    }
                    GlassCard {
                        Column(verticalArrangement = Arrangement.spacedBy(AzraelSpace.listGap)) {
                            when (val st = provisionState) {
                                is UiState.Loading -> AzraelLoadingState(t["common.loading"])
                                is UiState.Error -> AzraelErrorState(
                                    message = st.message,
                                    actionLabel = t["action.retry"],
                                    onAction = { vm.refreshProvision() }
                                )
                                is UiState.Ready -> Text(
                                    if (st.data.hasKey) t["login.provision.ready"] else t["login.provision.none"],
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "$devicesActive / $devicesMax",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (devicesActive >= devicesMax) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                                )
                            }
                            Text(
                                t("devices.limit1", devicesMax) + t["devices.limit2"],
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            when (val s = devicesState) {
                                is UiState.Loading -> AzraelSkeletonList(t["common.loading"])
                                is UiState.Error -> AzraelErrorState(
                                    message = s.message,
                                    actionLabel = t["action.retry"],
                                    onAction = { vm.refreshDevices() }
                                )
                                is UiState.Ready -> if (devices.isEmpty()) {
                                    AzraelEmptyState(title = t["short.listNotLoaded"])
                                }
                            }
                            devices.forEach { dev ->
                                val devId = dev.devId
                                val st = dev.status ?: "?"
                                val devTitle = dev.label?.takeIf { it.isNotBlank() }
                                    ?: dev.platform?.takeIf { it.isNotBlank() }
                                    ?: t["devices.title.col"]
                                val isCurrent = devId == vm.currentDeviceId()
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(devTitle, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                        Text(
                                            when (st) {
                                                "active" -> t["active.lower"]
                                                "revoked" -> t["devices.revoked.lower"]
                                                "frozen" -> t["admin.frozen.lower"]
                                                else -> st
                                            },
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (st == "active") MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
                                        )
                                        if (isCurrent) {
                                            Text(t["devices.thisDevice.lower"], style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                    Label(
                                        t("devices.short", devId.take(12), dev.lastSeen ?: "—") +
                                            t("channel.rotations", dev.rotations ?: 0)
                                    )
                                }
                                if (st == "active") {
                                    if (revokeTarget?.devId == devId) {
                                        Text(
                                            if (isCurrent) t["devices.thisDevice"]
                                            else t("devices.revoke.warn", devTitle),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                        AccentButton(t["devices.revoke.yes"]) {
                                            scope.launch {
                                                status = t["devices.revoking"]
                                                when (val r = vm.revokeDevice(devId)) {
                                                    is UiState.Ready -> {
                                                        revokeTarget = null
                                                        status = if (isCurrent) t["devices.current.revoked"]
                                                        else t["devices.revoked"]
                                                        vm.refreshDevices()
                                                    }
                                                    is UiState.Error -> status = r.message
                                                    UiState.Loading -> Unit
                                                }
                                            }
                                        }
                                        AccentButton(t["action.cancel"]) { revokeTarget = null }
                                    } else {
                                        AccentButton(t["devices.revoke"]) { revokeTarget = dev }
                                    }
                                }
                            }
                            AccentButton(if (devicesBusy) t["short.updating"] else t["short.update"]) {
                                scope.launch { vm.refreshDevices() }
                            }
                            OutlinedTextField(
                                value = provisionKey,
                                onValueChange = { provisionKey = it },
                                label = { Text(t["login.provision.label"]) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = fieldColors(MaterialTheme.colorScheme.secondary)
                            )
                            AccentButton(t["devices.check"]) {
                                scope.launch {
                                    status = "…"
                                    when (val r = vm.deviceStatusLabel()) {
                                        is UiState.Ready -> status = if (r.data.isBlank()) t["notBound"] else t("devices.statusRow", r.data)
                                        is UiState.Error -> status = r.message
                                        UiState.Loading -> Unit
                                    }
                                }
                            }
                            if (vm.isL2Available()) {
                                AccentButton(t["devices.rotate"]) {
                                    scope.launch {
                                        status = t["channel.rotating"]
                                        when (val r = vm.rotateDeviceKey()) {
                                            is UiState.Ready -> { status = t["devices.provision.ready"]; vm.refreshDevices() }
                                            is UiState.Error -> status = r.message
                                            UiState.Loading -> Unit
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    t["channel.l2.unavailable"],
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
                "privacy" -> {
                    GlassCard {
                        Column(verticalArrangement = Arrangement.spacedBy(AzraelSpace.listGap)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = hiddenFromSearch,
                                    onCheckedChange = { hiddenFromSearch = it },
                                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.secondary)
                                )
                                Text(t["privacy.hidden"], style = MaterialTheme.typography.bodyMedium)
                            }
                            Label(t["privacy.whoSearch"])
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("all" to t["all.lower"], "inviter" to t["chats.allInviting"], "invited" to t["chats.allInvited"], "custom" to t["chats.myList"]).forEach { (valKey, label) ->
                                    AccentButton(if (whoCanSearch == valKey) "• $label" else label) { whoCanSearch = valKey }
                                }
                            }
                            Label(t["privacy.whoWrite"])
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("all" to t["all.lower"], "inviter" to t["chats.allInviting"], "invited" to t["chats.allInvited"], "custom" to t["chats.myList"]).forEach { (valKey, label) ->
                                    AccentButton(if (whoCanWrite == valKey) "• $label" else label) { whoCanWrite = valKey }
                                }
                            }
                            AccentButton(if (privacyLoaded) t["privacy.save"] else t["privacy.loadSave"]) {
                                scope.launch {
                                    status = t["common.saving"]
                                    when (val r = vm.savePrivacy(hiddenFromSearch, whoCanSearch, whoCanWrite)) {
                                        is UiState.Ready -> status = t["privacy.saved"]
                                        is UiState.Error -> status = r.message
                                        UiState.Loading -> Unit
                                    }
                                }
                            }
                        }
                    }
                }
                "appearance" -> {
                    ConfigGroup(t["appearance.title"]) {
                        ConfigurableList(
                            listOf(
                                ChoiceSpec(
                                    key = "theme.mode",
                                    title = t["appearance.title"],
                                    options = listOf(
                                        ChoiceOption("system", t["theme.system"]),
                                        ChoiceOption("dark", t["theme.dark"]),
                                        ChoiceOption("light", t["theme.light"])
                                    ),
                                    value = AzraelThemeState.mode.code,
                                    onSet = { code -> AzraelThemeState.set(AppThemeMode.of(code)) }
                                )
                            )
                        )
                    }
                }
                else -> {
                    ConfigGroup(t["channel.endpoint.label"]) {
                        ConfigurableList(
                            listOf(
                                ActionSpec(
                                    key = "channel.health",
                                    title = t["channel.check.health"],
                                    label = t["channel.check.health"]
                                ) {
                                    scope.launch {
                                        status = "…"
                                        when (val r = vm.healthText()) {
                                            is UiState.Ready -> status = t("channel.health.responds", r.data)
                                            is UiState.Error -> status = r.message
                                            UiState.Loading -> Unit
                                        }
                                    }
                                },
                                FieldSpec(
                                    key = "channel.l2",
                                    title = t["channel.l2.key"],
                                    label = t["channel.l2.key"],
                                    value = l2Pub,
                                    onSet = { l2Pub = it }
                                ),
                                ActionSpec(
                                    key = "channel.l2.apply",
                                    title = t["channel.l2.apply"],
                                    label = t["channel.l2.apply"]
                                ) {
                                    AppRuntime.srvXPubB64 = l2Pub.trim().ifBlank { null }
                                    status = t["channel.l2.applied"]
                                }
                            )
                        )
                        Label(t["profile.autoDelete.hint2"])
                    }
                    GlassCard {
                        Column(verticalArrangement = Arrangement.spacedBy(AzraelSpace.listGap)) {
                            Text(t["channel.l2.gateway"], style = MaterialTheme.typography.titleMedium)
                            OutlinedTextField(
                                value = gatewayUrl,
                                onValueChange = { gatewayUrl = it },
                                label = { Text("https://…/api/gateway/v1") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = fieldColors(MaterialTheme.colorScheme.secondary)
                            )
                            AccentButton(t["vpn.connect"]) {
                                val url = normalizedEndpoint(gatewayUrl)
                                if (url == null) {
                                    gwStatus = t["login.need.https"]
                                } else {
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
                            }
                            if (gwStatus.isNotBlank()) Label(gwStatus)
                            Label(t["channel.l2.privacy"])
                        }
                    }
                    GlassCard {
                        Column(verticalArrangement = Arrangement.spacedBy(AzraelSpace.listGap)) {
                            Text(t["channel.title"], style = MaterialTheme.typography.titleMedium)
                            Text(
                                t("tab.role", profile.role) + t["login.protect"],
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            AccentButton(t["channel.logout"]) { onLogout() }
                            Label(t["profile.delete.warn"])
                            if (deleteConfirm) {
                                Text(t("profile.delete.confirm", profile.username), color = MaterialTheme.colorScheme.error)
                                AccentButton(t["profile.delete.yes"]) {
                                    scope.launch {
                                        status = t["common.deleting"]
                                        when (val r = vm.deleteProfile()) {
                                            is UiState.Ready -> {
                                                status = t["profile.deleted"]
                                                onLogout()
                                            }
                                            is UiState.Error -> status = r.message
                                            UiState.Loading -> Unit
                                        }
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
        }
    }
}
