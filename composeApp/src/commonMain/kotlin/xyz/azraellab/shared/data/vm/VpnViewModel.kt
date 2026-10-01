package xyz.azraellab.shared.data.vm

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import xyz.azraellab.shared.core.api.AppClient
import xyz.azraellab.shared.data.UiState
import xyz.azraellab.shared.data.Repos
import xyz.azraellab.shared.data.model.AwgStatusDto
import xyz.azraellab.shared.data.model.IncysDownloadDto
import xyz.azraellab.shared.data.model.VpnServerDto
import xyz.azraellab.shared.data.model.VpnSummaryDto

/**
 * Состояние вкладки VPN: сводка/список/refresh живут в StateFlow, A-WG и Incys - тоже.
 * Список серверов [servers] перезагружается по выбранной папке (null = все).
 */
class VpnViewModel(
    private val client: AppClient,
    externalScope: CoroutineScope? = null
) : AppViewModel(externalScope) {

    private val _summary = MutableStateFlow<UiState<VpnSummaryDto>>(UiState.Loading)
    val summary: StateFlow<UiState<VpnSummaryDto>> = _summary.asStateFlow()

    private val _awg = MutableStateFlow<UiState<AwgStatusDto>>(UiState.Loading)
    val awg: StateFlow<UiState<AwgStatusDto>> = _awg.asStateFlow()

    private val _incys = MutableStateFlow<UiState<List<IncysDownloadDto>>>(UiState.Loading)
    val incys: StateFlow<UiState<List<IncysDownloadDto>>> = _incys.asStateFlow()

    private val _servers = MutableStateFlow<UiState<List<VpnServerDto>>>(UiState.Ready(emptyList()))
    val servers: StateFlow<UiState<List<VpnServerDto>>> = _servers.asStateFlow()

    fun refreshAll() {
        loadInto(_summary) { Repos.vpnSummary(client) }
        loadInto(_awg) { Repos.awgStatus(client) }
        loadInto(_incys) { Repos.incysDownloads(client) }
    }

    fun loadServers(folder: String?) = loadInto(_servers) { Repos.vpnServers(client, folder) }

    suspend fun refreshServerLists(): UiState<VpnSummaryDto> = Repos.vpnRefresh(client)
}