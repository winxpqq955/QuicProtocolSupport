package industries._5505.quic_protocol_support.client;

import net.minecraft.client.network.ServerAddress;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public class InetSocketAddressWithOrigin extends InetSocketAddress {
    private final ServerAddress origin;
    
    public InetSocketAddressWithOrigin(InetAddress addr, int port, ServerAddress origin) {
        super(addr, port);
        this.origin = origin;
    }
    
    public ServerAddress getOrigin() {
        return origin;
    }
}
