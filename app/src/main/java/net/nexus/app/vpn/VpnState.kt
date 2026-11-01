package net.nexus.app.vpn

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class VpnStatus { DISCONNECTED, CONNECTING, CONNECTED, ERROR }

/** Process-wide VPN state; written by [NexusVpnService], observed by the UI. */
object VpnState {
    private val _status = MutableStateFlow(VpnStatus.DISCONNECTED)
    val status: StateFlow<VpnStatus> = _status

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _connectedAt = MutableStateFlow(0L)
    val connectedAt: StateFlow<Long> = _connectedAt

    private val _serverId = MutableStateFlow<String?>(null)
    val serverId: StateFlow<String?> = _serverId

    private val _serverName = MutableStateFlow<String?>(null)
    val serverName: StateFlow<String?> = _serverName

    private val _isAutoSelected = MutableStateFlow(false)
    val isAutoSelected: StateFlow<Boolean> = _isAutoSelected

    private val _sessionRxBytes = MutableStateFlow(0L)
    val sessionRxBytes: StateFlow<Long> = _sessionRxBytes

    private val _sessionTxBytes = MutableStateFlow(0L)
    val sessionTxBytes: StateFlow<Long> = _sessionTxBytes

    private val _rxSpeedBps = MutableStateFlow(0L)
    val rxSpeedBps: StateFlow<Long> = _rxSpeedBps

    private val _txSpeedBps = MutableStateFlow(0L)
    val txSpeedBps: StateFlow<Long> = _txSpeedBps

    /** Loopback SOCKS port of the running core (0 = not running). Used by the speed test to go through the VPN. */
    @Volatile
    var socksPort: Int = 0

    fun updateTraffic(rxBytes: Long, txBytes: Long, rxSpeed: Long = 0L, txSpeed: Long = 0L) {
        _sessionRxBytes.value = rxBytes
        _sessionTxBytes.value = txBytes
        _rxSpeedBps.value = rxSpeed
        _txSpeedBps.value = txSpeed
    }

    fun connecting(serverId: String, isAuto: Boolean = false, serverName: String? = null) {
        _serverId.value = serverId
        _isAutoSelected.value = isAuto
        _serverName.value = serverName
        _error.value = null
        _status.value = VpnStatus.CONNECTING
    }

    fun connected(serverId: String? = null, isAuto: Boolean = false, serverName: String? = null) {
        _connectedAt.value = System.currentTimeMillis()
        _sessionRxBytes.value = 0L
        _sessionTxBytes.value = 0L
        _rxSpeedBps.value = 0L
        _txSpeedBps.value = 0L
        if (serverId != null) _serverId.value = serverId
        if (serverName != null) _serverName.value = serverName
        _isAutoSelected.value = isAuto
        _status.value = VpnStatus.CONNECTED
    }

    fun disconnected() {
        socksPort = 0
        _connectedAt.value = 0
        _sessionRxBytes.value = 0L
        _sessionTxBytes.value = 0L
        _rxSpeedBps.value = 0L
        _txSpeedBps.value = 0L
        _serverId.value = null
        _serverName.value = null
        _isAutoSelected.value = false
        _status.value = VpnStatus.DISCONNECTED
    }

    fun failed(message: String) {
        socksPort = 0
        _connectedAt.value = 0
        _sessionRxBytes.value = 0L
        _sessionTxBytes.value = 0L
        _rxSpeedBps.value = 0L
        _txSpeedBps.value = 0L
        _serverId.value = null
        _serverName.value = null
        _isAutoSelected.value = false
        _error.value = message
        _status.value = VpnStatus.ERROR
    }
}
