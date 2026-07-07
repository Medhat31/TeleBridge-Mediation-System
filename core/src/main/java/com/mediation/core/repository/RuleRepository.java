package com.mediation.core.repository;

import com.mediation.core.entities.MediationRule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository class handling database operations for MediationRules in the core engine.
 */
public class RuleRepository {

    private static final Logger log = LoggerFactory.getLogger(RuleRepository.class);

    /**
     * Retrieves all mediation rules currently marked as active in the database.
     * These rules dictate how raw CDRs are filtered and routed by the mediation pipeline.
     *
     * @return A list of active MediationRule objects. Returns an empty list if none are found or if a database error occurs.
     */
    public List<MediationRule> getActiveRules() {
        List<MediationRule> rules = new ArrayList<>();
        String sql = "SELECT rule_id, source_node_id, destination_node_id, " +
                     "filter_zero_duration, filter_emergency, is_active " +
                     "FROM Mediation_Rules WHERE is_active = TRUE";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                rules.add(new MediationRule(
                        rs.getInt("rule_id"),
                        rs.getInt("source_node_id"),
                        rs.getInt("destination_node_id"),
                        rs.getBoolean("filter_zero_duration"),
                        rs.getBoolean("filter_emergency"),
                        rs.getBoolean("is_active")
                ));
            }
            log.info("Fetched {} active mediation rules.", rules.size());
        } catch (SQLException e) {
            log.error("Database error while fetching active rules.", e);
        }
        return rules;
    }
}