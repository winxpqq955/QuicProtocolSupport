package industries._5505.quic_protocol_support.mixin.client;

import industries._5505.quic_protocol_support.client.InetSocketAddressWithOrigin;
import net.minecraft.client.network.AddressResolver;
import net.minecraft.client.network.ServerAddress;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.net.InetAddress;
import java.net.InetSocketAddress;

@Mixin(AddressResolver.class)
public interface AddressResolverMixin {
	@Redirect(method = "method_36903", at = @At(value = "NEW", target = "(Ljava/net/InetAddress;I)Ljava/net/InetSocketAddress;"))
	private static InetSocketAddress method_36903(InetAddress addr, int port, ServerAddress serverAddress) {
		return new InetSocketAddressWithOrigin(addr, port, serverAddress);
	}
}
