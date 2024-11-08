/**
 * Data Access Object for managing face recognition log entries.
 * Handles logging of face recognition attempts, retrieving logs, and tracking failed attempts.
 */
package com.biolock.database.dao;

import android.util.Log;
import com.biolock.database.DatabaseHelper;
import com.biolock.model.FaceRecognitionLog;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FaceRecognitionLogDao {
    private static final String TAG = "FaceRecognitionLogDao";

    // ============================
    // Logging Operations
    // ============================

    /**
     * Logs a face recognition attempt in the database
     * @param log FaceRecognitionLog object containing attempt details
     */
    public void logAttempt(FaceRecognitionLog log) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            String sql = "INSERT INTO face_recognition_logs (user_id, attempt_timestamp, success, " +
                    "similarity, device_info, ip_address, action_type) " +
                    "VALUES (?, NOW(), ?, ?, ?, ?, ?)";

            conn = DatabaseHelper.getInstance().getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setLong(1, log.getUserId());
            stmt.setBoolean(2, log.isSuccess());
            stmt.setFloat(3, log.getSimilarity());
            stmt.setString(4, log.getDeviceInfo());
            stmt.setString(5, log.getIpAddress());
            stmt.setString(6, log.getActionType().name());

            stmt.executeUpdate();
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    // ============================
    // Log Retrieval Operations
    // ============================

    /**
     * Retrieves recent face recognition logs for a user
     * @param userId ID of the user
     * @param limit Maximum number of logs to retrieve
     * @return List of FaceRecognitionLog objects
     */
    public List<FaceRecognitionLog> getRecentLogs(Long userId, int limit) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            String sql = "SELECT * FROM face_recognition_logs WHERE user_id = ? " +
                    "ORDER BY attempt_timestamp DESC LIMIT ?";

            conn = DatabaseHelper.getInstance().getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setLong(1, userId);
            stmt.setInt(2, limit);
            rs = stmt.executeQuery();

            List<FaceRecognitionLog> logs = new ArrayList<>();
            while (rs.next()) {
                logs.add(mapResultSetToLog(rs));
            }
            return logs;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    /**
     * Retrieves face recognition logs within a date range
     * @param userId ID of the user
     * @param start Start date of the range
     * @param end End date of the range
     * @return List of FaceRecognitionLog objects
     */
    public List<FaceRecognitionLog> getLogsByDateRange(Long userId, Date start, Date end) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            String sql = "SELECT * FROM face_recognition_logs WHERE user_id = ? " +
                    "AND attempt_timestamp BETWEEN ? AND ? ORDER BY attempt_timestamp DESC";

            conn = DatabaseHelper.getInstance().getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setLong(1, userId);
            stmt.setTimestamp(2, new Timestamp(start.getTime()));
            stmt.setTimestamp(3, new Timestamp(end.getTime()));
            rs = stmt.executeQuery();

            List<FaceRecognitionLog> logs = new ArrayList<>();
            while (rs.next()) {
                logs.add(mapResultSetToLog(rs));
            }
            return logs;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    // ============================
    // Security Analysis Operations
    // ============================

    /**
     * Counts failed login attempts since a specific time
     * @param userId ID of the user
     * @param since Time from which to count failed attempts
     * @return Number of failed login attempts
     */
    public int getFailedAttempts(Long userId, Date since) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            String sql = "SELECT COUNT(*) FROM face_recognition_logs " +
                    "WHERE user_id = ? AND success = false " +
                    "AND attempt_timestamp > ? AND action_type = 'LOGIN'";

            conn = DatabaseHelper.getInstance().getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setLong(1, userId);
            stmt.setTimestamp(2, new Timestamp(since.getTime()));
            rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    // ============================
    // Helper Methods
    // ============================

    /**
     * Maps a database result set row to a FaceRecognitionLog object
     * @param rs ResultSet containing log data
     * @return Populated FaceRecognitionLog object
     */
    private FaceRecognitionLog mapResultSetToLog(ResultSet rs) throws SQLException {
        FaceRecognitionLog log = new FaceRecognitionLog();

        // Basic log details
        log.setLogId(rs.getLong("log_id"));
        log.setUserId(rs.getLong("user_id"));
        log.setAttemptTimestamp(rs.getTimestamp("attempt_timestamp"));

        // Recognition details
        log.setSuccess(rs.getBoolean("success"));
        log.setSimilarity(rs.getFloat("similarity"));

        // Device and connection info
        log.setDeviceInfo(rs.getString("device_info"));
        log.setIpAddress(rs.getString("ip_address"));

        // Action type
        log.setActionType(FaceRecognitionLog.ActionType.valueOf(rs.getString("action_type")));

        return log;
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