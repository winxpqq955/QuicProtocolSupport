package industries._5505.quic_protocol_support.quic

import io.netty.incubator.codec.quic.QuicConnectionIdGenerator
import org.apache.commons.codec.digest.Blake3
import java.nio.ByteBuffer

internal const val MAX_CONNECTION_ID_LENGTH = 20

class Blake3ConnectionIdGenerator(key: ByteArray) : QuicConnectionIdGenerator {
	private val hash = Blake3.initKeyedHash(key)

	override fun newId(length: Int): ByteBuffer {
		throw UnsupportedOperationException("must always have an input")
	}

	override fun newId(input: ByteBuffer, length: Int): ByteBuffer {
		require(length > 0) { "length: $length (expected: > 0)" }
		require(length in 0..maxConnectionIdLength()) { "length: $length (expected: 0-${maxConnectionIdLength()})" }

		hash.reset()
		if (input.hasArray()) {
			hash.update(input.array(), input.arrayOffset() + input.position(), input.remaining())
		} else {
			val buffer = ByteArray(input.remaining())
			input.get(buffer)
			hash.update(buffer)
		}
		return ByteBuffer.wrap(hash.doFinalize(length))
	}

	override fun maxConnectionIdLength() = MAX_CONNECTION_ID_LENGTH
	override fun isIdempotent() = true
}
