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
internal fun ShortenerView(client: AppClient, scrolls: ScrollPositions) {
    val vm = rememberViewModel { ShortenerViewModel(client) }
    val rowsState by vm.rows.collectAsUiState()
    var url by remember { mutableStateOf("") }
    var custom by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var lastShort by remember { mutableStateOf("") }
    var qrCode by remember { mutableStateOf("") }
    var qrError by remember { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    val rows = (rowsState as? UiState.Ready)?.data ?: emptyList()

    LaunchedEffect(Unit) { vm.refresh() }

    val scroll = rememberSectionScroll(scrolls, "shortener")
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize().verticalScroll(scroll)) {
        Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
        ) {
        Text(t["tab.shortener"], style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        IconButton(onClick = { vm.refresh() }) {
            Icon(Icons.Filled.Refresh, contentDescription = t["action.refresh"], tint = MaterialTheme.colorScheme.secondary)
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
                colors = fieldColors(MaterialTheme.colorScheme.primary)
            )
            OutlinedTextField(
                value = custom,
                onValueChange = { custom = it },
                label = { Text(t["short.codeLabel"]) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = fieldColors(MaterialTheme.colorScheme.secondary)
            )
            AccentButton(t["action.create"]) {
                val u = url.trim()
                if (u.isEmpty()) {
                    status = t["short.urlHint"]
                    return@AccentButton
                }
                scope.launch {
                    status = t["short.creating"]
                    when (val r = vm.create(u, custom)) {
                        is UiState.Ready -> {
                            lastShort = r.data
                            qrCode = ""
                            qrError = ""
                            status = t("short.createdOne", lastShort)
                            url = ""
                            custom = ""
                            vm.refresh()
                        }
                        is UiState.Error -> status = r.message
                        is UiState.Loading -> { /* не используется */ }
                    }
                }
            }
            if (status.isNotBlank()) {
                Text(status, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
            }
        }
        }

        if (lastShort.isNotBlank()) {
        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(lastShort, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                AccentButton(t["short.copyLink"]) {
                    clipboard.setText(AnnotatedString(lastShort))
                    status = t["short.linkCopied"]
                }
            }
        }
        }

        when (val s = rowsState) {
            is UiState.Loading -> AzraelSkeletonList(t["common.loading"])
            is UiState.Error -> AzraelErrorState(
                message = s.message,
                actionLabel = t["action.retry"],
                onAction = { vm.refresh() }
            )
            is UiState.Ready -> if (rows.isEmpty()) {
                AzraelEmptyState(title = t["short.none"], subtitle = t["short.none.hint"])
            } else {
                Label(t("short.count", rows.size))
            }
        }

        rows.forEach { r ->
        val shortUrl = "https://azrael-lab.xyz/s/${r.code}"
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(shortUrl, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                Text(r.url, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f), maxLines = 2)
                Label(t("short.clicks", r.clicks ?: 0) + (r.created?.let { " · $it" } ?: ""))
                val shownQr = if (qrCode == r.code) qrBitmap(qrCode) else null
                if (qrCode == r.code && shownQr == null) {
                    Text(qrError.ifBlank { t["short.qrUnavailable"] }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
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
                            when (val q = vm.qr(r.code)) {
                                is UiState.Ready -> {
                                    qrCode = r.code
                                    qrError = if (qrBitmap(q.data) == null) t["short.qrEmpty"] else ""
                                    status = t("short.qr", shortUrl)
                                }
                                is UiState.Error -> {
                                    qrError = q.message
                                    status = qrError
                                }
                                is UiState.Loading -> { /* не используется */ }
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
                            when (val d = vm.delete(r.code)) {
                                is UiState.Ready -> {
                                    status = t["short.deleted"]
                                    vm.refresh()
                                }
                                is UiState.Error -> status = d.message
                                is UiState.Loading -> { /* не используется */ }
                            }
                        }
                    }) { Icon(Icons.Filled.Delete, contentDescription = t["action.delete"], tint = MaterialTheme.colorScheme.error) }
                }
            }
        }
        }
    }
}

// ---- VPN: реальные данные free-VPN/AWG/Incy, ошибки видны ----
