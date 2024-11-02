// FaceEnrollmentActivity.java
package com.biolock.ui.settings;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
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
import com.biolock.database.DatabaseHelper;
import com.biolock.model.FaceEmbedding;
import com.biolock.repository.FaceEmbeddingRepository;
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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FaceEnrollmentActivity extends AppCompatActivity {
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
            Toast.makeText(this, "Error initializing face recognition", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize ML Kit face detector
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
                setScanningOverlay();  // Keep the scanning overlay after passing liveness
                checkFaceQuality(face);  // Immediately check face quality
            } else {
                setNormalOverlay();  // Show normal overlay during liveness check
                captureButton.setEnabled(false);
            }
        } else {
            checkFaceQuality(face);
        }
    }

    private void checkFaceQuality(Face face) {
        float rotY = face.getHeadEulerAngleY();
        float rotZ = face.getHeadEulerAngleZ();

        if (Math.abs(rotY) < 10 && Math.abs(rotZ) < 10) {
            updateStatus("Good position - ready to capture");
            setScanningOverlay();  // Keep scanning overlay when in good position
            captureButton.setEnabled(true);
        } else {
            updateStatus("Please look straight at the camera");
            setNormalOverlay();  // Switch to normal overlay when position is not good
            captureButton.setEnabled(false);
        }
    }

    private void captureAndEnrollFace() {
        imageCapture.takePicture(cameraExecutor, new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy image) {
                try {
                    InputImage inputImage = InputImage.fromMediaImage(
                            image.getImage(),
                            image.getImageInfo().getRotationDegrees()
                    );

                    faceDetector.process(inputImage)
                            .addOnSuccessListener(faces -> {
                                if (!faces.isEmpty()) {
                                    Face detectedFace = faces.get(0);

                                    Bitmap faceBitmap = FacePreprocessor.extractFace(
                                            image.getImage(),
                                            detectedFace.getBoundingBox(),
                                            image.getImageInfo().getRotationDegrees()
                                    );

                                    if (faceBitmap != null) {
                                        float[] embedding = faceRecognition.generateEmbedding(faceBitmap);
                                        float[] normalizedEmbedding =
                                                FacePreprocessor.normalizeEmbedding(embedding);

                                        FaceEmbedding faceEmbedding = new FaceEmbedding();
                                        faceEmbedding.setUserId(sessionManager.getUserId());
                                        faceEmbedding.setEmbeddingData(
                                                FacePreprocessor.bitmapToByteArray(faceBitmap));
                                        faceEmbedding.setConfidenceScore(0.0);  // Initial confidence

                                        Result<Long> result =
                                                faceEmbeddingRepository.saveFaceEmbedding(faceEmbedding);

                                        if (result.isSuccess()) {
                                            runOnUiThread(() -> {
                                                Toast.makeText(FaceEnrollmentActivity.this,
                                                        "Face enrolled successfully",
                                                        Toast.LENGTH_SHORT).show();
                                                finish();
                                            });
                                        } else {
                                            runOnUiThread(() -> {
                                                Toast.makeText(FaceEnrollmentActivity.this,
                                                        "Enrollment failed: " + result.getError().getMessage(),
                                                        Toast.LENGTH_SHORT).show();
                                            });
                                        }
                                    }
                                }
                            })
                            .addOnFailureListener(e -> {
                                runOnUiThread(() -> {
                                    Toast.makeText(FaceEnrollmentActivity.this,
                                            "Face detection failed: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                });
                            })
                            .addOnCompleteListener(task -> {
                                image.close();
                            });

                } catch (Exception e) {
                    image.close();
                    runOnUiThread(() -> {
                        Toast.makeText(FaceEnrollmentActivity.this,
                                "Error processing image: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
                }
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                runOnUiThread(() -> {
                    Toast.makeText(FaceEnrollmentActivity.this,
                            "Capture failed: " + exception.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void updateStatus(String message) {
        runOnUiThread(() -> statusText.setText(message));
    }

    private void setNormalOverlay() {
        runOnUiThread(() ->
                overlayView.setBackgroundResource(R.drawable.normal_overlay));
    }

    private void setScanningOverlay() {
        runOnUiThread(() ->
                overlayView.setBackgroundResource(R.drawable.scanning_overlay));
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