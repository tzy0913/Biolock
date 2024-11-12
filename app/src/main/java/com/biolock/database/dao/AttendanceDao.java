/**
 * Data Access Object for handling attendance-related database operations.
 * Provides methods for marking attendance and retrieving attendance records.
 */
package com.biolock.database.dao;

// Android Core
import android.util.Log;

// Biolock Components
import com.biolock.database.DatabaseHelper;
import com.biolock.model.Attendance;

// Java SQL
import java.sql.*;

// Java Collections
import java.util.ArrayList;
import java.util.List;

public class AttendanceDao {
    private static final String TAG = "AttendanceDao";

    /**
     * Records attendance for a user in a specific session
     * @param userId ID of the user marking attendance
     * @param sessionId ID of the session for which attendance is being marked
     * @throws SQLException if database operation fails
     */
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

    /**
     * Retrieves attendance records for a specific session
     * Includes all assigned students, marking absent students as "NOT MARKED"
     * @param sessionId ID of the session to retrieve attendance for
     * @return List of Attendance objects containing attendance records
     * @throws SQLException if database operation fails
     */
    public List<Attendance> getSessionAttendance(Long sessionId) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
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

    /**
     * Maps a database result set to an Attendance object
     * @param rs ResultSet containing attendance data
     * @return Populated Attendance object
     * @throws SQLException if mapping fails
     */
    private Attendance mapResultSetToAttendance(ResultSet rs) throws SQLException {
        Attendance attendance = new Attendance();

        // Basic attendance info
        attendance.setAttendanceId(rs.getLong("attendance_id"));
        attendance.setUserId(rs.getLong("user_id"));
        attendance.setSessionId(rs.getLong("session_id"));
        attendance.setTimestamp(rs.getTime("timestamp"));
        attendance.setStatus(rs.getString("status"));

        // Session details
        attendance.setSessionDate(rs.getDate("date"));
        attendance.setStartTime(rs.getTime("start_time"));
        attendance.setEndTime(rs.getTime("end_time"));

        // Module info
        attendance.setModuleCode(rs.getString("module_code"));
        attendance.setModuleName(rs.getString("module_name"));
        attendance.setSection(rs.getString("section"));
        attendance.setRoom(rs.getString("room"));

        // Student info
        attendance.setStudentName(rs.getString("student_name"));

        return attendance;
    }

    /**
     * Safely closes database resources
     * @param conn Database connection to close
     * @param stmt SQL statement to close
     * @param rs ResultSet to close
     */
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