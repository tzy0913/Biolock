/**
 * Data Access Object for handling class-related database operations.
 * Provides methods for retrieving class information for both instructors and students.
 */
package com.biolock.database.dao;

// Android Core
import android.util.Log;

// Biolock Components
import com.biolock.database.DatabaseHelper;
import com.biolock.model.Attendance;
import com.biolock.model.CourseClass;

// Java SQL
import java.sql.*;

// Java Collections
import java.util.ArrayList;
import java.util.List;

public class ClassDao {
    private static final String TAG = "ClassDao";

    // ============================
    // Instructor-related methods
    // ============================

    /**
     * Retrieves classes for an instructor within a specified date range
     * Includes class status (UPCOMING, ONGOING, COMPLETED)
     */
    public List<CourseClass> findClassesByInstructor(Long instructorId, Date startDate, Date endDate) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            String sql = "SELECT DISTINCT c.*, s.session_id, s.date, s.start_time, s.end_time, s.validation_code, " +
                    "CASE " +
                    "   WHEN NOW() < CONCAT(s.date, ' ', s.start_time) THEN 'UPCOMING' " +
                    "   WHEN NOW() BETWEEN CONCAT(s.date, ' ', s.start_time) AND DATE_ADD(CONCAT(s.date, ' ', s.end_time), INTERVAL 30 MINUTE) THEN 'ONGOING' " +
                    "   ELSE 'COMPLETED' " +
                    "END as class_status " +
                    "FROM classes c " +
                    "JOIN sessions s ON c.class_id = s.class_id " +
                    "WHERE c.instructor_id = ? AND s.date BETWEEN ? AND ? " +
                    "ORDER BY s.date, s.start_time";

            conn = DatabaseHelper.getInstance().getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setLong(1, instructorId);
            stmt.setDate(2, startDate);
            stmt.setDate(3, endDate);

            // Log the query parameters
            Log.d(TAG, "SQL: " + sql);
            Log.d(TAG, String.format("instructorId: %d, startDate: %s, endDate: %s",
                    instructorId, startDate, endDate));

            rs = stmt.executeQuery();

            List<CourseClass> classes = new ArrayList<>();
            while (rs.next()) {
                CourseClass classObj = mapResultSetToClass(rs);
                classes.add(classObj);

                Log.d(TAG, String.format("Found class: ID=%d, Module=%s, Session=%d, Date=%s, Status=%s, Code=%s",
                        classObj.getClassId(),
                        classObj.getModuleCode(),
                        classObj.getSessionId(),
                        classObj.getSessionDate(),
                        classObj.getStatus(),
                        classObj.getValidationCode()));
            }

            Log.d(TAG, "Total classes found: " + classes.size());
            return classes;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    // ============================
    // Student-related methods
    // ============================

    /**
     * Retrieves classes and attendance status for a student within a date range
     */
    public List<Attendance> findClassesByStudent(Long studentId, Date startDate, Date endDate) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            String sql =
                    "SELECT c.*, s.session_id, s.date, s.start_time, s.end_time, s.validation_code, " +
                            "a.attendance_id, a.timestamp, a.status, " +
                            "CASE " +
                            "   WHEN a.status IS NOT NULL THEN UPPER(a.status) " +
                            "   WHEN NOW() < CONCAT(s.date, ' ', s.start_time) THEN 'UPCOMING' " +
                            "   WHEN NOW() BETWEEN CONCAT(s.date, ' ', s.start_time) AND DATE_ADD(CONCAT(s.date, ' ', s.end_time), INTERVAL 30 MINUTE) THEN 'ONGOING' " +
                            "   ELSE 'ABSENT' " +
                            "END as calculated_status " +
                            "FROM classes c " +
                            "JOIN class_assignments ca ON c.class_id = ca.class_id " +
                            "JOIN sessions s ON c.class_id = s.class_id " +
                            "LEFT JOIN attendance a ON s.session_id = a.session_id AND a.user_id = ? " +
                            "WHERE ca.user_id = ? AND s.date BETWEEN ? AND ? " +
                            "ORDER BY s.date, s.start_time";

            conn = DatabaseHelper.getInstance().getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setLong(1, studentId);
            stmt.setLong(2, studentId);
            stmt.setDate(3, startDate);
            stmt.setDate(4, endDate);
            rs = stmt.executeQuery();

            List<Attendance> attendances = new ArrayList<>();
            while (rs.next()) {
                Attendance attendance = mapResultSetToAttendance(rs, studentId);
                attendance.setStatus(rs.getString("calculated_status"));
                attendances.add(attendance);
            }
            return attendances;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    /**
     * Retrieves classes and attendance status for a student on a specific date
     */
    public List<Attendance> findClassesByStudentForDate(Long studentId, Date date) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            String sql =
                    "SELECT c.*, s.session_id, s.date, s.start_time, s.end_time, " +
                            "a.attendance_id, a.timestamp, a.status, " + "s.validation_code, " +
                            "CASE " +
                            "   WHEN a.status IS NOT NULL THEN UPPER(a.status) " +
                            "   WHEN NOW() < CONCAT(s.date, ' ', s.start_time) THEN 'UPCOMING' " +
                            "   WHEN NOW() BETWEEN CONCAT(s.date, ' ', s.start_time) AND DATE_ADD(CONCAT(s.date, ' ', s.end_time), INTERVAL 30 MINUTE) THEN 'ONGOING' " +
                            "   ELSE 'ABSENT' " +
                            "END as calculated_status " +
                            "FROM classes c " +
                            "JOIN class_assignments ca ON c.class_id = ca.class_id " +
                            "JOIN sessions s ON c.class_id = s.class_id " +
                            "LEFT JOIN attendance a ON s.session_id = a.session_id AND a.user_id = ? " +
                            "WHERE ca.user_id = ? AND DATE(s.date) = ? " +
                            "ORDER BY s.start_time";

