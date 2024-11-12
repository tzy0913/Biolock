/**
 * Utility class for preprocessing camera images for face detection.
 * Handles image format conversion, face extraction, and confidence calculation.
 * Supports various image formats including YUV and single-plane formats.
 */
package com.biolock.utils;

// Android Core Components
import android.graphics.*;
import android.media.Image;
import android.util.Log;

// Google ML Kit
import com.google.mlkit.vision.face.Face;

// Java IO & NIO
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

public class FacePreprocessor {
    private static final String TAG = "FacePreprocessor";

    // ============================
    // Face Extraction Operations
    // ============================

    /**
     * Extracts and preprocesses face region from camera image
     * Includes rotation, padding, and scaling to standard size
     *
     * @param image Source camera image
     * @param boundingBox Face bounding box from detector
     * @param rotation Required rotation in degrees
     * @return Processed face bitmap (112x112) or null if extraction fails
     */
    public static Bitmap extractFace(Image image, Rect boundingBox, int rotation) {
        try {
            Log.d(TAG, "Starting face extraction...");
            Log.d(TAG, String.format("Image: %dx%d, Format: %d, Rotation: %d",
                    image.getWidth(), image.getHeight(), image.getFormat(), rotation));
            Log.d(TAG, "Bounding box: " + boundingBox.toString());

            // Convert full image to bitmap
            Bitmap fullBitmap = imageToBitmap(image);
            if (fullBitmap == null) {
                Log.e(TAG, "Failed to create bitmap from image");
                return null;
            }

            // Apply rotation if needed
            if (rotation != 0) {
                Matrix matrix = new Matrix();
                matrix.postRotate(rotation);
                fullBitmap = Bitmap.createBitmap(fullBitmap, 0, 0,
                        fullBitmap.getWidth(), fullBitmap.getHeight(), matrix, true);
            }

            // Calculate padding and crop dimensions
            int padding = Math.min(boundingBox.width(), boundingBox.height()) / 4;
            int left = Math.max(0, boundingBox.left - padding);
            int top = Math.max(0, boundingBox.top - padding);
            int width = Math.min(fullBitmap.getWidth() - left, boundingBox.width() + 2 * padding);
            int height = Math.min(fullBitmap.getHeight() - top, boundingBox.height() + 2 * padding);

            if (width <= 0 || height <= 0) {
                Log.e(TAG, "Invalid face dimensions after padding");
                return null;
            }

            // Extract and scale face region
            Log.d(TAG, String.format("Extracting face region: %d,%d - %dx%d",
                    left, top, width, height));
            Bitmap faceBitmap = Bitmap.createBitmap(fullBitmap, left, top, width, height);
            return Bitmap.createScaledBitmap(faceBitmap, 112, 112, true);

        } catch (Exception e) {
            Log.e(TAG, "Error extracting face: " + e.getMessage(), e);
            return null;
        }
    }

    // ============================
    // Image Format Conversion
    // ============================

    /**
     * Converts camera Image to Bitmap, handling different formats
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
     * Processes single-plane images (JPEG/RGBA format)
     */
    private static Bitmap handleSinglePlaneImage(Image.Plane plane, int width, int height) {
        ByteBuffer buffer = plane.getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);

        // Try direct JPEG decoding
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        if (bitmap != null) {
            return bitmap;
        }

        // Handle RGBA data if JPEG decoding fails
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
     * Processes YUV format images
     */
    private static Bitmap handleYuvImage(Image image, Image.Plane[] planes, int width, int height) {
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        // Calculate and allocate buffers
        int ySize = yBuffer.remaining();
        int totalSize = width * height * 3 / 2;
        byte[] nv21 = new byte[totalSize];

        // Copy Y plane
        yBuffer.get(nv21, 0, ySize);

        // Process UV planes
        byte[] uBytes = new byte[uBuffer.remaining()];
        byte[] vBytes = new byte[vBuffer.remaining()];
        uBuffer.get(uBytes);
        vBuffer.get(vBytes);

        // Interleave U and V data
        int uvPos = ySize;
        int uvSize = totalSize - ySize;
        for (int i = 0; i < uvSize / 2; i++) {
            nv21[uvPos++] = vBytes[i];
            nv21[uvPos++] = uBytes[i];
        }

        // Convert to JPEG then Bitmap
        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, width, height, null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, width, height), 100, out);

        byte[] imageBytes = out.toByteArray();
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    }

    // ============================
    // Confidence Calculation
    // ============================

    /**
     * Calculates confidence score for detected face based on pose
     * Considers head angle and rotation
     *
     * @param face Detected face from MLKit
     * @return Confidence score between 0 and 1
     */
    public static double calculateFaceConfidence(Face face) {
        double baseConfidence = 0.4;  // Base confidence value
        double angleWeight = 0.3;     // Weight for head angle
        double rotationWeight = 0.3;  // Weight for head rotation
        double maxAngle = 45.0;       // Maximum acceptable angle

        // Calculate angle-based confidence
        double angleConfidence = 1.0 - (Math.abs(face.getHeadEulerAngleY()) / maxAngle);
        double rotationConfidence = 1.0 - (Math.abs(face.getHeadEulerAngleZ()) / maxAngle);

        // Compute weighted score
        double confidence = baseConfidence +
                (angleConfidence * angleWeight) +
                (rotationConfidence * rotationWeight);

        return Math.min(confidence, 1.0);
    }
}