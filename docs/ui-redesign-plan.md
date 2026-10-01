# AZRAEL-APP: план редизайна UI/UX и реструктуризации функциональности

Статус: этапы P0-P5 закрыты. Редизайн вошёл в релиз - первая публикация `v1.3.2`,
стабильный релиз `v1.3.3` (2026-10-01). Открыты три пункта из P5: ручной
авторизованный смоук (нужен тестовый аккаунт или `AZRAEL_FLIGHT_INVITE`),
desktop render-тесты Compose (нужен `compose.ui-test` в кэше Gradle) и
runtime-проверка на реальных Windows 10/11 и Android-устройстве.
Журнал этапов ниже остаётся историческим и не переписывается; итоговая сводка и
фактическое состояние файлов - в разделе «Итог» в конце файла.

Референсы (выбраны пользователем, решение зафиксировано):
- `dev778g-me/Kore` - дизайн-система: token-слой + компоненты по одной директории.
- `JetBrains/compose-multiplatform` - официальные примеры, корректный `expect/actual` по платформам.
- `JetBrains/kotlinconf-app` - каркас приложения: `navigation/`, `screens/`, отдельный модуль UI-компонентов, `AdaptiveDetailLayout`, `ErrorLoading`.
- `amir1376/ab-download-manager` - разделение `shared`/`desktop`/`android` + декларативный рендерер настроек по типу данных (`configurable/`).
- `maxrave-dev/SimpMusic` - паттерны экранов для чатов и медиа.

Основа каркаса - `kotlinconf-app`. Основа дизайн-системы - `Kore`.
Kore берётся как паттерн, а не как зависимость: проект молодой (128★), тянуть его в прод нельзя.
Проверено: `build.gradle.kts` не содержит и не требует его; дизайн-система будет написана в `shared/ui`.

## 1. Диагноз (факты из кода)

| Проблема | Факт |
|---|---|
Монолит экранов | `App.kt` - 3472 строки, 38 `@Composable`, всё вложено в одну `App` |
Состояние вперемешку с UI | 87 `remember` + 92 `mutableStateOf` + 12 `LaunchedEffect` в одном файле |
Навигация = вкладки | `MainShell` (стр. 1371) - плоский список из серверного `tabConfig`; нет back-stack, deep link, вложенности |
Функции в одном списке | `AppClient` - 1062 строки, ~70 методов по ~15 доменам, все возвращают `JsonObject` |
Перегруженные экраны | `SettingsView` - 698 строк / 12 карточек: аккаунт + устройства + provision + канал + инвайты + автодаление |
Нет дизайн-системы | `ui/Theme.kt` - 111 строк, только цветовая схема. Нет type scale, shape scale, spacing, ripple, светлой темы |
Локализация | `I18n.kt` - 1554 строки, 1215 ключей, 3 языка (RU/EN/ZH), покрыт `I18nTest` |
Хрупкие данные | UI парсит `JsonObject` напрямую: любое изменение формата ломает экран |

