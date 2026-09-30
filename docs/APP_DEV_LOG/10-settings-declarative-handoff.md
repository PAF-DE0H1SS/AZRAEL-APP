# AZRAEL-APP: рабочая память — P2 «декларативные настройки» (handoff)

Статус: **в работе**. Создан для постоянного хранения состояния сессии (не в /tmp).
Обновляется по мере выполнения; финальный актуальный источник — `docs/ui-redesign-plan.md`.

## 0. Как возобновить сессию
1. Прочитать этот файл целиком.
2. Проверить актуальные границы блока `SettingsView` в App.kt (см. §2).
3. Делать TODO-задачи из §6 строго по порядку.

## 1. Цель и критерий приёмки
- Переписать `SettingsView` (блок `:2740–3425` в
  `composeApp/src/commonMain/kotlin/xyz/azraellab/shared/App.kt`) на модель
  `data/configurable/` (`ConfigurableList`/`ConfigSpec`).
- Вкладка «Настройки» = меню из 6 разделов + навигация `Detail(Setting, arg)`.
- Тема переключается через `AzraelThemeState` (убрать `ThemeManager.toggler`).
- Инвайты перенести из настроек в `AdminView`.
- Убрать `onThreat` из `SettingsView` (ThreatMode больше не нужен).
- Сборка и тесты зелёные. **Критерий приёмки**: «новая настройка добавляется строкой
  описания списка» (одна `ConfigSpec`-строка в секции).
- Директива пользователя: действовать автономно, без вопросов.

## 2. Границы блока (подтверждены повторным чтением)
- `:2738` = `}`, `:2740` = `// ---- Настройки: профиль, приватность, устройства, канал, шлюз, выход ----`
- `:2742-2743` = `@Composable private fun SettingsView(`; `onThreat: (ThreatMode) -> Unit,` на `:2747`
- Конец блока: `:3425` = `}`; `:3427` = `// ---- Общие элементы ----`; файл 3488 строк.
- Внутри блока **нет** top-level-функций.
- Способ замены: Write нового кода в temp-файл → python-splice:
  `src[:2739]` + new + `src[3425:]`.

## 3. Финальная сигнатура и dispatch
```kotlin
@Composable
private fun SettingsView(
    client: AppClient, profile: AppProfile, scrolls: ScrollPositions,
    onRefreshBoot: () -> Unit, onLogout: () -> Unit, nativeGreeting: () -> String,
    arg: String?, onOpenSetting: (String) -> Unit
)
```
- **Без `onThreat`.**
- Dispatch: `section = arg?.takeIf { it.isNotBlank() }`.
  - `null -> SettingsMenu(client, profile, onRefreshBoot, nativeGreeting, onOpenSetting)`
  - `"account" -> AccountSection(client, profile, onRefreshBoot)`
  - `"devices" -> DevicesSection(client)`
  - `"security" -> SecuritySection(client, onLogout)`
  - `"privacy" -> PrivacySection(client, profile)`
  - `"appearance" -> AppearanceSection()`
  - `"channel" -> ChannelSection(client, profile, onLogout)`
  - `else -> PlaceholderScreen(section ?: "", t("tab.soon", section ?: ""))`
- Заголовок: `t["settings.title"]` (меню) / `t("settings.section.$section")`.
- Скролл: `rememberSectionScroll(scrolls, if (section == null) "settings" else "settings.$section")`.
- Root-Column: `Modifier.fillMaxSize().verticalScroll(scroll)` + `Arrangement.spacedBy(14.dp)`.
  У каждой секции свой `status` локально.

## 4. Карта секций (точный порт старого кода)
### Menú (SettingsMenu)
- `AzraelCard`: список `AzraelListTile` из `Triple(id, icon, title)`:
  `Account→Person`, `Devices→Devices`, `Security→Shield`, `Privacy→VisibilityOff`,
  `Appearance→Palette`, `Channel→Dns`.
  leading tint `AzraelSecondary`, trailing `Icons.AutoMirrored.Filled.KeyboardArrowRight`
  tint `AzraelTextDim`, `onClick = { onOpenSetting(id) }`.
