package com.biolock.database.dao;

import com.biolock.database.DatabaseHelper;
import com.biolock.model.User;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDao {
    private final DatabaseHelper dbHelper;

    public UserDao() {
        this.dbHelper = DatabaseHelper.getInstance();
    }

    public long insert(User user, String password) throws SQLException {
        Connection conn = null;
        try {
            conn = dbHelper.getConnection();
            String sql = "INSERT INTO users (name, email, role, password) VALUES (?, ?, ?, PASSWORD(?))";

            try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

                pstmt.setString(1, user.getName());
                pstmt.setString(2, user.getEmail());
                pstmt.setString(3, user.getRole());
                pstmt.setString(4, password);

                int affectedRows = pstmt.executeUpdate();
                if (affectedRows == 0) {
                    throw new SQLException("Creating user failed, no rows affected.");
                }

                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return generatedKeys.getLong(1);
                    } else {
                        throw new SQLException("Creating user failed, no ID obtained.");
                    }
                }
            }
        } finally {
            dbHelper.releaseConnection(conn);
        }
    }

    public User validateCredentials(String email, String password) throws SQLException {
        Connection conn = null;
        try {
            conn = dbHelper.getConnection();
            String sql = "SELECT * FROM users WHERE email = ? AND password = PASSWORD(?)";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setString(1, email);
                pstmt.setString(2, password);

                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return mapResultSetToUser(rs);
                    }
                    return null;
                }
            }
        } finally {
            dbHelper.releaseConnection(conn);
        }
    }

    public User findById(long userId) throws SQLException {
        Connection conn = null;
        try {
            conn = dbHelper.getConnection();
            String sql = "SELECT * FROM users WHERE user_id = ?";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setLong(1, userId);

                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return mapResultSetToUser(rs);
                    }
                    return null;
                }
            }
        } finally {
            dbHelper.releaseConnection(conn);
        }
    }

    public User findByEmail(String email) throws SQLException {
        Connection conn = null;
        try {
            conn = dbHelper.getConnection();
            String sql = "SELECT * FROM users WHERE email = ?";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setString(1, email);

                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return mapResultSetToUser(rs);
                    }
                    return null;
                }
            }
        } finally {
            dbHelper.releaseConnection(conn);
        }

    }

    public List<User> findAll() throws SQLException {
        Connection conn = null;
        try {
            conn = dbHelper.getConnection();
            String sql = "SELECT * FROM users ORDER BY name";
            List<User> users = new ArrayList<>();

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                while (rs.next()) {
                    users.add(mapResultSetToUser(rs));
                }
            }
            return users;
        } finally {
            dbHelper.releaseConnection(conn);
        }
    }

    public List<User> findByRole(String role) throws SQLException {
        Connection conn = null;
        try {
            conn = dbHelper.getConnection();
            String sql = "SELECT * FROM users WHERE role = ?";
            List<User> users = new ArrayList<>();

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setString(1, role);

                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        users.add(mapResultSetToUser(rs));
                    }
                }
            }
            return users;
        } finally {
            dbHelper.releaseConnection(conn);
        }
    }

    public void update(User user) throws SQLException {
        Connection conn = null;
        try {
            conn = dbHelper.getConnection();
            String sql = "UPDATE users SET name = ?, email = ?, role = ? WHERE user_id = ?";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setString(1, user.getName());
                pstmt.setString(2, user.getEmail());
                pstmt.setString(3, user.getRole());
                pstmt.setLong(4, user.getUserId());

                int affectedRows = pstmt.executeUpdate();
                if (affectedRows == 0) {
                    throw new SQLException("Updating user failed, no user found.");
                }
            }
        } finally {
            dbHelper.releaseConnection(conn);
        }
    }

    public void delete(long userId) throws SQLException {
        Connection conn = null;
        try {
            conn = dbHelper.getConnection();
            String sql = "DELETE FROM users WHERE user_id = ?";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setLong(1, userId);

                int affectedRows = pstmt.executeUpdate();
                if (affectedRows == 0) {
                    throw new SQLException("Deleting user failed, no user found.");
                }
            }
        } finally {
            dbHelper.releaseConnection(conn);
        }
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getLong("user_id"));
        user.setName(rs.getString("name"));
        user.setEmail(rs.getString("email"));
        user.setRole(rs.getString("role"));
        user.setCreatedAt(rs.getTimestamp("created_at"));
        return user;
    }
}