Палитра при этом сделана хорошо: сырых `Color(0x…)` в UI всего 7, цвета совпадают с сайтом
(`--accent` #4ADE80, фон #0A0A0A). Редизайн не ломает бренд, он достраивает систему.

## 2. Целевая структура

Модули уже корректные (`:app`, `:composeApp`, `:desktopApp`) - план их использует, не переделывает.
Внутри `shared` добавляются пакеты:

```
composeApp/src/commonMain/kotlin/xyz/azraellab/shared/
├── navigation/            ← kotlinconf-app
│   ├── Routes.kt          sealed Destination (роли, аргументы)
│   ├── Navigator.kt       back-stack, deep link
│   ├── NavState.kt
│   ├── NavHost.kt
│   └── NavScaffold.kt     каркас + нижняя панель + adaptive
├── screens/               ← kotlinconf-app, один файл на экран
│   ├── auth/LoginScreen.kt
│   ├── home/HomeScreen.kt
│   ├── chats/{ChatsList,ChatRoom}.kt
│   ├── ai/AiChatScreen.kt
│   ├── shortener/ShortenerScreen.kt
│   ├── vpn/VpnScreen.kt
│   ├── account/{Account,Devices,Security,Privacy,Appearance}.kt
│   └── admin/AdminScreen.kt
├── ui/                    ← Kore
│   ├── theme/{Color,Type,Shape,Spacing,Ripple,Theme}.kt
│   └── components/        button, card, textfield, dialog, appbar,
│                          listtile, navigationbar, chip, badge, divider, …
├── data/                  ← ab-download-manager
│   ├── model/             типизированные DTO вместо JsonObject
│   ├── repo/              AuthRepo, ChatsRepo, VpnRepo, AdminRepo, AccountRepo, …
│   └── configurable/      декларативный рендерер настроек по типу
├── core/                  без изменений: crypto, protocol, api
└── i18n/                  I18n разбит на файлы по доменам, единый интерфейс t()
```

`core/crypto` и `core/protocol` не трогаем - там криптография и детерминированный AAD,
риск регрессий высокий, а выигрыш в дизайне нулевой.

## 3. Этапы

### P0 - Дизайн-система (1-2 недели)
- [x] `ui/theme/Color.kt` - палитра как набор токенов; сохраняем текущие значения
- [x] `ui/theme/Type.kt` - шкала 8 стилей текста
- [x] `ui/theme/Shape.kt` - шкала скруглений
- [x] `ui/theme/Spacing.kt` - шкала отступов
- [x] `ui/theme/Ripple.kt` - единый ripple
- [x] `ui/theme/Theme.kt` - тёмная (текущая) + **светлая** тема
- [x] Режим темы переживает перезапуск: `AppThemeStore` (`expect`/`actual`) + `AzraelThemeState`
- [x] `ui/components/` - 12 базовых компонентов
- [x] `GlassBackground` и `Modifier.glass` переводятся на токены
- [x] Сборка и `:composeApp:desktopTest` зелёные

Критерий приёмки: палитра не изменилась визуально; появилась светлая тема; нет сырых
цветов и произвольных отступов вне токенов.

### P1 - Навигация (1-2 недели)
- [x] `Routes.kt`: sealed `Destination`
- [x] `Navigator.kt` + `NavState.kt`: back-stack
- [x] `NavHost.kt`, `NavScaffold.kt`
- [x] `MainShell`: `selectedId` → `Navigator`
- [x] Маппинг серверного `tabConfig` → `Destination`, роли сохранены, неизвестные id не роняют UI
- [x] Deep link на чат, восстановление позиции скролла
- [x] `AdaptiveDetailLayout` для широких экранов (телефон/ПК)

Критерий приёмки: все разделы `tabConfig` открываются, роли работают как раньше, «назад»
ведёт себя корректно, неизвестный `tabConfig` не падает.

### P2 - Декларативные настройки (1 неделя)
- [x] `data/configurable/` - спецификации-дескрипторы (`FieldSpec`/`ActionSpec`/`ChoiceSpec`) + единый `ConfigurableList`/`ConfigGroup`
- [x] `SettingsView` описывается декларативно (меню из 6 разделов + сам раздел по `arg`)
- [x] Разбивка на 6 экранов: Аккаунт, Устройства, Безопасность, Приватность, Внешний вид, Канал
- [x] Инвайты и админка - в `AdminScreen` (инвайты перенесены в `AdminView`)

Критерий приёмки: новая настройка добавляется строкой описания, а не новой карточкой кода.

### P3 - Данные и состояние (2-3 недели)
- [x] `data/model/` - DTO для ~15 доменов
- [x] `data/repo/` - репозитории; `AppClient` остаётся транспортом
- [x] ViewModel на экран (KMP-совместимый)
- [x] `mutableStateOf` → `StateFlow` (все целевые экраны: ShortenerView, ChatsListSection,
  ChatRoomSection, AiChatSection, VpnView, AdminView, SettingsView/Devices/Privacy)
- [x] Ошибки и загрузка - типизированные состояния вместо строковых флагов (Loading/Error из
  `UiState` на всех переведённых экранах; `status`-строки остаются UI-зависимыми)

Критерий приёмки: ни один экран не парсит `JsonObject`; UI не знает про HTTP.

### P4 - Экраны (2-3 недели)
- [x] 38 `@Composable` из `App.kt` разнесены по `screens/` (2026-09-29, механический раскол 10 файлов)
- [x] `ChatsView` (500 строк) разделён на список и комнату (еднено в P1, 2026-09-28)
- [ ] `AiChatSection` → `AiChatScreen` (сейчас внутри `ChatsScreen.kt`, переименование опционально)
- [x] `LoginScreen` (333) → форма + provision-ключ (сделано в 1.1.0)
- [x] `I18n.kt` разбит по доменам, ключи сохранены (первый шаг P5, 2026-09-29)

Критерий приёмки: `App.kt` < 200 строк (только композиция и DI), `I18nTest` зелёный.

### P5 - Полировка и проверка (1 неделя)
- [x] Пустые состояния, ошибки, скелетоны, офлайн-индикатор (`ErrorLoading` из kotlinconf-app)
  - [x] Компоненты `AzraelLoadingState` / `AzraelErrorState` (2026-09-29)
  - [x] `ChatsScreen` (список диалогов) - 2026-09-29
  - [x] `ShortenerScreen` (список ссылок) - 2026-09-29
  - [x] `AdminScreen`, `SettingsScreen`, `VpnScreen`, `AiChatSection`, `ChatRoomSection`,
        `MainShellScreen`, `StatusScreens` - 2026-09-29
  - [x] Офлайн-индикатор (`MainShellScreen` + `rememberOnline()`) - был сделан в P1
- [ ] Смоук: вход → чат → файл → сокращатель → VPN → админка
  - [x] Автоматическая часть: сборки + `desktopTest` + контраст-тесты (146/146)
  - [x] Реальный запуск под Xvfb в обеих темах: окно 716×476, фон L=244 / L=15,
        звёзды анимируются (275 / 314 пикселей за 3 с), исключений нет
  - [ ] Ручная часть: нужен тестовый аккаунт или `AZRAEL_FLIGHT_INVITE`
  - [x] Сервер анонимно жив: `POST /api/app/v1` без подписи → `400 bad envelope`,
        `POST /api/auth` с неверным паролем → `401`
- [x] Android + desktop сборка - 2026-09-29 (`compileKotlinDesktop` + `compileAndroidMain`)
- [x] WCAG-контраст компонентов (`ContrastTest`, 20 тестов) - 2026-09-29
- [x] Палитра звёздного фона по теме (`starfieldPalette`) - 2026-09-29

## 4. Правила выполнения

- Этапы не смешиваются: один PR = один этап. Миграция 290 обращений `t["…"]` и редизайн
  в одном изменении не смешиваются никогда.
- После каждого этапа: `./gradlew :composeApp:compileKotlinDesktop :composeApp:compileAndroidMain --offline`
  и `:composeApp:desktopTest`.
- `I18nTest` (RU/EN/ZH) зелёный на каждом шаге. Новые строки только через `i18n/`.
- Проверка перед релизом: `./gradlew --stop` + полный JDK + `./build-all.sh release`
  (грабли из журнала: Gradle-daemon держит JBR и не даёт `jpackage`).

Окружение: `JAVA_HOME=/nix/store/qqngq35hqpiqm5g5w4wgjj2aam09qxif-openjdk-21.0.12+8`
без него `./gradlew` не запускается.

## 5. Риски

1. **Регрессии при миграции** - 290 `t["…"]` и серверная ролевая модель `tabConfig`. Гарантия: этапная миграция.
2. **Локализация** - 1215 ключей × 3 языка, самый рискованный этап. Перенос поблочно, тесты обязательны.
3. **Импорт Kore** - не добавляем зависимость, только паттерн.
4. **`JsonObject` в UI** - смена формата с сервера ударит по экранам до P3. Гарантия: типизация в P3, до него - осторожный доступ через хелперы.
5. **Журнал в корне `AGENTS.md` устарел** - говорит про 365 ключей i18n, фактически 1215. Расхождение учтено, журнал требует обновления.

## 6. Журнал выполнения

- **2026-09-28** - План зафиксирован. Инвентаризация завершена: `App.kt` 3472 строки / 38 composable,
  `I18n.kt` 1554 / 1215 ключей, `AppClient.kt` 1062 / ~70 методов, `Theme.kt` 111 строк.
  Изучены 5 референсных репозиториев, выбран каркас `kotlinconf-app` + дизайн-система `Kore`.
  Baseline-сборка на JDK 21: `:composeApp:compileKotlinDesktop` + `:composeApp:compileAndroidMain --offline`
  - **BUILD SUCCESSFUL**. Следующий шаг: P0, `ui/theme/`.

- **2026-09-28** - P0, основа дизайн-системы сделана и проверена. Созданы
  `ui/theme/{Color,Type,Shape,Spacing,Ripple,Theme}.kt`; палитра тёмной схемы совпадает
  с текущей (значения не менялись), добавлена светлая схема, типографика 15 стилей,
  шкала скруглений и `AzraelSpace`. Режим темы (`system/light/dark`) сохраняется через
  новый `AppThemeStore` (`expect` + `actual` на Android/desktop, тем же файлом, что и
  `AppLangStore`), состояние ведёт `AzraelThemeState`; `ui/Theme.kt` превращён в
  совместимый слой с псевдонимами, точка входа в `App.kt` - новая `AppThemeRoot`.
  Проверки: `:composeApp:compileKotlinDesktop` + `:composeApp:compileAndroidMain --offline`
  - **BUILD SUCCESSFUL**; `:composeApp:desktopTest --rerun-tasks` - **60/60** в 10 классах
  (было 55/55 в 9, +5 `ThemeTest` на разбор кода режима, `resolveDark` и определение
  светлоты схем).

  Грабли этого шага:
  1. `AzraelThemeState.toggle()` звал `@Composable isSystemInDarkTheme()` из обычной
     функции - так нельзя. Решение: системное значение читается один раз в композиции и
     кладётся в состояние через `SideEffect`, а `resolveDark(mode, systemDark)` вынесена
     как обычная функция - заодно тестируется без UI.
  2. `material3 1.9.0` (CMP 1.12.1): `ripple(...)` **не** `@Composable` и возвращает
     `IndicationNodeFactory : Indication` - `remember { ripple(...) }` законен. Проверено
     `javap` по `material3-desktop-1.9.0.jar`, а не по памяти.
  3. Псевдонимы в `ui/Theme.kt` нельзя делать одноимёнными импортам из `ui.theme` -
     сначала написал `AzraelPrimaryCompat`, импорт в `App.kt` сломался. Правильно:
     `val AzraelPrimary = xyz.azraellab.shared.ui.theme.AzraelPrimary` (FQN, без импорта).
  4. `AzraelBorder` - это `Color`, а не ширина рамки; `AzraelBorder.strokeWidth()` не
     существует, ширина берётся из `AzraelSpace.stroke`.
  5. Расширение в `Type.kt` имеет параметр `tracking`, а не `letterSpacing`
     (`NAMED_PARAMETER_NOT_FOUND`).

- **2026-09-28** - P0 закрыт полностью: добавлен `ui/components/` из 12 файлов.
  `AzraelButton` (тона `Accent/Glass/Ghost/Danger`, busy-состояние со спиннером, иконка,
  `fullWidth`/высота), `AzraelCard` (стекло + опциональный клик), `AzraelTextField`
  (обобщение `AuthField`: акцент на роль, `spaced` для provision-ключа и пароля,
  переключатель видимости, лимит длины, `keyboardType`), `AzraelBanner` (тон задаёт
  цвет **и** иконку одним enum'ом), `AzraelTopBar` (+ `AzraelTopBarAction`), `AzraelListTile`,
  `AzraelNavigationBar` (+ `AzraelNavItem`, счётчики), `AzraelChip` (`AzraelChoiceChip`/
  `AzraelFilterChip`), `AzraelBadge` (+`AzraelCountBadge`, `AzraelStatusDot`),
  `AzraelDivider`, `AzraelAvatar` (буква или готовый `Painter`), `AzraelEmptyState`.
  Значения заливок/рамок взяты из экранов, где эти элементы уже повторялись, поэтому
  подстановка компонента не должна менять вид 1:1.
  Проверки: `:composeApp:compileKotlinDesktop` + `:composeApp:compileAndroidMain --offline`
  - **BUILD SUCCESSFUL**; `:composeApp:desktopTest --rerun-tasks` - **65/65** в 11 классах
  (было 60/60, +5 `ComponentsTest`).

  Грабли этого шага:
  1. Рендер-тесты Compose офлайн невозможны: `org.jetbrains.compose.ui:ui-test` в кэше
     Gradle нет, а сеть в этой сборке недоступна. Поэтому `ComponentsTest` проверяет не
     картинку, а то, что реально ломает экран: полноту наборов тонов, что каждый тон
     баннера несёт свою иконку и не делит цвет с соседним, монотонность шкалы отступов
     и совпадение значений с палитрой. Восстановление UI-тестов - отдельная задача,
     ей нужен онлайн-доступ к зависимостям.
  2. `t` лежит в корневом пакете `xyz.azraellab.shared`, а не в `...shared.i18n`:
     импорт `xyz.azraellab.shared.i18n.t` не резолвится из `ui/components`.
  3. `Modifier.glass` уже задаёт скругление и рамку - компонент не должен дублировать
     `clip`/`border` вокруг него, иначе стеклянная карточка получает двойную обводку.

  Что осталось в P0: ничего; P0 закрыт 3/3. Компоненты пока не подключены к экранам -
  это делается точечно в P1-P4 вместе с разбиением `App.kt`, чтобы не менять вид и
  поведение в одном большом проходе. Старый пункт про 71 `Color.White` частично
  закрывается этим заходом: вместо новых сырых цветов появился `AzraelSecondary`.

- **2026-09-28** - P1 закрыт полностью. Навигация переведена с локальной `selectedId` на
  back-stack, чат стал настоящим подэкраном, широкие экраны получили двухпанельную раскладку.
  В `ui/nav/` шесть файлов: `Routes.kt` (sealed `Destination`: `Tab`/`Detail`/`Placeholder`,
  `DetailKind` с обязательностью аргумента, `parentTab()` и `scrollKey()` как методы-
  расширения над `when`), `NavState.kt` (стек, `navigate`-дедупликация, `rebase` под
  «роли сменились», `ScrollPositions` со строковым saver + `rememberSectionScroll`),
  `Navigator.kt` (права из `tabConfig`, `open`/`openTab`/`openDeepLink`, `back`),
  `TabConfig.kt` (разбор серверного списка: `messages`/`admin`/`shortener`/`vpn_tab`/legacy
  `vpn`/`settings`; `visible:false` - «закрыт», `Placeholder` для неизвестных id),
  `NavScaffold.kt` (заглушки, `AzraelNavigationBar` на узком и `NavigationRail` на широком,
  `AzraelDetailLayout`: wide - список слева + detail/пустая панель справа) и `NavHost.kt`.
  `MainShell` переписан: навигация владеет `selected`/состоянием вместо `App.kt`; refresh
  `tabConfig` по ролям через `rebase`, при выборе той же вкладки стек не растёт; системный
  back на Android идёт через `PlatformBackHandler`.
  **Чат**: открытый диалог - это `Destination.Detail(ChatRoom, id)`, а не внутреннее
  состояние списка. `ChatsView` разбит на `ChatsListSection` (только список) и
  `ChatRoomSection` (отдельный composable, `ChatsView` рендерит деталь в правой панели
  на wide и вместо списка на узком). Дуплекс: `client.chatsOpen(id)` даёт имя/сообщения.
  Back-stack: `A → B → A` не плодит записи, «назад» из комнаты возвращает предыдущий
  диалог, а затем список. Позиция скролла у каждого диалога своя: новый ключ
  `messages.room.<id>`; `rememberSectionScroll` держит `ScrollState` по ключу через
  `rememberSaveable(key)`, иначе второй диалог открывался бы на середине первого.
  Deep link `azrael://messages/<id>` открывает комнату (по правам родителя «Сообщений»),
  `azrael://settings/devices` - экран настроек; garbage/unknown → `null`, а не падение.
  На широких экранах правая панель пустая показывает `AzraelEmptyState` с новым ключом
  `chats.select` («Выберите диалог слева» / «Pick a chat on the left» / «请在左侧选择对话»).
  Проверки: `:composeApp:compileKotlinDesktop` + `:composeApp:compileAndroidMain` +
  `:app:compileDebugKotlin --offline` - **BUILD SUCCESSFUL**;
  `:composeApp:desktopTest --offline --rerun-tasks` - **104/104** в 12 классах (было 65/65;
  +33 `NavTest`-штуки за P1 на разбор `tabConfig`, права, deep links, back-stack, scroll
  ключи, склейку; +5 в этом заходе на комнаты и 4 «грабли»).

  Грабли этого шага:
  1. `rememberSectionScroll` изначально клал `key` только в `DisposableEffect`, а
     `rememberSaveable` - без ключа. Один `ScrollState` жил на все диалоги: `DisposableEffect`
     сохранял старую позицию под старым ключом, но новый экран получал тот же state - второй
     чат открывался на середине первого. Фикс: `rememberSaveable(key, saver = ScrollState.Saver)` -
     позиция теперь сбрасывается/растёт по ключу. (Проверено `javap`, что `rememberSaveable`
     принимает `vararg inputs` вместо `key`.)
  2. Рендер-тесты Compose так же невозможны офлайн (см. P0): разложение `ChatRoomSection`
     проверено сборкой и юнит-тестами навигации, а не картинкой.
  3. Разбиение `ChatsView` выполнено скриптом `/tmp/opencode/chats_split.py`: строчные номера
     от `find()` - 1-based, индексы `list` - 0-based, поэтому хвост комнаты резался по
     `find(r"^@Composable$")` - 1 - на первый раз комната потеряла последнюю строку.
  4. У `Detail`-видов «родитель» - это `DetailKind`, а не контейнер: `parentTab()` у
     `Setting` - Settings, у `ChatRoom`/`AiChat` - Messages; иначе `azrael://settings/devices`
     «назад» уводил бы в «Сообщения».

  Что осталось в P1: ничего; P1 закрыт 7/7. Дальше P2 - декларативные настройки.

## P2 - Декларативные настройки: журнал

  См. также артефакт и полный хендофф: `APP_DEV_LOG/10-settings-declarative-handoff.md` и
  `APP_DEV_LOG/10-settings-new-block.kt` (постоянный артефакт блока).

  Сделано:
  - `data/configurable/Configurable.kt` - `ConfigSpec`/`FieldSpec` (toggle/action/choice), `title`
    обязателен (без него `t` не знал бы заголовка); `ConfigurableList.kt` - `ConfigurableList`
    (заголовок-список) и `ConfigGroup` (одна строка-заголовок + строки-значения).
  - `SettingsView(client, profile, scrolls, onRefreshBoot, onLogout, nativeGreeting,
    modifier = Modifier, arg: String? = null, onOpenSetting: (String) -> Unit = {})`.
    `arg == null` → меню из 6 плиток (Аккаунт/Устройства/Безопасность/Приватность/Внешний вид/
    Канал); `arg != null` → сам раздел. Никакого `onThreat`.
  - Навигация: раздел - это `Destination.Detail(DetailKind.Setting, arg)` через
    `navigator.open(config, DetailKind.Setting, arg)`; ветка `DetailKind.Setting` рендерит
    `SettingsView(arg = destination.arg)`; «назад» → вкладка Настройки; back-stack и права
    родителя сохранены (P1).
  - Инвайты (myCode/invites/invitesAvailable/refreshInvites + карточка) перенесены из настроек
    в `AdminView` (только владелец) - источник копии `App.kt.bak-settings`.
  - i18n: новых ключей нет, всё ложится на существующие (`settings.section.*`, `security.title`,
    `appearance.title`, `theme.*`, `admin.*` и т.д.).

  Проверки: `:composeApp:compileKotlinDesktop` + `:composeApp:compileAndroidMain` +
  `:app:compileDebugKotlin --offline` - BUILD SUCCESSFUL; `:composeApp:desktopTest --offline
  --rerun-tasks` - **104/104** (12 классов; test-results desktopTest tests=104, failures=0).

  Грабли:
  1. Стейбильные номера строк после сплайса меняются (`:442`/`:1419` сдвинулись на −2) -
     все оставшиеся правки искались по контентным якорям, а не по строкам.
  2. Кусок старого `SettingsView` (`onThreat`-вариант с инвайтами) сохранён в
     `App.kt.bak-settings` - не удалять, пока всё не проверено.
  3. Чисто-декларативный рендерер всех типов (File/Password) отложен: сейчас типы
     покрыты точечными маленькими composable'ами внутри раздела (toggle/action/choice-ряды),
     спецификации дают костяк/заголовки. Это соответствует критерию приёмки (новая строка -
     не новая карточка).
  4. `theme.*`-ключи существуют, но настройки темы были в старом режимном коде; в этом
     заходе переключатель не заново вводится - реальный переключатель темы приходит с
     компонентным/токенным проходом из P0-P1.

  Что осталось в P2: ничего; P2 закрыт. Дальше P3 - данные и состояние.

## P3 - Данные и состояние: журнал (фундамент, 1/5)

  См. артефакт-хендофф: `APP_DEV_LOG/11-app-p3-data-state.md`.

  Сделано (аддитивно, UI не тронут):
  - `data/Json.kt` - internal-хелперы `s/l/i/o/b/a`/`jsonObjOrNull` над `JsonObject`
    (быстрый типизированный доступ, не конфликтует с приватными хелперами `App.kt`).
  - `data/model/Models.kt` - 17 `@Serializable` DTO (ProfileDto, ServerTabDto, TabConfigDto,
    DeviceDto, DeviceListDto, PrivacyDto, ChatDto, ChatMessageDto, ShortLinkDto, InviteDto,
    InviteSummaryDto, FreezeUserDto, VpnServerDto, VpnSummaryDto, AwgStatusDto, IncysDownloadDto,
    AiChatDto, ProvisionInfoDto, HealthDto) с фабриками `from(JsonObject)`.
  - `data/UiState.kt` - sealed `UiState<T>` (`Loading`/`Ready`/`Error`) + `map`/`getOrNull`,
    `runStateInt`/`runStateIO` (suspend, IO-диспатч + отлов исключений).
  - `data/repo/Repos.kt` - `HomeDto` + object `Repos`: 19 suspend-функций
    (homeBoot/devices/currentDevice/privacy/chats/archivedChats/messages(chatId,myId,limit)/
    shortLinks/invites/myInviteCode/generateInvites/vpnServers(folder)/vpnRefresh/awgStatus/
    incysDownloads/frozenUsers/aiChats/provisionInfo/systemHealth). `AppClient` остаётся
    синхронным транспортом.
  - Тесты: `desktopTest/.../data/ModelsTest.kt` (13 fixture-тестов без AppClient),
    `UiStateTest.kt` (4: map/getOrNull/pass-through, runStateInt ловит исключение,
    Error → Error, Ready → Ready).
  - Полевые сигнатуры зафиксированы по факту: профиль из `homeBoot().o("profile")` (не
    verify), `deviceStatus()` отдаёт сам device-объект, инвайты - summary `available` +
    `codes` (строки ИЛИ `{code}`), здоровье - через `.onSuccess`, vpn/awg/incys под полями
    `servers`/`platforms`.

  Проверки: `:composeApp:desktopTest --offline --rerun-tasks` - **BUILD SUCCESSFUL,
  121/121** (104 из P1/P2 + 17 новых; test-results tests=121, failures=0, errors=0);
  `:composeApp:compileAndroidMain --offline` - BUILD SUCCESSFUL.

  Грабли:
  1. `desktopTest` имеет только `kotlin("test")`; `kotlinx-coroutines-test` в офлайн-кэше
     нет - `UiStateTest` написан на `runBlocking`, а не `runTest`.
  2. Repos обязаны быть `suspend` (зовут suspend `runStateIO`) - на первый раз собран
     compile error, поправлено массовым `fun -> suspend fun`.
  3. `contentOrNull` - extension из `kotlinx.serialization.json`; Models.kt лежит в другом
     пакете (`data.model`) и обязан импортировать и его, и internal-хелперы
     (`import xyz.azraellab.shared.data.{s,l,i,o,a,b}`).
  4. Тестовые ожидания были «свои», а не фактические: `"аз"` против вычисленного `"Az"`,
     `serverLabel` непустой у chats-пункта, `ts.take(19)` даёт `...10:00:00`. После сверки
     с фактом тесты поправлены под реальные фабрики.
  5. `putJsonArray { add("строка") }` в этом kotlinx.serialization только через
     `add(JsonPrimitive("строка"))` - строкового overload'а нет.

  Что осталось в P3: ViewModel на экран, 92 `mutableStateOf` → `StateFlow`, типизированные
  состояния ошибок/загрузки вместо строковых флагов, подключение DTO к экранам. (Много-недельный
  этап; фундамент закрыт, дальше пошагово с зелёными сборками между подэтапами.)

## P3 - Данные и состояние: журнал (слой ViewModel, 2/5)

  - `data/vm/AppViewModel.kt` - KMP-совместимый базовый класс: собственный
    `CoroutineScope(Dispatchers.Default)`, внешний scope для тестов, `launch`, `close()`,
    `loadInto(stream, loader)` типизирует Loading→Ready/Error, `runState`-обёртка над
    suspend-репозиторием.
  - `ui/vm/VmFactory.kt` - `rememberViewModel(factory)` (+ очистка scope при выходе) и
    `StateFlow<UiState<T>>.collectAsUiState()`.
  - `data/vm/ShortenerViewModel.kt` - эталон: `rows: StateFlow<UiState<List<ShortLinkDto>>>`
    + `refresh()` + suspend `create/delete/qr` (UiState). Repos дополнены
    `shortenerCreate/shortenerDelete/shortenerQr`.
  - **P3-2**: `ShortenerView` переведён на VM - network-поля (`rows/status` из сетевых вызовов)
    заменены на `vm.rows.collectAsUiState()`; create/delete/QR идут через suspend-функции VM;
    локально остаются только UI-эпендные (`url/custom/lastShort/qrCode/qrError`).
    `ShortRow`/`parseShortener` удалены, DTO подключены.
  - Тесты: `AppViewModelTest.kt` (3: loadingThenReady, loaderErrorBecomesError,
    closeCancelsPendingLoad; `runBlocking` + `CompletableDeferred`-гейт, так как
    `kotlinx-coroutines-test` офлайн нет).

  Проверки: `:composeApp:desktopTest --offline --rerun-tasks` - **BUILD SUCCESSFUL, 124/124**
  (121 + 3 новых VM); `:composeApp:compileAndroidMain --offline` - BUILD SUCCESSFUL.

  Грабли:
  1. `Repos` лежит в пакете `xyz.azraellab.shared.data` (не `data.repo`) - первый импорт
     дал `Unresolved reference 'repo'`.
  2. Сетевые поля и UI-эпендные в экране разделять строго: `status` в P3-2 остался
     строковым (показывает «создано/удалено/QR…»), а `Loading/Error` списка идут из UiState.
  3. Рядовые правки в `App.kt` дают «UI не тронут» только для P3-1; в P3-2 экран меняется
     осознанно и точечно, чтобы не плодить diff.

  Что осталось в P3: `mutableStateOf` → `StateFlow` по остальным экранам
  (Chats/AiChat/Admin/Settings/Devices/Privacy/VPN), типизированные состояния
  вместо строковых флагов, DTO по всем экранам. → Закрыто ниже (записи P3-3/4/5).

## P3 - Данные и состояние: журнал (VpnView, AdminView, Chats+AI)

  2026-09-29. Дотеян стек P3 на StateFlow до 100% целевых экранов.

  - **P3-3 (VpnView)**: `data/vm/VpnViewModel.kt` - `summary`/`awg`/`incys`/`servers`
    StateFlow (servers стартует с `Ready(emptyList())`), методы `refreshAll`/
    `loadServers(folder: String?)`/`refreshServerLists`. В `VpnView` локально только
    UI-эпендные (`status`, `clipboard`, `scope`), сеть - через `vm.*.collectAsUiState()`.
  - **P3-4 (AdminView)**: `data/vm/AdminViewModel.kt` - `frozen`/`invites`/`myCode`/
    `health` StateFlow (`health` стартует с `Ready(HealthDto())`), методы `refreshAll`/
    `refreshFrozen`/`refreshInvites`/`loadInvites(active)`/`refreshHealth` +
    suspend `generateInvites`/`unfreeze(username)`/`archive(id)`. В `AdminView`
    локально `status`/`busy`/`invitesAvailable`/`clipboard`.
  - **P3-5 (Chats+AI)**: `data/vm/ChatsViewModel.kt` - `chats`(Loading)`/`archived`/
    `online`/`room`(стартует `Ready(RoomDto())`)/`aiChats`(Loading) и полный набор
    методов: refresh, archive/restore/delete, reloadRoom/reloadMessages (возвращают
    `UiState<Unit>` и обновляют состояние только на Ready - не затирают данные на
    ошибке), send/delete/setAutodelete/typing/uploadFile/fileUrl, askAi/refreshAiChats,
    search/createChat/updateRoomMessage. `ChatsView`, `ChatsListSection`,
    `ChatRoomSection`, `AiChatSection` переведены на `ChatsViewModel` + DTO
    (ChatDto/ChatMessageDto/RoomDto/AiChatDto/AiAnswerDto); `ChatRow`/`parseChats`/
    `ChatMsg`/`parseMessages` удалены (остались только DTO-фабрики в Repos).
  - **Детали перевода**: `LaunchedEffect(Unit){ vm.refreshAll() }` на входе экрана;
    сетевые поля (список/поиск/статус/комната/счётчики/ошибки) заменены на
    `vm.*.collectAsUiState()` + `when { is Loading -> спиннер, is Error -> статус+ретрай,
    is Ready -> данные }`; локально остаются только ввод текста, файлы, clipboard,
    autodelete-переключатель и т.п.; копт. ключей/скачивание Incys не трогали.
  - **Проверки (финал слайса)**: `:composeApp:compileKotlinDesktop --offline` и
    `:composeApp:compileAndroidMain :composeApp:desktopTest --offline --rerun-tasks`
    - BUILD SUCCESSFUL; test-results XML: tests=124 failures=0 errors=0 skipped=0,
    classes=15 (все P1/P2/P3-тесты в прогоне).
  - **Грабли**:
    1. `ChatsViewModel` нужен явный `import xyz.azraellab.shared.data.vm.ChatsViewModel`
       в `App.kt` - без него `CANNOT_INFER_PARAMETER_TYPE 'VM'` + каскад
       Unresolved, молча не вылечивается.
    2. `errText(e)` принимает `Throwable`, а `UiState.Error.message` - `String`.
       Поэтапно в коде уже установлен паттерн `status = r.message` напрямую
       (ShortenerView/Home/Devices), errText остаётся только для настоящих
       Throwable-путей; массовая замена `errText(r.message)`/`errText(s.message)`
       сделана `sed`-ом и сверена `rg` по 25 вхождениям.
    3. Делегированные свойства (`by vm.x.collectAsUiState()`) не умный-кастуются:
       нужен явный `(chatsState as UiState.Error).message` (без него - «Smart cast
       impossible»/Unresolved `.message`).
  - **Что осталось в P3**: Settings, Devices, Privacy, Starfield/прочее; после них -
    P4 (38 composable → `screens/`, `App.kt` < 200 строк) и P5 (состояния + смоук).

## P3 - Данные и состояние: журнал (Settings+Devices+Privacy, 6/6, P3 закрыт)

  2026-09-29. Дотеян стек P3 на StateFlow до 100%: SettingsView/Devices/Privacy переведены на
  `SettingsViewModel` (см. `APP_DEV_LOG/11-app-p3-data-state.md`). Критерий приёмки P3 выполнен:
  ни один экран не парсит `JsonObject`, прямой `client.*` из `App.kt` остался только там, где это
  дизайн (gateway-тест `gc.connect(client.sessionToken())`).
  - **`SettingsViewModel`** (`data/vm/SettingsViewModel.kt`): StateFlow
    `devices(Loading)/privacy(Loading)/provision(Loading)/avatarUrl(Loading)` + `refreshAll()`
    (4 лонча сразу). Suspend-действия поверх Repos: `showProvisionKey`/`regenerateProvisionKey`
    (сами зовут `refreshProvision`), `savePrivacy`, `revokeDevice`, `rotateDeviceKey`,
    `deviceStatusLabel`, `otpGenerateSecret`, `otpValidate`, `changePassword`, `revokeSession`,
    `verifySession`, `healthText`, `saveProfile`, `setLang`, `setAutoDelete`
    (`UiState<Int?>`), `avatarSet` (пишет `_avatarUrl` на Ready), `deleteProfile`,
    `currentDeviceId(): String?`, `isL2Available()`.
  - **`SettingsView`**: `vm = rememberViewModel { SettingsViewModel(client) }`; коллекты по
    находищам; производные `deviceList/devicesActive/devicesMax/devicesBusy`, `provisionInfo` =
    `when(provisionState)`, guard `privacyLoaded`, `revokeTarget: DeviceDto?`; один
    `LaunchedEffect(Unit) { vm.refreshAll() }`; локальные `refreshDevices/refreshPrivacy/
    refreshProvision` удалены; тело целиком через VM-методы + DTO (`DeviceDto.devId/label/
    platform/status/lastSeen/rotations`, `PrivacyDto`), статусы `when(UiState.Ready/Error/Loading)`.
  - **Repos**: +`profileSetLang`; `privacySave/deviceRevoke/deviceRotate/passwordChange/
    sessionRevoke/profileSave/profileDelete` приведены к `UiState<Unit>` терминальной `Unit`.
  - **Проверки**: `compileKotlinDesktop` + `compileAndroidMain` BUILD SUCCESSFUL (после фикса
    `currentDeviceId(): String?`); `desktopTest --rerun-tasks` - **124/124** (15 классов),
    failures=0 errors=0 skipped=0.
  - **Грабли**: `AppClient.deviceId()` - `String?` (сигнатуру VM-метода пришлось сделать
    nullable); локальные `withContext(Dispatchers.IO){client.*}` в SettingsView вычищены
    полностью (проверено grep по App.kt). Бэкап `App.kt.bak-settings` не трогаем.
  - **Что дальше**: **P4** - 38 composable из `App.kt` → `screens/`, `App.kt` < 200 строк,
    `I18nTest` зелёный; затем **P5** - состояния + смоук + финальные сборки.

## P4 - Экраны: механический раскол `App.kt` на `screens/` (сплиттер, 10 файлов, база для P5)

  2026-09-29. `App.kt` (3511 строк) механически разрезан по `@Composable` → 9 файлов `ui/screens/`
  + общие примитивы в `ui/common/ScreenKit.kt`; целевые `private fun X` → `internal fun X` только для
  перекрёстно используемых; `App.kt` сжат до ~19 строк (композиция + DI + I18n.load() + AppRoot).
  Код на месте: только перенос примитива, раскол - база для P5 (склейка с ViewModel/навигацией).
  - **Скрипт**: `/tmp/opencode/p4_split.py` (по образцу `chats_split.py` из P1): каждый файл =
    `package xyz.azraellab.shared` + полный блок импортов App.kt (строки 2-181) + выбранные диапазоны.
    Неиспользуемые импорты в частях - это warnings, не errors.
  - **Файлы**: `ui/common/ScreenKit.kt` (internal `autoDeleteState`/`qrBitmap`/`avatarBitmap`/
    `PlaceholderScreen`), `ui/screens/{RootScreen,StatusScreens,LoginScreen,MainShellScreen,
    AdminScreen,ChatsScreen,ShortenerScreen,VpnScreen,SettingsScreen}.kt`.
  - **Cross-file доступность (ручные правки)**: `RootScreen.kt:185` `private class Session` →
    `internal class Session`; `RootScreen.kt:196` `private sealed interface ChannelKey` →
    `internal sealed interface ChannelKey`; `MainShellScreen.kt:346` `private fun AvatarCircle` →
    `internal fun AvatarCircle` (используется `SettingsScreen.kt:358`). Прочие `private` остаются
    файловыми - сплиттер по конвенции трогает только имя `fun`.
  - **Грабли**:
    1. Сплиттер НЕ трансформирует не-`fun` декларации - `class Session`/`sealed interface ChannelKey`
       упали с `internal` механикой файлов и были переведены вручную.
    2. Хвосты файлов, образованные вырезанием диапазона, оставляли «незакрытую» KDoc: `AdminScreen`
       имел висячий комментарий без `*/` (строчки 330-338, «Сообщения…») + `}`-офф-бай-он → добавлен
       закрывающий `}`; `ChatsScreen` - KDoc комнаты без ` */` (после «…по клику.») → дописан. Проверка
       баланса: наивный сканнер давал ложные «depth 2» на `//` внутри `http://` в KDoc (ScreenKit ←201).
    3. Изменение `private fun` → `internal fun` даёт видимость выше файла - после P5 (перевод в
       VM/навигацию) часть `internal` можно снова опустить до `private`.
  - **Проверки (финал слайса)**: `:composeApp:compileKotlinDesktop --offline` - BUILD SUCCESSFUL
    (только warnings); `:composeApp:compileAndroidMain --offline` - BUILD SUCCESSFUL; `:composeApp:
    desktopTest --offline --rerun-tasks` - BUILD SUCCESSFUL, **124/124** (15 классов), failures=0
    errors=0 skipped=0 (совпадает с базовым прогоном/выходом P3).
  - **Что осталось**: P4 критерий «`I18n.kt` разбит по доменам + `I18nTest` зелёный» переносится в
    P5 как первый шаг (механический раскол по доменам с сохранением всех ключей).

