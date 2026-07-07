package com.mediation.core;

import com.mediation.core.config.AppConfig;
import com.mediation.core.entities.Cdr;
import com.mediation.core.entities.MediationRule;
import com.mediation.core.entities.Node;
import com.mediation.core.orchestration.ContainerManager;
import com.mediation.core.orchestration.FileArchiver;
import com.mediation.core.parsing.TlvParser;
import com.mediation.core.repository.DBConnection;
import com.mediation.core.repository.NodeRepository;
import com.mediation.core.repository.RuleRepository;
import com.mediation.core.repository.ActivityLogger;
import com.mediation.core.transfer.CdrExporter;
import com.mediation.core.transfer.TransferClient;
import com.mediation.core.transfer.TransferFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The primary entry point and orchestration engine for the TeleBridge Mediation System.
 * This class runs a continuous background pipeline to fetch, parse, mediate, and distribute telecom data.
 */
public class Core {

    private static final Logger log = LoggerFactory.getLogger(Core.class);
    private static volatile boolean running = true;

    /**
     * The main execution loop of the Mediation Engine.
     * Initializes the environment, handles graceful shutdowns, and continuously triggers the pipeline cycle.
     */
    public static void main(String[] args) {
        log.info("===================================================");
        log.info("=== TeleBridge Gateway Engine Pipeline Active ===");
        log.info("===================================================");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutdown signal received. Cleaning up...");
            running = false;
            DBConnection.shutdown();
            log.info("Shutdown complete.");
        }));

        int pollInterval = AppConfig.getPollIntervalSeconds();
        log.info("Engine polling interval: {} seconds", pollInterval);

        String inputDir = AppConfig.getInputDir();
        String outputDir = AppConfig.getOutputDir();
        String archiveDir = AppConfig.getArchiveDir();
        new File(inputDir).mkdirs();
        new File(outputDir).mkdirs();
        new File(archiveDir).mkdirs();

        log.info("Workspace root: {}", AppConfig.getProjectRoot());
        log.info("Input:   {}", inputDir);
        log.info("Output:  {}", outputDir);
        log.info("Archive: {}", archiveDir);

        while (running) {
            try {
                runPipelineCycle(inputDir, outputDir, archiveDir);
            } catch (Exception e) {
                log.error("[CRITICAL FAILURE] Pipeline cycle failed!", e);
            }

            if (running) {
                log.info("[SLEEPING] Next cycle in {} seconds...\n", pollInterval);
                try {
                    Thread.sleep(pollInterval * 1000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.info("Engine interrupted. Exiting.");
                    break;
                }
            }
        }
    }

    /**
     * Executes a single end-to-end processing cycle.
     * The pipeline consists of: fetching network configurations, provisioning infrastructure,
     * downloading raw files, parsing data, applying mediation rules, exporting cleaned data, and archiving.
     *
     * @param inputDir   Directory where raw files are downloaded.
     * @param outputDir  Directory where processed files are staged.
     * @param archiveDir Directory where original raw files are permanently stored.
     */
    private static void runPipelineCycle(String inputDir, String outputDir, String archiveDir) {
        log.info("[PHASE 1] Fetching Network Topology...");
        ActivityLogger.logInfo("Starting Engine Cycle (Phase 1: Fetching Topology)");
        NodeRepository nodeRepo = new NodeRepository();
        RuleRepository ruleRepo = new RuleRepository();
        List<Node> allNodes = nodeRepo.getAllNodes();
        List<MediationRule> activeRules = ruleRepo.getActiveRules();

        Map<Integer, Node> networkRegistry = new HashMap<>();
        List<Node> activeUpstreamNodes = new ArrayList<>();
        List<Node> activeDownstreamNodes = new ArrayList<>();
        
        ContainerManager containerManager = new ContainerManager();

        for (Node node : allNodes) {
            if (node.isActive()) {
                networkRegistry.put(node.getNodeId(), node);
                if ("UPSTREAM".equalsIgnoreCase(node.getNodeType())) {
                    activeUpstreamNodes.add(node);
                } else if ("DOWNSTREAM".equalsIgnoreCase(node.getNodeType())) {
                    activeDownstreamNodes.add(node);
                }
            } else {
                containerManager.teardownNode(node);
            }
        }

        for (Node node : allNodes) {
            String expectedIp = containerManager.getContainerName(node);
            if (!expectedIp.equals(node.getIpAddress())) {
                log.info("  -> Auto-syncing ip_address for '{}': {} -> {}", node.getNodeName(), node.getIpAddress(), expectedIp);
                nodeRepo.updateIpAddress(node.getNodeId(), expectedIp);
                node.setIpAddress(expectedIp);
            }
        }

        if (activeUpstreamNodes.isEmpty()) {
            log.warn("  -> No active upstream nodes found. Skipping cycle.");
            return;
        }

        java.util.Iterator<Node> it = activeUpstreamNodes.iterator();
        while (it.hasNext()) {
            Node node = it.next();
            boolean hasValidRule = false;
            for (MediationRule rule : activeRules) {
                if (rule.getSourceNodeId() == node.getNodeId()) {
                    boolean destNodeActive = false;
                    for (Node dest : activeDownstreamNodes) {
                        if (dest.getNodeId() == rule.getDestinationNodeId()) {
                            destNodeActive = true;
                            break;
                        }
                    }
                    
                    if (destNodeActive) {
                        hasValidRule = true;
                        break;
                    }
                }
            }
            if (!hasValidRule) {
                log.warn("  -> Skipping download for Node [{}]: No active mediation rules with an active downstream node configured.", node.getNodeName());
                ActivityLogger.logWarning("Skipping download for Node " + node.getNodeName() + " - No valid downstream route.");
                it.remove();
            }
        }

        if (activeUpstreamNodes.isEmpty()) {
            log.warn("  -> No valid upstream nodes with active rules found. Skipping cycle.");
            return;
        }

        log.info("[PHASE 2 & 3] Provisioning and Polling...");
        ActivityLogger.logInfo("Polling for new CDR files from upstream nodes");

        for (Node node : activeUpstreamNodes) {
            containerManager.provisionNode(node); 
        }
        for (Node node : activeDownstreamNodes) {
            containerManager.provisionNode(node);
        }

        waitForContainersReady(activeUpstreamNodes);

        for (Node node : activeUpstreamNodes) {
            try {
                TransferClient client = TransferFactory.getClient(node.getProtocol());
                client.downloadCdrs(node, inputDir);
            } catch (Exception e) {
                log.error("Failed to download CDRs from node: {}", node.getNodeName(), e);
                ActivityLogger.logError("Failed to download CDRs from node: " + node.getNodeName());
            }
        }

        log.info("[PHASE 4, 5, 6] Mediating and Forwarding Traffic...");
        TlvParser parser = new TlvParser();
        MediationProcessor mediator = new MediationProcessor();
        CdrExporter exporter = new CdrExporter();
        com.mediation.core.repository.MetricsRepository metricsRepo = new com.mediation.core.repository.MetricsRepository();
        
        File inputFolder = new File(inputDir);
        File[] files = inputFolder.listFiles();
        
        int totalProcessedCdrsThisCycle = 0;
        java.util.Set<File> failedUploads = new java.util.HashSet<>();

        if (files != null && files.length > 0) {
            for (Node sourceNode : activeUpstreamNodes) {
                for (File file : files) {
                    if (file.isFile() && !file.getName().startsWith(".") && file.getName().startsWith(sourceNode.getNodeName())) {
                        
                        log.info("  -> Decoding: {}", file.getName());
                        ActivityLogger.logInfo("Decoding file: " + file.getName());
                        List<Cdr> rawCdrs = parser.parseFile(file);
                        
                        Map<Integer, List<Cdr>> routedCdrs = mediator.process(rawCdrs, sourceNode, activeRules);
                        
                        boolean fileHasErrors = false;
                        for (Map.Entry<Integer, List<Cdr>> route : routedCdrs.entrySet()) {
                            int destNodeId = route.getKey();
                            List<Cdr> cleanData = route.getValue();
                            
                            if (!cleanData.isEmpty() && networkRegistry.containsKey(destNodeId)) {
                                Node destNode = networkRegistry.get(destNodeId);
                                
                                File payload = exporter.exportToCsv(cleanData, outputDir, file.getName(), destNode.getNodeName());
                                
                                try {
                                    TransferClient destClient = TransferFactory.getClient(destNode.getProtocol());
                                    destClient.uploadFile(destNode, payload);
                                    
                                    totalProcessedCdrsThisCycle += cleanData.size();
                                    ActivityLogger.logSuccess("Forwarded " + cleanData.size() + " cleaned CDRs to " + destNode.getNodeName());
                                } catch (Exception e) {
                                    log.error("Failed to upload to destination node: {}", destNode.getNodeName(), e);
                                    ActivityLogger.logError("Failed to upload to destination node: " + destNode.getNodeName());
                                    fileHasErrors = true;
                                }
                            }
                        }
                        if (fileHasErrors) {
                            failedUploads.add(file);
                        }
                    }
                }
            }
            
            if (totalProcessedCdrsThisCycle > 0) {
                metricsRepo.incrementCdrsToday(totalProcessedCdrsThisCycle);
            }
            
        } else {
            log.info("  -> No files found in input directory. Nothing to process.");
            ActivityLogger.logInfo("No files found. Engine cycle idle.");
        }

        log.info("[PHASE 7] Archiving Original Files...");
        FileArchiver archiver = new FileArchiver();
        File[] archiveFiles = inputFolder.listFiles();
        if (archiveFiles != null) {
            for (File file : archiveFiles) {
                if (file.isFile() && !file.getName().startsWith(".")) {
                    if (failedUploads.contains(file)) {
                        log.warn("  -> Skipping archive for file {} due to upstream/downstream transfer failure. Will retry on next cycle.", file.getName());
                    } else {
                        archiver.archiveFile(file, archiveDir);
                    }
                }
            }
        }

        log.info("=== Engine Pipeline Cycle Completed Successfully ===");
        if (totalProcessedCdrsThisCycle > 0) {
            ActivityLogger.logSuccess("Engine cycle completed. Processed " + totalProcessedCdrsThisCycle + " records.");
        }
    }

    /**
     * Checks if provisioned Docker containers are actively running.
     * Utilizes an exponential backoff strategy to wait for containers to boot up.
     *
     * @param nodes List of upstream and downstream nodes to verify.
     */
    private static void waitForContainersReady(List<Node> nodes) {
        int maxRetries = 5;
        long waitMs = 1000;
        ContainerManager cm = new ContainerManager();

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            boolean allReady = true;
            
            for (Node node : nodes) {
                try {
                    String containerName = cm.getContainerName(node);
                    ProcessBuilder pb = new ProcessBuilder("docker", "ps", "-q", "--filter",
                            "name=^" + containerName + "$",
                            "--filter", "status=running");
                    pb.redirectErrorStream(true);
                    Process process = pb.start();
                    String output = new String(process.getInputStream().readAllBytes()).trim();
                    process.waitFor();
                    
                    if (output.isEmpty()) {
                        allReady = false;
                        break;
                    }
                } catch (Exception e) {
                    allReady = false;
                    break;
                }
            }

            if (allReady) {
                log.info("  -> All containers are ready.");
                return;
            }

            log.info("  -> Waiting for containers... (attempt {}/{})", attempt, maxRetries);
            try {
                Thread.sleep(waitMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            waitMs = Math.min(waitMs * 2, 5000);
        }

        log.warn("  -> Some containers may not be ready. Proceeding anyway.");
    }
}