- Identity/verify-карточка (старый блок :2842-2880):
  - кнопка `t["channel.check.session"]` → `scope.launch { status="…"; val d=withContext(IO){client.verify()}
    ; status = t("channel.session.ok", (d.s("username") ?: "?") + " · " + (d.s("role") ?: "?")) }`
    catch → `status = errText(e)`
  - `AccentButton(t["settings.refreshAccount"]) { onRefreshBoot() }`
  - `Label(t("settings.platform", platformName(), nativeGreeting()))`
- Языковая карточка: `AppLang.entries`, кнопки
  `(if (I18n.lang == lang) "• $title" else title)`; в named-лямбре — `if (lang != null) { … }`.

### Аккаунт (AccountSection)
- account-карточка: `account.title`, `settings.refreshAccount`, `settings.platform`,
  `tg.bound`/`notBound`.
- профиль: `profile.title/name/tag/gender/save/saved`, `common.saving`,
  `profile.avatar.info/upload`; avatar через `rememberFilePicker(listOf("image/*"), 5L*1024*1024)`
  + `stripDataUrl`.
- язык: `settings.lang`, `settings.langSaved`.

### Устройства (DevicesSection)
- `devices.title`, `"$devicesActive / $devicesMax"` (цвет `AzraelDanger` если active>=max иначе
  `AzraelSecondary`), `devices.limit1`+`devices.limit2`; пусто → `common.loading`/`short.listNotLoaded`.
- Строка устройства: `devices.title.col`, статус `active.lower`/`devices.revoked.lower`/
  `admin.frozen.lower` (active → AzraelSecondary, иначе AzraelDanger), isCurrent →
  `devices.thisDevice.lower` (AzraelPrimary), `devices.short` + `channel.rotations`.
- Revoke: `devices.revoke` → `revokeTarget = d`; подтверждение: `devices.thisDevice` или
  `t("devices.revoke.warn", title)`, `devices.revoke.yes`/`action.cancel`;
  результат `devices.current.revoked` / `devices.revoked`; busy `devices.revoking`.
  Метод `client.deviceRevoke(devId)`.
- Проверка ключа: `devices.check` → `client.deviceStatus()` → `t("devices.statusRow", st)`;
  поле `login.provision.label`.
- L2: `if (client.isL2Available) { devices.rotate → client.rotateDeviceKey → channel.rotating /
  devices.provision.ready } else Label(t["channel.l2.unavailable"])`.
- кнопка обновления `short.update`/`short.updating`.
- OTP-секрет показывается как текст + `otp.copy` → clipboard → `devices.key.copied`.

### Безопасность (SecuritySection)
- Смена пароля: `login.password`, `login.currentPassword`, `login.newPassword`,
  `login.changePassword`, валидация (min 8) → `login.need.passwords`; `common.changing`,
  `login.passwordChanged`. **`secret=true` полевое.**
- Revoke session: `channel.revokeSession` → `client.revokeSession()` → `channel.session.revoked` → `onLogout()`.
- Provision: `devices.provision.show` → `client.provisionKey()` → key в `provisionKey`,
  `devices.provision.once`/`devicees.provision.issued` + `refreshProvision()`;
  показать ключ (bold AzraelSecondary) + `otp.copy` → clipboard → `devices.key.copied`;
  `devices.provision.regen` → `client.provisionRegenerate()` → key, `channel.key.old`.
  **provisionKey BooleanField: `secret=true`.**
- OTP: `otp.issue` → `client.otpGenerateSecret()` → `otpSecret`, `otp.got`;
  поле `otp.code` (**не secret**); `devices.checkCode` → `client.otpValidate(code)` →
  `otp.accepted`/`otp.rejected`, пусто → `otp.enter`.
  OTP-секрет в desc у `ActionSpec("otp.issue")`: `otp.secret`/`otp.secret.empty`.

### Приватность (PrivacySection)
- `privacy.title/hidden/whoSearch/whoWrite/save/loadSave/error/saved`, `common.saving`.
- AutoDelete: `profile.autoDelete`, `profile.autoDelete.hint1` +
  `t("profile.autoDelete.now", autoDeleteState(autoDeleteDays))` (AsDescriptive/DirectText),
  кнопки `off`/`d30s`/`d90`/`d365` → `client.profileAutoDelete(d)` → `chats.autoDelete.off`/
  `t("chats.autoDelete.in", d)` (`chats.autoDelete.days` — см. App.kt :2125).
- Загрузка: `refreshPrivacy()` = `client.profilePrivacyGet().o("privacy")` →
  `hidden_from_search`, `privacy_who_can_search`, `privacy_who_can_write`.

