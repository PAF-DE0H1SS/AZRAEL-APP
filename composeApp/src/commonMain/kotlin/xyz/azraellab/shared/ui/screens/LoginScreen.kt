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


// ---- Вход и регистрация аккаунта (пароль, инвайт, пол, аватар) ----
// Оформление повторяет /login сайта (app/login/page.tsx): узкая карточка по центру,
// сегмент-переключатель вкладок, «стеклянные» поля, чипы выбора, аватар-круг
// и полноширинная кнопка действия. Фирменные цвета и фон - из ui/Theme.kt.

private enum class AuthTab { Login, Register }

@Composable
internal fun LoginScreen(
    client: AppClient,
    onThreat: (ThreatMode) -> Unit,
    initStatus: String?,
    onLoggedIn: (Session) -> Unit
) {
    // rememberSaveable, а не remember: поворот экрана, смена темы/языка или смерть
    // процесса пересоздают Activity - форма входа не должна молча очищаться.
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

    // Уже привязанная установка: вход по логину/паролю без ключа привязки -
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
                    .glass(corner = 26.dp, borderColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f))
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
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.95f)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (tab == AuthTab.Login) t["login.access.note"] else t["login.register.hint"],
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f),
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
                        accent = MaterialTheme.colorScheme.primary,
                        leading = Icons.Filled.Person,
                        enabled = !busy
                    )
                    AuthField(
                        value = password,
                        onValueChange = { password = it; clearError() },
                        label = t["login.password.new"],
                        accent = MaterialTheme.colorScheme.primary,
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
                        // Коды на сайте всегда в верхнем регистре - приводим сразу.
                        onValueChange = { inviteCode = it.uppercase().take(11); clearError() },
                        label = t["login.invite.label"],
                        supporting = t["login.invite.hint"],
                        accent = MaterialTheme.colorScheme.secondary,
                        leading = Icons.Filled.Key,
                        spaced = true,
                        enabled = !busy
                    )
                    AuthField(
                        value = username,
                        onValueChange = { username = it; clearError() },
                        label = t["login.username"],
                        accent = MaterialTheme.colorScheme.primary,
                        leading = Icons.Filled.Person,
                        enabled = !busy
                    )
                    AuthField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = t["login.displayName"],
                        accent = MaterialTheme.colorScheme.primary,
                        leading = Icons.Filled.Person,
                        enabled = !busy
                    )
                    AuthField(
                        value = password,
                        onValueChange = { password = it; clearError() },
                        label = t["login.password.new"],
                        accent = MaterialTheme.colorScheme.primary,
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
                        accent = MaterialTheme.colorScheme.primary,
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
                    tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    t["channel.security"],
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/** Заголовок над карточкой входа: имя приложения и вводная строка. */
@Composable
private fun BrandMark() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            t["app.name"],
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.95f),
            letterSpacing = 3.sp
        )
        Spacer(Modifier.height(6.dp))
        Text(
            t["login.intro1"] + t["login.intro2"],
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.42f),
            textAlign = TextAlign.Center
        )
    }
}

/** Сегмент-переключатель «Вход / Регистрация» - как на сайте. */
@Composable
private fun AuthTabs(selected: AuthTab, enabled: Boolean, onSelect: (AuthTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.04f))
            .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.06f), RoundedCornerShape(14.dp))
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
                        .background(if (active) MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f) else Color.Transparent)
                        .clickable(enabled = enabled) { onSelect(item) }
                        .padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        color = if (active) MaterialTheme.colorScheme.secondary.copy(alpha = 0.95f) else MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
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
                tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f),
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
                        tint = MaterialTheme.colorScheme.secondary.copy(alpha = if (visible) 0.75f else 0.4f),
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
            focusedContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.07f),
            unfocusedContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.04f),
            disabledContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.02f),
            focusedBorderColor = accent.copy(alpha = 0.7f),
            unfocusedBorderColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
            disabledBorderColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.06f),
            focusedLabelColor = accent,
            unfocusedLabelColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.42f),
            disabledLabelColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f),
            focusedTextColor = MaterialTheme.colorScheme.secondary,
            unfocusedTextColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.9f),
            disabledTextColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f),
            cursorColor = accent,
            focusedLeadingIconColor = accent.copy(alpha = 0.9f),
            unfocusedLeadingIconColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f),
            focusedSupportingTextColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f),
            unfocusedSupportingTextColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)
        )
    )
}

/** Полоса надёжности пароля при регистрации - как PasswordStrength на сайте. */
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
                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f))
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
                Triple("male", t["login.gender.male"], MaterialTheme.colorScheme.secondary),
                Triple("female", t["login.gender.female"], MaterialTheme.colorScheme.error)
            ).forEach { (value, label, accent) ->
                val active = selected == value
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (active) accent.copy(alpha = 0.18f) else MaterialTheme.colorScheme.secondary.copy(alpha = 0.04f))
                        .border(
                            1.dp,
                            if (active) accent.copy(alpha = 0.45f) else MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f),
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
                        color = if (active) accent else MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)
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
                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.04f))
                .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                .clickable(enabled = enabled, onClick = onPick)
                .padding(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.06f))
                    .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f), CircleShape),
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
                        tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    avatar?.name ?: t["login.avatar.hint"],
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = if (avatar == null) 0.45f else 0.85f),
                    maxLines = 1
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    if (avatar == null) t["login.avatar.pick"] else t["login.avatar.change"],
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.85f)
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
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
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
            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.06f))
            .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.Key,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                t["login.provision.label"],
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.95f)
            )
        }
        Text(
            t["login.provision.site1"] + t["login.provision.site2"],
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
        )
        AuthField(
            value = value,
            onValueChange = onValueChange,
            label = t["login.provision.name"],
            accent = MaterialTheme.colorScheme.secondary,
            leading = Icons.Filled.Key,
            spaced = true,
            enabled = enabled
        )
    }
}

/** Чекбокс «Запомнить устройство» - вся строка кликабельна. */
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
                checkedColor = MaterialTheme.colorScheme.secondary,
                uncheckedColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f),
                checkmarkColor = MaterialTheme.colorScheme.onPrimary
            )
        )
        Spacer(Modifier.width(4.dp))
        Text(
            t["login.remember"],
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
        )
    }
}

/** Статус/ошибка над кнопкой действия - как на сайте (красный текст под полями). */
@Composable
private fun StatusBanner(text: String, isError: Boolean) {
    val accent = if (isError) Color(0xFFF87171) else MaterialTheme.colorScheme.secondary
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
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.88f)
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
            .background(MaterialTheme.colorScheme.secondary.copy(alpha = fill))
            .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f), shape)
            .clickable(enabled = !busy, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (busy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = MaterialTheme.colorScheme.secondary,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(10.dp))
            }
            Text(
                if (busy) busyLabel else label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = if (busy) 0.45f else 0.88f)
            )
        }
    }
}

/** Лимит аватара на клиенте: 10 МиБ файла (сервер отвергнет большее). */
private const val MAX_AVATAR_BYTES = 10L * 1024 * 1024

/** Запас под base64-размер того же файла (4/3) + заполнение, чтобы не упереться в 413. */
private const val MAX_AVATAR_B64 = 16L * 1024 * 1024
