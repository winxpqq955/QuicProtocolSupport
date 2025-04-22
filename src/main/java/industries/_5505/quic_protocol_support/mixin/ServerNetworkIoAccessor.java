package industries._5505.quic_protocol_support.mixin;

import io.netty.channel.ChannelFuture;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.ServerNetworkIo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(ServerNetworkIo.class)
public interface ServerNetworkIoAccessor {
	@Accessor
	List<ChannelFuture> getChannels();

	@Accessor
	List<ClientConnection> getConnections();
}
