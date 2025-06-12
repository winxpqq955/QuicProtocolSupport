package industries._5505.quic_protocol_support.mixin;

import io.netty.channel.Channel;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import net.minecraft.network.ClientConnection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.SocketAddress;

@Mixin(ClientConnection.class)
public class ClientConnectionMixin {
	@Shadow
	private Channel channel;

	@Shadow
	private SocketAddress address;

	@Shadow
	private boolean encrypted;

	@Redirect(method = "channelActive", at = @At(value = "FIELD", target = "Lnet/minecraft/network/ClientConnection;address:Ljava/net/SocketAddress;"))
	private void channelActiveSetAddress(ClientConnection instance, SocketAddress value) {
		if (channel instanceof QuicStreamChannel) {
			address = channel.parent().remoteAddress();
		} else {
			address = value;
		}
	}

	@Redirect(method = "disconnect", at = @At(value = "FIELD", target = "Lnet/minecraft/network/ClientConnection;channel:Lio/netty/channel/Channel;"))
	private Channel disconnectGetChannel(ClientConnection instance) {
		if (channel instanceof QuicStreamChannel) {
			return channel.parent();
		} else {
			return channel;
		}
	}

	@Inject(method = "setupEncryption", at = @At("HEAD"), cancellable = true)
	private void setupEncryption(CallbackInfo callbackInfo) {
		if (encrypted) callbackInfo.cancel();
	}
}
