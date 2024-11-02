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

    public long insert(FaceEmbedding embedding) throws SQLException {
        String sql = "INSERT INTO face_embeddings (user_id, embedding_data, confidence_score) VALUES (?, ?, ?)";

        try (Connection conn = dbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

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
    }

    public List<FaceEmbedding> findByUserId(long userId) throws SQLException {
        String sql = "SELECT * FROM face_embeddings WHERE user_id = ? ORDER BY created_at DESC";
        List<FaceEmbedding> embeddings = new ArrayList<>();

        try (Connection conn = dbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    embeddings.add(mapResultSetToFaceEmbedding(rs));
                }
            }
        }
        return embeddings;
    }

    public FaceEmbedding findById(long embeddingId) throws SQLException {
        String sql = "SELECT * FROM face_embeddings WHERE embedding_id = ?";

        try (Connection conn = dbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, embeddingId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToFaceEmbedding(rs);
                }
                return null;
            }
        }
    }

    public List<FaceEmbedding> findAll() throws SQLException {
        String sql = "SELECT * FROM face_embeddings ORDER BY created_at DESC";
        List<FaceEmbedding> embeddings = new ArrayList<>();

        try (Connection conn = dbHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                embeddings.add(mapResultSetToFaceEmbedding(rs));
            }
        }
        return embeddings;
    }

    public void update(FaceEmbedding embedding) throws SQLException {
        String sql = "UPDATE face_embeddings SET embedding_data = ?, confidence_score = ? WHERE embedding_id = ?";

        try (Connection conn = dbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setBytes(1, embedding.getEmbeddingData());
            pstmt.setDouble(2, embedding.getConfidenceScore());
            pstmt.setLong(3, embedding.getEmbeddingId());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Updating face embedding failed, no embedding found with ID " +
                        embedding.getEmbeddingId());
            }
        }
    }

    public void delete(long embeddingId) throws SQLException {
        String sql = "DELETE FROM face_embeddings WHERE embedding_id = ?";

        try (Connection conn = dbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, embeddingId);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Deleting face embedding failed, no embedding found with ID " +
                        embeddingId);
            }
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