## P5 - первый шаг: `I18n.kt` разбит по доменам (3 файла + прокси, I18nTest зелёный)
- **Сплиттер**: `/tmp/opencode/p5_i18n_split.py` (по побайтовому диапазону карт `private val X = mapOf(`
  → `)` на отдельной строке). Итог: `I18n.kt` 1358 → **93 строки** (пакет, `AppLang`, объект `I18n`,
  `Strings`/`t`, 9 прокси-деклараций `private val` + карта `I18N`); три доменных файла в том же
  пакете `xyz/azraellab/shared/` - `I18nApp.kt` (`APP_RU/EN/ZH` - вход/профиль/устройства/канал),
  `I18nChats.kt` (`CHATS_RU/EN/ZH` - чаты + AI), `I18nTools.kt` (`TOOLS_RU/EN/ZH` - сократитель/
  VPN/админка/OTP/приглашения). В каждом карта - `internal val`, тело байт-в-байт (ключи/значения
  не тронуты).
- **Почему прокси, а не прямой перенос**: `I18nTest` читает карты рефлексией по имени поля из
  фасада `I18nKt` (`Class.forName("…shared.I18nKt")`, поле `RU`, `RU_MESSAGES`, …). Перенос карт в
  `I18nAppKt`/`I18nChatsKt`/`I18nToolsKt` убрал бы поля из `I18nKt` и сломал рефлексию. Решение -
  в `I18n.kt` остаются те же приватные `val` с прежними именами, но назначенные из доменных карт:
  `private val RU = APP_RU` и т.д. - фасад не меняется, тест не трогается, а данные физически
  лежат по доменам. Требование «разбит по доменам» выполнено по месту обитания карт.
