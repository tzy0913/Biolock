package com.biolock.ui.login;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.biolock.R;
import com.biolock.model.User;
import com.biolock.repository.FaceEmbeddingRepository;
import com.biolock.repository.Result;
import com.biolock.repository.UserRepository;
import com.biolock.ui.dashboard.DashboardActivity;
import com.biolock.utils.FaceAuthenticator;
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

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private static final int PERMISSION_REQUEST_CODE = 10;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.CAMERA};

    // UI Components
    private LinearLayout loginChoiceLayout;
    private LinearLayout manualLoginLayout;
    private ConstraintLayout faceLoginLayout;
    private PreviewView previewView;
    private TextView statusText;
    private EditText editTextEmail;
    private EditText editTextPassword;
    private View overlayView;

    // Camera Components
    private FaceDetector faceDetector;
    private final ExecutorService cameraExecutor = Executors.newSingleThreadExecutor();

    // Utility Components
    private UserRepository userRepository;
    private FaceEmbeddingRepository faceRepository;
    private FaceRecognition faceRecognition;
    private FaceAuthenticator faceAuthenticator;
    private LivenessDetector livenessDetector;
    private SessionManager sessionManager;

    // State
    private boolean livenessCheckPassed = false;
    private Long lastUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        sessionManager = new SessionManager(this);
        // Get the lastUserId from sharedPreferences, even if not currently logged in
        lastUserId = sessionManager.getUserId();
        if (lastUserId == -1) { // If no last user found
            lastUserId = null;
        }

        if (sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, DashboardActivity.class));
            finish();
            return;
        }

        initializeComponents();
    }

    private void initializeComponents() {
        initializeViews();

        ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage("Initializing...");
        progress.setCancelable(false);
        progress.show();

        new Thread(() -> {
            try {
                userRepository = new UserRepository();
                faceRepository = new FaceEmbeddingRepository();
                faceRecognition = new FaceRecognition(this);
                faceAuthenticator = new FaceAuthenticator(faceRepository, faceRecognition);
                livenessDetector = new LivenessDetector();

                FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                        .build();
                faceDetector = FaceDetection.getClient(options);

                runOnUiThread(() -> {
                    progress.dismiss();
                    setupClickListeners();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progress.dismiss();
                    Toast.makeText(this, "Initialization failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        }).start();
    }

    private void initializeViews() {
        loginChoiceLayout = findViewById(R.id.loginChoiceLayout);
        manualLoginLayout = findViewById(R.id.manualLoginLayout);
        faceLoginLayout = findViewById(R.id.faceLoginLayout);
        previewView = findViewById(R.id.previewView);
        statusText = findViewById(R.id.statusTextView);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        overlayView = findViewById(R.id.overlayView);
    }

    private void setupClickListeners() {
        findViewById(R.id.buttonFaceLogin).setOnClickListener(v -> startFaceLogin());

        findViewById(R.id.buttonManualLogin).setOnClickListener(v -> {
            loginChoiceLayout.setVisibility(View.GONE);
            manualLoginLayout.setVisibility(View.VISIBLE);
        });

        findViewById(R.id.buttonLogin).setOnClickListener(v -> handleManualLogin());

        findViewById(R.id.buttonRegister).setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));

        // Back button from manual login
        findViewById(R.id.buttonBackToChoice).setOnClickListener(v -> {
            manualLoginLayout.setVisibility(View.GONE);
            loginChoiceLayout.setVisibility(View.VISIBLE);
        });

        // Back button from face login
        findViewById(R.id.buttonBackToChoiceFromFace).setOnClickListener(v -> {
            faceLoginLayout.setVisibility(View.GONE);
            loginChoiceLayout.setVisibility(View.VISIBLE);
            // Additional cleanup for face login
            if (faceRecognition != null) {
                faceRecognition.close();
            }
            livenessCheckPassed = false;
            livenessDetector.reset();
        });
    }

    private void startFaceLogin() {
        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSION_REQUEST_CODE);
            return;
        }

        if (lastUserId == null || lastUserId == -1) {
            Toast.makeText(this, "Please login manually first to enable face login", Toast.LENGTH_LONG).show();
            loginChoiceLayout.setVisibility(View.GONE);
            manualLoginLayout.setVisibility(View.VISIBLE);
            return;
        }

        loginChoiceLayout.setVisibility(View.GONE);
        faceLoginLayout.setVisibility(View.VISIBLE);
        startCamera();
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

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(cameraExecutor, image -> {
            Image mediaImage = image.getImage();
            if (mediaImage != null) {
                InputImage inputImage = InputImage.fromMediaImage(
                        mediaImage,
                        image.getImageInfo().getRotationDegrees()
                );

                // Don't close the image here - moved to after processing
                faceDetector.process(inputImage)
                        .addOnSuccessListener(faces -> {
                            if (faces.isEmpty()) {
                                updateStatus("No face detected");
                                setNormalOverlay();
                                image.close(); // Close if no face detected
                            } else if (faces.size() > 1) {
                                updateStatus("Multiple faces detected");
                                setNormalOverlay();
                                image.close(); // Close if multiple faces
                            } else {
                                Face face = faces.get(0);
                                processLivenessAndAuthentication(face, mediaImage,
                                        image.getImageInfo().getRotationDegrees());
                                image.close(); // Close after processing
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Face detection failed", e);
                            updateStatus("Detection failed");
                            setNormalOverlay();
                            image.close(); // Close on failure
                        });
            } else {
                image.close();
            }
        });

        try {
            cameraProvider.unbindAll();
            cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageAnalysis
            );
            preview.setSurfaceProvider(previewView.getSurfaceProvider());
        } catch (Exception e) {
            Log.e(TAG, "Error binding camera uses cases", e);
            Toast.makeText(this, "Error binding camera uses cases",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void processLivenessAndAuthentication(Face face, Image image, int rotation) {
        if (!livenessCheckPassed) {
            LivenessDetector.LivenessResult result = livenessDetector.processFrame(face);
            updateStatus(result.message);

            if (result.state == LivenessDetector.LivenessState.COMPLETED) {
                livenessCheckPassed = true;
                setScanningOverlay();
                // Extract face and authenticate
                Bitmap faceBitmap = FacePreprocessor.extractFace(image, face.getBoundingBox(), rotation);
                if (faceBitmap != null) {
                    authenticateUser(faceBitmap);
                }
            } else {
                setNormalOverlay();
            }
        }
    }

    private void authenticateUser(Bitmap faceBitmap) {
        if (lastUserId == null || lastUserId == -1) {
            updateStatus("Please login manually first to enable face login");
            return;
        }

        new Thread(() -> {
            try {
                // Generate embedding directly using FaceRecognition
                float[] newEmbedding = faceRecognition.generateEmbedding(faceBitmap);

                // FaceAuthenticator now handles all authentication
                if (faceAuthenticator.authenticate(lastUserId, newEmbedding)) {
                    Result<User> userResult = userRepository.getUserById(lastUserId);
                    if (userResult.isSuccess()) {
                        handleSuccessfulLogin(userResult.getData());
                        return;
                    }
                }
                updateStatus("Face authentication failed");

            } catch (Exception e) {
                Log.e(TAG, "Authentication error", e);
                updateStatus("Authentication error: " + e.getMessage());
            }
        }).start();
    }

    private void handleManualLogin() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage("Logging in...");
        progress.setCancelable(false);
        progress.show();

        new Thread(() -> {
            try {
                Result<User> result = userRepository.login(email, password);
                runOnUiThread(() -> {
                    progress.dismiss();
                    if (result.isSuccess()) {
                        handleSuccessfulLogin(result.getData());
                    } else {
                        Toast.makeText(LoginActivity.this, result.getError().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progress.dismiss();
                    Toast.makeText(LoginActivity.this,
                            "Login failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void handleSuccessfulLogin(User user) {
        runOnUiThread(() -> {
            sessionManager.createLoginSession(
                    user.getUserId(),
                    user.getEmail(),
                    user.getName(),
                    user.getRole()
            );
            startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
            finish();
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
                startFaceLogin();
            } else {
                Toast.makeText(this, "Camera permissions are required",
                        Toast.LENGTH_SHORT).show();
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