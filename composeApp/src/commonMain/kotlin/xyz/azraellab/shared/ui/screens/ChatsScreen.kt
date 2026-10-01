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


/**
 * Раздел «Сообщения»: список диалогов, AI-чат и открытая комната.
 *
 * Комната — это `Destination.Detail(ChatRoom)`, а не локальное состояние, иначе
 * «назад» её не закрывал бы, а ссылка `azrael://messages/42` открывала бы просто
 * раздел. На широком экране список остаётся слева (переключение комнаты по клику),
 * на узком комната занимает весь экран, а её кнопка «назад» уходит в back-stack.
 */
@Composable
internal fun ChatsView(
    client: AppClient,
    profile: AppProfile,
    scrolls: ScrollPositions,
    roomId: Long?,
    onRoomChange: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    var section by remember { mutableStateOf(0) }
    val vm = rememberViewModel { ChatsViewModel(client, profile.id) }
    BoxWithConstraints(modifier) {
        val wide = isWideLayout(maxWidth)
        val detail: (@Composable (Modifier) -> Unit)? = roomId?.let { id ->
            { m: Modifier ->
                ChatRoomSection(
                    vm, scrolls, id, modifier = m,
                    onBack = { onRoomChange(null) }
                )
            }
        }
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
                AzraelDetailLayout(
                    wide = wide,
                    list = { m: Modifier ->
                        ChatsListSection(vm, scrolls, onOpen = { onRoomChange(it) }, modifier = m)
                    },
                    detail = detail,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    empty = { m: Modifier ->
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = m.padding(AzraelSpace.lg)
                        ) { Label(t["chats.select"]) }
                    }
                )
            } else {
                AiChatSection(vm, scrolls, Modifier.weight(1f).fillMaxWidth())
            }
        }
    }
}
@Composable
private fun ChatsListSection(
    vm: ChatsViewModel,
    scrolls: ScrollPositions,
    onOpen: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val chatsState by vm.chats.collectAsUiState()
    val archivedState by vm.archived.collectAsUiState()
    val onlineState by vm.online.collectAsUiState()
    val chats = chatsState.getOrNull() ?: emptyList()
    val archived = archivedState.getOrNull() ?: emptyList()
    val online = onlineState.getOrNull() ?: emptyMap()
    var status by remember { mutableStateOf("") }
    var searchQ by remember { mutableStateOf("") }
    var found by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun refresh() = scope.launch { vm.refresh() }
    LaunchedEffect(Unit) { refresh() }

    val scroll = rememberSectionScroll(scrolls, "messages.dialogs")
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier.verticalScroll(scroll)
    ) {
        if (status.isNotBlank()) {
            Text(status, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
        }

        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Label(t["chats.new"])
                OutlinedTextField(
                    value = searchQ,
                    onValueChange = { searchQ = it },
                    label = { Text(t["login.login.hint"]) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors(MaterialTheme.colorScheme.secondary)
                )
                AccentButton(if (searching) t["chats.searching"] else t["chats.find"]) {
                    val q = searchQ.trim()
                    if (q.length < 2) {
                        status = t["chats.find.hint"]
                        return@AccentButton
                    }
                    scope.launch {
                        searching = true
                        when (val r = vm.search(q)) {
                            is UiState.Ready -> { found = r.data; status = t("chats.found", r.data.size) }
                            is UiState.Error -> status = r.message
                            else -> Unit
                        }
                        searching = false
                    }
                }
                found.forEach { u ->
                    val uid = u.l("id") ?: return@forEach
                    Row(
                        modifier = Modifier.fillMaxWidth().clickableNoRipple {
                            scope.launch {
                                when (val r = vm.createChat(uid)) {
                                    is UiState.Ready -> {
                                        if (r.data != 0L) {
                                            onOpen(r.data)
                                            found = emptyList(); searchQ = ""
                                            status = t["chats.open"]
                                            vm.refresh()
                                        }
                                    }
                                    is UiState.Error -> status = r.message
                                    else -> Unit
                                }
                            }
                        },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(u.s("display_name")?.ifBlank { null } ?: u.s("username") ?: "?", fontWeight = FontWeight.Bold)
                            Text("@${u.s("username") ?: "?"} · uid $uid", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        }
                        Icon(Icons.Filled.PersonAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        when (val s = chatsState) {
            is UiState.Loading -> AzraelSkeletonList(t["common.loading"])
            is UiState.Error -> AzraelErrorState(
                message = s.message,
                actionLabel = t["action.retry"],
                onAction = { scope.launch { vm.refresh() } }
            )
            is UiState.Ready -> if (chats.isEmpty() && archived.isEmpty()) {
                AzraelEmptyState(title = t["chats.none"], subtitle = t["chats.none.hint"])
            } else {
                Label(t("chats.count", chats.size))
            }
        }
        chats.forEach { c ->
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickableNoRipple { onOpen(c.id) },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier.height(8.dp).width(8.dp).background(
                                        if (online[c.partnerId] == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                        CircleShape
                                    )
                                )
                                Text(c.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                            Text(
                                c.lastText ?: t["empty"],
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f),
                                maxLines = 1
                            )
                        }
                        if (c.unread > 0) {
                            Box(
                                modifier = Modifier.background(MaterialTheme.colorScheme.error, CircleShape).padding(horizontal = 8.dp, vertical = 2.dp)
                            // Счётчик лежит на сплошной заливке `error`, и пара «заливка + цифра»
                            // обязана быть именно смысловой — это единственное, что читается
                            // без подписи. Раньше здесь стоял `Color.White`: на тёмной схеме
                            // белый даёт 3.67:1, то есть цифра в счётчике непрочитываема.
                            // `onError` — материаловский «текст поверх danger» и даёт 5.03:1.
                            ) { Text("${c.unread}", color = MaterialTheme.colorScheme.onError, style = MaterialTheme.typography.labelSmall) }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AccentButton(t["chats.archiveSection"]) {
                            scope.launch {
                                val r = vm.archiveChat(c.id)
                                status = if (r is UiState.Ready) t["chats.archived"] else (r as? UiState.Error)?.message ?: ""
                                if (r is UiState.Ready) { vm.refresh(); vm.loadArchived() }
                            }
                        }
                        AccentButton(t["chats.delete"]) {
                            scope.launch {
                                val r = vm.deleteChat(c.id)
                                status = if (r is UiState.Ready) t["chats.deleted"] else (r as? UiState.Error)?.message ?: ""
                                if (r is UiState.Ready) vm.refresh()
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
                        Text(c.lastText ?: t["empty"], maxLines = 1, color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f))
                    }
                    AccentButton(t["chats.restore"]) {
                        scope.launch {
                            when (val r = vm.restore(c.id)) {
                                is UiState.Ready -> status = t["chats.archiveRestore"]
                                is UiState.Error -> status = r.message
                                else -> Unit
                            }
                        }
                    }
                }
            }
        }
        AccentButton(t["chats.showArchive"]) {
            scope.launch {
                when (val r = vm.loadArchived()) {
                    is UiState.Ready -> status = t("chats.archivedCount", r.data.size)
                    is UiState.Error -> status = r.message
                    else -> Unit
                }
            }
        }
    }
}

/**
 * Открытый диалог — отдельный экран, а не хвост списка.
 *
 * Раньше список и комната жили в одной `Column` и переключались условием
 * `openId == null`: на широком экране это означало, что комната занимала место
 * списка целиком, а deep link `azrael://messages/42` открывал просто раздел
 * «Сообщения». Теперь комната приходит как [DetailKind.ChatRoom] с id чата в
 * `arg`, поэтому телефон показывает её вместо списка, а широкий экран держит
 * список слева ([AzraelDetailLayout]) и переключает комнату по клику.
 */
@Composable
private fun ChatRoomSection(
    vm: ChatsViewModel,
    scrolls: ScrollPositions,
    chatId: Long,
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    val roomState by vm.room.collectAsUiState()
    val room = roomState.getOrNull()
    val msgs = room?.msgs ?: emptyList()
    // Имя приходит из списка диалогов; если там этого чата нет (архив, чат удалён
    // между загрузкой списка и открытием) — показываем нейтральный заголовок,
    // а не пустую строку.
    val name = room?.name?.takeIf { it.isNotBlank() } ?: t["chats.room.title"]
    val days = room?.days
    var input by remember { mutableStateOf("") }
    var pendingFile by remember { mutableStateOf<Pair<String, String>?>(null) }
    var status by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    // Ключ с id чата: у каждого диалога своя позиция, и она не зависит от того,
    // открыт ли рядом список. `chats.open` (`chatsOpen`) отмечает чат прочитанным,
    // поэтому перечитывать надо при каждой смене чата, а не только при входе.
    LaunchedEffect(chatId) {
        status = ""
        vm.reloadRoom(chatId)
    }
    val scroll = rememberSectionScroll(scrolls, roomScrollKey(chatId))
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier.verticalScroll(scroll)
    ) {
        if (status.isNotBlank()) {
            Text(status, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            AccentButton(t["vpn.backToList"]) {
                onBack()
            }
            Text(name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = {
                scope.launch {
                    when (val r = vm.reloadMessages(chatId)) {
                        is UiState.Error -> status = r.message
                        else -> Unit
                    }
                }
            }) {
                Icon(Icons.Filled.Refresh, contentDescription = t["action.refresh"], tint = MaterialTheme.colorScheme.secondary)
            }
        }
        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Label(t["chats.autoDelete"])
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Сроки берутся из Repos.AUTODELETE_DAYS: UI и проверка в
                    // Repos.autodeleteSet не должны разъезжаться по набору.
                    val labels = mapOf(1 to t["d1"], 7 to t["d7"], 30 to t["d30"])
                    Repos.AUTODELETE_DAYS.forEach { d ->
                        val title = if (d == null) t["off"] else labels[d] ?: d.toString()
                        AccentButton(if (d == days) "• $title" else title) {
                            scope.launch {
                                when (val r = vm.setAutodelete(chatId, d)) {
                                    is UiState.Ready -> status = if (d == null) t["chats.autoDelete.off"] else t("chats.autoDelete.days", d)
                                    is UiState.Error -> status = r.message
                                    else -> Unit
                                }
                            }
                        }
                    }
                }
            }
        }
        when (val s = roomState) {
            is UiState.Loading -> AzraelSkeletonList(t["common.loading"])
            is UiState.Error -> AzraelErrorState(
                message = s.message,
                actionLabel = t["action.retry"],
                onAction = { scope.launch { vm.reloadRoom(chatId) } }
            )
            is UiState.Ready -> if (msgs.isEmpty()) {
                AzraelEmptyState(title = t["chats.noMessages"], subtitle = t["chats.room.empty.hint"])
            }
        }
        msgs.forEach { m ->
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = if (m.mine) Alignment.CenterEnd else Alignment.CenterStart) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .background(
                            if (m.mine) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f) else MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f),
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
                            Icon(Icons.Filled.AttachFile, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Text(name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary, maxLines = 1)
                        }
                        // Ссылка на вложение сервер отдаёт только для свежего fileToken.
                        m.fileToken?.let { token ->
                            AccentButton(t["short.copyLink"]) {
                                scope.launch {
                                    when (val r = vm.fileUrl(token)) {
                                        is UiState.Ready -> if (!r.data.isNullOrBlank()) {
                                            clipboard.setText(AnnotatedString(r.data))
                                            status = t["short.linkCopied"]
                                        } else status = t["vpn.sub.unavailable"]
                                        is UiState.Error -> status = r.message
                                        else -> Unit
                                    }
                                }
                            }
                        }
                    }
                    m.createdAt?.let {
                        Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f))
                    }
                    if (m.mine) {
                        AccentButton(t["chats.deleteMessage"]) {
                            scope.launch {
                                when (val r = vm.deleteMessage(m.id)) {
                                    is UiState.Ready -> {
                                        vm.updateRoomMessage(m.id)
                                        status = t["chats.messageDeleted"]
                                    }
                                    is UiState.Error -> status = r.message
                                    else -> Unit
                                }
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
                        if (next.isNotBlank()) {
                            scope.launch { vm.typing(chatId) }
                        }
                    },
                    label = { Text(t["chats.message.hint"]) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors(MaterialTheme.colorScheme.primary)
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
                        when (val r = vm.sendMessage(chatId, text.ifBlank { null }, file?.first)) {
                            is UiState.Ready -> {
                                input = ""
                                pendingFile = null
                                vm.reloadMessages(chatId)
                                status = ""
                            }
                            is UiState.Error -> status = r.message
                            else -> Unit
                        }
                        busy = false
                    }
                }
                val pickAttach = rememberFilePicker(listOf("*/*"), 20L * 1024 * 1024) { res ->
                    when (res) {
                        is FilePick.Picked -> {
                            val picked = res.file
                            scope.launch {
                                status = t("file.uploadBusy", picked.name)
                                when (val r = vm.uploadFile(picked.base64, picked.mime, picked.name)) {
                                    is UiState.Ready -> {
                                        pendingFile = r.data to picked.name
                                        status = t["chats.fileReady"]
                                    }
                                    is UiState.Error -> status = r.message
                                    else -> Unit
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
@Composable
private fun AiChatSection(vm: ChatsViewModel, scrolls: ScrollPositions, modifier: Modifier = Modifier) {
    val aiState by vm.aiChats.collectAsUiState()
    val chats = aiState.getOrNull() ?: emptyList()
    var prompt by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }
    var activeId by remember { mutableStateOf<Long?>(null) }
    var model by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    fun refresh() {
        status = ""
        vm.refreshAiChats()
    }
    LaunchedEffect(Unit) { vm.refreshAiChats() }

    val scroll = rememberSectionScroll(scrolls, "messages.ai")
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier.verticalScroll(scroll)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        Text(t["chats.ai"], style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        IconButton(onClick = { refresh() }) { Icon(Icons.Filled.Refresh, contentDescription = t["action.refresh"], tint = MaterialTheme.colorScheme.secondary) }
        }
        if (status.isNotBlank()) {
            Text(status, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
        }
        when (val s = aiState) {
            is UiState.Loading -> AzraelSkeletonList(t["common.loading"])
            is UiState.Error -> AzraelErrorState(
                message = s.message,
                actionLabel = t["action.retry"],
                onAction = { vm.refreshAiChats() }
            )
            is UiState.Ready -> if (chats.isEmpty()) {
                AzraelEmptyState(title = t["ai.history.empty"])
            } else {
                Label(t("chats.count", chats.size))
            }
        }
        chats.forEach { c ->
        GlassCard(modifier = Modifier.fillMaxWidth().clickableNoRipple {
            activeId = if (activeId == c.id) null else c.id
            answer = c.lastMessage ?: answer
        }) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(c.title, fontWeight = FontWeight.Bold)
                Text(
                    c.lastMessage ?: t["chats.noMessagesParens"],
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.65f),
                    maxLines = 2
                )
                Text(
                    t("chats.messagesCount", c.msgCount) +
                    (c.updatedAt?.let { " · $it" } ?: ""),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
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
                colors = fieldColors(MaterialTheme.colorScheme.secondary)
            )
            AccentButton(if (busy) t["ai.thinking"] else t["ai.ask"]) {
                val text = prompt.trim()
                if (text.isEmpty() || busy) return@AccentButton
                busy = true
                status = t["ai.request"]
                scope.launch {
                    when (val r = vm.askAi(text, activeId)) {
                        is UiState.Ready -> {
                            answer = r.data.answer.ifBlank { t["ai.emptyAnswer"] }
                            model = r.data.model
                            prompt = ""
                            status = t["ai.ready"]
                            vm.refreshAiChats()
                        }
                        is UiState.Error -> status = r.message
                        else -> Unit
                    }
                    busy = false
                }
            }
        }
        }
    }
}

// ---- Сократитель ссылок: создание, QR, копирование, удаление ----

