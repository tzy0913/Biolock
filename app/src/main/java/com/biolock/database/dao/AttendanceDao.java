package com.biolock.database.dao;

import android.util.Log;
import com.biolock.database.DatabaseHelper;
import com.biolock.model.Attendance;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AttendanceDao {
    private static final String TAG = "AttendanceDao";

    // For marking attendance
    public void markAttendance(Long userId, Long sessionId) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            String sql = "INSERT INTO attendance (user_id, session_id, timestamp) " +
                    "VALUES (?, ?, CURRENT_TIME())";

            conn = DatabaseHelper.getInstance().getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setLong(1, userId);
            stmt.setLong(2, sessionId);
            stmt.executeUpdate();
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    // Get attendance for a specific session
    public List<Attendance> getSessionAttendance(Long sessionId) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            // Modified query to show all assigned students
            String sql =
                    "SELECT u.name as student_name, u.email as student_email, " +
                            "a.attendance_id, a.user_id, a.session_id, a.timestamp, a.status " +
                            "FROM class_assignments ca " +
                            "JOIN users u ON ca.user_id = u.user_id " +
                            "JOIN sessions s ON ca.class_id = s.class_id " +
                            "LEFT JOIN attendance a ON a.user_id = u.user_id AND a.session_id = s.session_id " +
                            "WHERE s.session_id = ? " +
                            "ORDER BY u.name";

            conn = DatabaseHelper.getInstance().getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setLong(1, sessionId);
            rs = stmt.executeQuery();

            List<Attendance> attendances = new ArrayList<>();
            while (rs.next()) {
                Attendance attendance = new Attendance();
                attendance.setSessionId(sessionId);
                attendance.setAttendanceId(rs.getLong("attendance_id"));
                attendance.setUserId(rs.getLong("user_id"));
                attendance.setTimestamp(rs.getTime("timestamp"));

                // If no attendance record exists, mark as NOT MARKED
                String status = rs.getString("status");
                attendance.setStatus(status != null ? status : "NOT MARKED");

                attendance.setStudentName(rs.getString("student_name"));
                attendance.setStudentEmail(rs.getString("student_email"));
                attendances.add(attendance);
            }
            return attendances;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    private Attendance mapResultSetToAttendance(ResultSet rs) throws SQLException {
        Attendance attendance = new Attendance();
        attendance.setAttendanceId(rs.getLong("attendance_id"));
        attendance.setUserId(rs.getLong("user_id"));
        attendance.setSessionId(rs.getLong("session_id"));
        attendance.setTimestamp(rs.getTime("timestamp"));
        attendance.setStatus(rs.getString("status"));

        attendance.setSessionDate(rs.getDate("date"));
        attendance.setStartTime(rs.getTime("start_time"));
        attendance.setEndTime(rs.getTime("end_time"));
        attendance.setModuleCode(rs.getString("module_code"));
        attendance.setModuleName(rs.getString("module_name"));
        attendance.setSection(rs.getString("section"));
        attendance.setRoom(rs.getString("room"));
        attendance.setStudentName(rs.getString("student_name"));

        return attendance;
    }

    private void closeResources(Connection conn, Statement stmt, ResultSet rs) {
        if (rs != null) {
            try { rs.close(); } catch (SQLException e) {
                Log.e(TAG, "Error closing ResultSet", e);
            }
        }
        if (stmt != null) {
            try { stmt.close(); } catch (SQLException e) {
                Log.e(TAG, "Error closing Statement", e);
            }
        }
        if (conn != null) {
            DatabaseHelper.getInstance().releaseConnection(conn);
        }
    }
}