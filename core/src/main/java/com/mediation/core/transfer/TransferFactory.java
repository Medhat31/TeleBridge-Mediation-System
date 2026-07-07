package com.mediation.core.transfer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory class responsible for instantiating the correct network transfer client
 * based on the protocol specified by the Node configuration.
 */
public class TransferFactory {

    private static final Logger log = LoggerFactory.getLogger(TransferFactory.class);

    /**
     * Generates a concrete implementation of TransferClient (FTP or SFTP) based on the requested protocol.
     *
     * @param protocol The protocol string (e.g., "FTP", "SFTP").
     * @return An instantiated TransferClient ready for network communication.
     * @throws IllegalArgumentException if the protocol is null, empty, or unsupported.
     */
    public static TransferClient getClient(String protocol) {
        if (protocol == null || protocol.isBlank()) {
            throw new IllegalArgumentException("Protocol cannot be null or empty.");
        }

        switch (protocol.toUpperCase()) {
            case "FTP":
                log.debug("Creating FTP client.");
                return new FtpClient();

            case "SFTP":
                log.debug("Creating SFTP client.");
                return new SftpClient();

            default:
                throw new IllegalArgumentException("Unsupported transfer protocol: " + protocol);
        }
    }
}