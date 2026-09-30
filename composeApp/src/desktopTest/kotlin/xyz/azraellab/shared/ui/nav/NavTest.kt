package xyz.azraellab.shared.ui.nav

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Навигация — единственное место, где ошибка выглядит как «приложение сломалось»:
 * back-stack теряется при повороте экрана, а незнакомый `tabConfig` роняет UI.
 * Поэтому проверяем логику стека и разбора ссылок, а не картинку.
 */
class NavTest {

    private fun config(
        vararg ids: String,
        defaultTab: String? = null
    ): TabConfig = buildTabConfig(
        tabs = ids.map { ServerTab(id = it, serverLabel = it, visible = true) },
        defaultTab = defaultTab
    )

    // ---- разбор ссылок ----

    @Test
    fun deepLinkOpensChatRoomById() {
        assertEquals(
            Destination.Detail(DetailKind.ChatRoom, "42"),
            parseDestination("messages/42")
        )
    }

    @Test
    fun deepLinkWithoutArgStaysInTab() {
        assertEquals(Destination.Tab(TabSpec.Settings), parseDestination("settings"))
    }

    @Test
    fun deepLinkArgOpensSettingsSubScreen() {
        // Раздел без аргумента — сам раздел, с аргументом — конкретный подэкран.
        assertEquals(
            Destination.Detail(DetailKind.Setting, "devices"),
            parseDestination("settings/devices")
        )
    }

    @Test
    fun deepLinkIsTrimmedAndSchemeStripped() {
        assertEquals(Destination.Tab(TabSpec.Vpn), parseDestination("  vpn_tab/  "))
        assertEquals(Destination.Tab(TabSpec.Shortener), parseDestination("shortener/"))
    }

    @Test
    fun deepLinkOnGarbageReturnsNullInsteadOfThrowing() {
        // Ссылка приходит извне: пустое значение или мусор не должны ронять приложение.
        assertNull(parseDestination(""))
        assertNull(parseDestination("   "))
        assertNull(parseDestination("///"))
        assertNull(parseDestination("settings/settings/extra"))
    }

    @Test
    fun unknownTabIdBecomesPlaceholderNotTab() {
        // Подэкран для неизвестного раздела не выдумываем: только заглушка с его
        // настоящим id, чтобы было видно расхождение с сервером.
        assertEquals(Destination.Placeholder("new_section"), parseDestination("new_section"))
    }

    @Test
    fun bothVpnIdsMapToSameTab() {
        // `vpn_tab` и `vpn` — исторически два id одного раздела.
        assertEquals(Destination.Tab(TabSpec.Vpn), parseDestination("vpn_tab"))
        assertEquals(Destination.Tab(TabSpec.Vpn), parseDestination("vpn"))
    }

    // ---- TabConfig: роли и неизвестные id ----

    @Test
    fun everyServerTabIsKeptEvenIfUnknownToClient() {
        val cfg = config("messages", "billing_v2")
        assertEquals(listOf("messages", "billing_v2"), cfg.items.map { it.id })
        // Неизвестный раздел виден и выбрать его можно — иначе он молча пропадёт.
        assertNotNull(cfg.destinationOf("billing_v2"))
    }

    @Test
    fun defaultTabFallsBackWhenNotGranted() {
        // Сервер может прислать `defaultTab` admin'а, а роль — обычная: показываем
        // первый доступный раздел, а не пустой экран.
        val cfg = config("messages", "settings", defaultTab = "admin")
        assertEquals("messages", cfg.rootId)
    }

    @Test
    fun emptyConfigStillHasUsableRoot() {
        val cfg = buildTabConfig(tabs = emptyList(), defaultTab = "messages")
        assertTrue(cfg.items.isEmpty())
        assertEquals(Destination.Tab(TabSpec.Settings), cfg.root)
    }

    @Test
    fun tabClosedOnServerCannotBeOpened() {
        val cfg = config("messages", "settings")
        assertNull(cfg.destinationOf("admin"))
        assertNull(cfg.destinationOf("nonsense"))
    }