### Внешний вид (AppearanceSection)
- Тема через `AzraelThemeState`: `theme.system/dark/light`.

### Канал (ChannelSection)
- `channel.check.health` → `client.systemHealth()` → `t("channel.health.responds", …)`
  или `errText`.
- L2 key: поле `channel.l2.key` (l2Pub), `channel.l2.apply` →
  `AppRuntime.srvXPubB64 = l2Pub.trim().ifBlank { null }`, `channel.l2.applied`;
  label `profile.autoDelete.hint2`.
- Gateway: `channel.l2.gateway`, поле `https://…/api/gateway/v1` (gatewayUrl),
  `vpn.connect` → `normalizedEndpoint(gatewayUrl)` (null → `gwStatus = login.need.https`),
  `AppRuntime.gatewayUrl = url`; `vpn.connecting`; `GatewayClient(url)`,
  `gc.connect(client.sessionToken())` → `"handshake=" + (ok ? gateway.ok : noResponse) +
  " · " + t["gateway.status"] + "=" + statusText`; catch → `t("admin.gatewayError", errText(e))`.
  Label `channel.l2.privacy`.
- Logout/delete: `channel.logout` → `onLogout()`; `profile.delete.warn`; confirm:
  `profile.delete.confirm` (username), `profile.delete.yes` → `client.profileDelete()` →
  `common.deleting`/`profile.deleted` → `onLogout()`, `action.cancel`.

## 5. Инвайты → AdminView (перед `:1735`)
- Порт блока :3147-3203 со своей копией государства:
  `myCode` (из `client.invitesMyCode().s("code")`), `invites` (`client.invitesList(true)`
  → `items`), `invitesAvailable` (`d.i("available")`) + `refreshInvites()` + LaunchedEffect(Unit).
- UI: `myCode` + `admin.copyMyCode` → clipboard → `admin.codeCopied`;
  кнопка `admin.generate`/`t("admin.generateFree", n)` → `invitesGenerate()` →
  `t("admin.createdCodes", d.a("codes").size)` + refresh;
  список: `code + used_by`, copy (ContentCopy) / archive (Archive → `invitesArchive(id)` →
  `admin.codeArchive` + refresh); `admin.activeNone`; `admin.showCodeArchive` →
  `invitesList(false)` → `t("admin.archivedCodes", size)`.
- Ключи: `admin.invites`, `admin.myCode`, `admin.usedBy`, `action.copy`, `chats.archive`,
  `admin.generating`, `admin.invitesError`.

## 6. TODO (порядок обязателен)
1. **[в работе]** Написать новый блок `SettingsView` в `/tmp/opencode/new_settings.kt`:
   `object SettingId { ACCOUNT/DEVICES/SECURITY/PRIVACY/APPEARANCE/CHANNEL }`, обёртка
   `SettingsView`, `SettingsMenu`, `AccountSection`, `DevicesSection`, `SecuritySection`,
   `PrivacySection`, `AppearanceSection`, `ChannelSection` — по карте §4.
   Затем python-splice: `src[:2739]` + new + `src[3425:]` в App.kt.
2. Wiring `onOpenSetting`: инлайн в MainShell `{ arg -> navigator.open(config,
   DetailKind.Setting, arg) }` → параметр MainContent → call site SettingsView (`arg = null`,
   без `onThreat`) → ветка `when(kind)` в Detail (ChatRoom / Setting / AiChat-fallback).
3. Перенести инвайты в AdminView (перед :1735) по §5.
4. `:composeApp:compileKotlinDesktop --offline`.
5. Полная сборка:
   `:composeApp:compileKotlinDesktop :composeApp:compileAndroidMain :app:compileDebugKotlin --offline`.
6. Тесты: `:composeApp:desktopTest --offline --rerun-tasks` (ожидание 104/104).
7. Обновить `docs/ui-redesign-plan.md` (+ этот файл, + журнал).

## 7. Окружение и команды
- `JAVA_HOME=/nix/store/qqngq35hqpiqm5g5w4wgjj2aam09qxif-openjdk-21.0.12+8`
- рабочий каталог: `/home/azrael/PROXMOX_SRV/APP`
- `--offline` обязателен (без сети к Gradle).

