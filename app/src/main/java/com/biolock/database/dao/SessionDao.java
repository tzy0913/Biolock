/**
 * Data Access Object for managing class sessions.
 * Handles operations related to starting, ending, and validating class sessions,
 * including generation and verification of validation codes.
 */
package com.biolock.database.dao;

// Android Core
import android.util.Log;

// Biolock Components
import com.biolock.database.DatabaseHelper;
import com.biolock.model.Session;
import com.biolock.repository.Result;

// Java SQL
import java.sql.*;

// Java Utilities
import java.util.Random;

public class SessionDao {
    private static final String TAG = "SessionDao";

    // ============================
    // Session Management Operations
    // ============================

    /**
     * Starts a session by generating and setting a validation code
     * Only works within the valid class time window
     * @param sessionId ID of the session to start
     * @return Result containing the validation code or error message
     */
    public Result<String> startSession(Long sessionId) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            // First check if session already has a code
            String checkSql = "SELECT validation_code FROM sessions " +
                    "WHERE session_id = ? AND " +
                    "NOW() BETWEEN CONCAT(date, ' ', start_time) " +
                    "AND DATE_ADD(CONCAT(date, ' ', end_time), INTERVAL 30 MINUTE)";

            conn = DatabaseHelper.getInstance().getConnection();
            stmt = conn.prepareStatement(checkSql);
            stmt.setLong(1, sessionId);
            rs = stmt.executeQuery();

            if (rs.next() && rs.getString("validation_code") != null) {
                return Result.error("Session already started");
            }

            // Generate and set new code
            String validationCode = String.format("%06d", new Random().nextInt(999999));

            String sql = "UPDATE sessions SET validation_code = ? " +
                    "WHERE session_id = ? AND " +
                    "NOW() BETWEEN CONCAT(date, ' ', start_time) " +
                    "AND DATE_ADD(CONCAT(date, ' ', end_time), INTERVAL 30 MINUTE)";

            stmt = conn.prepareStatement(sql);
            stmt.setString(1, validationCode);
            stmt.setLong(2, sessionId);

            int updated = stmt.executeUpdate();
            if (updated > 0) {
                Log.d(TAG, "Session started with code: " + validationCode);
                return Result.success(validationCode);
            } else {
                return Result.error("Cannot start session - not within class time");
            }
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    /**
     * Ends a session before its scheduled end time
     * Only works for active sessions that haven't ended yet
     * @param sessionId ID of the session to end
     * @return Result indicating success or failure
     */
    public Result<Void> endSessionEarly(Long sessionId) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            // First verify this is an active session
            String checkSql = "SELECT end_time FROM sessions " +
                    "WHERE session_id = ? AND validation_code IS NOT NULL " +
                    "AND NOW() < CONCAT(date, ' ', end_time)";

            conn = DatabaseHelper.getInstance().getConnection();
            stmt = conn.prepareStatement(checkSql);
            stmt.setLong(1, sessionId);
            rs = stmt.executeQuery();

            if (!rs.next()) {
                return Result.error("Session not found or already ended");
            }

            // Update end time to current time
            String sql = "UPDATE sessions SET end_time = CURRENT_TIME() " +
                    "WHERE session_id = ? AND validation_code IS NOT NULL";

            stmt = conn.prepareStatement(sql);
            stmt.setLong(1, sessionId);

            int updated = stmt.executeUpdate();
            if (updated > 0) {
                Log.d(TAG, "Session ended early: " + sessionId);
                return Result.success(null);
            } else {
                return Result.error("Failed to end session");
            }
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    // ============================
    // Session Validation Operations
    // ============================

    /**
     * Validates a session code within the active time window
     * @param sessionId ID of the session
     * @param code Validation code to verify
     * @return Result containing boolean indicating if code is valid
     */
    public Result<Boolean> validateSessionCode(Long sessionId, String code) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            String sql = "SELECT 1 FROM sessions " +
                    "WHERE session_id = ? AND validation_code = ? " +
                    "AND NOW() BETWEEN CONCAT(date, ' ', start_time) " +
                    "AND DATE_ADD(CONCAT(date, ' ', end_time), INTERVAL 30 MINUTE)";

            conn = DatabaseHelper.getInstance().getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setLong(1, sessionId);
            stmt.setString(2, code);

            rs = stmt.executeQuery();
            return Result.success(rs.next());
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    // ============================
    // Session Retrieval Operations
    // ============================

    /**
     * Retrieves detailed session information by ID
     * @param sessionId ID of the session to retrieve
     * @return Result containing Session object or error message
     */
    public Result<Session> getSessionById(Long sessionId) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            String sql = "SELECT * FROM sessions WHERE session_id = ?";

            conn = DatabaseHelper.getInstance().getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setLong(1, sessionId);

            rs = stmt.executeQuery();
            if (rs.next()) {
                Session session = mapResultSetToSession(rs);
                return Result.success(session);
            } else {
                return Result.error("Session not found");
            }
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    // ============================
    // Helper Methods
    // ============================

    /**
     * Maps a database result set row to a Session object
     */
    private Session mapResultSetToSession(ResultSet rs) throws SQLException {
        Session session = new Session();
        session.setSessionId(rs.getLong("session_id"));
        session.setClassId(rs.getLong("class_id"));
        session.setDate(rs.getDate("date"));
        session.setStartTime(rs.getTime("start_time"));
        session.setEndTime(rs.getTime("end_time"));
        session.setValidationCode(rs.getString("validation_code"));
        return session;
    }

    /**
     * Safely closes database resources
     */
    private void closeResources(Connection conn, Statement stmt, ResultSet rs) {
        if (rs != null) {
            try { rs.close(); } catch (SQLException e) { Log.e(TAG, "Error closing ResultSet", e); }
        }
        if (stmt != null) {
            try { stmt.close(); } catch (SQLException e) { Log.e(TAG, "Error closing Statement", e); }
        }
        if (conn != null) {
            DatabaseHelper.getInstance().releaseConnection(conn);
        }
    }
}