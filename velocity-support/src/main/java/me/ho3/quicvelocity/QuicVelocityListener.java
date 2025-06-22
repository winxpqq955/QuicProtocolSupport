package me.ho3.quicvelocity;

import com.velocitypowered.api.proxy.ProxyServer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.incubator.codec.quic.*;
import me.ho3.quicvelocity.config.QuicConfig;
import me.ho3.quicvelocity.quic.Blake3ConnectionIdGenerator;
import me.ho3.quicvelocity.quic.Blake3TokenHandler;
import org.slf4j.Logger;

import java.io.File;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

public class QuicVelocityListener {

    private final Logger logger;
    private final ProxyServer server;

    public QuicVelocityListener(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
    }    public void startQuicListener(InetSocketAddress bindAddress, Path configDir) {
        //有空写
        final boolean useEpoll = false; // Epoll.isAvailable();
        logger.info("Using {} event loop", useEpoll ? "Epoll" : "NIO");

        final File config = configDir.resolve(QuicConfig.QUIC_CONFIG_DIR).toFile();
        if (!config.exists()) {
            config.mkdirs();
            logger.info("Created QUIC config directory: {}", config.getAbsolutePath());
        }

        final File keyFile = new File(config, QuicConfig.KEY_FILE);
        final File certificateFile = new File(config, QuicConfig.CERTIFICATE_FILE);

        if (!keyFile.exists() || !certificateFile.exists()) {
            logger.error("TLS key or certificate not found; make sure that it is in the right place");
            logger.error("Expected key file: {}", keyFile.getAbsolutePath());
            logger.error("Expected certificate file: {}", certificateFile.getAbsolutePath());
            logger.error("You must use a valid, CA-signed TLS certificate for QUIC to work properly");
            return;
        }

        final QuicSslContext context;
        try {
            context = QuicSslContextBuilder.forServer(keyFile, null, certificateFile)
                .applicationProtocols(QuicConfig.APPLICATION_PROTOCOL)
                .build();
            logger.info("Successfully loaded TLS certificate and key");
        } catch (Exception e) {
            logger.error("Failed to create QUIC SSL context", e);
            return;
        }

        // Generate random keys for Blake3 handlers
        final byte[] tokenKey = new byte[QuicConfig.BLAKE3_KEY_LENGTH];
        final byte[] connectionIdKey = new byte[QuicConfig.BLAKE3_KEY_LENGTH];
        final SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(tokenKey);
        secureRandom.nextBytes(connectionIdKey);
        logger.debug("Generated Blake3 cryptographic keys");

        final var codec = new QuicServerCodecBuilder()
            .sslContext(context)
            .maxIdleTimeout(QuicConfig.DEFAULT_IDLE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .initialMaxData(QuicConfig.DEFAULT_MAX_DATA)
            .initialMaxStreamsBidirectional(QuicConfig.DEFAULT_MAX_BIDIRECTIONAL_STREAMS)
            .initialMaxStreamDataBidirectionalRemote(QuicConfig.DEFAULT_MAX_DATA)
            .congestionControlAlgorithm(QuicCongestionControlAlgorithm.BBR)
            .tokenHandler(new Blake3TokenHandler(tokenKey))
            .connectionIdAddressGenerator(new Blake3ConnectionIdGenerator(connectionIdKey))
            .resetTokenGenerator(QuicResetTokenGenerator.signGenerator())
            .handler(new ChannelInboundHandlerAdapter() {
                @Override
                public void userEventTriggered(ChannelHandlerContext context, Object event) {
                    if (event instanceof QuicPathEvent.PeerMigrated peerMigrated) {
                        logger.info("QUIC peer migrated from {} to {}",
                            context.channel().remoteAddress(), peerMigrated.remote());
                    }
                    context.fireUserEventTriggered(event);
                }

                @Override
                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                    logger.warn("Error in QUIC connection handler: {}", cause.getMessage());
                    logger.debug("Full exception:", cause);
                }

                @Override
                public boolean isSharable() {
                    return true;
                }
            })
            .streamHandler(new ChannelInitializer<QuicStreamChannel>() {
                @Override
                protected void initChannel(QuicStreamChannel channel) {
                    logger.debug("New QUIC stream connected: {}", channel.remoteAddress());

                    // Add Velocity-specific protocol handling
                    channel.pipeline().addLast("quic-velocity-handler",
                        new QuicVelocityStreamHandler(server, logger));
                }
            })
            .build();        try {
            // Use NIO event loop group for now
            // TODO: Add Epoll support when dependencies are resolved
            final EventLoopGroup eventLoopGroup = new NioEventLoopGroup();

            final ChannelFuture channelFuture = new Bootstrap()
                .group(eventLoopGroup)
                .channel(NioDatagramChannel.class)
                .handler(codec)
                .bind(bindAddress)
                .sync();

            logger.info("QUIC listener successfully started on {}", bindAddress);
            logger.info("Configuration: max_data={}, idle_timeout={}s, max_streams={}",
                QuicConfig.DEFAULT_MAX_DATA,
                QuicConfig.DEFAULT_IDLE_TIMEOUT_SECONDS,
                QuicConfig.DEFAULT_MAX_BIDIRECTIONAL_STREAMS);            // Add shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    logger.info("Shutting down QUIC listener...");
                    channelFuture.channel().close().sync();
                    eventLoopGroup.shutdownGracefully().sync();
                    logger.info("QUIC listener stopped");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warn("Interrupted during QUIC shutdown");
                }
            }));

        } catch (Exception e) {
            logger.error("Failed to start QUIC listener on {}", bindAddress, e);
        }
    }
}
