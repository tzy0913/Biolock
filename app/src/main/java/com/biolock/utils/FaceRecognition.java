package com.biolock.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;

public class FaceRecognition {
    private static final String TAG = "FaceRecognition";
    private static final String MODEL_PATH = "mobile_face_net.tflite";
    private static final int INPUT_SIZE = 112;
    private static final float IMAGE_MEAN = 127.5f;
    private static final float IMAGE_STD = 128.0f;
    private static final int EMBEDDING_SIZE = 192;

    private final Interpreter interpreter;
    private final ImageProcessor imageProcessor;
    private final TensorImage inputImageBuffer;
    private final float[][] embeddingBuffer;

    public FaceRecognition(Context context) throws IOException {
        MappedByteBuffer modelBuffer = FileUtil.loadMappedFile(context, MODEL_PATH);
        Interpreter.Options interpreterOptions = new Interpreter.Options();
        interpreter = new Interpreter(modelBuffer, interpreterOptions);

        imageProcessor = new ImageProcessor.Builder()
                .add(new ResizeOp(INPUT_SIZE, INPUT_SIZE, ResizeOp.ResizeMethod.BILINEAR))
                .add(new NormalizeOp(IMAGE_MEAN, IMAGE_STD))
                .build();

        inputImageBuffer = new TensorImage(DataType.FLOAT32);
        embeddingBuffer = new float[1][EMBEDDING_SIZE];
    }

    public float[] generateEmbedding(Bitmap face) {
        // Add this scaling before processing
        Bitmap scaledFace = Bitmap.createScaledBitmap(face, INPUT_SIZE, INPUT_SIZE, true);
        inputImageBuffer.load(scaledFace);
        TensorImage processedImage = imageProcessor.process(inputImageBuffer);
        interpreter.run(processedImage.getBuffer(), embeddingBuffer);
        return embeddingBuffer[0];
    }

    private float[] l2Normalize(float[] embedding) {
        float squareSum = 0.0f;
        for (float val : embedding) {
            squareSum += val * val;
        }

        float l2Norm = (float) Math.sqrt(squareSum);
        if (l2Norm > 0) {
            for (int i = 0; i < embedding.length; i++) {
                embedding[i] /= l2Norm;
            }
        }
        return embedding;
    }

    public byte[] embeddingToBytes(float[] embedding) {
        if (embedding == null || embedding.length != EMBEDDING_SIZE) {
            Log.e(TAG, "Invalid embedding array");
            return null;
        }

        ByteBuffer buffer = ByteBuffer.allocate(4 * EMBEDDING_SIZE)
                .order(ByteOrder.LITTLE_ENDIAN);
        for (float value : embedding) {
            buffer.putFloat(value);
        }

        return buffer.array();
    }

    public float[] bytesToEmbedding(byte[] bytes) {
        if (bytes == null || bytes.length != 4 * EMBEDDING_SIZE) {
            Log.e(TAG, "Invalid byte array");
            return null;
        }

        float[] embedding = new float[EMBEDDING_SIZE];
        ByteBuffer.wrap(bytes)
                .order(ByteOrder.LITTLE_ENDIAN)
                .asFloatBuffer()
                .get(embedding);

        return embedding;
    }

    public float calculateSimilarity(float[] embedding1, float[] embedding2) {
        if (embedding1 == null || embedding2 == null ||
                embedding1.length != embedding2.length) {
            return 0.0f;
        }

        // Normalize embeddings first
        normalizeEmbedding(embedding1);
        normalizeEmbedding(embedding2);

        // Calculate cosine similarity
        float dotProduct = 0.0f;
        for (int i = 0; i < embedding1.length; i++) {
            dotProduct += embedding1[i] * embedding2[i];
        }

        // Adjust similarity curve to be more lenient in middle range
        float rawSimilarity = (dotProduct + 1.0f) / 2.0f;  // Convert from [-1,1] to [0,1]
        return (float) Math.pow(rawSimilarity, 0.7);  // Adjust curve to be more lenient
    }

    private void normalizeEmbedding(float[] embedding) {
        // Calculate magnitude
        float sumSquares = 0.0f;
        for (float v : embedding) {
            if (!Float.isNaN(v) && !Float.isInfinite(v)) {
                sumSquares += v * v;
            }
        }

        float magnitude = (float) Math.sqrt(sumSquares);

        // Normalize if magnitude is significant
        if (magnitude > 1e-6f) {
            for (int i = 0; i < embedding.length; i++) {
                embedding[i] /= magnitude;
            }
        }
    }

    private void logEmbeddingValues(String label, float[] embedding) {
        if (embedding == null) return;

        StringBuilder sb = new StringBuilder(label + " first 5 values: ");
        for (int i = 0; i < Math.min(5, embedding.length); i++) {
            sb.append(String.format("%.4f", embedding[i]));
            if (i < 4) sb.append(", ");
        }
        Log.d(TAG, sb.toString());
    }

    public void close() {
        if (interpreter != null) {
            interpreter.close();
        }
    }
}