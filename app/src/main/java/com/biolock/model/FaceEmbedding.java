/**
 * Model class representing a face embedding.
 * Stores the numerical representation of facial features along with metadata
 * for use in facial recognition and verification.
 */
package com.biolock.model;

// Java Utilities
import java.util.Date;

public class FaceEmbedding {
    // ============================
    // Core Embedding Data
    // ============================
    private Long embeddingId;
    private Long userId;
    private byte[] embeddingData;    // Raw numerical representation of facial features

    // ============================
    // Quality Metrics
    // ============================
    private double confidenceScore;   // Confidence level of the embedding quality

    // ============================
    // Metadata
    // ============================
    private Date createdAt;          // Timestamp of embedding creation

    // ============================
    // Constructors
    // ============================

    /**
     * Default constructor
     */
    public FaceEmbedding() {}

    /**
     * Constructor for creating a new face embedding
     * @param userId ID of the user this embedding belongs to
     * @param embeddingData Raw byte array of facial feature data
     * @param confidenceScore Confidence score of the embedding quality
     */
    public FaceEmbedding(Long userId, byte[] embeddingData, double confidenceScore) {
        this.userId = userId;
        this.embeddingData = embeddingData;
        this.confidenceScore = confidenceScore;
        this.createdAt = new Date();
    }

    // ============================
    // Core Data Getters/Setters
    // ============================

    public Long getEmbeddingId() {
        return embeddingId;
    }

    public void setEmbeddingId(Long embeddingId) {
        this.embeddingId = embeddingId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public byte[] getEmbeddingData() {
        return embeddingData;
    }

    public void setEmbeddingData(byte[] embeddingData) {
        this.embeddingData = embeddingData;
    }

    // ============================
    // Quality Metrics Getters/Setters
    // ============================

    public double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    // ============================
    // Metadata Getters/Setters
    // ============================

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}