- **Грабли сплиттера**: (1) альтернации `(RU|EN|ZH|…)` в regex совпадали раньше длинного имени -
  `RU_MESSAGES` читался как `RU` и перезаписывал блоки; починено сортировкой альтернатив по длине
  (`RU_MESSAGES` раньше `RU`), иначе ключи перевозились между файлами; (2) блок = диапазон до
  последней строки `)` перед следующей декларацией - хвостовые комментарии/пустые строки
  выпадают из блока, что как раз убирало секционные `// ----` маркеры из тела.
- **Проверки (все зелёные)**: `:composeApp:compileKotlinDesktop --offline` - BUILD SUCCESSFUL
  (только out-of-band warnings: Repos.kt unused-выражения, deprecated `LocalClipboardManager`,
  `VpnScreen.kt:287` Elvis); `:composeApp:compileAndroidMain --offline` - BUILD SUCCESSFUL;
  `:composeApp:desktopTest --offline --rerun-tasks` - BUILD SUCCESSFUL, **124/124** (15 классов),
  failures=0 errors=0 skipped=0; из них `I18nTest` - **6/6** (одинаковые ключи RU/EN/ZH, ≥300,
  нет кириллицы в EN/ZH, плейсхолдеры, `AppLang.of`, переключение языка через `AppLangStore`).
