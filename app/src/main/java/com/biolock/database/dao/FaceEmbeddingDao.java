// FaceEmbeddingDao.java
package com.biolock.database.dao;

import com.biolock.database.DatabaseHelper;
import com.biolock.model.FaceEmbedding;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FaceEmbeddingDao {
    private final DatabaseHelper dbHelper;

    public FaceEmbeddingDao() {
        this.dbHelper = DatabaseHelper.getInstance();
    }

    public long insertOrUpdate(FaceEmbedding embedding) throws SQLException {
        Connection conn = null;
        try {
            conn = dbHelper.getConnection();
            String sql = "INSERT INTO face_embeddings (user_id, embedding_data, confidence_score) " +
                    "VALUES (?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE " +
                    "embedding_data = VALUES(embedding_data), " +
                    "confidence_score = VALUES(confidence_score)";

            try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

                pstmt.setLong(1, embedding.getUserId());
                pstmt.setBytes(2, embedding.getEmbeddingData());
                pstmt.setDouble(3, embedding.getConfidenceScore());

                int affectedRows = pstmt.executeUpdate();
                if (affectedRows == 0) {
                    throw new SQLException("Creating face embedding failed, no rows affected.");
                }

                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return generatedKeys.getLong(1);
                    } else {
                        throw new SQLException("Creating face embedding failed, no ID obtained.");
                    }
                }
            }
        } finally {
            dbHelper.releaseConnection(conn);
        }
    }

    public List<FaceEmbedding> findByUserId(long userId) throws SQLException {
        Connection conn = null;
        try {
            conn = dbHelper.getConnection();
            String sql = "SELECT * FROM face_embeddings WHERE user_id = ? ORDER BY created_at DESC";
            List<FaceEmbedding> embeddings = new ArrayList<>();

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setLong(1, userId);

                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        embeddings.add(mapResultSetToFaceEmbedding(rs));
                    }
                }
            }
            return embeddings;
        } finally {
            dbHelper.releaseConnection(conn);
        }
    }

    public FaceEmbedding findById(long embeddingId) throws SQLException {
        Connection conn = null;
        try {
            conn = dbHelper.getConnection();
            String sql = "SELECT * FROM face_embeddings WHERE embedding_id = ?";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setLong(1, embeddingId);

                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return mapResultSetToFaceEmbedding(rs);
                    }
                    return null;
                }
            }
        } finally {
            dbHelper.releaseConnection(conn);
        }
    }

    public List<FaceEmbedding> findAll() throws SQLException {
        Connection conn = null;
        try {
            conn = dbHelper.getConnection();
            String sql = "SELECT * FROM face_embeddings ORDER BY created_at DESC";
            List<FaceEmbedding> embeddings = new ArrayList<>();

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                while (rs.next()) {
                    embeddings.add(mapResultSetToFaceEmbedding(rs));
                }
            }
            return embeddings;
        } finally {
            dbHelper.releaseConnection(conn);
        }
    }

    public void delete(long embeddingId) throws SQLException {
        Connection conn = null;
        try {
            conn = dbHelper.getConnection();
            String sql = "DELETE FROM face_embeddings WHERE embedding_id = ?";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setLong(1, embeddingId);

                int affectedRows = pstmt.executeUpdate();
                if (affectedRows == 0) {
                    throw new SQLException("Deleting face embedding failed, no embedding found with ID " +
                            embeddingId);
                }
            }
        } finally {
            dbHelper.releaseConnection(conn);
        }
    }

    public void deleteByUserId(long userId) throws SQLException {
        Connection conn = null;
        try {
            conn = dbHelper.getConnection();
            String sql = "DELETE FROM face_embeddings WHERE user_id = ?";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setLong(1, userId);
                pstmt.executeUpdate();
            }
        } finally {
            dbHelper.releaseConnection(conn);
        }
    }

    private FaceEmbedding mapResultSetToFaceEmbedding(ResultSet rs) throws SQLException {
        FaceEmbedding embedding = new FaceEmbedding();
        embedding.setEmbeddingId(rs.getLong("embedding_id"));
        embedding.setUserId(rs.getLong("user_id"));
        embedding.setEmbeddingData(rs.getBytes("embedding_data"));
        embedding.setConfidenceScore(rs.getDouble("confidence_score"));
        embedding.setCreatedAt(rs.getTimestamp("created_at"));
        return embedding;
    }
}