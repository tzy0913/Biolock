package com.biolock.database.dao;

import com.biolock.database.DatabaseHelper;
import com.biolock.model.ClassAssignment;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ClassAssignmentDao {
    private final DatabaseHelper dbHelper;

    public ClassAssignmentDao() {
        this.dbHelper = DatabaseHelper.getInstance();
    }

    public long insert(ClassAssignment assignment) throws SQLException {
        String sql = "INSERT INTO class_assignments (class_id, user_id, role) VALUES (?, ?, ?)";

        try (Connection conn = dbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setLong(1, assignment.getClassId());
            pstmt.setLong(2, assignment.getUserId());
            pstmt.setString(3, assignment.getRole());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating class assignment failed, no rows affected.");
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                } else {
                    throw new SQLException("Creating class assignment failed, no ID obtained.");
                }
            }
        }
    }

    public List<ClassAssignment> findByClassId(long classId) throws SQLException {
        String sql = "SELECT * FROM class_assignments WHERE class_id = ?";
        List<ClassAssignment> assignments = new ArrayList<>();

        try (Connection conn = dbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, classId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    assignments.add(mapResultSetToClassAssignment(rs));
                }
            }
        }
        return assignments;
    }

    public List<ClassAssignment> findByUserId(long userId) throws SQLException {
        String sql = "SELECT * FROM class_assignments WHERE user_id = ?";
        List<ClassAssignment> assignments = new ArrayList<>();

        try (Connection conn = dbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    assignments.add(mapResultSetToClassAssignment(rs));
                }
            }
        }
        return assignments;
    }

    public void delete(long assignmentId) throws SQLException {
        String sql = "DELETE FROM class_assignments WHERE assignment_id = ?";

        try (Connection conn = dbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, assignmentId);
            pstmt.executeUpdate();
        }
    }

    private ClassAssignment mapResultSetToClassAssignment(ResultSet rs) throws SQLException {
        ClassAssignment assignment = new ClassAssignment();
        assignment.setAssignmentId(rs.getLong("assignment_id"));
        assignment.setClassId(rs.getLong("class_id"));
        assignment.setUserId(rs.getLong("user_id"));
        assignment.setRole(rs.getString("role"));
        assignment.setCreatedAt(rs.getTimestamp("created_at"));
        return assignment;
    }
}