package com.biolock.database.dao;

import com.biolock.database.DatabaseHelper;
import com.biolock.model.Class;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ClassDao {
    private final DatabaseHelper dbHelper;

    public ClassDao() {
        this.dbHelper = DatabaseHelper.getInstance();
    }

    public long insert(Class classObj) throws SQLException {
        String sql = "INSERT INTO classes (module_code, module_name, section, type, instructor, room) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = dbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, classObj.getModuleCode());
            pstmt.setString(2, classObj.getModuleName());
            pstmt.setString(3, classObj.getSection());
            pstmt.setString(4, classObj.getType());
            pstmt.setLong(5, classObj.getInstructor());
            pstmt.setString(6, classObj.getRoom());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating class failed, no rows affected.");
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                } else {
                    throw new SQLException("Creating class failed, no ID obtained.");
                }
            }
        }
    }

    public List<Class> findByInstructor(long instructorId) throws SQLException {
        String sql = "SELECT * FROM classes WHERE instructor = ?";
        List<Class> classes = new ArrayList<>();

        try (Connection conn = dbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, instructorId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    classes.add(mapResultSetToClass(rs));
                }
            }
        }
        return classes;
    }

    private Class mapResultSetToClass(ResultSet rs) throws SQLException {
        Class classObj = new Class();
        classObj.setClassId(rs.getLong("class_id"));
        classObj.setModuleCode(rs.getString("module_code"));
        classObj.setModuleName(rs.getString("module_name"));
        classObj.setSection(rs.getString("section"));
        classObj.setType(rs.getString("type"));
        classObj.setInstructor(rs.getLong("instructor"));
        classObj.setRoom(rs.getString("room"));
        return classObj;
    }
}