    // ---- back-stack ----

    @Test
    fun backStackSurvivesEncodeDecode() {
        val state = NavState.of(
            Destination.Tab(TabSpec.Messages),
            Destination.Detail(DetailKind.ChatRoom, "7")
        )
        val restored = NavState.decode(state.encode())
        assertNotNull(restored)
        assertEquals(state.stack, restored.stack)
        assertEquals(Destination.Detail(DetailKind.ChatRoom, "7"), restored.current)
    }

    @Test
    fun unknownStackEntriesAreDroppedOnDecode() {
        // Ранняя схема или битая строка не должны ронять запуск: `t:not_a_tab` и
        // `d:Unknown:1` отбрасываются, корень и валидные пункты остаются.
        val restored = NavState.decode("t:not_a_tab|t:messages|d:Unknown:1|t:settings")
        assertNotNull(restored)
        assertEquals(
            listOf(Destination.Tab(TabSpec.Messages), Destination.Tab(TabSpec.Settings)),
            restored.stack
        )
    }

    @Test
    fun placeholderSurvivesDecodeButIsEvictedByRebase() {
        // `x:` — это настоящий пункт (сервер прислал раздел, которого нет в клиенте),
        // поэтому он переживает пересоздание Activity. А «раздел сняли на сервере»
        // закрывает уже `rebase`, у которого есть `tabConfig`.
        val restored = NavState.decode("t:messages|x:billing_v2")
        assertNotNull(restored)
        assertEquals(Destination.Placeholder("billing_v2"), restored.current)
        // `x:` без id — мусор, такой пункт не восстанавливаем.
        assertNull(NavState.decode("x:"))
    }

    @Test
    fun detailWithoutRequiredArgIsDropped() {
        assertNull(NavState.decode("d:ChatRoom:"))
    }

    @Test
    fun blankStackIsRejected() {
        assertNull(NavState.decode(""))
        assertNull(NavState.decode("   "))
        assertNull(NavState.decode("|||"))
    }

    @Test
    fun backFromRootDoesNothing() {
        val state = NavState.of(Destination.Tab(TabSpec.Settings))
        assertFalse(state.canGoBack)
        assertFalse(state.back())
        assertEquals(Destination.Tab(TabSpec.Settings), state.current)
    }

    @Test
    fun backReturnsToPreviousTab() {
        val state = NavState.of(Destination.Tab(TabSpec.Messages))
        state.navigate(Destination.Tab(TabSpec.Settings))
        state.navigate(Destination.Detail(DetailKind.Setting))
        assertEquals(3, state.depth)
        assertTrue(state.back())
        assertEquals(Destination.Tab(TabSpec.Settings), state.current)
        assertTrue(state.back())
        assertEquals(Destination.Tab(TabSpec.Messages), state.current)
        assertFalse(state.back())
    }

    @Test
    fun reselectingOpenTabDoesNotGrowStack() {
        // Раньше переключение туда-обратно теряло позицию скролла: вкладка была
        // единственной выбранной, стека не было. Теперь дублей в стеке не появляется.
        val state = NavState.of(Destination.Tab(TabSpec.Messages))
        state.navigate(Destination.Tab(TabSpec.Settings))
        state.navigate(Destination.Detail(DetailKind.ChatRoom, "1"))
        state.navigate(Destination.Tab(TabSpec.Settings))
        assertEquals(2, state.depth)
        assertEquals(Destination.Tab(TabSpec.Settings), state.current)
    }

    @Test
    fun navigatingToCurrentDestinationIsNoOp() {
        val state = NavState.of(Destination.Tab(TabSpec.Vpn))
        assertFalse(state.navigate(Destination.Tab(TabSpec.Vpn)))
        assertEquals(1, state.depth)
    }

    // ---- Navigator ----

    @Test
    fun navigatorRejectsTabOutsideGrantedConfig() {
        val cfg = config("messages", "settings")
        val navigator = Navigator(NavState.of(cfg.root))
        assertFalse(navigator.openTab(cfg, "admin"))
        assertEquals(Destination.Tab(TabSpec.Messages), navigator.destination)
    }