## 8. Подтверждённые сигнатуры (перечитаны, не менять)
- `Navigator.open(config: TabConfig, detail: DetailKind, arg: String? = null): Boolean`
  (Navigator.kt :37-42; проверки `detail.argRequired` и прав parentTab);
  `openTab(config, id)` :27-28.
- `DetailKind = { ChatRoom, AiChat, Setting }` (Routes.kt).
- `rememberSectionScroll(scrolls, "settings"|"settings.$section")`, `ScrollPositions` (NavState.kt).
- `AzraelCard(modifier, corner, padding = 18.dp, onClick = null, content: ColumnScope.() -> Unit)`
  — onClick через `Modifier.clickable(onClick)`, дефолтный padding 18.dp НЕ переопределять.
- `AzraelListTile(title, modifier, subtitle = null, leading, trailing, onClick = null,
  enabled = true, titleColor = null, minHeight = AzraelSpace.touchTarget)`
  — leading/trailing `(@Composable () -> Unit)?`.
- `AvatarCircle(profile: AppProfile)` :1570; `Label(text)` :3463; `AccentButton(label,
  modifier = Modifier, onClick)` :3468; `GlassCard` :3455.
- `AppThemeMode` :12-21, `AzraelThemeState` :30-60 (Theme.kt).
- AppClient (AppClient.kt): `deviceList()` :494, `provisionKey()` :714, `provisionRegenerate()` :716,
  `verify()`=AUTH_VERIFY :731, `revokeSession(otherToken)` :738, `profileAutoDelete(days)` :759,
  `profilePrivacyGet()` :761, `invitesMyCode()` :857, `invitesList(active)` :858,
  `invitesGenerate()` :860 (возвращает `available` + `codes`), `invitesArchive(id)` :861,
  `deviceRevoke(devId)`, `rotateDeviceKey()`, `deviceStatus()`, `otpGenerateSecret()`,
  `otpValidate(code)`, `changePassword`, `profileDelete()`, `profileAvatarUrl()`,
  `systemHealth()`, `deviceId()`, `sessionToken()`, `isL2Available`.
- Wiring-точки: MainShell ~:1437, MainContent ~:1617-1641, Detail-ветка ~:1652,
  AdminView заканчивается на :1735 (frozen), далее `:1737 // ---- Сообщения ----`.

## 9. Техника Kotlin и ограничения
- В named-lambda (`onSet = {...}`) НЕЛЬЗЯ `return@...` → использовать `if/else`;
  в `else`-ветке `when(section)` передавать `section ?: ""`.
- `secret=true` для паролей и provisionKey; `otp.code` — `secret=false`.
- i18n: **новых ключей не добавлять**; `t(key, vararg)` форма существует (App.kt :1648);
  legacy I18n.kt :1095+ не трогать.
- Хелперы не удалять: `makeClient` :3429, `fieldColors` :3437, `clickableNoRipple` :3443,
  `SurfaceGlass` :3449, `GlassCard` :3455, `Label` :3463, `AccentButton` :3468,
  `PlaceholderScreen`; `errText` :287, `normalizedEndpoint` :190, `autoDeleteState`,
  `platformName`/`rememberFilePicker`/`stripDataUrl` (Platform.kt), `AppRuntime`,
  `defaultAppSrvPubB64` :127, `defaultGatewayUrl` :129, `AzraelSpace` :170.
- Иконки уже импортированы: KeyboardArrowRight, Dns, Palette, Person, Devices, Shield,
  VisibilityOff, ContentCopy, Archive, Refresh; `ui.{AppThemeMode, AzraelThemeState}`;
  `data.configurable.*`; `ui.components.*`.

## 10. Сверка i18n (финальная, ключи существуют в 3 языках)
Все ключи §4-§5 подтверждены грепом по I18n.kt. Особо проверить при написании:
- `settings.section.{account,devices,security,privacy,appearance,channel}` + `settings.title`.
- `theme.{system,dark,light}`.
- `channel.revokeSession`, `channel.session.revoked`, `login.passwordChanged`,
  `devices.revoke`, `devices.revoked`, `devices.statusRow`, `channel.rotating`,
  `login.provision.label`, `login.provision.ready`, `login.provision.none`.

## 11. Прогресс
- P0 (дизайн-система) — закрыт.
- P1 (навигация) — закрыт.
- P2 (настройки): подготовка 100% (все сигнатуры/границы/i18n проверены),
  сам код нового блока ещё не написан и не вшит.