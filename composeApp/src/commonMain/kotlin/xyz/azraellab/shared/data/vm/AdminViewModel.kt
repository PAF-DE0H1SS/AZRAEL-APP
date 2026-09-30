package xyz.azraellab.shared.data.vm

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import xyz.azraellab.shared.core.api.AppClient
import xyz.azraellab.shared.data.UiState
import xyz.azraellab.shared.data.Repos
import xyz.azraellab.shared.data.model.FreezeUserDto
import xyz.azraellab.shared.data.model.HealthDto
import xyz.azraellab.shared.data.model.InviteDto
import xyz.azraellab.shared.data.model.InviteSummaryDto

/**
 * Владeльческий раздел «Админ»: заморозка/разморозка, приглашения и статус канала.
 * Сетевые списки живут в StateFlow, однократные действия — suspend-функции.
 */
class AdminViewModel(
    private val client: AppClient,
    externalScope: CoroutineScope? = null
) : AppViewModel(externalScope) {

    private val _frozen = MutableStateFlow<UiState<List<FreezeUserDto>>>(UiState.Loading)
    val frozen: StateFlow<UiState<List<FreezeUserDto>>> = _frozen.asStateFlow()

    private val _invites = MutableStateFlow<UiState<List<InviteDto>>>(UiState.Loading)
    val invites: StateFlow<UiState<List<InviteDto>>> = _invites.asStateFlow()

    private val _myCode = MutableStateFlow<UiState<String>>(UiState.Loading)
    val myCode: StateFlow<UiState<String>> = _myCode.asStateFlow()

    private val _health = MutableStateFlow<UiState<HealthDto>>(UiState.Ready(HealthDto()))
    val health: StateFlow<UiState<HealthDto>> = _health.asStateFlow()

    fun refreshAll() {
        loadInto(_frozen) { Repos.frozenUsers(client) }
        loadInto(_invites) { Repos.invites(client, true) }
        loadInto(_myCode) { Repos.myInviteCode(client) }
    }

    fun refreshFrozen() = loadInto(_frozen) { Repos.frozenUsers(client) }

    fun refreshInvites(active: Boolean = true) = loadInto(_invites) { Repos.invites(client, active) }

    /** Загрузка (активных или архивных) приглашений с возвратом результата для сообщений. */
    suspend fun loadInvites(active: Boolean): UiState<List<InviteDto>> {
        _invites.value = UiState.Loading
        val st = Repos.invites(client, active)
        _invites.value = st
        return st
    }

    fun refreshHealth() = loadInto(_health) { Repos.systemHealth(client) }

    suspend fun generateInvites(): UiState<InviteSummaryDto> = Repos.generateInvites(client)

    suspend fun unfreeze(username: String): UiState<Unit> = Repos.unfreezeUser(client, username)

    suspend fun archive(id: Long): UiState<Unit> = Repos.archiveInvite(client, id)
}