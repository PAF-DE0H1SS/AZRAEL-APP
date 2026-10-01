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
import xyz.azraellab.shared.ui.components.AzraelBanner
import xyz.azraellab.shared.ui.components.AzraelBannerTone
import xyz.azraellab.shared.ui.components.AzraelButton
import xyz.azraellab.shared.ui.components.AzraelButtonTone
import xyz.azraellab.shared.ui.components.AzraelCard
import xyz.azraellab.shared.ui.components.AzraelChoiceChip
import xyz.azraellab.shared.ui.components.AzraelEmptyState
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


// ---- Главный экран: повтор /main сайта, табы из home.boot, роли ----

@Composable
internal fun MainShell(
    session: Session,
    boot: JsonObject,
    /**
     * Счётчик refresh `home.boot`. Сам по себе не состояние навигации: `Session.boot`
     * - обычное поле, поэтому без этого счётчика `MainShell` не пересобрался бы
     * после обновления прав. Он же входит в ключ `rebase`.
     */
    bootTick: Int,
    onRefreshBoot: () -> Unit,
    onLogout: () -> Unit,
    nativeGreeting: () -> String
) {
    val profile = parseProfile(boot.o("profile") ?: JsonObject(emptyMap()))
    val config = buildTabConfig(
        tabs = parseTabs(boot.o("tabConfig")),
        defaultTab = boot.o("tabConfig")?.s("defaultTab")
    )
    if (config.items.isEmpty()) {
        // Сервер не вернул ни одного доступного раздела (роль/конфиг) - не молчим, а объясняем.
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AzraelEmptyState(
                title = t["main.noTabs"],
                subtitle = t("main.noTabs.hint", profile.role)
            )
            Spacer(Modifier.height(16.dp))
            AccentButton(t["action.refresh"]) { onRefreshBoot() }
            Spacer(Modifier.height(8.dp))
            AccentButton(t["action.logout"]) { onLogout() }
        }
        return
    }
    val client = session.client
    val state = rememberNavState(config.root)
    val navigator = remember(state) { Navigator(state) }
    val selectedId = selectedTabId(state, config)

    // Смена tabConfig (refresh boot или смена роли) не должна выкидывать из открытого
    // подэкрана и не должна оставлять на разделе, который сервер уже закрыл.
    // `rebase` идемпотентен, поэтому повторный запуск на `bootTick` безопасен.
    LaunchedEffect(config, bootTick) { navigator.rebase(config) }

    // Deep link (`azrael://messages/42`). Ссылка приходит извне, поэтому права
    // проверяются в `openDeepLink(config, …)`: раздел, которого нет в tabConfig
    // этой роли, не откроется - ни админка по прямой ссылке, ни чат без «Сообщений».
    //
    // Ключ эффекта - `deepLinkVersion`, а не `bootTick`: ссылка может прийти в
    // уже работающее приложение (`onNewIntent`), и раньше она молча ждала
    // следующего refresh `tabConfig` - «нажал на ссылку, ничего не открылось».
    val deepLinkVersion = DeepLinkSignal.version
    LaunchedEffect(deepLinkVersion) {
        AppDeepLink.consume()?.let { raw ->
            if (!navigator.openDeepLink(config, raw)) {
                // Мусор или закрытый раздел - не молчим, а остаёмся на месте:
                // раньше такая ссылка просто игнорировалась, и ссылка «ничего не сделала».
                logAzraelError("nav", "deep link ignored: $raw", null)
            }
        }
    }

    // Позиции скролла живут здесь, а не внутри экранов: список пересоздаётся при
    // каждом переходе, и без общей карты возврат на вкладку начинался сверху.
    val scrolls = rememberScrollPositions()

    // Системная кнопка «Назад» (Android). На узком экране рельсы с кнопкой нет,
    // поэтому иначе «назад» из комнаты чата просто закрывал бы приложение.
    PlatformBackHandler(enabled = navigator.canGoBack) { navigator.back() }

    AzraelNavScaffold(
        items = config.items,
        selectedId = selectedId,
        onSelect = { id -> navigator.openTab(config, id) },
        onLogout = onLogout,
        onBack = if (navigator.canGoBack) ({ navigator.back() }) else null
    ) { contentModifier ->
        Column(modifier = contentModifier.fillMaxSize()) {
            // Офлайн-индикатор: сеть пропала, а список молча показывал бы старые
            // данные - пользователь решил бы, что новых сообщений нет. Баннер стоит
            // над шапкой и появляется только когда сети действительно нет.
            if (!rememberOnline()) {
                AzraelBanner(
                    text = t["net.offline"],
                    tone = AzraelBannerTone.Warning,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AzraelSpace.md, AzraelSpace.md, AzraelSpace.md, 0.dp)
                )
            }
            Box(modifier = Modifier.fillMaxWidth().padding(AzraelSpace.md, AzraelSpace.md, AzraelSpace.md, 0.dp)) {
                TriStateHeader(profile, onRefreshBoot)
            }
            NavHost(state, modifier = Modifier.weight(1f).fillMaxWidth().padding(AzraelSpace.lg)) { destination ->
                MainContent(
                    destination = destination,
                    config = config,
                    scrolls = scrolls,
                    client = client,
                    profile = profile,
                    onOpenSetting = { arg -> navigator.open(config, DetailKind.Setting, arg) },
                    onRefreshBoot = onRefreshBoot,
                    onLogout = onLogout,
                    onOpenRoom = { id ->
                        if (id == null) navigator.back() else navigator.open(config, DetailKind.ChatRoom, id.toString())
                    },
                    nativeGreeting = nativeGreeting,
                    modifier = Modifier.fillMaxSize()
                )
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
                Icon(Icons.Filled.Refresh, contentDescription = t["main.refresh"], tint = MaterialTheme.colorScheme.secondary)
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
        Text(role, color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun AvatarCircle(profile: AppProfile) {
    val letter = (profile.displayName.ifBlank { profile.username }).take(1).uppercase()
    Box(
        modifier = Modifier
            .width(40.dp)
            .height(40.dp)
            .background(roleColor(profile.role).copy(alpha = 0.35f), CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(letter, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

// `tabIcon` и `SideRail` уехали в `ui/nav`: иконка раздела теперь часть `TabSpec`
// (иначе она расходилась с `tabLabel` - «vpn_tab» и «vpn» локализовались вместе,
// а иконка знала только `vpn_tab`), а рельса рисуется тем же стеклом, что и
// нижняя панель.

@Composable
private fun MainContent(
    destination: Destination,
    config: TabConfig,
    scrolls: ScrollPositions,
    client: AppClient,
    profile: AppProfile,
    onOpenSetting: (String) -> Unit,
    onRefreshBoot: () -> Unit,
    onLogout: () -> Unit,
    onOpenRoom: (Long?) -> Unit,
    nativeGreeting: () -> String,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        when (destination) {
            // Ключ скролла - id раздела: список «Сообщений» и AI-чат внутри него
            // скроллятся независимо, а возвращение на вкладку восстанавливает
            // именно ту позицию, с которой ушли.
            is Destination.Tab -> when (destination.spec) {
                TabSpec.Messages -> ChatsView(client, profile, scrolls, roomId = null, onRoomChange = onOpenRoom)
                TabSpec.Admin -> AdminView(client, profile, scrolls)
                TabSpec.Shortener -> ShortenerView(client, scrolls)
                TabSpec.Vpn -> VpnView(client, scrolls)
                TabSpec.Settings -> SettingsView(
                    client = client, profile = profile, scrolls = scrolls,
                    onRefreshBoot = onRefreshBoot,
                    onLogout = onLogout, nativeGreeting = nativeGreeting,
                    onOpenSetting = onOpenSetting
                )
            }
            // Комната чата - уже настоящий подэкран: она приходит из deep link
            // `messages/42` и из клика по списку, поэтому её позиция скролла
            // отдельная, а «назад» возвращает в список. Остальные подэкраны
            // появятся в P2-P4; пока показываем раздел-родитель, чтобы «назад»
            // вёл туда же, куда пользователь пришёл, а не в пустоту. Settings
            // открывает подэкраны декларативно через `onOpenSetting`.
            is Destination.Detail -> when (destination.kind) {
                DetailKind.ChatRoom -> ChatsView(
                    client, profile, scrolls,
                    roomId = destination.arg?.toLongOrNull(),
                    onRoomChange = onOpenRoom
                )
                DetailKind.Setting -> SettingsView(
                    client = client, profile = profile, scrolls = scrolls,
                    onRefreshBoot = onRefreshBoot,
                    onLogout = onLogout, nativeGreeting = nativeGreeting,
                    arg = destination.arg, onOpenSetting = onOpenSetting
                )
                else -> MainContent(
                    Destination.Tab(destination.parentTab()),
                    config, scrolls, client, profile, onOpenSetting, onRefreshBoot, onLogout, onOpenRoom, nativeGreeting
                )
            }
            is Destination.Placeholder -> PlaceholderScreen(
                destination.tabId,
                t("tab.soon", config.find(destination.tabId)?.label ?: destination.tabId)
            )
        }
    }
}

// ---- Админка (только владелец; таб сервер отдаёт только для owner) ----

