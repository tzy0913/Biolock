package com.biolock.database.dao;

import com.biolock.database.DatabaseHelper;
import com.biolock.model.Session;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SessionDao {
    private final DatabaseHelper dbHelper;

    public SessionDao() {
        this.dbHelper = DatabaseHelper.getInstance();
    }

    public long insert(Session session) throws SQLException {
        String sql = "INSERT INTO sessions (class_id, date, start_time, end_time) VALUES (?, ?, ?, ?)";

        try (Connection conn = dbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setLong(1, session.getClassId());
            pstmt.setDate(2, session.getDate());
            pstmt.setTime(3, session.getStartTime());
            pstmt.setTime(4, session.getEndTime());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating session failed, no rows affected.");
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                } else {
                    throw new SQLException("Creating session failed, no ID obtained.");
                }
            }
        }
    }

    public List<Session> findByClassId(long classId) throws SQLException {
        String sql = "SELECT * FROM sessions WHERE class_id = ? ORDER BY date, start_time";
        List<Session> sessions = new ArrayList<>();

        try (Connection conn = dbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, classId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    sessions.add(mapResultSetToSession(rs));
                }
            }
        }
        return sessions;
    }

    private Session mapResultSetToSession(ResultSet rs) throws SQLException {
        Session session = new Session();
        session.setSessionId(rs.getLong("session_id"));
        session.setClassId(rs.getLong("class_id"));
        session.setDate(rs.getDate("date"));
        session.setStartTime(rs.getTime("start_time"));
        session.setEndTime(rs.getTime("end_time"));
        return session;
    }
}