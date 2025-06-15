package industries._5505.quic_protocol_support.client;

import industries._5505.quic_protocol_support.mixin.ClientConnectionAccessor;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.incubator.codec.quic.*;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import static industries._5505.quic_protocol_support.QuicListener.APPLICATION_PROTOCOL;

public class QuicConnect {
    private static final long MAX_DATA = 2_097_152L; // 2 MiB

    public static ChannelFuture connectUsingQuic(InetSocketAddress address, boolean useEpollIfAvailable, ClientConnection connection) {
        final boolean useEpoll = Epoll.isAvailable() && useEpollIfAvailable;

        final QuicSslContext context = QuicSslContextBuilder.forClient()
            // uncomment the line below if you want to accept self-signed certificates
            //.trustManager(io.netty.handler.ssl.util.InsecureTrustManagerFactory.INSTANCE)
            .applicationProtocols(APPLICATION_PROTOCOL)
            .build();

        final var codec = new QuicClientCodecBuilder()
            .sslContext(context)
            .maxIdleTimeout(5, TimeUnit.SECONDS)
            .initialMaxData(MAX_DATA)
            .initialMaxStreamDataBidirectionalLocal(MAX_DATA)
            .congestionControlAlgorithm(QuicCongestionControlAlgorithm.BBR2)
            .build();

        final io.netty.channel.Channel channel;
        try {
            channel = new Bootstrap()
                .group(useEpoll ? ClientConnection.EPOLL_CLIENT_IO_GROUP.get() : ClientConnection.CLIENT_IO_GROUP.get())
                .channel(useEpoll ? EpollDatagramChannel.class : NioDatagramChannel.class)
                .handler(codec)
                .bind(0)
                .sync()
                .channel();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while binding channel", e);
        }

        try {
            return QuicChannel.newBootstrap(channel)
                .streamHandler(new ChannelInboundHandlerAdapter())
                .remoteAddress(address)
                .connect()
                .get()
                .createStream(QuicStreamType.BIDIRECTIONAL, new ChannelInitializer<QuicStreamChannel>() {
                    @Override
                    protected void initChannel(QuicStreamChannel channel) {
                        ((ClientConnectionAccessor) connection).setEncrypted(true);
                        final io.netty.channel.ChannelPipeline pipeline = channel.pipeline();
                        ClientConnection.addHandlers(pipeline, NetworkSide.CLIENTBOUND);
                        pipeline.addLast("packet_handler", connection);
                    }
                })
                .get()
                .parent()
                .newSucceededFuture();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create QUIC stream", e);
        }
    }
}
