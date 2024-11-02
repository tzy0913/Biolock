package com.biolock.model;

import java.sql.Timestamp;

public class FaceEmbedding {
    private long embeddingId;
    private long userId;
    private byte[] embeddingData;
    private double confidenceScore;
    private Timestamp createdAt;

    // Getters and setters
    public long getEmbeddingId() { return embeddingId; }
    public void setEmbeddingId(long embeddingId) { this.embeddingId = embeddingId; }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public byte[] getEmbeddingData() { return embeddingData; }
    public void setEmbeddingData(byte[] embeddingData) { this.embeddingData = embeddingData; }

    public double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(double confidenceScore) { this.confidenceScore = confidenceScore; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}