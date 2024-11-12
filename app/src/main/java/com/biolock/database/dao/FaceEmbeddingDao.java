/**
 * Data Access Object for managing face embedding data in the database.
 * Handles CRUD operations for user face recognition data including embeddings and confidence scores.
 */
package com.biolock.database.dao;

// Android Core
import android.util.Log;

// Biolock Components
import com.biolock.database.DatabaseHelper;

// Java SQL
import java.sql.*;

// Java Collections
import java.util.Arrays;

public class FaceEmbeddingDao {
    private static final String TAG = "FaceEmbeddingDao";
    private static final int IV_LENGTH = 16; // AES initialization vector length

    // ============================
    // Read Operations
    // ============================

    /**
     * Retrieves face embedding data for a specific user
     * @param userId ID of the user
     * @return byte array containing embedding data, or null if not found
     */
    public byte[] getEmbedding(Long userId) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            String sql = "SELECT AES_DECRYPT(embedding_data, get_encryption_key()) as decrypted_data " +
                    "FROM face_embeddings WHERE user_id = ?";

            conn = DatabaseHelper.getInstance().getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setLong(1, userId);
            rs = stmt.executeQuery();

            if (rs.next()) {
                byte[] bytes = rs.getBytes("decrypted_data");
                if (bytes != null) {
                    // If length is IV_LENGTH more than expected, remove the IV
                    if (bytes.length > IV_LENGTH) {
                        Log.d(TAG, "Removing IV from decrypted data");
                        return Arrays.copyOfRange(bytes, IV_LENGTH, bytes.length);
                    }
                    return bytes;
                }
            }
            return null;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    /**
     * Checks if a user has existing face enrollment
     * @param userId ID of the user
     * @return true if user has face enrollment, false otherwise
     */
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

    // ============================
    // Write Operations
    // ============================

    /**
     * Saves new face embedding data for a user
     * @param userId ID of the user
     * @param embeddingData byte array of face embedding data
     * @param confidenceScore confidence score of the face embedding
     */
    public void save(Long userId, byte[] embeddingData, double confidenceScore) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            if (embeddingData == null) {
                throw new SQLException("Embedding data cannot be null");
            }

            // Add padding bytes to handle IV
            byte[] paddedData = new byte[embeddingData.length + IV_LENGTH];
            System.arraycopy(embeddingData, 0, paddedData, IV_LENGTH, embeddingData.length);

            String sql = "INSERT INTO face_embeddings (user_id, embedding_data, confidence_score, created_at) " +
                    "VALUES (?, AES_ENCRYPT(?, get_encryption_key()), ?, NOW())";

            conn = DatabaseHelper.getInstance().getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setLong(1, userId);
            stmt.setBytes(2, paddedData);
            stmt.setDouble(3, confidenceScore);

            stmt.executeUpdate();
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    /**
     * Updates existing face embedding data for a user
     * @param userId ID of the user
     * @param embeddingData new face embedding data
     * @param confidenceScore new confidence score
     */
    public void update(Long userId, byte[] embeddingData, double confidenceScore) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            if (embeddingData == null) {
                throw new SQLException("Embedding data cannot be null");
            }

            // Add padding bytes to handle IV
            byte[] paddedData = new byte[embeddingData.length + IV_LENGTH];
            System.arraycopy(embeddingData, 0, paddedData, IV_LENGTH, embeddingData.length);

            String sql = "UPDATE face_embeddings " +
                    "SET embedding_data = AES_ENCRYPT(?, get_encryption_key()), " +
                    "confidence_score = ?, " +
                    "created_at = NOW() " +
                    "WHERE user_id = ?";

            conn = DatabaseHelper.getInstance().getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setBytes(1, paddedData);
            stmt.setDouble(2, confidenceScore);
            stmt.setLong(3, userId);

            stmt.executeUpdate();
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    /**
     * Deletes face embedding data for a user
     * @param userId ID of the user whose data should be deleted
     */
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

    // ============================
    // Helper Methods
    // ============================

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