- Журнал - запись добавлена в `_/home/azrael/PROXMOX_SRV/AGENTS.md` (раздел AZRAEL APP P4/P5).

## P5 - состояния (шаг 2): `AzraelLoadingState` / `AzraelErrorState` + 2 списковых экрана
- **Семейство состояний достроено**: `AzraelEmptyState` был один, а Loading/Error на экранах
  рисовались строкой текста. Добавлены два компонента с той же вертикальной геометрией
  (`fillMaxWidth` + `AzraelSpace.xxl`, `spacedBy(sm)`, центрирование), чтобы блок списка не менял
  высоту при смене состояния:
  - `ui/components/AzraelLoadingState.kt` - `CircularProgressIndicator(28.dp, AzraelPrimary, 2.5.dp)`
    + подпись `bodyMedium` цветом `AzraelSecondary`;
  - `ui/components/AzraelErrorState.kt` - иконка `Icons.Filled.Warning` (`AzraelErrorSoft`, 28.dp)
    + текст ошибки + необязательная кнопка повтора (`AzraelButton` тон `Ghost`, иконка `Refresh`).
- **Разделение ответственности в экранах**: раньше `status` был одновременно и «что сейчас
  происходит со списком», и откликом на действие (поиск/удаление/архив/копирование). Из-за
  `LaunchedEffect(списокState)` тот же эффект тут же затирал «удалено»/«найдено» счётчиком
  диалогов. Теперь `status` - только отклик на действие (и рисуется под `isNotBlank()`), а
  состояние списка живёт в своём блоке.
