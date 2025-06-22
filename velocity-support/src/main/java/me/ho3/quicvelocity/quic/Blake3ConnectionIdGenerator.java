package me.ho3.quicvelocity.quic;

import io.netty.incubator.codec.quic.QuicConnectionIdGenerator;
import org.apache.commons.codec.digest.Blake3;
import java.nio.ByteBuffer;

public class Blake3ConnectionIdGenerator implements QuicConnectionIdGenerator {
	public static final int MAX_CONNECTION_ID_LENGTH = 20;

	private final Blake3 hash;

	public Blake3ConnectionIdGenerator(byte[] key) {
		this.hash = Blake3.initKeyedHash(key);
	}

	@Override
	public ByteBuffer newId(int length) {
		throw new UnsupportedOperationException("must always have an input");
	}

	@Override
	public ByteBuffer newId(ByteBuffer input, int length) {
		if (length < 1 || length > MAX_CONNECTION_ID_LENGTH) {
			throw new IllegalArgumentException("length: " + length + " (expected between 1 and " + MAX_CONNECTION_ID_LENGTH + ")");
		}

		hash.reset();
		if (input.hasArray()) {
			hash.update(input.array(), input.arrayOffset() + input.position(), input.remaining());
		} else {
			byte[] buffer = new byte[input.remaining()];
			input.get(buffer);
			hash.update(buffer);
		}

		return ByteBuffer.wrap(hash.doFinalize(length));
	}

	@Override
	public int maxConnectionIdLength() {
		return MAX_CONNECTION_ID_LENGTH;
	}

	@Override
	public boolean isIdempotent() {
		return true;
	}
}
