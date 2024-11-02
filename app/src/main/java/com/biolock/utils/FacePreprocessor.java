package com.biolock.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.media.Image;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

public class FacePreprocessor {
    public static Bitmap extractFace(Image image, Rect boundingBox, int rotation) {
        try {
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.capacity()];
            buffer.get(bytes);
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

            // Rotate if needed
            if (rotation != 0) {
                Matrix matrix = new Matrix();
                matrix.postRotate(rotation);
                bitmap = Bitmap.createBitmap(bitmap, 0, 0,
                        bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            }

            // Crop to face bounding box
            return Bitmap.createBitmap(bitmap,
                    boundingBox.left, boundingBox.top,
                    boundingBox.width(), boundingBox.height());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] bitmapToByteArray(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }

    public static float[] normalizeEmbedding(float[] embedding) {
        float sum = 0;
        for (float value : embedding) {
            sum += value * value;
        }
        float norm = (float) Math.sqrt(sum);

        float[] normalized = new float[embedding.length];
        for (int i = 0; i < embedding.length; i++) {
            normalized[i] = embedding[i] / norm;
        }
        return normalized;
    }
}