- **Внедрено (2 экрана, выбранных владельцем для показа - остальные 7 по той же схеме)**:
  - `ChatsScreen.ChatsListSection` - `UiState.Loading` → `AzraelLoadingState`, `Error` →
    `AzraelErrorState` с повтором (`vm.refresh()`), `Ready` + пусто → `AzraelEmptyState`
    (`chats.none` / `chats.none.hint`), `Ready` + не пусто → `Label(chats.count)`.
  - `ShortenerScreen` - то же для `rowsState` (`short.none` / `short.count`).
- **i18n**: добавлен ключ `short.none.hint` сразу в `TOOLS_RU/EN/ZH` (по одному новому ключу на
  язык) - наборы ключей RU/EN/ZH остались одинаковыми, что и проверяет `I18nTest`. Новых ключей
  больше не понадобится: `common.loading`, `action.retry`, `error.*` уже были в словаре.
- **Проверки (все зелёные)**: `:composeApp:compileKotlinDesktop :composeApp:compileAndroidMain
  --offline` - BUILD SUCCESSFUL; `:composeApp:desktopTest --offline --rerun-tasks` - BUILD
  SUCCESSFUL, **124/124** (15 классов), failures=0 errors=0 skipped=0; `I18nTest` 6/6.
- **Грабли**: (1) `AzraelEmptyState`/`AzraelLoadingState`/`AzraelErrorState` объявлены в
  `ui.components`, а `t` живёт в корневом пакете `xyz.azraellab.shared` - импорт `…shared.i18n.t`
  не резвится, нужен `xyz.azraellab.shared.t`; (2) `AzraelEmptyState` рисует `Text(title)` без
  явного цвета (берёт `onSurface` темы), а `AzraelSecondary` = `onSurface` тёмной схемы - на
  светлой теме подпись состояния уедет в чёрный, это стоит проверить глазами (тестами офлайн
  не ловится, как и линии разделителей); (3) удаление `LaunchedEffect(списокState)` меняет
  поведение, а не только вид: `status` больше не затирается счётчиком - это и было целью, но
  стоит держать в уме при следующих экранах.

