/**
 * Data Access Object for managing user security settings.
 * Handles CRUD operations for user-specific security configurations such as
 * failed login attempts and lockout durations.
 */
package com.biolock.database.dao;

import android.util.Log;
import com.biolock.database.DatabaseHelper;
import com.biolock.model.SecuritySettings;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SecuritySettingsDao {
    private static final String TAG = "SecuritySettingsDao";

    // ============================
    // Read Operations
    // ============================

    /**
     * Retrieves security settings for a specific user
     * @param userId ID of the user
     * @return SecuritySettings object if found, null otherwise
     */
    public SecuritySettings getSettings(Long userId) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            String sql = "SELECT * FROM user_security_settings WHERE user_id = ?";

            conn = DatabaseHelper.getInstance().getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setLong(1, userId);
            rs = stmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToSecuritySettings(rs);
            }
            return null;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    // ============================
    // Write Operations
    // ============================

    /**
     * Creates new security settings for a user
     * @param settings SecuritySettings object containing user's security configuration
     */
    public void save(SecuritySettings settings) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            String sql = "INSERT INTO user_security_settings " +
                    "(user_id, max_failed_attempts, lockout_duration_mins) " +
                    "VALUES (?, ?, ?)";

            conn = DatabaseHelper.getInstance().getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setLong(1, settings.getUserId());
            stmt.setInt(2, settings.getMaxFailedAttempts());
            stmt.setInt(3, settings.getLockoutDurationMins());

            stmt.executeUpdate();
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    /**
     * Updates existing security settings for a user
     * @param settings SecuritySettings object containing updated configuration
     */
    public void updateSettings(SecuritySettings settings) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            String sql = "UPDATE user_security_settings SET " +
                    "max_failed_attempts = ?, " +
                    "lockout_duration_mins = ? " +
                    "WHERE user_id = ?";

            conn = DatabaseHelper.getInstance().getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, settings.getMaxFailedAttempts());
            stmt.setInt(2, settings.getLockoutDurationMins());
            stmt.setLong(3, settings.getUserId());

            stmt.executeUpdate();
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    // ============================
    // Helper Methods
    // ============================

    /**
     * Maps a database result set row to a SecuritySettings object
     * @param rs ResultSet containing security settings data
     * @return Populated SecuritySettings object
     */
    private SecuritySettings mapResultSetToSecuritySettings(ResultSet rs) throws SQLException {
        SecuritySettings settings = new SecuritySettings();

        // User identifier
        settings.setUserId(rs.getLong("user_id"));

        // Security parameters
        settings.setMaxFailedAttempts(rs.getInt("max_failed_attempts"));
        settings.setLockoutDurationMins(rs.getInt("lockout_duration_mins"));

        // Metadata
        settings.setLastUpdated(rs.getTimestamp("last_updated"));

        return settings;
    }

    /**
     * Safely closes database resources
     */
    private void closeResources(Connection conn, Statement stmt, ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                Log.e(TAG, "Error closing ResultSet", e);
            }
        }
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                Log.e(TAG, "Error closing Statement", e);
            }
        }
        if (conn != null) {
            DatabaseHelper.getInstance().releaseConnection(conn);
        }
    }
}