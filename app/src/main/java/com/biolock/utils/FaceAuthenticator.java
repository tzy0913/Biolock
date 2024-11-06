package com.biolock.utils;

import android.graphics.Bitmap;
import android.util.Log;

public class FaceAuthenticator {
    private static final String TAG = "FaceAuthenticator";
    private static final float SIMILARITY_THRESHOLD = 0.90f;

    private final FaceRecognition faceRecognition;
    private final LivenessDetector livenessDetector;
    private float lastSimilarityScore;

    public FaceAuthenticator(FaceRecognition faceRecognition) {
        this.faceRecognition = faceRecognition;
        this.livenessDetector = new LivenessDetector();
    }

    // Compare two face embeddings directly
    public boolean matchFace(float[] storedEmbedding, float[] newEmbedding) {
        if (newEmbedding == null || storedEmbedding == null) {
            Log.e(TAG, "One or both embeddings are null");
            return false;
        }

        // Calculate similarity and store score
        lastSimilarityScore = faceRecognition.calculateSimilarity(newEmbedding, storedEmbedding);
        Log.d(TAG, "Similarity Score: " + lastSimilarityScore + " (Threshold: " + SIMILARITY_THRESHOLD + ")");

        return lastSimilarityScore >= SIMILARITY_THRESHOLD;
    }

    // Convert between bytes and embeddings
    public float[] bytesToEmbedding(byte[] embeddingData) {
        return faceRecognition.bytesToEmbedding(embeddingData);
    }

    public byte[] embeddingToBytes(float[] embedding) {
        return faceRecognition.embeddingToBytes(embedding);
    }

    // Generate embedding from bitmap
    public float[] generateEmbedding(Bitmap face) {
        return faceRecognition.generateEmbedding(face);
    }

    public float getLastSimilarityScore() {
        return lastSimilarityScore;
    }

    public LivenessDetector getLivenessDetector() {
        return livenessDetector;
    }
}