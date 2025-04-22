package industries._5505.quic_protocol_support.mixin.client;

import industries._5505.quic_protocol_support.client.InetSocketAddressWithOrigin;
import industries._5505.quic_protocol_support.client.QuicConnect;
import industries._5505.quic_protocol_support.client.QuicServerAddressProperties;
import io.netty.channel.ChannelFuture;
import net.minecraft.network.ClientConnection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.InetSocketAddress;

@Mixin(ClientConnection.class)
public class ClientConnectionMixin {
	@Inject(method = "connect(Ljava/net/InetSocketAddress;ZLnet/minecraft/network/ClientConnection;)Lio/netty/channel/ChannelFuture;", at = @At("HEAD"), cancellable = true)
	private static void connect(InetSocketAddress address, boolean useEpoll, ClientConnection connection, CallbackInfoReturnable<ChannelFuture> callbackInfoReturnable) {
		if (address instanceof InetSocketAddressWithOrigin addressWithOrigin) {
			if (((QuicServerAddressProperties) (Object) addressWithOrigin.getOrigin()).isQuic()) {
				callbackInfoReturnable.setReturnValue(QuicConnect.connectUsingQuic(address, useEpoll, connection));
			}
		}
	}
}
