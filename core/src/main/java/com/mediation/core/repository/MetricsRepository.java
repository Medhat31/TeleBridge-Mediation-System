package com.mediation.core.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Handles database operations related to system processing metrics and analytics.
 */
public class MetricsRepository {

    private static final Logger log = LoggerFactory.getLogger(MetricsRepository.class);

    /**
     * Increments the total count of successfully processed CDRs for the current day.
     * Uses PostgreSQL upsert capabilities to insert a new row if one doesn't exist for today,
     * or adds to the existing total.
     *
     * @param count The number of successfully parsed and mediated records to add to today's total.
     */
    public void incrementCdrsToday(int count) {
        if (count <= 0) return;

        String sql = "INSERT INTO Daily_Metrics (metric_date, total_cdrs_processed) " +
                     "VALUES (CURRENT_DATE, ?) " +
                     "ON CONFLICT (metric_date) " +
                     "DO UPDATE SET total_cdrs_processed = Daily_Metrics.total_cdrs_processed + EXCLUDED.total_cdrs_processed";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, count);
            stmt.executeUpdate();
            log.info("Successfully updated daily metrics with {} new processed CDRs.", count);
            
        } catch (SQLException e) {
            log.error("Failed to update daily metrics.", e);
        }
    }
}
