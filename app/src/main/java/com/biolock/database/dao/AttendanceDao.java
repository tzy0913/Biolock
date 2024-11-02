package com.biolock.database.dao;

import com.biolock.database.DatabaseHelper;
import com.biolock.model.Attendance;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AttendanceDao {
    private final DatabaseHelper dbHelper;

    public AttendanceDao() {
        this.dbHelper = DatabaseHelper.getInstance();
    }

    public long insert(Attendance attendance) throws SQLException {
        String sql = "INSERT INTO attendance (user_id, session_id, status) VALUES (?, ?, ?)";

        try (Connection conn = dbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setLong(1, attendance.getUserId());
            pstmt.setLong(2, attendance.getSessionId());
            pstmt.setString(3, attendance.getStatus());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating attendance record failed, no rows affected.");
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                } else {
                    throw new SQLException("Creating attendance record failed, no ID obtained.");
                }
            }
        }
    }

    public List<Attendance> findBySessionId(long sessionId) throws SQLException {
        String sql = "SELECT * FROM attendance WHERE session_id = ? ORDER BY timestamp";
        List<Attendance> attendances = new ArrayList<>();

        try (Connection conn = dbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, sessionId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    attendances.add(mapResultSetToAttendance(rs));
                }
            }
        }
        return attendances;
    }

    public List<Attendance> findByUserIdAndTimeRange(long userId, Timestamp startTime, Timestamp endTime)
            throws SQLException {
        String sql = "SELECT * FROM attendance WHERE user_id = ? AND timestamp BETWEEN ? AND ? ORDER BY timestamp";
        List<Attendance> attendances = new ArrayList<>();

        try (Connection conn = dbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, userId);
            pstmt.setTimestamp(2, startTime);
            pstmt.setTimestamp(3, endTime);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    attendances.add(mapResultSetToAttendance(rs));
                }
            }
        }
        return attendances;
    }

    private Attendance mapResultSetToAttendance(ResultSet rs) throws SQLException {
        Attendance attendance = new Attendance();
        attendance.setAttendanceId(rs.getLong("attendance_id"));
        attendance.setUserId(rs.getLong("user_id"));
        attendance.setSessionId(rs.getLong("session_id"));
        attendance.setTimestamp(rs.getTimestamp("timestamp"));
        attendance.setStatus(rs.getString("status"));
        return attendance;
    }
}