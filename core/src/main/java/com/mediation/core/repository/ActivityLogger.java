package com.mediation.core.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Handles database logging for system activities.
 * Ensures the system maintains a rolling window of recent events for the dashboard.
 */
public class ActivityLogger {

    private static final Logger log = LoggerFactory.getLogger(ActivityLogger.class);

    /**
     * Persists a new activity log entry to the database and triggers the rolling retention cleanup.
     *
     * @param message The descriptive text of the event.
     * @param type    The severity/type of the event ('success', 'info', 'warning', 'error').
     */
    public static void logActivity(String message, String type) {
        String insertSql = "INSERT INTO Activity_Log (message, log_type) VALUES (?, ?)";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(insertSql)) {
            
            stmt.setString(1, message);
            stmt.setString(2, type);
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            log.error("Failed to insert activity log to DB: {}", message, e);
        }
        
        cleanupOldLogs();
    }
    
    /**
     * Helper to log an 'info' level event.
     *
     * @param message The descriptive text of the event.
     */
    public static void logInfo(String message) {
        logActivity(message, "info");
    }

    /**
     * Helper to log a 'success' level event.
     *
     * @param message The descriptive text of the event.
     */
    public static void logSuccess(String message) {
        logActivity(message, "success");
    }

    /**
     * Helper to log a 'warning' level event.
     *
     * @param message The descriptive text of the event.
     */
    public static void logWarning(String message) {
        logActivity(message, "warning");
    }

    /**
     * Helper to log an 'error' level event.
     *
     * @param message The descriptive text of the event.
     */
    public static void logError(String message) {
        logActivity(message, "error");
    }

    /**
     * Enforces the retention policy by deleting all but the 500 most recent records.
     * Prevents the database from bloating indefinitely over time.
     */
    private static void cleanupOldLogs() {
        String deleteSql = "DELETE FROM Activity_Log WHERE id NOT IN (" +
                           "  SELECT id FROM Activity_Log ORDER BY log_time DESC LIMIT 500" +
                           ")";
                           
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(deleteSql)) {
            
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            log.error("Failed to cleanup old activity logs.", e);
        }
    }
}
