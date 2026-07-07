package com.mediation.core.repository;

import com.mediation.core.entities.Node;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles database operations for Node entities in the core engine.
 */
public class NodeRepository {

    private static final Logger log = LoggerFactory.getLogger(NodeRepository.class);

    /**
     * Retrieves all network nodes (upstream and downstream) stored in the database.
     * This includes both active and inactive nodes.
     *
     * @return A list of populated Node objects.
     */
    public List<Node> getAllNodes() {
        List<Node> nodes = new ArrayList<>();
        String sql = "SELECT node_id, node_name, node_type, protocol, ip_address, port, " +
                     "username, password, is_active FROM Nodes";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                nodes.add(new Node(
                        rs.getInt("node_id"),
                        rs.getString("node_name"),
                        rs.getString("node_type"),
                        rs.getString("protocol"),
                        rs.getString("ip_address"),
                        rs.getInt("port"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getBoolean("is_active")
                ));
            }
            log.info("Fetched {} nodes from database.", nodes.size());
        } catch (SQLException e) {
            log.error("Database error while fetching nodes.", e);
        }
        return nodes;
    }

    /**
     * Updates the IP address field for a given node in the database.
     * In this containerized environment, the ip_address field maps to the internal Docker container name.
     *
     * @param nodeId        The primary key ID of the node.
     * @param containerName The new internal Docker network name for the node.
     */
    public void updateIpAddress(int nodeId, String containerName) {
        String sql = "UPDATE Nodes SET ip_address = ? WHERE node_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, containerName);
            stmt.setInt(2, nodeId);
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                log.debug("  -> Synced ip_address for node_id {} to '{}'", nodeId, containerName);
            }
        } catch (SQLException e) {
            log.error("Failed to update ip_address for node_id {}", nodeId, e);
        }
    }
}