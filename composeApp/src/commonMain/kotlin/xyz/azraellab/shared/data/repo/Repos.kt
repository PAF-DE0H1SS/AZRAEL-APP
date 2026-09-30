package xyz.azraellab.shared.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import xyz.azraellab.shared.core.api.AppClient
import xyz.azraellab.shared.data.model.AiAnswerDto
import xyz.azraellab.shared.data.model.AiChatDto
import xyz.azraellab.shared.data.model.AwgStatusDto
import xyz.azraellab.shared.data.model.ChatDto
import xyz.azraellab.shared.data.model.ChatMessageDto
import xyz.azraellab.shared.data.model.DeviceListDto
import xyz.azraellab.shared.data.model.DeviceDto
import xyz.azraellab.shared.data.model.FreezeUserDto
import xyz.azraellab.shared.data.model.HealthDto
import xyz.azraellab.shared.data.model.IncysDownloadDto
import xyz.azraellab.shared.data.model.InviteDto
import xyz.azraellab.shared.data.model.InviteSummaryDto
import xyz.azraellab.shared.data.model.PrivacyDto
import xyz.azraellab.shared.data.model.ProfileDto
import xyz.azraellab.shared.data.model.ProvisionInfoDto
import xyz.azraellab.shared.data.model.RoomDto
import xyz.azraellab.shared.data.model.ShortLinkDto
import xyz.azraellab.shared.data.model.TabConfigDto
import xyz.azraellab.shared.data.model.VpnServerDto
import xyz.azraellab.shared.data.model.VpnSummaryDto

@Serializable
data class HomeDto(
    val profile: ProfileDto = ProfileDto(),
    val lang: String? = null,
    val tabConfig: TabConfigDto = TabConfigDto(emptyList())
)

object Repos {

    suspend fun homeBoot(client: AppClient): UiState<HomeDto> = runStateIO {
        val boot = client.homeBoot()
        HomeDto(
            profile = ProfileDto.from(boot.o("profile") ?: JsonObject(emptyMap())),
            lang = boot.s("lang") ?: boot.o("profile")?.s("lang"),
            tabConfig = TabConfigDto.from(boot.o("tabConfig"))
        )
    }

    suspend fun devices(client: AppClient): UiState<DeviceListDto> = runStateIO {
        DeviceListDto.from(client.deviceList())
    }

    suspend fun currentDevice(client: AppClient): UiState<DeviceDto> = runStateIO {
        DeviceDto.from(client.deviceStatus())
    }

    suspend fun privacy(client: AppClient): UiState<PrivacyDto> = runStateIO {
        PrivacyDto.from(client.profilePrivacyGet().o("privacy") ?: JsonObject(emptyMap()))
    }

    suspend fun chats(client: AppClient): UiState<List<ChatDto>> = runStateIO {
        ChatDto.fromList(client.chatsList())
    }

    suspend fun archivedChats(client: AppClient): UiState<List<ChatDto>> = runStateIO {
        ChatDto.fromList(client.chatsListArchived())
    }

    suspend fun messages(client: AppClient, chatId: Long, myId: Long, limit: Int? = 50): UiState<List<ChatMessageDto>> = runStateIO {
        ChatMessageDto.fromList(client.chatsMessages(chatId, limit = limit), myId)
    }

    suspend fun searchUsers(client: AppClient, q: String): UiState<List<JsonObject>> = runStateIO {
        client.searchUsers(q).a("users").mapNotNull { runCatching { it.jsonObject }.getOrNull() }
    }

    suspend fun chatsPresence(client: AppClient, ids: List<Long>): UiState<Map<Long, Boolean>> = runStateIO {
        client.chatsPresence(ids).o("online")?.entries.orEmpty()
            .mapNotNull { e -> e.key.toLongOrNull()?.let { id -> id to ((e.value as? JsonPrimitive)?.content?.toBooleanStrictOrNull() ?: false) } }
            .toMap()
    }

    suspend fun createChat(client: AppClient, uid: Long): UiState<Long> = runStateIO {
        client.chatsCreate(uid.toString()).o("chat")?.l("id") ?: 0L
    }

