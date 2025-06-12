@file:JvmName("QuicConnect")

package industries._5505.quic_protocol_support.client

import industries._5505.quic_protocol_support.APPLICATION_PROTOCOL
import industries._5505.quic_protocol_support.mixin.ClientConnectionAccessor
import io.netty.bootstrap.Bootstrap
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelInitializer
import io.netty.channel.epoll.Epoll
import io.netty.channel.epoll.EpollDatagramChannel
import io.netty.channel.socket.nio.NioDatagramChannel
import io.netty.incubator.codec.quic.*
import net.minecraft.network.ClientConnection
import net.minecraft.network.NetworkSide
import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit

private const val MAX_DATA: Long = 2_097_152 // 2 MiB

fun connectUsingQuic(address: InetSocketAddress, useEpollIfAvailable: Boolean, connection: ClientConnection): ChannelFuture {
	val useEpoll = Epoll.isAvailable() && useEpollIfAvailable

	val context = QuicSslContextBuilder.forClient()
		// uncomment the line below if you want to accept self-signed certificates
		//.trustManager(io.netty.handler.ssl.util.InsecureTrustManagerFactory.INSTANCE)
		.applicationProtocols(APPLICATION_PROTOCOL)
		.build()

	val codec = QuicClientCodecBuilder()
		.sslContext(context)
		.maxIdleTimeout(5, TimeUnit.SECONDS)
		.initialMaxData(MAX_DATA)
		.initialMaxStreamDataBidirectionalLocal(MAX_DATA)
		.congestionControlAlgorithm(QuicCongestionControlAlgorithm.BBR2)
		.build()

	val channel = Bootstrap()
		.group(if (useEpoll) ClientConnection.EPOLL_CLIENT_IO_GROUP.get() else ClientConnection.CLIENT_IO_GROUP.get())
		.channel(if (useEpoll) EpollDatagramChannel::class.java else NioDatagramChannel::class.java)
		.handler(codec)
		.bind(0)
		.sync()
		.channel()

	return QuicChannel.newBootstrap(channel)
		.streamHandler(ChannelInboundHandlerAdapter())
		.remoteAddress(address)
		.connect()
		.get()
		.createStream(QuicStreamType.BIDIRECTIONAL, object : ChannelInitializer<QuicStreamChannel>() {
			override fun initChannel(channel: QuicStreamChannel) {
				(connection as ClientConnectionAccessor).setEncrypted(true)
				val pipeline = channel.pipeline()
				ClientConnection.addHandlers(pipeline, NetworkSide.CLIENTBOUND)
				pipeline.addLast("packet_handler", connection)
			}
		})
		.get()
		.parent()
		.newSucceededFuture()
}
