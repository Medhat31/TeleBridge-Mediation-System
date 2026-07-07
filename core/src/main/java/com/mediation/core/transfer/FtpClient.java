package com.mediation.core.transfer;

import com.mediation.core.entities.Node;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Concrete implementation of TransferClient for communicating with FTP nodes.
 * Uses Apache Commons Net FTPClient to securely download raw CDRs and upload CSVs.
 */
public class FtpClient implements TransferClient {

    private static final Logger log = LoggerFactory.getLogger(FtpClient.class);

    /**
     * Connects to an upstream FTP server, downloads all files in the root directory,
     * and deletes the remote files upon successful retrieval.
     *
     * @param node           The upstream node configuration.
     * @param localTargetDir The local directory where files will be stored.
     */
    @Override
    public void downloadCdrs(Node node, String localTargetDir) {
        log.info("[TRANSFER] Initiating FTP connection to {}", node.getNodeName());

        FTPClient ftpClient = new FTPClient();

        try {
            ftpClient.connect(node.getIpAddress(), node.getPort());
            boolean success = ftpClient.login(node.getUsername(), node.getPassword());

            if (!success) {
                log.error("  -> ERROR: FTP login failed for {}", node.getNodeName());
                return;
            }

            log.info("  -> SUCCESS: FTP session established.");

            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

            File localDir = new File(localTargetDir);
            if (!localDir.exists()) {
                localDir.mkdirs();
            }

            FTPFile[] files = ftpClient.listFiles();

            if (files == null || files.length == 0) {
                log.info("  -> No new files found for {}", node.getNodeName());
            } else {
                for (FTPFile file : files) {
                    if (file.isFile() && !file.getName().startsWith(".")) {
                        String remoteFile = file.getName();
                        File localFile = new File(localTargetDir + File.separator + remoteFile);

                        log.info("  -> DOWNLOADING: {} ({} bytes)", remoteFile, file.getSize());

                        try (OutputStream outputStream = new FileOutputStream(localFile)) {
                            boolean retrieved = ftpClient.retrieveFile(remoteFile, outputStream);

                            if (retrieved) {
                                ftpClient.deleteFile(remoteFile);
                                log.info("  -> CLEARED: Removed {} from switch memory.", remoteFile);
                            }
                        }
                    }
                }
            }

        } catch (IOException e) {
            log.error("  -> CRITICAL: FTP connection failed for node: {}", node.getNodeName(), e);
        } finally {
            try {
                if (ftpClient.isConnected()) {
                    ftpClient.logout();
                    ftpClient.disconnect();
                }
            } catch (IOException ex) {
                log.warn("Error disconnecting FTP client.", ex);
            }
        }
    }

    /**
     * Connects to a downstream FTP server and uploads the processed CSV file.
     *
     * @param node         The downstream node configuration.
     * @param fileToUpload The local CSV file to be uploaded.
     */
    @Override
    public void uploadFile(Node node, File fileToUpload) {
        log.info("  -> [TRANSFER] Forwarding via FTP to {}", node.getNodeName());
        FTPClient ftpClient = new FTPClient();

        try (java.io.InputStream inputStream = new java.io.FileInputStream(fileToUpload)) {
            ftpClient.connect(node.getIpAddress(), node.getPort());
            ftpClient.login(node.getUsername(), node.getPassword());
            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

            boolean stored = ftpClient.storeFile(fileToUpload.getName(), inputStream);
            if (stored) {
                log.info("     SUCCESS: Uploaded {}", fileToUpload.getName());
            } else {
                log.error("     ERROR: FTP Server rejected the file.");
            }
        } catch (Exception e) {
            log.error("     ERROR: FTP Upload failed for {}", node.getNodeName(), e);
        } finally {
            try {
                if (ftpClient.isConnected()) {
                    ftpClient.logout();
                    ftpClient.disconnect();
                }
            } catch (Exception ex) {
                log.warn("Error disconnecting FTP client.", ex);
            }
        }
    }
}
