package com.biolock.utils;

import android.util.Log;
import com.biolock.model.FaceEmbedding;
import com.biolock.repository.FaceEmbeddingRepository;
import com.biolock.repository.Result;

public class FaceAuthenticator {
    private static final String TAG = "FaceAuthenticator";
    private static final float SIMILARITY_THRESHOLD = 0.85f;

    private final FaceEmbeddingRepository repository;
    private final LivenessDetector livenessDetector;
    private final FaceRecognition faceRecognition;
    private float lastSimilarityScore;

    public FaceAuthenticator(FaceEmbeddingRepository repository, FaceRecognition faceRecognition) {
        this.repository = repository;
        this.faceRecognition = faceRecognition;
        this.livenessDetector = new LivenessDetector();
    }

    public boolean authenticate(long userId, float[] newEmbedding) {
        if (newEmbedding == null) {
            Log.e(TAG, "New embedding is null");
            return false;
        }

        // Get stored embedding
        Result<FaceEmbedding> storedEmbeddingResult = repository.getFaceEmbedding(userId);
        if (!storedEmbeddingResult.isSuccess()) {
            Log.e(TAG, "Failed to get stored embedding");
            return false;
        }

        FaceEmbedding storedEmbedding = storedEmbeddingResult.getData();
        float[] storedEmbeddingArray = faceRecognition.bytesToEmbedding(storedEmbedding.getEmbeddingData());

        if (storedEmbeddingArray == null) {
            Log.e(TAG, "Failed to convert stored embedding");
            return false;
        }

        // Calculate similarity and store score
        lastSimilarityScore = faceRecognition.calculateSimilarity(newEmbedding, storedEmbeddingArray);
        Log.d(TAG, "Similarity Score: " + lastSimilarityScore + " (Threshold: " + SIMILARITY_THRESHOLD + ")");

        return lastSimilarityScore >= SIMILARITY_THRESHOLD;
    }

    public float getLastSimilarityScore() {
        return lastSimilarityScore;
    }

    public LivenessDetector getLivenessDetector() {
        return livenessDetector;
    }
}