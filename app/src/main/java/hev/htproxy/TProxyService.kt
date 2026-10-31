package hev.htproxy

/**
 * JNI bindings for libhev-socks5-tunnel.so (heiher/hev-socks5-tunnel).
 * Used by INCY, Happ, and v2rayNG to bridge Android's VpnService Layer-3 TUN interface
 * to a local SOCKS5 proxy via high-performance lwIP stack in C.
 */
object TProxyService {
    init {
        System.loadLibrary("hev-socks5-tunnel")
    }

    @JvmStatic
    external fun TProxyStartService(configPath: String, fd: Int): Boolean

    @JvmStatic
    external fun TProxyStopService(): Boolean

    @JvmStatic
    external fun TProxyIsRunning(): Boolean

    @JvmStatic
    external fun TProxyGetStats(): LongArray
}