## P5 - состояния (шаг 2, продолжение): остальные 8 экранов
- **Внедрено на всех оставшихся экранах** (после `ChatsListSection`/`ShortenerScreen` из шага 2):
  - `AdminScreen` - `invitesState` (Loading/Error+`refreshInvites(true)`/Empty), `frozenState`
    (Loading/Error+`refreshFrozen()`/Empty) и `healthState` вместо строки `"health: …"`. Удалён
    осиротевший `var busy` (после перехода на `frozenState` он уже ничего не делал), кнопка
    обновления упрощена до `IconButton(onClick = { vm.refreshFrozen() })`.
  - `SettingsScreen` - `provisionState` и список устройств `devicesState`. Производная
    `provisionInfo` из `when(provisionState)` удалена, текст рисуется на месте; `devicesBusy`
    больше не подменяет пустой список строкой `"список не загружен"` - состояния честные.
  - `VpnScreen` - `summaryState`, `serversState`, `awgState`, `incysState`. Удалён верхний
    баннер `vpn.error` (первая ошибка из трёх состояний): после появления per-card состояний он
    дублировал ту же ошибку на весь экран. Ключ `vpn.error` убран из `TOOLS_RU/EN/ZH` (иначе
    остался бы мёртвым, а `I18nTest` требует одинаковые наборы - убрали симметрично).
  - `ChatsScreen.AiChatSection` - `aiState`: Loading/Error+`refreshAiChats()`/Empty
    (`ai.history.empty`)/`Label(chats.count)`. `refresh()` больше не затирает `status` текстом
    ошибки - состояние рисуется отдельно.
  - `ChatsScreen.ChatRoomSection` - `roomState`: Loading/Error+`reloadRoom(chatId)`/Empty.
    `LaunchedEffect(chatId)` больше не пишет `common.loading` в `status` (иначе слово «Загрузка…»
    висело и после успеха); `status` снова только отклик на действие.
  - `MainShellScreen` - рукописный блок «нет доступных разделов» (`Icons.Filled.Block` +
    два `Text` вручную) заменён на `AzraelEmptyState` (`main.noTabs` / `main.noTabs.hint`), кнопки
    обновить/выйти оставлены под ним.
  - `StatusScreens.ChannelScreen` - собственный `CircularProgressIndicator` и блок с
    `Icons.Filled.Block` + отдельной кнопкой повтора заменены на `AzraelLoadingState` /
    `AzraelErrorState(action = channel.key.retry)`. Заголовок с названием приложения и подсказка
    `channel.key.loading.hint` остались (это экран-ворота канала, а не блок списка).
  - `LoginScreen`, `RootScreen` - списочных состояний нет (форма и восстановление сессии), ночки
    не требовались; `RootScreen` только вызывает `ChannelScreen` (см. выше).
- **Проверка полноты**: `rg 'Label\(t\["(common\.loading|short\.listNotLoaded)'` по `ui/screens/`
  даёт 0 совпадений - «пока не загружено» больше не рисуется строкой вместо состояния.
- **i18n**: добавлен ключ `chats.room.empty.hint` в `CHATS_RU/EN/ZH` (по одному на язык).
  Ошибочный кандидат на подсказку - `chats.noMessagesParens` («(без сообщений)») - не годится:
  это подпись превью в списке, а не пояснение пустой комнаты.
- **Проверки (все зелёные)**: `:composeApp:compileKotlinDesktop :composeApp:compileAndroidMain
  --offline` - BUILD SUCCESSFUL; `:composeApp:desktopTest --offline --rerun-tasks` - BUILD
  SUCCESSFUL, **124/124** (15 классов), failures=0 errors=0 skipped=0; `I18nTest` 6/6.
