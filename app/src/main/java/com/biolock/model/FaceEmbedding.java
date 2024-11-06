package com.biolock.model;

import java.util.Date;

public class FaceEmbedding {
    private Long embeddingId;
    private Long userId;
    private byte[] embeddingData;
    private double confidenceScore;
    private Date createdAt;

    public FaceEmbedding() {}

    public FaceEmbedding(Long userId, byte[] embeddingData, double confidenceScore) {
        this.userId = userId;
        this.embeddingData = embeddingData;
        this.confidenceScore = confidenceScore;
        this.createdAt = new Date();
    }

    // Getters and Setters
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

    public double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}