    @Test
    fun navigatorOpensChatRoomWithArgAndSkipsItWithout() {
        val cfg = config("messages")
        val navigator = Navigator(NavState.of(cfg.root))
        assertTrue(navigator.open(cfg, DetailKind.ChatRoom, "9"))
        assertEquals(Destination.Detail(DetailKind.ChatRoom, "9"), navigator.destination)
        // Без id чата открывать нечего — тихий no-op, а не битый экран.
        assertFalse(navigator.open(cfg, DetailKind.ChatRoom))
        assertEquals(Destination.Detail(DetailKind.ChatRoom, "9"), navigator.destination)
    }

    @Test
    fun navigatorRejectsDetailOfTabClosedForRole() {
        // Роли без «Настроек»: экран настройки — подэкран Settings, а не Messages,
        // поэтому раньше `open(Setting)` пускал туда вообще всех.
        val cfg = config("messages")
        val navigator = Navigator(NavState.of(cfg.root))
        assertFalse(navigator.open(cfg, DetailKind.Setting))
        assertEquals(Destination.Tab(TabSpec.Messages), navigator.destination)
        assertTrue(navigator.open(config("messages", "settings"), DetailKind.Setting))
    }

    @Test
    fun deepLinkNavigatesOnlyInsideGrantedTabs() {
        val cfg = config("messages", "settings")
        val navigator = Navigator(NavState.of(cfg.root))
        assertTrue(navigator.openDeepLink(cfg, "messages/42"))
        assertEquals(Destination.Detail(DetailKind.ChatRoom, "42"), navigator.destination)
        // Админки в tabConfig нет — внешняя ссылка не должна её открывать.
        assertFalse(navigator.openDeepLink(cfg, "admin"))
        assertFalse(navigator.openDeepLink(cfg, "admin/users"))
        assertEquals(Destination.Detail(DetailKind.ChatRoom, "42"), navigator.destination)
        // Права раздела наследует и ссылка в подраздел: без «Настроек» в роли
        // `azrael://settings/devices` закрыт, даже если раздел «Сообщения» открыт.
        assertTrue(navigator.openDeepLink(cfg, "settings/devices"))
        assertEquals(Destination.Detail(DetailKind.Setting, "devices"), navigator.destination)
        val withoutSettings = config("messages")
        assertFalse(navigator.openDeepLink(withoutSettings, "settings/devices"))
        assertTrue(navigator.openDeepLink(withoutSettings, "messages/7"))
    }

    @Test
    fun deepLinkRejectsGarbageAndUnknownTabs() {
        val cfg = config("messages", "settings")
        val navigator = Navigator(NavState.of(cfg.root))
        assertFalse(navigator.openDeepLink(cfg, ""))
        assertFalse(navigator.openDeepLink(cfg, "/"))
        assertFalse(navigator.openDeepLink(cfg, "messages/42/extra"))
        // Неизвестный id — не маршрут: заглушка показывается для вкладки из
        // `tabConfig`, но внешняя ссылка в неё не ведёт.
        assertFalse(navigator.openDeepLink(cfg, "new_section"))
        assertEquals(1, navigator.depth)
    }

    @Test
    fun detailParentTabFollowsKindNotContainer() {
        assertEquals(TabSpec.Settings, Destination.Detail(DetailKind.Setting, "devices").parentTab())
        assertEquals(TabSpec.Messages, Destination.Detail(DetailKind.ChatRoom, "1").parentTab())
        assertEquals(TabSpec.Messages, Destination.Detail(DetailKind.AiChat).parentTab())
    }