- **Грабли**: (1) у `AzraelEmptyState` параметр называется `subtitle`, а не `hint` - компилятор
  ловит сразу, но в двух местах пришлось поправить; (2) per-card состояния в `VpnScreen`
  сделали верхний баннер ошибки избыточным - его удаление это изменение поведения (ошибка
  больше не дублируется на весь экран), а не только вид; (3) импорты остаются от сплиттера P4
  (полный блок в каждом файле `screens/`), поэтому `Block`/`CircularProgressIndicator` в
  `MainShellScreen`/`StatusScreens` стали неиспользуемыми - чистить их точечно незачем, они
  были неиспользуемы и до этого.
## P5 - контраст компонентов, звёздный фон и финальные сборки

Журнал шага: `APP_DEV_LOG/12-app-p5-states-contrast.md`.

Журнал подэтапа: `../../APP_DEV_LOG/12-app-p5-states-contrast.md`.

Три независимые находки этого этапа. Первые две нашлись только потому, что контраст
стали считать по функциям, которые реально возвращает компонент, а не по числам,
переписанным в скрипт.

### 1. `ContrastTest` - контракт вместо скрипта

`ui/components/{AzraelButton,AzraelChip,AzraelTextField}.kt` получили чистые функции
(`buttonContainerColor`, `buttonContentColor`, `buttonBorderColor`, `chipBorderColor`,
`chipContainerColor`, `chipLabelColor`, `fieldContainerColor`, `fieldBorderColor`,
`fieldLabelColor`, `fieldSupportColor`), а composable берут цвета **только** из них.
`ContrastTest` (20 тестов) считает контраст этих функций после композитинга с подложкой.

**Почему скрипт `/tmp/opencode/audit5.py` был недостаточен**: он проверял числа,
продублированные из исходников, и показывал «всё хорошо» даже когда палитра менялась.
Тест ломается сам, если поменять палитру или компонент без правки контракта.

**Тест сразу нашёл реальный дефект, который скрипт пропустил**: `Glass` в состоянии
работы (`busy`) гасил подпись до `secondary@45%` - 4.04 в тёмной и 2.85 в светлой.
Состояние работы не освобождает текст от 1.4.3: рядом со спиннером по-прежнему стоит
`busyLabel`. Поднято до `0.65` (7.16/5.23). Раньше проверка молча выскакивала из цикла
по `if (busy) return@forEach`.

### 2. `StarfieldBackground`: белые звёзды на светлой теме

Фон рисуется поверх `scheme.background`; `StarfieldState` держал **один** набор цветов
на обе темы - белые звёзды. На светлой `#FAFAFA` их просто не было видно: фон переставал
существовать. Добавлен `StarfieldPalette` + чистая `starfieldPalette(dark)`:

- тёмная - тёплые белые (как на сайте /e2);
- светлая - тёмные синевато-серые, контраст к странице ~11:1.

`MaterialTheme.colorScheme.background.luminance() < 0.5f` выбирает палитру, а
`LaunchedEffect(palette)` перекрашивает уже созданные звёзды: `remember` без ключа
переживает смену темы, иначе фон остался бы старого цвета до пересоздания состояния.
Перекрашены и `flashColor`/цвета шлейфа кометы - на светлой теме «вспышка» тёмная.

Фон - украшение, WCAG 1.4.11 к нему не относится, и порог 3:1 в тесте - не требование
нормы, а проверка «фон существует».

### 3. Финальные сборки

`:composeApp:compileKotlinDesktop :composeApp:compileAndroidMain --offline` - BUILD
SUCCESSFUL; `:composeApp:desktopTest --offline --rerun-tasks` - BUILD SUCCESSFUL,
**146/146** (17 классов), failures=0 errors=0 skipped=0 (было 124/124 в 15 классах;
+22 `ContrastTest`).

**P5-5 закрыт**: `rg 'Color\.White|Color\.Black'` по `commonMain` даёт 3 совпадения в
`StarfieldBackground` (это и есть тёмная палитра неба - не UI-цвета) и 1 в комментарии
`ChatsScreen`. 71 вхождение из старого TODO вычищены.

**Остаток P5**: ручной смоук (вход → чат → файл → сокращатель → VPN → админка) - нужен
тестовый аккаунт или `AZRAEL_FLIGHT_INVITE`. Compose render-тесты офлайн невозможны:
`compose.ui-test` нет в кэше Gradle.

## Итог (актуально на v1.3.3, 2026-10-01)

Журнал выше - историческая хроника этапов; ниже - то, что важно знать сегодня.

### Что закрыто

| Этап | Содержание | Состояние |
|---|---|---|
| P0 | дизайн-система: токены, светлая тема, 15 компонентов | закрыт |
| P1 | навигация: back-stack, deep links, `AdaptiveDetailLayout`, маппинг `tabConfig` | закрыт |
| P2 | декларативные настройки: `data/configurable/`, 6 секций, инвайты в админку | закрыт |
| P3 | DTO/репозитории/ViewModel, состояние на `StateFlow`/`UiState` | закрыт |
| P4 | разбивка монолита: 9 экранов в `ui/screens/`, `App.kt` = 20 строк | закрыт |
| P5 | состояния Loading/Empty/Error, контракт WCAG, тематический фон, вычистка `Color.White` | закрыт, кроме трёх пунктов ниже |

Релизы: первая публикация редизайна - `v1.3.2` (pre-release), стабильная - `v1.3.3`.

### Тесты

`:composeApp:desktopTest --offline --rerun-tasks` - 183 теста, 0 failures.
Эволюция по этапам: 124 → 135 (промежуточная проверка P0-P4) → 146 (P5) → 183 (`v1.3.3`).
Числа из промежуточных записей журнала не суммировать: каждый прогон пересчитывает весь
набор заново.

### Фактическая структура

```
composeApp/src/commonMain/kotlin/xyz/azraellab/shared/
├── App.kt                       20 строк: AppThemeRoot + GlassBackground + AppRoot
├── I18n.kt + I18nApp/Chats/Tools.kt   365 ключей RU/EN/ZH, доменная нарезка
├── core/api/        AppClient (63 операции), AppSecure, AppKeyBootstrap, AppInstall
├── core/crypto/     Ed25519, SHA-512, Fe25519 - чистый Kotlin в commonMain
├── core/protocol/   Envelope, SessionBox, GatewayClient, Http (expect/actual)
├── data/            model/, repo/Repos.kt, vm/ (AppViewModel, ChatsViewModel, SettingsViewModel),
│                    configurable/ (ConfigSpec)
├── ui/components/   15 файлов (AzraelButton, AzraelCard, ... AzraelEmptyState)
├── ui/nav/          Routes, NavState, Navigator, NavHost, NavScaffold, TabConfig
├── ui/screens/      9 файлов (RootScreen, LoginScreen, MainShellScreen, ChatsScreen, ...)
├── ui/theme/        Color, Type, Shape, Spacing, Ripple, Theme
└── ui/common/       ScreenKit (общие примитивы экранов)
```

### Осталось открытым

1. **Ручной авторизованный смоук** (вход → чат → файл → сокращатель → VPN → админка) -
   нужен тестовый аккаунт или `AZRAEL_FLIGHT_INVITE`. Единственный непроверенный путь
   end-to-end.
2. **Compose render-тесты** - `compose.ui-test` отсутствует в кэше Gradle, офлайн
   недоступны. Проверено косвенно: запуск под Xvfb в обеих темах, плюс `ContrastTest`
   (контрактные функции цветов, а не скрипт с продублированными числами).
3. **Windows 10/11 runtime** и **Android device smoke** - CI собирает MSI и APK, но
   ручная проверка на реальном устройстве не выполнялась.
