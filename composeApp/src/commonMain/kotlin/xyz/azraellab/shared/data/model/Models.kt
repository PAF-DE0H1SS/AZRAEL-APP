package xyz.azraellab.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import xyz.azraellab.shared.data.a
import xyz.azraellab.shared.data.b
import xyz.azraellab.shared.data.i
import xyz.azraellab.shared.data.l
import xyz.azraellab.shared.data.o
import xyz.azraellab.shared.data.s

@Serializable
data class ProfileDto(
    val id: Long = 0,
    val uid: String = "",
    val username: String = "",
    @SerialName("display_name") val displayName: String = "",
    val tag: String? = null,
    val gender: String = "",
    val role: String = "standard",
    @SerialName("has_avatar") val hasAvatar: Boolean = false,
    @SerialName("auto_delete_account_days") val autoDeleteDays: Int? = null,
    @SerialName("tg_bound") val tgBound: Boolean = false,
    @SerialName("tg_username") val tgUsername: String? = null,
    val lang: String? = null
) {
    companion object {
        fun from(o: JsonObject): ProfileDto = ProfileDto(
            id = o.l("id") ?: 0,
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
    }
}

@Serializable
data class ServerTabDto(
    val id: String,
    @Transient val serverLabel: String = id,
    val visible: Boolean = true
) {
    companion object {
        fun from(o: JsonObject): ServerTabDto = ServerTabDto(
            id = o.s("id") ?: "",
            serverLabel = o.s("label") ?: o.s("id") ?: "",
            visible = !o.containsKey("visible") || o.b("visible")
        )
    }
}

@Serializable
data class TabConfigDto(val tabs: List<ServerTabDto>) {
    companion object {
        fun from(o: JsonObject?): TabConfigDto = TabConfigDto(
            tabs = o?.a("tabs").orEmpty().mapNotNull { e ->
                (e as? JsonObject)?.let { runCatching { ServerTabDto.from(it) }.getOrNull() }
            }
        )
    }
}

@Serializable
data class DeviceDto(
    val devId: String = "",
    val label: String? = null,
    val platform: String? = null,
    /**
     * Привязан ли этот devId к аккаунту. Сервер для непривязанной установки
     * отдаёт `{ bound: false, devId: null }` — поля `status` там нет вовсе,
     * поэтому `status` без `bound` нельзя трактовать как «состояние».
     */
    val bound: Boolean = false,
    val status: String? = null,
    val lastSeen: String? = null,
    val rotations: Int? = null
) {
    companion object {
        fun from(o: JsonObject): DeviceDto = DeviceDto(
            devId = o.s("devId") ?: "",
            label = o.s("label"),
            platform = o.s("platform"),
            bound = o.s("bound") == "true" || o.s("status") != null,
            status = o.s("status"),
            lastSeen = o.s("lastSeen"),
            rotations = o.i("rotations")
        )

        fun fromList(o: JsonObject): List<DeviceDto> =
            o.a("devices").mapNotNull { e ->
                (e as? JsonObject)?.let { runCatching { from(it) }.getOrNull() }
            }
    }
}

@Serializable
data class DeviceListDto(
    val active: Int = 0,
    val max: Int = 0,
    val devices: List<DeviceDto> = emptyList()
) {
    companion object {
        fun from(o: JsonObject): DeviceListDto = DeviceListDto(
            active = o.i("active") ?: 0,
            max = o.i("max") ?: 0,
            devices = DeviceDto.fromList(o)
        )
    }
}

@Serializable
data class PrivacyDto(
    @SerialName("hidden_from_search") val hiddenFromSearch: Boolean = false,
    @SerialName("privacy_who_can_search") val whoCanSearch: String = "all",
    @SerialName("privacy_who_can_write") val whoCanWrite: String = "all"
) {
    companion object {
        fun from(o: JsonObject): PrivacyDto = PrivacyDto(
            hiddenFromSearch = o.b("hidden_from_search"),
            whoCanSearch = o.s("privacy_who_can_search")?.takeIf { it.isNotBlank() } ?: "all",
            whoCanWrite = o.s("privacy_who_can_write")?.takeIf { it.isNotBlank() } ?: "all"
        )
    }
}

@Serializable
data class ChatDto(
    val id: Long = 0,
    val name: String = "?",
    @Transient val partnerId: Long = 0,
    val lastText: String? = null,
    val unread: Int = 0,
    val ts: String? = null
) {
    companion object {
        fun from(o: JsonObject): ChatDto = ChatDto(
            id = o.l("id") ?: 0,
            name = run {
                val partner = o.o("partner")
                partner?.s("display_name")?.ifBlank { null } ?: partner?.s("username") ?: "?"
            },
            partnerId = o.o("partner")?.l("id") ?: o.o("partner")?.l("uid") ?: 0,
            lastText = o.o("last")?.s("text"),
            unread = o.i("unread") ?: 0,
            ts = o.s("ts")?.take(19)
        )

        fun fromList(o: JsonObject): List<ChatDto> =
            o.a("items").mapNotNull { e ->
                (e as? JsonObject)?.let { runCatching { from(it) }.getOrNull() }
            }
    }
}

@Serializable
data class ChatMessageDto(
    val id: Long = 0,
    val text: String? = null,
    @Transient val mine: Boolean = false,
    @SerialName("file_name") val fileName: String? = null,
    @SerialName("file_token") val fileToken: String? = null,
    @SerialName("created_at") val createdAt: String? = null
) {
    companion object {
        fun from(o: JsonObject, myId: Long): ChatMessageDto {
            val sender = o.l("sender_id") ?: 0
            return ChatMessageDto(
                id = o.l("id") ?: 0,
                text = o.s("text"),
                mine = myId != 0L && sender == myId,
                fileName = o.s("file_name"),
                fileToken = o.s("file_token"),
                createdAt = o.s("created_at")?.take(19)
            )
        }

        fun fromList(o: JsonObject, myId: Long): List<ChatMessageDto> =
            o.a("messages").mapNotNull { e ->
                (e as? JsonObject)?.let { runCatching { from(it, myId) }.getOrNull() }
            }
    }
}

@Serializable
data class ShortLinkDto(
    val code: String = "",
    val url: String = "",
    val clicks: Int? = null,
    val created: String? = null
) {
    companion object {
        fun from(o: JsonObject): ShortLinkDto = ShortLinkDto(
            code = o.s("code") ?: "",
            url = o.s("url") ?: "",
            clicks = o.i("clicks"),
            created = o.s("created")
        )

        fun fromList(o: JsonObject): List<ShortLinkDto> =
            o.a("items").mapNotNull { e ->
                (e as? JsonObject)?.let { runCatching { from(it) }.getOrNull() }
            }
    }
}

@Serializable
data class InviteDto(
    val id: Long = 0,
    val code: String = "",
    val usedBy: String? = null
) {
    companion object {
        fun from(o: JsonObject): InviteDto = InviteDto(
            id = o.l("id") ?: 0,
            code = o.s("code") ?: "",
            usedBy = o.s("used_by")
        )

        fun fromList(o: JsonObject): List<InviteDto> =
            o.a("items").mapNotNull { e ->
                (e as? JsonObject)?.let { runCatching { from(it) }.getOrNull() }
            }
    }
}

@Serializable
data class InviteSummaryDto(
    val available: Int? = null,
    val generatedCodes: List<String> = emptyList()
) {
    companion object {
        fun from(o: JsonObject): InviteSummaryDto = InviteSummaryDto(
            available = o.i("available"),
            generatedCodes = o.a("codes").mapNotNull { e ->
                if (e is JsonPrimitive) e.contentOrNull
                else (e as? JsonObject)?.s("code")
            }
        )
    }
}

@Serializable
data class FreezeUserDto(
    val username: String = "",
    val frozenAt: String? = null
) {
    companion object {
        fun from(o: JsonObject): FreezeUserDto = FreezeUserDto(
            username = o.s("username") ?: "",
            frozenAt = o.s("frozen_at")
        )

        fun fromList(o: JsonObject): List<FreezeUserDto> =
            o.a("frozen").mapNotNull { e ->
                (e as? JsonObject)?.let { runCatching { from(it) }.getOrNull() }
            }
    }
}

@Serializable
data class VpnServerDto(
    val flag: String? = null,
    val name: String = "?",
    val host: String = "?",
    val port: String? = null,
    val latency: String? = null,
    val alive: Boolean = false,
    val link: String? = null
) {
    companion object {
        fun from(o: JsonObject): VpnServerDto = VpnServerDto(
            flag = o.s("flag"),
            name = o.s("name") ?: "?",
            host = o.s("host") ?: "?",
            port = o.s("port"),
            latency = o.s("latency"),
            alive = o.b("alive"),
            link = o.s("link")
        )

        fun fromList(o: JsonObject): List<VpnServerDto> =
            o.a("servers").mapNotNull { e ->
                (e as? JsonObject)?.let { runCatching { from(it) }.getOrNull() }
            }
    }
}

@Serializable
data class VpnSummaryDto(
    val alive: Int = 0,
    val total: Int = 0,
    val subUrl: String? = null
) {
    companion object {
        fun from(o: JsonObject): VpnSummaryDto = VpnSummaryDto(
            alive = o.i("alive") ?: 0,
            total = o.i("total") ?: 0,
            subUrl = o.s("subUrl")
        )
    }
}

@Serializable
data class AwgStatusDto(
    val awgEnabled: Boolean = false,
    val endpoint: String? = null,
    val vpnConfig: String? = null
) {
    companion object {
        fun from(o: JsonObject): AwgStatusDto = AwgStatusDto(
            awgEnabled = o.b("awgEnabled"),
            endpoint = o.s("endpoint"),
            vpnConfig = o.s("vpnConfig")
        )
    }
}

@Serializable
data class IncysDownloadDto(
    val name: String = "?",
    val filename: String = "?"
) {
    companion object {
        fun from(o: JsonObject): IncysDownloadDto = IncysDownloadDto(
            name = o.s("name") ?: "?",
            filename = o.s("filename") ?: "?"
        )

        fun fromList(o: JsonObject): List<IncysDownloadDto> =
            o.a("platforms").mapNotNull { e ->
                (e as? JsonObject)?.let { runCatching { from(it) }.getOrNull() }
            }
    }
}

@Serializable
data class AiChatDto(
    val id: Long = 0,
    val title: String = "?",
    val ts: String? = null,
    @SerialName("last_message") val lastMessage: String? = null,
    @SerialName("msg_count") val msgCount: Int = 0,
    @SerialName("updated_at") val updatedAt: String? = null
) {
    companion object {
        fun from(o: JsonObject): AiChatDto = AiChatDto(
            id = o.l("id") ?: 0,
            title = o.s("title") ?: o.s("name") ?: "?",
            ts = o.s("ts")?.take(19),
            lastMessage = o.s("last_message"),
            msgCount = o.i("msg_count") ?: 0,
            updatedAt = o.s("updated_at")?.take(19)
        )

        fun fromList(o: JsonObject): List<AiChatDto> =
            (o.a("chats") + o.a("items")).mapNotNull { e ->
                (e as? JsonObject)?.let { runCatching { from(it) }.getOrNull() }
            }
    }
}

@Serializable
data class RoomDto(
    val msgs: List<ChatMessageDto> = emptyList(),
    val name: String = "",
    val days: Int? = null
)

@Serializable
data class AiAnswerDto(
    val answer: String = "",
    val model: String = ""
)

@Serializable
data class ProvisionInfoDto(
    val hasKey: Boolean = false
) {
    companion object {
        fun from(o: JsonObject): ProvisionInfoDto = ProvisionInfoDto(
            hasKey = o.b("hasKey") || o.b("has_key") || o.containsKey("key")
        )
    }
}

@Serializable
data class HealthDto(
    val ok: Boolean = false,
    val gateway: String? = null,
    val version: Int? = null
) {
    companion object {
        fun from(o: JsonObject): HealthDto = HealthDto(
            ok = o.b("ok") || (o.i("err") ?: 0) == 0,
            gateway = o.s("gateway"),
            version = o.i("version")
        )
    }
}