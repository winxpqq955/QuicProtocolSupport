package industries._5505.quic_protocol_support;

import industries._5505.quic_protocol_support.mixin.ClientConnectionAccessor;
import industries._5505.quic_protocol_support.quic.Blake3ConnectionIdGenerator;
import industries._5505.quic_protocol_support.quic.Blake3TokenHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.incubator.codec.quic.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.RateLimitedConnection;
import net.minecraft.server.dedicated.MinecraftDedicatedServer;
import net.minecraft.server.network.ServerHandshakeNetworkHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.net.InetSocketAddress;
import java.security.SecureRandom;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class QuicListener {
    public static final String APPLICATION_PROTOCOL = "minecraft";
    private static final long MAX_DATA = 2_097_152L; // 2 MiB

    private static final Logger logger = LogManager.getLogger();

    public static void startQuicListener(
            MinecraftDedicatedServer server,
            InetSocketAddress socketAddress,
            List<ChannelFuture> channels,
            List<ClientConnection> connections
    ) {
        final boolean useEpoll = Epoll.isAvailable() && server.isUsingNativeTransport();

        final File config = FabricLoader.getInstance().getConfigDir().resolve("quic").toFile();
        final File keyFile = new File(config, "key.pem");
        final File certificateFile = new File(config, "certificate.pem");

        if (!keyFile.exists() || !certificateFile.exists()) {
            logger.error("TLS key or certificate not found; make sure that it is in the right place");
            return;
        }

        final QuicSslContext context = QuicSslContextBuilder.forServer(keyFile, null, certificateFile)
            .applicationProtocols(APPLICATION_PROTOCOL)
            .build();

        // Generate random keys for Blake3 handlers
        final byte[] tokenKey = new byte[32];
        final byte[] connectionIdKey = new byte[32];
        final SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(tokenKey);
        secureRandom.nextBytes(connectionIdKey);

        final var codec = new QuicServerCodecBuilder()
            .sslContext(context)
            .maxIdleTimeout(5, TimeUnit.SECONDS)
            .initialMaxData(MAX_DATA)
            .initialMaxStreamsBidirectional(1)
            .initialMaxStreamDataBidirectionalRemote(MAX_DATA)
            .congestionControlAlgorithm(QuicCongestionControlAlgorithm.BBR2)
            .tokenHandler(new Blake3TokenHandler(tokenKey))
            .connectionIdAddressGenerator(new Blake3ConnectionIdGenerator(connectionIdKey))
            .resetTokenGenerator(QuicResetTokenGenerator.signGenerator())
            .handler(new ChannelInboundHandlerAdapter() {
                @Override
                public void userEventTriggered(ChannelHandlerContext context, Object event) {
                    if (event instanceof QuicPathEvent.PeerMigrated peerMigrated) {
                        for (ClientConnection connection : connections) {
                            final var oldAddress = connection.getAddress();
                            final InetSocketAddress newAddress = peerMigrated.remote();
                            final ClientConnectionAccessor accessor = (ClientConnectionAccessor) connection;

                            if (accessor.getChannel() instanceof QuicStreamChannel
                                && accessor.getChannel().parent() == context.channel()
                                && !newAddress.equals(oldAddress)
                            ) {
                                accessor.setAddress(newAddress);
                                logger.info("{} was migrated to {}", oldAddress, newAddress);
                            }
                        }
                    }

                    context.fireUserEventTriggered(event);
                }

                @Override
                public boolean isSharable() {
                    return true;
                }
            })
            .streamHandler(new ChannelInitializer<QuicStreamChannel>() {
                @Override
                protected void initChannel(QuicStreamChannel channel) {
                    final io.netty.channel.ChannelPipeline channelPipeline = channel.pipeline();
                    ClientConnection.addHandlers(channelPipeline, NetworkSide.SERVERBOUND);

                    final int rateLimit = server.getRateLimit();
                    final ClientConnection clientConnection = rateLimit > 0
                        ? new RateLimitedConnection(rateLimit)
                        : new ClientConnection(NetworkSide.SERVERBOUND);

                    ((ClientConnectionAccessor) clientConnection).setEncrypted(true);
                    connections.add(clientConnection);

                    channelPipeline.addLast("packet_handler", clientConnection);
                    clientConnection.setPacketListener(new ServerHandshakeNetworkHandler(server, clientConnection));
                }
            })
            .build();

        try {
            final ChannelFuture channelFuture = new Bootstrap()
                .group(useEpoll ? ClientConnection.EPOLL_CLIENT_IO_GROUP.get() : ClientConnection.CLIENT_IO_GROUP.get())
                .channel(useEpoll ? EpollDatagramChannel.class : NioDatagramChannel.class)
                .handler(codec)
                .bind(socketAddress)
                .sync();

            channels.add(channelFuture);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while starting QUIC listener", e);
        }
    }
}
