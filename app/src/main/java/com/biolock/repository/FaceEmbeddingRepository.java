package com.biolock.repository;

import android.util.Log;
import com.biolock.database.DatabaseHelper;
import com.biolock.database.dao.FaceEmbeddingDao;
import com.biolock.model.FaceEmbedding;
import java.sql.SQLException;
import java.util.List;

public class FaceEmbeddingRepository {
    private static final String TAG = "FaceEmbeddingRepository";
    private final DatabaseHelper dbHelper;
    private final FaceEmbeddingDao faceEmbeddingDao;

    public FaceEmbeddingRepository() {
        this.dbHelper = DatabaseHelper.getInstance();
        this.faceEmbeddingDao = new FaceEmbeddingDao();
    }

    private void ensureInitialized() throws SQLException {
        if (!dbHelper.isInitialized()) {
            Log.d(TAG, "Initializing database for face embedding operations");
            dbHelper.initialize();
        }
    }

    public Result<Long> saveFaceEmbedding(FaceEmbedding embedding) {
        try {
            ensureInitialized();
            Log.d(TAG, "Saving face embedding for user: " + embedding.getUserId());

            long embeddingId = faceEmbeddingDao.insertOrUpdate(embedding);
            Log.d(TAG, "Successfully saved embedding with ID: " + embeddingId);

            return Result.success(embeddingId);
        } catch (SQLException e) {
            Log.e(TAG, "Database error while saving embedding", e);
            return Result.error(e);
        }
    }

    public Result<FaceEmbedding> getFaceEmbedding(long userId) {
        try {
            ensureInitialized();
            Log.d(TAG, "Retrieving face embedding for user: " + userId);

            List<FaceEmbedding> embeddings = faceEmbeddingDao.findByUserId(userId);
            if (!embeddings.isEmpty()) {
                return Result.success(embeddings.get(0));
            } else {
                Log.w(TAG, "No face embedding found for user: " + userId);
                return Result.error(new Exception("No face embedding found"));
            }
        } catch (SQLException e) {
            Log.e(TAG, "Database error while retrieving embedding", e);
            return Result.error(e);
        }
    }

    public Result<Boolean> hasFaceEmbedding(long userId) {
        try {
            ensureInitialized();
            List<FaceEmbedding> embeddings = faceEmbeddingDao.findByUserId(userId);
            return Result.success(!embeddings.isEmpty());
        } catch (SQLException e) {
            Log.e(TAG, "Error checking face embedding existence", e);
            return Result.error(e);
        }
    }

    public Result<Boolean> deleteFaceEmbedding(long userId) {
        try {
            ensureInitialized();
            Log.d(TAG, "Deleting face embedding for user: " + userId);
            faceEmbeddingDao.deleteByUserId(userId);
            return Result.success(true);
        } catch (SQLException e) {
            Log.e(TAG, "Database error while deleting embedding", e);
            return Result.error(e);
        }
    }

    public Result<Double> getEmbeddingConfidence(long userId) {
        try {
            ensureInitialized();
            List<FaceEmbedding> embeddings = faceEmbeddingDao.findByUserId(userId);
            if (!embeddings.isEmpty()) {
                double avgConfidence = embeddings.stream()
                        .mapToDouble(FaceEmbedding::getConfidenceScore)
                        .average()
                        .orElse(0.0);
                return Result.success(avgConfidence);
            } else {
                return Result.error(new Exception("No face embeddings found for user"));
            }
        } catch (SQLException e) {
            Log.e(TAG, "Error getting embedding confidence", e);
            return Result.error(e);
        }
    }
}