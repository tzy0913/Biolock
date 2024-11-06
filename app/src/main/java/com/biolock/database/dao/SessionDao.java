package com.biolock.database.dao;

import android.util.Log;

import com.biolock.database.DatabaseHelper;
import com.biolock.model.Session;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SessionDao {
    private static final String TAG = "SessionDao";

    public Session findById(Long sessionId) throws SQLException {
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
                return mapResultSetToSession(rs);
            }
            return null;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public List<Session> findTodaySessions(Long classId) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            String sql = "SELECT * FROM sessions " +
                    "WHERE class_id = ? AND DATE(date) = CURDATE() " +
                    "ORDER BY start_time";

            conn = DatabaseHelper.getInstance().getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setLong(1, classId);
            rs = stmt.executeQuery();

            List<Session> sessions = new ArrayList<>();
            while (rs.next()) {
                sessions.add(mapResultSetToSession(rs));
            }
            return sessions;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public List<Session> findSessionsByDateRange(Long classId, Date startDate, Date endDate) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            String sql = "SELECT * FROM sessions " +
                    "WHERE class_id = ? AND date BETWEEN ? AND ? " +
                    "ORDER BY date, start_time";

            conn = DatabaseHelper.getInstance().getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setLong(1, classId);
            stmt.setDate(2, new java.sql.Date(startDate.getTime()));
            stmt.setDate(3, new java.sql.Date(endDate.getTime()));
            rs = stmt.executeQuery();

            List<Session> sessions = new ArrayList<>();
            while (rs.next()) {
                sessions.add(mapResultSetToSession(rs));
            }
            return sessions;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

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