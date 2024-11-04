package com.biolock.database.dao;

import com.biolock.database.DatabaseHelper;
import com.biolock.model.Session;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class SessionDao {
    private final DatabaseHelper dbHelper;

    public SessionDao() {
        this.dbHelper = DatabaseHelper.getInstance();
    }

    public int insert(Session session) throws SQLException {
        Connection conn = null;
        try {
            conn = dbHelper.getConnection();
            String sql = "INSERT INTO sessions (class_id, date, start_time, end_time) VALUES (?, ?, ?, ?)";

            try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setInt(1, session.getClassId());
                pstmt.setString(2, session.getDate().toString());
                pstmt.setString(3, session.getStartTime().toString());
                pstmt.setString(4, session.getEndTime().toString());

                int affectedRows = pstmt.executeUpdate();
                if (affectedRows == 0) {
                    throw new SQLException("Creating session failed, no rows affected.");
                }

                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return generatedKeys.getInt(1);
                    } else {
                        throw new SQLException("Creating session failed, no ID obtained.");
                    }
                }
            }
        } finally {
            dbHelper.releaseConnection(conn);
        }
    }

    public Session getById(int sessionId) throws SQLException {
        Connection conn = null;
        try {
            conn = dbHelper.getConnection();
            String sql = "SELECT * FROM sessions WHERE session_id = ?";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, sessionId);

                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return mapResultSetToSession(rs);
                    }
                    return null;
                }
            }
        } finally {
            dbHelper.releaseConnection(conn);
        }
    }

    public List<Session> getByDateRange(LocalDate startDate, LocalDate endDate) throws SQLException {
        Connection conn = null;
        try {
            conn = dbHelper.getConnection();
            String sql = "SELECT * FROM sessions WHERE date BETWEEN ? AND ? ORDER BY date DESC, start_time DESC";
            List<Session> sessions = new ArrayList<>();

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, startDate.toString());
                pstmt.setString(2, endDate.toString());

                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        sessions.add(mapResultSetToSession(rs));
                    }
                }
            }
            return sessions;
        } finally {
            dbHelper.releaseConnection(conn);
        }
    }

    private Session mapResultSetToSession(ResultSet rs) throws SQLException {
        Session session = new Session();
        session.setSessionId(rs.getInt("session_id"));
        session.setClassId(rs.getInt("class_id"));
        session.setDate(LocalDate.parse(rs.getString("date")));
        session.setStartTime(LocalTime.parse(rs.getString("start_time")));
        session.setEndTime(LocalTime.parse(rs.getString("end_time")));
        return session;
    }
}