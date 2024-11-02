package com.biolock.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.TensorOperator;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;

public class FaceRecognition {
    private static final String MODEL_PATH = "mobile_face_net.tflite";
    private static final int INPUT_SIZE = 112;
    private static final float IMAGE_MEAN = 127.5f;
    private static final float IMAGE_STD = 128.0f;
    private static final int EMBEDDING_SIZE = 512;

    private final Interpreter interpreter;
    private final ImageProcessor imageProcessor;
    private final TensorImage inputImageBuffer;
    private final float[][] embeddingBuffer;

    public FaceRecognition(Context context) throws IOException {
        MappedByteBuffer modelBuffer = FileUtil.loadMappedFile(context, MODEL_PATH);
        Interpreter.Options interpreterOptions = new Interpreter.Options();
        interpreter = new Interpreter(modelBuffer, interpreterOptions);

        // Initialize image processor
        imageProcessor = new ImageProcessor.Builder()
                .add(new ResizeOp(INPUT_SIZE, INPUT_SIZE, ResizeOp.ResizeMethod.BILINEAR))
                .add(new NormalizeOp(IMAGE_MEAN, IMAGE_STD))
                .build();

        // Initialize input and output buffers
        inputImageBuffer = new TensorImage(DataType.FLOAT32);
        embeddingBuffer = new float[1][EMBEDDING_SIZE];
    }

    public float[] generateEmbedding(Bitmap face) {
        if (face == null) {
            throw new IllegalArgumentException("Face bitmap cannot be null");
        }

        // Load and preprocess the image
        inputImageBuffer.load(face);
        TensorImage processedImage = imageProcessor.process(inputImageBuffer);

        // Generate embedding
        interpreter.run(processedImage.getBuffer(), embeddingBuffer);

        return embeddingBuffer[0];
    }

    public float calculateSimilarity(float[] embedding1, float[] embedding2) {
        if (embedding1 == null || embedding2 == null ||
                embedding1.length != EMBEDDING_SIZE || embedding2.length != EMBEDDING_SIZE) {
            throw new IllegalArgumentException("Invalid embeddings");
        }

        float sum = 0;
        for (int i = 0; i < EMBEDDING_SIZE; i++) {
            sum += (embedding1[i] - embedding2[i]) * (embedding1[i] - embedding2[i]);
        }
        float distance = (float) Math.sqrt(sum);
        return 1.0f / (1.0f + distance);
    }

    public void close() {
        if (interpreter != null) {
            interpreter.close();
        }
    }
}