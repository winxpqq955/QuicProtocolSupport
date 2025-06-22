package me.ho3.quicvelocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import me.ho3.quicvelocity.config.QuicConfig;
import org.slf4j.Logger;

import java.net.InetSocketAddress;
import java.nio.file.Path;

@Plugin(
    id = "quic-velocity-support",
    name = "QUIC Velocity Support",
    version = "1.0.0",
    description = "QUIC protocol support for Velocity proxy server",
    authors = {"QuicProtocolSupport"}
)
public class QuicSupportVelocity {

    @Inject 
    private Logger logger;
    
    @Inject 
    private ProxyServer server;
    
    @Inject
    @DataDirectory
    private Path dataDirectory;
    
    private QuicVelocityListener quicListener;

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info("Initializing QUIC support for Velocity...");
        
        try {
            // Initialize QUIC listener
            quicListener = new QuicVelocityListener(server, logger);
            
            // Get main server address and calculate QUIC address
            InetSocketAddress mainAddress = server.getBoundAddress();
            int quicPort = QuicConfig.getQuicPort(mainAddress.getPort());
            
            if (!QuicConfig.isValidPort(quicPort)) {
                logger.error("Invalid QUIC port calculated: {}. Main port: {}", quicPort, mainAddress.getPort());
                return;
            }
            
            InetSocketAddress quicAddress = new InetSocketAddress(
                mainAddress.getAddress(), 
                quicPort
            );
            
            logger.info("Starting QUIC listener on {}", quicAddress);
            logger.info("Main Velocity server: {}", mainAddress);
            
            quicListener.startQuicListener(quicAddress, dataDirectory);
            
            logger.info("QUIC support initialized successfully!");
            logger.info("=".repeat(60));
            logger.info("QUIC Configuration:");
            logger.info("  Listen Address: {}", quicAddress);
            logger.info("  Config Directory: {}/{}", dataDirectory, QuicConfig.QUIC_CONFIG_DIR);
            logger.info("  Required Files:");
            logger.info("    - {}", QuicConfig.CERTIFICATE_FILE);
            logger.info("    - {}", QuicConfig.KEY_FILE);
            logger.info("  Client Connection: quic://{}:{}", 
                mainAddress.getHostString(), quicPort);
            logger.info("=".repeat(60));
            
        } catch (Exception e) {
            logger.error("Failed to initialize QUIC support", e);
        }
    }
    
    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        logger.info("Shutting down QUIC support...");
        // Cleanup will be handled by shutdown hooks in QuicVelocityListener
    }
}
