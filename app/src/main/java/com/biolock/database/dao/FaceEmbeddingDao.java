package com.biolock.database.dao;

import android.util.Log;

import com.biolock.database.DatabaseHelper;
import com.biolock.model.FaceEmbedding;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FaceEmbeddingDao {
    private static final String TAG = "FaceEmbeddingDao";

    public byte[] getEmbedding(Long userId) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            String sql = "SELECT embedding_data FROM face_embeddings WHERE user_id = ?";

            conn = DatabaseHelper.getInstance().getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setLong(1, userId);
            rs = stmt.executeQuery();

            if (rs.next()) {
                Blob blob = rs.getBlob("embedding_data");
                return blob.getBytes(1, (int) blob.length());
            }
            return null;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public void save(Long userId, byte[] embeddingData, double confidenceScore) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            String sql = "INSERT INTO face_embeddings (user_id, embedding_data, confidence_score, created_at) " +
                    "VALUES (?, ?, ?, NOW())";

            conn = DatabaseHelper.getInstance().getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setLong(1, userId);
            stmt.setBytes(2, embeddingData);
            stmt.setDouble(3, confidenceScore);

            stmt.executeUpdate();
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    public void update(Long userId, byte[] embeddingData, double confidenceScore) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            String sql = "UPDATE face_embeddings SET embedding_data = ?, confidence_score = ?, " +
                    "created_at = NOW() WHERE user_id = ?";

            conn = DatabaseHelper.getInstance().getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setBytes(1, embeddingData);
            stmt.setDouble(2, confidenceScore);
            stmt.setLong(3, userId);

            stmt.executeUpdate();
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    public boolean hasEnrollment(Long userId) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            String sql = "SELECT COUNT(*) FROM face_embeddings WHERE user_id = ?";

            conn = DatabaseHelper.getInstance().getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setLong(1, userId);
            rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public void delete(Long userId) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            String sql = "DELETE FROM face_embeddings WHERE user_id = ?";

            conn = DatabaseHelper.getInstance().getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setLong(1, userId);

            stmt.executeUpdate();
        } finally {
            closeResources(conn, stmt, null);
        }
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