    @Test
    fun rebaseKeepsOpenDetailButReplacesRoot() {
        val cfg = config("messages", defaultTab = "messages")
        val navigator = Navigator(NavState.of(cfg.root))
        navigator.open(cfg, DetailKind.ChatRoom, "5")
        val grown = config("messages", "admin", defaultTab = "admin")
        navigator.rebase(grown)
        // Подэкран сохранился, но внизу стека теперь новый корень.
        assertEquals(Destination.Detail(DetailKind.ChatRoom, "5"), navigator.destination)
        assertTrue(navigator.back())
        assertEquals(Destination.Tab(TabSpec.Admin), navigator.destination)
    }

    @Test
    fun rebaseEvictsTabServerClosedForRole() {
        val wide = config("messages", "admin", defaultTab = "admin")
        val navigator = Navigator(NavState.of(wide.root))
        navigator.openTab(wide, "admin")
        // Роль сменилась: админка пропала из tabConfig.
        val narrow = config("messages", "settings", defaultTab = "messages")
        navigator.rebase(narrow)
        assertEquals(Destination.Tab(TabSpec.Messages), navigator.destination)
    }

    @Test
    fun scrollKeysAreStableAcrossTabReordering() {
        // Refresh boot может вернуть разделы в другом порядке: ключ скролла строится
        // из id, а не из позиции, иначе список прыгал бы наверх.
        assertEquals(
            Destination.Tab(TabSpec.Vpn).scrollKey(),
            Destination.Tab(TabSpec.Vpn).scrollKey()
        )
        assertTrue(Destination.Tab(TabSpec.Vpn).scrollKey().contains("vpn_tab"))
    }

    // ---- видимость разделов и позиции скролла ----

    @Test
    fun tabHiddenByServerIsNotInNavBar() {
        // `visible: false` — это «закрыт по роли», а не «неизвестен»: раздел обязан
        // исчезнуть из панели, но его нельзя терять в разборе (иначе неизвестный
        // раздел и закрытый по роли выглядели бы одинаково).
        val cfg = buildTabConfig(
            tabs = listOf(
                ServerTab("messages", "Сообщения", visible = true),
                ServerTab("admin", "Админка", visible = false),
                ServerTab("settings", "Настройки", visible = true)
            ),
            defaultTab = "admin"
        )
        assertEquals(listOf("messages", "settings"), cfg.items.map { it.id })
        // `defaultTab` на закрытый раздел игнорируется — иначе корень был бы невидим.
        assertEquals("messages", cfg.rootId)
        assertNull(cfg.destinationOf("admin"))
    }

    @Test
    fun allTabsHiddenStillHasUsableRoot() {
        val cfg = buildTabConfig(
            tabs = listOf(ServerTab("admin", "Админка", visible = false)),
            defaultTab = "admin"
        )
        assertTrue(cfg.items.isEmpty())
        assertEquals(Destination.Tab(TabSpec.Settings), cfg.root)
    }

    @Test
    fun scrollPositionsSurviveEncodeDecode() {
        // Позиция скролла переживает пересоздание Activity: карта хранится строкой
        // `key=offset`, повреждённые элементы отбрасываются поштучно.
        val positions = ScrollPositions()
        positions["messages.dialogs"] = 812
        positions["settings"] = 0
        val decoded = ScrollPositions.decode(positions.encoded())
        assertEquals(812, decoded["messages.dialogs"])
        assertEquals(0, decoded["settings"])
        assertEquals("812", ScrollPositions.decode("messages.dialogs=812")["messages.dialogs"].toString())
    }

    @Test
    fun brokenScrollOffsetsAreDroppedWithoutLosingTheRest() {
        // Мусор в одной записи не должен обнулять весь список: сохраняется то,
        // что распарсилось, остальное — как будто позиции не было.
        val decoded = ScrollPositions.decode("settings=42,broken,admin=x,shortener=7")
        assertEquals(42, decoded["settings"])
        assertEquals(7, decoded["shortener"])
        // `admin=x` не число — запись отброшена целиком, читается как «позиции нет».
        assertNull(decoded["admin"])
        assertNull(decoded["broken"])
        assertEquals(0, ScrollPositions(decoded)["admin"])
    }

