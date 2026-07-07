package com.mediation.website.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Handles database operations for fetching system metrics and statistics.
 * Primarily used to populate the website dashboard with analytics.
 */
public class MetricsRepository {

    /**
     * Retrieves the total number of Call Detail Records (CDRs) that have been successfully
     * processed by the core engine today.
     *
     * @return The integer count of processed records, or 0 if no records exist for today.
     */
    public int getCdrsProcessedToday() {
        String sql = "SELECT total_cdrs_processed FROM Daily_Metrics ORDER BY metric_date DESC LIMIT 1";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getInt("total_cdrs_processed");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0; 
    }
}
