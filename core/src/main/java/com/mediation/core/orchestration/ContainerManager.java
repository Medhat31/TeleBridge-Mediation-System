package com.mediation.core.orchestration;

import com.mediation.core.config.AppConfig;
import com.mediation.core.entities.Node;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

/**
 * Manages the lifecycle of Docker containers that act as upstream or downstream network nodes.
 * Automatically spins up, tears down, and verifies the health of FTP/SFTP servers.
 */
public class ContainerManager {

    private static final Logger log = LoggerFactory.getLogger(ContainerManager.class);

    /**
     * Provisions the infrastructure for a specific node by creating local mount directories
     * and spinning up the appropriate Docker container. Skips if already running.
     *
     * @param node The node configuration from the database.
     */
    public void provisionNode(Node node) {
        log.info("Provisioning {} infrastructure for Node: {}", node.getNodeType(), node.getNodeName());
        
        createNodeDirectory(node);
        
        String containerName = getContainerName(node);
        
        if (isContainerRunning(containerName)) {
            log.info("  -> Container '{}' is already running. Skipping.", containerName);
            return;
        }

        removeStoppedContainer(containerName);
        
        switch (node.getProtocol().toUpperCase()) {
            case "SFTP":
                startSftpContainer(node, containerName);
                break;
            case "FTP":
                startFtpContainer(node, containerName);
                break;
            default:
                log.warn("  -> Skipping Docker provisioning: Protocol {} not recognized.", node.getProtocol());
        }
    }

    /**
     * Tears down the infrastructure for an inactive node by force-removing its Docker container.
     *
     * @param node The node configuration to tear down.
     */
    public void teardownNode(Node node) {
        String containerName = getContainerName(node);
        removeStoppedContainer(containerName);
    }

    /**
     * Stops and forcefully removes a Docker container by name.
     *
     * @param containerName The exact name of the Docker container.
     */
    public void stopContainer(String containerName) {
        log.info("  -> Stopping container: {}", containerName);
        executeBashCommand("docker rm -f " + containerName);
    }

    /**
     * Generates a standardized, immutable container name based on the node's database ID.
     *
     * @param node The node configuration.
     * @return The deterministic container name.
     */
    public String getContainerName(Node node) {
        return "telebridge_" + node.getNodeType().toLowerCase() + "_" + node.getNodeId();
    }

    /**
     * Creates the local host directory that will be mounted as a volume inside the Docker container.
     * Sets read/write/execute permissions to allow the container user access.
     *
     * @param node The node configuration.
     */
    private void createNodeDirectory(Node node) {
        String path = AppConfig.getNodeVolumesDir() + File.separator
                + node.getNodeType().toUpperCase() + File.separator + node.getNodeId();
        File baseDir = new File(path);
        
        if (!baseDir.exists()) {
            if (baseDir.mkdirs()) {
                log.info("  -> Created volume directory: {}", baseDir.getAbsolutePath());
            } else {
                log.error("  -> FAILED to create directory: {}", baseDir.getAbsolutePath());
            }
        } else {
            log.debug("  -> Volume directory already exists: {}", baseDir.getAbsolutePath());
        }
        
        baseDir.setReadable(true, false);
        baseDir.setWritable(true, false);
        baseDir.setExecutable(true, false);
    }

    /**
     * Verifies if a container with the specified name is actively running.
     *
     * @param containerName The exact name of the container.
     * @return true if running, false otherwise.
     */
    private boolean isContainerRunning(String containerName) {
        try {
            ProcessBuilder pb = new ProcessBuilder("docker", "ps", "-q", "--filter", "name=^" + containerName + "$");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            String output;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                output = reader.readLine();
            }
            process.waitFor();
            
            return output != null && !output.trim().isEmpty();
        } catch (Exception e) {
            log.warn("Could not check container status for '{}'. Assuming not running.", containerName);
            return false;
        }
    }

    /**
     * Finds and forcefully removes a container (running or stopped) to prevent naming conflicts.
     *
     * @param containerName The exact name of the container.
     */
    private void removeStoppedContainer(String containerName) {
        try {
            ProcessBuilder pb = new ProcessBuilder("docker", "ps", "-aq", "--filter", "name=^" + containerName + "$");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            String output;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                output = reader.readLine();
            }
            process.waitFor();
            
            if (output != null && !output.trim().isEmpty()) {
                log.info("  -> Tearing down container: {}", containerName);
                executeBashCommand("docker rm -f " + containerName);
            }
        } catch (Exception e) {
            log.warn("Could not remove container '{}'.", containerName);
        }
    }

    /**
     * Boots an SFTP server container using the 'atmoz/sftp' image.
     *
     * @param node          The node configuration.
     * @param containerName The generated container name.
     */
    private void startSftpContainer(Node node, String containerName) {
        String hostVolumePath = getHostVolumePath(node);
        String dockerNetwork = AppConfig.getDockerNetwork();
        
        String dockerCommand = String.format(
                "docker run -d --name %s --network %s -v %s:/home/%s/share atmoz/sftp %s:%s:1001",
                containerName,
                dockerNetwork,
                hostVolumePath,
                node.getUsername(),
                node.getUsername(),
                node.getPassword()
        );

        log.info("  -> Executing: {}", dockerCommand);
        executeBashCommand(dockerCommand);
    }
    
    /**
     * Boots an FTP server container using the 'delfer/alpine-ftp-server' image.
     *
     * @param node          The node configuration.
     * @param containerName The generated container name.
     */
    private void startFtpContainer(Node node, String containerName) {
        String hostVolumePath = getHostVolumePath(node);
        String dockerNetwork = AppConfig.getDockerNetwork();
        
        String dockerCommand = String.format(
                "docker run -d --name %s --network %s -e USERS=\"%s|%s\" -v %s:/ftp/%s delfer/alpine-ftp-server",
                containerName,
                dockerNetwork,
                node.getUsername(),
                node.getPassword(),
                hostVolumePath,
                node.getUsername()
        );

        log.info("  -> Executing: {}", dockerCommand);
        executeBashCommand(dockerCommand);
    }

    /**
     * Resolves the host-side absolute path for Docker bind mounts based on the node ID.
     *
     * @param node The node configuration.
     * @return The absolute path string.
     */
    private String getHostVolumePath(Node node) {
        String hostRoot = AppConfig.getHostProjectRoot();
        return hostRoot + "/node_volumes/" + node.getNodeType().toUpperCase() + "/" + node.getNodeId();
    }

    /**
     * Executes a raw bash command and pipes the output to the application logger.
     *
     * @param command The bash command string to execute.
     */
    private void executeBashCommand(String command) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", command);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.debug("  Docker: {}", line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                log.info("  -> Container operation completed successfully.");
            } else {
                log.error("  -> Container operation failed. Exit code: {}", exitCode);
            }

        } catch (Exception e) {
            log.error("  -> Failed to execute bash command.", e);
        }
    }
}