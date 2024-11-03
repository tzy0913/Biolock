package com.biolock.database.dao;

import com.biolock.database.DatabaseHelper;
import com.biolock.model.SecuritySettings;
import java.sql.*;

public class SecuritySettingsDao {
    private final DatabaseHelper dbHelper;

    public SecuritySettingsDao() {
        this.dbHelper = DatabaseHelper.getInstance();
    }

    public void insertOrUpdate(SecuritySettings settings) throws SQLException {
        Connection conn = null;
        try {
            conn = dbHelper.getConnection();
            String sql = "INSERT INTO user_security_settings (user_id, max_failed_attempts, lockout_duration_mins) " +
                    "VALUES (?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE " +
                    "max_failed_attempts = VALUES(max_failed_attempts), " +
                    "lockout_duration_mins = VALUES(lockout_duration_mins)";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setLong(1, settings.getUserId());
                pstmt.setInt(2, settings.getMaxFailedAttempts());
                pstmt.setInt(3, settings.getLockoutDurationMins());

                pstmt.executeUpdate();
            }
        } finally {
            dbHelper.releaseConnection(conn);
        }
    }

    public void updateFailedAttempt(long userId) throws SQLException {
        Connection conn = null;
        try {
            conn = dbHelper.getConnection();
            String sql = "UPDATE user_security_settings SET " +
                    "failed_attempts_count = failed_attempts_count + 1, " +
                    "last_failed_attempt = CURRENT_TIMESTAMP " +
                    "WHERE user_id = ?";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setLong(1, userId);
                pstmt.executeUpdate();
            }
        } finally {
            dbHelper.releaseConnection(conn);
        }
    }

    public void resetFailedAttempts(long userId) throws SQLException {
        Connection conn = null;
        try {
            conn = dbHelper.getConnection();
            String sql = "UPDATE user_security_settings SET " +
                    "failed_attempts_count = 0, " +
                    "last_failed_attempt = NULL " +
                    "WHERE user_id = ?";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setLong(1, userId);
                pstmt.executeUpdate();
            }
        } finally {
            dbHelper.releaseConnection(conn);
        }
    }

    public SecuritySettings findByUserId(long userId) throws SQLException {
        Connection conn = null;
        try {
            conn = dbHelper.getConnection();
            String sql = "SELECT * FROM user_security_settings WHERE user_id = ?";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setLong(1, userId);

                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return mapResultSetToSecuritySettings(rs);
                    }
                    return null;
                }
            }
        } finally {
            dbHelper.releaseConnection(conn);
        }
    }

    private SecuritySettings mapResultSetToSecuritySettings(ResultSet rs) throws SQLException {
        SecuritySettings settings = new SecuritySettings();
        settings.setUserId(rs.getLong("user_id"));
        settings.setMaxFailedAttempts(rs.getInt("max_failed_attempts"));
        settings.setLockoutDurationMins(rs.getInt("lockout_duration_mins"));
        // Note: last_updated is handled by MySQL, no need to map it
        return settings;
    }
}