    /** Открыть диалог: сообщения, имя собеседника и срок автоудаления. */
    suspend fun openChat(client: AppClient, chatId: Long, myId: Long): UiState<RoomDto> = runStateIO {
        val d = client.chatsOpen(chatId)
        RoomDto(
            msgs = ChatMessageDto.fromList(d, myId),
            name = ChatDto.fromList(d).firstOrNull()?.name ?: "",
            days = client.chatsAutodeleteGet(chatId).i("days")
        )
    }

    suspend fun autodeleteSet(client: AppClient, chatId: Long, days: Int?): UiState<Int?> = runStateIO {
        client.chatsAutodeleteSet(chatId, days)
        days
    }

    suspend fun sendMessage(client: AppClient, chatId: Long, text: String?, fileToken: String?): UiState<Unit> = runStateIO {
        client.chatsSend(chatId, text = text, fileToken = fileToken)
    }

    suspend fun deleteMessage(client: AppClient, id: Long): UiState<Unit> = runStateIO {
        client.chatsDelete(id)
    }

    suspend fun archiveChat(client: AppClient, id: Long): UiState<Unit> = runStateIO {
        client.chatsArchive(id)
    }

    suspend fun restoreChat(client: AppClient, id: Long): UiState<Unit> = runStateIO {
        client.chatsRestore(id)
    }

    suspend fun deleteChat(client: AppClient, id: Long): UiState<Unit> = runStateIO {
        client.chatsDeleteChat(id)
    }

    suspend fun typing(client: AppClient, chatId: Long): UiState<Unit> = runStateIO {
        client.chatsTyping(chatId)
    }

    suspend fun chatFileUpload(client: AppClient, base64: String, mime: String, name: String): UiState<String> = runStateIO {
        client.chatsFileUpload(base64, mime, name).s("fileToken") ?: ""
    }

    suspend fun chatFileUrl(client: AppClient, fileToken: String): UiState<String> = runStateIO {
        client.chatsFileUrl(fileToken).s("url") ?: ""
    }

    suspend fun aiChatAsk(client: AppClient, text: String, chatId: Long?): UiState<AiAnswerDto> = runStateIO {
        val d = client.aiChatSend(text, chatId)
        AiAnswerDto(answer = d.s("answer") ?: "", model = d.s("model") ?: "")
    }

    suspend fun shortLinks(client: AppClient): UiState<List<ShortLinkDto>> = runStateIO {
        ShortLinkDto.fromList(client.shortenerList())
    }

    suspend fun shortenerCreate(client: AppClient, url: String, custom: String?): UiState<String> = runStateIO {
        val d = client.shortenerCreate(url, custom)
        d.s("shortUrl") ?: d.s("url") ?: url
    }

    suspend fun shortenerDelete(client: AppClient, code: String): UiState<Unit> = runStateIO {
        client.shortenerDelete(code)
        Unit
    }

    suspend fun shortenerQr(client: AppClient, code: String): UiState<String> = runStateIO {
        client.shortenerQr(code).s("qr") ?: ""
    }

    suspend fun invites(client: AppClient, active: Boolean): UiState<List<InviteDto>> = runStateIO {
        InviteDto.fromList(client.invitesList(active))
    }

    suspend fun myInviteCode(client: AppClient): UiState<String> = runStateIO {
        client.invitesMyCode().s("code") ?: ""
    }

    suspend fun generateInvites(client: AppClient): UiState<InviteSummaryDto> = runStateIO {
        InviteSummaryDto.from(client.invitesGenerate())
    }

    suspend fun vpnServers(client: AppClient, folder: String?): UiState<List<VpnServerDto>> = runStateIO {
        VpnServerDto.fromList(client.vpnFreeServers(folder))
    }

    suspend fun vpnSummary(client: AppClient): UiState<VpnSummaryDto> = runStateIO {
        VpnSummaryDto.from(client.vpnFreeSummary())
    }

    suspend fun vpnRefresh(client: AppClient): UiState<VpnSummaryDto> = runStateIO {
        VpnSummaryDto.from(client.vpnFreeRefresh())
    }

    suspend fun awgStatus(client: AppClient): UiState<AwgStatusDto> = runStateIO {
        AwgStatusDto.from(client.vpnAwgStatus())
    }

    suspend fun incysDownloads(client: AppClient): UiState<List<IncysDownloadDto>> = runStateIO {
        IncysDownloadDto.fromList(client.vpnIncysDownloads())
    }

