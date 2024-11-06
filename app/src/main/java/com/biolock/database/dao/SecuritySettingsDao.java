package com.biolock.database.dao;

import android.util.Log;

import com.biolock.database.DatabaseHelper;
import com.biolock.model.SecuritySettings;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SecuritySettingsDao {
    private static final String TAG = "SecuritySettingsDao";

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

    private SecuritySettings mapResultSetToSecuritySettings(ResultSet rs) throws SQLException {
        SecuritySettings settings = new SecuritySettings();
        settings.setUserId(rs.getLong("user_id"));
        settings.setMaxFailedAttempts(rs.getInt("max_failed_attempts"));
        settings.setLockoutDurationMins(rs.getInt("lockout_duration_mins"));
        settings.setLastUpdated(rs.getTimestamp("last_updated"));
        return settings;
    }

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