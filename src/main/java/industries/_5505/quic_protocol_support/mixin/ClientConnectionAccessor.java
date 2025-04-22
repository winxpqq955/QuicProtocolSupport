package industries._5505.quic_protocol_support.mixin;

import io.netty.channel.Channel;
import net.minecraft.network.ClientConnection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.net.SocketAddress;

@Mixin(ClientConnection.class)
public interface ClientConnectionAccessor {
	@Accessor
	Channel getChannel();

	@Accessor
	void setAddress(SocketAddress address);

	@Accessor
	void setEncrypted(boolean encrypted);
}
