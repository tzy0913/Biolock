package com.biolock.ui.settings;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.biolock.R;
import com.biolock.repository.FaceAuthenticationRepository;
import com.biolock.repository.Result;
import com.biolock.utils.FacePreprocessor;
import com.biolock.utils.LivenessDetector;
import com.biolock.utils.SessionManager;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FaceEnrollmentActivity extends AppCompatActivity {
    private static final String TAG = "FaceEnrollmentActivity";
    private static final int PERMISSION_REQUEST_CODE = 10;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.CAMERA};

    // UI Components
    private PreviewView previewView;
    private TextView statusText;
    private Button captureButton;
    private View overlayView;

    // Camera Components
    private FaceDetector faceDetector;
    private ImageCapture imageCapture;
    private final ExecutorService cameraExecutor = Executors.newSingleThreadExecutor();

    // Utility Components
    private LivenessDetector livenessDetector;
    private SessionManager sessionManager;
    private FaceAuthenticationRepository faceAuthenticationRepository;

    // State
    private boolean livenessCheckPassed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_enrollment);

        initializeComponents();

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSION_REQUEST_CODE);
        }
    }

    private void initializeComponents() {
        // Initialize UI components
        previewView = findViewById(R.id.previewView);
        statusText = findViewById(R.id.statusText);
        captureButton = findViewById(R.id.captureButton);
        overlayView = findViewById(R.id.overlayView);

        // Initialize utils and repositories
        sessionManager = new SessionManager(this);
        faceAuthenticationRepository = new FaceAuthenticationRepository(this);
        livenessDetector = new LivenessDetector();

        // Initialize ML Kit face detector with high accuracy settings
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build();
        faceDetector = FaceDetection.getClient(options);

        // Set up button listeners
        captureButton.setEnabled(false);
        captureButton.setOnClickListener(v -> captureAndEnrollFace());
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera", e);
                Toast.makeText(this, "Error starting camera: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases(ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(cameraExecutor, image -> {
            InputImage inputImage = InputImage.fromMediaImage(
                    image.getImage(),
                    image.getImageInfo().getRotationDegrees()
            );

            faceDetector.process(inputImage)
                    .addOnSuccessListener(faces -> {
                        if (faces.isEmpty()) {
                            updateStatus("No face detected");
                            setNormalOverlay();
                            captureButton.setEnabled(false);
                        } else if (faces.size() > 1) {
                            updateStatus("Multiple faces detected");
                            setNormalOverlay();
                            captureButton.setEnabled(false);
                        } else {
                            Face face = faces.get(0);
                            processLivenessDetection(face);
                        }
                        image.close();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Face detection failed", e);
                        updateStatus("Detection failed");
                        setNormalOverlay();
                        image.close();
                    });
        });

        try {
            cameraProvider.unbindAll();
            cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture,
                    imageAnalysis
            );
            preview.setSurfaceProvider(previewView.getSurfaceProvider());
        } catch (Exception e) {
            Log.e(TAG, "Error binding camera uses cases", e);
            Toast.makeText(this, "Error binding camera uses cases",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void processLivenessDetection(Face face) {
        if (!livenessCheckPassed) {
            LivenessDetector.LivenessResult result = livenessDetector.processFrame(face);
            updateStatus(result.message);

            if (result.state == LivenessDetector.LivenessState.COMPLETED) {
                livenessCheckPassed = true;
                setScanningOverlay();
                checkFaceQuality(face);
            } else {
                setNormalOverlay();
                captureButton.setEnabled(false);
            }
        } else {
            checkFaceQuality(face);
        }
    }

    private void checkFaceQuality(Face face) {
        float rotY = face.getHeadEulerAngleY();
        float rotZ = face.getHeadEulerAngleZ();

        boolean angleYOk = Math.abs(rotY) < 15;
        boolean angleZOk = Math.abs(rotZ) < 15;

        if (angleYOk && angleZOk) {
            updateStatus("Good position - ready to capture");
            setScanningOverlay();
            captureButton.setEnabled(true);
        } else {
            StringBuilder guidance = new StringBuilder("Please adjust:");
            if (!angleYOk) {
                guidance.append(" face the camera directly");
            }
            if (!angleZOk) {
                if (guidance.length() > 14) guidance.append(" and");
                guidance.append(" keep head straight");
            }
            updateStatus(guidance.toString());
            setNormalOverlay();
            captureButton.setEnabled(false);
        }
    }

    private void captureAndEnrollFace() {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Processing face enrollment...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        Long userId = sessionManager.getUserId();

        imageCapture.takePicture(ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageCapturedCallback() {
                    @Override
                    public void onCaptureSuccess(@NonNull ImageProxy imageProxy) {
                        try {
                            Image image = imageProxy.getImage();
                            if (image == null) {
                                handleError("Failed to capture image", progressDialog);
                                imageProxy.close();
                                return;
                            }

                            final int rotation = imageProxy.getImageInfo().getRotationDegrees();
                            InputImage inputImage = InputImage.fromMediaImage(image, rotation);

                            faceDetector.process(inputImage)
                                    .addOnSuccessListener(faces -> {
                                        if (!faces.isEmpty()) {
                                            Face detectedFace = faces.get(0);
                                            new Thread(() -> {
                                                try {
                                                    Bitmap faceBitmap = FacePreprocessor.extractFace(
                                                            image,
                                                            detectedFace.getBoundingBox(),
                                                            rotation
                                                    );

                                                    if (faceBitmap == null) {
                                                        runOnUiThread(() ->
                                                                handleError("Failed to process face image", progressDialog));
                                                        return;
                                                    }

                                                    // Calculate confidence score here
                                                    double confidenceScore = FacePreprocessor.calculateFaceConfidence(detectedFace);
                                                    Log.d(TAG, "Face confidence score: " + confidenceScore);

                                                    // Only proceed if confidence is above threshold
                                                    if (confidenceScore < 0.4) { // Threshold now between 0-1
                                                        runOnUiThread(() ->
                                                                handleError("Face quality too low. Please try again with better lighting and positioning",
                                                                        progressDialog));
                                                        return;
                                                    }

                                                    Result<Boolean> result =
                                                            faceAuthenticationRepository.enrollFace(userId, faceBitmap, confidenceScore);

                                                    runOnUiThread(() -> handleEnrollmentResult(result, progressDialog));

                                                } catch (Exception e) {
                                                    Log.e(TAG, "Error processing face", e);
                                                    runOnUiThread(() ->
                                                            handleError("Error processing face: " + e.getMessage(),
                                                                    progressDialog));
                                                } finally {
                                                    imageProxy.close();
                                                }
                                            }).start();
                                        } else {
                                            handleError("No face detected", progressDialog);
                                            imageProxy.close();
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        handleError("Face detection failed: " + e.getMessage(), progressDialog);
                                        imageProxy.close();
                                    });

                        } catch (Exception e) {
                            Log.e(TAG, "Error in capture process", e);
                            handleError("Error capturing image: " + e.getMessage(), progressDialog);
                            imageProxy.close();
                        }
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException e) {
                        Log.e(TAG, "Image capture failed", e);
                        handleError("Failed to capture image: " + e.getMessage(), progressDialog);
                    }
                });
    }

    private void handleEnrollmentResult(Result<Boolean> result, ProgressDialog progressDialog) {
        runOnUiThread(() -> {
            progressDialog.dismiss();
            if (result.isSuccess() && result.getData()) {
                Toast.makeText(this, "Face enrolled successfully", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                new AlertDialog.Builder(this)
                        .setTitle("Enrollment Failed")
                        .setMessage(result.getError())
                        .setPositiveButton("OK", null)
                        .show();
            }
        });
    }

    private void handleError(String message, ProgressDialog progressDialog) {
        runOnUiThread(() -> {
            progressDialog.dismiss();
            new AlertDialog.Builder(this)
                    .setTitle("Error")
                    .setMessage(message)
                    .setPositiveButton("OK", null)
                    .show();
        });
    }

    private void updateStatus(String message) {
        runOnUiThread(() -> statusText.setText(message));
    }

    private void setNormalOverlay() {
        runOnUiThread(() -> overlayView.setBackgroundResource(R.drawable.normal_overlay));
    }

    private void setScanningOverlay() {
        runOnUiThread(() -> overlayView.setBackgroundResource(R.drawable.scanning_overlay));
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Camera permissions are required",
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        livenessDetector.reset();
        livenessCheckPassed = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}