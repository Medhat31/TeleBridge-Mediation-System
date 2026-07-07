package com.mediation.website.service;

import com.mediation.website.entity.Node;
import com.mediation.website.repository.NodeRepository;

import java.util.List;

/**
 * Service layer for managing network Nodes.
 * Contains the business logic for validating node configurations, generating network addresses,
 * and orchestrating the creation and deletion of nodes along with their associated Docker containers.
 */
public class NodeService {

    private final NodeRepository nodeRepository;

    public NodeService() {
        this.nodeRepository = new NodeRepository();
    }

    /**
     * Retrieves all configured nodes from the repository.
     *
     * @return A list of all Node objects.
     */
    public List<Node> getAllNodes() {
        return nodeRepository.findAll();
    }

    /**
     * Retrieves a specific node by its ID.
     *
     * @param id The ID of the node.
     * @return The Node object if found, or null.
     */
    public Node getNodeById(int id) {
        return nodeRepository.findById(id);
    }

    /**
     * Validates and creates a new node in the system.
     * Calculates the proper internal Docker network IP address based on the node's type and generated ID.
     *
     * @param node The incoming Node configuration.
     * @return The fully formed Node object, including its new ID and calculated IP address.
     * @throws IllegalArgumentException If the node type is invalid.
     */
    public Node createNode(Node node) {
        if (!"UPSTREAM".equals(node.getNodeType()) && !"DOWNSTREAM".equals(node.getNodeType())) {
            throw new IllegalArgumentException("Node type must be UPSTREAM or DOWNSTREAM");
        }

        if (node.getPort() <= 0) {
            if ("FTP".equalsIgnoreCase(node.getProtocol())) {
                node.setPort(21);
            } else if ("SFTP".equalsIgnoreCase(node.getProtocol())) {
                node.setPort(22);
            }
        }

        node.setIpAddress("pending...");
        
        Node insertedNode = nodeRepository.insert(node);
        if (insertedNode == null) {
            return null; 
        }

        String expectedIpAddress = "telebridge_" + insertedNode.getNodeType().toLowerCase() + "_" + insertedNode.getNodeId();
        
        boolean updated = nodeRepository.updateIpAddress(insertedNode.getNodeId(), expectedIpAddress, insertedNode.getPort());
        if (updated) {
            insertedNode.setIpAddress(expectedIpAddress);
        }

        return insertedNode;
    }

    /**
     * Updates an existing node's configuration.
     * Prevents modification of structural properties like Node Type, Protocol, and IP Address.
     *
     * @param id          The ID of the node to update.
     * @param updatedNode The incoming node data containing updates.
     * @return True if the update was successful, false otherwise.
     */
    public boolean updateNode(int id, Node updatedNode) {
        Node existingNode = nodeRepository.findById(id);
        if (existingNode == null) {
            return false;
        }

        updatedNode.setIpAddress(existingNode.getIpAddress());
        updatedNode.setPort(existingNode.getPort());

        updatedNode.setNodeType(existingNode.getNodeType());
        updatedNode.setProtocol(existingNode.getProtocol());

        if (updatedNode.getPassword() == null || updatedNode.getPassword().trim().isEmpty()) {
            updatedNode.setPassword(existingNode.getPassword());
        }

        return nodeRepository.update(id, updatedNode);
    }

    /**
     * Toggles a node's active status.
     * If deactivated, it safely deactivates any Mediation Rules associated with this node to prevent routing errors.
     *
     * @param id       The ID of the node.
     * @param isActive The new active status.
     * @return True if successful.
     */
    public boolean toggleNodeStatus(int id, boolean isActive) {
        boolean updated = nodeRepository.updateStatus(id, isActive);
        if (updated && !isActive) {
            com.mediation.website.repository.RuleRepository ruleRepo = new com.mediation.website.repository.RuleRepository();
            for (com.mediation.website.entity.MediationRule rule : ruleRepo.findAll()) {
                if (rule.getSourceNodeId() == id || rule.getDestinationNodeId() == id) {
                    if (rule.isActive()) {
                        ruleRepo.updateStatus(rule.getRuleId(), false);
                    }
                }
            }
        }
        return updated;
    }

    /**
     * Deletes a node from the system.
     * Fails if the node is actively used by a Mediation Rule.
     * Upon success, it deletes the associated Docker volume directory from the host.
     *
     * @param id The ID of the node to delete.
     * @return True if the node was successfully deleted.
     * @throws IllegalArgumentException If the node is currently bound to a Mediation Rule.
     */
    public boolean deleteNode(int id) {
        Node node = nodeRepository.findById(id);
        if (node == null) {
            return false;
        }

        com.mediation.website.repository.RuleRepository ruleRepo = new com.mediation.website.repository.RuleRepository();
        for (com.mediation.website.entity.MediationRule rule : ruleRepo.findAll()) {
            if (rule.getSourceNodeId() == id || rule.getDestinationNodeId() == id) {
                throw new IllegalArgumentException("Cannot delete node. It is currently being used by a Mediation Rule.");
            }
        }
        
        boolean deleted = nodeRepository.delete(id);
        if (deleted) {
            String volumesDir = System.getenv("WORKSPACE_ROOT");
            if (volumesDir == null) {
                volumesDir = "/app";
            }
            java.io.File nodeDir = new java.io.File(volumesDir + java.io.File.separator + "node_volumes" + java.io.File.separator + node.getNodeType().toUpperCase() + java.io.File.separator + id);
            deleteDirectory(nodeDir);
        }
        
        return deleted;
    }

    /**
     * Recursively deletes a directory and all of its contents.
     * Used to clean up persistent Docker volumes when a node is deleted.
     *
     * @param directoryToBeDeleted The File object representing the directory.
     */
    private void deleteDirectory(java.io.File directoryToBeDeleted) {
        java.io.File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (java.io.File file : allContents) {
                deleteDirectory(file);
            }
        }
        directoryToBeDeleted.delete();
    }
}
