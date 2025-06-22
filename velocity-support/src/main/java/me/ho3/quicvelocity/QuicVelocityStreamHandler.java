package me.ho3.quicvelocity;

import com.velocitypowered.api.proxy.ProxyServer;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import org.slf4j.Logger;

/**
 * Handles QUIC stream connections for Velocity proxy server
 */
public class QuicVelocityStreamHandler extends ChannelInboundHandlerAdapter {
    
    private final ProxyServer server;
    private final Logger logger;
    
    public QuicVelocityStreamHandler(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
    }
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.info("QUIC stream channel active: {}", ctx.channel().remoteAddress());
        super.channelActive(ctx);
    }
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // Handle incoming QUIC data
        // This would need to be adapted to Velocity's protocol handling
        logger.debug("Received QUIC data: {}", msg);
        
        // For now, we'll just pass it through
        // In a real implementation, you would convert QUIC data to Velocity's format
        super.channelRead(ctx, msg);
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.info("QUIC stream channel inactive: {}", ctx.channel().remoteAddress());
        super.channelInactive(ctx);
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("Error in QUIC stream handler", cause);
        ctx.close();
    }
}
