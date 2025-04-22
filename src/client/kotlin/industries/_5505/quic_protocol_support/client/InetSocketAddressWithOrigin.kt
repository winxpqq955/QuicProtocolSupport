package industries._5505.quic_protocol_support.client

import net.minecraft.client.network.ServerAddress
import java.net.InetAddress
import java.net.InetSocketAddress

class InetSocketAddressWithOrigin(addr: InetAddress, port: Int, val origin: ServerAddress) : InetSocketAddress(addr, port)
