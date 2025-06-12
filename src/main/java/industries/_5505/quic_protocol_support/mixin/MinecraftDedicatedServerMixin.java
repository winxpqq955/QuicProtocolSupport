package industries._5505.quic_protocol_support.mixin;

import com.mojang.datafixers.DataFixer;
import industries._5505.quic_protocol_support.QuicListener;
import industries._5505.quic_protocol_support.QuicServerProperties;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.SaveLoader;
import net.minecraft.server.WorldGenerationProgressListenerFactory;
import net.minecraft.server.dedicated.MinecraftDedicatedServer;
import net.minecraft.server.dedicated.ServerPropertiesHandler;
import net.minecraft.util.ApiServices;
import net.minecraft.world.level.storage.LevelStorage;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;

@Mixin(MinecraftDedicatedServer.class)
public abstract class MinecraftDedicatedServerMixin extends MinecraftServer {
	@Shadow
	@Final
	static Logger LOGGER;

	@Shadow
	public abstract ServerPropertiesHandler getProperties();

	public MinecraftDedicatedServerMixin(Thread serverThread, LevelStorage.Session session, ResourcePackManager dataPackManager, SaveLoader saveLoader, Proxy proxy, DataFixer dataFixer, ApiServices apiServices, WorldGenerationProgressListenerFactory worldGenerationProgressListenerFactory) {
		super(serverThread, session, dataPackManager, saveLoader, proxy, dataFixer, apiServices, worldGenerationProgressListenerFactory);
	}

	@Inject(method = "setupServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/ServerNetworkIo;bind(Ljava/net/InetAddress;I)V", shift = At.Shift.AFTER))
	private void setupServer(CallbackInfoReturnable<Boolean> callbackInfoReturnable) throws IOException {
		var quicPort = ((QuicServerProperties) getProperties()).getQuicPort();
		if (quicPort > 0) {
			LOGGER.info("Starting Minecraft server (QUIC) on {}:{}", getServerIp().isEmpty() ? "*" : getServerIp(), quicPort);

			var networkIo = (ServerNetworkIoAccessor) getNetworkIo();
			QuicListener.startQuicListener(
				(MinecraftDedicatedServer) (Object) this,
				new InetSocketAddress(
					getServerIp().isEmpty() ? null : InetAddress.getByName(getServerIp()),
					quicPort
				),
				networkIo.getChannels(),
				networkIo.getConnections()
			);
		}
	}
}
