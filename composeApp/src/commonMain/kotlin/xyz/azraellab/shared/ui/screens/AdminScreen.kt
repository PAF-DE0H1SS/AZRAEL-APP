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
internal fun AdminView(client: AppClient, profile: AppProfile, scrolls: ScrollPositions) {
    val vm = rememberViewModel { AdminViewModel(client) }
    var status by remember { mutableStateOf("") }
    val frozenState by vm.frozen.collectAsUiState()
    val invitesState by vm.invites.collectAsUiState()
    val myCodeState by vm.myCode.collectAsUiState()
    val healthState by vm.health.collectAsUiState()
    var invitesAvailable by remember { mutableStateOf<Int?>(null) }
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { vm.refreshAll() }

    val scroll = rememberSectionScroll(scrolls, "admin")
    Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxSize().verticalScroll(scroll)) {
        Text(t["tab.admin"], style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            t["admin.hint"],
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
        )
        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Label(t["profile.uid"])
                Text(profile.uid, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AccentButton(t["admin.health"]) {
                        vm.refreshHealth()
                    }
                }
                if (status.isNotBlank()) {
                    Text(status, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
                when (val h = healthState) {
                    is UiState.Loading -> AzraelLoadingState(t["common.loading"])
                    is UiState.Error -> AzraelErrorState(
                        message = h.message,
                        actionLabel = t["action.retry"],
                        onAction = { vm.refreshHealth() }
                    )
                    is UiState.Ready -> Text(
                        "health: ${if (h.data.ok) "ok" else "err"} ${h.data.gateway ?: ""}".trim(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(t["admin.invites"], style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                val myCode = (myCodeState as? UiState.Ready)?.data?.takeIf { it.isNotBlank() }
                if (myCode != null) {
                    Text(t("admin.myCode", myCode), color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                    AccentButton(t["admin.copyMyCode"]) {
                        clipboard.setText(AnnotatedString(myCode))
                        status = t["admin.codeCopied"]
                    }
                }
                AccentButton(if (invitesAvailable == null) t["admin.generate"] else t("admin.generateFree", invitesAvailable)) {
                    scope.launch {
                        status = t["admin.generating"]
                        when (val d = vm.generateInvites()) {
                            is UiState.Ready -> {
                                invitesAvailable = d.data.available
                                status = t("admin.createdCodes", d.data.generatedCodes.size)
                                vm.refreshInvites(true)
                            }
                            is UiState.Error -> status = d.message
                            is UiState.Loading -> { /* не используется */ }
                        }
                    }
                }
                val invites = (invitesState as? UiState.Ready)?.data ?: emptyList()
                when (val s = invitesState) {
                    is UiState.Loading -> AzraelSkeletonList(t["common.loading"])
                    is UiState.Error -> AzraelErrorState(
                        message = s.message,
                        actionLabel = t["action.retry"],
                        onAction = { vm.refreshInvites(true) }
                    )
                    is UiState.Ready -> if (invites.isEmpty()) AzraelEmptyState(title = t["admin.activeNone"])
                }
                invites.forEach { it0 ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(it0.code + (it0.usedBy?.let { t("admin.usedBy", it) } ?: ""), style = MaterialTheme.typography.bodyMedium)
                        Row {
                            IconButton(onClick = {
                                clipboard.setText(AnnotatedString(it0.code))
                                status = t["admin.codeCopied"]
                            }) { Icon(Icons.Filled.ContentCopy, contentDescription = t["action.copy"], tint = MaterialTheme.colorScheme.secondary) }
                            IconButton(onClick = {
                                scope.launch {
                                    when (val r = vm.archive(it0.id)) {
                                        is UiState.Ready -> {
                                            status = t["admin.codeArchive"]
                                            vm.refreshInvites(true)
                                        }
                                        is UiState.Error -> status = r.message
                                        is UiState.Loading -> { /* не используется */ }
                                    }
                                }
                            }) { Icon(Icons.Filled.Archive, contentDescription = t["chats.archive"], tint = MaterialTheme.colorScheme.primary) }
                        }
                    }
                }
                AccentButton(t["admin.showCodeArchive"]) {
                    scope.launch {
                        when (val d = vm.loadInvites(false)) {
                            is UiState.Ready -> status = t("admin.archivedCodes", d.data.size)
                            is UiState.Error -> status = d.message
                            is UiState.Loading -> { /* не используется */ }
                        }
                    }
                }
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
                    IconButton(onClick = { vm.refreshFrozen() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = t["action.refresh"], tint = MaterialTheme.colorScheme.secondary)
                    }
                }
                val frozen = (frozenState as? UiState.Ready)?.data ?: emptyList()
                when (val s = frozenState) {
                    is UiState.Loading -> AzraelSkeletonList(t["common.loading"])
                    is UiState.Error -> AzraelErrorState(
                        message = s.message,
                        actionLabel = t["action.retry"],
                        onAction = { vm.refreshFrozen() }
                    )
                    is UiState.Ready -> if (frozen.isEmpty()) {
                        AzraelEmptyState(title = t["admin.frozen.none"])
                    }
                }
                frozen.forEach { u ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("@${u.username}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        Label(t("admin.frozen.state", u.frozenAt ?: "—"))
                        AccentButton(t("admin.unfreeze", u.username)) {
                            scope.launch {
                                status = t("admin.unfreezing", u.username)
                                when (val r = vm.unfreeze(u.username)) {
                                    is UiState.Ready -> {
                                        status = t("admin.unfrozen", u.username)
                                        vm.refreshFrozen()
                                    }
                                    is UiState.Error -> status = r.message
                                    is UiState.Loading -> { /* не используется */ }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
