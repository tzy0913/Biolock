package com.biolock.utils;

import com.biolock.model.FaceEmbedding;
import com.biolock.repository.FaceEmbeddingRepository;
import com.biolock.repository.Result;

public class FaceAuthenticator {
    private static final float SIMILARITY_THRESHOLD = 0.7f;
    private final FaceEmbeddingRepository repository;
    private final LivenessDetector livenessDetector;
    private final FaceRecognition faceRecognition;

    public FaceAuthenticator(FaceEmbeddingRepository repository, FaceRecognition faceRecognition) {
        this.repository = repository;
        this.faceRecognition = faceRecognition;
        this.livenessDetector = new LivenessDetector();
    }

    public boolean authenticate(long userId, float[] newEmbedding) {
        Result<FaceEmbedding> storedEmbeddingResult = repository.getFaceEmbedding(userId);

        if (storedEmbeddingResult.isSuccess()) {
            FaceEmbedding storedEmbedding = storedEmbeddingResult.getData();
            Result<Float> comparisonResult = repository.compareEmbeddings(
                    bytesFromFloatArray(newEmbedding),
                    storedEmbedding.getEmbeddingData()
            );

            if (comparisonResult.isSuccess()) {
                return comparisonResult.getData() >= SIMILARITY_THRESHOLD;
            }
        }
        return false;
    }

    public LivenessDetector getLivenessDetector() {
        return livenessDetector;
    }

    private byte[] bytesFromFloatArray(float[] floats) {
        java.nio.ByteBuffer buffer = java.nio.ByteBuffer.allocate(4 * floats.length);
        for (float value : floats) {
            buffer.putFloat(value);
        }
        return buffer.array();
    }
}