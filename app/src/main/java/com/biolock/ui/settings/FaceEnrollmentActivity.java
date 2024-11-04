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
import com.biolock.model.FaceEmbedding;
import com.biolock.model.FaceRecognitionLog;
import com.biolock.repository.FaceEmbeddingRepository;
import com.biolock.repository.FaceRecognitionLogRepository;
import com.biolock.repository.Result;
import com.biolock.utils.FacePreprocessor;
import com.biolock.utils.FaceRecognition;
import com.biolock.utils.LivenessDetector;
import com.biolock.utils.SessionManager;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.Arrays;
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
    private FaceRecognition faceRecognition;
    private LivenessDetector livenessDetector;
    private SessionManager sessionManager;
    private FaceEmbeddingRepository faceEmbeddingRepository;

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
        faceEmbeddingRepository = new FaceEmbeddingRepository();
        livenessDetector = new LivenessDetector();

        try {
            faceRecognition = new FaceRecognition(this);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing face recognition", e);
            Toast.makeText(this, "Error initializing face recognition", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

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

        Log.d(TAG, String.format("Face angles - Y: %.2f, Z: %.2f", rotY, rotZ));

        boolean angleYOk = Math.abs(rotY) < 15;
        boolean angleZOk = Math.abs(rotZ) < 15;

        Log.d(TAG, String.format("Angle checks - Y: %b, Z: %b", angleYOk, angleZOk));

        if (angleYOk && angleZOk) {
            Log.d(TAG, "Quality check passed - enabling capture button");
            updateStatus("Good position - ready to capture");
            setScanningOverlay();
            captureButton.setEnabled(true);
        } else {
            Log.d(TAG, "Quality check failed - keeping capture button disabled");
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

        FaceRecognitionLogRepository logsRepository = new FaceRecognitionLogRepository();
        long userId = sessionManager.getUserId();

        imageCapture.takePicture(ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageCapturedCallback() {
                    @Override
                    public void onCaptureSuccess(@NonNull ImageProxy imageProxy) {
                        try {
                            Image image = imageProxy.getImage();
                            if (image == null) {
                                logEnrollmentAttempt(logsRepository, userId, false, 0.0f);
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
                                            // Process in background thread
                                            new Thread(() -> {
                                                try {
                                                    // Extract face bitmap
                                                    Bitmap faceBitmap = FacePreprocessor.extractFace(
                                                            image,
                                                            detectedFace.getBoundingBox(),
                                                            rotation
                                                    );

                                                    if (faceBitmap == null) {
                                                        logEnrollmentAttempt(logsRepository, userId, false, 0.0f);
                                                        runOnUiThread(() -> handleError("Failed to process face image", progressDialog));
                                                        return;
                                                    }

                                                    // Generate embedding using FaceRecognition
                                                    float[] embedding = faceRecognition.generateEmbedding(faceBitmap);
                                                    byte[] embeddingBytes = faceRecognition.embeddingToBytes(embedding);

                                                    // Calculate confidence score
                                                    double confidenceScore = calculateConfidence(detectedFace);

                                                    // Create embedding object
                                                    FaceEmbedding faceEmbedding = new FaceEmbedding();
                                                    faceEmbedding.setUserId(userId);
                                                    faceEmbedding.setEmbeddingData(embeddingBytes);
                                                    faceEmbedding.setConfidenceScore(confidenceScore);

                                                    // Save to database
                                                    Result<Long> result = faceEmbeddingRepository.saveFaceEmbedding(faceEmbedding);

                                                    // Log the enrollment attempt
                                                    logEnrollmentAttempt(logsRepository, userId, result.isSuccess(), (float)confidenceScore);

                                                    runOnUiThread(() -> handleEnrollmentResult(result, progressDialog));

                                                } catch (Exception e) {
                                                    Log.e(TAG, "Error processing face", e);
                                                    logEnrollmentAttempt(logsRepository, userId, false, 0.0f);
                                                    runOnUiThread(() -> handleError("Error processing face: " + e.getMessage(), progressDialog));
                                                } finally {
                                                    imageProxy.close();
                                                }
                                            }).start();
                                        } else {
                                            logEnrollmentAttempt(logsRepository, userId, false, 0.0f);
                                            handleError("No face detected", progressDialog);
                                            imageProxy.close();
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        logEnrollmentAttempt(logsRepository, userId, false, 0.0f);
                                        handleError("Face detection failed: " + e.getMessage(), progressDialog);
                                        imageProxy.close();
                                    });

                        } catch (Exception e) {
                            Log.e(TAG, "Error in capture process", e);
                            logEnrollmentAttempt(logsRepository, userId, false, 0.0f);
                            handleError("Error capturing image: " + e.getMessage(), progressDialog);
                            imageProxy.close();
                        }
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException e) {
                        Log.e(TAG, "Image capture failed", e);
                        logEnrollmentAttempt(logsRepository, userId, false, 0.0f);
                        handleError("Failed to capture image: " + e.getMessage(), progressDialog);
                    }
                });
    }

    private void logEnrollmentAttempt(FaceRecognitionLogRepository logsRepository, long userId,
                                      boolean success, float similarity) {
        try {
            FaceRecognitionLog log = new FaceRecognitionLog();
            log.setUserId(userId);
            log.setSuccess(success);
            log.setSimilarity(similarity);
            log.setDeviceInfo(android.os.Build.MODEL);  // Device model
            log.setIpAddress("127.0.0.1");  // Local device
            log.setActionType(FaceRecognitionLog.ActionType.ENROLLMENT);

            Result<Long> result = logsRepository.logAttempt(log);
            if (!result.isSuccess()) {
                Log.e(TAG, "Failed to log enrollment attempt: " + result.getError().getMessage());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error logging enrollment attempt", e);
        }
    }

    private double calculateConfidence(Face face) {
        double baseConfidence = 0.4;
        double angleWeight = 0.3;
        double rotationWeight = 0.3;
        double maxAngle = 45.0;

        double angleConfidence = 1.0 - (Math.abs(face.getHeadEulerAngleY()) / maxAngle);
        double rotationConfidence = 1.0 - (Math.abs(face.getHeadEulerAngleZ()) / maxAngle);

        return Math.min((baseConfidence +
                (angleConfidence * angleWeight) +
                (rotationConfidence * rotationWeight)) * 100, 100.0);
    }

    private void handleEnrollmentResult(Result<Long> result, ProgressDialog progressDialog) {
        runOnUiThread(() -> {
            progressDialog.dismiss();
            if (result.isSuccess()) {
                Toast.makeText(this, "Face enrolled successfully", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                new AlertDialog.Builder(this)
                        .setTitle("Enrollment Failed")
                        .setMessage(result.getError().getMessage())
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
        if (faceRecognition != null) {
            faceRecognition.close();
        }
        cameraExecutor.shutdown();
    }
}