package me.ho3.quicvelocity.config;

/**
 * Configuration class for QUIC Velocity Support
 */
public class QuicConfig {

    // Default configuration values
    public static final int DEFAULT_PORT_OFFSET = 1;
    public static final long DEFAULT_MAX_DATA = 2_097_152L; // 2 MiB
    public static final int DEFAULT_IDLE_TIMEOUT_SECONDS = 30;
    public static final int DEFAULT_MAX_BIDIRECTIONAL_STREAMS = 100;

    // Certificate paths
    public static final String QUIC_CONFIG_DIR = "quic";
    public static final String CERTIFICATE_FILE = "certificate.pem";
    public static final String KEY_FILE = "key.pem";

    // Application protocol
    public static final String APPLICATION_PROTOCOL = "minecraft";

    // Blake3 configuration
    public static final int BLAKE3_KEY_LENGTH = 32;
    public static final int TOKEN_HASH_LENGTH = 32;
    public static final int TOKEN_TIMESTAMP_WINDOW_MS = 5 * 60 * 1000; // 5 minutes

    private QuicConfig() {
        // Utility class
    }

    /**
     * Get the QUIC port based on the main server port
     * @param mainPort the main server port
     * @return the QUIC port
     */
    public static int getQuicPort(int mainPort) {
        return mainPort + DEFAULT_PORT_OFFSET;
    }

    /**
     * Validate if the provided port is valid for QUIC
     * @param port the port to validate
     * @return true if valid
     */
    public static boolean isValidPort(int port) {
        return port > 0 && port <= 65535;
    }
}
