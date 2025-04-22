package industries._5505.quic_protocol_support.mixin;

import industries._5505.quic_protocol_support.QuicServerProperties;
import net.minecraft.server.dedicated.AbstractPropertiesHandler;
import net.minecraft.server.dedicated.ServerPropertiesHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Properties;

@Mixin(ServerPropertiesHandler.class)
public abstract class ServerPropertiesHandlerMixin extends AbstractPropertiesHandler<ServerPropertiesHandler> implements QuicServerProperties {
	@Unique
	private int quicPort;

	public ServerPropertiesHandlerMixin(Properties properties) {
		super(properties);
	}

	@Override
	public int getQuicPort() {
		return quicPort;
	}

	@Inject(method = "<init>", at = @At("TAIL"))
	private void init(CallbackInfo callbackInfo) {
		quicPort = this.getInt("quic-port", -1);
	}
}
