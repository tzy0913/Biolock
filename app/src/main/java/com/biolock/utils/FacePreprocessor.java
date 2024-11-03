package com.biolock.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

/**
 * Handles camera image preprocessing for face detection.
 * Only concerned with image format conversion and face extraction.
 */
public class FacePreprocessor {
    private static final String TAG = "FacePreprocessor";

    /**
     * Extracts face region from camera image
     */
    public static Bitmap extractFace(Image image, Rect boundingBox, int rotation) {
        try {
            Log.d(TAG, "Starting face extraction...");
            Log.d(TAG, String.format("Image: %dx%d, Format: %d, Rotation: %d",
                    image.getWidth(), image.getHeight(), image.getFormat(), rotation));
            Log.d(TAG, "Bounding box: " + boundingBox.toString());

            Bitmap fullBitmap = imageToBitmap(image);
            if (fullBitmap == null) {
                Log.e(TAG, "Failed to create bitmap from image");
                return null;
            }

            // Rotate if needed
            if (rotation != 0) {
                Matrix matrix = new Matrix();
                matrix.postRotate(rotation);
                fullBitmap = Bitmap.createBitmap(fullBitmap, 0, 0,
                        fullBitmap.getWidth(), fullBitmap.getHeight(), matrix, true);
            }

            // Add padding around face
            int padding = Math.min(boundingBox.width(), boundingBox.height()) / 4;

            // Calculate crop dimensions
            int left = Math.max(0, boundingBox.left - padding);
            int top = Math.max(0, boundingBox.top - padding);
            int width = Math.min(fullBitmap.getWidth() - left, boundingBox.width() + 2 * padding);
            int height = Math.min(fullBitmap.getHeight() - top, boundingBox.height() + 2 * padding);

            if (width <= 0 || height <= 0) {
                Log.e(TAG, "Invalid face dimensions after padding");
                return null;
            }

            // Crop face region
            Log.d(TAG, String.format("Extracting face region: %d,%d - %dx%d",
                    left, top, width, height));
            Bitmap faceBitmap = Bitmap.createBitmap(fullBitmap, left, top, width, height);

            // Scale the cropped face region to 112x112
            return Bitmap.createScaledBitmap(faceBitmap, 112, 112, true);

        } catch (Exception e) {
            Log.e(TAG, "Error extracting face: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Converts camera Image to Bitmap
     */
    private static Bitmap imageToBitmap(Image image) {
        try {
            Image.Plane[] planes = image.getPlanes();
            int width = image.getWidth();
            int height = image.getHeight();

            if (planes.length == 1) {
                return handleSinglePlaneImage(planes[0], width, height);
            } else if (planes.length >= 3) {
                return handleYuvImage(image, planes, width, height);
            }

            Log.e(TAG, "Unsupported number of planes: " + planes.length);
            return null;

        } catch (Exception e) {
            Log.e(TAG, "Error converting image to bitmap: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Handles single plane image (JPEG/RGBA)
     */
    private static Bitmap handleSinglePlaneImage(Image.Plane plane, int width, int height) {
        ByteBuffer buffer = plane.getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);

        // Try direct decoding first
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        if (bitmap != null) {
            return bitmap;
        }

        // If direct decoding fails, handle RGBA data
        int[] colors = new int[width * height];
        int pixelStride = plane.getPixelStride();
        int rowStride = plane.getRowStride();
        int rowPadding = rowStride - pixelStride * width;

        buffer.rewind();
        int pos = 0;

        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                int pixel = 0;
                for (int i = 0; i < pixelStride; i++) {
                    if (buffer.hasRemaining()) {
                        pixel = (pixel << 8) | (buffer.get() & 0xFF);
                    }
                }
                colors[pos++] = pixel;
            }
            if (rowPadding > 0) {
                buffer.position(buffer.position() + rowPadding);
            }
        }

        Bitmap newBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        newBitmap.setPixels(colors, 0, width, 0, 0, width, height);
        return newBitmap;
    }

    /**
     * Handles YUV format image
     */
    private static Bitmap handleYuvImage(Image image, Image.Plane[] planes, int width, int height) {
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        // Calculate buffer sizes
        int ySize = yBuffer.remaining();
        int totalSize = width * height * 3 / 2;
        byte[] nv21 = new byte[totalSize];

        // Copy Y plane
        yBuffer.get(nv21, 0, ySize);

        // Copy U and V planes
        byte[] uBytes = new byte[uBuffer.remaining()];
        byte[] vBytes = new byte[vBuffer.remaining()];
        uBuffer.get(uBytes);
        vBuffer.get(vBytes);

        // Interleave U and V planes
        int uvPos = ySize;
        int uvSize = totalSize - ySize;
        for (int i = 0; i < uvSize / 2; i++) {
            nv21[uvPos++] = vBytes[i];
            nv21[uvPos++] = uBytes[i];
        }

        // Convert to JPEG
        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, width, height, null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, width, height), 100, out);

        // Convert to Bitmap
        byte[] imageBytes = out.toByteArray();
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    }
}