package com.mediation.website.repository;

import com.mediation.website.entity.ActivityLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles database operations related to the system activity log.
 * This log is displayed on the website dashboard to show recent events.
 */
public class ActivityRepository {

    /**
     * Retrieves the most recent system activity logs from the database, ordered by time.
     *
     * @param limit The maximum number of log entries to retrieve.
     * @return A list of recent ActivityLog objects.
     */
    public List<ActivityLog> getRecentActivities(int limit) {
        List<ActivityLog> activities = new ArrayList<>();
        String sql = "SELECT * FROM Activity_Log ORDER BY log_time DESC LIMIT ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, limit);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ActivityLog log = new ActivityLog();
                    log.setId(rs.getInt("id"));
                    log.setLogTime(rs.getTimestamp("log_time"));
                    log.setMessage(rs.getString("message"));
                    log.setType(rs.getString("log_type"));
                    activities.add(log);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return activities;
    }
}
