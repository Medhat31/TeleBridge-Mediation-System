package com.mediation.core.transfer;

import com.mediation.core.entities.Node;
import java.io.File;

/**
 * Defines the contract for all network transfer protocols (e.g., FTP, SFTP).
 * Ensures a consistent interface for downloading raw CDRs and uploading processed CSVs.
 */
public interface TransferClient {
    
    /**
     * Connects to a remote upstream node and downloads available CDR files to the local machine.
     *
     * @param node           The upstream node configuration (IP, port, credentials).
     * @param localTargetDir The local directory where downloaded files should be staged.
     */
    void downloadCdrs(Node node, String localTargetDir);
     
    /**
     * Connects to a remote downstream node and uploads a processed CSV file.
     *
     * @param node         The downstream node configuration (IP, port, credentials).
     * @param fileToUpload The local CSV file ready for transmission.
     */
    void uploadFile(Node node, File fileToUpload);
}