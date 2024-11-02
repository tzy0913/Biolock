// FaceEmbeddingRepository.java
package com.biolock.repository;

import com.biolock.database.dao.FaceEmbeddingDao;
import com.biolock.model.FaceEmbedding;
import java.sql.SQLException;
import java.util.List;

public class FaceEmbeddingRepository {
    private final FaceEmbeddingDao faceEmbeddingDao;

    public FaceEmbeddingRepository() {
        this.faceEmbeddingDao = new FaceEmbeddingDao();
    }

    public Result<Long> saveFaceEmbedding(FaceEmbedding embedding) {
        try {
            // Check if user already has embeddings
            List<FaceEmbedding> existingEmbeddings =
                    faceEmbeddingDao.findByUserId(embedding.getUserId());

            if (!existingEmbeddings.isEmpty()) {
                // Remove old embeddings if exist (keep only latest)
                for (FaceEmbedding existing : existingEmbeddings) {
                    faceEmbeddingDao.delete(existing.getEmbeddingId());
                }
            }

            // Save new embedding
            long embeddingId = faceEmbeddingDao.insert(embedding);
            return Result.success(embeddingId);
        } catch (SQLException e) {
            return Result.error(e);
        }
    }

    public Result<FaceEmbedding> getFaceEmbedding(long userId) {
        try {
            List<FaceEmbedding> embeddings = faceEmbeddingDao.findByUserId(userId);
            if (!embeddings.isEmpty()) {
                // Return the most recent embedding
                return Result.success(embeddings.get(0));
            } else {
                return Result.error(new Exception("No face embedding found for user"));
            }
        } catch (SQLException e) {
            return Result.error(e);
        }
    }

    public Result<Boolean> hasFaceEmbedding(long userId) {
        try {
            List<FaceEmbedding> embeddings = faceEmbeddingDao.findByUserId(userId);
            return Result.success(!embeddings.isEmpty());
        } catch (SQLException e) {
            return Result.error(e);
        }
    }

    public Result<Void> deleteFaceEmbedding(long userId) {
        try {
            List<FaceEmbedding> embeddings = faceEmbeddingDao.findByUserId(userId);
            for (FaceEmbedding embedding : embeddings) {
                faceEmbeddingDao.delete(embedding.getEmbeddingId());
            }
            return Result.success(null);
        } catch (SQLException e) {
            return Result.error(e);
        }
    }

    public Result<Float> compareEmbeddings(byte[] embedding1, byte[] embedding2) {
        try {
            // Convert byte arrays back to float arrays
            float[] floatEmbedding1 = bytesToFloatArray(embedding1);
            float[] floatEmbedding2 = bytesToFloatArray(embedding2);

            // Calculate cosine similarity
            float similarity = calculateCosineSimilarity(floatEmbedding1, floatEmbedding2);
            return Result.success(similarity);
        } catch (Exception e) {
            return Result.error(e);
        }
    }

    private float calculateCosineSimilarity(float[] embedding1, float[] embedding2) {
        if (embedding1.length != embedding2.length) {
            throw new IllegalArgumentException("Embedding dimensions do not match");
        }

        float dotProduct = 0.0f;
        float norm1 = 0.0f;
        float norm2 = 0.0f;

        for (int i = 0; i < embedding1.length; i++) {
            dotProduct += embedding1[i] * embedding2[i];
            norm1 += embedding1[i] * embedding1[i];
            norm2 += embedding2[i] * embedding2[i];
        }

        norm1 = (float) Math.sqrt(norm1);
        norm2 = (float) Math.sqrt(norm2);

        return dotProduct / (norm1 * norm2);
    }

    private float[] bytesToFloatArray(byte[] bytes) {
        float[] floats = new float[bytes.length / 4];
        java.nio.ByteBuffer buffer = java.nio.ByteBuffer.wrap(bytes);
        for (int i = 0; i < floats.length; i++) {
            floats[i] = buffer.getFloat();
        }
        return floats;
    }

    public Result<List<FaceEmbedding>> getAllEmbeddings() {
        try {
            List<FaceEmbedding> embeddings = faceEmbeddingDao.findAll();
            return Result.success(embeddings);
        } catch (SQLException e) {
            return Result.error(e);
        }
    }

    public Result<Double> getEmbeddingConfidence(long userId) {
        try {
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
            return Result.error(e);
        }
    }

    public Result<Void> updateEmbeddingConfidence(long embeddingId, double newConfidence) {
        try {
            FaceEmbedding embedding = faceEmbeddingDao.findById(embeddingId);
            if (embedding != null) {
                embedding.setConfidenceScore(newConfidence);
                faceEmbeddingDao.update(embedding);
                return Result.success(null);
            } else {
                return Result.error(new Exception("Embedding not found"));
            }
        } catch (SQLException e) {
            return Result.error(e);
        }
    }
}