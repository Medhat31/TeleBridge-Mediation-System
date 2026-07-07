package com.mediation.core.transfer;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.mediation.core.config.AppConfig;
import com.mediation.core.entities.Node;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Vector;

/**
 * Concrete implementation of TransferClient for communicating with SFTP nodes.
 * Uses JSch to securely connect, download raw CDRs, and upload CSVs over SSH.
 */
public class SftpClient implements TransferClient {

    private static final Logger log = LoggerFactory.getLogger(SftpClient.class);

    /**
     * Connects to an upstream SFTP server over SSH, navigates to the 'share' directory,
     * downloads all available files, and securely deletes the remote files upon success.
     *
     * @param node           The upstream node configuration.
     * @param localTargetDir The local directory where files will be stored.
     */
    @Override
    public void downloadCdrs(Node node, String localTargetDir) {
        log.info("[TRANSFER] Initiating SFTP connection to {} ({}:{})",
                node.getNodeName(), node.getIpAddress(), node.getPort());

        JSch jsch = new JSch();
        Session session = null;
        ChannelSftp sftpChannel = null;

        try {
            session = jsch.getSession(node.getUsername(), node.getIpAddress(), node.getPort());
            session.setPassword(node.getPassword());

            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", AppConfig.getSshStrictHostKeyChecking());
            session.setConfig(config);

            session.connect(5000);
            sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect();
            log.info("  -> SUCCESS: Secure channel established.");

            File localDir = new File(localTargetDir);
            if (!localDir.exists()) {
                localDir.mkdirs();
            }

            sftpChannel.cd("share");

            Vector<ChannelSftp.LsEntry> fileList = sftpChannel.ls("*");

            if (fileList.isEmpty()) {
                log.info("  -> No new files found for {}", node.getNodeName());
            } else {
                for (ChannelSftp.LsEntry file : fileList) {
                    if (!file.getAttrs().isDir() && !file.getFilename().startsWith(".")) {
                        String remoteFile = file.getFilename();
                        String localFile = localTargetDir + File.separator + remoteFile;

                        log.info("  -> DOWNLOADING: {} ({} bytes)", remoteFile, file.getAttrs().getSize());

                        sftpChannel.get(remoteFile, localFile);
                        sftpChannel.rm(remoteFile);

                        log.info("  -> CLEARED: Removed {} from node's storage.", remoteFile);
                    }
                }
            }

        } catch (SftpException e) {
            log.error("  -> ERROR: SFTP protocol error for node: {}", node.getNodeName(), e);
        } catch (Exception e) {
            log.error("  -> CRITICAL: Connection failed for node: {}", node.getNodeName(), e);
        } finally {
            if (sftpChannel != null && sftpChannel.isConnected()) {
                sftpChannel.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    /**
     * Connects to a downstream SFTP server, navigates to the 'share' directory,
     * and securely uploads the processed CSV file over SSH.
     *
     * @param node         The downstream node configuration.
     * @param fileToUpload The local CSV file to be uploaded.
     */
    @Override
    public void uploadFile(Node node, File fileToUpload) {
        log.info("  -> [TRANSFER] Forwarding via SFTP to {}", node.getNodeName());
        JSch jsch = new JSch();
        Session session = null;
        ChannelSftp sftpChannel = null;

        try {
            session = jsch.getSession(node.getUsername(), node.getIpAddress(), node.getPort());
            session.setPassword(node.getPassword());
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", AppConfig.getSshStrictHostKeyChecking());
            session.setConfig(config);

            session.connect(5000);
            sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect();

            sftpChannel.cd("share");

            sftpChannel.put(fileToUpload.getAbsolutePath(), fileToUpload.getName());
            log.info("     SUCCESS: Uploaded {}", fileToUpload.getName());

        } catch (Exception e) {
            log.error("     ERROR: SFTP Upload failed for {}", node.getNodeName(), e);
        } finally {
            if (sftpChannel != null && sftpChannel.isConnected()) {
                sftpChannel.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }
}
