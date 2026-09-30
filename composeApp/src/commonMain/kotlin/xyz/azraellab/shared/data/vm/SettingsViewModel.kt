package xyz.azraellab.shared.data.vm

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import xyz.azraellab.shared.core.api.AppClient
import xyz.azraellab.shared.data.Repos
import xyz.azraellab.shared.data.UiState
import xyz.azraellab.shared.data.model.DeviceListDto
import xyz.azraellab.shared.data.model.PrivacyDto
import xyz.azraellab.shared.data.model.ProvisionInfoDto

/**
 * Раздел «Настройки»: профиль, приватность, устройства, provision-ключ и OTP.
 * Сетевые списки живут в StateFlow, однократные действия — suspend-функции
 * поверх [Repos], чтобы экран не знал про HTTP и JsonObject.
 */
class SettingsViewModel(
    private val client: AppClient,
    externalScope: CoroutineScope? = null
) : AppViewModel(externalScope) {

    private val _devices = MutableStateFlow<UiState<DeviceListDto>>(UiState.Loading)
    val devices: StateFlow<UiState<DeviceListDto>> = _devices.asStateFlow()

    private val _privacy = MutableStateFlow<UiState<PrivacyDto>>(UiState.Loading)
    val privacy: StateFlow<UiState<PrivacyDto>> = _privacy.asStateFlow()

    private val _provision = MutableStateFlow<UiState<ProvisionInfoDto>>(UiState.Loading)
    val provision: StateFlow<UiState<ProvisionInfoDto>> = _provision.asStateFlow()

    private val _avatarUrl = MutableStateFlow<UiState<String>>(UiState.Loading)
    val avatarUrl: StateFlow<UiState<String>> = _avatarUrl.asStateFlow()

    fun refreshAll() {
        refreshDevices()
        refreshPrivacy()
        refreshProvision()
        loadAvatar()
    }

    fun refreshDevices() = loadInto(_devices) { Repos.devices(client) }

    fun refreshPrivacy() = loadInto(_privacy) { Repos.privacy(client) }

    fun refreshProvision() = loadInto(_provision) { Repos.provisionInfo(client) }

    fun loadAvatar() = loadInto(_avatarUrl) { Repos.profileAvatarUrl(client) }

    /** Показать/выдать provision-ключ; после ответа обновляет инфо о ключе. */
    suspend fun showProvisionKey(): UiState<String> {
        val st = Repos.provisionKeyShow(client)
        refreshProvision()
        return st
    }

    /** Перевыпустить provision-ключ; после ответа обновляет инфо о ключе. */
    suspend fun regenerateProvisionKey(): UiState<String> {
        val st = Repos.provisionRegen(client)
        refreshProvision()
        return st
    }

    suspend fun savePrivacy(hidden: Boolean, whoSearch: String, whoWrite: String): UiState<Unit> =
        Repos.privacySave(client, hidden, whoSearch, whoWrite)

    suspend fun revokeDevice(devId: String): UiState<Unit> = Repos.deviceRevoke(client, devId)

    suspend fun rotateDeviceKey(): UiState<Unit> = Repos.deviceRotate(client)

    suspend fun deviceStatusLabel(): UiState<String> = Repos.deviceStatusLine(client)

    suspend fun otpGenerateSecret(): UiState<String> = Repos.otpSecret(client)

    suspend fun otpValidate(code: String): UiState<Boolean> = Repos.otpCheck(client, code)

    suspend fun changePassword(old: String, new: String): UiState<Unit> = Repos.passwordChange(client, old, new)

    suspend fun revokeSession(): UiState<Unit> = Repos.sessionRevoke(client)

    suspend fun verifySession(): UiState<String> = Repos.verifySession(client)

    suspend fun healthText(): UiState<String> = Repos.healthText(client)

    suspend fun saveProfile(name: String?, tag: String?, gender: String?): UiState<Unit> =
        Repos.profileSave(client, name, tag, gender)

    suspend fun setLang(code: String): UiState<Unit> = Repos.profileSetLang(client, code)

    suspend fun setAutoDelete(days: Int?): UiState<Int?> = Repos.autoDeleteSet(client, days)

    suspend fun avatarSet(base64: String, mime: String): UiState<String> {
        val st = Repos.profileAvatarSet(client, base64, mime)
        if (st is UiState.Ready) _avatarUrl.value = st
        return st
    }

    suspend fun deleteProfile(): UiState<Unit> = Repos.profileDelete(client)

    /** Текущее устройство (для бейджа «это устройство»). */
    fun currentDeviceId(): String? = client.deviceId()

    fun isL2Available(): Boolean = client.isL2Available
}