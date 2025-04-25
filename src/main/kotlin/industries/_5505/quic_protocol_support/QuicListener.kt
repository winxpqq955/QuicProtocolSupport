@file:JvmName("QuicListener")

package industries._5505.quic_protocol_support

import industries._5505.quic_protocol_support.mixin.ClientConnectionAccessor
import industries._5505.quic_protocol_support.quic.Blake3ConnectionIdGenerator
import industries._5505.quic_protocol_support.quic.Blake3TokenHandler
import io.netty.bootstrap.Bootstrap
import io.netty.channel.*
import io.netty.channel.epoll.Epoll
import io.netty.channel.epoll.EpollDatagramChannel
import io.netty.channel.socket.nio.NioDatagramChannel
import io.netty.incubator.codec.quic.QuicPathEvent
import io.netty.incubator.codec.quic.QuicServerCodecBuilder
import io.netty.incubator.codec.quic.QuicSslContextBuilder
import io.netty.incubator.codec.quic.QuicStreamChannel
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.network.ClientConnection
import net.minecraft.network.NetworkSide
import net.minecraft.network.RateLimitedConnection
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerHandshakeNetworkHandler
import org.apache.logging.log4j.LogManager
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.security.SecureRandom
import java.util.*
import java.util.concurrent.TimeUnit

const val APPLICATION_PROTOCOL = "minecraft"

private val logger = LogManager.getLogger()

fun startQuicListener(
	server: MinecraftServer,
	address: InetAddress?,
	properties: QuicServerProperties,
	channels: MutableList<ChannelFuture>,
	connections: MutableList<ClientConnection>
) {
	val useEpoll = Epoll.isAvailable() && server.isUsingNativeTransport

	val config = FabricLoader.getInstance().configDir.resolve("quic")
	val keyFile = config.resolve("key.pem").toFile()
	val certificateFile = config.resolve("certificate.pem").toFile()
	if (!keyFile.exists() || !certificateFile.exists()) {
		logger.fatal("TLS key or certificate not found; please make sure that it is in the right place and that it is accessible")
		return
	}

	val context = QuicSslContextBuilder.forServer(keyFile, null, certificateFile)
		.applicationProtocols(APPLICATION_PROTOCOL)
		.build()

	val parentChannelAddresses = WeakHashMap<Channel, SocketAddress>()

	val codec = QuicServerCodecBuilder()
		.sslContext(context)
		.maxIdleTimeout(30, TimeUnit.SECONDS)
		.initialMaxData(10000000)
		.initialMaxStreamDataBidirectionalLocal(1000000)
		.initialMaxStreamDataBidirectionalRemote(1000000)
		.initialMaxStreamsBidirectional(100)
		.initialMaxStreamsUnidirectional(100)
		.tokenHandler(Blake3TokenHandler(ByteArray(32).apply(SecureRandom()::nextBytes)))
		.connectionIdAddressGenerator(Blake3ConnectionIdGenerator(ByteArray(32).apply(SecureRandom()::nextBytes)))
		.handler(object : ChannelInboundHandlerAdapter() {
			override fun userEventTriggered(context: ChannelHandlerContext, event: Any) {
				if (event is QuicPathEvent.New || event is QuicPathEvent.PeerMigrated) {
					val newAddress = event.remote()
					parentChannelAddresses[context.channel()] = newAddress

					for (connection in connections) {
						val oldAddress = connection.address
						val accessor = connection as ClientConnectionAccessor

						if (accessor.getChannel() is QuicStreamChannel
							&& accessor.getChannel().parent() == context.channel()
							&& !newAddress.equals(oldAddress)
						) {
							accessor.setAddress(newAddress)
							logger.info("{} was migrated to {}", oldAddress, newAddress)
						}
					}
				}

				context.fireUserEventTriggered(event)
			}

			override fun channelInactive(context: ChannelHandlerContext) {
				parentChannelAddresses -= context.channel()

				context.fireChannelInactive()
			}

			override fun isSharable() = true
		})
		.streamHandler(object : ChannelInitializer<QuicStreamChannel>() {
			override fun initChannel(channel: QuicStreamChannel) {
				val channelPipeline = channel.pipeline()
				ClientConnection.addHandlers(channelPipeline, NetworkSide.SERVERBOUND)

				val rateLimit = server.rateLimit
				val clientConnection = if (rateLimit > 0) RateLimitedConnection(rateLimit) else ClientConnection(NetworkSide.SERVERBOUND)
				(clientConnection as ClientConnectionAccessor).setEncrypted(true)
				connections += clientConnection

				channelPipeline.addLast("packet_handler", clientConnection)
				clientConnection.packetListener = ServerHandshakeNetworkHandler(server, clientConnection)

				parentChannelAddresses[channel.parent()]?.also { (clientConnection as ClientConnectionAccessor).setAddress(it) }
			}
		})
		.build()

	channels += Bootstrap()
		.group(if (useEpoll) ClientConnection.EPOLL_CLIENT_IO_GROUP.get() else ClientConnection.CLIENT_IO_GROUP.get())
		.channel(if (useEpoll) EpollDatagramChannel::class.java else NioDatagramChannel::class.java)
		.handler(codec)
		.bind(InetSocketAddress(address, properties.quicPort))
		.syncUninterruptibly()
}
