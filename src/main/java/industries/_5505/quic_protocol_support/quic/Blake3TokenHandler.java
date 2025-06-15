package industries._5505.quic_protocol_support.quic;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.incubator.codec.quic.QuicTokenHandler;
import org.apache.commons.codec.digest.Blake3;

import java.net.InetSocketAddress;
import java.security.MessageDigest;

import static industries._5505.quic_protocol_support.quic.Blake3ConnectionIdGenerator.MAX_CONNECTION_ID_LENGTH;

public class Blake3TokenHandler implements QuicTokenHandler {
    private static final int HASH_LENGTH = 32;
    private static final int TIMESTAMP_WINDOW_SIZE = 5 * 60 * 1000;
    
    private final Blake3 hash;
    
    public Blake3TokenHandler(byte[] key) {
        this.hash = Blake3.initKeyedHash(key);
    }
    
    @Override
    public boolean writeToken(ByteBuf out, ByteBuf dcid, InetSocketAddress address) {
        out.writeBytes(hash(address, dcid, System.currentTimeMillis() / TIMESTAMP_WINDOW_SIZE));
        out.writeBytes(dcid, dcid.readerIndex(), dcid.readableBytes());
        return true;
    }
    
    @Override
    public int validateToken(ByteBuf token, InetSocketAddress address) {
        if (token.readableBytes() < HASH_LENGTH) {
            return -1;
        }
        
        byte[] hash = new byte[HASH_LENGTH];
        token.getBytes(token.readerIndex(), hash);
        ByteBuf dcid = token.slice(token.readerIndex() + HASH_LENGTH, token.readableBytes() - HASH_LENGTH);
        
        long windowId = System.currentTimeMillis() / TIMESTAMP_WINDOW_SIZE;
        byte[] expectedHashNow = hash(address, dcid, windowId);
        byte[] expectedHashPrevious = hash(address, dcid, windowId - 1);
        
        // constant-time comparison
        boolean equalNow = MessageDigest.isEqual(expectedHashNow, hash);
        boolean equalPrevious = MessageDigest.isEqual(expectedHashPrevious, hash);
        return (equalNow || equalPrevious) ? HASH_LENGTH : -1;
    }
    
    private byte[] hash(InetSocketAddress address, ByteBuf dcid, long windowId) {
        if (dcid.readableBytes() < 1 || dcid.readableBytes() > MAX_CONNECTION_ID_LENGTH) {
            throw new IllegalArgumentException(
                "connection ID may not be longer than " + MAX_CONNECTION_ID_LENGTH + " bytes"
            );
        }
        
        ByteBuf cleartext = Unpooled.buffer();
        try {
            cleartext.writeInt(address.getPort());
            cleartext.writeLong(windowId);
            cleartext.writeBytes(address.getAddress().getAddress());
            cleartext.writeBytes(dcid, dcid.readerIndex(), dcid.readableBytes());
            
            hash.reset();
            hash.update(cleartext.array(), cleartext.arrayOffset() + cleartext.readerIndex(), cleartext.readableBytes());
            return hash.doFinalize(HASH_LENGTH);
        } finally {
            cleartext.release();
        }
    }
    
    @Override
    public int maxTokenLength() {
        return HASH_LENGTH + MAX_CONNECTION_ID_LENGTH;
    }
}
