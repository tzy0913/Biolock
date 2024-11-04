package com.biolock.database.dao;

import com.biolock.database.DatabaseHelper;
import com.biolock.model.Attendance;
import java.sql.*;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class AttendanceDao {
    private final DatabaseHelper dbHelper;

    public AttendanceDao() {
        this.dbHelper = DatabaseHelper.getInstance();
    }

    public long insert(Attendance attendance) throws SQLException {
        Connection conn = null;
        try {
            conn = dbHelper.getConnection();
            String sql = "INSERT INTO attendance (user_id, session_id, timestamp, status) VALUES (?, ?, ?, ?)";

            try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setLong(1, attendance.getUserId());
                pstmt.setInt(2, attendance.getSessionId());
                pstmt.setString(3, attendance.getTimestamp().toString());
                pstmt.setString(4, attendance.getStatus());

                int affectedRows = pstmt.executeUpdate();
                if (affectedRows == 0) {
                    throw new SQLException("Creating attendance failed, no rows affected.");
                }

                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return generatedKeys.getLong(1);
                    } else {
                        throw new SQLException("Creating attendance failed, no ID obtained.");
                    }
                }
            }
        } finally {
            dbHelper.releaseConnection(conn);
        }
    }

    public List<Attendance> getByUserId(long userId) throws SQLException {
        Connection conn = null;
        try {
            conn = dbHelper.getConnection();
            String sql = "SELECT * FROM attendance WHERE user_id = ?";

            List<Attendance> attendanceList = new ArrayList<>();
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setLong(1, userId);

                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        attendanceList.add(mapResultSetToAttendance(rs));
                    }
                }
            }
            return attendanceList;
        } finally {
            dbHelper.releaseConnection(conn);
        }
    }

    private Attendance mapResultSetToAttendance(ResultSet rs) throws SQLException {
        Attendance attendance = new Attendance();
        attendance.setAttendanceId(rs.getInt("attendance_id"));
        attendance.setUserId(rs.getLong("user_id"));
        attendance.setSessionId(rs.getInt("session_id"));
        attendance.setTimestamp(LocalTime.parse(rs.getString("timestamp")));
        attendance.setStatus(rs.getString("status"));
        return attendance;
    }
}