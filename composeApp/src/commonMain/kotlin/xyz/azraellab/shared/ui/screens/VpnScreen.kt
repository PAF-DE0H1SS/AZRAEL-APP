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
import xyz.azraellab.shared.data.vm.AdminViewModel
import xyz.azraellab.shared.data.vm.ChatsViewModel
import xyz.azraellab.shared.data.vm.SettingsViewModel
import xyz.azraellab.shared.data.vm.ShortenerViewModel
import xyz.azraellab.shared.data.vm.VpnViewModel
import xyz.azraellab.shared.ui.vm.collectAsUiState
import xyz.azraellab.shared.ui.vm.rememberViewModel


@Composable
internal fun VpnView(client: AppClient, scrolls: ScrollPositions) {
    val vm = rememberViewModel { VpnViewModel(client) }
    val summaryState by vm.summary.collectAsUiState()
    val awgState by vm.awg.collectAsUiState()
    val incysState by vm.incys.collectAsUiState()
    val serversState by vm.servers.collectAsUiState()
    var status by remember { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { vm.refreshAll() }

    val scroll = rememberSectionScroll(scrolls, "vpn")
    Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxSize().verticalScroll(scroll)) {
        Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
        ) {
        Text("VPN", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        IconButton(onClick = { vm.refreshAll() }) {
            Icon(Icons.Filled.Refresh, contentDescription = t["action.refresh"], tint = MaterialTheme.colorScheme.secondary)
        }
        }
        if (status.isNotBlank()) Text(status, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)

        val s = (summaryState as? UiState.Ready)?.data
        GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(t["vpn.freeServers"], style = MaterialTheme.typography.titleMedium)
            when (summaryState) {
                is UiState.Loading -> AzraelLoadingState(t["common.loading"])
                is UiState.Error -> AzraelErrorState(
                    message = (summaryState as UiState.Error).message,
                    actionLabel = t["action.retry"],
                    onAction = { vm.refreshAll() }
                )
                is UiState.Ready -> if (s == null) {
                    AzraelEmptyState(title = t["vpn.summaryNotLoaded"])
                } else {
                Text(t("vpn.totalAlive", s.total, s.alive), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                val sub = s.subUrl ?: ""
                Label(t["vpn.sub.hint"])
                OutlinedTextField(
                    value = sub,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("subUrl") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors(MaterialTheme.colorScheme.primary)
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
        }

        GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(t["vpn.servers"], style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AccentButton(t["all"]) {
                    vm.loadServers(null)
                    status = t["common.loading"]
                }
                listOf("fast", "ok", "reality", "russia", "europe").forEach { f ->
                    AccentButton(f) {
                        vm.loadServers(f)
                        status = t["common.loading"]
                    }
                }
            }
            AccentButton(t["short.updateServer"]) {
                scope.launch {
                    status = t["vpn.downloading"]
                    when (val d = vm.refreshServerLists()) {
                        is UiState.Ready -> status = t("vpn.listUpdatedFull", d.data.alive, d.data.total)
                        is UiState.Error -> status = t("short.updateFailed", d.message)
                        is UiState.Loading -> { /* не используется */ }
                    }
                    vm.refreshAll()
                }
            }
            val servers = (serversState as? UiState.Ready)?.data ?: emptyList()
            when (val st = serversState) {
                is UiState.Loading -> AzraelSkeletonList(t["common.loading"])
                is UiState.Error -> AzraelErrorState(
                    message = st.message,
                    actionLabel = t["action.retry"],
                    onAction = { vm.loadServers(null) }
                )
                is UiState.Ready -> if (servers.isEmpty()) {
                    AzraelEmptyState(title = t["short.listNotLoaded"])
                }
            }
            servers.take(25).forEach { v ->
                val link = v.link ?: ""
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "${v.flag ?: ""} ${v.name} · ${v.host}:${v.port ?: "?"} " +
                            v.latency?.let { "· ${t("vpn.ms", it)}" } ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (v.alive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        maxLines = 1
                    )
                    if (link.isNotBlank()) {
                        IconButton(onClick = {
                            clipboard.setText(AnnotatedString(link))
                            status = t["vpn.configCopied"]
                        }) { Icon(Icons.Filled.ContentCopy, contentDescription = t["vpn.copyConfig"], tint = MaterialTheme.colorScheme.secondary) }
                    }
                }
            }
        }
        }

        GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("A-WG (Amnezia WireGuard)", style = MaterialTheme.typography.titleMedium)
            val a = (awgState as? UiState.Ready)?.data
            when (val st = awgState) {
                is UiState.Loading -> AzraelLoadingState(t["common.loading"])
                is UiState.Error -> AzraelErrorState(
                    message = st.message,
                    actionLabel = t["action.retry"],
                    onAction = { vm.refreshAll() }
                )
                is UiState.Ready -> if (a == null) {
                    AzraelEmptyState(title = t["vpn.awg.unknown"])
                } else {
                Text(
                    if (a.awgEnabled) t["on"] else t["notConfigured"],
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (a.awgEnabled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
                )
                a.endpoint?.let { Label("endpoint: $it") }
                val conf = a.vpnConfig
                if (conf != null) {
                    OutlinedTextField(
                        value = conf,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("vpnConfig") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors(MaterialTheme.colorScheme.secondary)
                    )
                    AccentButton(t["vpn.awg.copy"]) {
                        clipboard.setText(AnnotatedString(conf))
                        status = t["vpn.awg.copied"]
                    }
                } else if (a.awgEnabled) {
                    Label(t["vpn.awg.configHint"])
                }
                }
            }
        }
        }

        GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(t["vpn.incy"], style = MaterialTheme.typography.titleMedium)
            val platforms = (incysState as? UiState.Ready)?.data ?: emptyList()
            when (val st = incysState) {
                is UiState.Loading -> AzraelSkeletonList(t["common.loading"])
                is UiState.Error -> AzraelErrorState(
                    message = st.message,
                    actionLabel = t["action.retry"],
                    onAction = { vm.refreshAll() }
                )
                is UiState.Ready -> if (platforms.isEmpty()) {
                    AzraelEmptyState(title = t["vpn.downloadsHint"])
                }
            }
            platforms.forEach { p ->
                Text("${p.name} — ${p.filename}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f))
            }
        }
        }
    }
}

// ---- Настройки (декларативные): меню + 6 разделов ----
