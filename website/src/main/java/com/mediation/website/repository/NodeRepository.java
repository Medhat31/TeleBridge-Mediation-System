package com.mediation.website.repository;

import com.mediation.website.entity.Node;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles database operations for Node entities in the website administration interface.
 * Provides full CRUD capabilities (Create, Read, Update, Delete) to manage network nodes.
 */
public class NodeRepository {

    /**
     * Retrieves all network nodes from the database.
     *
     * @return A list of Node objects representing every configured node.
     */
    public List<Node> findAll() {
        List<Node> nodes = new ArrayList<>();
        String sql = "SELECT * FROM Nodes ORDER BY node_id";
        
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                nodes.add(mapRowToNode(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return nodes;
    }

    /**
     * Finds a specific node by its unique database ID.
     *
     * @param id The primary key of the node.
     * @return The populated Node object, or null if it cannot be found.
     */
    public Node findById(int id) {
        String sql = "SELECT * FROM Nodes WHERE node_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToNode(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Inserts a new node into the database.
     *
     * @param node The Node object containing the configuration to persist.
     * @return The Node object updated with its generated database ID, or null on failure.
     */
    public Node insert(Node node) {
        String sql = "INSERT INTO Nodes (node_name, node_type, protocol, ip_address, port, username, password, is_active) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING node_id";
                     
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, node.getNodeName());
            stmt.setString(2, node.getNodeType());
            stmt.setString(3, node.getProtocol());
            stmt.setString(4, node.getIpAddress());
            stmt.setInt(5, node.getPort());
            stmt.setString(6, node.getUsername());
            stmt.setString(7, node.getPassword());
            stmt.setBoolean(8, node.isActive());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    node.setNodeId(rs.getInt("node_id"));
                    return node;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Updates an existing node's full configuration in the database.
     *
     * @param id   The primary key of the node to update.
     * @param node The Node object containing the updated fields.
     * @return True if the update was successful, false otherwise.
     */
    public boolean update(int id, Node node) {
        String sql = "UPDATE Nodes SET node_name=?, node_type=?, protocol=?, ip_address=?, port=?, username=?, password=? " +
                     "WHERE node_id=?";
                     
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, node.getNodeName());
            stmt.setString(2, node.getNodeType());
            stmt.setString(3, node.getProtocol());
            stmt.setString(4, node.getIpAddress());
            stmt.setInt(5, node.getPort());
            stmt.setString(6, node.getUsername());
            stmt.setString(7, node.getPassword());
            stmt.setInt(8, id);
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Toggles the active/inactive status of a node.
     *
     * @param id       The primary key of the node.
     * @param isActive True to activate the node, false to deactivate.
     * @return True if the update was successful.
     */
    public boolean updateStatus(int id, boolean isActive) {
        String sql = "UPDATE Nodes SET is_active=? WHERE node_id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setBoolean(1, isActive);
            stmt.setInt(2, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Updates the IP address and port of a node. 
     * Used primarily by the core engine to map Docker container names to nodes.
     *
     * @param id        The primary key of the node.
     * @param ipAddress The new IP address or Docker container name.
     * @param port      The new port.
     * @return True if the update was successful.
     */
    public boolean updateIpAddress(int id, String ipAddress, int port) {
        String sql = "UPDATE Nodes SET ip_address=?, port=? WHERE node_id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, ipAddress);
            stmt.setInt(2, port);
            stmt.setInt(3, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Removes a node entirely from the database.
     *
     * @param id The primary key of the node to delete.
     * @return True if the deletion was successful.
     */
    public boolean delete(int id) {
        String sql = "DELETE FROM Nodes WHERE node_id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Helper function to convert a SQL ResultSet row into a Java Node object.
     *
     * @param rs The ResultSet positioned at the current row.
     * @return A populated Node object.
     * @throws SQLException If column names are invalid or database errors occur.
     */
    private Node mapRowToNode(ResultSet rs) throws SQLException {
        return new Node(
            rs.getInt("node_id"),
            rs.getString("node_name"),
            rs.getString("node_type"),
            rs.getString("protocol"),
            rs.getString("ip_address"),
            rs.getInt("port"),
            rs.getString("username"),
            rs.getString("password"),
            rs.getBoolean("is_active")
        );
    }
}
