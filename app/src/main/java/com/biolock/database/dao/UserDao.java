package com.biolock.database.dao;

import android.util.Log;

import com.biolock.database.DatabaseHelper;
import com.biolock.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDao {
    private static final String TAG = "UserDAO";

    public User findById(Long userId) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            String sql = "SELECT * FROM users WHERE user_id = ?";

            conn = DatabaseHelper.getInstance().getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setLong(1, userId);
            rs = stmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToUser(rs);
            }
            return null;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public User findByEmail(String email) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            String sql = "SELECT user_id, name, email, role, password, created_at " +
                    "FROM users WHERE email = ?";

            conn = DatabaseHelper.getInstance().getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, email);
            rs = stmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToUser(rs);
            }
            return null;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public Long save(User user) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            // Use MySQL's PASSWORD() function for encryption
            String sql = "INSERT INTO users (name, email, role, password, created_at) " +
                    "VALUES (?, ?, ?, PASSWORD(?), NOW())";

            conn = DatabaseHelper.getInstance().getConnection();
            stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, user.getName());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getRole());
            stmt.setString(4, user.getPassword());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating user failed, no rows affected.");
            }

            rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getLong(1);
            } else {
                throw new SQLException("Creating user failed, no ID obtained.");
            }
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public boolean verifyPassword(String email, String password) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            // Compare with encrypted password
            String sql = "SELECT COUNT(*) FROM users " +
                    "WHERE email = ? AND password = PASSWORD(?)";

            conn = DatabaseHelper.getInstance().getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, email);
            stmt.setString(2, password);
            rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public void update(User user) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            String sql = "UPDATE users SET name = ?, email = ?, role = ? " +
                    "WHERE user_id = ?";

            conn = DatabaseHelper.getInstance().getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, user.getName());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getRole());
            stmt.setLong(4, user.getUserId());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Updating user failed, no rows affected.");
            }
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    public boolean credentialsExist(String email) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
            conn = DatabaseHelper.getInstance().getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, email);
            rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getLong("user_id"));
        user.setName(rs.getString("name"));
        user.setEmail(rs.getString("email"));
        user.setRole(rs.getString("role"));
        user.setPassword(rs.getString("password"));
        user.setCreatedAt(rs.getTimestamp("created_at"));
        return user;
    }

    private void closeResources(Connection conn, Statement stmt, ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                Log.e(TAG, "Error closing ResultSet", e);
            }
        }
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                Log.e(TAG, "Error closing Statement", e);
            }
        }
        if (conn != null) {
            DatabaseHelper.getInstance().releaseConnection(conn);
        }
    }
}