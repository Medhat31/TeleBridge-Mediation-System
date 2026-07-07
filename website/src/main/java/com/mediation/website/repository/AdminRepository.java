package com.mediation.website.repository;

import com.mediation.website.entity.Admin;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Handles database operations for system administrators.
 * Used primarily for user authentication on the website.
 */
public class AdminRepository {

    /**
     * Finds an administrator in the database by their email address.
     * Used during the login process to verify credentials.
     *
     * @param email The email address of the administrator to find.
     * @return An Admin object if found, or null if no administrator exists with that email.
     */
    public Admin findByEmail(String email) {
        String sql = "SELECT * FROM Admins WHERE email = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, email);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Admin(
                        rs.getInt("admin_id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("password")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