            conn = DatabaseHelper.getInstance().getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setLong(1, studentId);
            stmt.setLong(2, studentId);
            stmt.setDate(3, date);
            rs = stmt.executeQuery();

            List<Attendance> attendances = new ArrayList<>();
            while (rs.next()) {
                Attendance attendance = mapResultSetToAttendance(rs, studentId);
                attendance.setStatus(rs.getString("calculated_status"));
                attendances.add(attendance);
            }
            return attendances;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    /**
     * Retrieves classes for week view with attendance status
     */
    public List<Attendance> findClassesByStudentRange(Long studentId, Date startDate, Date endDate) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            String sql =
                    "SELECT c.*, s.session_id, s.date, s.start_time, s.end_time, " +
                            "a.attendance_id, a.timestamp, a.status, " +
                            "CASE " +
                            "   WHEN a.status IS NULL AND NOW() > DATE_ADD(CONCAT(s.date, ' ', s.end_time), INTERVAL 30 MINUTE) THEN 'absent' " +
                            "   WHEN a.status IS NULL THEN NULL " +
                            "   ELSE a.status " +
                            "END as calculated_status " +
                            "FROM classes c " +
                            "JOIN class_assignments ca ON c.class_id = ca.class_id " +
                            "JOIN sessions s ON c.class_id = s.class_id " +
                            "LEFT JOIN attendance a ON s.session_id = a.session_id AND a.user_id = ? " +
                            "WHERE ca.user_id = ? AND s.date BETWEEN ? AND ? " +
                            "ORDER BY s.date, s.start_time";

            conn = DatabaseHelper.getInstance().getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setLong(1, studentId);
            stmt.setLong(2, studentId);
            stmt.setDate(3, startDate);
            stmt.setDate(4, endDate);
            rs = stmt.executeQuery();

            List<Attendance> attendances = new ArrayList<>();
            while (rs.next()) {
                attendances.add(mapResultSetToAttendance(rs, studentId));
            }
            return attendances;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    // ============================
    // General class methods
    // ============================

    /**
     * Retrieves a single class by its ID
     */
    public CourseClass findClassById(Long classId) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            String sql = "SELECT * FROM classes WHERE class_id = ?";

            conn = DatabaseHelper.getInstance().getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setLong(1, classId);
            rs = stmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToClass(rs);
            }
            return null;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    // ============================
    // Helper methods
    // ============================

    /**
     * Maps a ResultSet row to a CourseClass object
     */
    private CourseClass mapResultSetToClass(ResultSet rs) throws SQLException {
        CourseClass classObj = new CourseClass();
        try {
            // Basic class info
            classObj.setClassId(rs.getLong("class_id"));
            classObj.setModuleCode(rs.getString("module_code"));
            classObj.setModuleName(rs.getString("module_name"));
            classObj.setSection(rs.getString("section"));
            classObj.setType(rs.getString("type"));
            classObj.setRoom(rs.getString("room"));
            classObj.setInstructorId(rs.getLong("instructor_id"));

            // Session info
            classObj.setSessionId(rs.getLong("session_id"));
            classObj.setSessionDate(rs.getDate("date"));
            classObj.setStartTime(rs.getTime("start_time"));
            classObj.setEndTime(rs.getTime("end_time"));
            classObj.setStatus(rs.getString("class_status"));
            classObj.setValidationCode(rs.getString("validation_code"));

            return classObj;
        } catch (SQLException e) {
            Log.e(TAG, "Error mapping result set to class", e);
            throw e;
        }
    }

    /**
     * Maps a ResultSet row to an Attendance object
     */
    private Attendance mapResultSetToAttendance(ResultSet rs, Long studentId) throws SQLException {
        Attendance attendance = new Attendance();

        // Attendance info
        attendance.setAttendanceId(rs.getLong("attendance_id"));
        attendance.setUserId(studentId);
        attendance.setSessionId(rs.getLong("session_id"));
        attendance.setTimestamp(rs.getTime("timestamp"));
        attendance.setStatus(rs.getString("calculated_status"));

        // Session/class details
        attendance.setSessionDate(rs.getDate("date"));
        attendance.setStartTime(rs.getTime("start_time"));
        attendance.setEndTime(rs.getTime("end_time"));
        attendance.setModuleCode(rs.getString("module_code"));
        attendance.setModuleName(rs.getString("module_name"));
        attendance.setSection(rs.getString("section"));
        attendance.setRoom(rs.getString("room"));
        attendance.setValidationCode(rs.getString("validation_code"));

        return attendance;
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