    suspend fun frozenUsers(client: AppClient): UiState<List<FreezeUserDto>> = runStateIO {
        FreezeUserDto.fromList(client.adminFreezeList())
    }

    suspend fun unfreezeUser(client: AppClient, username: String): UiState<Unit> = runStateIO {
        client.adminUnfreeze(username)
    }

    suspend fun archiveInvite(client: AppClient, id: Long): UiState<Unit> = runStateIO {
        client.invitesArchive(id)
    }

    suspend fun aiChats(client: AppClient): UiState<List<AiChatDto>> = runStateIO {
        AiChatDto.fromList(client.aiChatList())
    }

    suspend fun provisionInfo(client: AppClient): UiState<ProvisionInfoDto> = runStateIO {
        ProvisionInfoDto.from(client.provisionInfo())
    }

    suspend fun systemHealth(client: AppClient): UiState<HealthDto> = runStateIO {
        HealthDto.from(client.systemHealth())
    }

    // --- settings (profile / privacy / devices / provision / otp) ---

    suspend fun profileAvatarUrl(client: AppClient): UiState<String> = runStateIO {
        client.profileAvatarUrl().s("url") ?: ""
    }

    suspend fun provisionKeyShow(client: AppClient): UiState<String> = runStateIO {
        client.provisionKey().s("key") ?: ""
    }

    suspend fun provisionRegen(client: AppClient): UiState<String> = runStateIO {
        client.provisionRegenerate().s("key") ?: ""
    }

    suspend fun privacySave(client: AppClient, hidden: Boolean, whoSearch: String, whoWrite: String): UiState<Unit> = runStateIO {
        client.profilePrivacySet(
            mapOf(
                "hiddenFromSearch" to hidden,
                "whoCanSearch" to whoSearch,
                "whoCanWrite" to whoWrite
            )
        )
        Unit
    }

    suspend fun deviceRevoke(client: AppClient, devId: String): UiState<Unit> = runStateIO {
        client.deviceRevoke(devId)
        Unit
    }

    suspend fun deviceRotate(client: AppClient): UiState<Unit> = runStateIO {
        client.rotateDeviceKey()
        Unit
    }

    suspend fun deviceStatusLine(client: AppClient): UiState<String> = runStateIO {
        client.deviceStatus().s("status") ?: "?"
    }

    suspend fun otpSecret(client: AppClient): UiState<String> = runStateIO {
        client.otpGenerateSecret().s("secret") ?: ""
    }

    suspend fun otpCheck(client: AppClient, code: String): UiState<Boolean> = runStateIO {
        client.otpValidate(code).b("ok")
    }

    suspend fun passwordChange(client: AppClient, old: String, new: String): UiState<Unit> = runStateIO {
        client.changePassword(old, new)
        Unit
    }

    suspend fun sessionRevoke(client: AppClient): UiState<Unit> = runStateIO {
        client.revokeSession()
        Unit
    }

    /** Сессия: "имя пользователя · роль". */
    suspend fun verifySession(client: AppClient): UiState<String> = runStateIO {
        val d = client.verify()
        (d.s("username") ?: "?") + " · " + (d.s("role") ?: "?")
    }

    /** Сырой ответ system.health (для отладочной карточки канала). */
    suspend fun healthText(client: AppClient): UiState<String> = runStateIO {
        client.systemHealth().toString().take(120)
    }

    suspend fun profileSave(client: AppClient, name: String?, tag: String?, gender: String?): UiState<Unit> = runStateIO {
        client.profileUpdate(displayName = name, tag = tag, gender = gender)
        Unit
    }

    suspend fun profileSetLang(client: AppClient, code: String): UiState<Unit> = runStateIO {
        client.profileSetLang(code)
        Unit
    }

    suspend fun autoDeleteSet(client: AppClient, days: Int?): UiState<Int?> = runStateIO {
        client.profileAutoDelete(days)
        days
    }

    suspend fun profileAvatarSet(client: AppClient, base64: String, mime: String): UiState<String> = runStateIO {
        client.profileAvatarSet(base64, mime)
        client.profileAvatarUrl().s("url") ?: ""
    }

    suspend fun profileDelete(client: AppClient): UiState<Unit> = runStateIO {
        client.profileDelete()
        Unit
    }
}