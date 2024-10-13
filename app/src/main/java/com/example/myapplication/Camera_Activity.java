package com.example.myapplication;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.EditText;

import com.example.myapplication.R;

import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

public class Camera_Activity extends CameraActivity {

    private static final String TAG = "OpenCV";
    private static final int CAMERA_INDEX = CameraBridgeViewBase.CAMERA_ID_FRONT;
    private static final double BLURRY_THRESHOLD = 40.0; // Adjust this threshold as needed

    private CameraBridgeViewBase cameraBridgeViewBase;
    private CascadeClassifier cascadeClassifier;
    private ProgressBar progressBar;
    private Button retakeButton;
    private LinearLayout userInputForm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        initializeOpenCV();
        initializeCameraView();
        loadCascade();
        setupRetakeButton();
    }

    private void initializeOpenCV() {
        if (!OpenCVLoader.initDebug()) {
            Log.e(TAG, "OpenCV initialization failed.");
        }
    }

    private void initializeCameraView() {
        cameraBridgeViewBase = findViewById(R.id.cameraView);
        progressBar = findViewById(R.id.progress_bar);
        userInputForm = findViewById(R.id.user_input_form);
        retakeButton = findViewById(R.id.retake_button);

        cameraBridgeViewBase.setCameraIndex(CAMERA_INDEX);
        cameraBridgeViewBase.setCvCameraViewListener(new CameraBridgeViewBase.CvCameraViewListener2() {
            @Override
            public void onCameraViewStarted(int width, int height) {}

            @Override
            public void onCameraViewStopped() {}

            @Override
            public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
                return processFrame(inputFrame);
            }
        });
        cameraBridgeViewBase.enableView();
    }

    private Mat processFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat rgb = inputFrame.rgba();
        Mat gray = inputFrame.gray();
        MatOfRect rects = new MatOfRect();

        Core.flip(rgb, rgb, 1); // Flip horizontally for front camera

        // Check if the frame is blurry first
        if (isImageBlurry(rgbToBitmap(rgb))) {
            Log.d(TAG, "Captured image is blurry. Not detecting faces.");
            return rgb; // Return the frame without processing
        }

        // Proceed with face detection if the image is not blurry
        detectFaces(gray, rects);
        drawFaceRectangles(rgb, rects);

        if (rects.toArray().length > 0) {
            Log.d(TAG, "Face detected");
            runOnUiThread(() -> {
                progressBar.setVisibility(View.VISIBLE);
                Bitmap faceBitmap = extractFace(rgb, rects.toArray()[0]);
                if (faceBitmap != null) {
                    progressBar.setVisibility(View.GONE);
                    cameraBridgeViewBase.disableView(); // Stop the camera preview
                    showInputDialog(faceBitmap); // Show the face dialog
                }
            });
        }

        return rgb;
    }

    // Helper method to convert Mat to Bitmap
    private Bitmap rgbToBitmap(Mat rgb) {
        Bitmap bmp = Bitmap.createBitmap(rgb.cols(), rgb.rows(), Bitmap.Config.ARGB_8888);
        org.opencv.android.Utils.matToBitmap(rgb, bmp);
        return bmp;
    }



    private void detectFaces(Mat gray, MatOfRect rects) {
        if (cascadeClassifier != null) {
            cascadeClassifier.detectMultiScale(gray, rects, 1.15, 5);
        }
    }

    private void drawFaceRectangles(Mat rgb, MatOfRect rects) {
        for (Rect rect : rects.toArray()) {
            // Use the original rectangle's coordinates
            int x = rect.x;
            int y = rect.y;
            int width = rect.width;
            int height = rect.height;

            // Create a new rectangle with original coordinates
            Rect adjustedRect = new Rect(x, y, width, height);

            // Draw the original rectangle on the image
            Imgproc.rectangle(rgb, adjustedRect.tl(), adjustedRect.br(), new Scalar(0, 255, 0), 3);
        }
    }


    private Bitmap extractFace(Mat frame, Rect rect) {
        Mat face = new Mat(frame, rect);
        Bitmap bmp = Bitmap.createBitmap(face.cols(), face.rows(), Bitmap.Config.ARGB_8888);
        org.opencv.android.Utils.matToBitmap(face, bmp);
        return bmp;
    }

    private void showInputDialog(Bitmap faceBitmap) {
        Log.d(TAG, "Showing input dialog");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.input_dialog, null);
        ImageView faceImageView = dialogView.findViewById(R.id.face_image);
        faceImageView.setImageBitmap(faceBitmap);
        EditText usernameInput = dialogView.findViewById(R.id.username);

        builder.setView(dialogView)
                .setPositiveButton("Submit", (dialog, which) -> {
                    String username = usernameInput.getText().toString();
                    Log.d(TAG, "Username: " + username);
                    finish(); // Close this activity and return to home
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    dialog.dismiss();
                    cameraBridgeViewBase.enableView(); // Re-enable the camera
                })
                .show();
    }


    private boolean isImageBlurry(Bitmap bitmap) {
        // Convert the Bitmap to Mat for processing
        Mat mat = new Mat();
        org.opencv.android.Utils.bitmapToMat(bitmap, mat);

        // Convert to grayscale
        Mat gray = new Mat();
        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_BGR2GRAY);

        // Compute the Laplacian
        Mat laplacian = new Mat();
        Imgproc.Laplacian(gray, laplacian, CvType.CV_64F);

        // Calculate the mean and variance
        Scalar mean = Core.mean(laplacian); // Mean of the Laplacian
        Mat laplacianSquared = new Mat();
        Core.multiply(laplacian, laplacian, laplacianSquared); // Element-wise square

        Scalar meanSquared = Core.mean(laplacianSquared); // Mean of squared Laplacian
        double variance = meanSquared.val[0] - mean.val[0] * mean.val[0]; // Variance formula

        return variance < BLURRY_THRESHOLD; // Check if variance is below the threshold
    }



    private void showAlert(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> {
                    dialog.dismiss();
                    cameraBridgeViewBase.enableView(); // Re-enable the camera for retake
                })
                .show();
    }

    private void loadCascade() {
        try (InputStream inputStream = getResources().openRawResource(R.raw.lbpcascade_frontalface);
             FileOutputStream fileOutputStream = new FileOutputStream(
                     getDir("cascade", MODE_PRIVATE) + "/haarcascade_frontalface_alt.xml")) {

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, bytesRead);
            }

            cascadeClassifier = new CascadeClassifier(getDir("cascade", MODE_PRIVATE)
                    + "/haarcascade_frontalface_alt.xml");
            if (cascadeClassifier.empty()) {
                cascadeClassifier = null;
                Log.e(TAG, "Failed to load cascade classifier.");
            }
        } catch (IOException e) {
            Log.e(TAG, "Error loading cascade file: " + e.getMessage());
        }
    }

    private void setupRetakeButton() {
        retakeButton.setOnClickListener(v -> {
            userInputForm.setVisibility(View.GONE);
            retakeButton.setVisibility(View.GONE);
            cameraBridgeViewBase.enableView();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        cameraBridgeViewBase.enableView();
    }

    @Override
    protected void onPause() {
        super.onPause();
        cameraBridgeViewBase.disableView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraBridgeViewBase.disableView();
    }

    @Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(cameraBridgeViewBase);
    }
}