    @Test
    fun scrollWriteOfSameOffsetIsNotAStateChange() {
        // Запись идёт в `onDispose` при каждом уходе с экрана, поэтому одинаковое
        // значение не должно перерисовывать подписанный экран.
        val positions = ScrollPositions()
        positions["admin"] = 10
        positions["admin"] = 10
        assertEquals(10, positions["admin"])
        assertEquals("admin=10", positions.encoded())
    }

    @Test
    fun everyChatKeepsItsOwnScrollPosition() {
        // Комната приходит как `Detail(ChatRoom, id)`, поэтому список диалогов и
        // каждый отдельный чат — разные экраны с разными позициями. Общий ключ
        // (как было, пока комната жила в списке) означал бы, что открытый диалог
        // всегда показывается с начала, а у второго чата позиция первого.
        assertEquals(roomScrollKey(42), roomScrollKey(42))
        assertNotEquals(roomScrollKey(42), roomScrollKey(43))
        // Список и комната не должны делить одну позицию.
        assertNotEquals("messages.dialogs", roomScrollKey(42))
        val positions = ScrollPositions()
        positions["messages.dialogs"] = 100
        positions[roomScrollKey(42)] = 640
        positions[roomScrollKey(43)] = 15
        val decoded = ScrollPositions.decode(positions.encoded())
        assertEquals(100, decoded["messages.dialogs"])
        assertEquals(640, decoded[roomScrollKey(42)])
        assertEquals(15, decoded[roomScrollKey(43)])
    }

    @Test
    fun switchingRoomsUnwindsToPreviousRoomThenToList() {
        // Широкий экран держит список слева, поэтому «назад» из комнаты обязан
        // вернуть предыдущий открытый диалог, а только потом — список. Раньше
        // открытый чат был локальным состоянием, и «назад» его просто закрывал.
        val cfg = config("messages")
        val navigator = Navigator(NavState.of(cfg.root))
        assertTrue(navigator.open(cfg, DetailKind.ChatRoom, "1"))
        assertTrue(navigator.open(cfg, DetailKind.ChatRoom, "2"))
        assertTrue(navigator.back())
        assertEquals(Destination.Detail(DetailKind.ChatRoom, "1"), navigator.destination)
        assertTrue(navigator.back())
        assertEquals(Destination.Tab(TabSpec.Messages), navigator.destination)
        assertFalse(navigator.canGoBack)
    }

    @Test
    fun reopeningRoomFromAnotherRoomDoesNotGrowStack() {
        // A → B → A: возврат к уже открытому диалогу не плодит записи, иначе
        // после него «назад» уводил бы в тот же чат ещё раз.
        val cfg = config("messages")
        val navigator = Navigator(NavState.of(cfg.root))
        navigator.open(cfg, DetailKind.ChatRoom, "1")
        navigator.open(cfg, DetailKind.ChatRoom, "2")
        navigator.open(cfg, DetailKind.ChatRoom, "1")
        assertEquals(2, navigator.depth)
        assertTrue(navigator.back())
        assertEquals(Destination.Tab(TabSpec.Messages), navigator.destination)
    }

    @Test
    fun roomDeepLinkIsUndoableBackToTheList() {
        // `azrael://messages/42` открывает комнату, а не «просто раздел»:
        // у подэкрана есть родитель, и «назад» из него возвращает список.
        val cfg = config("messages")
        val navigator = Navigator(NavState.of(cfg.root))
        assertTrue(navigator.openDeepLink(cfg, "messages/42"))
        assertTrue(navigator.canGoBack)
        assertTrue(navigator.back())
        assertEquals(Destination.Tab(TabSpec.Messages), navigator.destination)
    }

    @Test
    fun roomArgMustBeNumberToShowTheRoom() {
        // Экран рисует комнату только когда `arg` — id чата; мусорный аргумент
        // (`azrael://messages/abc`) оставляет список, а не пустой диалог.
        val room = parseDestination("messages/abc")
        assertEquals(Destination.Detail(DetailKind.ChatRoom, "abc"), room)
        assertNull((room as Destination.Detail).arg?.toLongOrNull())
    }
}
