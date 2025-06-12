package industries._5505.quic_protocol_support.quic

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.incubator.codec.quic.QuicTokenHandler
import org.apache.commons.codec.digest.Blake3
import java.net.InetSocketAddress
import java.security.MessageDigest

private const val HASH_LENGTH = 32
private const val TIMESTAMP_WINDOW_SIZE = 5 * 60 * 1000

class Blake3TokenHandler(key: ByteArray) : QuicTokenHandler {
	private val hash = Blake3.initKeyedHash(key)

	override fun writeToken(out: ByteBuf, dcid: ByteBuf, address: InetSocketAddress): Boolean {
		out.writeBytes(hash(address, dcid, System.currentTimeMillis() / TIMESTAMP_WINDOW_SIZE))
		out.writeBytes(dcid, dcid.readerIndex(), dcid.readableBytes())
		return true
	}

	override fun validateToken(token: ByteBuf, address: InetSocketAddress): Int {
		if (token.readableBytes() < HASH_LENGTH) return -1

		val hash = ByteArray(HASH_LENGTH)
		token.getBytes(token.readerIndex(), hash)
		val dcid = token.slice(token.readerIndex() + HASH_LENGTH, token.readableBytes() - HASH_LENGTH)

		val windowId = System.currentTimeMillis() / TIMESTAMP_WINDOW_SIZE
		val expectedHashNow = hash(address, dcid, windowId)
		val expectedHashPrevious = hash(address, dcid, windowId - 1)

		// constant-time comparison
		val equalNow = MessageDigest.isEqual(expectedHashNow, hash)
		val equalPrevious = MessageDigest.isEqual(expectedHashPrevious, hash)
		return if (equalNow or equalPrevious) HASH_LENGTH else -1
	}

	private fun hash(address: InetSocketAddress, dcid: ByteBuf, windowId: Long): ByteArray {
		require(dcid.readableBytes() in 1..MAX_CONNECTION_ID_LENGTH) {
			"connection ID may not be longer than $MAX_CONNECTION_ID_LENGTH bytes"
		}

		val cleartext = Unpooled.buffer().apply {
			writeInt(address.port)
			writeLong(windowId)
			writeBytes(address.address.address)
			writeBytes(dcid, dcid.readerIndex(), dcid.readableBytes())
		}

		hash.reset()
		hash.update(cleartext.array(), cleartext.arrayOffset() + cleartext.readerIndex(), cleartext.readableBytes())
		cleartext.release()
		return hash.doFinalize(HASH_LENGTH)
	}

	override fun maxTokenLength() = HASH_LENGTH + MAX_CONNECTION_ID_LENGTH
}
