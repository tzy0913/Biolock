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

    public int insert(Class classData) throws SQLException {
        Connection conn = null;
        try {
            conn = dbHelper.getConnection();
            String sql = "INSERT INTO classes (module_code, module_name, section, type, instructor, room) VALUES (?, ?, ?, ?, ?, ?)";

            try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, classData.getModuleCode());
                pstmt.setString(2, classData.getModuleName());
                pstmt.setString(3, classData.getSection());
                pstmt.setString(4, classData.getType());
                pstmt.setString(5, classData.getInstructor());
                pstmt.setString(6, classData.getRoom());

                int affectedRows = pstmt.executeUpdate();
                if (affectedRows == 0) {
                    throw new SQLException("Creating class failed, no rows affected.");
                }

                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return generatedKeys.getInt(1);
                    } else {
                        throw new SQLException("Creating class failed, no ID obtained.");
                    }
                }
            }
        } finally {
            dbHelper.releaseConnection(conn);
        }
    }

    public Class getById(int classId) throws SQLException {
        Connection conn = null;
        try {
            conn = dbHelper.getConnection();
            String sql = "SELECT * FROM classes WHERE class_id = ?";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, classId);

                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return mapResultSetToClass(rs);
                    }
                    return null;
                }
            }
        } finally {
            dbHelper.releaseConnection(conn);
        }
    }

    private Class mapResultSetToClass(ResultSet rs) throws SQLException {
        Class classData = new Class();
        classData.setClassId(rs.getInt("class_id"));
        classData.setModuleCode(rs.getString("module_code"));
        classData.setModuleName(rs.getString("module_name"));
        classData.setSection(rs.getString("section"));
        classData.setType(rs.getString("type"));
        classData.setInstructor(rs.getString("instructor"));
        classData.setRoom(rs.getString("room"));
        return classData;
    }
}