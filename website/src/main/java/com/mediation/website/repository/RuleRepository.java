package com.mediation.website.repository;

import com.mediation.website.entity.MediationRule;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles database operations for MediationRule entities in the website administration interface.
 * Provides CRUD capabilities to manage how the system filters and routes CDRs.
 */
public class RuleRepository {

    /**
     * Retrieves all mediation rules from the database.
     *
     * @return A list of MediationRule objects representing every configured rule.
     */
    public List<MediationRule> findAll() {
        List<MediationRule> rules = new ArrayList<>();
        String sql = "SELECT * FROM Mediation_Rules ORDER BY rule_id";
        
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                rules.add(mapRowToRule(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return rules;
    }

    /**
     * Finds a specific mediation rule by its unique database ID.
     *
     * @param id The primary key of the rule.
     * @return The populated MediationRule object, or null if not found.
     */
    public MediationRule findById(int id) {
        String sql = "SELECT * FROM Mediation_Rules WHERE rule_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToRule(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Inserts a new mediation rule into the database.
     *
     * @param rule The MediationRule object containing the configuration.
     * @return The MediationRule object updated with its generated database ID, or null on failure.
     */
    public MediationRule insert(MediationRule rule) {
        String sql = "INSERT INTO Mediation_Rules (source_node_id, destination_node_id, filter_zero_duration, filter_emergency, is_active) " +
                     "VALUES (?, ?, ?, ?, ?) RETURNING rule_id";
                     
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, rule.getSourceNodeId());
            stmt.setInt(2, rule.getDestinationNodeId());
            stmt.setBoolean(3, rule.isFilterZeroDuration());
            stmt.setBoolean(4, rule.isFilterEmergency());
            stmt.setBoolean(5, rule.isActive());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    rule.setRuleId(rs.getInt("rule_id"));
                    return rule;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Updates an existing mediation rule in the database.
     *
     * @param id   The primary key of the rule to update.
     * @param rule The MediationRule object containing updated values.
     * @return True if the update was successful.
     */
    public boolean update(int id, MediationRule rule) {
        String sql = "UPDATE Mediation_Rules SET source_node_id=?, destination_node_id=?, filter_zero_duration=?, filter_emergency=? " +
                     "WHERE rule_id=?";
                     
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, rule.getSourceNodeId());
            stmt.setInt(2, rule.getDestinationNodeId());
            stmt.setBoolean(3, rule.isFilterZeroDuration());
            stmt.setBoolean(4, rule.isFilterEmergency());
            stmt.setInt(5, id);
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Toggles the active/inactive status of a mediation rule.
     *
     * @param id       The primary key of the rule.
     * @param isActive True to activate, false to deactivate.
     * @return True if the update was successful.
     */
    public boolean updateStatus(int id, boolean isActive) {
        String sql = "UPDATE Mediation_Rules SET is_active=? WHERE rule_id=?";
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
     * Removes a mediation rule from the database.
     *
     * @param id The primary key of the rule to delete.
     * @return True if the deletion was successful.
     */
    public boolean delete(int id) {
        String sql = "DELETE FROM Mediation_Rules WHERE rule_id=?";
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
     * Helper function to convert a SQL ResultSet row into a Java MediationRule object.
     *
     * @param rs The ResultSet positioned at the current row.
     * @return A populated MediationRule object.
     * @throws SQLException If database errors occur.
     */
    private MediationRule mapRowToRule(ResultSet rs) throws SQLException {
        return new MediationRule(
            rs.getInt("rule_id"),
            rs.getInt("source_node_id"),
            rs.getInt("destination_node_id"),
            rs.getBoolean("filter_zero_duration"),
            rs.getBoolean("filter_emergency"),
            rs.getBoolean("is_active")
        );
    }
}
