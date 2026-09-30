package xyz.azraellab.shared.data

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import xyz.azraellab.shared.data.model.AiChatDto
import xyz.azraellab.shared.data.model.AwgStatusDto
import xyz.azraellab.shared.data.model.ChatDto
import xyz.azraellab.shared.data.model.ChatMessageDto
import xyz.azraellab.shared.data.model.DeviceDto
import xyz.azraellab.shared.data.model.DeviceListDto
import xyz.azraellab.shared.data.model.FreezeUserDto
import xyz.azraellab.shared.data.model.HealthDto
import xyz.azraellab.shared.data.model.IncysDownloadDto
import xyz.azraellab.shared.data.model.InviteDto
import xyz.azraellab.shared.data.model.InviteSummaryDto
import xyz.azraellab.shared.data.model.PrivacyDto
import xyz.azraellab.shared.data.model.ProfileDto
import xyz.azraellab.shared.data.model.ProvisionInfoDto
import xyz.azraellab.shared.data.model.ShortLinkDto
import xyz.azraellab.shared.data.model.TabConfigDto
import xyz.azraellab.shared.data.model.VpnServerDto
import xyz.azraellab.shared.data.model.VpnSummaryDto

/**
 * Разбор фикстур серверного JSON в DTO. Сеть не нужна: проверяем только
 * сопоставление полей, на котором построен репозиторий.
 */
class ModelsTest {

    private fun textField(label: String, text: String) = buildJsonObject {
        put("label", label)
        put("text", text)
    }

    @Test
    fun profileParsesCoreFields() {
        val o = buildJsonObject {
            put("id", 7L)
            put("uid", "u-123")
            put("username", "az")
            put("display_name", "Азраэль")
            put("gender", "unknown")
            put("role", "admin")
            put("has_avatar", true)
        }
        val p = ProfileDto.from(o)
        assertEquals(7L, p.id)
        assertEquals("u-123", p.uid)
        assertEquals("az", p.username)
        assertEquals("Азраэль", p.displayName)
        assertTrue(p.hasAvatar)
    }

    @Test
    fun profileMissingFieldsHaveDefaults() {
        val p = ProfileDto.from(buildJsonObject {})
        assertEquals(0L, p.id)
        assertEquals("", p.uid)
        assertFalse(p.tgBound)
        assertNull(p.lang)
    }

    @Test
    fun tabConfigParsesTabsWithServerLabel() {
        val o = buildJsonObject {
            putJsonArray("tabs") {
                add(buildJsonObject { put("id", "chats") })
                add(buildJsonObject { put("id", "vpn_tab"); put("label", "vpn"); put("visible", false) })
            }
        }
        val tabs = TabConfigDto.from(o).tabs
        assertEquals(2, tabs.size)
        assertEquals("chats", tabs[0].id)
        assertEquals("chats", tabs[0].serverLabel)
        assertTrue(tabs[0].visible)
        assertEquals("vpn", tabs[1].serverLabel)
        assertFalse(tabs[1].visible)
    }

    @Test
    fun deviceListParsesSummaryAndRowFields() {
        val o = buildJsonObject {
            put("active", 1)
            put("max", 3)
            putJsonArray("devices") {
                add(buildJsonObject {
                    put("devId", "abcd1234")
                    put("label", "Linux")
                    put("platform", "desktop")
                    put("status", "active")
                    put("lastSeen", "2026-09-29")
                    put("rotations", 2)
                })
                add(buildJsonObject {
                    put("devId", "deadbeef")
                    put("status", "revoked")
                })
            }
        }
        val d = DeviceListDto.from(o)
        assertEquals(1, d.active)
        assertEquals(3, d.max)
        assertEquals(2, d.devices.size)
        assertEquals("abcd1234", d.devices[0].devId)
        assertEquals("Linux", d.devices[0].label)
        assertEquals("active", d.devices[0].status)
        assertEquals(2, d.devices[0].rotations)
        assertEquals("revoked", d.devices[1].status)
    }

    @Test
    fun deviceRowFallsBackToPlatformLabel() {
        val o = buildJsonObject {
            put("devId", "x")
            put("platform", "mobile")
            put("status", "active")
        }
        val d = DeviceDto.from(o)
        assertNull(d.label)
        assertEquals("mobile", d.platform)
    }

    @Test
    fun privacyParsesWhoCanFields() {
        val o = buildJsonObject {
            put("hidden_from_search", true)
            put("privacy_who_can_search", "friends")
        }
        val p = PrivacyDto.from(o)
        assertTrue(p.hiddenFromSearch)
        assertEquals("friends", p.whoCanSearch)
        assertEquals("all", p.whoCanWrite)
    }

