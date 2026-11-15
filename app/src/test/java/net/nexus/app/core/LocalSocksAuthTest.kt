package net.nexus.app.core

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.DataInputStream
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

class LocalSocksAuthTest {

    /** Minimal SOCKS5 server that demands username/password auth and records what it received. */
    @Test
    fun `java socks client authenticates to the loopback inbound with session credentials`() {
        ServerSocket(0).use { server ->
            val (user, pass) = LocalSocksAuth.rotate(server.localPort)
            var gotUser = ""
            var gotPass = ""
            val srv = thread {
                server.accept().use { s ->
                    val inp = DataInputStream(s.getInputStream())
                    val out = s.getOutputStream()
                    inp.readByte() // version
                    val n = inp.readUnsignedByte()
                    val methods = ByteArray(n).also { inp.readFully(it) }
                    require(methods.contains(0x02)) { "client did not offer user/pass auth" }
                    out.write(byteArrayOf(5, 2)); out.flush()
                    inp.readByte() // auth version
                    gotUser = String(ByteArray(inp.readUnsignedByte()).also { inp.readFully(it) })
                    gotPass = String(ByteArray(inp.readUnsignedByte()).also { inp.readFully(it) })
                    out.write(byteArrayOf(1, 0)); out.flush()
                    // CONNECT request: ver, cmd, rsv, atyp + addr + port
                    inp.readByte(); inp.readByte(); inp.readByte()
                    when (inp.readUnsignedByte()) {
                        1 -> inp.readFully(ByteArray(4))
                        3 -> inp.readFully(ByteArray(inp.readUnsignedByte()))
                        4 -> inp.readFully(ByteArray(16))
                    }
                    inp.readShort()
                    out.write(byteArrayOf(5, 0, 0, 1, 127, 0, 0, 1, 0, 80)); out.flush()
                }
            }
            val proxy = Proxy(Proxy.Type.SOCKS, InetSocketAddress("127.0.0.1", server.localPort))
            Socket(proxy).use { it.connect(InetSocketAddress.createUnresolved("example.com", 443), 3_000) }
            srv.join(3_000)
            assertEquals(user, gotUser)
            assertEquals(pass, gotPass)
        }
    }
}
