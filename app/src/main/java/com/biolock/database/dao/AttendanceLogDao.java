package com.biolock.database.dao;

import com.biolock.database.DatabaseHelper;
import com.biolock.model.AttendanceLog;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AttendanceLogDao {
    private final DatabaseHelper dbHelper;

    public AttendanceLogDao() {
        this.dbHelper = DatabaseHelper.getInstance();
    }

    public long insert(AttendanceLog log) throws SQLException {
        String sql = "INSERT INTO logs (user_id, action, attendance_id) VALUES (?, ?, ?)";

        try (Connection conn = dbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setLong(1, log.getUserId());
            pstmt.setString(2, log.getAction());
            pstmt.setLong(3, log.getAttendanceId());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating attendance log failed, no rows affected.");
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                } else {
                    throw new SQLException("Creating attendance log failed, no ID obtained.");
                }
            }
        }
    }

    public List<AttendanceLog> findByUserId(long userId) throws SQLException {
        String sql = "SELECT * FROM logs WHERE user_id = ? ORDER BY timestamp DESC";
        List<AttendanceLog> logs = new ArrayList<>();

        try (Connection conn = dbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapResultSetToAttendanceLog(rs));
                }
            }
        }
        return logs;
    }

    public List<AttendanceLog> findByAttendanceId(long attendanceId) throws SQLException {
        String sql = "SELECT * FROM logs WHERE attendance_id = ? ORDER BY timestamp DESC";
        List<AttendanceLog> logs = new ArrayList<>();

        try (Connection conn = dbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, attendanceId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapResultSetToAttendanceLog(rs));
                }
            }
        }
        return logs;
    }

    private AttendanceLog mapResultSetToAttendanceLog(ResultSet rs) throws SQLException {
        AttendanceLog log = new AttendanceLog();
        log.setLogId(rs.getLong("log_id"));
        log.setUserId(rs.getLong("user_id"));
        log.setTimestamp(rs.getTimestamp("timestamp"));
        log.setAction(rs.getString("action"));
        log.setAttendanceId(rs.getLong("attendance_id"));
        return log;
    }
}