    @Test
    fun chatDerivesNameFromPartnerAndBoundMine() {
        val o = buildJsonObject {
            put("id", 11L)
            putJsonObject("partner") {
                put("id", 5L)
                put("username", "bob")
            }
            putJsonObject("last") {
                put("text", "привет")
            }
            put("unread", 3)
            put("ts", "2026-09-29T10:00:00.000Z")
        }
        val c = ChatDto.from(o)
        assertEquals(11L, c.id)
        assertEquals("bob", c.name)
        assertEquals(5L, c.partnerId)
        assertEquals("привет", c.lastText)
        assertEquals(3, c.unread)
        assertEquals("2026-09-29T10:00:00", c.ts)

        val msgs = buildJsonObject {
            putJsonArray("messages") {
                add(buildJsonObject { put("id", 1L); put("sender_id", 5L); put("text", "hi") })
                add(buildJsonObject { put("id", 2L); put("sender_id", 5L); put("text", "hi2") })
            }
        }
        val parsed = ChatMessageDto.fromList(msgs, myId = 5L)
        assertEquals(2, parsed.size)
        assertTrue(parsed[0].mine)
        assertEquals("hi", parsed[0].text)
    }

    @Test
    fun shortLinkParsesFullRow() {
        val o = buildJsonObject { put("code", "abc"); put("url", "https://a.example"); put("clicks", 9) }
        val s = ShortLinkDto.from(o)
        assertEquals("abc", s.code)
        assertEquals(9, s.clicks)
    }

    @Test
    fun invitesParseItemsAndSummary() {
        val o = buildJsonObject {
            put("available", 2)
            putJsonArray("codes") {
                add(JsonPrimitive("c1"))
                add(buildJsonObject { put("code", "c2") })
            }
            putJsonArray("items") {
                add(buildJsonObject { put("id", 3L); put("code", "inv1"); put("used_by", "bob") })
            }
        }
        assertEquals(2, InviteSummaryDto.from(o).available)
        assertEquals(listOf("c1", "c2"), InviteSummaryDto.from(o).generatedCodes)
        val list = InviteDto.fromList(o)
        assertEquals(1, list.size)
        assertEquals("inv1", list[0].code)
        assertEquals("bob", list[0].usedBy)
    }

    @Test
    fun frozenUsersParseRows() {
        val o = buildJsonObject {
            putJsonArray("frozen") {
                add(buildJsonObject { put("username", "spam"); put("frozen_at", "2026-09-29") })
            }
        }
        val list = FreezeUserDto.fromList(o)
        assertEquals(1, list.size)
        assertEquals("spam", list[0].username)
        assertEquals("2026-09-29", list[0].frozenAt)
    }

    @Test
    fun vpnParsesServersSummaryAndAwg() {
        val o = buildJsonObject {
            put("alive", 4)
            put("total", 10)
            putJsonArray("servers") {
                add(buildJsonObject {
                    put("flag", "de")
                    put("name", "Berlin")
                    put("host", "de.example")
                    put("port", "51820")
                    put("latency", "42")
                    put("alive", true)
                    put("link", "vpn://..")
                })
            }
        }
        assertEquals(4, VpnSummaryDto.from(o).alive)
        val servers = VpnServerDto.fromList(o)
        assertEquals(1, servers.size)
        assertEquals("Berlin", servers[0].name)
        assertTrue(servers[0].alive)

        val awg = buildJsonObject { put("awgEnabled", true); put("endpoint", "1.1.1.1") }
        val a = AwgStatusDto.from(awg)
        assertTrue(a.awgEnabled)
        assertEquals("1.1.1.1", a.endpoint)
    }

    @Test
    fun incysParsesPlatformRows() {
        val o = buildJsonObject {
            putJsonArray("platforms") {
                add(buildJsonObject { put("name", "Windows"); put("filename", "incys.exe") })
            }
        }
        val list = IncysDownloadDto.fromList(o)
        assertEquals(1, list.size)
        assertEquals("Windows", list[0].name)
        assertEquals("incys.exe", list[0].filename)
    }

    @Test
    fun aiChatsAndHealthAndProvisionParse() {
        val chats = buildJsonObject {
            putJsonArray("items") {
                add(buildJsonObject { put("id", 1L); put("title", "Беседа") })
            }
        }
        assertEquals("Беседа", AiChatDto.fromList(chats).first().title)

        val health = buildJsonObject { put("ok", true); put("gateway", "gw") }
        val h = HealthDto.from(health)
        assertTrue(h.ok)
        assertEquals("gw", h.gateway)

        val prov = buildJsonObject { put("has_key", true) }
        assertTrue(ProvisionInfoDto.from(prov).hasKey)
    }
}